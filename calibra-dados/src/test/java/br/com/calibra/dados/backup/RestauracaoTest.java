package br.com.calibra.dados.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.node.ObjectNode;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.config.Chave;
import br.com.calibra.dados.json.Json;

class RestauracaoTest {

    private static final DadosOficina ANTES = new DadosOficina("Oficina Antiga", "", "", "", "", "logo.png");
    private static final DadosOficina DEPOIS = new DadosOficina("Oficina Nova", "", "", "", "", null);

    @TempDir
    Path pasta;

    /** Critério de pronto da fase 2: um backup é restaurado com sucesso. */
    @Test
    void restaurarDevolveBancoEAnexosDoBackup() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        base.configuracao.gravar(Chave.OFICINA, ANTES);
        Files.writeString(base.pastas.anexos().resolve("logo.png"), "logo antigo");
        InfoBackup backup = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);

        // Depois do backup: muda a oficina, apaga o logo e cria outro anexo.
        base.configuracao.gravar(Chave.OFICINA, DEPOIS);
        Files.delete(base.pastas.anexos().resolve("logo.png"));
        Files.writeString(base.pastas.anexos().resolve("novo.png"), "anexo novo");

        InfoBackup seguranca = base.restauracao().restaurar(backup.arquivo());

        assertEquals(ANTES, base.configuracao.ler(Chave.OFICINA));
        assertEquals("logo antigo", Files.readString(base.pastas.anexos().resolve("logo.png")));
        assertFalse(Files.exists(base.pastas.anexos().resolve("novo.png")));
        assertEquals(MotivoBackup.ANTES_DE_RESTAURAR, seguranca.manifesto().motivo());
        assertEquals(base.pastas.backupsPadrao(), seguranca.arquivo().getParent());

        // O backup de segurança guarda o que havia antes da restauração: dá para voltar.
        base.restauracao().restaurar(seguranca.arquivo());
        assertEquals(DEPOIS, base.configuracao.ler(Chave.OFICINA));
        assertTrue(Files.exists(base.pastas.anexos().resolve("novo.png")));
    }

    @Test
    void conferirNaoMexeNosDados() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        base.configuracao.gravar(Chave.OFICINA, ANTES);
        Files.writeString(base.pastas.anexos().resolve("logo.png"), "logo antigo");
        InfoBackup backup = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);
        base.configuracao.gravar(Chave.OFICINA, DEPOIS);
        Files.delete(base.pastas.anexos().resolve("logo.png"));

        Manifesto manifesto = base.restauracao().conferir(backup.arquivo());

        assertEquals(backup.manifesto(), manifesto);
        assertEquals(DEPOIS, base.configuracao.ler(Chave.OFICINA));
        assertFalse(Files.exists(base.pastas.anexos().resolve("logo.png")), "o anexo é conferido na pasta, sem voltar");
    }

    /** Fase 3B: o backup antigo (formato 1, com os anexos dentro do zip) continua restaurando, sem a pasta nova. */
    @Test
    void backupAntigoComOsAnexosNoZipContinuaRestaurando() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        base.configuracao.gravar(Chave.OFICINA, ANTES);
        Files.writeString(base.pastas.anexos().resolve("logo.png"), "logo antigo");
        Files.createDirectories(base.pastas.anexos().resolve("turbos"));
        Files.writeString(base.pastas.anexos().resolve("turbos").resolve("mapa.png"), "mapa");
        InfoBackup novo = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);
        Path antigo = paraFormato1(novo.arquivo(), Files.createDirectories(pasta.resolve("antigos"))
                .resolve("calibra-backup-2026-10-11-183000.zip"));
        apagarPasta(pasta.resolve("backups").resolve(AnexosDosBackups.PASTA));
        base.configuracao.gravar(Chave.OFICINA, DEPOIS);
        Files.delete(base.pastas.anexos().resolve("logo.png"));

        base.restauracao().restaurar(antigo);

        assertEquals(1, Backup.lerInfo(antigo).orElseThrow().manifesto().formato());
        assertEquals(ANTES, base.configuracao.ler(Chave.OFICINA));
        assertEquals("logo antigo", Files.readString(base.pastas.anexos().resolve("logo.png")));
        assertEquals("mapa", Files.readString(base.pastas.anexos().resolve("turbos").resolve("mapa.png")));
    }

    /**
     * Formato 2: sem o arquivo do anexo na pasta anexos-dos-backups (ex.: o zip copiado sozinho para um pen drive), a
     * restauração recusa, explica e não mexe em nada, nem faz o backup de segurança.
     */
    @Test
    void anexoQueFaltaNaPastaERecusadoENadaMuda() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        base.configuracao.gravar(Chave.OFICINA, ANTES);
        Files.writeString(base.pastas.anexos().resolve("logo.png"), "logo antigo");
        InfoBackup backup = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);
        Files.delete(AnexosDosBackups.doBackup(backup.arquivo())
                .arquivo(backup.manifesto().arquivos().getFirst().sha256()));
        base.configuracao.gravar(Chave.OFICINA, DEPOIS);

        BackupInvalidoException erro =
                assertThrows(BackupInvalidoException.class, () -> base.restauracao().restaurar(backup.arquivo()));

        assertTrue(erro.getMessage().startsWith("Falta 1 anexo deste backup na pasta "), erro.getMessage());
        assertTrue(erro.getMessage().endsWith("copie também a pasta anexos-dos-backups."), erro.getMessage());
        assertEquals(DEPOIS, base.configuracao.ler(Chave.OFICINA));
        assertEquals("logo antigo", Files.readString(base.pastas.anexos().resolve("logo.png")));
        assertTrue(Backup.listar(base.pastas.backupsPadrao()).isEmpty(), "nem o backup de segurança foi feito");
    }

    /** Formato 2: um arquivo da pasta anexos-dos-backups mexido (outro conteúdo com o mesmo nome) é recusado. */
    @Test
    void anexoAlteradoNaPastaERecusado() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Files.writeString(base.pastas.anexos().resolve("logo.png"), "logo antigo");
        InfoBackup backup = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);
        Files.writeString(AnexosDosBackups.doBackup(backup.arquivo())
                .arquivo(backup.manifesto().arquivos().getFirst().sha256()), "logo trocado");
        Files.writeString(base.pastas.anexos().resolve("logo.png"), "logo de agora");

        BackupInvalidoException erro =
                assertThrows(BackupInvalidoException.class, () -> base.restauracao().restaurar(backup.arquivo()));

        assertTrue(erro.getMessage().contains("o anexo logo.png não confere"), erro.getMessage());
        assertEquals("logo de agora", Files.readString(base.pastas.anexos().resolve("logo.png")));
        assertThrows(BackupInvalidoException.class, () -> base.restauracao().conferir(backup.arquivo()),
                "a conferência também acusa");
    }

    /** Formato 2: um manifesto mexido não leva a ler nem a gravar fora das pastas do Calibra. */
    @Test
    void manifestoComCaminhoOuImpressaoDigitalInvalidaERecusado() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Files.writeString(base.pastas.anexos().resolve("logo.png"), "logo");
        InfoBackup backup = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);
        AnexoNoBackup logo = backup.manifesto().arquivos().getFirst();

        Path fora = comArquivos(backup.arquivo(), "fora.zip", new AnexoNoBackup("../../fora.txt", logo.sha256(), 4));
        assertThrows(BackupInvalidoException.class, () -> base.restauracao().restaurar(fora));
        assertFalse(Files.exists(pasta.resolve("fora.txt")));
        assertFalse(Files.exists(pasta.resolve("dados").resolve("fora.txt")));

        Path absoluto = comArquivos(backup.arquivo(), "absoluto.zip",
                new AnexoNoBackup(pasta.resolve("absoluto.txt").toString(), logo.sha256(), 4));
        assertThrows(BackupInvalidoException.class, () -> base.restauracao().restaurar(absoluto));
        assertFalse(Files.exists(pasta.resolve("absoluto.txt")));

        Path sha = comArquivos(backup.arquivo(), "sha.zip", new AnexoNoBackup("logo.png", "../../calibra.db", 4));
        BackupInvalidoException erro =
                assertThrows(BackupInvalidoException.class, () -> base.restauracao().restaurar(sha));
        assertTrue(erro.getMessage().contains("impressão digital inválida"), erro.getMessage());
    }

    @Test
    void backupCorrompidoERecusadoENadaMuda() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        base.configuracao.gravar(Chave.OFICINA, ANTES);
        InfoBackup backup = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);
        base.configuracao.gravar(Chave.OFICINA, DEPOIS);
        Path corrompido = trocarBytesDoBanco(backup.arquivo(), pasta.resolve("corrompido.zip"));

        BackupInvalidoException erro =
                assertThrows(BackupInvalidoException.class, () -> base.restauracao().restaurar(corrompido));

        assertTrue(erro.getMessage().contains("corrompido"), erro.getMessage());
        assertEquals(DEPOIS, base.configuracao.ler(Chave.OFICINA));
        assertTrue(Backup.listar(base.pastas.backupsPadrao()).isEmpty(), "nem o backup de segurança foi feito");
    }

    @Test
    void arquivoQueNaoEBackupERecusado() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        Path texto = Files.writeString(pasta.resolve("planilha.zip"), "não é zip");
        assertThrows(BackupInvalidoException.class, () -> base.restauracao().restaurar(texto));

        Path zipSemManifesto = pasta.resolve("outro.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipSemManifesto))) {
            zip.putNextEntry(new ZipEntry("foto.jpg"));
            zip.write(1);
            zip.closeEntry();
        }
        BackupInvalidoException erro =
                assertThrows(BackupInvalidoException.class, () -> base.restauracao().restaurar(zipSemManifesto));
        assertTrue(erro.getMessage().contains("manifesto"), erro.getMessage());
        assertThrows(BackupInvalidoException.class, () -> base.restauracao().restaurar(pasta.resolve("sumiu.zip")));
    }

    @Test
    void backupDeVersaoMaisNovaERecusado() throws Exception {
        BaseDeTeste novo = BaseDeTeste.criada(pasta.resolve("app-novo"));
        Migracoes.migrar(novo.banco, novo.backup, Migracoes.LOCAL_PADRAO, BaseDeTeste.FUTURA_OK);
        InfoBackup backupNovo = novo.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);
        assertEquals(BaseDeTeste.VERSAO_FUTURA, backupNovo.manifesto().versaoBanco());

        BaseDeTeste antigo = BaseDeTeste.criada(pasta.resolve("app-antigo"));
        antigo.configuracao.gravar(Chave.OFICINA, ANTES);
        BackupInvalidoException erro = assertThrows(BackupInvalidoException.class,
                () -> antigo.restauracao().restaurar(backupNovo.arquivo()));

        assertTrue(erro.getMessage().contains("versão mais nova"), erro.getMessage());
        assertEquals(ANTES, antigo.configuracao.ler(Chave.OFICINA));
        assertEquals(BaseDeTeste.VERSAO_BANCO, antigo.banco.versaoDoEsquema());
    }

    /** Formato 1: um anexo dentro do zip com caminho para fora da pasta de anexos é recusado. */
    @Test
    void anexoComCaminhoMaliciosoERecusado() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        InfoBackup backup = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);
        Path antigo = paraFormato1(backup.arquivo(), pasta.resolve("antigo.zip"));
        Path malicioso = copiarComEntradaExtra(antigo, pasta.resolve("malicioso.zip"), "anexos/../../fora.txt");
        assertThrows(BackupInvalidoException.class, () -> base.restauracao().restaurar(malicioso));
        assertFalse(Files.exists(pasta.resolve("fora.txt")));
    }

    @Test
    void comparaVersoesDoBanco() {
        assertTrue(Restauracao.maisNova("2", "1"));
        assertTrue(Restauracao.maisNova("10", "9"), "compara como número, não como texto");
        assertFalse(Restauracao.maisNova("1", "1"));
        assertFalse(Restauracao.maisNova("1", "2"));
        assertFalse(Restauracao.maisNova(null, "1"));
        assertTrue(Restauracao.maisNova("1", null));
    }

    /** Copia o zip trocando um byte no meio do banco: o SHA-256 deixa de conferir. */
    private static Path trocarBytesDoBanco(Path origem, Path destino) throws Exception {
        try (ZipFile entrada = new ZipFile(origem.toFile());
                OutputStream arquivo = Files.newOutputStream(destino);
                ZipOutputStream saida = new ZipOutputStream(arquivo)) {
            Enumeration<? extends ZipEntry> itens = entrada.entries();
            while (itens.hasMoreElements()) {
                ZipEntry item = itens.nextElement();
                byte[] dados;
                try (InputStream leitura = entrada.getInputStream(item)) {
                    dados = leitura.readAllBytes();
                }
                if (item.getName().equals("calibra.db")) {
                    dados[dados.length / 2] ^= 0x55;
                }
                saida.putNextEntry(new ZipEntry(item.getName()));
                saida.write(dados);
                saida.closeEntry();
            }
        }
        return destino;
    }

    /**
     * O backup como a 0.3.0 fazia (formato 1): o banco e os anexos dentro do zip e o manifesto sem a lista
     * "arquivos". Os anexos vêm da pasta anexos-dos-backups do backup novo.
     */
    private static Path paraFormato1(Path novo, Path destino) throws Exception {
        Manifesto manifesto = Backup.lerInfo(novo).orElseThrow().manifesto();
        AnexosDosBackups guardados = AnexosDosBackups.doBackup(novo);
        ObjectNode antigo = Json.mapeador().valueToTree(manifesto);
        antigo.put("formato", Manifesto.FORMATO_ANEXOS_NO_ZIP);
        antigo.remove("arquivos");
        try (ZipFile entrada = new ZipFile(novo.toFile());
                ZipOutputStream saida = new ZipOutputStream(Files.newOutputStream(destino))) {
            saida.putNextEntry(new ZipEntry(Backup.ENTRADA_BANCO));
            try (InputStream banco = entrada.getInputStream(entrada.getEntry(Backup.ENTRADA_BANCO))) {
                banco.transferTo(saida);
            }
            saida.closeEntry();
            for (AnexoNoBackup anexo : manifesto.arquivos()) {
                saida.putNextEntry(new ZipEntry(Backup.PREFIXO_ANEXOS + anexo.caminho()));
                Files.copy(guardados.arquivo(anexo.sha256()), saida);
                saida.closeEntry();
            }
            saida.putNextEntry(new ZipEntry(Backup.ENTRADA_MANIFESTO));
            saida.write(Json.mapeador().writeValueAsBytes(antigo));
            saida.closeEntry();
        }
        return destino;
    }

    /** Uma cópia do backup, na mesma pasta, com outra lista de anexos no manifesto. */
    private static Path comArquivos(Path origem, String nome, AnexoNoBackup... arquivos) throws Exception {
        Manifesto m = Backup.lerInfo(origem).orElseThrow().manifesto();
        Manifesto mexido = new Manifesto(m.formato(), m.versaoApp(), m.versaoBanco(), m.criadoEm(), m.motivo(),
                m.sha256Banco(), m.tamanhoBanco(), arquivos.length, List.of(arquivos));
        Path destino = origem.resolveSibling(nome);
        try (ZipFile entrada = new ZipFile(origem.toFile());
                ZipOutputStream saida = new ZipOutputStream(Files.newOutputStream(destino))) {
            saida.putNextEntry(new ZipEntry(Backup.ENTRADA_BANCO));
            try (InputStream banco = entrada.getInputStream(entrada.getEntry(Backup.ENTRADA_BANCO))) {
                banco.transferTo(saida);
            }
            saida.closeEntry();
            saida.putNextEntry(new ZipEntry(Backup.ENTRADA_MANIFESTO));
            saida.write(Json.escreverFormatado(mexido).getBytes(StandardCharsets.UTF_8));
            saida.closeEntry();
        }
        return destino;
    }

    private static void apagarPasta(Path pasta) throws Exception {
        try (Stream<Path> itens = Files.walk(pasta)) {
            for (Path item : itens.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(item);
            }
        }
    }

    private static Path copiarComEntradaExtra(Path origem, Path destino, String nomeExtra) throws Exception {
        try (ZipFile entrada = new ZipFile(origem.toFile());
                ZipOutputStream saida = new ZipOutputStream(Files.newOutputStream(destino))) {
            Enumeration<? extends ZipEntry> itens = entrada.entries();
            while (itens.hasMoreElements()) {
                ZipEntry item = itens.nextElement();
                saida.putNextEntry(new ZipEntry(item.getName()));
                try (InputStream leitura = entrada.getInputStream(item)) {
                    leitura.transferTo(saida);
                }
                saida.closeEntry();
            }
            saida.putNextEntry(new ZipEntry(nomeExtra));
            saida.write("invasor".getBytes(StandardCharsets.UTF_8));
            saida.closeEntry();
        }
        return destino;
    }
}
