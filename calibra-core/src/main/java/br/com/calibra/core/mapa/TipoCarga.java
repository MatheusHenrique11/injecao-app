package br.com.calibra.core.mapa;

/** Como a carga é medida no eixo de uma tabela da injeção (SPEC 7.5 e 9). */
public enum TipoCarga {
    MAP_KPA("MAP (kPa absolutos)", "kPa"),
    TPS_PERCENTUAL("TPS (%)", "%");

    private final String rotulo;
    private final String unidade;

    TipoCarga(String rotulo, String unidade) {
        this.rotulo = rotulo;
        this.unidade = unidade;
    }

    public String rotulo() {
        return rotulo;
    }

    public String unidade() {
        return unidade;
    }
}
