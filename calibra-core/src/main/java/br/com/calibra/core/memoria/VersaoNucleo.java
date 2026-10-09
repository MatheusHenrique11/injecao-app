package br.com.calibra.core.memoria;

/**
 * Versão do núcleo de cálculo. Vai junto de todo resultado (regra 2.5 da SPEC) para que um cálculo
 * antigo continue preso à versão das fórmulas que o gerou. Mudou fórmula, sobe a versão.
 */
public final class VersaoNucleo {

    public static final String ATUAL = "1.0.0";

    private VersaoNucleo() {
    }
}
