package br.com.calibra.core.calculo;

import static br.com.calibra.core.Fixtures.assertProximo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.Fixtures;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.memoria.Aviso;
import br.com.calibra.core.memoria.NivelAviso;
import br.com.calibra.core.memoria.Passo;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.memoria.VersaoNucleo;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.ModoInjecao;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.unidades.Conversoes;
import org.junit.jupiter.api.Test;

/** Comportamentos da cadeia além dos casos golden. */
class CalculoDimensionamentoTest {

    private static Resultado<Dimensionamento> resultado(EntradaDimensionamento entrada) {
        return CalculoDimensionamento.calcular(entrada);
    }

    private static Dimensionamento calcular(EntradaDimensionamento entrada) {
        return resultado(entrada).valor();
    }

    private static EntradaDimensionamento comMotor(EntradaDimensionamento entrada,
            java.util.function.UnaryOperator<ConfiguracaoMotor.Construtor> mudanca) {
        return entrada.comMotor(mudanca.apply(entrada.motor().paraConstrutor()).construir());
    }

    private static boolean temAviso(Resultado<?> r, NivelAviso nivel, String trecho) {
        return r.avisos().stream().anyMatch(a -> a.nivel() == nivel && a.mensagem().contains(trecho));
    }

    private static List<String> erros(EntradaDimensionamento entrada) {
        return assertThrows(EntradaInvalidaException.class, () -> CalculoDimensionamento.calcular(entrada)).erros();
    }

    // ---------- Memória ----------

    @Test
    void resultadoTrazMemoriaComFormulasEVersaoDoNucleo() {
        Resultado<Dimensionamento> r = resultado(Fixtures.casoB());
        assertEquals(VersaoNucleo.ATUAL, r.versaoNucleo());
        List<String> formulas = r.memoria().stream().map(Passo::formula).toList();
        assertTrue(formulas.contains(Formulas.TEXTO_WA_KAR));
        assertTrue(formulas.contains(Formulas.TEXTO_MAP));
        assertTrue(formulas.contains(Formulas.TEXTO_DC));
        assertTrue(r.memoria().size() > 25, "memória curta demais: " + r.memoria().size());
    }

    // ---------- Meta ----------

    @Test
    void metaNaRodaConverteParaOVirabrequim() {
        Dimensionamento d = calcular(Fixtures.casoB().comMeta(Meta.naRoda(300, 15, 7000, Uso.RUA)));
        assertEquals(352.941, Conversoes.kwParaCv(d.potencia().virabrequimKw()), 1e-3);
    }

    @Test
    void metaNaRodaSemPerdaERecusada() {
        Meta semPerda = new Meta(Conversoes.cvParaKw(300), br.com.calibra.core.projeto.PontoMedicao.RODA, null, 7000,
                null, Uso.RUA);
        assertTrue(erros(Fixtures.casoB().comMeta(semPerda)).stream().anyMatch(e -> e.contains("perda de transmissão")));
    }

    @Test
    void rotacaoAcimaDaMaximaDoMotorERecusada() {
        List<String> erros = erros(Fixtures.casoB().comMeta(Meta.noVirabrequim(400, 8000, Uso.RUA)));
        assertTrue(erros.stream().anyMatch(e -> e.contains("passa da rotação máxima")), erros::toString);
    }

    // ---------- Parâmetros ----------

    @Test
    void caminhoPorBsfc() {
        EntradaDimensionamento entrada = Fixtures.casoB().comParametros(Parametros.padrao().comBsfc(0.85));
        Dimensionamento d = calcular(entrada);
        double hp = Conversoes.cvParaHp(400);
        double afrAlvo = CombustiveisPadrao.EHC.afrEstequiometrico() * 0.80;
        assertEquals(hp * afrAlvo * 0.85 / 60.0, d.combustao().vazaoArLbMin(), 1e-9);
        assertEquals(0.85, d.combustao().bsfcLbHpH(), 1e-9);
    }

    @Test
    void veDigitadaComoPercentualERecusadaComExplicacao() {
        EntradaDimensionamento entrada = new EntradaDimensionamento(Fixtures.casoB().motor(), Fixtures.casoB().meta(),
                0.80, 95, 25, 300, Parametros.padrao(), null, null, null, null, null, null, null);
        assertTrue(erros(entrada).stream().anyMatch(e -> e.contains("0,95 = 95%")));
    }

