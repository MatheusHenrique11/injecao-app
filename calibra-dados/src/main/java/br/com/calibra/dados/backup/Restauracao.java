package br.com.calibra.dados.backup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.flywaydb.core.api.MigrationVersion;
import org.sqlite.SQLiteConfig;

import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.json.Json;

/**
 * Restaura um backup (SPEC 10): confere o arquivo inteiro antes, guarda os dados atuais num backup de
 * segurança, troca os arquivos e atualiza o banco se o backup for de uma versão mais antiga.
 *
 * <p>Ninguém pode estar usando o banco durante a troca: o app mostra a janela de progresso e não deixa
 * fazer outra coisa enquanto isso.
 */
public final class Restauracao {

    private static final Logger LOG = Logger.getLogger(Restauracao.class.getName());

    private final Backup backup;

    public Restauracao(Backup backup) {
        this.backup = Objects.requireNonNull(backup, "backup");
    }

    /**
     * Lê e confere o backup inteiro (impressão digital e integridade) sem mexer nos dados atuais. No formato 2, os
     * anexos são conferidos na própria pasta {@code anexos-dos-backups}, sem copiar.
     */
    public Manifesto conferir(Path zip) {
        Path pasta = pastaDeTrabalho();
        try {
            return extrairEConferir(zip, pasta, false);
        } finally {
            Arquivos.apagarArvore(pasta);
        }
    }

    /**
     * Troca os dados atuais pelos do backup.
     *
     * @return o backup de segurança feito com os dados que estavam antes
     * @throws BackupInvalidoException se o arquivo não puder ser restaurado (nada é alterado)
     */
    public InfoBackup restaurar(Path zip) {
        Path pasta = pastaDeTrabalho();
        try {
            Manifesto manifesto = extrairEConferir(zip, pasta, true);
            String atual = backup.banco().versaoDoEsquema();
            if (maisNova(manifesto.versaoBanco(), atual)) {
                throw new BackupInvalidoException("Este backup é de uma versão mais nova do GarageTech ("
                        + manifesto.versaoApp() + ", banco v" + manifesto.versaoBanco()
                        + "). Atualize o GarageTech antes de restaurar.");
            }
            InfoBackup seguranca = backup.fazer(backup.pastas().backupsPadrao(), MotivoBackup.ANTES_DE_RESTAURAR);
            trocar(pasta);
            LOG.info(() -> "Backup restaurado: " + zip + " (dados anteriores em " + seguranca.arquivo() + ")");
            Migracoes.migrar(backup.banco(), backup);
            return seguranca;
        } finally {
            Arquivos.apagarArvore(pasta);
        }
    }

    /** Volta um backup sem fazer backup de segurança e sem migrar: usado quando uma migração falha. */
    public void voltar(Path zip) {
        Path pasta = pastaDeTrabalho();
        try {
            extrairEConferir(zip, pasta, true);
            trocar(pasta);
            LOG.warning(() -> "Dados voltaram ao backup " + zip);
        } finally {
            Arquivos.apagarArvore(pasta);
        }
    }

    private Path pastaDeTrabalho() {
        try {
            Files.createDirectories(backup.pastas().temporaria());
            return Files.createTempDirectory(backup.pastas().temporaria(), "restauracao-");
        } catch (IOException erro) {
            throw new ErroDados("Não foi possível criar a pasta de trabalho: " + erro.getMessage(), erro);
        }
    }

    /**
     * Extrai o banco e os anexos na pasta e confere tudo; não mexe nos dados atuais.
     *
     * @param trazerAnexos no formato 2, copiar os anexos para a pasta de trabalho (false = só conferir)
     */
    private static Manifesto extrairEConferir(Path zip, Path pasta, boolean trazerAnexos) {
        if (!Files.isRegularFile(zip)) {
            throw new BackupInvalidoException("Arquivo de backup não encontrado: " + zip);
        }
        try (ZipFile arquivo = new ZipFile(zip.toFile(), StandardCharsets.UTF_8)) {
            Manifesto manifesto = lerManifesto(arquivo);
            if (manifesto.formato() > Manifesto.FORMATO) {
                throw new BackupInvalidoException("Este backup foi feito por uma versão mais nova do GarageTech ("
                        + manifesto.versaoApp() + "). Atualize o GarageTech antes de restaurar.");
            }
            ZipEntry entradaBanco = arquivo.getEntry(Backup.ENTRADA_BANCO);
            if (entradaBanco == null) {
                throw new BackupInvalidoException("O backup está incompleto: falta o banco de dados.");
            }
            Path banco = pasta.resolve(Backup.ENTRADA_BANCO);
            try (InputStream dados = arquivo.getInputStream(entradaBanco)) {
                Files.copy(dados, banco);
            }
            if (!Arquivos.sha256(banco).equals(manifesto.sha256Banco())) {
                throw new BackupInvalidoException("O backup está corrompido: o banco não confere com o manifesto.");
            }
            if (manifesto.formato() <= Manifesto.FORMATO_ANEXOS_NO_ZIP) {
                extrairAnexos(arquivo, pasta.resolve("anexos"));
            } else {
                trazerAnexos(manifesto, AnexosDosBackups.doBackup(zip), trazerAnexos ? pasta.resolve("anexos") : null);
            }
            conferirBanco(banco, manifesto);
            return manifesto;
        } catch (ZipException erro) {
            throw new BackupInvalidoException("O arquivo não é um backup do GarageTech (zip inválido).", erro);
        } catch (IOException erro) {
            throw new BackupInvalidoException("Não foi possível ler o backup: " + erro.getMessage(), erro);
        }
    }

