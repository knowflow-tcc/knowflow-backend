package br.ucsal.fluxoconhecimento.service;

// Aviso de que a árvore de categorias mudou (nome ou posição)
public record CategoriasAlteradasEvent(Long idCategoria) {
}
