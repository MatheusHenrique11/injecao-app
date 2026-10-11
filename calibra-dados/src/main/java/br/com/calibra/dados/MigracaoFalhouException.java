package br.com.calibra.dados;

/** A atualização do banco falhou; a mensagem diz se os dados voltaram ao backup. */
public class MigracaoFalhouException extends ErroDados {

    private static final long serialVersionUID = 1L;

    public MigracaoFalhouException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
