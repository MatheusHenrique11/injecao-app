package br.com.calibra.core.motor;

/** Perfil do comando de válvulas (SPEC 6.3). */
public enum Comando {
    ORIGINAL("Original"),
    LEVE("Leve"),
    MEDIO("Médio"),
    AGRESSIVO("Agressivo");

    private final String rotulo;

    Comando(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