    private static Manifesto lerManifesto(ZipFile arquivo) throws IOException {
        ZipEntry entrada = arquivo.getEntry(Backup.ENTRADA_MANIFESTO);
        if (entrada == null) {
            throw new BackupInvalidoException("O arquivo não é um backup do GarageTech (falta o manifesto).");
        }
        try (InputStream dados = arquivo.getInputStream(entrada)) {
            return Json.ler(new String(dados.readAllBytes(), StandardCharsets.UTF_8), Manifesto.class);
        } catch (ErroDados erro) {
            throw new BackupInvalidoException("O manifesto do backup está ilegível.", erro);
        }
    }

    private static void extrairAnexos(ZipFile arquivo, Path destino) throws IOException {
        Files.createDirectories(destino);
        Enumeration<? extends ZipEntry> entradas = arquivo.entries();
        while (entradas.hasMoreElements()) {
            ZipEntry entrada = entradas.nextElement();
            String nome = entrada.getName();
            if (entrada.isDirectory() || !nome.startsWith(Backup.PREFIXO_ANEXOS)) {
                continue;
            }
            Path alvo = destino.resolve(nome.substring(Backup.PREFIXO_ANEXOS.length())).normalize();
            if (!alvo.startsWith(destino)) {
                throw new BackupInvalidoException("O backup tem um anexo com caminho inválido: " + nome);
            }
            Files.createDirectories(alvo.getParent());
            try (InputStream dados = arquivo.getInputStream(entrada)) {
                Files.copy(dados, alvo);
            }
        }
    }

    /**
     * Os anexos do formato 2, da pasta {@code anexos-dos-backups} ao lado do zip, cada um conferido pelo SHA-256.
     * Primeiro confere se todos estão lá (sem copiar nada); depois copia e confere o que foi copiado.
     *
     * @param destino a pasta de anexos de trabalho; null = só conferir os arquivos da pasta, sem copiar
     */
    private static void trazerAnexos(Manifesto manifesto, AnexosDosBackups guardados, Path destino)
            throws IOException {
        record Copia(AnexoNoBackup anexo, Path guardado, Path alvo) {
        }
        Path raiz = destino != null ? destino : guardados.pasta().resolve("conferencia");
        List<Copia> copias = new ArrayList<>();
        int faltando = 0;
        for (AnexoNoBackup anexo : manifesto.arquivos()) {
            Path alvo = dentroDaPasta(raiz, anexo.caminho());
            Path guardado = guardados.arquivo(anexo.sha256());
            if (Files.isRegularFile(guardado)) {
                copias.add(new Copia(anexo, guardado, alvo));
            } else {
                faltando++;
            }
        }
        if (faltando > 0) {
            throw new BackupInvalidoException((faltando == 1 ? "Falta 1 anexo" : "Faltam " + faltando + " anexos")
                    + " deste backup na pasta " + guardados.pasta() + ". Ela fica ao lado do arquivo do backup: se "
                    + "você copiou o backup para outro lugar, copie também a pasta " + AnexosDosBackups.PASTA + ".");
        }
        if (destino != null) {
            Files.createDirectories(destino);
        }
        for (Copia copia : copias) {
            Path conferido = copia.guardado();
            if (destino != null) {
                Files.createDirectories(copia.alvo().getParent());
                Files.copy(copia.guardado(), copia.alvo(), StandardCopyOption.REPLACE_EXISTING);
                conferido = copia.alvo();
            }
            if (!Arquivos.sha256(conferido).equals(copia.anexo().sha256())) {
                throw new BackupInvalidoException("O backup está corrompido: o anexo " + copia.anexo().caminho()
                        + " não confere com o manifesto.");
            }
        }
    }

