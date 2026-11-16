package br.ucsal.fluxoconhecimento.service;

import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;

// RNF09 - mínimo de 8 caracteres, com letras e números
public final class PoliticaSenha {

    public static final int TAMANHO_MINIMO = 8;
    public static final int TAMANHO_MAXIMO = 72;

    private PoliticaSenha() {
    }

    public static void validar(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO) {
            throw new RegraNegocioException("senha",
                    "A senha deve ter pelo menos " + TAMANHO_MINIMO + " caracteres, com letras e números.");
        }
        if (senha.length() > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("senha",
                    "A senha pode ter no máximo " + TAMANHO_MAXIMO + " caracteres.");
        }
        boolean temLetra = senha.chars().anyMatch(Character::isLetter);
        boolean temNumero = senha.chars().anyMatch(Character::isDigit);
        if (!temLetra || !temNumero) {
            throw new RegraNegocioException("senha", "A senha deve conter letras e números, por exemplo: suporte2026.");
        }
    }
}
