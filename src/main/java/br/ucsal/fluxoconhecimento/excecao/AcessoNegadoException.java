package br.ucsal.fluxoconhecimento.excecao;

// Usuário logado, mas sem permissão sobre este registro (RN06)
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
