package br.ucsal.fluxoconhecimento.dto;

import java.time.LocalDate;

import br.ucsal.fluxoconhecimento.model.StatusProblema;

// Filtros da busca (RF08 e RF19)
public record FiltroBuscaDTO(
        String texto,
        Long categoriaId,
        Long departamentoId,
        StatusProblema status,
        LocalDate de,
        LocalDate ate,
        String ordem,
        boolean somenteFavoritos) {
}
