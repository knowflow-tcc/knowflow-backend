package br.ucsal.fluxoconhecimento.model;

// Ações registradas no histórico (RN10, RF11)
public enum AcaoHistorico {
    INCLUSAO("Inclusão"),
    ALTERACAO("Alteração"),
    ARQUIVAMENTO("Arquivamento"),
    VALIDACAO("Validação"),
    DESATIVACAO("Desativação"),
    REATIVACAO("Reativação"),
    ANONIMIZACAO("Anonimização");

    private final String rotulo;

    AcaoHistorico(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
