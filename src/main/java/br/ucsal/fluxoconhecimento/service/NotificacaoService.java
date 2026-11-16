package br.ucsal.fluxoconhecimento.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.ucsal.fluxoconhecimento.dto.NotificacoesDTO;
import br.ucsal.fluxoconhecimento.excecao.RecursoNaoEncontradoException;
import br.ucsal.fluxoconhecimento.model.Notificacao;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Usuario;
import br.ucsal.fluxoconhecimento.repository.NotificacaoRepository;

// RF21 - notifica o autor quando a sua solução é validada ou comentada
@Service
public class NotificacaoService {

    static final int LIMITE_LISTA = 20;

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioLogadoService usuarioLogado;

    public NotificacaoService(NotificacaoRepository notificacaoRepository, UsuarioLogadoService usuarioLogado) {
        this.notificacaoRepository = notificacaoRepository;
        this.usuarioLogado = usuarioLogado;
    }

    // Cria o aviso para destinatario
    @Transactional
    public void notificar(Usuario destinatario, Usuario autorDaAcao, Problema problema, String mensagem) {
        if (Objects.equals(destinatario.getId(), autorDaAcao.getId()) || !destinatario.isAtivo()) {
            return;
        }
        notificacaoRepository.save(new Notificacao(destinatario, problema, mensagem, LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public NotificacoesDTO listar(Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        return new NotificacoesDTO(
                notificacaoRepository.countByUsuarioIdAndLidaFalse(usuario.getId()),
                notificacaoRepository.findByUsuarioIdOrderByDataHoraDescIdDesc(usuario.getId(),
                        PageRequest.of(0, LIMITE_LISTA)).stream().map(Conversor::notificacao).toList());
    }

    @Transactional
    public void marcarComoLida(Long id, Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        Notificacao notificacao = notificacaoRepository.findById(id)
                .filter(n -> n.getUsuario().getId().equals(usuario.getId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada."));
        notificacao.setLida(true);
    }

    @Transactional
    public void marcarTodasComoLidas(Long idUsuarioLogado) {
        Usuario usuario = usuarioLogado.obter(idUsuarioLogado);
        notificacaoRepository.marcarTodasComoLidas(usuario.getId());
    }
}
