package br.ucsal.fluxoconhecimento.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import br.ucsal.fluxoconhecimento.excecao.RegraNegocioException;

// RNF09 - senha com no mínimo 8 caracteres, contendo letras e números
class PoliticaSenhaTest {

    @Test
    void aceitaSenhaComOitoCaracteresLetrasENumeros() {
        assertDoesNotThrow(() -> PoliticaSenha.validar("suporte1"));
        assertDoesNotThrow(() -> PoliticaSenha.validar("Fluxo2026Conhecimento"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc123", "1234567"})
    void recusaSenhaCurta(String senha) {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class, () -> PoliticaSenha.validar(senha));
        assertEquals("senha", erro.getCampo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"somenteletras", "12345678"})
    void recusaSenhaSemLetrasOuSemNumeros(String senha) {
        assertThrows(RegraNegocioException.class, () -> PoliticaSenha.validar(senha));
    }

    @Test
    void recusaSenhaAcimaDoLimiteDoBcrypt() {
        assertThrows(RegraNegocioException.class, () -> PoliticaSenha.validar("a1".repeat(40)));
    }
}
