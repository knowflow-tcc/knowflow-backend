package br.ucsal.fluxoconhecimento.dto;

import java.util.List;

// Nó da árvore de categorias com seus filhos (GET /api/categorias/arvore)
public record CategoriaNoDTO(Long id, String nome, Long idPai, int nivel, String caminho, List<CategoriaNoDTO> filhos) {
}
