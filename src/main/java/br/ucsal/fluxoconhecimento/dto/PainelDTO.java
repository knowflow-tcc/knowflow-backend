package br.ucsal.fluxoconhecimento.dto;

import java.util.List;

// Painel de indicadores (RF13)
public record PainelDTO(
        Long departamentoId,
        String departamentoNome,
        long totalProblemas,
        long problemasAbertos,
        long problemasResolvidos,
        int taxaResolucao,
        long totalSolucoes,
        long solucoesValidadas,
        List<ItemContagemDTO> problemasRecorrentes,
        List<ItemContagemDTO> categoriasComMaisProblemas,
        List<ItemContagemDTO> palavrasChaveMaisUsadas,
        List<ItemContagemDTO> evolucaoMensal,
        List<ItemContagemDTO> abertosPorDepartamento,
        List<ItemContagemDTO> solucoesMaisUteis) {
}
