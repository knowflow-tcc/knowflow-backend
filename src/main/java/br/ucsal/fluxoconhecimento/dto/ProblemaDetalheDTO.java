package br.ucsal.fluxoconhecimento.dto;

import java.time.LocalDateTime;
import java.util.List;

// Detalhes do problema (RF20)
public record ProblemaDetalheDTO(
        Long id,
        String titulo,
        String descricao,
        String status,
        String statusRotulo,
        boolean ativo,
        LocalDateTime dataRegistro,
        String autorNome,
        Long departamentoId,
        String departamentoNome,
        Long categoriaId,
        List<String> categoriaCaminho,
        List<String> palavrasChave,
        List<CausaDTO> causas,
        List<SolucaoDTO> solucoes,
        List<HistoricoDTO> historico,
        List<AnexoDTO> anexos,
        boolean favorito,
        boolean podeEditar,
        boolean podeRegistrarItens) {
}
