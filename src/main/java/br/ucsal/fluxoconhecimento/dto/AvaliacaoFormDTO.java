package br.ucsal.fluxoconhecimento.dto;

import jakarta.validation.constraints.NotNull;

// RF18 - {"ajudou": true} ou {"ajudou": false}
public record AvaliacaoFormDTO(
        @NotNull(message = "Informe se a solução ajudou (true) ou não (false).")
        Boolean ajudou) {
}
