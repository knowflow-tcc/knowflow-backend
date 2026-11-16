package br.ucsal.fluxoconhecimento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Entrada de cadastro/edição de departamento (RF03)
public record DepartamentoFormDTO(
        @NotBlank(message = "Informe o nome do departamento.")
        @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.") String nome,
        @Size(max = 500, message = "A descrição pode ter no máximo 500 caracteres.") String descricao) {
}
