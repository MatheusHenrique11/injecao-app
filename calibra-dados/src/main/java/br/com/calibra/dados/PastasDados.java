package br.com.calibra.dados;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Pastas de dados do Calibra (SPEC 5). No app, a raiz é %LOCALAPPDATA%\Calibra, nunca a pasta de
 * instalação; nos testes, uma pasta temporária.
 */
public record PastasDados(Path raiz) {

    public PastasDados {
        Objects.requireNonNull(raiz, "raiz");
        raiz = raiz.toAbsolutePath().normalize();
    }

    public Path banco() {
        return raiz.resolve("calibra.db");
    }

    public Path anexos() {
        return raiz.resolve("anexos");
    }

    /** Pasta de backup quando o usuário não escolhe outra. */
    public Path backupsPadrao() {
        return raiz.resolve("backups");
    }

    public Path logs() {
        return raiz.resolve("logs");
    }

    /** Cache das bibliotecas (fontes do PDFBox, nativos do JavaFX e do SQLite). */
    public Path cache() {
        return raiz.resolve("cache");
    }

    /** Arquivos de trabalho do backup e da restauração; ficam no mesmo disco do banco. */
    public Path temporaria() {
        return cache().resolve("tmp");
    }

    public void criar() throws IOException {
        for (Path pasta : List.of(raiz, anexos(), backupsPadrao(), logs(), cache(), temporaria())) {
            Files.createDirectories(pasta);
        }
    }
}
