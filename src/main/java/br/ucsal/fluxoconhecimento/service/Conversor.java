package br.ucsal.fluxoconhecimento.service;

import java.util.List;

import br.ucsal.fluxoconhecimento.dto.AnexoDTO;
import br.ucsal.fluxoconhecimento.dto.AnexoInfo;
import br.ucsal.fluxoconhecimento.dto.CausaDTO;
import br.ucsal.fluxoconhecimento.dto.ComentarioDTO;
import br.ucsal.fluxoconhecimento.dto.HistoricoDTO;
import br.ucsal.fluxoconhecimento.dto.NotificacaoDTO;
import br.ucsal.fluxoconhecimento.dto.SolucaoDTO;
import br.ucsal.fluxoconhecimento.dto.UsuarioDTO;
import br.ucsal.fluxoconhecimento.dto.UsuarioLogadoDTO;
import br.ucsal.fluxoconhecimento.model.AvaliacaoSolucao;
import br.ucsal.fluxoconhecimento.model.Causa;
import br.ucsal.fluxoconhecimento.model.Comentario;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Historico;
import br.ucsal.fluxoconhecimento.model.Notificacao;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.Usuario;

// Converte entidades em DTOs
public final class Conversor {

    private Conversor() {
    }

    public static UsuarioDTO usuario(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getNome(), u.getEmail(), u.getPerfil().name(), u.getPerfil().getRotulo(),
                u.getDepartamento().getId(), u.getDepartamento().getNome(), u.isAtivo());
    }

    public static UsuarioLogadoDTO usuarioLogado(Usuario u) {
        return new UsuarioLogadoDTO(u.getId(), u.getNome(), u.getEmail(), u.getPerfil().name(),
                u.getPerfil().getRotulo(), u.getDepartamento().getId(), u.getDepartamento().getNome());
    }

    public static HistoricoDTO historico(Historico h) {
        return new HistoricoDTO(h.getId(), h.getAcao().name(), h.getAcao().getRotulo(), h.getTabelaAfetada(),
                h.getIdRegistro(), h.getUsuario().getNome(), h.getDataHora());
    }

    public static CausaDTO causa(Causa c, Usuario logado) {
        boolean podeEditar = c.isAtivo() && c.getProblema().isAtivo() && Permissoes.podeEditarCausa(logado, c);
        return new CausaDTO(c.getId(), c.getDescricao(), c.getUsuario().getNome(), podeEditar);
    }

    // Solução sem a lista de anexos (respostas das operações sobre uma única solução)
    public static SolucaoDTO solucao(Solucao s, Usuario logado) {
        return solucao(s, logado, List.of());
    }

    public static SolucaoDTO solucao(Solucao s, Usuario logado, List<AnexoDTO> anexos) {
        boolean editavel = s.isAtivo() && s.getProblema().isAtivo();
        Boolean minhaAvaliacao = s.getAvaliacoes().stream()
                .filter(a -> a.getUsuario().getId().equals(logado.getId()))
                .map(AvaliacaoSolucao::isAjudou)
                .findFirst().orElse(null);
        return new SolucaoDTO(s.getId(), s.getDescricao(), s.isValidada(), s.getDataRegistro(),
                s.getUsuario().getNome(),
                editavel && Permissoes.podeEditarSolucao(logado, s),
                editavel && !s.isValidada() && Permissoes.podeValidar(logado, s.getProblema()),
                s.getTotalAjudou(),
                s.getTotalNaoAjudou(),
                minhaAvaliacao,
                editavel && Permissoes.podeAvaliar(logado, s),
                editavel && Permissoes.podeComentar(logado),
                s.getComentariosAtivos().stream().map(c -> comentario(c, logado)).toList(),
                anexos);
    }

    public static ComentarioDTO comentario(Comentario c, Usuario logado) {
        boolean editavel = c.isAtivo() && c.getSolucao().isAtivo() && c.getSolucao().getProblema().isAtivo();
        return new ComentarioDTO(c.getId(), c.getTexto(), c.getDataHora(), c.getUsuario().getNome(),
                editavel && Permissoes.podeArquivarComentario(logado, c));
    }

    public static AnexoDTO anexo(AnexoInfo a, Usuario logado, Departamento departamentoDoProblema, boolean problemaAtivo) {
        return new AnexoDTO(a.id(), a.nomeArquivo(), a.tipoConteudo(), a.tamanho(),
                a.tipoConteudo() != null && a.tipoConteudo().startsWith("image/"),
                a.dataHora(), a.autorNome(),
                problemaAtivo && Permissoes.podeArquivarAnexo(logado, a.idAutor(), departamentoDoProblema));
    }

    public static NotificacaoDTO notificacao(Notificacao n) {
        return new NotificacaoDTO(n.getId(), n.getMensagem(), n.isLida(), n.getDataHora(), n.getProblema().getId());
    }
}
