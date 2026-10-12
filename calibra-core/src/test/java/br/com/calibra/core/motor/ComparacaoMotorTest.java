package br.com.calibra.core.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.Fixtures;
import br.com.calibra.core.combustivel.CombustiveisPadrao;

/** Campos do motor em português e a diferença entre duas versões (SPEC 6.3). */
class ComparacaoMotorTest {

    @Test
    void descreveTodosOsCamposNaOrdemDaTela() {
        ConfiguracaoMotor motor = Fixtures.motor(4).construir();
        List<ComparacaoMotor.Linha> linhas = ComparacaoMotor.descrever(motor);
        assertEquals("Número de cilindros", linhas.getFirst().campo());
        assertEquals("4", linhas.getFirst().valor());
        assertTrue(linhas.contains(new ComparacaoMotor.Linha("Diâmetro (mm)", "82,5")));
        assertTrue(linhas.contains(new ComparacaoMotor.Linha("Cilindrada calculada (cm³)", "1.984")));
        assertTrue(linhas.contains(new ComparacaoMotor.Linha("Rotação máxima (rpm)", "7.500")));
        assertTrue(linhas.contains(new ComparacaoMotor.Linha("Ordem de ignição", "1-3-4-2")));
        assertTrue(linhas.contains(new ComparacaoMotor.Linha("Ângulos dos PMS (°)", "0; 180; 360; 540")));
        assertTrue(linhas.contains(new ComparacaoMotor.Linha("Ângulo entre bancadas (°)", "—")), "em linha");
        assertTrue(linhas.contains(new ComparacaoMotor.Linha("Combustível", "Gasolina C comum E32 (E32, versão 1)")));
        assertTrue(linhas.contains(new ComparacaoMotor.Linha("Sensor de fase", "sim")));
    }

    @Test
    void mostraSoOQueMudou() {
        ConfiguracaoMotor v1 = Fixtures.motor(4).construir();
        ConfiguracaoMotor v2 = v1.paraConstrutor()
                .diametroMm(83.0)
                .taxaCompressao(10.0)
                .combustivel(CombustiveisPadrao.EHC)
                .construir();
        assertEquals(List.of(
                new ComparacaoMotor.Diferenca("Diâmetro (mm)", "82,5", "83"),
                new ComparacaoMotor.Diferenca("Cilindrada calculada (cm³)", "1.984", "2.008"),
                new ComparacaoMotor.Diferenca("Taxa de compressão (:1)", "9,5", "10"),
                new ComparacaoMotor.Diferenca("Combustível", "Gasolina C comum E32 (E32, versão 1)",
                        "Etanol hidratado (93% em massa) (EHC, versão 1)")),
                ComparacaoMotor.comparar(v1, v2));
    }

    @Test
    void motorIgualNaoTemDiferenca() {
        ConfiguracaoMotor v1 = Fixtures.motor(6).construir();
        assertTrue(ComparacaoMotor.comparar(v1, v1.paraConstrutor().construir()).isEmpty());
    }

    @Test
    void mudarCilindrosMostraOrdemEAngulosNovos() {
        ConfiguracaoMotor v1 = Fixtures.motor(4).construir();
        ConfiguracaoMotor v2 = SugestoesMotor.aplicar(v1.paraConstrutor(), 6).construir();
        List<String> campos = ComparacaoMotor.comparar(v1, v2).stream().map(ComparacaoMotor.Diferenca::campo).toList();
        assertTrue(campos.containsAll(List.of("Número de cilindros", "Ordem de ignição", "Ângulos dos PMS (°)",
                "Cilindrada calculada (cm³)")), campos::toString);
    }
}
