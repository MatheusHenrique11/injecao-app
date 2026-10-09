package br.com.calibra.core.calculo;

import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.projeto.Meta;

/**
 * Tudo que o dimensionamento precisa. Os campos marcados "opcional" podem ser null; use os métodos
 * {@code com...} para preenchê-los.
 *
 * @param lambdaAlvo lambda alvo na potência máxima
 * @param ve eficiência volumétrica na potência máxima (fração), sem padrão
 * @param temperaturaAmbienteC temperatura do ar na entrada
 * @param pressaoBaseKpa pressão de base do combustível: diferencial no bico, com regulador referenciado
 * @param bico opcional: bico a avaliar (único ou da bancada primária)
 * @param bicoSecundario opcional: bico da bancada secundária, só com 2 bicos por cilindro
 * @param tensaoV opcional: tensão para ler tabelas de tempo morto
 * @param marchaLenta opcional: condição de marcha lenta para a checagem do bico
 * @param vazaoBombaDisponivelLh opcional: vazão da bomba na pressão exigida
 * @param faixaSensorMapKpa opcional: faixa do sensor MAP, em kPa absolutos
 * @param injecao opcional: injeção para conferir as saídas
 */
public record EntradaDimensionamento(
        ConfiguracaoMotor motor,
        Meta meta,
        double lambdaAlvo,
        double ve,
        double temperaturaAmbienteC,
        double pressaoBaseKpa,
        Parametros parametros,
        Bico bico,
        Bico bicoSecundario,
        Double tensaoV,
        MarchaLenta marchaLenta,
        Double vazaoBombaDisponivelLh,
        Double faixaSensorMapKpa,
        Injecao injecao) {

    /** Entrada sem nenhum opcional. */
    public static EntradaDimensionamento de(ConfiguracaoMotor motor, Meta meta, double lambdaAlvo, double ve,
            double temperaturaAmbienteC, double pressaoBaseKpa, Parametros parametros) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, null, null, null, null, null, null, null);
    }

    public EntradaDimensionamento comMotor(ConfiguracaoMotor novo) {
        return new EntradaDimensionamento(novo, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao);
    }

    public EntradaDimensionamento comMeta(Meta nova) {
        return new EntradaDimensionamento(motor, nova, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao);
    }

    public EntradaDimensionamento comParametros(Parametros novos) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                novos, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao);
    }

    public EntradaDimensionamento comPressaoBase(double novaKpa) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, novaKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao);
    }

    public EntradaDimensionamento comBico(Bico novo) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, novo, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao);
    }

    public EntradaDimensionamento comBicoSecundario(Bico novo) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, novo, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa, injecao);
    }

    public EntradaDimensionamento comTensao(Double novaV) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, novaV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao);
    }

    public EntradaDimensionamento comMarchaLenta(MarchaLenta nova) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, nova, vazaoBombaDisponivelLh, faixaSensorMapKpa, injecao);
    }

    public EntradaDimensionamento comBombaDisponivel(Double vazaoLh) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoLh, faixaSensorMapKpa, injecao);
    }

    public EntradaDimensionamento comSensorMap(Double faixaKpa) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaKpa, injecao);
    }

    public EntradaDimensionamento comInjecao(Injecao nova) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                nova);
    }
}
