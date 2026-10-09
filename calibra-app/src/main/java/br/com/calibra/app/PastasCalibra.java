package br.com.calibra.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Pastas do app em %LOCALAPPDATA%\Calibra (SPEC 5), nunca na pasta de instalação. */
final class PastasCalibra {

    private PastasCalibra() {
    }

    static Path base() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path raiz = localAppData != null ? Path.of(localAppData) : Path.of(System.getProperty("user.home"));
        return raiz.resolve("Calibra");
    }

    /**
     * O PDFBox guarda um cache das fontes do sistema. Sem esta configuração, ele grava na pasta do
     * usuário; aqui ele vai para %LOCALAPPDATA%\Calibra\cache.
     */
    static void prepararCacheDeFontes() {
        if (System.getProperty("pdfbox.fontcache") != null) {
            return;
        }
        Path cache = base().resolve("cache");
        try {
            Files.createDirectories(cache);
            System.setProperty("pdfbox.fontcache", cache.toString());
        } catch (IOException erro) {
            System.getLogger(PastasCalibra.class.getName()).log(System.Logger.Level.WARNING,
                    "Não foi possível criar a pasta de cache " + cache, erro);
        }
    }
}
