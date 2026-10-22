package br.com.calibra.core.memoria;

/**
 * Versão do núcleo de cálculo. Vai junto de todo resultado (regra 2.5 da SPEC) para que um cálculo
 * antigo continue preso à versão das fórmulas que o gerou. Mudou fórmula, sobe a versão.
 */
public final class VersaoNucleo {

    /**
     * 1.2.0: o gerador do mapa base (fase 4, bloco 4.3). As contas do dimensionamento não mudaram.
     * <br>1.1.0: mapa do compressor, ponto de torque, meta pela pressão e torque × câmbio (fase 2, bloco 2.4).
     */
    public static final String ATUAL = "1.2.0";

    private VersaoNucleo() {
    }
}
