package br.ucsal.fluxoconhecimento;

import java.time.LocalDateTime;

import br.ucsal.fluxoconhecimento.model.Categoria;
import br.ucsal.fluxoconhecimento.model.Causa;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Solucao;
import br.ucsal.fluxoconhecimento.model.StatusProblema;
import br.ucsal.fluxoconhecimento.model.Usuario;

// Cria objetos de dominio prontos para os testes unitarios
public final class Fabrica {

    private Fabrica() {
    }

    public static Departamento departamento(long id, String nome) {
        Departamento d = new Departamento(nome, null);
        d.setId(id);
        return d;
    }

    public static Usuario usuario(long id, Perfil perfil, Departamento departamento) {
        Usuario u = new Usuario("Usuário " + id, "usuario" + id + "@empresa.com", "hash", perfil, departamento);
        u.setId(id);
        return u;
    }

    public static Categoria categoria(long id, String nome, Categoria pai) {
        Categoria c = new Categoria(nome, pai);
        c.setId(id);
        return c;
    }

    public static Problema problema(long id, Usuario autor, Departamento departamento, Categoria categoria) {
        Problema p = new Problema();
        p.setId(id);
        p.setTitulo("Problema " + id);
        p.setDescricao("Descrição do problema " + id);
        p.setUsuario(autor);
        p.setDepartamento(departamento);
        p.setCategoria(categoria);
        p.setDataRegistro(LocalDateTime.now());
        p.setStatus(StatusProblema.ABERTO);
        return p;
    }

    public static Causa causa(long id, Problema problema, Usuario autor) {
        Causa c = new Causa("Causa " + id, problema, autor);
        c.setId(id);
        problema.getCausas().add(c);
        return c;
    }

    public static Solucao solucao(long id, Problema problema, Usuario autor) {
        Solucao s = new Solucao("Solução " + id, problema, autor, LocalDateTime.now());
        s.setId(id);
        problema.getSolucoes().add(s);
        return s;
    }
}
