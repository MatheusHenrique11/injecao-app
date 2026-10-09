package br.com.calibra.core.calculo;

import static br.com.calibra.core.Fixtures.assertProximo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Um teste por fórmula da SPEC 7.1, com valores conferidos à mão, e o caso golden A (7.6). */
class FormulasTest {

    @Test
    void potenciaEmHp() {
        assertEquals(394.528, Formulas.potenciaHp(400), 1e-9);
    }

    @Test
    void vazaoDeArPorKar() {
        assertEquals(37.57410, Formulas.vazaoArPorKar(394.528, 10.5), 1e-5);
    }

    @Test
    void vazaoDeArPorBsfc() {
        assertEquals(57.30833, Formulas.vazaoArPorBsfc(650, 11.5, 0.46), 1e-5);
    }

    @Test
    void combustivelEmLbH() {
        assertEquals(336.6855, Formulas.combustivelLbH(37.57410, 8.37, 0.80), 1e-3);
    }

    @Test
    void bsfcDeConferencia() {
        assertEquals(0.85339, Formulas.bsfc(336.6855, 394.528), 1e-5);
    }

    @Test
    void vazaoPorBico() {
        assertEquals(47.725, Formulas.vazaoPorBico(152.72, 4, 0.80), 1e-9);
    }

    @Test
    void correcaoPorPressao() {
        assertEquals(1212.4356, Formulas.corrigirPorPressao(1050, 300, 400), 1e-3);
        assertEquals(1050.0, Formulas.corrigirPorPressao(1050, 300, 300), 1e-12);
    }

    @Test
    void vazaoNoCombustivelReal() {
        assertEquals(976.707, Formulas.vazaoNoCombustivel(1050, 0.70, 0.809), 1e-3);
    }

    @Test
    void vazaoDaBomba() {
        assertEquals(188.875, Formulas.vazaoBombaLh(152.8, 0.809), 1e-3);
    }

    @Test
    void pressaoAtmosfericaPelaAltitude() {
        assertEquals(101.325, Formulas.pressaoAtmosfericaKpa(0), 1e-12);
        assertEquals(94.32, Formulas.pressaoAtmosfericaKpa(600), 0.01);
    }

    @Test
    void temperaturaNaSaidaDoCompressor() {
        assertEquals(445.93, Formulas.temperaturaSaidaCompressorK(298.15, 2.91, 0.72), 0.05);
    }

    @Test
    void temperaturaNoColetor() {
        assertEquals(54.556, Formulas.temperaturaColetor(172.78, 0.80, 25), 1e-3);
    }

    @Test
    void massaDeArPorCilindro() {
        assertEquals(1.21696e-3, Formulas.massaArCilindroKg(0.95, 241000, 0.0005, 327.7), 1e-8);
    }

    @Test
    void massaDeCombustivelPorInjecao() {
        assertEquals(1.81744e-4, Formulas.massaCombustivelPorInjecao(1.21696e-3, 8.37, 0.80, 1), 1e-9);
        assertEquals(0.90872e-4, Formulas.massaCombustivelPorInjecao(1.21696e-3, 8.37, 0.80, 2), 1e-9);
    }

    @Test
    void tempoDeInjecao() {
        // bico de 1.050 cm³/min (fluido 0,70 kg/L) em etanol 0,809 kg/L = 790,16 g/min = 0,0131693 kg/s
        assertEquals(14.8006, Formulas.tempoInjecaoMs(1.81744e-4, 0.0131693, 1.0), 1e-3);
    }

    @Test
    void cicloEDutyCycle() {
        assertEquals(17.142857, Formulas.cicloMs(7000), 1e-6);
        assertEquals(0.86337, Formulas.dutyCycle(14.8006, 1, 7000), 1e-5);
        assertEquals(2 * 0.86337, Formulas.dutyCycle(14.8006, 2, 7000), 1e-5);
    }

    @Test
    void esforco() {
        assertEquals(25.217, Formulas.bmepBar(294.2, 2.0, 7000), 1e-3);
        assertEquals(21.6533, Formulas.velocidadeMediaPistao(0.0928, 7000), 1e-4);
        assertEquals(401.331, Formulas.torqueNm(294.2, 7000), 1e-3);
    }

    @Test
    void metaNaRodaParaVirabrequim() {
        assertEquals(352.941, Formulas.potenciaNoVirabrequim(300, 0.15), 1e-3);
        assertEquals(300.0, Formulas.potenciaNoVirabrequim(300, 0.0), 1e-12);
    }

    /**
     * Caso golden A (SPEC 7.6): 650 hp, A/F 11,5, BSFC 0,46 → Wa = 57,31 lb/min. Com VE 0,80, Tm 130 °F,
     * 6.000 rpm e 348 pol³ → MAP = 25,89 psia. PR com 2 psi de perda e 13,7 psia na entrada → 2,04.
     */
    @Test
    void casoGoldenA_garrett() {
        double wa = Formulas.vazaoArPorBsfc(650, 11.5, 0.46);
        assertProximo(57.31, wa, "Wa");

        double map = Formulas.mapPsia(wa, 130, 0.80, 6000, 348);
        assertProximo(25.89, map, "MAP");

        double pr = Formulas.razaoPressao(map, 2, 14.7, 1);
        assertProximo(2.04, pr, "PR");
    }
}
