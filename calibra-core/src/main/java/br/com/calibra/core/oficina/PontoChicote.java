package br.com.calibra.core.oficina;

/**
 * Um ponto do chicote (SPEC 6.6.5): a função do fio (ex.: bico do cilindro 1, bobina, TPS, MAP), o conector, o pino da
 * injeção e a observação, como o dono escreve. O app não valida pinagem nem bitola.
 *
 * @param aVerificar o ponto ainda não foi conferido no manual da injeção (regra 2.7)
 */
public record PontoChicote(String funcao, String conector, String pinoDaInjecao, String observacao,
        boolean aVerificar) {

    public PontoChicote {
        funcao = limpo(funcao);
        conector = limpo(conector);
        pinoDaInjecao = limpo(pinoDaInjecao);
        observacao = limpo(observacao);
    }

    private static String limpo(String texto) {
        return texto == null ? "" : texto.strip();
    }
}
