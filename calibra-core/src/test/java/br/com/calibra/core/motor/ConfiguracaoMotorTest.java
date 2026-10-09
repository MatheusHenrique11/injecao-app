package br.com.calibra.core.motor;

import static br.com.calibra.core.Fixtures.motor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import br.com.calibra.core.memoria.Aviso;
import br.com.calibra.core.memoria.NivelAviso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Testes obrigatórios da SPEC 6.3 para motores de 3 a 12 cilindros. */
class ConfiguracaoMotorTest {

    /** Volume de um cilindro de 82,5 × 92,8 mm. */
    private static final double CC_POR_CILINDRO = 496.0729;

    static IntStream tresADoze() {
        return IntStream.rangeClosed(3, 12);
    }

    private static Validacao validar(ConfiguracaoMotor.Construtor construtor) {
        return ValidadorMotor.validar(construtor.construir());
    }

    private static boolean temErroCom(Validacao v, String trecho) {
        return v.erros().stream().anyMatch(e -> e.contains(trecho));
    }

    // ---------- Parametrizados de 3 a 12 ----------

    @ParameterizedTest
    @MethodSource("tresADoze")
    void configuracaoPadraoEhValida(int n) {
        Validacao v = validar(motor(n));
        assertTrue(v.valida(), () -> n + " cilindros: " + v.erros());
    }

    @ParameterizedTest
    @MethodSource("tresADoze")
    void cilindradaCorreta(int n) {
        assertEquals(CC_POR_CILINDRO * n, motor(n).construir().cilindradaCc(), 1e-3 * n);
    }

    @ParameterizedTest
    @MethodSource("tresADoze")
    void saidasExigidasCorretas(int n) {
        SaidasExigidas.Saidas sequencial = SaidasExigidas.calcular(motor(n).construir(), 1);
        assertEquals(n, sequencial.bicos());
        assertEquals(n, sequencial.ignicao());

        SaidasExigidas.Saidas duasBancadas = SaidasExigidas.calcular(motor(n).bicosPorCilindro(2).construir(), 1);
        assertEquals(2 * n, duasBancadas.bicos());

        SaidasExigidas.Saidas distribuidor = SaidasExigidas.calcular(
                motor(n).modoIgnicao(ModoIgnicao.DISTRIBUIDOR).modoInjecao(ModoInjecao.SIMULTANEO).construir(), 1);
        assertEquals(1, distribuidor.ignicao());
        assertEquals(1, distribuidor.bicos());

        if (n % 2 == 0) {
            SaidasExigidas.Saidas pares = SaidasExigidas.calcular(motor(n)
                    .modoInjecao(ModoInjecao.SEMISSEQUENCIAL).modoIgnicao(ModoIgnicao.CENTELHA_PERDIDA).construir(), 1);
            assertEquals(n / 2, pares.bicos());
            assertEquals(n / 2, pares.ignicao());
        }
    }

    @ParameterizedTest
    @MethodSource("tresADoze")
    void ordemSugeridaEhPermutacaoValida(int n) {
        List<Integer> ordem = SugestoesMotor.ordemIgnicao(n);
        assertEquals(n, ordem.size());
        assertEquals(1, ordem.getFirst());
        Set<Integer> esperados = new HashSet<>();
        for (int c = 1; c <= n; c++) {
            esperados.add(c);
        }
        assertEquals(esperados, new HashSet<>(ordem));
    }

    @ParameterizedTest
    @MethodSource("tresADoze")
    void angulosPadraoComEspacamentoIgual(int n) {
        List<Double> angulos = SugestoesMotor.angulosPms(n);
        assertEquals(n, angulos.size());
        for (int i = 0; i < n; i++) {
            assertEquals(i * 720.0 / n, angulos.get(i), 1e-9);
        }
    }

    // ---------- Faixa de cilindros ----------

    @ParameterizedTest
    @ValueSource(ints = {2, 13, 0, -1})
    void foraDe3a12ERecusadoComAMensagemCerta(int n) {
        Validacao v = validar(motor(4).cilindros(n));
        assertEquals(List.of("O app aceita motores de 3 a 12 cilindros."), v.erros());
    }

    // ---------- Ímpar e par: centelha perdida e semissequencial ----------