    @Test
    void kArForaDaFaixaERecusado() {
        assertTrue(erros(Fixtures.casoB().comParametros(Parametros.padrao().comKAr(12)))
                .stream().anyMatch(e -> e.contains("k_ar")));
    }

    /** Faixa ampliada para motores antigos de 2 válvulas (decisão 40): de 8,0 a 11. */
    @Test
    void kArAceitaDe8a11() {
        calcular(Fixtures.casoB().comParametros(Parametros.padrao().comKAr(8.0)));
        calcular(Fixtures.casoB().comParametros(Parametros.padrao().comKAr(11.0)));
        List<String> abaixo = erros(Fixtures.casoB().comParametros(Parametros.padrao().comKAr(7.9)));
        assertTrue(abaixo.contains("O fator k_ar deve ficar entre 8,0 e 11 hp por lb/min."), abaixo::toString);
        assertTrue(erros(Fixtures.casoB().comParametros(Parametros.padrao().comKAr(11.1)))
                .stream().anyMatch(e -> e.contains("k_ar")));
    }

    /** A tela de Configurações confere os padrões de cálculo com a mesma regra do cálculo. */
    @Test
    void validarParametrosSozinho() {
        assertTrue(CalculoDimensionamento.validarParametros(Parametros.padrao()).isEmpty());
        assertEquals(List.of("O fator k_ar deve ficar entre 8,0 e 11 hp por lb/min."),
                CalculoDimensionamento.validarParametros(Parametros.padrao().comKAr(7.9)));
        assertEquals(List.of("Informe os parâmetros do cálculo."), CalculoDimensionamento.validarParametros(null));
    }

    @Test
    void motorInvalidoBloqueiaOCalculo() {
        EntradaDimensionamento entrada = comMotor(Fixtures.casoC(), c -> c.modoInjecao(ModoInjecao.SEMISSEQUENCIAL));
        assertTrue(erros(entrada).stream().anyMatch(e -> e.contains("semissequencial")));
    }

    // ---------- Bico ----------

    @Test
    void semBicoNaoAvaliaDcEAvisa() {
        Resultado<Dimensionamento> r = resultado(Fixtures.casoB().comBico(null));
        assertNull(r.valor().bicoAvaliado());
        assertFalse(r.valor().avaliacao(Indicador.DC_BICO).isPresent());
        assertTrue(temAviso(r, NivelAviso.INFORMACAO, "Nenhum bico informado"));
        assertProximo(1058, r.valor().bicoNecessario().vazaoNominalCm3Min(), "vazão nominal sem bico");
    }

    @Test
    void vazaoNominalMinimaComTempoMortoLevaODcAoMaximo() {
        Dimensionamento d = calcular(Fixtures.casoB());
        double minima = d.bicoAvaliado().vazaoNominalMinimaCm3Min();
        assertTrue(minima > 1050, "o bico de 1.050 deu vermelho; o mínimo tem de ser maior");
        Dimensionamento comMinimo = calcular(Fixtures.casoB().comBico(Bico.informado("mínimo", minima, 300, 0.70, 1.0)));
        assertEquals(0.80, comMinimo.bicoAvaliado().dutyCycle(), 1e-9);
    }

    @Test
    void duasBancadasDividemAVazaoEUsamOMaiorDc() {
        EntradaDimensionamento entrada = comMotor(Fixtures.casoB(), c -> c.bicosPorCilindro(2))
                .comBico(Bico.informado("primário", 440, 300, 0.70, 0.9))
                .comBicoSecundario(Bico.informado("secundário", 650, 300, 0.70, 1.1));
        Dimensionamento d = calcular(entrada);
        Dimensionamento.BicoAvaliado b = d.bicoAvaliado();
        assertEquals(8, d.bicoNecessario().numeroBicos());
        assertTrue(Double.isFinite(b.tiTotalSecundarioMs()));
        assertEquals(b.tiEfetivoMs() + 1.1, b.tiTotalSecundarioMs(), 1e-9);
        assertEquals(Formulas.dutyCycle(b.tiTotalSecundarioMs(), 1, 7000), b.dutyCycle(), 1e-9);
        assertTrue(Double.isNaN(b.vazaoNominalMinimaCm3Min()));
    }

