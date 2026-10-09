package br.com.calibra.core.motor;

/** Construção de pistões e bielas (SPEC 6.3). Entra no semáforo da BMEP. */
public enum Construcao {
    ORIGINAL("Original"),
    FORJADO("Forjado");

    private final String rotulo;

    Construcao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
