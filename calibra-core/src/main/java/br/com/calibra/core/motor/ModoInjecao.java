package br.com.calibra.core.motor;

/** Modo de injeção (SPEC 6.3). Define quantas injeções cada bico faz por ciclo (SPEC 7.1). */
public enum ModoInjecao {
    SEQUENCIAL("Sequencial", 1),
    SEMISSEQUENCIAL("Semissequencial", 2),
    SIMULTANEO("Simultâneo", 2);

    private final String rotulo;
    private final int injecoesPorCiclo;

    ModoInjecao(String rotulo, int injecoesPorCiclo) {
        this.rotulo = rotulo;
        this.injecoesPorCiclo = injecoesPorCiclo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Sequencial = 1; semissequencial e simultâneo = 2 (SPEC 7.1). */
    public int injecoesPorCiclo() {
        return injecoesPorCiclo;
    }
}
