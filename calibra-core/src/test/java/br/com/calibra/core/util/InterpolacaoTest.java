package br.com.calibra.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import br.com.calibra.core.util.Interpolacao.Ponto;
import org.junit.jupiter.api.Test;

class InterpolacaoTest {

    private final List<Ponto> curva = List.of(new Ponto(500, 180), new Ponto(300, 260), new Ponto(400, 220));

    @Test
    void interpolaEntrePontosMesmoForaDeOrdem() {
        assertEquals(240.0, Interpolacao.linear(curva, 350).orElseThrow(), 1e-12);
        assertEquals(190.0, Interpolacao.linear(curva, 475).orElseThrow(), 1e-12);
    }

    @Test
    void devolveOValorExatoNosPontos() {
        assertEquals(260.0, Interpolacao.linear(curva, 300).orElseThrow(), 1e-12);
        assertEquals(180.0, Interpolacao.linear(curva, 500).orElseThrow(), 1e-12);
    }

    @Test
    void naoExtrapola() {
        assertTrue(Interpolacao.linear(curva, 299).isEmpty());
        assertTrue(Interpolacao.linear(curva, 501).isEmpty());
        assertTrue(Interpolacao.linear(List.of(), 1).isEmpty());
    }

    @Test
    void umPontoSoServeApenasNoProprioX() {
        List<Ponto> unico = List.of(new Ponto(13.5, 1.0));
        assertEquals(1.0, Interpolacao.linear(unico, 13.5).orElseThrow(), 1e-12);
        assertTrue(Interpolacao.linear(unico, 12.0).isEmpty());
    }
}
