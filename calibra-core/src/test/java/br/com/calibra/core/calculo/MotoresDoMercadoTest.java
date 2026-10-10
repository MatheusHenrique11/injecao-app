package br.com.calibra.core.calculo;

import static br.com.calibra.core.Fixtures.assertProximo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import br.com.calibra.core.MotoresDoMercado;
import br.com.calibra.core.MotoresDoMercado.Motor;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.Validacao;
import br.com.calibra.core.motor.ValidadorMotor;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Motores do mercado brasileiro de preparação passando pela cadeia inteira (fase 1.1). O Calibra não é
 * só para o 2.0 dos casos golden: aqui entram AP, Opala, Omega, Família II, os 5 cilindros da Fiat e da
 * VW, o G4GC e os VW modernos.
 */
class MotoresDoMercadoTest {

    /** VE usada nas contas de série; a VE implícita mostra o quanto ela precisaria ser. */
    private static final double VE_DE_TESTE = 0.90;

    static List<Motor> todos() {
        return MotoresDoMercado.TODOS;
    }

    static List<Motor> comDadosDeSerie() {
        return MotoresDoMercado.comDadosDeSerie();
    }

    static List<Motor> aspiradosDeSerie() {
        return MotoresDoMercado.aspiradosDeSerie();
    }

    private static Dimensionamento calcularDeSerie(Motor m, Parametros parametros) {
        ConfiguracaoMotor motor = m.configuracao().combustivel(CombustiveisPadrao.E25).construir();
        double lambda = CombustiveisPadrao.E25.lambdaPadrao(m.aspiracao().sobrealimentado());
        EntradaDimensionamento entrada = EntradaDimensionamento.de(motor,
                Meta.noVirabrequim(m.potenciaSerieCv(), m.rotacaoPotencia(), Uso.RUA), lambda, VE_DE_TESTE, 25, 300,
                parametros);
        return CalculoDimensionamento.calcular(entrada).valor();
    }

    // ---------- Geometria ----------

    @ParameterizedTest(name = "{0}")
    @MethodSource("todos")
    void cilindradaBateComAFicha(Motor m) {
        assertProximo(m.cilindradaFichaCc(), m.configuracao().construir().cilindradaCc(), m.nome() + " (" + m.fonte() + ")");
    }

