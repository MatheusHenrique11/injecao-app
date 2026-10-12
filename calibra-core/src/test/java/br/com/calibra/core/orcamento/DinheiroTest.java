package br.com.calibra.core.orcamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/** Reais em centavos inteiros, no formato brasileiro. */
class DinheiroTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "1.234,56|123456",
        "1234,5|123450",
        "R$ 1.234,56|123456",
        "1234|123400",
        "0,99|99",
        "' 12 '|1200"})
    void leValoresDigitados(String texto, long centavos) {
        assertEquals(new Dinheiro(centavos), Dinheiro.ler(texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.5", "1,234", "1.23,45", "abc", "1,2,3"})
    void recusaValoresAmbiguos(String texto) {
        NumberFormatException erro = assertThrows(NumberFormatException.class, () -> Dinheiro.ler(texto));
        assertTrue(erro.getMessage().contains("vírgula"), erro.getMessage());
    }

    @Test
    void escreveEmReais() {
        assertEquals("R$ 1.234,56", new Dinheiro(123456).formatar());
        assertEquals("0,05", new Dinheiro(5).formatarSemSimbolo());
        assertEquals(new Dinheiro(150), new Dinheiro(100).mais(new Dinheiro(50)));
        assertThrows(NumberFormatException.class, () -> Dinheiro.ler(" "));
    }
}
