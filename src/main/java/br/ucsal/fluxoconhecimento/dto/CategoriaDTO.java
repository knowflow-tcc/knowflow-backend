package br.ucsal.fluxoconhecimento.dto;

import java.util.List;

// Categoria na ordem da árvore (pré-ordem), com nível e caminho
public record CategoriaDTO(Long id, String nome, Long idPai, int nivel, String caminho, List<String> caminhoPartes) {
}
