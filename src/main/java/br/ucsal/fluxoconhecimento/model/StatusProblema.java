package br.ucsal.fluxoconhecimento.model;

// Situação do problema (RF23): aberto ou resolvido
public enum StatusProblema {
    ABERTO("Aberto"),
    RESOLVIDO("Resolvido");

    private final String rotulo;

    StatusProblema(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
