package br.com.calibra.core.pecas;

/** Recursos de uma injeção programável que podem ser exigidos pelo projeto (SPEC 7.3 e 9). */
public enum Recurso {
    CONTROLE_PRESSAO_TURBO("Controle de pressão do turbo"),
    BORBOLETA_ELETRONICA("Borboleta eletrônica"),
    SENSOR_ETANOL("Sensor de etanol"),
    DETONACAO("Sensor de detonação"),
    WIDEBAND("Wideband interna"),
    CAMBIO("Controle de câmbio"),
    LARGADA("Controle de largada");

    private final String rotulo;

    Recurso(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
