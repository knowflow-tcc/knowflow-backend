package br.ucsal.fluxoconhecimento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Novo comentário em uma solução (RF17)
public record ComentarioFormDTO(
        @NotBlank(message = "Escreva o comentário.")
        @Size(max = 2000, message = "O comentário pode ter no máximo 2000 caracteres.")
        String texto) {
}
