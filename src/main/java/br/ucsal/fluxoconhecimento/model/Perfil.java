package br.ucsal.fluxoconhecimento.model;

// Perfis de usuário (RN03: um perfil por usuário)
public enum Perfil {
    COLABORADOR("Colaborador"),
    GESTOR("Gestor"),
    ADMINISTRADOR("Administrador");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
