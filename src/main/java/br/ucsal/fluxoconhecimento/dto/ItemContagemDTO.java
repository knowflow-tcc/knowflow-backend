package br.ucsal.fluxoconhecimento.dto;

// Item de ranking do painel: rótulo e quantidade
public record ItemContagemDTO(String rotulo, long quantidade, Long id) {

    public ItemContagemDTO(String rotulo, long quantidade) {
        this(rotulo, quantidade, null);
    }
}
