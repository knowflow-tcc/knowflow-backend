package br.ucsal.fluxoconhecimento.dto;

import jakarta.validation.constraints.NotBlank;

// RF14 - passo 2: o código recebido no link e a nova senha (RNF09)
public record RedefinirSenhaDTO(
        @NotBlank(message = "O link de recuperação está incompleto. Abra novamente o link do e-mail.") String token,
        @NotBlank(message = "Informe a nova senha.") String senha) {
}
