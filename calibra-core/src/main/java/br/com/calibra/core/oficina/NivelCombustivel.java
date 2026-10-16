package br.com.calibra.core.oficina;

/** Nível do tanque na entrada do veículo, como no marcador do painel. */
public enum NivelCombustivel {
    RESERVA("Reserva"),
    UM_QUARTO("1/4"),
    MEIO("1/2"),
    TRES_QUARTOS("3/4"),
    CHEIO("Cheio");

    private final String rotulo;

    NivelCombustivel(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
