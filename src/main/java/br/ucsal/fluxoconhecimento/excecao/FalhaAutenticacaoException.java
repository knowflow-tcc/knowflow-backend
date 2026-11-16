package br.ucsal.fluxoconhecimento.excecao;

// Login recusado - HTTP 401
public class FalhaAutenticacaoException extends RuntimeException {

    public FalhaAutenticacaoException(String mensagem) {
        super(mensagem);
    }
}
