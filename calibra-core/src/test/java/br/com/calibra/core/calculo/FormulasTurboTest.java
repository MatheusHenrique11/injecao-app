package br.com.calibra.core.calculo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.unidades.Conversoes;

/** Fórmulas novas do núcleo 1.1.0: vazão corrigida, MAP ao contrário e potência pela vazão de ar. */
class FormulasTurboTest {

    /**
     * Conta feita à mão com a referência da Garrett (545 °R e 13,95 psia): Wa = 57,31 lb/min, T1 = 77 °F (536,67 °R)
     * e P1 = 13,7 psia. raiz(536,67 / 545) = 0,992328; 13,7 / 13,95 = 0,982079; Wc = 57,31 × 0,992328 / 0,982079 =
     * 57,908 lb/min.
     */
    @Test
    void vazaoCorrigidaComAReferenciaDaGarrett() {
        assertEquals(57.908, Formulas.vazaoCorrigida(57.31, 536.67, 13.7, 545.0, 13.95), 0.001);
    }

    /** A mesma conta em SI (kg/s, K e kPa) dá a mesma vazão corrigida: a fórmula não depende da unidade. */
    @Test
    void vazaoCorrigidaEmSi() {
        double kgS = Formulas.vazaoCorrigida(Conversoes.lbMinParaKgS(57.31), Conversoes.cParaK(25.0),
                Conversoes.psiParaKpa(13.7), 545.0 / 1.8, Conversoes.psiParaKpa(13.95));
        assertEquals(57.908, Conversoes.kgSParaLbMin(kgS), 0.001);
    }

    @Test
    void mapAoContrarioDevolveAVazao() {
        double map = Formulas.mapPsia(57.31, 130.0, 0.80, 6000, 348);
        assertEquals(57.31, Formulas.vazaoArPeloMap(map, 130.0, 0.80, 6000, 348), 1e-9);
    }

    @Test
    void potenciaPelaVazaoEOInversoDaVazaoPelaPotencia() {
        assertEquals(394.5, Formulas.potenciaPorKar(Formulas.vazaoArPorKar(394.5, 10.5), 10.5), 1e-9);
        double wa = Formulas.vazaoArPorBsfc(650, 11.5, 0.46);
        assertEquals(650.0, Formulas.potenciaPorBsfc(wa, 11.5, 0.46), 1e-9);
    }

    @Test
    void mapPelaPressaoSomaAAtmosferica() {
        assertEquals(194.3, Formulas.mapPelaPressao(94.3, 100.0), 1e-12);
    }
}
