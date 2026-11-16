package br.ucsal.fluxoconhecimento.estrutura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Testes da tabela hash de busca (RF08, RF09)
class IndiceBuscaHashTest {

    private IndiceBuscaHash indice;

    @BeforeEach
    void setUp() {
        indice = new IndiceBuscaHash();
    }

    // Indexa o texto com peso 1 por ocorrência (como a descrição de um problema)
    private void indexar(long id, String texto) {
        Map<String, Integer> pesos = new HashMap<>();
        for (String termo : NormalizadorTexto.termos(texto)) {
            pesos.merge(termo, 1, Integer::sum);
        }
        indice.indexar(id, pesos);
    }

    private List<Long> idsEncontrados(String consulta) {
        return indice.buscar(NormalizadorTexto.termos(consulta), 10, null).stream()
                .map(IndiceBuscaHash.Resultado::id)
                .toList();
    }

    @Test
    void encontraSomenteProblemasComTermosEmComum() {
        indexar(1, "impressora do financeiro nao imprime");
        indexar(2, "wifi caindo no segundo andar");
        indexar(3, "sistema de rh lento");

        assertEquals(List.of(1L), idsEncontrados("Impressoras travando"));
        assertTrue(idsEncontrados("monitor quebrado").isEmpty());
    }

    @Test
    void termoRaroValeMaisQueTermoComum() {
        // "lento" aparece em 3 problemas; "vpn" só no problema 4
        indexar(1, "sistema lento");
        indexar(2, "computador lento");
        indexar(3, "rede lento");
        indexar(4, "vpn cai");

        List<Long> ids = idsEncontrados("vpn lento");
        assertEquals(4L, ids.get(0));
        assertEquals(4, ids.size());
    }

    @Test
    void problemaComMaisTermosEmComumVemPrimeiro() {
        indexar(1, "impressora sem papel");
        indexar(2, "impressora do financeiro com papel atolado");
        indexar(3, "teclado sem fio");

        assertEquals(2L, idsEncontrados("papel atolado na impressora").get(0));
    }

    @Test
    void pesoMaiorNoTituloAumentaARelevancia() {
        indice.indexar(1, Map.of("email", 1, "celular", 1));   // "email" só na descrição
        indice.indexar(2, Map.of("email", 3, "servidor", 1));  // "email" no título (peso 3)

        assertEquals(List.of(2L, 1L), idsEncontrados("email"));
    }

    @Test
    void informaOsTermosEmComum() {
        indexar(1, "wifi do segundo andar caindo");

        IndiceBuscaHash.Resultado r = indice.buscar(NormalizadorTexto.termos("wi-fi caindo toda hora"), 10, null).get(0);
        assertEquals(List.of("wifi", "caindo"), r.termosEmComum());
    }

    @Test
    void semelhancaE100QuandoTodosOsTermosDaConsultaAparecem() {
        indexar(1, "vpn nao conecta no home office");

        IndiceBuscaHash.Resultado r = indice.buscar(NormalizadorTexto.termos("VPN não conecta"), 10, null).get(0);
        assertEquals(100, r.semelhanca());
    }

    @Test
    void semelhancaParcialQuandoSoParteDosTermosAparece() {
        indexar(1, "impressora sem papel");
        indexar(2, "monitor piscando");

        int semelhanca = indice.buscar(NormalizadorTexto.termos("impressora piscando"), 10, null).stream()
                .filter(r -> r.id() == 1).findFirst().orElseThrow().semelhanca();
        assertTrue(semelhanca > 0 && semelhanca < 100, "semelhança deveria ser parcial, foi " + semelhanca);
    }

    @Test
    void ignoraOProblemaInformado() {
        indexar(1, "vpn nao conecta");
        indexar(2, "vpn lenta");

        List<IndiceBuscaHash.Resultado> r = indice.buscar(NormalizadorTexto.termos("vpn"), 10, 1L);
        assertEquals(1, r.size());
        assertEquals(2L, r.get(0).id());
    }

    @Test
    void respeitaOLimite() {
        for (long id = 1; id <= 8; id++) {
            indexar(id, "erro no sistema " + id);
        }
        assertEquals(3, indice.buscar(NormalizadorTexto.termos("erro"), 3, null).size());
    }

    @Test
    void reindexarSubstituiOsTermosAntigos() {
        indexar(1, "impressora quebrada");
        indexar(1, "monitor quebrado");

        assertTrue(idsEncontrados("impressora").isEmpty());
        assertEquals(List.of(1L), idsEncontrados("monitor"));
        assertEquals(1, indice.totalDocumentos());
    }

    @Test
    void removerTiraOProblemaEOsTermosQueSoEleTinha() {
        indexar(1, "impressora quebrada");
        indexar(2, "monitor quebrado");

        indice.remover(1);

        assertTrue(idsEncontrados("impressora").isEmpty());
        assertEquals(List.of(2L), idsEncontrados("quebrado"));
        assertFalse(indice.contem(1));
        assertEquals(2, indice.totalTermos()); // sobram "monitor" e "quebrado"
    }

    @Test
    void consultaVaziaNaoRetornaNada() {
        indexar(1, "impressora quebrada");
        assertTrue(indice.buscar(List.of(), 10, null).isEmpty());
    }
}
