package br.ucsal.fluxoconhecimento.dto;

import java.util.List;

// Problema semelhante sugerido no cadastro (RF09)
public record SemelhanteDTO(
        Long id,
        String titulo,
        String status,
        String statusRotulo,
        String categoriaCaminho,
        int semelhanca,
        List<String> termosEmComum,
        String solucaoValidada) {
}
