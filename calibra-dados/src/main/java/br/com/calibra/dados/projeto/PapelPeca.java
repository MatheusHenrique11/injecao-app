package br.com.calibra.dados.projeto;

/** Para que uma peça do catálogo foi usada num cálculo salvo. */
public enum PapelPeca {
    BICO("Bico", "o bico"),
    BICO_SECUNDARIO("Bico da bancada secundária", "o bico da bancada secundária"),
    BOMBA("Bomba", "a bomba"),
    INTERCOOLER("Intercooler", "o intercooler"),
    SENSOR_MAP("Sensor MAP", "o sensor MAP"),
    INJECAO("Injeção", "a injeção"),
    COMBUSTIVEL("Combustível", "o combustível"),
    TURBO("Turbo", "o turbo"),
    /** O câmbio do veículo (no projeto) ou o escolhido na calculadora, para o indicador torque × câmbio. */
    CAMBIO("Câmbio", "o câmbio");

    private final String rotulo;
    private final String comArtigo;

    PapelPeca(String rotulo, String comArtigo) {
        this.rotulo = rotulo;
        this.comArtigo = comArtigo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Para as mensagens: "o bico mudou no catálogo". */
    public String comArtigo() {
        return comArtigo;
    }
}
