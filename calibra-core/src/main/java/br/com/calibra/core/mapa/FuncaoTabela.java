package br.com.calibra.core.mapa;

/**
 * As tabelas que o mapa base preenche (SPEC 7.5). As demais (partida, aquecimento, aceleração rápida, correções)
 * ficam no checklist, apontando para o assistente da própria injeção.
 */
public enum FuncaoTabela {
    COMBUSTIVEL("Combustível"),
    IGNICAO("Ignição"),
    LAMBDA_ALVO("Lambda alvo");

    private final String rotulo;

    FuncaoTabela(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