    /** Montagens comuns do AP: o nome popular é ambíguo, a geometria não. */
    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "milenove: pistão do 2.0 + virabrequim do 1.8|82.5|86.4|1847",
        "1.9: bloco 1.8 + virabrequim do 2.0|81.0|92.8|1913",
        "2.0 com pistão 83|83.0|92.8|2008",
        "virabrequim 96 + pistão 83|83.0|96.0|2078",
        "virabrequim 95,5 + pistão 84|84.0|95.5|2117",
        "virabrequim 99 + pistão 85|85.0|99.0|2247"})
    void montagensDoAp(String montagem, double diametro, double curso, double cilindradaEsperada) {
        ConfiguracaoMotor motor = MotoresDoMercado.TODOS.get(2).configuracao()
                .diametroMm(diametro).cursoMm(curso).construir();
        assertEquals(cilindradaEsperada, motor.cilindradaCc(), 1.0, montagem);
        assertTrue(ValidadorMotor.validar(motor).valida(), montagem);
    }

    // ---------- Cadeia inteira ----------

    /** Projeto turbo de teste (120 cv por litro a 6.000 rpm) em cada geometria: a cadeia roda sem erro. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("todos")
    void cadeiaInteiraRodaEmTodosOsMotores(Motor m) {
        ConfiguracaoMotor motor = m.configuracao().aspiracao(Aspiracao.TURBO).construir();
        Validacao validacao = ValidadorMotor.validar(motor);
        assertTrue(validacao.valida(), () -> m.nome() + ": " + validacao.erros());

        double cv = 120.0 * motor.cilindradaCc() / 1000.0;
        EntradaDimensionamento entrada = EntradaDimensionamento.de(motor, Meta.noVirabrequim(cv, 6000, Uso.RUA), 0.80,
                VE_DE_TESTE, 25, 300, Parametros.padrao());
        Dimensionamento d = CalculoDimensionamento.calcular(entrada).valor();

        assertEquals(m.cilindros(), d.saidas().bicos(), "bicos em sequencial");
        assertEquals(m.cilindros(), d.saidas().ignicao(), "ignição em sequencial");
        assertTrue(d.ladoDoAr().razaoPressao() > 1.0, "120 cv/L pede pressão positiva");
        assertTrue(d.avaliacao(Indicador.BMEP).isPresent() && d.avaliacao(Indicador.VELOCIDADE_PISTAO).isPresent());
    }

    // ---------- Motores de série ----------

    /** Carro de série tem de ficar verde em BMEP e velocidade do pistão (referências da SPEC 7.4). */
    @ParameterizedTest(name = "{0}")
    @MethodSource("comDadosDeSerie")
    void motorDeSerieFicaVerdeEmBmepEPistao(Motor m) {
        Dimensionamento d = calcularDeSerie(m, Parametros.padrao());
        assertEquals(Cor.VERDE, d.avaliacao(Indicador.BMEP).orElseThrow().cor(), m.nome());
        assertEquals(Cor.VERDE, d.avaliacao(Indicador.VELOCIDADE_PISTAO).orElseThrow().cor(), m.nome());
    }

    /**
     * VE implícita = VE usada × MAP necessário ÷ P_atm: a VE que o motor de série precisaria ter, ao nível
     * do mar, para fazer a potência de fábrica. A faixa de 0,60 a 1,05 é de plausibilidade, escolhida por
     * mim, não é regra da spec. Com k_ar 10,5 os Omega dão cerca de 0,68–0,71 (decisão 40).
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("aspiradosDeSerie")
    void veImplicitaDosAspiradosDeSerieEPlausivel(Motor m) {
        double veImplicita = VE_DE_TESTE * calcularDeSerie(m, Parametros.padrao()).ladoDoAr().mapSobrePatm();
        assertTrue(veImplicita >= 0.60 && veImplicita <= 1.05, m.nome() + ": VE implícita " + veImplicita);
    }

    /** Com k_ar 9,0 (referência de motor antigo de 2 válvulas), os Omega voltam para a faixa realista. */
    @Test
    void motoresAntigosComKar9FicamComVeRealista() {
        for (Motor m : aspiradosDeSerie()) {
            if (m.valvulasPorCilindro() != 2) {
                continue;
            }
            double comKar105 = VE_DE_TESTE * calcularDeSerie(m, Parametros.padrao()).ladoDoAr().mapSobrePatm();
            double comKar9 = VE_DE_TESTE * calcularDeSerie(m, Parametros.padrao().comKAr(9.0)).ladoDoAr().mapSobrePatm();
            assertTrue(comKar105 < 0.75, m.nome() + ": com k_ar 10,5 a VE implícita fica baixa (" + comKar105 + ")");
            assertTrue(comKar9 >= 0.75 && comKar9 <= 1.05, m.nome() + ": com k_ar 9,0 a VE implícita é " + comKar9);
        }
    }

    /** O Marea Turbo de série (182 cv a 6.000 rpm) passa pela cadeia turbo e pede pressão positiva. */
    @Test
    void mareaTurboDeSerie() {
        Motor marea = MotoresDoMercado.TODOS.stream()
                .filter(m -> m.nome().startsWith("Fiat 2.0 20V Turbo")).findFirst().orElseThrow();
        Dimensionamento d = calcularDeSerie(marea, Parametros.padrao());
        assertTrue(d.ladoDoAr().razaoPressao() > 1.0);
        assertEquals(5, d.saidas().bicos());
        assertEquals(Cor.AMARELO, d.avaliacao(Indicador.MAPA_COMPRESSOR).orElseThrow().cor(), "sem mapa: amarelo");
    }
}
