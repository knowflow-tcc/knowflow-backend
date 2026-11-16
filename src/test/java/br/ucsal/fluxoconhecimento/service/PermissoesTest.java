package br.ucsal.fluxoconhecimento.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.ucsal.fluxoconhecimento.Fabrica;
import br.ucsal.fluxoconhecimento.excecao.AcessoNegadoException;
import br.ucsal.fluxoconhecimento.model.Categoria;
import br.ucsal.fluxoconhecimento.model.Departamento;
import br.ucsal.fluxoconhecimento.model.Perfil;
import br.ucsal.fluxoconhecimento.model.Problema;
import br.ucsal.fluxoconhecimento.model.Usuario;

// RF12, RN06 e RN07 - quem pode registrar, editar e validar
class PermissoesTest {

    private Departamento ti;
    private Departamento financeiro;
    private Usuario autor;
    private Usuario colegaDaTi;
    private Usuario gestorDaTi;
    private Usuario gestorDoFinanceiro;
    private Usuario admin;
    private Problema problemaDaTi;

    @BeforeEach
    void setUp() {
        ti = Fabrica.departamento(1, "TI");
        financeiro = Fabrica.departamento(2, "Financeiro");
        autor = Fabrica.usuario(1, Perfil.COLABORADOR, ti);
        colegaDaTi = Fabrica.usuario(2, Perfil.COLABORADOR, ti);
        gestorDaTi = Fabrica.usuario(3, Perfil.GESTOR, ti);
        gestorDoFinanceiro = Fabrica.usuario(4, Perfil.GESTOR, financeiro);
        admin = Fabrica.usuario(5, Perfil.ADMINISTRADOR, ti);
        Categoria categoria = Fabrica.categoria(1, "Redes", null);
        problemaDaTi = Fabrica.problema(1, autor, ti, categoria);
    }

    @Test
    void colaboradorEGestorRegistramAdministradorNao() {
        assertTrue(Permissoes.podeRegistrar(autor));
        assertTrue(Permissoes.podeRegistrar(gestorDaTi));
        assertFalse(Permissoes.podeRegistrar(admin));
    }

    @Test
    void rn06ColaboradorSoEditaOQueCriou() {
        assertTrue(Permissoes.podeEditarProblema(autor, problemaDaTi));
        assertFalse(Permissoes.podeEditarProblema(colegaDaTi, problemaDaTi));
    }

    @Test
    void rn06GestorEditaOsRegistrosDoSeuDepartamento() {
        assertTrue(Permissoes.podeEditarProblema(gestorDaTi, problemaDaTi));
        assertFalse(Permissoes.podeEditarProblema(gestorDoFinanceiro, problemaDaTi));
    }

    @Test
    void rn06GestorTambemEditaOQueEleMesmoCriouEmOutroDepartamento() {
        Problema doGestorNoFinanceiro = Fabrica.problema(2, gestorDaTi, financeiro, problemaDaTi.getCategoria());
        assertTrue(Permissoes.podeEditarProblema(gestorDaTi, doGestorNoFinanceiro));
    }

    @Test
    void rn07ValidaSomenteOGestorDoDepartamentoDoProblemaOuOAdministrador() {
        assertTrue(Permissoes.podeValidar(gestorDaTi, problemaDaTi));
        assertTrue(Permissoes.podeValidar(admin, problemaDaTi));
        assertFalse(Permissoes.podeValidar(gestorDoFinanceiro, problemaDaTi));
        assertFalse(Permissoes.podeValidar(autor, problemaDaTi));
    }

    @Test
    void exigirPerfilLancaAcessoNegado() {
        assertThrows(AcessoNegadoException.class, () -> Permissoes.exigirPerfil(autor, Perfil.ADMINISTRADOR));
    }
}
