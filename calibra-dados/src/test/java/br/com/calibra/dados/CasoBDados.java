package br.com.calibra.dados;

import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.MarchaLenta;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.Construcao;
import br.com.calibra.core.motor.RodaFonica;
import br.com.calibra.core.motor.SugestoesMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;

/**
 * Caso golden B da SPEC 7.6 (o mesmo dos testes do core), com os opcionais preenchidos para o JSON
 * passar por todos os tipos: injeção, marcha lenta, bomba e sensor MAP.
 */
public final class CasoBDados {

    private CasoBDados() {
    }

    public static EntradaDimensionamento entrada() {
        ConfiguracaoMotor motor = SugestoesMotor.aplicar(ConfiguracaoMotor.construtor(), 4)
                .diametroMm(Math.sqrt(2000.0 * 1000.0 / (Math.PI / 4.0 * 92.8 * 4)))
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
                .combustivel(CombustiveisPadrao.EHC)
                .sensorEtanol(false)
                .altitudeLocalM(600)
                .construir();
        return EntradaDimensionamento.de(motor, Meta.noVirabrequim(400, 7000, Uso.RUA), 0.80, 0.95, 25, 300,
                Parametros.padrao())
                .comBico(Bico.informado("1.050 cm³/min", 1050, 300, 0.70, 1.0))
                .comMarchaLenta(new MarchaLenta(35, 0.50))
                .comBombaDisponivel(255.0)
                .comSensorMap(400.0)
                .comInjecao(Injecao.informada("Injeção de teste", 8, 4));
    }
}
