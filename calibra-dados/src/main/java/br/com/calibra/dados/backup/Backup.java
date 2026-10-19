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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
 * Backup do Calibra (SPEC 10). Desde a fase 3B (formato 2), um zip com o banco e um manifesto que lista os anexos; os
 * anexos ficam uma vez só na pasta {@code anexos-dos-backups}, ao lado dos zips ({@link AnexosDosBackups}). O zip
 * antigo (formato 1, com os anexos dentro) continua restaurando.
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
    /** Final do nome de um arquivo ainda sendo gravado (o backup, ou a cópia de um anexo). */
    static final String PARCIAL = ".parcial";

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
     * Faz um backup completo na pasta e apaga os mais antigos, mantendo os {@link #MANTER} mais recentes; depois tira
     * da pasta {@code anexos-dos-backups} o que nenhum backup usa mais.
     *
     * <p>Os anexos são guardados antes da cópia do banco: um anexo removido no meio do backup fica de fora, e o banco,
     * copiado depois, também não o tem; um removido depois de guardado só sobra no backup, sem uso.
     *
     * @throws ErroDados se não conseguir gravar (pasta sem permissão, disco cheio, pen drive removido)
     */
    public InfoBackup fazer(Path pasta, MotivoBackup motivo) {
        Path copia = null;
        try {
            Files.createDirectories(pasta);
            Files.createDirectories(pastas.temporaria());
            LocalDateTime agora = LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
            List<AnexoNoBackup> arquivos = guardarAnexos(AnexosDosBackups.na(pasta));
            String versaoBanco = banco.versaoDoEsquema();
            copia = copiarBanco();
            Manifesto manifesto = new Manifesto(Manifesto.FORMATO, versaoApp, versaoBanco, agora, motivo,
                    Arquivos.sha256(copia), Files.size(copia), arquivos.size(), arquivos);
            Path destino = nomeLivre(pasta, agora);
            Path parcial = destino.resolveSibling(destino.getFileName() + PARCIAL);
            escreverZip(parcial, copia, manifesto);
            Arquivos.mover(parcial, destino);
            manterMaisRecentes(pasta, MANTER, destino);
            limparAnexosSemUso(pasta);
            LOG.info(() -> "Backup feito (" + motivo.rotulo() + "): " + destino);
            return new InfoBackup(destino, manifesto, Files.size(destino));
        } catch (IOException | SQLException erro) {
            throw new ErroDados("Não foi possível fazer o backup em " + pasta + ": " + erro.getMessage(), erro);
        } finally {
            apagarCopia(copia);
        }
    }

    /** O espaço que os anexos dos backups ocupam na pasta (a pasta {@code anexos-dos-backups}), em bytes. */
    public static long tamanhoDosAnexos(Path pasta) {
        try {
            return AnexosDosBackups.na(pasta).tamanho();
        } catch (IOException erro) {
            LOG.log(Level.WARNING, "Não foi possível medir os anexos dos backups em " + pasta, erro);
            return 0;
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

    /**
     * Os arquivos da pasta de anexos que vão no backup. Fica de fora a cópia pela metade ({@code .parcial}) de um
     * anexo sendo guardado, ou que sobrou de uma queda de energia: ela não é anexo de ninguém.
     */
    private List<Path> anexosDoBackup() throws IOException {
        return Arquivos.arquivosDentro(pastas.anexos()).stream()
                .filter(arquivo -> !arquivo.getFileName().toString().endsWith(PARCIAL))
                .toList();
    }

    /**
     * Guarda os anexos na pasta {@code anexos-dos-backups} (o que já está lá não é copiado de novo). Um anexo removido
     * no meio (a tela segue aberta durante o backup automático) fica de fora, em vez de derrubar o backup inteiro.
     */
    private List<AnexoNoBackup> guardarAnexos(AnexosDosBackups guardados) throws IOException {
        List<AnexoNoBackup> lista = new ArrayList<>();
        for (Path anexo : anexosDoBackup()) {
            String caminho = pastas.anexos().relativize(anexo).toString().replace('\\', '/');
            Optional<AnexosDosBackups.Guardado> guardado = guardados.guardar(anexo);
            if (guardado.isPresent()) {
                lista.add(new AnexoNoBackup(caminho, guardado.get().sha256(), guardado.get().tamanho()));
            } else {
                LOG.info(() -> "Anexo removido durante o backup, ficou de fora: " + anexo);
            }
        }
        return lista;
    }

    /** Monta o zip: o banco e o manifesto, com a lista dos anexos guardados. */
    private static void escreverZip(Path destino, Path copiaBanco, Manifesto manifesto) throws IOException {
        try (OutputStream saida = new BufferedOutputStream(Files.newOutputStream(destino));
                ZipOutputStream zip = new ZipOutputStream(saida, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(ENTRADA_BANCO));
            Files.copy(copiaBanco, zip);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(ENTRADA_MANIFESTO));
            zip.write(Json.escreverFormatado(manifesto).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    /**
     * Tira da pasta {@code anexos-dos-backups} o que nenhum backup da pasta usa mais e as cópias pela metade. Se algum
     * backup da pasta não puder ser lido, não apaga nada (ele pode usar um desses arquivos) e deixa para o próximo
     * backup. Um erro aqui só vai para o log: o backup já está feito.
     */
    static void limparAnexosSemUso(Path pasta) {
        AnexosDosBackups guardados = AnexosDosBackups.na(pasta);
        try {
            Set<String> emUso = new HashSet<>();
            for (Path arquivo : arquivosDeBackup(pasta)) {
                Optional<InfoBackup> info = lerInfo(arquivo);
                if (info.isEmpty()) {
                    LOG.warning(() -> "Limpeza de " + guardados.pasta() + " adiada: o backup " + arquivo
                            + " não pôde ser lido.");
                    return;
                }
                info.get().manifesto().arquivos().forEach(anexo -> emUso.add(anexo.sha256()));
            }
            int apagados = guardados.limpar(emUso);
            if (apagados > 0) {
                LOG.info(() -> "Anexos que nenhum backup usa mais, apagados de " + guardados.pasta() + ": " + apagados
                        + ".");
            }
        } catch (IOException | RuntimeException erro) {
            LOG.log(Level.WARNING, "Não foi possível limpar " + guardados.pasta(), erro);
        }
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
