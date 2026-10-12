package br.com.calibra.core.cadastro;

/**
 * Tipo do veículo (SPEC 6.2). Define quais campos aparecem: placa, chassi e tração nos terrestres;
 * nome, inscrição e casco nos náuticos. OUTRO conta como terrestre (decisão 61).
 */
public enum TipoVeiculo {
    CARRO("Carro", false),
    PICAPE_UTILITARIO("Picape / utilitário", false),
    MOTO("Moto", false),
    QUADRICICLO_UTV("Quadriciclo / UTV", false),
    BARCO("Barco", true),
    LANCHA("Lancha", true),
    JET_SKI("Jet ski", true),
    OUTRO("Outro", false);

    private final String rotulo;
    private final boolean nautico;

    TipoVeiculo(String rotulo, boolean nautico) {
        this.rotulo = rotulo;
        this.nautico = nautico;
    }

    public String rotulo() {
        return rotulo;
    }

    public boolean nautico() {
        return nautico;
    }

    public boolean terrestre() {
        return !nautico;
    }

    /** Unidade do uso acumulado: km nos terrestres, horas de motor nos náuticos (SPEC 6.2). */
    public String unidadeUso() {
        return nautico ? "h" : "km";
    }
}