    /** O caminho do anexo dentro da pasta de anexos; um caminho que sai dela (ex.: "../fora.txt") é recusado. */
    private static Path dentroDaPasta(Path raiz, String caminho) {
        try {
            Path base = raiz.toAbsolutePath().normalize();
            Path alvo = base.resolve(caminho).normalize();
            if (!alvo.startsWith(base) || alvo.equals(base)) {
                throw new BackupInvalidoException("O backup tem um anexo com caminho inválido: " + caminho);
            }
            return alvo;
        } catch (InvalidPathException erro) {
            throw new BackupInvalidoException("O backup tem um anexo com caminho inválido: " + caminho, erro);
        }
    }

    /** PRAGMA integrity_check e versão do banco igual à do manifesto. */
    private static void conferirBanco(Path banco, Manifesto manifesto) {
        SQLiteConfig config = new SQLiteConfig();
        try (Connection conexao = config.createConnection("jdbc:sqlite:" + banco)) {
            try (Statement consulta = conexao.createStatement();
                    ResultSet resultado = consulta.executeQuery("PRAGMA integrity_check")) {
                String situacao = resultado.next() ? resultado.getString(1) : "sem resposta";
                if (!"ok".equalsIgnoreCase(situacao)) {
                    throw new BackupInvalidoException("O banco dentro do backup está danificado (" + situacao + ").");
                }
            }
            String versao = BancoDados.versaoDoEsquema(conexao);
            if (!Objects.equals(versao, manifesto.versaoBanco())) {
                throw new BackupInvalidoException("O backup não confere: banco v" + versao + ", manifesto v"
                        + manifesto.versaoBanco() + ".");
            }
        } catch (SQLException erro) {
            throw new BackupInvalidoException("O banco dentro do backup não abre: " + erro.getMessage(), erro);
        }
    }

    /** Uma versão de banco é mais nova que a outra? (null = banco ainda sem migração) */
    static boolean maisNova(String versao, String referencia) {
        if (versao == null) {
            return false;
        }
        return referencia == null || MigrationVersion.fromVersion(versao).isNewerThan(referencia);
    }

    /**
     * Troca banco e anexos pelos da pasta de trabalho. Os atuais vão para a pasta de trabalho primeiro;
     * se algo falhar no meio, tudo volta como estava.
     */
    private void trocar(Path pasta) {
        PastasDados pastas = backup.pastas();
        Path banco = pastas.banco();
        Path guardado = pasta.resolve("dados-anteriores");
        Deque<Path[]> feitos = new ArrayDeque<>();
        try {
            Files.createDirectories(guardado);
            moverSeExistir(banco, guardado.resolve("calibra.db"), feitos);
            moverSeExistir(irmao(banco, "-wal"), guardado.resolve("calibra.db-wal"), feitos);
            moverSeExistir(irmao(banco, "-shm"), guardado.resolve("calibra.db-shm"), feitos);
            moverSeExistir(pastas.anexos(), guardado.resolve("anexos"), feitos);
            moverSeExistir(pasta.resolve(Backup.ENTRADA_BANCO), banco, feitos);
            moverSeExistir(pasta.resolve("anexos"), pastas.anexos(), feitos);
            Files.createDirectories(pastas.anexos());
        } catch (IOException erro) {
            desfazer(feitos, erro);
            throw new ErroDados("Não foi possível trocar os dados pelo backup; os dados atuais foram mantidos. "
                    + erro.getMessage(), erro);
        }
    }

    private static Path irmao(Path arquivo, String sufixo) {
        return arquivo.resolveSibling(arquivo.getFileName() + sufixo);
    }

    private static void moverSeExistir(Path origem, Path destino, Deque<Path[]> feitos) throws IOException {
        if (Files.exists(origem)) {
            Arquivos.mover(origem, destino);
            feitos.push(new Path[] {origem, destino});
        }
    }

    private static void desfazer(Deque<Path[]> feitos, IOException causa) {
        while (!feitos.isEmpty()) {
            Path[] movimento = feitos.pop();
            try {
                Arquivos.mover(movimento[1], movimento[0]);
            } catch (IOException erro) {
                causa.addSuppressed(erro);
                LOG.log(Level.SEVERE, "Não foi possível desfazer a troca de " + movimento[0], erro);
            }
        }
    }
}
