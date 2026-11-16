package br.ucsal.fluxoconhecimento.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.ComentarioDTO;
import br.ucsal.fluxoconhecimento.dto.ComentarioFormDTO;
import br.ucsal.fluxoconhecimento.excecao.RecursoNaoEncontradoException;
import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;
import br.ucsal.fluxoconhecimento.model.AcaoHistorico;
import br.ucsal.fluxoconhecimento.model.Comentario;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.ComentarioRepository;
import br.ucsal.fluxoconhecimento.repository.SolucaoRepository;

// RF17 - comentar soluções registradas (Colaborador e Gestor)
@Service
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final SolucaoRepository solucaoRepository;
    private final HistoricoService historicoService;
    private final NotificacaoService notificacaoService;
    private final UsuarioLogadoService usuarioLogado;

    public ComentarioService(ComentarioRepository comentarioRepository, SolucaoRepository solucaoRepository,
                             HistoricoService historicoService, NotificacaoService notificacaoService,
                             UsuarioLogadoService usuarioLogado) {
        this.comentarioRepository = comentarioRepository;
        this.solucaoRepository = solucaoRepository;
        this.historicoService = historicoService;
        this.notificacaoService = notificacaoService;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional
    public ComentarioDTO comentar(Long idSolucao, ComentarioFormDTO form, Long idUsuarioLogado) {
        Usuario autor = usuarioLogado.obter(idUsuarioLogado);
        Permissoes.exigir(Permissoes.podeComentar(autor), "Somente colaboradores e gestores comentam soluções.");
        Solucao solucao = solucaoRepository.findById(idSolucao)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solução não encontrada."));
        if (!solucao.isAtivo() || !solucao.getProblema().isAtivo()) {
            throw new RegraNegocioException("Esta solução (ou o seu problema) está arquivada e não recebe comentários.");
        }

        Comentario comentario = comentarioRepository.save(
                new Comentario(form.texto().trim(), solucao, autor, LocalDateTime.now()));
        solucao.getComentarios().add(comentario);
        historicoService.registrar(AcaoHistorico.INCLUSAO, Historico.TABELA_COMENTARIO, comentario.getId(), autor);
        notificacaoService.notificar(solucao.getUsuario(), autor, solucao.getProblema(),
                autor.getNome() + " comentou a sua solução em \"" + solucao.getProblema().getTitulo() + "\".");
        return Conversor.comentario(comentario, autor);
    }

    @Transactional
    public ComentarioDTO arquivar(Long id, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Comentario comentario = comentarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Comentário não encontrado."));
        if (!comentario.isAtivo()) {
            throw new RegraNegocioException("Este comentário já está arquivado.");
        }
        Permissoes.exigir(Permissoes.podeArquivarComentario(usuario, comentario),
                Permissoes.motivoSemPermissaoDeEdicao(usuario));

        comentario.setAtivo(false);
        historicoService.registrar(AcaoHistorico.ARQUIVAMENTO, Historico.TABELA_COMENTARIO, comentario.getId(), usuario);
        return Conversor.comentario(comentario, usuario);
    }
}
