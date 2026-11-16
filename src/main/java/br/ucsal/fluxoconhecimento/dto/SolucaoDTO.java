package br.ucsal.fluxoconhecimento.dto;

import java.time.LocalDateTime;
import java.util.List;

// Solução mostrada nos detalhes do problema (RF06)
public record SolucaoDTO(
        Long id,
        String descricao,
        boolean validada,
        LocalDateTime dataRegistro,
        String autorNome,
        boolean podeEditar,
        boolean podeValidar,
        long totalAjudou,
        long totalNaoAjudou,
        Boolean minhaAvaliacao,
        boolean podeAvaliar,
        boolean podeComentar,
        List<ComentarioDTO> comentarios,
        List<AnexoDTO> anexos) {
}
