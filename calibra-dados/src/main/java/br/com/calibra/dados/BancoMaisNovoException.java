package br.com.calibra.dados;

/** O banco foi gravado por uma versão mais nova do Calibra; este app não mexe nele. */
public class BancoMaisNovoException extends ErroDados {

    private static final long serialVersionUID = 1L;

    public BancoMaisNovoException(String mensagem) {
        super(mensagem);
    }
}
