package br.com.calibra.core.pecas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Tabelas de tempo morto e curvas de bomba (dados fictícios, só de teste). */
class PecasTest {

    @Test
    void tempoMortoFixoValeParaQualquerCondicao() {
        TempoMorto fixo = TempoMorto.fixo(1.0);
        assertEquals(1.0, fixo.valorMs(null, 300).orElseThrow(), 1e-12);
        assertEquals(1.0, fixo.valorMs(9.0, 500).orElseThrow(), 1e-12);
    }

    @Test
    void tempoMortoDeUmaPressaoSoInterpolaNaTensao() {
        TempoMorto tabela = new TempoMorto(List.of(
                new TempoMorto.Ponto(12.0, 300, 1.20),
                new TempoMorto.Ponto(14.0, 300, 0.90)));
        assertEquals(1.05, tabela.valorMs(13.0, 300).orElseThrow(), 1e-12);
        assertEquals(1.05, tabela.valorMs(13.0, 400).orElseThrow(), 1e-12);
        assertTrue(tabela.valorMs(11.0, 300).isEmpty(), "não extrapola na tensão");
        assertTrue(tabela.valorMs(null, 300).isEmpty(), "tabela precisa da tensão");
    }

    @Test
    void tempoMortoDeDuasPressoesInterpolaNasDuas() {
        TempoMorto tabela = new TempoMorto(List.of(
                new TempoMorto.Ponto(12.0, 300, 1.20),
                new TempoMorto.Ponto(14.0, 300, 0.90),
                new TempoMorto.Ponto(12.0, 400, 1.30),
                new TempoMorto.Ponto(14.0, 400, 1.00)));
        assertEquals(1.10, tabela.valorMs(13.0, 350).orElseThrow(), 1e-12);
        assertTrue(tabela.valorMs(13.0, 450).isEmpty(), "não extrapola na pressão");
    }

    @Test
    void bombaInterpolaNaPressaoENaTensao() {
        Bomba bomba = new Bomba("teste", List.of(
                new Bomba.Ponto(300, 12.0, 180), new Bomba.Ponto(500, 12.0, 140),
                new Bomba.Ponto(300, 13.5, 200), new Bomba.Ponto(500, 13.5, 160)), "teste", null);
        assertEquals(190.0, bomba.vazaoLh(300, 12.75).orElseThrow(), 1e-9);
        assertEquals(180.0, bomba.vazaoLh(400, 13.5).orElseThrow(), 1e-9);
        assertTrue(bomba.vazaoLh(600, 13.5).isEmpty(), "não extrapola na pressão");
        assertTrue(bomba.vazaoLh(300, 14.0).isEmpty(), "não extrapola na tensão");
    }

    @Test
    void bombaDeUmaTensaoSoValeApenasNela() {
        Bomba bomba = new Bomba("só 12 V", List.of(new Bomba.Ponto(300, 12.0, 400), new Bomba.Ponto(600, 12.0, 350)),
                "teste", null);
        assertEquals(375.0, bomba.vazaoLh(450, 12.0).orElseThrow(), 1e-9);
        assertTrue(bomba.vazaoLh(450, 13.5).isEmpty());
    }
}
