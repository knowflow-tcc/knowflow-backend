package br.ucsal.fluxoconhecimento.dto;

// Quem está logado (usado para montar o menu)
public record UsuarioLogadoDTO(
        Long id,
        String nome,
        String email,
        String perfil,
        String perfilRotulo,
        Long departamentoId,
        String departamentoNome) {
}
