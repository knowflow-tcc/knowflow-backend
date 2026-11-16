package br.ucsal.fluxoconhecimento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Entrada de causa (RF05) ou solução (RF06): apenas o texto
public record DescricaoFormDTO(
        @NotBlank(message = "Escreva a descrição.")
        @Size(max = 5000, message = "A descrição pode ter no máximo 5000 caracteres.") String descricao) {
}
