package br.com.calibra.core.calculo;

import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.Cambio;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Turbo;
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
 * @param tensaoV opcional: tensão para ler tabelas de tempo morto e a curva da bomba
 * @param marchaLenta opcional: condição de marcha lenta para a checagem do bico
 * @param vazaoBombaDisponivelLh opcional: vazão da bomba na pressão exigida, digitada
 * @param faixaSensorMapKpa opcional: faixa do sensor MAP, em kPa absolutos
 * @param injecao opcional: injeção para conferir as saídas
 * @param bomba opcional: bomba do catálogo; a vazão disponível sai da curva, na pressão exigida e na tensão
 *     (no lugar de {@code vazaoBombaDisponivelLh}, nunca junto)
 * @param veTorque opcional: VE na rotação de torque máximo; sem ela, vale a VE da potência (núcleo 1.1.0)
 * @param turbo opcional: turbo do catálogo; com o mapa, os pontos de potência e de torque são conferidos nele
 * @param cambio opcional: câmbio do veículo; com o torque máximo dele, entra o indicador "Torque × câmbio"
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
        Injecao injecao,
        Bomba bomba,
        Double veTorque,
        Turbo turbo,
        Cambio cambio) {

    /** Entrada sem VE do torque, turbo e câmbio (como era até o núcleo 1.0.0). */
    public EntradaDimensionamento(ConfiguracaoMotor motor, Meta meta, double lambdaAlvo, double ve,
            double temperaturaAmbienteC, double pressaoBaseKpa, Parametros parametros, Bico bico, Bico bicoSecundario,
            Double tensaoV, MarchaLenta marchaLenta, Double vazaoBombaDisponivelLh, Double faixaSensorMapKpa,
            Injecao injecao, Bomba bomba) {
        this(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa, parametros, bico, bicoSecundario,
                tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa, injecao, bomba, null, null, null);
    }

    /** Entrada sem nenhum opcional. */
    public static EntradaDimensionamento de(ConfiguracaoMotor motor, Meta meta, double lambdaAlvo, double ve,
            double temperaturaAmbienteC, double pressaoBaseKpa, Parametros parametros) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, null, null, null, null, null, null, null, null, null, null, null);
    }

    public EntradaDimensionamento comMotor(ConfiguracaoMotor novo) {
        return new EntradaDimensionamento(novo, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao, bomba, veTorque, turbo, cambio);
    }

    public EntradaDimensionamento comMeta(Meta nova) {
        return new EntradaDimensionamento(motor, nova, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao, bomba, veTorque, turbo, cambio);
    }

    public EntradaDimensionamento comParametros(Parametros novos) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                novos, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao, bomba, veTorque, turbo, cambio);
    }

    public EntradaDimensionamento comPressaoBase(double novaKpa) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, novaKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao, bomba, veTorque, turbo, cambio);
    }

    public EntradaDimensionamento comBico(Bico novo) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, novo, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao, bomba, veTorque, turbo, cambio);
    }

    public EntradaDimensionamento comBicoSecundario(Bico novo) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, novo, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa, injecao,
                bomba, veTorque, turbo, cambio);
    }

    public EntradaDimensionamento comTensao(Double novaV) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, novaV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao, bomba, veTorque, turbo, cambio);
    }

    public EntradaDimensionamento comMarchaLenta(MarchaLenta nova) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, nova, vazaoBombaDisponivelLh, faixaSensorMapKpa, injecao,
                bomba, veTorque, turbo, cambio);
    }

    /** Vazão da bomba digitada (tira a bomba do catálogo, se houver). */
    public EntradaDimensionamento comBombaDisponivel(Double vazaoLh) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoLh, faixaSensorMapKpa, injecao,
                vazaoLh == null ? bomba : null, veTorque, turbo, cambio);
    }

    /** Bomba do catálogo, com a curva (tira a vazão digitada, se houver). */
    public EntradaDimensionamento comBomba(Bomba nova) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, nova == null ? vazaoBombaDisponivelLh : null,
                faixaSensorMapKpa, injecao, nova, veTorque, turbo, cambio);
    }

    public EntradaDimensionamento comSensorMap(Double faixaKpa) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaKpa, injecao,
                bomba, veTorque, turbo, cambio);
    }

    public EntradaDimensionamento comInjecao(Injecao nova) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                nova, bomba, veTorque, turbo, cambio);
    }

    public EntradaDimensionamento comVeTorque(Double nova) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao, bomba, nova, turbo, cambio);
    }

    public EntradaDimensionamento comTurbo(Turbo novo) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao, bomba, veTorque, novo, cambio);
    }

    public EntradaDimensionamento comCambio(Cambio novo) {
        return new EntradaDimensionamento(motor, meta, lambdaAlvo, ve, temperaturaAmbienteC, pressaoBaseKpa,
                parametros, bico, bicoSecundario, tensaoV, marchaLenta, vazaoBombaDisponivelLh, faixaSensorMapKpa,
                injecao, bomba, veTorque, turbo, novo);
    }

    /** A VE do ponto de torque: a informada, ou a da potência. */
    public double veNoTorque() {
        return veTorque != null ? veTorque : ve;
    }
}
