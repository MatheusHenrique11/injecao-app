package br.com.calibra.dados;

/**
 * Operação que uma regra do cadastro não deixa fazer (ex.: apagar veículo com projeto). A mensagem diz o
 * motivo e o que fazer no lugar.
 */
public class OperacaoRecusadaException extends ErroDados {

    private static final long serialVersionUID = 1L;

    public OperacaoRecusadaException(String mensagem) {
        super(mensagem);
    }
}
