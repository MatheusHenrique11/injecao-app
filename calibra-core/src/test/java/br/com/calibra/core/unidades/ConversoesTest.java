package br.com.calibra.core.unidades;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConversoesTest {

    @Test
    void constantesDaSpec() {
        assertEquals(294.2, Conversoes.cvParaKw(400), 1e-9);
        assertEquals(394.528, Conversoes.cvParaHp(400), 1e-9);
        assertEquals(6.894757, Conversoes.psiParaKpa(1), 1e-12);
        assertEquals(0.453592, Conversoes.lbParaKg(1), 1e-12);
        assertEquals(61.0237, Conversoes.litrosParaPol3(1), 1e-12);
    }

    @Test
    void valoresConhecidos() {
        assertEquals(394.528, Conversoes.kwParaHp(294.2), 1e-9);
        assertEquals(54.4444, Conversoes.fParaC(130), 1e-4);
        assertEquals(298.15, Conversoes.cParaK(25), 1e-12);
        assertEquals(300.0, Conversoes.barParaKpa(3), 1e-12);
        assertEquals(60.0, Conversoes.cm3MinParaLh(1000), 1e-12);
        // 1.000 cm³/min de fluido a 0,70 kg/L = 42 kg/h = 92,59 lb/h
        assertEquals(92.594, Conversoes.cm3MinParaLbH(1000, 0.70), 1e-3);
        // 37,57 lb/min = 0,284 kg/s
        assertEquals(0.28403, Conversoes.lbMinParaKgS(37.57), 1e-5);
    }

    /** Teste de coerência da SPEC 7.6: converter unidade e voltar devolve o valor original. */
    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 1.0, 13.81, 241.0, 1057.8, 6002.0, -40.0})
    void converterEVoltarDevolveOValorOriginal(double valor) {
        List<DoubleUnaryOperator[]> pares = List.of(
                par(Conversoes::cvParaKw, Conversoes::kwParaCv),
                par(Conversoes::cvParaHp, Conversoes::hpParaCv),
                par(Conversoes::kwParaHp, Conversoes::hpParaKw),
                par(Conversoes::psiParaKpa, Conversoes::kpaParaPsi),
                par(Conversoes::barParaKpa, Conversoes::kpaParaBar),
                par(Conversoes::cParaK, Conversoes::kParaC),
                par(Conversoes::cParaF, Conversoes::fParaC),
                par(Conversoes::lbParaKg, Conversoes::kgParaLb),
                par(Conversoes::lbMinParaKgS, Conversoes::kgSParaLbMin),
                par(Conversoes::lbHParaKgH, Conversoes::kgHParaLbH),
                par(Conversoes::kgHParaGMin, Conversoes::gMinParaKgH),
                par(Conversoes::litrosParaPol3, Conversoes::pol3ParaLitros),
                par(Conversoes::cm3ParaLitros, Conversoes::litrosParaCm3),
                par(Conversoes::cm3MinParaLh, Conversoes::lhParaCm3Min));
        double tolerancia = 1e-9 * Math.max(1.0, Math.abs(valor));
        for (DoubleUnaryOperator[] par : pares) {
            double ida = par[0].applyAsDouble(valor);
            assertEquals(valor, par[1].applyAsDouble(ida), tolerancia);
        }
    }

    private static DoubleUnaryOperator[] par(DoubleUnaryOperator ida, DoubleUnaryOperator volta) {
        return new DoubleUnaryOperator[] {ida, volta};
    }
}
