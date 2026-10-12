package br.com.calibra.core.cadastro;

/** Tração (SPEC 6.2), só nos veículos terrestres. */
public enum Tracao {
    DIANTEIRA("Dianteira"),
    TRASEIRA("Traseira"),
    INTEGRAL("Integral");

    private final String rotulo;

    Tracao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
