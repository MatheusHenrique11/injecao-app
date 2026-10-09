package br.com.calibra.core.memoria;

import static br.com.calibra.core.memoria.MemoriaCalculo.e;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MemoriaCalculoTest {

    @Test
    void anotaPassosAvisosEVersaoDoNucleo() {
        MemoriaCalculo memoria = new MemoriaCalculo();
        double wa = memoria.passo("Vazão de ar Wa", "Wa = P_hp / k_ar", 394.528 / 10.5, "lb/min",
                e("P_hp", 394.528, "hp"), e("k_ar", 10.5, "hp por lb/min"));
        memoria.aviso(NivelAviso.AMARELO, "Exemplo de aviso.");

        Resultado<Double> resultado = memoria.resultado(wa);

        assertEquals(37.574, resultado.valor(), 1e-3);
        assertEquals(1, resultado.memoria().size());
        Passo passo = resultado.memoria().getFirst();
        assertEquals("Wa = P_hp / k_ar", passo.formula());
        assertEquals("P_hp", passo.entradas().getFirst().simbolo());
        assertEquals("lb/min", passo.resultado().unidade());
        assertEquals(1, resultado.avisos().size());
        assertEquals(VersaoNucleo.ATUAL, resultado.versaoNucleo());
    }
}
