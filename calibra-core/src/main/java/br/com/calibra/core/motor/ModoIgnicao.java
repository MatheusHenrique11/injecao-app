package br.com.calibra.core.motor;

/** Modo de ignição (SPEC 6.3). */
public enum ModoIgnicao {
    SEQUENCIAL("Sequencial (bobina individual)"),
    CENTELHA_PERDIDA("Centelha perdida"),
    DISTRIBUIDOR("Distribuidor");

    private final String rotulo;

    ModoIgnicao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
