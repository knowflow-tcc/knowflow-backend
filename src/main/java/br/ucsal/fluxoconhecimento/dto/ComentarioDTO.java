package br.ucsal.fluxoconhecimento.dto;

import java.time.LocalDateTime;

// Comentário de uma solução (RF17)
public record ComentarioDTO(
        Long id,
        String texto,
        LocalDateTime dataHora,
        String autorNome,
        boolean podeArquivar) {
}
