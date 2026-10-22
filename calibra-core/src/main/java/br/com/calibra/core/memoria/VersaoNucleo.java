package br.com.calibra.core.memoria;

/**
 * Versão do núcleo de cálculo. Vai junto de todo resultado (regra 2.5 da SPEC) para que um cálculo
 * antigo continue preso à versão das fórmulas que o gerou. Mudou fórmula, sobe a versão.
 *
 * <p>Um resultado só fica desatualizado pelo núcleo quando as contas <em>dele</em> mudaram depois (decisão 295): o
 * mapa base (1.2.0) não muda as contas do dimensionamento, então um cálculo feito na 1.1.0 continua em dia.
 */
public final class VersaoNucleo {

    /**
     * 1.2.0: o gerador do mapa base (fase 4, bloco 4.3). As contas do dimensionamento não mudaram.
     * <br>1.1.0: mapa do compressor, ponto de torque, meta pela pressão e torque × câmbio (fase 2, bloco 2.4).
     */
    public static final String ATUAL = "1.2.0";

    /** A última versão que mudou as contas do dimensionamento. */
    public static final String ULTIMA_MUDANCA_DO_DIMENSIONAMENTO = "1.1.0";

    /** A última versão que mudou as contas do mapa base. */
    public static final String ULTIMA_MUDANCA_DO_MAPA_BASE = "1.2.0";

    private VersaoNucleo() {
    }

    /**
     * A versão é anterior à referência, comparando os números um a um ("1.1.0" é anterior a "1.2.0" e "1.10.0" não é
     * anterior a "1.9.0"). Versão vazia ou estranha conta como anterior.
     */
    public static boolean anterior(String versao, String referencia) {
        int[] a = numeros(versao);
        int[] b = numeros(referencia);
        if (a == null) {
            return true;
        }
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0;
            int y = i < b.length ? b[i] : 0;
            if (x != y) {
                return x < y;
            }
        }
        return false;
    }

    private static int[] numeros(String versao) {
        if (versao == null || !versao.matches("\\d+(\\.\\d+)*")) {
            return null;
        }
        String[] partes = versao.split("\\.");
        int[] numeros = new int[partes.length];
        for (int i = 0; i < partes.length; i++) {
            numeros[i] = Integer.parseInt(partes[i]);
        }
        return numeros;
    }
}
