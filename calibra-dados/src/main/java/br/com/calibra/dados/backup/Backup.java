package br.com.calibra.dados.backup;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.json.Json;

/**
 * Backup do Calibra (SPEC 10): um zip com o banco, a pasta de anexos e um manifesto.
 *
 * <p>O banco é copiado com {@code VACUUM INTO}, que tira uma foto consistente mesmo com o app aberto.
 * O zip é montado com outro nome e só é renomeado no fim, então nunca fica um backup pela metade com
 * o nome certo. Na pasta ficam os 30 backups mais recentes.
 */
public final class Backup {

    /** Quantos backups ficam na pasta (SPEC 10). */
    public static final int MANTER = 30;

    static final String ENTRADA_BANCO = "calibra.db";
    static final String ENTRADA_MANIFESTO = "manifesto.json";
    static final String PREFIXO_ANEXOS = "anexos/";

    private static final Logger LOG = Logger.getLogger(Backup.class.getName());
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
    private static final Pattern NOME = Pattern.compile("calibra-backup-(\\d{4}-\\d{2}-\\d{2}-\\d{6})(?:-(\\d+))?\\.zip");

    private final PastasDados pastas;
    private final BancoDados banco;
    private final String versaoApp;
    private final Clock relogio;

    public Backup(PastasDados pastas, BancoDados banco, String versaoApp, Clock relogio) {
        this.pastas = Objects.requireNonNull(pastas, "pastas");
        this.banco = Objects.requireNonNull(banco, "banco");
        this.versaoApp = Objects.requireNonNull(versaoApp, "versaoApp");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    public PastasDados pastas() {
        return pastas;
    }

    public BancoDados banco() {
        return banco;
    }

    /**
     * Faz um backup completo na pasta e apaga os mais antigos, mantendo os {@link #MANTER} mais recentes.
     *
     * @throws ErroDados se não conseguir gravar (pasta sem permissão, disco cheio, pen drive removido)
     */
    public InfoBackup fazer(Path pasta, MotivoBackup motivo) {
        Path copia = null;
        try {
            Files.createDirectories(pasta);
            Files.createDirectories(pastas.temporaria());
            LocalDateTime agora = LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
            String versaoBanco = banco.versaoDoEsquema();
            copia = copiarBanco();
            List<Path> anexos = Arquivos.arquivosDentro(pastas.anexos());
            Manifesto manifesto = new Manifesto(Manifesto.FORMATO, versaoApp, versaoBanco, agora, motivo,
                    Arquivos.sha256(copia), Files.size(copia), anexos.size());
            Path destino = nomeLivre(pasta, agora);
            Path parcial = destino.resolveSibling(destino.getFileName() + ".parcial");
            escreverZip(parcial, copia, anexos, manifesto);
            Arquivos.mover(parcial, destino);
            manterMaisRecentes(pasta, MANTER, destino);
            LOG.info(() -> "Backup feito (" + motivo.rotulo() + "): " + destino);
            return new InfoBackup(destino, manifesto, Files.size(destino));
        } catch (IOException | SQLException erro) {
            throw new ErroDados("Não foi possível fazer o backup em " + pasta + ": " + erro.getMessage(), erro);
        } finally {
            apagarCopia(copia);
        }
    }

    /** Backups da pasta, do mais novo para o mais antigo. Arquivos que não dá para ler ficam de fora. */
    public static List<InfoBackup> listar(Path pasta) {
        return arquivosDeBackup(pasta).stream().map(Backup::lerInfo).flatMap(Optional::stream).toList();
    }

    /** O backup mais recente da pasta, se houver. */
    public static Optional<InfoBackup> maisRecente(Path pasta) {
        return arquivosDeBackup(pasta).stream().map(Backup::lerInfo).flatMap(Optional::stream).findFirst();
    }

    /** Lê o manifesto de um arquivo de backup; vazio se o arquivo não for um backup legível. */
    public static Optional<InfoBackup> lerInfo(Path arquivo) {
        try (ZipFile zip = new ZipFile(arquivo.toFile(), StandardCharsets.UTF_8)) {
            ZipEntry entrada = zip.getEntry(ENTRADA_MANIFESTO);
            if (entrada == null) {
                return Optional.empty();
            }
            try (InputStream dados = zip.getInputStream(entrada)) {
                Manifesto manifesto = Json.ler(new String(dados.readAllBytes(), StandardCharsets.UTF_8), Manifesto.class);
                return Optional.of(new InfoBackup(arquivo, manifesto, Files.size(arquivo)));
            }
        } catch (IOException | ErroDados erro) {
            LOG.log(Level.WARNING, "Arquivo de backup ilegível: " + arquivo, erro);
            return Optional.empty();
        }
    }

    /**
     * Apaga os backups mais antigos da pasta, deixando os {@code quantos} mais recentes, e nunca o que acabou de ser
     * feito. A ordem vem da data no nome: com o relógio do PC atrasado, o backup novo pareceria o mais antigo e seria
     * apagado na hora (todo backup falharia, e a restauração também).
     */
    static void manterMaisRecentes(Path pasta, int quantos, Path novo) throws IOException {
        List<Path> outros = arquivosDeBackup(pasta).stream().filter(arquivo -> !arquivo.equals(novo)).toList();
        int manterOutros = Math.max(0, quantos - 1);
        for (Path antigo : outros.subList(Math.min(manterOutros, outros.size()), outros.size())) {
            Files.deleteIfExists(antigo);
            LOG.info(() -> "Backup antigo apagado (ficam os " + quantos + " mais recentes): " + antigo);
        }
    }

    /** Arquivos com nome de backup do Calibra, do mais novo para o mais antigo (pelo nome). */
    static List<Path> arquivosDeBackup(Path pasta) {
        if (!Files.isDirectory(pasta)) {
            return List.of();
        }
        try (Stream<Path> itens = Files.list(pasta)) {
            return itens.filter(Files::isRegularFile)
                    .filter(arquivo -> NOME.matcher(arquivo.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(Backup::chaveDeOrdem).reversed())
                    .toList();
        } catch (IOException erro) {
            throw new ErroDados("Não foi possível ler a pasta de backup " + pasta + ": " + erro.getMessage(), erro);
        }
    }

    /** "2026-10-11-183045" + contador com 4 dígitos: ordena certo mesmo com dois backups no mesmo segundo. */
    private static String chaveDeOrdem(Path arquivo) {
        Matcher partes = NOME.matcher(arquivo.getFileName().toString());
        if (!partes.matches()) {
            return "";
        }
        int contador = partes.group(2) == null ? 0 : Integer.parseInt(partes.group(2));
        return partes.group(1) + String.format("-%04d", contador);
    }

    private static Path nomeLivre(Path pasta, LocalDateTime agora) {
        String base = "calibra-backup-" + CARIMBO.format(agora);
        Path destino = pasta.resolve(base + ".zip");
        for (int contador = 1; Files.exists(destino); contador++) {
            destino = pasta.resolve(base + "-" + contador + ".zip");
        }
        return destino;
    }

    /** Cópia consistente do banco na pasta temporária. */
    private Path copiarBanco() throws IOException, SQLException {
        Path copia = Files.createTempFile(pastas.temporaria(), "backup-", ".db");
        Files.delete(copia); // o VACUUM INTO cria o arquivo; ele não pode existir antes
        try (Connection conexao = banco.conexao();
                PreparedStatement vacuo = conexao.prepareStatement("VACUUM INTO ?")) {
            vacuo.setString(1, copia.toString());
            vacuo.execute();
        }
        return copia;
    }

    private void escreverZip(Path destino, Path copiaBanco, List<Path> anexos, Manifesto manifesto)
            throws IOException {
        try (OutputStream saida = new BufferedOutputStream(Files.newOutputStream(destino));
                ZipOutputStream zip = new ZipOutputStream(saida, StandardCharsets.UTF_8)) {
            adicionar(zip, ENTRADA_BANCO, copiaBanco);
            for (Path anexo : anexos) {
                String relativo = pastas.anexos().relativize(anexo).toString().replace('\\', '/');
                adicionar(zip, PREFIXO_ANEXOS + relativo, anexo);
            }
            zip.putNextEntry(new ZipEntry(ENTRADA_MANIFESTO));
            zip.write(Json.escreverFormatado(manifesto).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    private static void adicionar(ZipOutputStream zip, String nome, Path arquivo) throws IOException {
        zip.putNextEntry(new ZipEntry(nome));
        Files.copy(arquivo, zip);
        zip.closeEntry();
    }

    private static void apagarCopia(Path copia) {
        if (copia == null) {
            return;
        }
        try {
            Files.deleteIfExists(copia);
        } catch (IOException erro) {
            LOG.log(Level.WARNING, "Não foi possível apagar a cópia temporária " + copia, erro);
        }
    }
}
