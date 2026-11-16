package br.ucsal.fluxoconhecimento.dto;

import java.time.LocalDateTime;

public record HistoricoDTO(
        Long id,
        String acao,
        String acaoRotulo,
        String tabelaAfetada,
        Long idRegistro,
        String usuarioNome,
        LocalDateTime dataHora) {
}
