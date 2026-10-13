package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.motor.ComparacaoMotor;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.PontoMedicao;
import br.com.calibra.core.projeto.Uso;

/** O caminho de volta do LeitorFormulario: escrever na tela e ler de novo dá a mesma entrada. */
class EscritorFormularioTest {

    private static EntradaDimensionamento casoB() {
        return LeitorFormulario.ler(ValoresDeTeste.casoB(), Parametros.padrao()).entrada();
    }

    private static EntradaDimensionamento idaEVolta(EntradaDimensionamento entrada) {
        Map<Campo, String> campos = new EnumMap<>(Campo.class);
        campos.putAll(EscritorFormulario.motor(entrada.motor()));
        campos.putAll(EscritorFormulario.semMotor(entrada));
        LeitorFormulario.Leitura leitura = LeitorFormulario.ler(campos, entrada.parametros());
        assertTrue(leitura.erros().isEmpty(), leitura.erros()::toString);
        return leitura.entrada();
    }

    @Test
    void casoBDigitadoVoltaIgual() {
        EntradaDimensionamento entrada = casoB();
        assertEquals(entrada, idaEVolta(entrada));
    }

    @Test
    void escreveComoNaTela() {
        Map<Campo, String> campos = EscritorFormulario.semMotor(casoB());
        assertEquals("400", campos.get(Campo.POTENCIA_CV));
        assertEquals("7.000", campos.get(Campo.RPM_POTENCIA));
        assertEquals("3", campos.get(Campo.PRESSAO_BASE_BAR));
        assertEquals("2,0", campos.get(Campo.PERDA_DESCARGA_PSI));
        assertEquals("1.050", campos.get(Campo.BICO_VAZAO));
        assertEquals("", campos.get(Campo.PERDA_TRANSMISSAO), "meta no virabrequim");
        Map<Campo, String> motor = EscritorFormulario.motor(casoB().motor());
        assertEquals("82,83", motor.get(Campo.DIAMETRO));
        assertEquals("1-3-4-2", motor.get(Campo.ORDEM_IGNICAO));
        assertEquals("0; 180; 360; 540", motor.get(Campo.ANGULOS_PMS));
        assertEquals("EHC", motor.get(Campo.COMBUSTIVEL));
        assertEquals("true", motor.get(Campo.SENSOR_FASE));
    }

    /** Motor com número quebrado (diâmetro que fecha 2.000 cm³): volta igual até a 3ª casa, como na tela. */
    @Test
    void motorComNumeroQuebradoVoltaSemDiferencaNaTela() {
        ConfiguracaoMotor original = casoB().motor().paraConstrutor()
                .diametroMm(Math.sqrt(2000.0 * 1000.0 / (Math.PI / 4.0 * 92.8 * 4)))
                .construir();
        ConfiguracaoMotor lido = LeitorFormulario.lerMotor(EscritorFormulario.motor(original)).motor();
        assertTrue(ComparacaoMotor.comparar(original, lido).isEmpty());
        assertEquals(original.diametroMm(), lido.diametroMm(), 1e-4);
    }

    @Test
    void metaPelaPressaoEVeDoTorqueVoltamIguais() {
        EntradaDimensionamento pelaPressao = casoB().comMeta(Meta.pelaPressao(140.0, 15.0, 7000, 4500, Uso.RUA))
                .comVeTorque(0.9);
        assertEquals(pelaPressao, idaEVolta(pelaPressao));
        Map<Campo, String> campos = EscritorFormulario.semMotor(pelaPressao);
        assertEquals("PRESSAO", campos.get(Campo.TIPO_META));
        assertEquals("1,4", campos.get(Campo.PRESSAO_META));
        assertEquals("BAR", campos.get(Campo.UNIDADE_PRESSAO_META));
        assertEquals("", campos.get(Campo.POTENCIA_CV), "a potência sai do cálculo");
        assertEquals("15", campos.get(Campo.PERDA_TRANSMISSAO));
        assertEquals("0,9", campos.get(Campo.VE_TORQUE));
        assertEquals("POTENCIA", EscritorFormulario.semMotor(casoB()).get(Campo.TIPO_META));
    }

    @Test
    void metaNaRodaComPerdaETorque() {
        EntradaDimensionamento naRoda = casoB().comMeta(new Meta(casoB().meta().potenciaKw(),
                PontoMedicao.RODA, 15.0, 7000, 5000, Uso.ARRANCADA));
        assertEquals(naRoda, idaEVolta(naRoda));
    }
}
