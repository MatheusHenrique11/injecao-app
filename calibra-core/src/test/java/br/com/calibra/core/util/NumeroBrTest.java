package br.com.calibra.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class NumeroBrTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "1050|1050",
        "1.050|1050",
        "3,5|3.5",
        "1.050,5|1050.5",
        "-2,5|-2.5",
        "' 13,81 '|13.81",
        "0,80|0.8",
        "12.000|12000"})
    void leNumerosNoFormatoBrasileiro(String texto, double esperado) {
        assertEquals(esperado, NumeroBr.ler(texto), 1e-12);
    }

    /** "3.5" viraria 35 se o ponto fosse lido como milhar: o app recusa e explica. */
    @ParameterizedTest
    @ValueSource(strings = {"3.5", "10.50", "1.05", "1,2,3", ",5", "1 050", "abc"})
    void recusaNumerosAmbiguosOuInvalidos(String texto) {
        NumberFormatException erro = assertThrows(NumberFormatException.class, () -> NumeroBr.ler(texto));
        assertTrue(erro.getMessage().contains("vírgula"), erro.getMessage());
    }

    @Test
    void recusaCampoVazio() {
        assertThrows(NumberFormatException.class, () -> NumeroBr.ler(""));
        assertThrows(NumberFormatException.class, () -> NumeroBr.ler("   "));
        assertThrows(NumberFormatException.class, () -> NumeroBr.ler(null));
    }

    @Test
    void formataComVirgulaEPontoDeMilhar() {
        assertEquals("1.057,8", NumeroBr.formatar(1057.8, 1));
        assertEquals("0,80", NumeroBr.formatar(0.8, 2));
        assertEquals("2,38", NumeroBr.formatar(2.375, 2));
        assertEquals("—", NumeroBr.formatar(Double.NaN, 2));
    }

    @Test
    void formataComCasasEntreMinimoEMaximo() {
        assertEquals("10,5", NumeroBr.formatar(10.5, 1, 3));
        assertEquals("9,75", NumeroBr.formatar(9.75, 1, 3));
        assertEquals("2,0", NumeroBr.formatar(2.0000000001, 1, 3));
        assertEquals("0,72", NumeroBr.formatar(0.72, 2, 3));
        assertEquals("0,725", NumeroBr.formatar(0.7249999, 2, 3));
        assertEquals("1.050", NumeroBr.formatar(1050, 0, 2));
        assertEquals("—", NumeroBr.formatar(Double.NaN, 1, 3));
        // O texto formatado volta ao mesmo número na leitura.
        assertEquals(9.75, NumeroBr.ler(NumeroBr.formatar(9.75, 1, 3)), 1e-12);
    }

    @Test
    void formataEscolhendoAsCasasPelaGrandeza() {
        assertEquals("1.058", NumeroBr.formatar(1057.8));
        assertEquals("241,0", NumeroBr.formatar(241.04));
        assertEquals("37,57", NumeroBr.formatar(37.574));
        assertEquals("0,809", NumeroBr.formatar(0.809));
        assertEquals("0,000500", NumeroBr.formatar(0.0005));
        assertEquals("0", NumeroBr.formatar(0.0));
        assertEquals("—", NumeroBr.formatar(Double.POSITIVE_INFINITY));
    }

    @Test
    void formataPercentual() {
        assertEquals("86,3%", NumeroBr.percentual(0.8633, 1));
        assertEquals("80%", NumeroBr.percentual(0.80, 0));
    }

    @Test
    void formataTamanhoDeArquivo() {
        assertEquals("1 KB", NumeroBr.tamanhoArquivo(300));
        assertEquals("35 KB", NumeroBr.tamanhoArquivo(35 * 1024));
        assertEquals("1,5 MB", NumeroBr.tamanhoArquivo(1_572_864));
        assertEquals("20,0 MB", NumeroBr.tamanhoArquivo(20L * 1024 * 1024));
    }

    /** Fase 4: o eixo de uma tabela, colado do software em linha, em coluna ou separado por ponto e vírgula. */
    @Test
    void leListaDeNumeros() {
        assertEquals(List.of(500.0, 1000.0, 1500.0), NumeroBr.lerLista("500 1.000 1.500"));
        assertEquals(List.of(500.0, 1000.0, 1500.0), NumeroBr.lerLista("500\t1000\t1500\n"));
        assertEquals(List.of(20.0, 30.5, 40.0), NumeroBr.lerLista("20\r\n30,5\r\n40"));
        assertEquals(List.of(20.0, 30.0), NumeroBr.lerLista(" 20 ; 30 "));
        assertEquals(List.of(), NumeroBr.lerLista("  "));
        NumberFormatException erro = assertThrows(NumberFormatException.class, () -> NumeroBr.lerLista("500 1.5 2000"));
        assertTrue(erro.getMessage().startsWith("número 2:"), erro.getMessage());
    }
}
