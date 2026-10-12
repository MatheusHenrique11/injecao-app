package br.com.calibra.core.pecas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.cadastro.TipoCambio;

/** Peças novas do catálogo (bloco 2.3): intercooler, sensor e câmbio. Valores fictícios. */
class PecasDoCatalogoTest {

    @Test
    void intercoolerComEficienciaOpcional() {
        assertTrue(new Intercooler("IC teste", TipoIntercooler.AR_AR, null, "teste", null).validar().isEmpty());
        assertTrue(new Intercooler("IC teste", TipoIntercooler.AR_AGUA, 0.85, "teste", null).validar().isEmpty());
        assertEquals(List.of("Escolha o tipo do intercooler (ar-ar ou ar-água).",
                "A eficiência do intercooler deve ficar entre 0 e 1 (0,80 = 80%)."),
                new Intercooler("IC teste", null, 80.0, "teste", null).validar());
    }

    @Test
    void sensorComFaixa() {
        assertTrue(new Sensor("MAP 4 bar", TipoSensor.MAP, 10.0, 400.0, "teste", null).validar().isEmpty());
        assertEquals(List.of("A faixa mínima do sensor precisa ser menor que a máxima."),
                new Sensor("MAP", TipoSensor.MAP, 400.0, 400.0, "teste", null).validar());
        assertEquals(List.of("A faixa máxima do sensor MAP deve ser maior que zero (kPa absolutos)."),
                new Sensor("MAP", TipoSensor.MAP, null, 0.0, "teste", null).validar());
        assertEquals("kPa abs.", TipoSensor.MAP.unidade());
    }

    @Test
    void cambioComTorqueMaximoOpcional() {
        assertTrue(new Cambio("Câmbio teste", TipoCambio.MANUAL, 400.0, "teste", null).validar().isEmpty());
        assertTrue(new Cambio("Câmbio teste", TipoCambio.AUTOMATICO, null, "teste", null).validar().isEmpty());
        assertEquals(List.of("Escolha o tipo do câmbio.", "O torque máximo do câmbio deve ser maior que zero (N·m)."),
                new Cambio("Câmbio teste", null, 0.0, "teste", null).validar());
    }
}
