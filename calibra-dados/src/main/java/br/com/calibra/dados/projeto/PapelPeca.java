package br.com.calibra.dados.projeto;

/** Para que uma peça do catálogo foi usada num cálculo salvo. */
public enum PapelPeca {
    BICO("Bico"),
    BICO_SECUNDARIO("Bico da bancada secundária"),
    BOMBA("Bomba"),
    INTERCOOLER("Intercooler"),
    SENSOR_MAP("Sensor MAP"),
    INJECAO("Injeção"),
    COMBUSTIVEL("Combustível");

    private final String rotulo;

    PapelPeca(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
