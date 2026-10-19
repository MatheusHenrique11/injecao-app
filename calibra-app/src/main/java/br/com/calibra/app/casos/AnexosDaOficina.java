package br.com.calibra.app.casos;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import br.com.calibra.dados.OperacaoRecusadaException;

/**
 * Os arquivos dos anexos da OS e do veículo (bloco 3.2): fotos em JPG ou PNG e documentos em PDF, até 20 MB, copiados
 * para a pasta de anexos (o backup leva junto). O nome é a impressão SHA-256 do arquivo: o mesmo arquivo anexado duas
 * vezes vira uma cópia só.
 */
public final class AnexosDaOficina {

    /** Pasta dos anexos dos veículos, dentro da pasta de anexos. */
    static final String PASTA = "veiculos";

    /** Maior arquivo aceito: uma foto de celular tem de 3 a 6 MB, e um PDF de nota fiscal, bem menos. */
    public static final long TAMANHO_MAXIMO_BYTES = 20L * 1024 * 1024;

    private static final Set<String> IMAGENS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ACEITOS = Set.of("jpg", "jpeg", "png", "pdf");
    /** O nome que {@link #guardar} dá: veiculos/7/0123456789abcdef.jpg, ou a cópia pela metade (.parcial). */
    private static final Pattern NOME_DA_COPIA = Pattern.compile(
            PASTA + "/\\d+/[0-9a-f]{16}\\.(jpg|png|pdf)(\\.parcial)?");

    /** O arquivo copiado: o caminho relativo à pasta de anexos, o tamanho e o SHA-256 (para conferir). */
    public record Copia(String arquivo, long tamanhoBytes, String sha256) {
    }

    private AnexosDaOficina() {
    }

    /**
     * Copia o arquivo para a pasta de anexos do veículo. Se a mesma cópia já existe, só devolve o caminho dela.
     *
     * @throws OperacaoRecusadaException se não for JPG, PNG ou PDF, ou passar de 20 MB (mensagem para a tela)
     * @throws IOException se não der para ler o arquivo ou gravar a cópia
     */
    public static Copia guardar(Path origem, Path anexos, long veiculoId) throws IOException {
        String nome = origem.getFileName().toString();
        String extensao = extensao(nome);
        if (!ACEITOS.contains(extensao)) {
            throw new OperacaoRecusadaException("O arquivo " + nome + " não é foto (JPG ou PNG) nem PDF. Fotos do "
                    + "iPhone em HEIC: mande pelo WhatsApp, que ele converte para JPG.");
        }
        if (Files.size(origem) > TAMANHO_MAXIMO_BYTES) {
            throw new OperacaoRecusadaException("O arquivo " + nome + " passa de 20 MB.");
        }
        String tipo = extensao.equals("jpeg") ? "jpg" : extensao;
        Path pasta = anexos.resolve(PASTA).resolve(Long.toString(veiculoId));
        Files.createDirectories(pasta);
        // O nome sai do SHA-256, calculado durante a cópia (a foto é lida uma vez só). Até lá, a cópia tem um nome
        // provisório terminado em .parcial, que o backup ignora e a limpeza ao abrir o app apaga, se sobrar.
        Path parcial = pasta.resolve(provisorio() + "." + tipo + ".parcial");
        try {
            MessageDigest resumo = novoResumo();
            try (InputStream entrada = new DigestInputStream(Files.newInputStream(origem), resumo)) {
                Files.copy(entrada, parcial, StandardCopyOption.REPLACE_EXISTING);
            }
            String sha256 = HexFormat.of().formatHex(resumo.digest());
            long tamanho = Files.size(parcial);
            String relativo = PASTA + "/" + veiculoId + "/" + sha256.substring(0, 16) + "." + tipo;
            Path destino = anexos.resolve(relativo);
            if (!Files.exists(destino)) {
                Files.move(parcial, destino, StandardCopyOption.ATOMIC_MOVE);
            }
            return new Copia(relativo, tamanho, sha256);
        } finally {
            Files.deleteIfExists(parcial);
        }
    }

    /** 16 caracteres hexadecimais ao acaso: o nome provisório tem o mesmo formato dos nomes de verdade. */
    private static String provisorio() {
        byte[] bytes = new byte[8];
        ThreadLocalRandom.current().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** O arquivo do anexo na pasta de anexos, se existir (um caminho que sai da pasta não vale). */
    public static Optional<Path> arquivo(String relativo, Path anexos) {
        Path arquivo = anexos.resolve(relativo).normalize();
        return arquivo.startsWith(anexos.normalize()) && Files.isRegularFile(arquivo)
                ? Optional.of(arquivo) : Optional.empty();
    }

    /** Apaga o arquivo do anexo, se ele existir na pasta de anexos (quando nenhum anexo o usa mais). */
    static void apagar(String relativo, Path anexos) throws IOException {
        Optional<Path> arquivo = arquivo(relativo, anexos);
        if (arquivo.isPresent()) {
            Files.deleteIfExists(arquivo.get());
        }
    }

    /** O caminho (relativo à pasta de anexos, com "/") tem o nome que o Calibra dá às cópias? */
    static boolean copiaDoCalibra(String relativo) {
        return NOME_DA_COPIA.matcher(relativo).matches();
    }

    /** Foto (a tela mostra a miniatura); o resto é documento. */
    public static boolean imagem(String arquivo) {
        return IMAGENS.contains(extensao(arquivo));
    }

    private static String extensao(String nome) {
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    private static MessageDigest novoResumo() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 faz parte de todo Java", erro);
        }
    }
}
