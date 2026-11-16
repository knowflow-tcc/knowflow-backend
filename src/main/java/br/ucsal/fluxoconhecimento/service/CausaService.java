package br.ucsal.fluxoconhecimento.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.CausaDTO;
import br.ucsal.fluxoconhecimento.dto.DescricaoFormDTO;
import br.ucsal.fluxoconhecimento.excecao.RecursoNaoEncontradoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Causa;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.CausaRepository;

// RF05 - associar uma ou mais causas a um problema; RF15 - editar e arquivar
@Service
public class CausaService {

    private final CausaRepository causaRepository;
    private final ProblemaService problemaService;
    private final BuscaService buscaService;
    private final HistoricoService historicoService;
    private final UsuarioLogadoService usuarioLogado;

    public CausaService(CausaRepository causaRepository, ProblemaService problemaService, BuscaService buscaService,
                        HistoricoService historicoService, UsuarioLogadoService usuarioLogado) {
        this.causaRepository = causaRepository;
        this.problemaService = problemaService;
        this.buscaService = buscaService;
        this.historicoService = historicoService;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional
    public CausaDTO registrar(Long idProblema, DescricaoFormDTO form, Long idUsuarioLogado) {
        Usuario autor = usuarioLogado.obter(idUsuarioLogado);
        Permissoes.exigir(Permissoes.podeRegistrar(autor), "Somente colaboradores e gestores registram causas.");
        Problema problema = problemaAtivoParaInclusao(idProblema);

        Causa causa = causaRepository.save(new Causa(form.descricao().trim(), problema, autor));
        problema.getCausas().add(causa);
        historicoService.registrar(AcaoHistorico.INCLUSAO, Historico.TABELA_CAUSA, causa.getId(), autor);
        buscaService.indexar(problema);
        return Conversor.causa(causa, autor);
    }

    @Transactional
    public CausaDTO atualizar(Long id, DescricaoFormDTO form, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Causa causa = buscarEditavel(id, usuario);
        causa.setDescricao(form.descricao().trim());
        historicoService.registrar(AcaoHistorico.ALTERACAO, Historico.TABELA_CAUSA, causa.getId(), usuario);
        buscaService.indexar(causa.getProblema());
        return Conversor.causa(causa, usuario);
    }

    @Transactional
    public CausaDTO arquivar(Long id, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Causa causa = buscarEditavel(id, usuario);
        causa.setAtivo(false);
        historicoService.registrar(AcaoHistorico.ARQUIVAMENTO, Historico.TABELA_CAUSA, causa.getId(), usuario);
        buscaService.indexar(causa.getProblema());
        return Conversor.causa(causa, usuario);
    }

    // RN05 - o problema precisa existir e estar ativo
    private Problema problemaAtivoParaInclusao(Long idProblema) {
        Problema problema = problemaService.buscarEntidade(idProblema);
        if (!problema.isAtivo()) {
            throw new RegraNegocioException("Este problema está arquivado; não é possível incluir causas.");
        }
        return problema;
    }

    private Causa buscarEditavel(Long id, Usuario usuario) {
        Causa causa = causaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Causa não encontrada."));
        if (!causa.isAtivo() || !causa.getProblema().isAtivo()) {
            throw new RegraNegocioException("Esta causa (ou o seu problema) está arquivada e não pode mais ser alterada.");
        }
        Permissoes.exigir(Permissoes.podeEditarCausa(usuario, causa), Permissoes.motivoSemPermissaoDeEdicao(usuario));
        return causa;
    }
}