    @Test
    void duasBancadasSemSecundarioSaoRecusadas() {
        EntradaDimensionamento entrada = comMotor(Fixtures.casoB(), c -> c.bicosPorCilindro(2));
        assertTrue(erros(entrada).stream().anyMatch(e -> e.contains("bancada secundária")));
    }

    @Test
    void tempoMortoPorTabelaPrecisaDaTensao() {
        TempoMorto tabela = new TempoMorto(List.of(
                new TempoMorto.Ponto(12.0, 300, 1.20),
                new TempoMorto.Ponto(14.0, 300, 0.90)));
        Bico bico = new Bico("tabela", 1050, 300, 0.70, tabela, null, "teste", null);
        EntradaDimensionamento entrada = Fixtures.casoB().comBico(bico);
        assertThrows(EntradaInvalidaException.class, () -> CalculoDimensionamento.calcular(entrada));
        Dimensionamento d = calcular(entrada.comTensao(13.0));
        assertEquals(1.05, d.bicoAvaliado().tempoMortoMs(), 1e-9);
    }

    // ---------- Marcha lenta ----------

    @Test
    void bicoGrandeNaMarchaLentaGeraAvisoAmarelo() {
        Resultado<Dimensionamento> r = resultado(Fixtures.casoB().comMarchaLenta(new MarchaLenta(40, 0.45)));
        double ti = r.valor().bicoAvaliado().tiEfetivoMarchaLentaMs();
        assertTrue(ti < 1.0, "TI de marcha lenta esperado abaixo de 1 ms, deu " + ti);
        assertTrue(temAviso(r, NivelAviso.AMARELO, "duas bancadas"));
    }

    @Test
    void semDadosDeMarchaLentaAvisaQueNaoVerificou() {
        Resultado<Dimensionamento> r = resultado(Fixtures.casoB());
        assertTrue(Double.isNaN(r.valor().bicoAvaliado().tiEfetivoMarchaLentaMs()));
        assertTrue(temAviso(r, NivelAviso.INFORMACAO, "Marcha lenta não verificada"));
    }

    // ---------- Bomba, sensor e injeção ----------

    @Test
    void bomba() {
        Dimensionamento d = calcular(Fixtures.casoB());
        assertProximo(188.9, d.bomba().vazaoLh(), "vazão da bomba");
        assertProximo(245.5, d.bomba().vazaoComFolgaVerdeLh(), "vazão com folga de 30%");
        double pressaoEsperada = 300 + (d.ladoDoAr().mapKpa() - d.ladoDoAr().patmKpa());
        assertEquals(pressaoEsperada, d.bomba().pressaoKpa(), 1e-9);
        assertFalse(d.avaliacao(Indicador.FOLGA_BOMBA).isPresent());

        assertEquals(Cor.VERDE, calcular(Fixtures.casoB().comBombaDisponivel(250.0))
                .avaliacao(Indicador.FOLGA_BOMBA).orElseThrow().cor());
        assertEquals(Cor.VERMELHO, calcular(Fixtures.casoB().comBombaDisponivel(200.0))
                .avaliacao(Indicador.FOLGA_BOMBA).orElseThrow().cor());
    }

    @Test
    void bombaDoAspiradoNaoSomaPressaoDeTurbo() {
        Dimensionamento d = calcular(Fixtures.casoD(0));
        assertEquals(300.0, d.bomba().pressaoKpa(), 1e-9);
    }

    @Test
    void sensorMap() {
        Dimensionamento turbo = calcular(Fixtures.casoB().comSensorMap(300.0));
        assertEquals(1.2 * turbo.ladoDoAr().mapKpa(), turbo.sensorMap().faixaMinimaKpa(), 1e-9);
        assertEquals(Cor.VERDE, turbo.avaliacao(Indicador.FAIXA_SENSOR_MAP).orElseThrow().cor());
        assertEquals(Cor.AMARELO, calcular(Fixtures.casoB().comSensorMap(250.0))
                .avaliacao(Indicador.FAIXA_SENSOR_MAP).orElseThrow().cor());

        Dimensionamento aspirado = calcular(Fixtures.casoD(0));
        assertEquals(aspirado.ladoDoAr().patmKpa(), aspirado.sensorMap().mapMaximoPrevistoKpa(), 1e-9);
    }

