package br.com.calibra.core.projeto;

/** Onde a potência meta é medida (SPEC 6.4). */
public enum PontoMedicao {
    VIRABREQUIM("Virabrequim"),
    RODA("Roda");

    private final String rotulo;

    PontoMedicao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
