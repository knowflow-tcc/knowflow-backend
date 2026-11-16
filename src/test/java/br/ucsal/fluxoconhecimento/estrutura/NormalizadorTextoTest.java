package br.ucsal.fluxoconhecimento.estrutura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class NormalizadorTextoTest {

    @Test
    void removeAcentosEMaiusculas() {
        assertEquals(List.of("conexao", "lenta"), NormalizadorTexto.termos("Conexão LENTA"));
    }

    @Test
    void descartaStopwordsEPalavrasDeUmaLetra() {
        assertEquals(List.of("impressora", "imprime"), NormalizadorTexto.termos("A impressora não imprime"));
        assertEquals(List.of("erro", "sistema"), NormalizadorTexto.termos("o erro no sistema e"));
    }

    @Test
    void juntaPalavrasComHifen() {
        assertEquals(List.of("wifi", "email"), NormalizadorTexto.termos("Wi-Fi e e-mail"));
    }

    @Test
    void reduzPluraisAoSingular() {
        assertEquals(List.of("impressora", "solucao", "mensagem", "tela"),
                NormalizadorTexto.termos("impressoras soluções mensagens telas"));
    }

    @Test
    void naoReduzPalavrasTerminadasEmUsOuSs() {
        assertEquals(List.of("virus", "status", "acess"), NormalizadorTexto.termos("vírus status acess"));
    }

    @Test
    void textoVazioOuNuloGeraListaVazia() {
        assertTrue(NormalizadorTexto.termos(null).isEmpty());
        assertTrue(NormalizadorTexto.termos("   ").isEmpty());
        assertTrue(NormalizadorTexto.termos("de para com").isEmpty());
    }

    @Test
    void consultaEIndiceGeramOMesmoTermoParaVariacoes() {
        // Tudo isso deve virar o mesmo termo, senão a busca não encontraria
        assertEquals(NormalizadorTexto.termos("Impressoras"), NormalizadorTexto.termos("impressora"));
        assertEquals(NormalizadorTexto.termos("WIFI"), NormalizadorTexto.termos("wi-fi"));
        assertEquals(NormalizadorTexto.termos("Solução"), NormalizadorTexto.termos("solucoes"));
    }
}
