package br.ucsal.fluxoconhecimento.dto;

import java.time.LocalDateTime;
import java.util.List;

// Problema na lista de resultados da busca (RF08, RF19)
public record ResultadoBuscaDTO(
        Long id,
        String titulo,
        String resumo,
        String status,
        String statusRotulo,
        LocalDateTime dataRegistro,
        String departamentoNome,
        String categoriaCaminho,
        String autorNome,
        int totalSolucoes,
        boolean temSolucaoValidada,
        List<String> palavrasChave,
        Integer semelhanca,
        List<String> termosEmComum,
        boolean favorito) {
}
