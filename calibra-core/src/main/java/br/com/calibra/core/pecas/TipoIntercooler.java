package br.com.calibra.core.pecas;

/** Tipo de intercooler (SPEC 7.3: ar-ar por padrão; ar-água sugerido para arrancada). */
public enum TipoIntercooler {
    AR_AR("Ar-ar"),
    AR_AGUA("Ar-água");

    private final String rotulo;

    TipoIntercooler(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
