package br.com.calibra.app.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.app.catalogo.DesenhoMapa.TipoElemento;
import br.com.calibra.core.pecas.CalibracaoImagem;
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.pecas.UnidadeVazao;
import br.com.calibra.core.unidades.Conversoes;

/** Digitalizador do mapa, sem tela: calibração dos eixos, pontos clicados, desfazer e reabrir. */
class DesenhoMapaTest {

    /** Eixo X: pixel 100 = 10 lb/min e pixel 700 = 60 lb/min. Eixo Y (de cima para baixo): 500 = PR 1,0 e 100 = 3,0. */
    private static final CalibracaoImagem CALIBRACAO = new CalibracaoImagem(100, 10, 700, 60, 500, 1.0, 100, 3.0);

    @Test
    void calibracaoDosEixos() {
        assertEquals(35.0, CALIBRACAO.vazao(400), 1e-12, "meio do eixo X");
        assertEquals(2.0, CALIBRACAO.razaoPressao(300), 1e-12, "meio do eixo Y, que cresce para cima");
        assertEquals(400.0, CALIBRACAO.xPixel(35.0), 1e-12);
        assertEquals(300.0, CALIBRACAO.yPixel(2.0), 1e-12);
        assertEquals(1.5, CALIBRACAO.razaoPressao(400), 1e-12);
        assertEquals(List.of("Eixo de vazão: marque dois pontos diferentes, com valores diferentes."),
                new CalibracaoImagem(100, 10, 100, 60, 500, 1.0, 100, 3.0).validar());
    }

    @Test
    void cliquesViramOMapaDoNucleo() {
        DesenhoMapa desenho = new DesenhoMapa();
        desenho.comecar(TipoElemento.SURGE, Double.NaN);
        desenho.adicionar(160, 400);
        desenho.adicionar(220, 300);
        desenho.comecar(TipoElemento.CHOKE, Double.NaN);
        desenho.adicionar(580, 400);
        desenho.adicionar(640, 300);
        desenho.comecar(TipoElemento.ILHA, 0.76);
        desenho.adicionar(300, 380);
        desenho.adicionar(450, 380);
        desenho.adicionar(380, 320);
        desenho.comecar(TipoElemento.ROTACAO, 120_000);
        desenho.adicionar(200, 330);
        desenho.adicionar(600, 350);

        MapaCompressor mapa = desenho.paraMapa(CALIBRACAO, UnidadeVazao.LB_MIN, 302.78, 96.18, 150_000.0);
        // Pixel 160 → 15 lb/min; 400 → PR 1,5.
        assertEquals(Conversoes.lbMinParaKgS(15), mapa.surge().getFirst().vazaoKgS(), 1e-12);
        assertEquals(1.5, mapa.surge().getFirst().razaoPressao(), 1e-12);
        assertEquals(2, mapa.choke().size());
        assertEquals(0.76, mapa.ilhas().getFirst().eficiencia(), 0.0);
        assertEquals(3, mapa.ilhas().getFirst().contorno().size());
        assertEquals(120_000, mapa.rotacoes().getFirst().rpm(), 0.0);
        assertEquals(UnidadeVazao.LB_MIN, mapa.eixoOriginal());
        assertTrue(mapa.validar().isEmpty(), mapa.validar()::toString);
    }

    @Test
    void desfazerTiraOUltimoPontoEOElementoVazio() {
        DesenhoMapa desenho = new DesenhoMapa();
        desenho.comecar(TipoElemento.SURGE, Double.NaN);
        desenho.adicionar(160, 400);
        desenho.comecar(TipoElemento.ILHA, 0.76);
        desenho.adicionar(300, 380);
        assertEquals(2, desenho.elementos().size());
        assertTrue(desenho.desfazer());
        assertEquals(1, desenho.elementos().size(), "a ilha ficou sem pontos e saiu");
        desenho.adicionar(310, 390);
        assertEquals(TipoElemento.ILHA, desenho.elementos().getLast().tipo(), "continua marcando a mesma ilha");
        assertTrue(desenho.desfazer());
        assertTrue(desenho.desfazer());
        assertTrue(desenho.elementos().isEmpty());
        assertFalse(desenho.desfazer(), "nada mais a desfazer");
    }

    @Test
    void surgeEChokeSaoUmaLinhaSo() {
        DesenhoMapa desenho = new DesenhoMapa();
        desenho.comecar(TipoElemento.SURGE, Double.NaN);
        desenho.adicionar(160, 400);
        desenho.comecar(TipoElemento.CHOKE, Double.NaN);
        desenho.adicionar(580, 400);
        desenho.comecar(TipoElemento.SURGE, Double.NaN);
        desenho.adicionar(220, 300);
        assertEquals(2, desenho.elementos().size());
        assertEquals(2, desenho.elementos().getFirst().pontos().size(), "voltar ao surge continua a mesma linha");
    }

    @Test
    void semEscolherOQueMarcarORecusaOClique() {
        assertThrows(IllegalStateException.class, () -> new DesenhoMapa().adicionar(1, 1));
    }

    @Test
    void reabrirUmMapaGravadoVoltaAosMesmosPixels() {
        MapaCompressor mapa = ExemplosCatalogo.mapaFicticio();
        CalibracaoImagem calibracao = new CalibracaoImagem(100, 10, 700, 60, 500, 1.0, 100, 3.0);
        DesenhoMapa desenho = DesenhoMapa.de(mapa, calibracao);
        MapaCompressor deNovo = desenho.paraMapa(calibracao, mapa.eixoOriginal(), mapa.temperaturaReferenciaK(),
                mapa.pressaoReferenciaKpa(), mapa.rotacaoMaximaRpm());
        assertEquals(mapa.surge().size(), deNovo.surge().size());
        for (int i = 0; i < mapa.surge().size(); i++) {
            assertEquals(mapa.surge().get(i).vazaoKgS(), deNovo.surge().get(i).vazaoKgS(), 1e-12);
            assertEquals(mapa.surge().get(i).razaoPressao(), deNovo.surge().get(i).razaoPressao(), 1e-12);
        }
        assertEquals(mapa.ilhas().getFirst().eficiencia(), deNovo.ilhas().getFirst().eficiencia(), 0.0);
        assertEquals(mapa.rotacoes().getFirst().rpm(), deNovo.rotacoes().getFirst().rpm(), 0.0);
        assertEquals(null, desenho.marcando(), "reabre sem nada escolhido para marcar");
    }

    @Test
    void unidadesDaReferencia() {
        assertEquals(302.778, UnidadesMapa.Temperatura.RANKINE.paraK(545), 0.001, "545 °R da Garrett");
        assertEquals(302.778, UnidadesMapa.Temperatura.FAHRENHEIT.paraK(85.33), 0.001);
        assertEquals(29.63, UnidadesMapa.Temperatura.CELSIUS.deK(302.778), 0.01);
        assertEquals(96.18, UnidadesMapa.Pressao.PSIA.paraKpa(13.95), 0.01, "13,95 psia da Garrett");
        assertEquals(100.0, UnidadesMapa.Pressao.BAR.paraKpa(1.0), 1e-9);
        assertEquals(13.95, UnidadesMapa.Pressao.PSIA.deKpa(UnidadesMapa.Pressao.PSIA.paraKpa(13.95)), 1e-9);
    }
}
