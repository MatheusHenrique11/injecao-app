package br.com.calibra.core.pecas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.Fixtures;

/**
 * Mapa do compressor (núcleo 1.1.0), com o mapa fictício dos testes. As contas esperadas foram feitas à mão sobre os
 * pontos do mapa (interpolação linear entre os pontos marcados).
 */
class MapaCompressorTest {

    private final MapaCompressor mapa = Fixtures.mapaFicticio();

    @Test
    void pontoNaIlhaCentralComMargensERotacao() {
        MapaCompressor.PontoNoMapa ponto = mapa.avaliar(0.3104, 2.91);
        assertTrue(ponto.dentro());
        assertEquals(0.77, ponto.eficiencia(), 1e-12);
        // Surge a PR 2,91: entre (0,22; 2,8) e (0,26; 3,4) → 0,22 + 0,11/0,6 × 0,04 = 0,227333.
        assertEquals((0.3104 - 0.227333) / 0.227333, ponto.margemSurge(), 1e-5);
        // Choke a PR 2,91: 0,44 (linha vertical) → (0,44 − 0,3104) / 0,44.
        assertEquals((0.44 - 0.3104) / 0.44, ponto.margemChoke(), 1e-9);
        // Linha de 110.000 na vazão 0,3104: PR 2,256125; de 140.000: 2,969077 → 110.000 + 0,917138 × 30.000.
        assertEquals(137_514.1, ponto.rotacaoRpm(), 1.0);
        assertFalse(ponto.acimaDasLinhas());
        assertEquals(0.77, mapa.eficienciaPico(), 1e-12);
    }

    @Test
    void ilhasDeFora() {
        assertEquals(0.70, mapa.avaliar(0.27, 2.91).eficiencia(), 1e-12, "fora da central e da do meio");
        MapaCompressor.PontoNoMapa semIlha = mapa.avaliar(0.15, 1.4);
        assertTrue(semIlha.dentro(), "entre o surge e o choke");
        assertNull(semIlha.eficiencia(), "fora das ilhas marcadas");
    }

    @Test
    void foraDoMapa() {
        assertEquals("à esquerda da linha de surge", mapa.avaliar(0.20, 2.91).foraDoMapa());
        assertEquals("à direita da linha de choke", mapa.avaliar(0.45, 2.0).foraDoMapa());
        assertEquals("PR 3,50 acima do topo do mapa (3,40)", mapa.avaliar(0.35, 3.5).foraDoMapa());
        MapaCompressor.PontoNoMapa fora = mapa.avaliar(0.20, 2.91);
        assertTrue(Double.isNaN(fora.margemSurge()));
        assertNull(fora.eficiencia());
    }

    @Test
    void pontoSobreAsLinhasEstaDentro() {
        assertEquals(0.0, mapa.avaliar(0.16, 2.0).margemSurge(), 1e-12, "em cima do surge");
        assertEquals(0.0, mapa.avaliar(0.42, 2.0).margemChoke(), 1e-12, "em cima do choke");
    }

    @Test
    void abaixoDoPrimeiroPontoValeOPrimeiro() {
        MapaCompressor.PontoNoMapa baixo = mapa.avaliar(0.12, 1.1);
        assertTrue(baixo.dentro());
        assertEquals(0.20, baixo.margemSurge(), 1e-12, "surge de 0,10 abaixo de PR 1,3");
        assertNull(baixo.rotacaoRpm(), "abaixo da menor linha de rotação");
    }

    @Test
    void acimaDaLinhaDeRotacaoMaxima() {
        // Na vazão 0,38, a linha de 160.000 (a rotação máxima) passa em PR 3,35.
        assertEquals("acima da linha de rotação máxima (160.000 rpm)", mapa.avaliar(0.38, 3.36).foraDoMapa());
        MapaCompressor semMaxima = Fixtures.mapaFicticio(0.70, 0.74, 0.77, null);
        MapaCompressor.PontoNoMapa acima = semMaxima.avaliar(0.38, 3.36);
        assertTrue(acima.dentro(), "sem rotação máxima informada, o ponto continua no mapa");
        assertTrue(acima.acimaDasLinhas());
        assertNull(acima.rotacaoRpm());
    }

    @Test
    void rotacaoInterpoladaAcimaDaMaximaEstaFora() {
        MapaCompressor baixa = Fixtures.mapaFicticio(0.70, 0.74, 0.77, 130_000.0);
        assertEquals("acima da rotação máxima do turbo (130.000 rpm)", baixa.avaliar(0.3104, 2.91).foraDoMapa());
    }

    @Test
    void contornoDaIlha() {
        List<MapaCompressor.PontoMapa> quadrado = List.of(new MapaCompressor.PontoMapa(1, 1),
                new MapaCompressor.PontoMapa(2, 1), new MapaCompressor.PontoMapa(2, 2), new MapaCompressor.PontoMapa(1, 2));
        assertTrue(MapaCompressor.dentroDoContorno(quadrado, 1.5, 1.5));
        assertFalse(MapaCompressor.dentroDoContorno(quadrado, 2.5, 1.5));
        assertFalse(MapaCompressor.dentroDoContorno(quadrado, 1.5, 2.5));
    }

    @Test
    void validarApontaOQueFalta() {
        MapaCompressor incompleto = new MapaCompressor(UnidadeVazao.KG_S, 0, 0,
                List.of(new MapaCompressor.PontoMapa(0.1, 1.5)), List.of(),
                List.of(new MapaCompressor.Ilha(76, List.of(new MapaCompressor.PontoMapa(0.2, 2)))),
                List.of(new MapaCompressor.LinhaRotacao(100_000, List.of(new MapaCompressor.PontoMapa(0.2, 2)))),
                -1.0);
        assertEquals(List.of(
                "Informe a temperatura e a pressão de referência do mapa (estão na legenda do fabricante).",
                "Marque pelo menos 2 pontos na linha de surge.",
                "Marque pelo menos 2 pontos na linha de choke.",
                "A ilha de 7.600% precisa de pelo menos 3 pontos.",
                "A eficiência de cada ilha fica entre 0 e 1 (0,76 = 76%).",
                "A linha de 100.000 rpm precisa de pelo menos 2 pontos.",
                "A rotação máxima do turbo deve ser maior que zero."), incompleto.validar());
        assertTrue(mapa.validar().isEmpty(), mapa.validar()::toString);
    }

    @Test
    void eixoOriginalConverte() {
        assertEquals(0.4536, UnidadeVazao.LB_MIN.paraKgS(60.0), 1e-4, "60 lb/min = 0,4536 kg/s");
        assertEquals(60.0, UnidadeVazao.LB_MIN.deKgS(UnidadeVazao.LB_MIN.paraKgS(60.0)), 1e-9);
        assertEquals(0.25, UnidadeVazao.KG_S.paraKgS(0.25), 0.0);
    }
}
