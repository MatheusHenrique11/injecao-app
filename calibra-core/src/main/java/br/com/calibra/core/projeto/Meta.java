package br.com.calibra.core.projeto;

import br.com.calibra.core.unidades.Conversoes;

/**
 * Meta do projeto (SPEC 6.4). A potência fica em kW dentro do núcleo; a tela converte de cv.
 *
 * <p>Meta pela pressão (núcleo 1.1.0, só sobrealimentado): em vez da potência, a pressão relativa do turbo; a
 * potência sai do cálculo. Aí {@code potenciaKw} é NaN.
 *
 * @param perdaTransmissaoPct obrigatória quando a potência é medida na roda (ex.: 15 = 15%)
 * @param rotacaoTorqueMaximo opcional na fase 1; obrigatória para gerar mapa base (fase 3)
 * @param pressaoRelativaKpa pressão do turbo acima da atmosférica; null na meta pela potência
 */
public record Meta(
        double potenciaKw,
        PontoMedicao pontoMedicao,
        Double perdaTransmissaoPct,
        int rotacaoPotenciaMaxima,
        Integer rotacaoTorqueMaximo,
        Uso uso,
        Double pressaoRelativaKpa) {

    /** Meta pela potência. */
    public Meta(double potenciaKw, PontoMedicao pontoMedicao, Double perdaTransmissaoPct, int rotacaoPotenciaMaxima,
            Integer rotacaoTorqueMaximo, Uso uso) {
        this(potenciaKw, pontoMedicao, perdaTransmissaoPct, rotacaoPotenciaMaxima, rotacaoTorqueMaximo, uso, null);
    }

    /** Meta no virabrequim, em cv. */
    public static Meta noVirabrequim(double potenciaCv, int rotacaoPotenciaMaxima, Uso uso) {
        return new Meta(Conversoes.cvParaKw(potenciaCv), PontoMedicao.VIRABREQUIM, null, rotacaoPotenciaMaxima,
                null, uso);
    }

    /** Meta na roda, em cv, com a perda de transmissão em %. */
    public static Meta naRoda(double potenciaCv, double perdaTransmissaoPct, int rotacaoPotenciaMaxima, Uso uso) {
        return new Meta(Conversoes.cvParaKw(potenciaCv), PontoMedicao.RODA, perdaTransmissaoPct,
                rotacaoPotenciaMaxima, null, uso);
    }

    /**
     * Meta pela pressão relativa do turbo, em kPa (só sobrealimentado).
     *
     * @param perdaTransmissaoPct com a perda, o cálculo também estima a potência na roda; null sem ela
     */
    public static Meta pelaPressao(double pressaoRelativaKpa, Double perdaTransmissaoPct, int rotacaoPotenciaMaxima,
            Integer rotacaoTorqueMaximo, Uso uso) {
        return new Meta(Double.NaN, perdaTransmissaoPct == null ? PontoMedicao.VIRABREQUIM : PontoMedicao.RODA,
                perdaTransmissaoPct, rotacaoPotenciaMaxima, rotacaoTorqueMaximo, uso, pressaoRelativaKpa);
    }

    /** A meta é a pressão do turbo (a potência sai do cálculo). */
    public boolean pelaPressao() {
        return pressaoRelativaKpa != null;
    }

    /** A mesma meta com outra rotação de torque máximo. */
    public Meta comRotacaoTorque(Integer rpm) {
        return new Meta(potenciaKw, pontoMedicao, perdaTransmissaoPct, rotacaoPotenciaMaxima, rpm, uso,
                pressaoRelativaKpa);
    }
}
