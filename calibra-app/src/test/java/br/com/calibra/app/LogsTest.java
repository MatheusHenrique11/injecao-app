package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogsTest {

    @Test
    void limitesDaSpec() {
        assertEquals(5 * 1024 * 1024, Logs.LIMITE_BYTES);
        assertEquals(10, Logs.ARQUIVOS);
    }

    @Test
    void gravaMensagemEErroNoArquivo(@TempDir Path pasta) throws Exception {
        FileHandler arquivo = Logs.criarArquivo(pasta);
        try {
            LogRecord registro = new LogRecord(Level.SEVERE, "Falhou ao abrir ação");
            registro.setLoggerName("br.com.calibra.app.Teste");
            registro.setThrown(new IllegalStateException("detalhe do erro"));
            arquivo.publish(registro);
        } finally {
            arquivo.close();
        }
        String texto = Files.readString(pasta.resolve("calibra-0.log"), StandardCharsets.UTF_8);
        assertTrue(texto.contains("SEVERE [Teste] Falhou ao abrir ação"), texto);
        assertTrue(texto.contains("java.lang.IllegalStateException: detalhe do erro"), texto);
    }
}
