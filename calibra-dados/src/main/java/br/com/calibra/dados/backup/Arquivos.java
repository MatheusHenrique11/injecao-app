package br.com.calibra.dados.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

/** Utilidades de arquivo do backup. */
final class Arquivos {

    private Arquivos() {
    }

    /** SHA-256 do arquivo, em hexadecimal. */
    static String sha256(Path arquivo) throws IOException {
        try (InputStream entrada = Files.newInputStream(arquivo)) {
            MessageDigest resumo = MessageDigest.getInstance("SHA-256");
            byte[] bloco = new byte[64 * 1024];
            int lidos;
            while ((lidos = entrada.read(bloco)) > 0) {
                resumo.update(bloco, 0, lidos);
            }
            return HexFormat.of().formatHex(resumo.digest());
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("O Java sempre tem SHA-256", erro);
        }
    }

    /** Move de uma vez quando o disco permite; senão, move substituindo. */
    static void mover(Path origem, Path destino) throws IOException {
        try {
            Files.move(origem, destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException erro) {
            Files.move(origem, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Arquivos comuns dentro da pasta (e subpastas), em ordem; vazio se a pasta não existir. */
    static List<Path> arquivosDentro(Path pasta) throws IOException {
        if (!Files.isDirectory(pasta)) {
            return List.of();
        }
        try (Stream<Path> itens = Files.walk(pasta)) {
            return itens.filter(Files::isRegularFile).sorted().toList();
        }
    }

    /** Apaga a pasta e tudo dentro; não reclama se ela não existir. */
    static void apagarArvore(Path pasta) {
        if (pasta == null || !Files.exists(pasta)) {
            return;
        }
        try (Stream<Path> itens = Files.walk(pasta)) {
            for (Path item : itens.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(item);
            }
        } catch (IOException erro) {
            throw new UncheckedIOException("Não foi possível apagar a pasta temporária " + pasta, erro);
        }
    }
}
