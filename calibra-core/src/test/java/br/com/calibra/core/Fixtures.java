package br.com.calibra.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.pecas.Turbo;
import br.com.calibra.core.pecas.UnidadeVazao;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.unidades.Conversoes;

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

    /**
     * Mapa de compressor FICTÍCIO, desenhado à mão só para os testes (nenhum mapa de fabricante entra no projeto). Em
     * kg/s × PR, com a referência da Garrett (545 °R e 13,95 psia). O ponto de potência do caso B (≈ 0,310 kg/s
     * corrigidos a PR 2,91) cai na ilha central de 77%, a ≈ 137.500 rpm.
     */
    public static MapaCompressor mapaFicticio() {
        return mapaFicticio(0.70, 0.74, 0.77, 160_000.0);
    }

    /** O mapa fictício com outras eficiências nas ilhas (de fora para dentro) e outra rotação máxima. */
    public static MapaCompressor mapaFicticio(double ilhaFora, double ilhaMeio, double ilhaCentro, Double rotacaoMaxima) {
        return new MapaCompressor(UnidadeVazao.LB_MIN, 545.0 / 1.8, Conversoes.psiParaKpa(13.95),
                List.of(p(0.10, 1.3), p(0.16, 2.0), p(0.22, 2.8), p(0.26, 3.4)),
                List.of(p(0.38, 1.3), p(0.42, 2.0), p(0.44, 2.8), p(0.44, 3.4)),
                List.of(new MapaCompressor.Ilha(ilhaFora, List.of(p(0.17, 1.6), p(0.40, 1.6), p(0.42, 2.6), p(0.36, 3.2),
                                p(0.24, 3.2), p(0.19, 2.4))),
                        new MapaCompressor.Ilha(ilhaMeio, List.of(p(0.22, 2.0), p(0.36, 2.0), p(0.38, 2.7), p(0.30, 3.0),
                                p(0.24, 2.6))),
                        new MapaCompressor.Ilha(ilhaCentro, List.of(p(0.27, 2.4), p(0.34, 2.4), p(0.35, 2.9),
                                p(0.30, 3.0), p(0.27, 2.8)))),
                List.of(new MapaCompressor.LinhaRotacao(80_000, List.of(p(0.12, 1.6), p(0.30, 1.55), p(0.40, 1.4))),
                        new MapaCompressor.LinhaRotacao(110_000, List.of(p(0.17, 2.3), p(0.33, 2.25), p(0.43, 2.0))),
                        new MapaCompressor.LinhaRotacao(140_000, List.of(p(0.23, 3.0), p(0.36, 2.95), p(0.44, 2.7))),
                        new MapaCompressor.LinhaRotacao(160_000, List.of(p(0.26, 3.4), p(0.38, 3.35), p(0.44, 3.1)))),
                rotacaoMaxima);
    }

    private static MapaCompressor.PontoMapa p(double vazaoKgS, double razaoPressao) {
        return new MapaCompressor.PontoMapa(vazaoKgS, razaoPressao);
    }

    /** Turbo fictício com um mapa (ou sem, com mapa null). */
    public static Turbo turbo(String nome, MapaCompressor mapa) {
        return new Turbo(nome, mapa, 0.63, null, null, "Mapa fictício de teste", null);
    }

    /** Tolerância dos casos golden da SPEC 7.6: 1% sobre o valor esperado. */
    public static void assertProximo(double esperado, double obtido, String grandeza) {
        double erro = Math.abs(obtido / esperado - 1.0);
        assertTrue(erro <= 0.01, () -> grandeza + ": esperado " + esperado + ", obtido " + obtido
                + String.format(" (diferença de %.2f%%)", erro * 100));
    }
}
