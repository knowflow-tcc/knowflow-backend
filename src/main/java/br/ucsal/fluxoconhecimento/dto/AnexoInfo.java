package br.ucsal.fluxoconhecimento.dto;

import java.time.LocalDateTime;

// Dados de um anexo lidos do banco sem o conteúdo do arquivo (RF16)
public record AnexoInfo(
        Long id,
        String nomeArquivo,
        String tipoConteudo,
        long tamanho,
        LocalDateTime dataHora,
        Long idAutor,
        String autorNome,
        Long idProblema,
        Long idSolucao) {
}
