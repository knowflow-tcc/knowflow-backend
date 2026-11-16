package br.ucsal.fluxoconhecimento.dto;

import jakarta.validation.constraints.NotBlank;

// Entrada de POST /api/auth/login (RF01)
public record LoginDTO(
        @NotBlank(message = "Informe o e-mail.") String email,
        @NotBlank(message = "Informe a senha.") String senha) {
}
