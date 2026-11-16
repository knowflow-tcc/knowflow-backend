package br.ucsal.fluxoconhecimento.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Entrada de registro/edição de problema (RF04, RF07)
public record ProblemaFormDTO(
        @NotBlank(message = "Informe o título do problema.")
        @Size(max = 150, message = "O título pode ter no máximo 150 caracteres.") String titulo,
        @NotBlank(message = "Descreva o problema.")
        @Size(max = 5000, message = "A descrição pode ter no máximo 5000 caracteres.") String descricao,
        @NotNull(message = "Escolha o departamento.") Long departamentoId,
        @NotNull(message = "Escolha a categoria.") Long categoriaId,
        @Size(max = 10, message = "Use no máximo 10 palavras-chave.") List<String> palavrasChave) {
}
