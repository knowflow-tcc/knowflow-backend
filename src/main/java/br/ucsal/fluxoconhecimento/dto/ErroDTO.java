package br.ucsal.fluxoconhecimento.dto;

import java.time.LocalDateTime;
import java.util.Map;

// Resposta padrão de erro da API (RNF13)
public record ErroDTO(int status, String mensagem, Map<String, String> campos, LocalDateTime dataHora) {

    public ErroDTO(int status, String mensagem) {
        this(status, mensagem, Map.of(), LocalDateTime.now());
    }

    public ErroDTO(int status, String mensagem, Map<String, String> campos) {
        this(status, mensagem, campos, LocalDateTime.now());
    }
}