    @Test
    void saidasDaInjecao() {
        Dimensionamento atende = calcular(Fixtures.casoB().comInjecao(Injecao.informada("FT450", 6, 4)));
        assertEquals(Cor.VERDE, atende.avaliacao(Indicador.SAIDAS_INJECAO).orElseThrow().cor());

        Dimensionamento falta = calcular(Fixtures.casoD(0).comInjecao(Injecao.informada("X", 8, 4)));
        assertEquals(Cor.VERMELHO, falta.avaliacao(Indicador.SAIDAS_INJECAO).orElseThrow().cor());
        assertTrue(falta.avaliacao(Indicador.SAIDAS_INJECAO).orElseThrow().mensagem()
                .contains("tem 8 saídas de bico; este motor pede 12"));
    }

    // ---------- Combustível, turbo e avisos ----------

    @Test
    void metanolTrazAvisoAmareloDeLambda() {
        EntradaDimensionamento entrada = comMotor(Fixtures.casoB(), c -> c.combustivel(CombustiveisPadrao.METANOL));
        assertTrue(temAviso(resultado(entrada), NivelAviso.AMARELO, "Metanol"));
    }

    @Test
    void metaSemPressaoPositivaUsaT2IgualAT1() {
        Resultado<Dimensionamento> r = resultado(Fixtures.casoB().comMeta(Meta.noVirabrequim(40, 7000, Uso.RUA)));
        Dimensionamento d = r.valor();
        assertTrue(d.ladoDoAr().razaoPressao() <= 1.0);
        assertEquals(25.0, d.ladoDoAr().t2C(), 1e-9);
        assertEquals(25.0, d.ladoDoAr().tmC(), 1e-9);
        assertTrue(temAviso(r, NivelAviso.INFORMACAO, "não pede pressão positiva"));
    }

    @Test
    void biturboAvisaSobreMetadeDaVazao() {
        EntradaDimensionamento entrada = comMotor(Fixtures.casoB(), c -> c.aspiracao(Aspiracao.BITURBO));
        assertTrue(temAviso(resultado(entrada), NivelAviso.INFORMACAO, "Biturbo"));
    }

    @Test
    void avisosDoMotorVaoParaOResultado() {
        EntradaDimensionamento entrada = comMotor(Fixtures.casoB(), c -> c.taxaCompressao(11.5));
        List<Aviso> avisos = resultado(entrada).avisos();
        assertTrue(avisos.stream().anyMatch(a -> a.mensagem().contains("sobrealimentação")));
    }

    // ---------- Iteração MAP ↔ Tm (SPEC 7.1: "se não convergir, erro") ----------

    @Test
    void iteracaoQueNaoConvergeDaErro() {
        double patmPsia = Conversoes.kpaParaPsi(Formulas.pressaoAtmosfericaKpa(600));
        double vdPol3 = Conversoes.litrosParaPol3(2.0);
        CalculoNaoConvergiuException erro = assertThrows(CalculoNaoConvergiuException.class,
                () -> CalculoDimensionamento.iterarMapTm(37.57, 0.95, 7000, vdPol3, patmPsia, 2, 1, 25, 0.72, 0.80, 1));
        assertTrue(erro.getMessage().contains("não convergiu"), erro.getMessage());

        CalculoDimensionamento.IteracaoMapTm convergiu = CalculoDimensionamento.iterarMapTm(37.57, 0.95, 7000, vdPol3,
                patmPsia, 2, 1, 25, 0.72, 0.80, CalculoDimensionamento.MAX_ITERACOES);
        assertTrue(convergiu.passos() > 1 && convergiu.passos() < CalculoDimensionamento.MAX_ITERACOES);
        assertProximo(55, convergiu.tmC(), "Tm convergida");
    }

    @Test
    void perdaAntesDoCompressorMaiorQueAPressaoAtmosfericaERecusada() {
        Parametros p = Parametros.padrao();
        Parametros errada = new Parametros(p.caminhoAr(), p.kAr(), p.bsfc(), p.dcMax(), p.etaC(), p.eps(),
                p.perdaDescargaKpa(), Conversoes.psiParaKpa(15), p.acrescimoTmAspiradoC(), p.densidadeFluidoTesteKgL(),
                p.tiMinimoMarchaLentaMs(), p.gruposSimultaneo(), p.limiares());
        List<String> erros = erros(Fixtures.casoB().comParametros(errada));
        assertTrue(erros.stream().anyMatch(e -> e.contains("perda antes do compressor")), erros::toString);
    }

