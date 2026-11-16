package br.ucsal.fluxoconhecimento.estrutura;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Árvore de categorias (categoria -> subcategorias).
// No banco cada categoria só guarda o id do pai; aqui a árvore é montada em memória.
// Usada no menu (pré-ordem), no caminho da categoria e na busca por categoria (inclui as subcategorias).
// Complexidade: montar O(n log n), caminho O(h), subárvore e percurso O(k).
public class ArvoreCategorias {

    // Dados mínimos de uma categoria para montar a árvore
    public record Entrada(long id, String nome, Long idPai) {
    }

    // Nó da árvore: uma categoria, seu pai e seus filhos
    public static final class No {
        private final long id;
        private final String nome;
        private No pai;
        private final List<No> filhos = new ArrayList<>();
        private int nivel;

        private No(long id, String nome) {
            this.id = id;
            this.nome = nome;
        }

        public long getId() {
            return id;
        }

        public String getNome() {
            return nome;
        }

        public No getPai() {
            return pai;
        }

        public List<No> getFilhos() {
            return Collections.unmodifiableList(filhos);
        }

        // Profundidade do nó: 0 para as raízes
        public int getNivel() {
            return nivel;
        }

        public boolean isFolha() {
            return filhos.isEmpty();
        }
    }

    private static final Comparator<No> POR_NOME =
            Comparator.comparing(No::getNome, String.CASE_INSENSITIVE_ORDER).thenComparingLong(No::getId);

    // Tabela hash id -> no: acesso direto a qualquer categoria em O(1)
    private final Map<Long, No> nos = new HashMap<>();
    private final List<No> raizes = new ArrayList<>();

    public ArvoreCategorias(Collection<Entrada> categorias) {
        // 1a passada: cria todos os nós
        for (Entrada c : categorias) {
            nos.put(c.id(), new No(c.id(), c.nome()));
        }
        // 2a passada: liga cada nó ao seu pai (sem pai, ou pai inexistente = raiz)
        for (Entrada c : categorias) {
            No no = nos.get(c.id());
            No pai = c.idPai() == null ? null : nos.get(c.idPai());
            if (pai == null || pai == no) {
                raizes.add(no);
            } else {
                no.pai = pai;
                pai.filhos.add(no);
            }
        }
        raizes.sort(POR_NOME);
        nos.values().forEach(no -> no.filhos.sort(POR_NOME));
        calcularNiveis();
    }

    // Percurso em largura a partir das raízes, definindo o nível de cada nó
    private void calcularNiveis() {
        Deque<No> fila = new ArrayDeque<>(raizes);
        Set<No> visitados = new HashSet<>();
        while (!fila.isEmpty()) {
            No no = fila.poll();
            if (!visitados.add(no)) {
                continue;
            }
            no.nivel = no.pai == null ? 0 : no.pai.nivel + 1;
            fila.addAll(no.filhos);
        }
    }

    public List<No> raizes() {
        return Collections.unmodifiableList(raizes);
    }

    public No no(long id) {
        return nos.get(id);
    }

    public boolean contem(long id) {
        return nos.containsKey(id);
    }

    public int tamanho() {
        return nos.size();
    }

    // Percurso em pré-ordem: lista na ordem do menu, com o nível de cada item
    public List<No> percorrerEmPreOrdem() {
        List<No> resultado = new ArrayList<>(nos.size());
        for (No raiz : raizes) {
            preOrdem(raiz, resultado, new HashSet<>());
        }
        return resultado;
    }

    private void preOrdem(No no, List<No> resultado, Set<No> visitados) {
        if (!visitados.add(no)) {
            return;
        }
        resultado.add(no);
        for (No filho : no.filhos) {
            preOrdem(filho, resultado, visitados);
        }
    }

    // Caminho da raiz até a categoria, ex.: [Infraestrutura, Redes e Internet, VPN]
    public List<String> caminho(long id) {
        List<String> nomes = new ArrayList<>();
        Set<No> visitados = new HashSet<>();
        for (No no = nos.get(id); no != null && visitados.add(no); no = no.pai) {
            nomes.add(no.nome);
        }
        Collections.reverse(nomes);
        return nomes;
    }

    // Caminho em texto: "Infraestrutura > Redes e Internet > VPN"
    public String caminhoTexto(long id) {
        return String.join(" > ", caminho(id));
    }

    // Ids da categoria e de todas as descendentes (busca em profundidade)
    public Set<Long> idsDaSubarvore(long id) {
        Set<Long> ids = new LinkedHashSet<>();
        No inicio = nos.get(id);
        if (inicio == null) {
            return ids;
        }
        Deque<No> pilha = new ArrayDeque<>();
        pilha.push(inicio);
        while (!pilha.isEmpty()) {
            No no = pilha.pop();
            if (ids.add(no.id)) {
                for (int i = no.filhos.size() - 1; i >= 0; i--) {
                    pilha.push(no.filhos.get(i));
                }
            }
        }
        return ids;
    }

    // Mover idCategoria para baixo de idNovoPai criaria um ciclo?
    public boolean criariaCiclo(long idCategoria, Long idNovoPai) {
        if (idNovoPai == null) {
            return false;
        }
        return idsDaSubarvore(idCategoria).contains(idNovoPai);
    }
}
