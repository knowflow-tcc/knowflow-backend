package br.ucsal.fluxoconhecimento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cadastro/edição de categoria; idPai vazio = raiz
public record CategoriaFormDTO(
        @NotBlank(message = "Informe o nome da categoria.")
        @Size(max = 80, message = "O nome pode ter no máximo 80 caracteres.") String nome,
        Long idPai) {
}
