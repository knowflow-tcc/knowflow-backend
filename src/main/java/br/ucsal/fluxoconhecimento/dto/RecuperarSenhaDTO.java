package br.ucsal.fluxoconhecimento.dto;

import jakarta.validation.constraints.NotBlank;

// RF14 - passo 1: {"email": "..."}
public record RecuperarSenhaDTO(
        @NotBlank(message = "Informe o seu e-mail.") String email) {
}
