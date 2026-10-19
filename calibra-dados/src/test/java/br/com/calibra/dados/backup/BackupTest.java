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
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.config.Chave;

class BackupTest {

    @TempDir
    Path pasta;

    /** Formato 2 (fase 3B): o zip leva o banco e o manifesto; os anexos ficam uma vez na pasta anexos-dos-backups. */
    @Test
    void zipTemBancoEManifestoEOsAnexosFicamNaPasta() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        base.configuracao.gravar(Chave.OFICINA, new DadosOficina("Oficina", "", "", "", "", "logo.png"));
        Files.writeString(base.pastas.anexos().resolve("logo.png"), "imagem");
        Files.createDirectories(base.pastas.anexos().resolve("turbos"));
        Files.writeString(base.pastas.anexos().resolve("turbos").resolve("mapa.png"), "mapa");

        InfoBackup info = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);

        // O relógio de teste anda 1 s por leitura: 18:30:00 foi a gravação da oficina; 18:30:01, o backup.
        assertEquals("calibra-backup-2026-10-11-183001.zip", info.arquivo().getFileName().toString());
        Manifesto m = info.manifesto();
        assertEquals(2, m.formato(), "formato 2: os anexos ficam fora do zip");
        assertEquals(BaseDeTeste.VERSAO_APP, m.versaoApp());
        assertEquals(BaseDeTeste.VERSAO_BANCO, m.versaoBanco());
        assertEquals(LocalDateTime.of(2026, 10, 11, 18, 30, 1), m.criadoEm());
        assertEquals(MotivoBackup.MANUAL, m.motivo());
        assertEquals(2, m.anexos());
        assertEquals(List.of(new AnexoNoBackup("logo.png", sha256("imagem"), 6),
                new AnexoNoBackup("turbos/mapa.png", sha256("mapa"), 4)), m.arquivos());
        try (ZipFile zip = new ZipFile(info.arquivo().toFile())) {
            assertEquals(List.of("calibra.db", "manifesto.json"), nomes(zip), "nenhum anexo dentro do zip");
            assertTrue(ler(zip, "manifesto.json").contains("\"versaoBanco\" : \"" + BaseDeTeste.VERSAO_BANCO + "\""),
                    "manifesto legível");
        }
        Path guardados = pasta.resolve("backups").resolve("anexos-dos-backups");
        assertEquals("imagem", Files.readString(guardados.resolve(sha256("imagem"))));
        assertEquals("mapa", Files.readString(guardados.resolve(sha256("mapa"))));
        assertEquals(2, quantos(guardados), "um arquivo por conteúdo, sem cópia pela metade");
        assertFalse(Files.exists(info.arquivo().resolveSibling(info.arquivo().getFileName() + ".parcial")));
        try (var restos = Files.list(base.pastas.temporaria())) {
            assertEquals(0, restos.count(), "a cópia temporária do banco foi apagada");
        }
    }

    /**
     * Uma foto que não mudou não é copiada de novo: o segundo backup não mexe na pasta, e o zip dele leva o banco, não a
     * foto (com o formato 1, cada zip levava a foto inteira).
     */
    @Test
    void segundoBackupNaoCopiaOsAnexosDeNovo() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        byte[] foto = new byte[300_000];
        new Random(7).nextBytes(foto);
        Files.write(base.pastas.anexos().resolve("foto.jpg"), foto);
        Path destino = pasta.resolve("backups");
        InfoBackup primeiro = base.backup.fazer(destino, MotivoBackup.MANUAL);
        Path guardado = destino.resolve("anexos-dos-backups").resolve(primeiro.manifesto().arquivos().getFirst()
                .sha256());
        Files.setLastModifiedTime(guardado, FileTime.fromMillis(0));

        InfoBackup segundo = base.backup.fazer(destino, MotivoBackup.MANUAL);

        assertEquals(primeiro.manifesto().arquivos(), segundo.manifesto().arquivos());
        assertEquals(FileTime.fromMillis(0), Files.getLastModifiedTime(guardado), "não foi copiada de novo");
        assertEquals(1, quantos(destino.resolve("anexos-dos-backups")));
        assertTrue(segundo.tamanhoBytes() < 150_000, "o zip leva o banco, não a foto: " + segundo.tamanhoBytes());
        assertEquals(300_000, Backup.tamanhoDosAnexos(destino));
    }

    /**
     * Revisão final: a cópia pela metade de um anexo ({@code .parcial}) não vai no backup, e um anexo removido no meio
     * do backup fica de fora, em vez de derrubar o backup inteiro.
     */
    @Test
    void anexoPelaMetadeOuRemovidoNaoDerrubaOBackup() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Path veiculo = Files.createDirectories(base.pastas.anexos().resolve("veiculos").resolve("1"));
        Files.writeString(veiculo.resolve("0123456789abcdef.jpg"), "foto");
        Files.writeString(veiculo.resolve("fedcba9876543210.jpg.parcial"), "foto pela me");

        InfoBackup info = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);

        assertEquals(List.of("veiculos/1/0123456789abcdef.jpg"),
                info.manifesto().arquivos().stream().map(AnexoNoBackup::caminho).toList(), "sem a cópia pela metade");
        assertEquals(1, info.manifesto().anexos(), "o manifesto conta só o que foi junto");
        AnexosDosBackups guardados = AnexosDosBackups.na(pasta.resolve("backups"));
        assertEquals(Optional.empty(), guardados.guardar(veiculo.resolve("sumiu.jpg")),
                "o que sumiu no meio fica de fora, sem erro");
    }

    /**
     * Depois de ficar com os 30 mais recentes, sai da pasta anexos-dos-backups o que nenhum backup restante usa (e a
     * cópia pela metade); o que algum backup usa fica.
     */
    @Test
    void limpezaTiraOQueNenhumBackupUsa() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Path destino = pasta.resolve("backups");
        Path antiga = Files.writeString(base.pastas.anexos().resolve("antiga.jpg"), "foto antiga");
        base.backup.fazer(destino, MotivoBackup.AUTOMATICO);
        Files.delete(antiga);
        Files.writeString(base.pastas.anexos().resolve("nova.jpg"), "foto nova");
        Path guardados = destino.resolve("anexos-dos-backups");
        Files.writeString(guardados.resolve("copia-123.parcial"), "pela metade");

        base.backup.fazer(destino, MotivoBackup.AUTOMATICO);
        assertTrue(Files.exists(guardados.resolve(sha256("foto antiga"))), "o primeiro backup ainda usa a foto");
        assertFalse(Files.exists(guardados.resolve("copia-123.parcial")), "a cópia pela metade sai");
        for (int i = 1; i < Backup.MANTER; i++) {
            base.backup.fazer(destino, MotivoBackup.AUTOMATICO);
        }

        assertEquals(Backup.MANTER, Backup.listar(destino).size(), "o primeiro backup saiu");
        assertFalse(Files.exists(guardados.resolve(sha256("foto antiga"))), "nenhum backup restante usa a foto");
        assertTrue(Files.exists(guardados.resolve(sha256("foto nova"))));
    }

    /**
     * Evidência do bloco 3B.0 (em target/, para conferir): dois backups com fotos de celular fictícias. O zip fica com
     * o tamanho do banco, a pasta anexos-dos-backups cresce só com a foto nova, e o primeiro backup restaura igual.
     */
    @Test
    void geraEvidenciaParaConferencia() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Path veiculo = Files.createDirectories(base.pastas.anexos().resolve("veiculos").resolve("1"));
        Random sorteio = new Random(11);
        for (int i = 1; i <= 4; i++) {
            Files.write(veiculo.resolve(String.format("%016x.jpg", i)), fotoAoAcaso(sorteio));
        }
        Path destino = pasta.resolve("backups");
        InfoBackup primeiro = base.backup.fazer(destino, MotivoBackup.MANUAL);
        long depoisDoPrimeiro = Backup.tamanhoDosAnexos(destino);
        Files.write(veiculo.resolve(String.format("%016x.jpg", 5)), fotoAoAcaso(sorteio));
        InfoBackup segundo = base.backup.fazer(destino, MotivoBackup.MANUAL);
        long depoisDoSegundo = Backup.tamanhoDosAnexos(destino);

        base.restauracao().restaurar(primeiro.arquivo());

        assertEquals(6_000_000, depoisDoPrimeiro, "as 4 fotos, uma vez");
        assertEquals(7_500_000, depoisDoSegundo, "só a foto nova entrou");
        assertTrue(segundo.tamanhoBytes() < 150_000, "o zip leva o banco: " + segundo.tamanhoBytes());
        assertEquals(4, quantos(veiculo), "o primeiro backup volta com as 4 fotos");
        Path alvo = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target");
        Files.createDirectories(alvo);
        Files.write(alvo.resolve("evidencia-backup-formato-2.txt"), List.of(
                "Bloco 3B.0: backup que guarda cada anexo uma vez (fotos fictícias de 1,5 MB)",
                "1º backup, 4 fotos: o zip tem " + NumeroBr.tamanhoArquivo(primeiro.tamanhoBytes())
                        + "; a pasta anexos-dos-backups, " + NumeroBr.tamanhoArquivo(depoisDoPrimeiro),
                "2º backup, 5 fotos: o zip tem " + NumeroBr.tamanhoArquivo(segundo.tamanhoBytes())
                        + "; a pasta anexos-dos-backups, " + NumeroBr.tamanhoArquivo(depoisDoSegundo),
                "No formato 1, cada um dos 30 backups levaria as fotos todas: "
                        + NumeroBr.tamanhoArquivo(depoisDoSegundo) + " a cada backup.",
                "Restaurar o 1º backup: as 4 fotos voltam, conferidas pelo SHA-256; a 5ª sai."),
                StandardCharsets.UTF_8);
    }

    private static byte[] fotoAoAcaso(Random sorteio) {
        byte[] foto = new byte[1_500_000];
        sorteio.nextBytes(foto);
        return foto;
    }

    /** Com um backup ilegível na pasta, a limpeza não apaga nada: ele pode usar algum arquivo dela. */
    @Test
    void limpezaEsperaQuandoUmBackupNaoPodeSerLido() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Path destino = pasta.resolve("backups");
        base.backup.fazer(destino, MotivoBackup.MANUAL);
        Path guardados = Files.createDirectories(destino.resolve("anexos-dos-backups"));
        String semUso = "f".repeat(64);
        Files.writeString(guardados.resolve(semUso), "de um backup que não dá para ler");
        Path ilegivel = Files.writeString(destino.resolve("calibra-backup-2026-01-01-000000.zip"), "não é zip");

        base.backup.fazer(destino, MotivoBackup.MANUAL);
        assertTrue(Files.exists(guardados.resolve(semUso)), "com um backup ilegível, nada sai");

        Files.delete(ilegivel);
        base.backup.fazer(destino, MotivoBackup.MANUAL);
        assertFalse(Files.exists(guardados.resolve(semUso)), "sem ele, a limpeza volta");
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

    /**
     * PC sem internet com o relógio atrasado (bateria do relógio fraca): o backup novo tem data anterior à dos 30 da
     * pasta. Ele fica, e sai o mais antigo dos outros; antes, o novo era apagado na hora e todo backup falhava.
     */
    @Test
    void relogioAtrasadoNaoApagaOBackupNovo() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Path destino = pasta.resolve("backups");
        InfoBackup maisAntigo = base.backup.fazer(destino, MotivoBackup.AUTOMATICO);
        for (int i = 1; i < Backup.MANTER; i++) {
            base.backup.fazer(destino, MotivoBackup.AUTOMATICO);
        }
        base.relogio.avancarSegundos(-30L * 24 * 3600);

        InfoBackup novo = base.backup.fazer(destino, MotivoBackup.MANUAL);

        assertTrue(Files.exists(novo.arquivo()), "o backup que acabou de ser feito fica");
        assertEquals(Backup.MANTER, Backup.listar(destino).size());
        assertFalse(Files.exists(maisAntigo.arquivo()), "sai o mais antigo dos outros");
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

    static String sha256(String texto) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(texto.getBytes(StandardCharsets.UTF_8)));
    }

    private static long quantos(Path pasta) throws Exception {
        try (Stream<Path> itens = Files.list(pasta)) {
            return itens.count();
        }
    }

    private static List<String> nomes(ZipFile zip) {
        return zip.stream().map(ZipEntry::getName).sorted().toList();
    }

    private static String ler(ZipFile zip, String nome) throws Exception {
        ZipEntry entrada = zip.getEntry(nome);
        assertNotNull(entrada, nome);
        try (InputStream dados = zip.getInputStream(entrada)) {
            return new String(dados.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
