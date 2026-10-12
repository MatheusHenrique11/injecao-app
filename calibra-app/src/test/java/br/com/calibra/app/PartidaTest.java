package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.config.Chave;
import br.com.calibra.dados.config.ConfiguracaoBackup;

class PartidaTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    @Test
    void primeiraAberturaCriaPastasEBancoSemBackup(@TempDir Path raiz) throws Exception {
        PastasDados pastas = new PastasDados(raiz.resolve("Calibra"));
        ContextoApp contexto = Partida.abrir(pastas, "0.2.0-teste", RELOGIO);

        for (Path pasta : new Path[] {pastas.anexos(), pastas.backupsPadrao(), pastas.logs(), pastas.cache()}) {
            assertTrue(Files.isDirectory(pasta), pasta.toString());
        }
        assertTrue(Files.isRegularFile(pastas.banco()));
        assertNull(contexto.migracao().versaoAntes(), "banco novo");
        assertTrue(contexto.migracao().migrou());
        assertNull(contexto.migracao().backupAntes(), "banco novo não precisa de backup");
        assertTrue(Backup.listar(pastas.backupsPadrao()).isEmpty());
        assertEquals("0.2.0-teste", contexto.versaoApp());
    }

    @Test
    void segundaAberturaNaoMigraEMantemOsDados(@TempDir Path raiz) throws Exception {
        PastasDados pastas = new PastasDados(raiz);
        ContextoApp primeira = Partida.abrir(pastas, "0.2.0-teste", RELOGIO);
        primeira.configuracao().gravar(Chave.BACKUP, new ConfiguracaoBackup("E:\\backup"));

        ContextoApp segunda = Partida.abrir(pastas, "0.2.0-teste", RELOGIO);
        assertFalse(segunda.migracao().migrou());
        assertEquals("E:\\backup", segunda.configuracao().ler(Chave.BACKUP).pasta());
    }

    @Test
    void cachesVaoParaAPastaDoCalibraSemTrocarOQueJaExiste(@TempDir Path raiz) {
        String antes = System.getProperty("javafx.cachedir");
        String pdfboxAntes = System.getProperty("pdfbox.fontcache");
        try {
            System.clearProperty("javafx.cachedir");
            PastasDados pastas = new PastasDados(raiz);
            Partida.prepararCaches(pastas);
            assertEquals(pastas.cache().toString(), System.getProperty("javafx.cachedir"));
            assertEquals(pdfboxAntes, System.getProperty("pdfbox.fontcache"), "o surefire já define este");
        } finally {
            if (antes == null) {
                System.clearProperty("javafx.cachedir");
            } else {
                System.setProperty("javafx.cachedir", antes);
            }
        }
    }

    @Test
    void pastaPadraoFicaNoLocalAppData() {
        String antes = System.getProperty(Partida.PROPRIEDADE_PASTA);
        try {
            System.clearProperty(Partida.PROPRIEDADE_PASTA);
            Path raiz = Partida.pastas().raiz();
            assertEquals("Calibra", raiz.getFileName().toString());
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                assertEquals(Path.of(localAppData).resolve("Calibra").toAbsolutePath().normalize(), raiz);
            }
        } finally {
            if (antes != null) {
                System.setProperty(Partida.PROPRIEDADE_PASTA, antes);
            }
        }
    }
}