    // ---------- Memória: tudo que decide um aviso fica registrado (regra 2.5) ----------

    @Test
    void memoriaRegistraTiMinimoECilindradaInformada() {
        EntradaDimensionamento entrada = comMotor(Fixtures.casoB(), c -> c.cilindradaInformadaCc(1990.0))
                .comMarchaLenta(new MarchaLenta(40, 0.45));
        Resultado<Dimensionamento> r = resultado(entrada);
        assertTrue(r.memoria().stream().anyMatch(p -> p.grandeza().equals("Cilindrada informada (conferência)")));
        Passo marchaLenta = r.memoria().stream()
                .filter(p -> p.grandeza().equals("TI efetivo na marcha lenta")).findFirst().orElseThrow();
        assertTrue(marchaLenta.entradas().stream().anyMatch(e -> e.simbolo().equals("TI mínimo")));
    }

    // ---------- Sensor MAP e saídas ----------

    @Test
    void sensorMapNoTurboSemPressaoPositivaUsaAPatm() {
        Dimensionamento d = calcular(Fixtures.casoB().comMeta(Meta.noVirabrequim(40, 7000, Uso.RUA)));
        assertTrue(d.ladoDoAr().mapKpa() < d.ladoDoAr().patmKpa());
        assertEquals(d.ladoDoAr().patmKpa(), d.sensorMap().mapMaximoPrevistoKpa(), 1e-9);
    }

    @Test
    void saidasConferemOLimiteDeCilindrosEmSequencial() {
        Injecao limitada = new Injecao("Marca", "Oito em sequencial", 16, 12, 8, Set.of(), "teste", null);
        Dimensionamento d = calcular(Fixtures.casoD(0).comInjecao(limitada));
        assertEquals(Cor.VERMELHO, d.avaliacao(Indicador.SAIDAS_INJECAO).orElseThrow().cor());
        assertTrue(d.avaliacao(Indicador.SAIDAS_INJECAO).orElseThrow().mensagem()
                .contains("controla até 8 cilindros em sequencial"));
    }

    // ---------- Recomendações (SPEC 7.3) ----------

    @Test
    void intercoolerArAguaNaArrancadaEArArNosOutrosUsos() {
        assertTrue(calcular(Fixtures.casoB().comMeta(Meta.noVirabrequim(400, 7000, Uso.ARRANCADA)))
                .recomendacoes().stream().anyMatch(r -> r.contains("ar-água")));
        assertTrue(calcular(Fixtures.casoB()).recomendacoes().stream().anyMatch(r -> r.contains("ar-ar")));
        assertTrue(calcular(Fixtures.casoD(0)).recomendacoes().stream().noneMatch(r -> r.contains("Intercooler")));
    }

    @Test
    void radiadorDeOleoPelaRegraDoDono() {
        assertTrue(temRadiador(Fixtures.casoD(0), Uso.CIRCUITO_ENDURANCE));
        assertTrue(temRadiador(Fixtures.casoD(0), Uso.NAUTICO));
        assertTrue(temRadiador(Fixtures.casoB(), Uso.OFF_ROAD), "off-road com BMEP 25,2 > 22 bar");
        assertFalse(temRadiador(Fixtures.casoD(0), Uso.OFF_ROAD), "off-road com BMEP 10,2 < 13 bar");
        assertFalse(temRadiador(Fixtures.casoB(), Uso.RUA));
    }

    private static boolean temRadiador(EntradaDimensionamento entrada, Uso uso) {
        Meta meta = entrada.meta();
        Meta comUso = new Meta(meta.potenciaKw(), meta.pontoMedicao(), meta.perdaTransmissaoPct(),
                meta.rotacaoPotenciaMaxima(), meta.rotacaoTorqueMaximo(), uso);
        return calcular(entrada.comMeta(comUso)).recomendacoes().stream().anyMatch(r -> r.contains("Radiador de óleo"));
    }
}
