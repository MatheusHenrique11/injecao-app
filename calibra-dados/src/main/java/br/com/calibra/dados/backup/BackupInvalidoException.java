package br.com.calibra.dados.backup;

import br.com.calibra.dados.ErroDados;

/** O arquivo escolhido não pode ser restaurado (não é backup, está corrompido ou é de versão mais nova). */
public class BackupInvalidoException extends ErroDados {

    private static final long serialVersionUID = 1L;

    public BackupInvalidoException(String mensagem) {
        super(mensagem);
    }

    public BackupInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
