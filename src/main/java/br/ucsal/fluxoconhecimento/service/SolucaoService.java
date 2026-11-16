package br.ucsal.fluxoconhecimento.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.DescricaoFormDTO;
import br.ucsal.fluxoconhecimento.dto.SolucaoDTO;
import br.ucsal.fluxoconhecimento.excecao.RecursoNaoEncontradoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.StatusProblema;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.SolucaoRepository;

// RF06 - soluções; RF15 - editar e arquivar; RF10 - validação
@Service
public class SolucaoService {

    private final SolucaoRepository solucaoRepository;
    private final ProblemaService problemaService;
    private final BuscaService buscaService;
    private final HistoricoService historicoService;
    private final UsuarioLogadoService usuarioLogado;
    private final NotificacaoService notificacaoService;

    public SolucaoService(SolucaoRepository solucaoRepository, ProblemaService problemaService,
                          BuscaService buscaService, HistoricoService historicoService,
                          UsuarioLogadoService usuarioLogado, NotificacaoService notificacaoService) {
        this.solucaoRepository = solucaoRepository;
        this.problemaService = problemaService;
        this.buscaService = buscaService;
        this.historicoService = historicoService;
        this.usuarioLogado = usuarioLogado;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public SolucaoDTO registrar(Long idProblema, DescricaoFormDTO form, Long idUsuarioLogado) {
        Usuario autor = usuarioLogado.obter(idUsuarioLogado);
        Permissoes.exigir(Permissoes.podeRegistrar(autor), "Somente colaboradores e gestores registram soluções.");
        Problema problema = problemaService.buscarEntidade(idProblema);
        if (!problema.isAtivo()) {
            throw new RegraNegocioException("Este problema está arquivado; não é possível incluir soluções.");
        }

        Solucao solucao = solucaoRepository.save(
                new Solucao(form.descricao().trim(), problema, autor, LocalDateTime.now()));
        problema.getSolucoes().add(solucao);
        historicoService.registrar(AcaoHistorico.INCLUSAO, Historico.TABELA_SOLUCAO, solucao.getId(), autor);
        buscaService.indexar(problema);
        return Conversor.solucao(solucao, autor);
    }

    @Transactional
    public SolucaoDTO atualizar(Long id, DescricaoFormDTO form, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Solucao solucao = buscarAtiva(id);
        Permissoes.exigir(Permissoes.podeEditarSolucao(usuario, solucao), Permissoes.motivoSemPermissaoDeEdicao(usuario));

        solucao.setDescricao(form.descricao().trim());
        historicoService.registrar(AcaoHistorico.ALTERACAO, Historico.TABELA_SOLUCAO, solucao.getId(), usuario);
        buscaService.indexar(solucao.getProblema());
        return Conversor.solucao(solucao, usuario);
    }

    @Transactional
    public SolucaoDTO arquivar(Long id, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Solucao solucao = buscarAtiva(id);
        Permissoes.exigir(Permissoes.podeEditarSolucao(usuario, solucao), Permissoes.motivoSemPermissaoDeEdicao(usuario));

        solucao.setAtivo(false);
        historicoService.registrar(AcaoHistorico.ARQUIVAMENTO, Historico.TABELA_SOLUCAO, solucao.getId(), usuario);
        buscaService.indexar(solucao.getProblema());
        return Conversor.solucao(solucao, usuario);
    }

    // RF10 - o gestor indica a solução como eficaz
    @Transactional
    public SolucaoDTO validar(Long id, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Solucao solucao = buscarAtiva(id);
        Problema problema = solucao.getProblema();
        Permissoes.exigir(Permissoes.podeValidar(usuario, problema),
                "Somente o gestor do departamento " + problema.getDepartamento().getNome()
                        + " ou um administrador pode validar esta solução.");
        if (solucao.isValidada()) {
            throw new RegraNegocioException("Esta solução já está validada.");
        }

        solucao.setValidada(true);
        historicoService.registrar(AcaoHistorico.VALIDACAO, Historico.TABELA_SOLUCAO, solucao.getId(), usuario);

        // RN08 - o problema passa automaticamente para "resolvido"
        if (problema.getStatus() != StatusProblema.RESOLVIDO) {
            problema.setStatus(StatusProblema.RESOLVIDO);
            historicoService.registrar(AcaoHistorico.ALTERACAO, Historico.TABELA_PROBLEMA, problema.getId(), usuario);
        }

        // RF21 - avisa o autor da solução
        notificacaoService.notificar(solucao.getUsuario(), usuario, problema,
                "Sua solução em \"" + problema.getTitulo() + "\" foi validada por " + usuario.getNome() + ".");
        return Conversor.solucao(solucao, usuario);
    }

    private Solucao buscarAtiva(Long id) {
        Solucao solucao = solucaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solução não encontrada."));
        if (!solucao.isAtivo() || !solucao.getProblema().isAtivo()) {
            throw new RegraNegocioException("Esta solução (ou o seu problema) está arquivada e não pode mais ser alterada.");
        }
        return solucao;
    }
}
