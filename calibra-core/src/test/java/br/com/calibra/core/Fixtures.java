package br.com.calibra.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.Construcao;
import br.com.calibra.core.motor.RodaFonica;
import br.com.calibra.core.motor.SugestoesMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;

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

    /**
     * Caso golden B (SPEC 7.6): 4 cilindros 2.0 turbo a etanol hidratado, 400 cv a 7.000 rpm, 600 m.
     * A spec não dá o diâmetro: usa o que fecha 2.000 cm³ com curso de 92,8 mm (decisão 7).
     */
    public static EntradaDimensionamento casoB() {
        ConfiguracaoMotor motor = motor(4)
                .diametroMm(Math.sqrt(2000.0 * 1000.0 / (Math.PI / 4.0 * 92.8 * 4)))
                .cursoMm(92.8)
                .aspiracao(Aspiracao.TURBO)
                .combustivel(CombustiveisPadrao.EHC)
                .altitudeLocalM(600)
                .construir();
        return EntradaDimensionamento.de(motor, Meta.noVirabrequim(400, 7000, Uso.RUA), 0.80, 0.95, 25, 300,
                Parametros.padrao())
                .comBico(Bico.informado("1.050 cm³/min", 1050, 300, 0.70, 1.0));
    }

    /** Caso golden C (SPEC 7.6): 3 cilindros 1.0 turbo a gasolina E32, 150 cv a 5.500 rpm, 600 m. */
    public static EntradaDimensionamento casoC() {
        ConfiguracaoMotor motor = motor(3)
                .diametroMm(74.5)
                .cursoMm(76.4)
                .aspiracao(Aspiracao.TURBO)
                .combustivel(CombustiveisPadrao.E32)
                .altitudeLocalM(600)
                .construir();
        return EntradaDimensionamento.de(motor, Meta.noVirabrequim(150, 5500, Uso.RUA), 0.80, 0.95, 25, 300,
                Parametros.padrao())
                .comBico(Bico.informado("440 cm³/min", 440, 300, 0.70, 1.0));
    }

    /** Caso golden D (SPEC 7.6): V12 6.0 aspirado a gasolina E32, 450 cv a 6.500 rpm. */
    public static EntradaDimensionamento casoD(double altitudeM) {
        ConfiguracaoMotor motor = motor(12)
                .diametroMm(89.0)
                .cursoMm(80.4)
                .aspiracao(Aspiracao.ASPIRADO)
                .combustivel(CombustiveisPadrao.E32)
                .altitudeLocalM(altitudeM)
                .construir();
        return EntradaDimensionamento.de(motor, Meta.noVirabrequim(450, 6500, Uso.RUA), 0.87, 0.90, 25, 300,
                Parametros.padrao())
                .comBico(Bico.informado("260 cm³/min", 260, 300, 0.70, 1.0));
    }

    /** Tolerância dos casos golden da SPEC 7.6: 1% sobre o valor esperado. */
    public static void assertProximo(double esperado, double obtido, String grandeza) {
        double erro = Math.abs(obtido / esperado - 1.0);
        assertTrue(erro <= 0.01, () -> grandeza + ": esperado " + esperado + ", obtido " + obtido
                + String.format(" (diferença de %.2f%%)", erro * 100));
    }
}
