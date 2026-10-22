package br.com.calibra.relatorios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import br.com.calibra.core.unidades.UnidadesPreferidas;
import br.com.calibra.core.unidades.UnidadesPreferidas.Potencia;
import br.com.calibra.core.unidades.UnidadesPreferidas.Pressao;
import br.com.calibra.core.unidades.UnidadesPreferidas.Temperatura;
import br.com.calibra.core.unidades.UnidadesPreferidas.Vazao;

/**
 * Fase 4 (decisão 289): as unidades preferidas mudam só a ordem do texto (a escolhida primeiro). Sem preferência, o
 * texto é o de sempre, com todas as unidades.
 */
class ApresentacaoTest {

    @AfterEach
    void voltarAoPadrao() {
        Apresentacao.usarUnidades(UnidadesPreferidas.padrao());
    }

    @Test
    void semPreferenciaOTextoDeSempre() {
        assertEquals("241,0 kPa · 2,41 bar · 34,95 psia", Apresentacao.pressaoAbsoluta(241));
        assertEquals("1,47 bar · 21,3 psi · 147 kPa", Apresentacao.pressaoManometrica(147));
        assertEquals("55,0 °C · 131 °F", Apresentacao.temperatura(55));
        assertEquals("400,0 cv · 394,5 hp · 294,2 kW", Apresentacao.potencia(294.2));
        assertEquals("1.050 cm³/min · 97,2 lb/h", Apresentacao.vazaoBico(1050, 0.70));
    }

    @Test
    void aEscolhidaVemPrimeiro() {
        Apresentacao.usarUnidades(new UnidadesPreferidas(Pressao.PSI, Temperatura.FAHRENHEIT, Potencia.KW,
                Vazao.G_MIN));
        assertEquals("34,95 psia · 241,0 kPa · 2,41 bar", Apresentacao.pressaoAbsoluta(241));
        assertEquals("21,3 psi · 1,47 bar · 147 kPa", Apresentacao.pressaoManometrica(147));
        assertEquals("131 °F · 55,0 °C", Apresentacao.temperatura(55));
        assertEquals("294,2 kW · 400,0 cv · 394,5 hp", Apresentacao.potencia(294.2));
        assertEquals("735 g/min · 1.050 cm³/min · 97,2 lb/h", Apresentacao.vazaoBico(1050, 0.70),
                "g/min no fluido de teste: 1.050 cm³/min × 0,70 g/cm³");

        Apresentacao.usarUnidades(new UnidadesPreferidas(Pressao.BAR, null, null, Vazao.LB_H));
        assertEquals("2,41 bar · 241,0 kPa · 34,95 psia", Apresentacao.pressaoAbsoluta(241));
        assertEquals("1,47 bar · 21,3 psi · 147 kPa", Apresentacao.pressaoManometrica(147), "bar já vem primeiro");
        assertEquals("97,2 lb/h · 1.050 cm³/min", Apresentacao.vazaoBico(1050, 0.70));
    }
}
