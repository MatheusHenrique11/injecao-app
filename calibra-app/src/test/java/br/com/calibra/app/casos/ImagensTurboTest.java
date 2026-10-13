package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.dados.OperacaoRecusadaException;

/** Imagens dos mapas copiadas para anexos\turbos (vão no backup). */
class ImagensTurboTest {

    @TempDir
    Path pasta;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 13, 9, 30, 5));

    @Test
    void copiaParaAPastaDosTurbos() throws IOException {
        Path origem = Files.write(pasta.resolve("Mapa GT28.PNG"), new byte[] {1, 2, 3});
        ImagensTurbo imagens = new ImagensTurbo(pasta.resolve("anexos"), relogio);
        String nome = imagens.copiar(origem, 7);
        assertEquals("turbo-7-20261013-093005.png", nome);
        Path copia = imagens.caminho(nome);
        assertEquals(pasta.resolve("anexos").resolve("turbos").resolve(nome), copia);
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(copia));
    }

    @Test
    void recusaOQueNaoEImagemENomeComPasta() throws IOException {
        Path texto = Files.writeString(pasta.resolve("mapa.txt"), "x");
        ImagensTurbo imagens = new ImagensTurbo(pasta.resolve("anexos"), relogio);
        assertThrows(OperacaoRecusadaException.class, () -> imagens.copiar(texto, 1));
        assertThrows(OperacaoRecusadaException.class, () -> imagens.caminho("..\\..\\banco.db"));
    }
}
