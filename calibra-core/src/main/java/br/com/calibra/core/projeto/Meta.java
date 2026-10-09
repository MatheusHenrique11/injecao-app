package br.com.calibra.core.projeto;

import br.com.calibra.core.unidades.Conversoes;

/**
 * Meta do projeto (SPEC 6.4). A potência fica em kW dentro do núcleo; a tela converte de cv.
 *
 * @param perdaTransmissaoPct obrigatória quando a potência é medida na roda (ex.: 15 = 15%)
 * @param rotacaoTorqueMaximo opcional na fase 1; obrigatória para gerar mapa base (fase 3)
 */
public record Meta(
        double potenciaKw,
        PontoMedicao pontoMedicao,
        Double perdaTransmissaoPct,
        int rotacaoPotenciaMaxima,
        Integer rotacaoTorqueMaximo,
        Uso uso) {

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
}
