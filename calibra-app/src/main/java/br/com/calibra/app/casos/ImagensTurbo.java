package br.com.calibra.app.casos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import br.com.calibra.dados.OperacaoRecusadaException;

/**
 * As imagens dos mapas de compressor, copiadas para {@code anexos\turbos\} (vão junto no backup). O turbo guarda só o
 * nome do arquivo. A imagem do fabricante fica no computador da oficina: nada vai para o repositório nem para a rede.
 */
public final class ImagensTurbo {

    private static final Set<String> EXTENSOES = Set.of("png", "jpg", "jpeg", "gif", "bmp");
    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path pasta;
    private final Clock relogio;

    /** @param anexos a pasta de anexos do app ({@code %LOCALAPPDATA%\Calibra\anexos}) */
    public ImagensTurbo(Path anexos, Clock relogio) {
        this.pasta = Objects.requireNonNull(anexos, "anexos").resolve("turbos");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /**
     * Copia a imagem escolhida para a pasta dos turbos.
     *
     * @param itemId o turbo no catálogo (entra no nome do arquivo)
     * @return o nome do arquivo copiado, para guardar no turbo
     * @throws OperacaoRecusadaException se não for uma imagem PNG, JPG, GIF ou BMP
     */
    public String copiar(Path origem, long itemId) throws IOException {
        String nome = origem.getFileName().toString();
        int ponto = nome.lastIndexOf('.');
        String extensao = ponto < 0 ? "" : nome.substring(ponto + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSOES.contains(extensao)) {
            throw new OperacaoRecusadaException("Escolha uma imagem PNG, JPG, GIF ou BMP do mapa do fabricante.");
        }
        Files.createDirectories(pasta);
        String destino = "turbo-" + itemId + "-" + CARIMBO.format(LocalDateTime.now(relogio)) + "." + extensao;
        Files.copy(origem, pasta.resolve(destino));
        return destino;
    }

    /** O arquivo de uma imagem guardada (só nomes simples, sem pastas). */
    public Path caminho(String nome) {
        Path arquivo = pasta.resolve(nome).normalize();
        if (!arquivo.getParent().equals(pasta.normalize())) {
            throw new OperacaoRecusadaException("Nome de imagem inválido: " + nome);
        }
        return arquivo;
    }
}
