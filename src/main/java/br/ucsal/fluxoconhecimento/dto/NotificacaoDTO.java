package br.ucsal.fluxoconhecimento.dto;

import java.time.LocalDateTime;

// Aviso ao autor de uma solução (RF21)
public record NotificacaoDTO(
        Long id,
        String mensagem,
        boolean lida,
        LocalDateTime dataHora,
        Long idProblema) {
}
