package br.ucsal.fluxoconhecimento.estrutura;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Tabela hash usada na busca (RF08) e nos semelhantes (RF09).
// Índice invertido: para cada termo, guarda os problemas que têm esse termo e o peso dele.
// Assim a busca só olha os problemas que têm alguma palavra da consulta (O(1) para achar cada termo).
// Relevância por TF-IDF: tf = 1 + ln(peso), idf = ln(1 + N / df).
public class IndiceBuscaHash {

    // Problema encontrado: pontuação, % de semelhança e termos em comum
    public record Resultado(long id, double pontuacao, int semelhanca, List<String> termosEmComum) {
    }

    // Lista invertida: termo -> (id do problema -> peso)
    private final Map<String, Map<Long, Integer>> listasInvertidas = new HashMap<>();

    // Id do problema -> (termo -> peso), para remover/reindexar rápido
    private final Map<Long, Map<String, Integer>> documentos = new HashMap<>();

    // Indexa (ou reindexa) um problema
    public synchronized void indexar(long id, Map<String, Integer> pesos) {
        remover(id);
        if (pesos.isEmpty()) {
            return;
        }
        Map<String, Integer> copia = new HashMap<>(pesos);
        documentos.put(id, copia);
        copia.forEach((termo, peso) ->
                listasInvertidas.computeIfAbsent(termo, t -> new HashMap<>()).put(id, peso));
    }

    // Tira um problema do índice (ex.: quando é arquivado)
    public synchronized void remover(long id) {
        Map<String, Integer> termosAntigos = documentos.remove(id);
        if (termosAntigos == null) {
            return;
        }
        for (String termo : termosAntigos.keySet()) {
            Map<Long, Integer> lista = listasInvertidas.get(termo);
            if (lista != null) {
                lista.remove(id);
                if (lista.isEmpty()) {
                    listasInvertidas.remove(termo);
                }
            }
        }
    }

    public synchronized void limpar() {
        listasInvertidas.clear();
        documentos.clear();
    }

    // Consulta por código em O(1): o problema está indexado?
    public synchronized boolean contem(long id) {
        return documentos.containsKey(id);
    }

    public synchronized int totalDocumentos() {
        return documentos.size();
    }

    public synchronized int totalTermos() {
        return listasInvertidas.size();
    }

    // Ids de todos os problemas indexados
    public synchronized Set<Long> ids() {
        return Set.copyOf(documentos.keySet());
    }

    // Busca os problemas mais relevantes para os termos da consulta
    public synchronized List<Resultado> buscar(Collection<String> termosConsulta, int limite, Long ignorarId) {
        int totalDocs = documentos.size();
        Map<Long, Double> pontuacoes = new HashMap<>();
        Map<Long, Double> idfEmComum = new HashMap<>();
        Map<Long, List<String>> termosEmComum = new HashMap<>();
        double idfTotalConsulta = 0;

        // Cada termo distinto da consulta visita APENAS a sua lista na tabela hash
        for (String termo : new LinkedHashSet<>(termosConsulta)) {
            Map<Long, Integer> lista = listasInvertidas.get(termo);
            int df = lista == null ? 1 : lista.size();
            double idf = Math.log(1.0 + (double) Math.max(totalDocs, 1) / df);
            idfTotalConsulta += idf;
            if (lista == null) {
                continue;
            }
            for (Map.Entry<Long, Integer> entrada : lista.entrySet()) {
                long id = entrada.getKey();
                if (ignorarId != null && id == ignorarId) {
                    continue;
                }
                double tf = 1.0 + Math.log(entrada.getValue());
                pontuacoes.merge(id, tf * idf, Double::sum);
                idfEmComum.merge(id, idf, Double::sum);
                termosEmComum.computeIfAbsent(id, k -> new ArrayList<>()).add(termo);
            }
        }

        final double idfTotal = idfTotalConsulta;
        return pontuacoes.entrySet().stream()
                .map(e -> new Resultado(
                        e.getKey(),
                        e.getValue() / Math.sqrt(documentos.get(e.getKey()).size()),
                        (int) Math.round(100.0 * idfEmComum.get(e.getKey()) / idfTotal),
                        List.copyOf(termosEmComum.get(e.getKey()))))
                .sorted(Comparator.comparingDouble(Resultado::pontuacao).reversed()
                        .thenComparing(Resultado::id, Comparator.reverseOrder()))
                .limit(limite)
                .toList();
    }
}