    @ParameterizedTest
    @ValueSource(ints = {3, 5, 7, 9, 11})
    void imparRecusaCentelhaPerdidaESemissequencial(int n) {
        assertTrue(temErroCom(validar(motor(n).modoIgnicao(ModoIgnicao.CENTELHA_PERDIDA)), "Centelha perdida"));
        assertTrue(temErroCom(validar(motor(n).modoInjecao(ModoInjecao.SEMISSEQUENCIAL)), "semissequencial"));
        Validacao permitidos = validar(motor(n).modoIgnicao(ModoIgnicao.DISTRIBUIDOR).modoInjecao(ModoInjecao.SIMULTANEO));
        assertTrue(permitidos.valida(), permitidos.erros()::toString);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 6, 8, 10, 12})
    void parComAngulosIguaisAceitaCentelhaPerdidaESemissequencial(int n) {
        Validacao v = validar(motor(n).modoIgnicao(ModoIgnicao.CENTELHA_PERDIDA).modoInjecao(ModoInjecao.SEMISSEQUENCIAL));
        assertTrue(v.valida(), v.erros()::toString);
    }

    @Test
    void parSemCilindroA360EhRecusado() {
        Validacao v = validar(motor(4).angulosPms(0, 90, 360, 540).modoIgnicao(ModoIgnicao.CENTELHA_PERDIDA));
        assertTrue(temErroCom(v, "par defasado 360°"), v.erros()::toString);
    }

    // ---------- Ordem de ignição ----------

    @Test
    void ordemComCilindroRepetidoEhRecusada() {
        Validacao v = validar(motor(4).ordemIgnicao(1, 3, 3, 2));
        assertTrue(temErroCom(v, "repete o cilindro 3"), v.erros()::toString);
        assertTrue(temErroCom(v, "não tem o cilindro 4"), v.erros()::toString);
    }

    @Test
    void ordemFaltandoCilindroEhRecusada() {
        Validacao v = validar(motor(4).ordemIgnicao(1, 3, 4, 5));
        assertTrue(temErroCom(v, "cilindro 5, que não existe"), v.erros()::toString);
        assertTrue(temErroCom(v, "não tem o cilindro 2"), v.erros()::toString);
    }

    @Test
    void ordemDeTamanhoErradoEhRecusada() {
        Validacao v = validar(motor(4).ordemIgnicao(1, 3, 4));
        assertTrue(temErroCom(v, "exatamente 4 números"), v.erros()::toString);
    }

    @Test
    void ordemQueNaoComecaNoCilindro1EhRecusada() {
        Validacao v = validar(motor(4).ordemIgnicao(3, 4, 2, 1));
        assertTrue(temErroCom(v, "começar no cilindro 1"), v.erros()::toString);
    }

    // ---------- Sensor de fase ----------

    @Test
    void sequencialSemSensorDeFaseEhRecusado() {
        Validacao v = validar(motor(4).rodaFonica(new RodaFonica(60, 2, false)));
        assertTrue(v.erros().contains("Injeção sequencial exige sensor de fase."), v.erros()::toString);
        assertTrue(v.erros().contains("Ignição sequencial exige sensor de fase."), v.erros()::toString);
    }

    @Test
    void semSensorDeFaseFuncionaComModosNaoSequenciais() {
        Validacao v = validar(motor(4).rodaFonica(new RodaFonica(60, 2, false))
                .modoInjecao(ModoInjecao.SEMISSEQUENCIAL).modoIgnicao(ModoIgnicao.CENTELHA_PERDIDA));
        assertTrue(v.valida(), v.erros()::toString);
    }

    // ---------- Avisos amarelos (nunca bloqueiam) ----------

    @Test
    void combinacoesRarasGeramAvisoSemBloquear() {
        Validacao vImpar = validar(motor(5).arranjo(Arranjo.V).anguloEntreBancadas(90.0));
        assertTrue(vImpar.valida(), vImpar.erros()::toString);
        assertTrue(temAvisoAmareloCom(vImpar.avisos(), "raro"));

        Validacao emLinha8 = validar(motor(8).arranjo(Arranjo.EM_LINHA).anguloEntreBancadas(null));
        assertTrue(emLinha8.valida(), emLinha8.erros()::toString);
        assertTrue(temAvisoAmareloCom(emLinha8.avisos(), "raro"));
    }

    @Test
    void outrosAvisosAmarelos() {
        assertTrue(temAvisoAmareloCom(validar(motor(4).taxaCompressao(11.5)).avisos(), "sobrealimentação"));
        assertTrue(temAvisoAmareloCom(validar(motor(4).rotacaoMaxima(9500)).avisos(), "9.000 rpm"));
        assertTrue(temAvisoAmareloCom(validar(motor(4).cilindradaInformadaCc(2100.0)).avisos(), "difere"));
        assertTrue(validar(motor(4).cilindradaInformadaCc(1990.0)).avisos().isEmpty());
    }

    private static boolean temAvisoAmareloCom(List<Aviso> avisos, String trecho) {
        return avisos.stream().anyMatch(a -> a.nivel() == NivelAviso.AMARELO && a.mensagem().contains(trecho));
    }

    // ---------- Campos fora da faixa ----------

    @Test
    void camposForaDaFaixaSaoRecusados() {
        assertTrue(temErroCom(validar(motor(4).taxaCompressao(5.5)), "taxa de compressão"));
        assertTrue(temErroCom(validar(motor(4).taxaCompressao(16.5)), "taxa de compressão"));
        assertTrue(temErroCom(validar(motor(4).valvulasPorCilindro(6)), "válvulas"));
        assertTrue(temErroCom(validar(motor(4).rotacaoMaxima(12500)), "rotação máxima"));
        assertTrue(temErroCom(validar(motor(4).rotacaoMarchaLenta(400)), "marcha lenta"));
        assertTrue(temErroCom(validar(motor(4).altitudeLocalM(3500)), "altitude"));
        assertTrue(temErroCom(validar(motor(4).bicosPorCilindro(3)), "bicos por cilindro"));
        assertTrue(temErroCom(validar(motor(4).diametroMm(0)), "diâmetro"));
        assertTrue(temErroCom(validar(motor(4).diametroMm(Double.NaN)), "diâmetro"));
        assertTrue(temErroCom(validar(motor(8).anguloEntreBancadas(null)), "ângulo entre bancadas"));
        assertTrue(temErroCom(validar(motor(4).angulosPms(0, 180, 540, 360)), "crescer"));
        assertTrue(temErroCom(validar(motor(4).angulosPms(0, 180, 360, 720)), "0 e 719"));
        assertTrue(temErroCom(validar(motor(4).rodaFonica(new RodaFonica(60, 60, true))), "faltantes"));
        assertTrue(temErroCom(validar(motor(4).combustivel(null)), "combustível"));
    }

    @Test
    void construtorVazioListaOsCamposQueFaltam() {
        Validacao v = ValidadorMotor.validar(SugestoesMotor.aplicar(ConfiguracaoMotor.construtor(), 4).construir());
        assertFalse(v.valida());
        assertTrue(v.erros().size() >= 8, v.erros()::toString);
    }

    // ---------- Saídas dos casos golden C e D (SPEC 7.6) ----------

    @Test
    void goldenC_tresCilindrosSequencial() {
        ConfiguracaoMotor c = motor(3).diametroMm(74.5).cursoMm(76.4).construir();
        assertTrue(ValidadorMotor.validar(c).valida());
        SaidasExigidas.Saidas saidas = SaidasExigidas.calcular(c, 1);
        assertEquals(3, saidas.bicos());
        assertEquals(3, saidas.ignicao());
    }

    @Test
    void goldenD_v12() {
        ConfiguracaoMotor sequencial = motor(12).diametroMm(89.0).cursoMm(80.4).construir();
        assertEquals(12, SaidasExigidas.calcular(sequencial, 1).bicos());
        assertEquals(12, SaidasExigidas.calcular(sequencial, 1).ignicao());

        ConfiguracaoMotor centelhaPerdida = sequencial.paraConstrutor().modoIgnicao(ModoIgnicao.CENTELHA_PERDIDA).construir();
        assertTrue(ValidadorMotor.validar(centelhaPerdida).valida());
        assertEquals(6, SaidasExigidas.calcular(centelhaPerdida, 1).ignicao());
    }
}
