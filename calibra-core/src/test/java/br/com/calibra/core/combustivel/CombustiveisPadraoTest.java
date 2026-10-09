package br.com.calibra.core.combustivel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tabela da SPEC 7.2, tolerância de 1%. */
class CombustiveisPadraoTest {

    @ParameterizedTest
    @CsvSource({"E32,12.80,0.756", "E25,13.21,0.752", "EHC,8.37,0.809", "EAC,9.0,0.789", "E85,9.75,0.78",
        "METANOL,6.42,0.792"})
    void afrEDensidadeDaTabela(String codigo, double afrEsperado, double densidade) {
        Combustivel c = CombustiveisPadrao.porCodigo(codigo).orElseThrow();
        assertEquals(afrEsperado, c.afrEstequiometrico(), afrEsperado * 0.01);
        assertEquals(densidade, c.densidadeKgL(), 1e-12);
    }

    @Test
    void gasolinaComEtanolPelaFracaoEmMassa() {
        GasolinaComEtanol e32 = new GasolinaComEtanol(0.32);
        assertEquals(0.3341, e32.fracaoMassicaEtanol(), 1e-4);
        assertEquals(12.796, e32.afrEstequiometrico(), 1e-3);
        assertEquals(14.7, new GasolinaComEtanol(0).afrEstequiometrico(), 1e-12);
        assertEquals(9.0, new GasolinaComEtanol(1).afrEstequiometrico(), 1e-12);
    }

    @Test
    void lambdasPadrao() {
        assertEquals(0.80, CombustiveisPadrao.E32.lambdaPadrao(true), 1e-12);
        assertEquals(0.87, CombustiveisPadrao.E32.lambdaPadrao(false), 1e-12);
        assertEquals(0.65, CombustiveisPadrao.METANOL.lambdaPadrao(true), 1e-12);
        assertEquals(0.70, CombustiveisPadrao.METANOL.lambdaPadrao(false), 1e-12);
    }

    @Test
    void metanolTemAvisoEOsOutrosNao() {
        assertNotNull(CombustiveisPadrao.METANOL.avisoLambda());
        assertNull(CombustiveisPadrao.E32.avisoLambda());
    }

    @Test
    void e32TemVigenciaFonteEVersao() {
        assertEquals(LocalDate.of(2026, 8, 1), CombustiveisPadrao.E32.vigenteDesde());
        assertEquals(1, CombustiveisPadrao.E32.versao());
        assertTrue(CombustiveisPadrao.E32.fonte().contains("SPEC 7.2"));
        assertTrue(CombustiveisPadrao.E32.eGasolina());
        assertEquals(6, CombustiveisPadrao.todos().size());
    }

    @Test
    void recusaValoresImpossiveis() {
        assertThrows(IllegalArgumentException.class, () -> new GasolinaComEtanol(1.2));
        assertThrows(IllegalArgumentException.class, () -> new EtanolHidratado(0));
        assertThrows(IllegalArgumentException.class, () -> new AfrFixo(-1));
    }
}
