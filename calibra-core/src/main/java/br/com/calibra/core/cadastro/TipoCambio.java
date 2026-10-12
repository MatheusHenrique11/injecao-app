package br.com.calibra.core.cadastro;

/** Câmbio do veículo (SPEC 6.2). Os dois últimos são de embarcação. */
public enum TipoCambio {
    MANUAL("Manual", false),
    AUTOMATICO("Automático", false),
    AUTOMATIZADO("Automatizado", false),
    DUPLA_EMBREAGEM("Dupla embreagem", false),
    CVT("CVT", false),
    NAUTICO_RABETA_OU_REVERSOR("Rabeta ou reversor", true),
    JATO("Jato", true);

    private final String rotulo;
    private final boolean nautico;

    TipoCambio(String rotulo, boolean nautico) {
        this.rotulo = rotulo;
        this.nautico = nautico;
    }

    public String rotulo() {
        return rotulo;
    }

    public boolean nautico() {
        return nautico;
    }

    /** Só automático, automatizado e dupla embreagem podem ser controlados pela injeção (SPEC 6.2). */
    public boolean podeSerControladoPelaInjecao() {
        return this == AUTOMATICO || this == AUTOMATIZADO || this == DUPLA_EMBREAGEM;
    }
}
