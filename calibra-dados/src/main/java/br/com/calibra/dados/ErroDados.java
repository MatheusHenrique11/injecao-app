package br.com.calibra.dados;

/** Falha ao ler ou gravar dados (banco, backup, anexos), com mensagem em português. */
public class ErroDados extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ErroDados(String mensagem) {
        super(mensagem);
    }

    public ErroDados(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
