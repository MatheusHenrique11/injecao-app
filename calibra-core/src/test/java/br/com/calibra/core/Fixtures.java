package br.com.calibra.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.Construcao;
import br.com.calibra.core.motor.RodaFonica;
import br.com.calibra.core.motor.SugestoesMotor;

/**
 * Dados de teste. Os valores que não afetam a conta testada (taxa, válvulas, roda fônica...) são
 * neutros e servem só para a configuração passar na validação. Nada disto vai para o app.
 */
public final class Fixtures {

    private Fixtures() {
    }

    /** Motor válido de n cilindros: sugestões do app + dados de teste. */
    public static ConfiguracaoMotor.Construtor motor(int cilindros) {
        return SugestoesMotor.aplicar(ConfiguracaoMotor.construtor(), cilindros)
                .diametroMm(82.5)
                .cursoMm(92.8)
                .taxaCompressao(9.5)
                .valvulasPorCilindro(4)
                .comando(Comando.ORIGINAL)
                .aspiracao(Aspiracao.TURBO)
                .pistoes(Construcao.ORIGINAL)
                .bielas(Construcao.ORIGINAL)
                .rotacaoMaxima(7500)
                .rotacaoMarchaLenta(900)
                .bicosPorCilindro(1)
                .bicosExtrasCorpoBorboleta(0)
                .rodaFonica(new RodaFonica(60, 2, true))
                .combustivel(CombustiveisPadrao.E32)
                .sensorEtanol(false)
                .altitudeLocalM(0);
    }

    /** Tolerância dos casos golden da SPEC 7.6: 1% sobre o valor esperado. */
    public static void assertProximo(double esperado, double obtido, String grandeza) {
        double erro = Math.abs(obtido / esperado - 1.0);
        assertTrue(erro <= 0.01, () -> grandeza + ": esperado " + esperado + ", obtido " + obtido
                + String.format(" (diferença de %.2f%%)", erro * 100));
    }
}
