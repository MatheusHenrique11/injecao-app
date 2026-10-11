package br.com.calibra.dados.config;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import br.com.calibra.dados.PastasDados;

/**
 * Onde ficam os backups (SPEC 10: pasta configurável, como um HD externo ou pen drive).
 *
 * @param pasta pasta escolhida, ou null para a pasta padrão (%LOCALAPPDATA%\Calibra\backups)
 */
public record ConfiguracaoBackup(String pasta) {

    public ConfiguracaoBackup {
        pasta = pasta == null || pasta.isBlank() ? null : pasta.strip();
    }

    public static ConfiguracaoBackup padrao() {
        return new ConfiguracaoBackup(null);
    }

    public boolean usaPastaPadrao() {
        return pasta == null;
    }

    /** A pasta escolhida, se ela estiver acessível agora (o pen drive pode estar fora). */
    public boolean pastaEscolhidaDisponivel() {
        if (pasta == null) {
            return false;
        }
        try {
            return Files.isDirectory(Path.of(pasta));
        } catch (InvalidPathException erro) {
            return false;
        }
    }

    /** A pasta que vale agora: a escolhida, se estiver acessível; senão, a padrão. */
    public Path pastaEfetiva(PastasDados pastas) {
        return pastaEscolhidaDisponivel() ? Path.of(pasta) : pastas.backupsPadrao();
    }
}
