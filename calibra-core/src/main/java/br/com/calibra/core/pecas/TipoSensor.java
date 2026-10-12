package br.com.calibra.core.pecas;

/** Tipo de sensor do catálogo. Só o MAP entra no cálculo hoje (semáforo da faixa do sensor, SPEC 7.4). */
public enum TipoSensor {
    MAP("MAP (pressão absoluta)", "kPa abs."),
    PRESSAO("Pressão (combustível, óleo)", "kPa"),
    TEMPERATURA("Temperatura", "°C"),
    OUTRO("Outro", "");

    private final String rotulo;
    private final String unidade;

    TipoSensor(String rotulo, String unidade) {
        this.rotulo = rotulo;
        this.unidade = unidade;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Unidade da faixa do sensor. */
    public String unidade() {
        return unidade;
    }
}
