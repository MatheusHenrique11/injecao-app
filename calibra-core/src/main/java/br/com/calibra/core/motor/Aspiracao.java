package br.com.calibra.core.motor;

/** Tipo de aspiração (SPEC 6.3). */
public enum Aspiracao {
    ASPIRADO("Aspirado"),
    TURBO("Turbo"),
    BITURBO("Biturbo"),
    COMPRESSOR("Compressor"),
    COMPRESSOR_E_TURBO("Compressor e turbo");

    private final String rotulo;

    Aspiracao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Tudo que não é aspirado tem pressão positiva no coletor. */
    public boolean sobrealimentado() {
        return this != ASPIRADO;
    }
}
