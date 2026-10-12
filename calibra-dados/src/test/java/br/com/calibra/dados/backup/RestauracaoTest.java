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
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.config.Chave;

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
        InfoBackup backup = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);
        base.configuracao.gravar(Chave.OFICINA, DEPOIS);

        Manifesto manifesto = base.restauracao().conferir(backup.arquivo());

        assertEquals(backup.manifesto(), manifesto);
        assertEquals(DEPOIS, base.configuracao.ler(Chave.OFICINA));
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

    @Test
    void anexoComCaminhoMaliciosoERecusado() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta.resolve("dados"));
        InfoBackup backup = base.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);
        Path malicioso = copiarComEntradaExtra(backup.arquivo(), pasta.resolve("malicioso.zip"),
                "anexos/../../fora.txt");
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
