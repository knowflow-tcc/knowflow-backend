package br.ucsal.fluxoconhecimento.service;

import java.util.Arrays;
import java.util.Objects;

import br.ucsal.fluxoconhecimento.excecao.AcessoNegadoException;
import br.ucsal.fluxoconhecimento.model.Causa;
import br.ucsal.fluxoconhecimento.model.Comentario;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.Usuario;

// Regras de permissão POR REGISTRO (RF12)
public final class Permissoes {

    private Permissoes() {
    }

    public static boolean podeRegistrar(Usuario usuario) {
        return usuario.getPerfil() == Perfil.COLABORADOR || usuario.getPerfil() == Perfil.GESTOR;
    }

    // RN06 - editar ou arquivar um registro criado por autor no departamento
    public static boolean podeEditar(Usuario usuario, Usuario autor, Departamento departamento) {
        if (!podeRegistrar(usuario)) {
            return false;
        }
        if (mesmoId(usuario.getId(), autor.getId())) {
            return true;
        }
        return usuario.getPerfil() == Perfil.GESTOR
                && mesmoId(usuario.getDepartamento().getId(), departamento.getId());
    }

    public static boolean podeEditarProblema(Usuario usuario, Problema problema) {
        return podeEditar(usuario, problema.getUsuario(), problema.getDepartamento());
    }

    public static boolean podeEditarCausa(Usuario usuario, Causa causa) {
        return podeEditar(usuario, causa.getUsuario(), causa.getProblema().getDepartamento());
    }

    public static boolean podeEditarSolucao(Usuario usuario, Solucao solucao) {
        return podeEditar(usuario, solucao.getUsuario(), solucao.getProblema().getDepartamento());
    }

    // RN07 - validar uma solução do problema
    public static boolean podeValidar(Usuario usuario, Problema problema) {
        if (usuario.getPerfil() == Perfil.ADMINISTRADOR) {
            return true;
        }
        return usuario.getPerfil() == Perfil.GESTOR
                && mesmoId(usuario.getDepartamento().getId(), problema.getDepartamento().getId());
    }

    // RF17 - comentam soluções os perfis Colaborador e Gestor
    public static boolean podeComentar(Usuario usuario) {
        return podeRegistrar(usuario);
    }

    // RF17 - arquiva o comentário o autor ou o gestor do departamento
    public static boolean podeArquivarComentario(Usuario usuario, Comentario comentario) {
        return podeEditar(usuario, comentario.getUsuario(), comentario.getSolucao().getProblema().getDepartamento());
    }

    // RF18 - o autor não avalia a própria solução
    public static boolean podeAvaliar(Usuario usuario, Solucao solucao) {
        return podeRegistrar(usuario) && !mesmoId(usuario.getId(), solucao.getUsuario().getId());
    }

    // RF16/RN06 - anexar a um problema: quem pode editá-lo
    public static boolean podeAnexarAoProblema(Usuario usuario, Problema problema) {
        return podeEditarProblema(usuario, problema);
    }

    // RF16/RN06 - anexar a uma solução: quem pode editá-la
    public static boolean podeAnexarASolucao(Usuario usuario, Solucao solucao) {
        return podeEditarSolucao(usuario, solucao);
    }

    // RF16 - arquiva o anexo quem enviou ou o gestor do departamento
    public static boolean podeArquivarAnexo(Usuario usuario, Long idAutorAnexo, Departamento departamentoDoProblema) {
        if (!podeRegistrar(usuario)) {
            return false;
        }
        return mesmoId(usuario.getId(), idAutorAnexo)
                || (usuario.getPerfil() == Perfil.GESTOR
                    && mesmoId(usuario.getDepartamento().getId(), departamentoDoProblema.getId()));
    }

    // Explica, conforme o perfil, por que a edição foi negada (RNF13)
    public static String motivoSemPermissaoDeEdicao(Usuario usuario) {
        return switch (usuario.getPerfil()) {
            case COLABORADOR -> "Você só pode alterar os registros que você mesmo criou.";
            case GESTOR -> "Você só pode alterar os registros que criou ou que são do seu departamento.";
            case ADMINISTRADOR -> "O administrador não altera registros de conhecimento; isso é feito por colaboradores e gestores.";
        };
    }

    public static void exigir(boolean permitido, String mensagem) {
        if (!permitido) {
            throw new AcessoNegadoException(mensagem);
        }
    }

    public static void exigirPerfil(Usuario usuario, Perfil... perfis) {
        if (Arrays.stream(perfis).noneMatch(p -> p == usuario.getPerfil())) {
            throw new AcessoNegadoException("O seu perfil (" + usuario.getPerfil().getRotulo()
                    + ") não tem acesso a esta funcionalidade.");
        }
    }

    private static boolean mesmoId(Long a, Long b) {
        return a != null && Objects.equals(a, b);
    }
}
