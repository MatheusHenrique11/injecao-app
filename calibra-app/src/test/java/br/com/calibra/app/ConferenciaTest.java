package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Conferência do pacote (Inicio --conferir), aqui no Java de desenvolvimento. */
class ConferenciaTest {

    @TempDir
    Path pasta;

    private static String imprimir(List<Conferencia.Item> itens, int codigoEsperado) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        assertEquals(codigoEsperado, Conferencia.imprimir(itens, new PrintStream(saida, true, StandardCharsets.UTF_8)));
        return saida.toString(StandardCharsets.UTF_8);
    }

    @Test
    void tudoCertoNoJavaDeDesenvolvimento() {
        List<Conferencia.Item> itens = Conferencia.executar(pasta);
        assertEquals(5, itens.size());
        assertTrue(itens.stream().allMatch(Conferencia.Item::ok), itens::toString);
        String texto = imprimir(itens, 0);
        assertTrue(texto.contains("OK      Vírgula decimal (pt-BR): 1.234,5 · R$ 1.234,56"), texto);
        assertTrue(texto.contains("OK      Banco SQLite e migrações: banco na versão 16, 6 combustíveis"), texto);
        assertTrue(texto.contains("OK      PDF com fontes e imagens"), texto);
        assertTrue(texto.contains("OK      Páginas para imprimir: "), texto);
        assertTrue(texto.contains("pontos (A4 a 72 DPI)"), texto);
        assertTrue(texto.endsWith("Tudo certo." + System.lineSeparator()), texto);
    }

    @Test
    void itemComFalhaDaCodigo1() {
        String texto = imprimir(List.of(new Conferencia.Item("Vírgula decimal (pt-BR)", false, "1,234.5")), 1);
        assertTrue(texto.contains("FALHOU  Vírgula decimal (pt-BR): 1,234.5"), texto);
        assertTrue(texto.contains("Há itens com falha."), texto);
    }
}
