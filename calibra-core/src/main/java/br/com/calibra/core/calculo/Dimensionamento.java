package br.com.calibra.core.calculo;

import java.util.List;
import java.util.Optional;

import br.com.calibra.core.motor.SaidasExigidas;
import br.com.calibra.core.semaforo.Avaliacao;
import br.com.calibra.core.semaforo.Indicador;

/**
 * Resultado do dimensionamento, em unidades métricas. NaN = não se aplica (ex.: PR no aspirado);
 * null = parte não calculada (ex.: bico avaliado quando nenhum bico foi informado).
 */
public record Dimensionamento(
        Potencia potencia,
        Combustao combustao,
        LadoDoAr ladoDoAr,
        PorInjecao porInjecao,
        BicoNecessario bicoNecessario,
        BicoAvaliado bicoAvaliado,
        BombaNecessaria bomba,
        Esforco esforco,
        SaidasExigidas.Saidas saidas,
        SensorMap sensorMap,
        PontoOperacao pontoOperacao,
        List<Avaliacao> semaforo,
        List<String> recomendacoes) {

    public Dimensionamento {
        semaforo = List.copyOf(semaforo);
        recomendacoes = List.copyOf(recomendacoes);
    }

    /** A avaliação de um indicador, se ele foi avaliado neste cálculo. */
    public Optional<Avaliacao> avaliacao(Indicador indicador) {
        return semaforo.stream().filter(a -> a.indicador() == indicador).findFirst();
    }

    /** Potência no virabrequim. */
    public record Potencia(double virabrequimKw, double cv, double hp, double especificaCvPorLitro) {
    }

    /** Ar e combustível na potência máxima. */
    public record Combustao(
            double afrEstequiometrico,
            double lambdaAlvo,
            double afrAlvo,
            double vazaoArKgS,
            double vazaoArLbMin,
            double combustivelKgH,
            double combustivelLbH,
            double bsfcLbHpH) {
    }

    /**
     * Pressão e temperatura no coletor.
     *
     * @param razaoPressao NaN no aspirado
     * @param t2C NaN no aspirado
     * @param iteracoes passos da iteração MAP ↔ Tm (0 no aspirado)
     */
    public record LadoDoAr(
            boolean sobrealimentado,
            double cilindradaCc,
            double patmKpa,
            double mapKpa,
            double razaoPressao,
            double t2C,
            double tmC,
            int iteracoes,
            double mapSobrePatm) {
    }

    /** Massas por cilindro e por injeção na potência máxima. */
    public record PorInjecao(int injecoesPorCiclo, double cicloMs, double massaArCilindroMg,
            double massaCombustivelPorInjecaoMg) {
    }

    /**
     * Bico necessário pela fórmula Q_bico = mf / (N_bicos × DC_max), sem tempo morto.
     *
     * @param vazaoNominalCm3Min no fluido e na pressão de teste
     */
    public record BicoNecessario(
            int numeroBicos,
            double vazaoMassaGMin,
            double vazaoCombustivelCm3Min,
            double vazaoNominalCm3Min,
            double pressaoTesteKpa,
            double densidadeTesteKgL) {
    }

    /**
     * Avaliação do bico informado. Campos "secundario" são NaN com uma bancada só.
     *
     * @param dutyCycle o maior DC entre as bancadas
     * @param vazaoNominalMinimaCm3Min menor vazão nominal que mantém o DC_max já com o tempo morto (NaN com duas
     *     bancadas ou se o tempo morto ocupar a janela toda)
     * @param tiEfetivoMarchaLentaMs NaN se a marcha lenta não foi informada
     */
    public record BicoAvaliado(
            double vazaoRealCm3Min,
            double vazaoRealSecundarioCm3Min,
            double tempoMortoMs,
            double tempoMortoSecundarioMs,
            double tiEfetivoMs,
            double tiTotalMs,
            double tiTotalSecundarioMs,
            double dutyCycle,
            double vazaoNominalMinimaCm3Min,
            double tiEfetivoMarchaLentaMs) {
    }

    /**
     * Bomba de combustível.
     *
     * @param pressaoKpa pressão exigida: base + pressão do turbo
     * @param folga NaN se a vazão da bomba não foi informada
     */
    public record BombaNecessaria(double vazaoLh, double pressaoKpa, double vazaoComFolgaVerdeLh, double folga) {
    }

    public record Esforco(double bmepBar, double torqueNm, double velocidadePistaoMS) {
    }

    /** Sensor MAP: MAP máximo previsto e faixa mínima (verde). */
    public record SensorMap(double mapMaximoPrevistoKpa, double faixaMinimaKpa) {
    }

    /** Ponto de operação do compressor na potência máxima (só sobrealimentado). */
    public record PontoOperacao(double vazaoArKgS, double vazaoArLbMin, double razaoPressao) {
    }
}
