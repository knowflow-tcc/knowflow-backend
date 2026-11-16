package br.ucsal.fluxoconhecimento.estrutura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Testes da árvore de categorias
class ArvoreCategoriasTest {

    private ArvoreCategorias arvore;

    @BeforeEach
    void setUp() {
        arvore = new ArvoreCategorias(List.of(
                new ArvoreCategorias.Entrada(4, "VPN", 2L),
                new ArvoreCategorias.Entrada(1, "Infraestrutura", null),
                new ArvoreCategorias.Entrada(2, "Redes e Internet", 1L),
                new ArvoreCategorias.Entrada(3, "Impressoras", 1L),
                new ArvoreCategorias.Entrada(5, "Sistemas", null),
                new ArvoreCategorias.Entrada(6, "E-mail", 5L)));
    }

    @Test
    void montaAsRaizesEmOrdemAlfabetica() {
        assertEquals(List.of("Infraestrutura", "Sistemas"),
                arvore.raizes().stream().map(ArvoreCategorias.No::getNome).toList());
    }

    @Test
    void percursoEmPreOrdemSegueAOrdemDoMenuComNiveis() {
        List<ArvoreCategorias.No> nos = arvore.percorrerEmPreOrdem();

        assertEquals(List.of("Infraestrutura", "Impressoras", "Redes e Internet", "VPN", "Sistemas", "E-mail"),
                nos.stream().map(ArvoreCategorias.No::getNome).toList());
        assertEquals(List.of(0, 1, 1, 2, 0, 1), nos.stream().map(ArvoreCategorias.No::getNivel).toList());
    }

    @Test
    void caminhoVaiDaRaizAteACategoria() {
        assertEquals(List.of("Infraestrutura", "Redes e Internet", "VPN"), arvore.caminho(4));
        assertEquals("Infraestrutura > Redes e Internet > VPN", arvore.caminhoTexto(4));
        assertEquals(List.of("Sistemas"), arvore.caminho(5));
    }

    @Test
    void subarvoreIncluiACategoriaETodasAsDescendentes() {
        assertEquals(Set.of(1L, 2L, 3L, 4L), arvore.idsDaSubarvore(1));
        assertEquals(Set.of(4L), arvore.idsDaSubarvore(4));
        assertTrue(arvore.idsDaSubarvore(99).isEmpty());
    }

    @Test
    void detectaCicloAoMoverParaDentroDaPropriaSubarvore() {
        assertTrue(arvore.criariaCiclo(1, 1L), "categoria dentro dela mesma");
        assertTrue(arvore.criariaCiclo(1, 4L), "Infraestrutura dentro de VPN (neta)");
        assertFalse(arvore.criariaCiclo(4, 5L), "VPN dentro de Sistemas e permitido");
        assertFalse(arvore.criariaCiclo(2, null), "virar categoria principal e permitido");
    }

    @Test
    void identificaFolhasEFilhos() {
        assertTrue(arvore.no(4).isFolha());
        assertFalse(arvore.no(1).isFolha());
        assertEquals(1L, arvore.no(2).getPai().getId());
        assertEquals(2, arvore.no(1).getFilhos().size());
    }

    @Test
    void paiInexistenteViraRaiz() {
        ArvoreCategorias comOrfa = new ArvoreCategorias(List.of(new ArvoreCategorias.Entrada(10, "Órfã", 77L)));
        assertEquals(1, comOrfa.raizes().size());
        assertEquals(List.of("Órfã"), comOrfa.caminho(10));
    }

    @Test
    void arvoreVazia() {
        ArvoreCategorias vazia = new ArvoreCategorias(List.of());
        assertEquals(0, vazia.tamanho());
        assertTrue(vazia.percorrerEmPreOrdem().isEmpty());
        assertTrue(vazia.caminho(1).isEmpty());
    }
}
