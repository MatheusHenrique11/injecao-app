package br.com.calibra.core.projeto;

/** Uso do veículo (SPEC 6.4). Muda margens e peças, não só o mapa. */
public enum Uso {
    RUA("Rua"),
    ARRANCADA("Arrancada"),
    CIRCUITO_ENDURANCE("Circuito / endurance"),
    OFF_ROAD("Off-road"),
    NAUTICO("Náutico");

    private final String rotulo;

    Uso(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
