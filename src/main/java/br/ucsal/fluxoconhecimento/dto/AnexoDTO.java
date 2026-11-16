package br.ucsal.fluxoconhecimento.dto;

import java.time.LocalDateTime;

// Anexo mostrado na tela (RF16); o arquivo é baixado em /api/anexos/{id}
public record AnexoDTO(
        Long id,
        String nomeArquivo,
        String tipoConteudo,
        long tamanho,
        boolean imagem,
        LocalDateTime dataHora,
        String autorNome,
        boolean podeArquivar) {
}
