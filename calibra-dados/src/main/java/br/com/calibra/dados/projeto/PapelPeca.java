package br.com.calibra.dados.projeto;

/** Para que uma peça do catálogo foi usada num cálculo salvo ou num mapa base salvo (fase 4). */
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
    CAMBIO("Câmbio", "o câmbio"),
    /** O modelo de VE da família, no mapa base (fase 4). */
    MODELO_VE("Modelo de VE", "o modelo de VE"),
    /** O modelo de ignição usado como fonte do avanço, no mapa base (fase 4). */
    MODELO_IGNICAO("Modelo de ignição", "o modelo de ignição");

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
