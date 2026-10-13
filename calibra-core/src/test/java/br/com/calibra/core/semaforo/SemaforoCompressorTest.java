package br.com.calibra.core.semaforo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.Fixtures;
import br.com.calibra.core.pecas.MapaCompressor;

/** Semáforo do compressor e do torque × câmbio (núcleo 1.1.0), nas fronteiras dos limiares. */
class SemaforoCompressorTest {

    private final MapaCompressor mapa = Fixtures.mapaFicticio();
    private final LimiaresSemaforo l = LimiaresSemaforo.padrao();

    private static MapaCompressor.PontoNoMapa ponto(double margemSurge, double margemChoke, Double rpm,
            Double eficiencia) {
        return new MapaCompressor.PontoNoMapa(0.31, 2.9, null, margemSurge, margemChoke, rpm, false, eficiencia);
    }

    private Cor cor(MapaCompressor.PontoNoMapa ponto) {
        return Semaforo.mapaCompressor("Fictício", mapa, ponto, null, l).cor();
    }

    @Test
    void verdeComEficienciaPertoDoPico() {
        Avaliacao avaliacao = Semaforo.mapaCompressor("Fictício", mapa, ponto(0.30, 0.20, 137_000.0, 0.77), null, l);
        assertEquals(Cor.VERDE, avaliacao.cor());
        assertEquals(0.77, avaliacao.valor(), 1e-12);
        assertTrue(avaliacao.mensagem().startsWith("Turbo Fictício — potência: eficiência de 77% (pico 77%)"),
                avaliacao.mensagem());
        assertTrue(avaliacao.mensagem().endsWith("informe a rotação de torque máximo."), avaliacao.mensagem());
    }

    @Test
    void fronteirasDoSurgeEDoChoke() {
        assertEquals(Cor.VERDE, cor(ponto(0.10, 0.20, 137_000.0, 0.77)), "10% do surge ainda é verde");
        assertEquals(Cor.AMARELO, cor(ponto(0.0999, 0.20, 137_000.0, 0.77)));
        assertEquals(Cor.VERDE, cor(ponto(0.30, 0.05, 137_000.0, 0.77)), "5% do choke ainda é verde");
        assertEquals(Cor.AMARELO, cor(ponto(0.30, 0.0499, 137_000.0, 0.77)));
    }

    @Test
    void fronteiraDaRotacao() {
        assertEquals(Cor.VERDE, cor(ponto(0.30, 0.20, 152_000.0, 0.77)), "95% de 160.000 ainda é verde");
        assertEquals(Cor.AMARELO, cor(ponto(0.30, 0.20, 152_001.0, 0.77)));
        MapaCompressor.PontoNoMapa acima = new MapaCompressor.PontoNoMapa(0.31, 2.9, null, 0.30, 0.20, null, true, 0.77);
        assertEquals(Cor.AMARELO, cor(acima), "acima das linhas desenhadas");
    }

    @Test
    void fronteiraDaEficiencia() {
        assertEquals(Cor.VERDE, cor(ponto(0.30, 0.20, 137_000.0, 0.72)), "5 pontos abaixo do pico ainda é verde");
        Avaliacao baixa = Semaforo.mapaCompressor("Fictício", mapa, ponto(0.30, 0.20, 137_000.0, 0.7199), null, l);
        assertEquals(Cor.AMARELO, baixa.cor());
        assertTrue(baixa.mensagem().contains("mais de 5 pontos abaixo do pico do mapa (77%)"), baixa.mensagem());
        assertEquals(Cor.AMARELO, cor(ponto(0.30, 0.20, 137_000.0, null)), "fora das ilhas");
    }

    @Test
    void valeAPiorCorDosDoisPontos() {
        MapaCompressor.PontoNoMapa fora = new MapaCompressor.PontoNoMapa(0.20, 2.9, "à esquerda da linha de surge",
                Double.NaN, Double.NaN, null, false, null);
        Avaliacao avaliacao = Semaforo.mapaCompressor("Fictício", mapa, ponto(0.30, 0.20, 137_000.0, 0.77), fora, l);
        assertEquals(Cor.VERMELHO, avaliacao.cor());
        assertTrue(avaliacao.mensagem().contains("potência: eficiência de 77%"), avaliacao.mensagem());
        assertTrue(avaliacao.mensagem().contains("torque: fora do mapa (à esquerda da linha de surge)"),
                avaliacao.mensagem());
    }

    @Test
    void semMapaFicaAmareloComONomeDoTurbo() {
        Avaliacao semMapa = Semaforo.compressorSemMapa("GT2860");
        assertEquals(Cor.AMARELO, semMapa.cor());
        assertTrue(semMapa.mensagem().startsWith("Turbo GT2860 sem mapa cadastrado"), semMapa.mensagem());
    }

    @Test
    void fronteirasDoTorqueCambio() {
        assertEquals(Cor.VERDE, Semaforo.torqueCambio(405, "teste", 450, l).cor(), "90% ainda é verde");
        assertEquals(Cor.AMARELO, Semaforo.torqueCambio(405.1, "teste", 450, l).cor());
        assertEquals(Cor.AMARELO, Semaforo.torqueCambio(450, "teste", 450, l).cor(), "100% ainda é amarelo");
        Avaliacao acima = Semaforo.torqueCambio(450.1, "teste", 450, l);
        assertEquals(Cor.VERMELHO, acima.cor());
        assertEquals(Indicador.TORQUE_CAMBIO, acima.indicador());
        assertTrue(acima.mensagem().startsWith("Torque estimado de 450 N·m = 100% do limite do câmbio teste (450 N·m)"),
                acima.mensagem());
    }

    @Test
    void limiaresNovosTemPadraoEJsonAntigoRecebeOPadrao() {
        assertEquals(0.10, l.compressorMargemSurge(), 0.0);
        assertEquals(0.05, l.compressorMargemChoke(), 0.0);
        assertEquals(0.95, l.compressorRotacaoFator(), 0.0);
        assertEquals(0.05, l.compressorEficienciaPontos(), 0.0);
        assertEquals(0.90, l.cambioVerdeAte(), 0.0);
        assertEquals(1.00, l.cambioAmareloAte(), 0.0);
        // Um JSON gravado antes do núcleo 1.1.0 chega com zero nos campos novos.
        LimiaresSemaforo antigo = new LimiaresSemaforo(0.80, 0.85, 0.30, 0.15, 13, 22, 1.15, 20, 25, 1.2, 1.0, 0.90, 0.98,
                0, 0, 0, 0, 0, 0);
        assertEquals(l, antigo);
    }
}
