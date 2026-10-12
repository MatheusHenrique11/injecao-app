package br.com.calibra.dados.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.config.Chave;

class BackupTest {

    @TempDir
    Path pasta;

    @Test
    void zipTemBancoAnexosEManifesto() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        base.configuracao.gravar(Chave.OFICINA, new DadosOficina("Oficina", "", "", "", "", "logo.png"));
        Files.writeString(base.pastas.anexos().resolve("logo.png"), "imagem");
        Files.createDirectories(base.pastas.anexos().resolve("turbos"));
        Files.writeString(base.pastas.anexos().resolve("turbos").resolve("mapa.png"), "mapa");

        InfoBackup info = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);

        // O relógio de teste anda 1 s por leitura: 18:30:00 foi a gravação da oficina; 18:30:01, o backup.
        assertEquals("calibra-backup-2026-10-11-183001.zip", info.arquivo().getFileName().toString());
        Manifesto m = info.manifesto();
        assertEquals(Manifesto.FORMATO, m.formato());
        assertEquals(BaseDeTeste.VERSAO_APP, m.versaoApp());
        assertEquals(BaseDeTeste.VERSAO_BANCO, m.versaoBanco());
        assertEquals(LocalDateTime.of(2026, 10, 11, 18, 30, 1), m.criadoEm());
        assertEquals(MotivoBackup.MANUAL, m.motivo());
        assertEquals(2, m.anexos());
        try (ZipFile zip = new ZipFile(info.arquivo().toFile())) {
            assertNotNull(zip.getEntry("calibra.db"));
            assertNotNull(zip.getEntry("manifesto.json"));
            assertEquals("imagem", ler(zip, "anexos/logo.png"));
            assertEquals("mapa", ler(zip, "anexos/turbos/mapa.png"));
            assertTrue(ler(zip, "manifesto.json").contains("\"versaoBanco\" : \"" + BaseDeTeste.VERSAO_BANCO + "\""),
                    "manifesto legível");
        }
        assertFalse(Files.exists(info.arquivo().resolveSibling(info.arquivo().getFileName() + ".parcial")));
        try (var restos = Files.list(base.pastas.temporaria())) {
            assertEquals(0, restos.count(), "a cópia temporária do banco foi apagada");
        }
    }

    @Test
    void listaDoMaisNovoParaOMaisAntigo() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Path destino = pasta.resolve("backups");
        InfoBackup primeiro = base.backup.fazer(destino, MotivoBackup.AUTOMATICO);
        base.relogio.avancarSegundos(3600);
        InfoBackup segundo = base.backup.fazer(destino, MotivoBackup.AO_FECHAR);
        Files.writeString(destino.resolve("anotacoes.txt"), "arquivo do usuário: não é backup");

        List<InfoBackup> lista = Backup.listar(destino);
        assertEquals(List.of(segundo.arquivo(), primeiro.arquivo()), lista.stream().map(InfoBackup::arquivo).toList());
        assertEquals(segundo.arquivo(), Backup.maisRecente(destino).orElseThrow().arquivo());
    }

    /** SPEC 10: manter os 30 mais recentes. Arquivos que não são backup ficam intactos. */
    @Test
    void mantemOs30MaisRecentes() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Path destino = pasta.resolve("backups");
        Files.createDirectories(destino);
        Files.writeString(destino.resolve("anotacoes.txt"), "não apagar");
        InfoBackup primeiro = base.backup.fazer(destino, MotivoBackup.AUTOMATICO);
        InfoBackup segundo = base.backup.fazer(destino, MotivoBackup.AUTOMATICO);
        for (int i = 0; i < 30; i++) {
            base.backup.fazer(destino, MotivoBackup.AUTOMATICO);
        }
        List<InfoBackup> lista = Backup.listar(destino);
        assertEquals(Backup.MANTER, lista.size());
        assertFalse(Files.exists(primeiro.arquivo()), "o mais antigo saiu");
        assertFalse(Files.exists(segundo.arquivo()));
        assertTrue(Files.exists(destino.resolve("anotacoes.txt")));
    }

    @Test
    void doisBackupsNoMesmoSegundoNaoSeSobrepoem() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Path destino = pasta.resolve("backups");
        Files.createDirectories(destino);
        // Um arquivo já ocupa o nome do segundo atual do relógio.
        Files.copy(base.backup.fazer(destino, MotivoBackup.MANUAL).arquivo(),
                destino.resolve("calibra-backup-2026-10-11-183001.zip"));
        InfoBackup novo = base.backup.fazer(destino, MotivoBackup.MANUAL);
        assertEquals("calibra-backup-2026-10-11-183001-1.zip", novo.arquivo().getFileName().toString());
        assertEquals(novo.arquivo(), Backup.listar(destino).getFirst().arquivo(), "o contador ordena depois");
    }

    @Test
    void arquivoQueNaoEBackupFicaForaDaLista() throws Exception {
        Path destino = Files.createDirectories(pasta.resolve("backups"));
        Files.writeString(destino.resolve("calibra-backup-2026-10-11-183000.zip"), "não é zip");
        assertTrue(Backup.listar(destino).isEmpty());
        assertTrue(Backup.lerInfo(destino.resolve("calibra-backup-2026-10-11-183000.zip")).isEmpty());
    }

    @Test
    void pastaSemPermissaoDaMensagemClara() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Path arquivoNoLugarDaPasta = Files.writeString(pasta.resolve("nao-e-pasta"), "x");
        ErroDados erro = assertThrows(ErroDados.class,
                () -> base.backup.fazer(arquivoNoLugarDaPasta, MotivoBackup.MANUAL));
        assertTrue(erro.getMessage().startsWith("Não foi possível fazer o backup"), erro.getMessage());
    }

    private static String ler(ZipFile zip, String nome) throws Exception {
        ZipEntry entrada = zip.getEntry(nome);
        assertNotNull(entrada, nome);
        try (InputStream dados = zip.getInputStream(entrada)) {
            return new String(dados.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
