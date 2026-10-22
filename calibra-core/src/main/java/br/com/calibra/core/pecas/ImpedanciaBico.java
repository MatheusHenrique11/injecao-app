package br.com.calibra.core.pecas;

/** A impedância de bico que as saídas da injeção aceitam (SPEC 9), como diz o manual dela. */
public enum ImpedanciaBico {
    ALTA("Alta"),
    BAIXA("Baixa"),
    ALTA_E_BAIXA("Alta e baixa");

    private final String rotulo;

    ImpedanciaBico(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
