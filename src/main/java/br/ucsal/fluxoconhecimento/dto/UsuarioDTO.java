package br.ucsal.fluxoconhecimento.dto;

public record UsuarioDTO(
        Long id,
        String nome,
        String email,
        String perfil,
        String perfilRotulo,
        Long departamentoId,
        String departamentoNome,
        boolean ativo) {
}
