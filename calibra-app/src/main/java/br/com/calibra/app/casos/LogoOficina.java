package br.com.calibra.app.casos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

import br.com.calibra.core.cadastro.DadosOficina;

/**
 * Logo da oficina: a imagem escolhida é copiada para {@code anexos\oficina\}, que entra no backup. O nome da
 * cópia leva a impressão digital da imagem, então trocar o logo nunca sobrescreve o arquivo que está em uso.
 */
public final class LogoOficina {

    /** Pasta do logo dentro de anexos. */
    static final String PASTA = "oficina";

    /** Maior imagem aceita; um logo de PDF não precisa de mais. */
    public static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024;

    private LogoOficina() {
    }

    /**
     * Copia a imagem para a pasta de anexos.
     *
     * @return o caminho relativo à pasta de anexos, para {@link DadosOficina#logo()}
     * @throws IllegalArgumentException se não for PNG ou JPG, ou passar de 5 MB (mensagem para a tela)
     * @throws IOException se não der para ler a imagem ou gravar a cópia
     */
    public static String guardar(Path imagem, Path anexos) throws IOException {
        String extensao = extensao(imagem);
        if (!extensao.equals("png") && !extensao.equals("jpg") && !extensao.equals("jpeg")) {
            throw new IllegalArgumentException("Use uma imagem PNG ou JPG para o logo.");
        }
        if (Files.size(imagem) > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("A imagem do logo passa de 5 MB. Use uma imagem menor.");
        }
        String nome = PASTA + "/logo-" + impressao(imagem) + "." + extensao;
        Path destino = anexos.resolve(nome);
        if (!Files.exists(destino)) {
            Files.createDirectories(destino.getParent());
            Path parcial = destino.resolveSibling(destino.getFileName() + ".parcial");
            Files.copy(imagem, parcial, StandardCopyOption.REPLACE_EXISTING);
            Files.move(parcial, destino, StandardCopyOption.ATOMIC_MOVE);
        }
        return nome;
    }

    /** O arquivo do logo, se a oficina tiver um e ele existir na pasta de anexos. */
    public static Optional<Path> arquivo(DadosOficina oficina, Path anexos) {
        if (oficina.logo() == null) {
            return Optional.empty();
        }
        Path arquivo = anexos.resolve(oficina.logo()).normalize();
        return arquivo.startsWith(anexos.normalize()) && Files.isRegularFile(arquivo)
                ? Optional.of(arquivo) : Optional.empty();
    }

    private static String extensao(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        int ponto = nome.lastIndexOf('.');
        return ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
    }

    /** Os 12 primeiros caracteres do SHA-256: a mesma imagem sempre ganha o mesmo nome. */
    private static String impressao(Path arquivo) throws IOException {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(arquivo));
            return HexFormat.of().formatHex(resumo).substring(0, 12);
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 faz parte de todo Java", erro);
        }
    }
}
