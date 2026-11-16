package br.ucsal.fluxoconhecimento.dto;

import br.ucsal.fluxoconhecimento.model.StatusProblema;
import jakarta.validation.constraints.NotNull;

// Entrada de PATCH /api/problemas/{id}/status (RF23)
public record StatusFormDTO(@NotNull(message = "Informe a situação: ABERTO ou RESOLVIDO.") StatusProblema status) {
}
