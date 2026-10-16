package br.com.calibra.app.casos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        long tamanho = Files.size(origem);
        if (tamanho > TAMANHO_MAXIMO_BYTES) {
            throw new OperacaoRecusadaException("O arquivo " + nome + " passa de 20 MB.");
        }
        String sha256 = sha256(origem);
        String relativo = PASTA + "/" + veiculoId + "/" + sha256.substring(0, 16) + "."
                + (extensao.equals("jpeg") ? "jpg" : extensao);
        Path destino = anexos.resolve(relativo);
        if (!Files.exists(destino)) {
            Files.createDirectories(destino.getParent());
            Path parcial = destino.resolveSibling(destino.getFileName() + ".parcial");
            Files.copy(origem, parcial, StandardCopyOption.REPLACE_EXISTING);
            Files.move(parcial, destino, StandardCopyOption.ATOMIC_MOVE);
        }
        return new Copia(relativo, tamanho, sha256);
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

    /** Foto (a tela mostra a miniatura); o resto é documento. */
    public static boolean imagem(String arquivo) {
        return IMAGENS.contains(extensao(arquivo));
    }

    private static String extensao(String nome) {
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    /** O SHA-256 do arquivo, lido aos pedaços (uma foto de 20 MB não precisa caber inteira na memória). */
    private static String sha256(Path arquivo) throws IOException {
        MessageDigest resumo;
        try {
            resumo = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 faz parte de todo Java", erro);
        }
        try (InputStream entrada = new DigestInputStream(Files.newInputStream(arquivo), resumo)) {
            entrada.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(resumo.digest());
    }
}
