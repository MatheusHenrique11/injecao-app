package br.com.calibra.core.cadastro;

/** O tipo de uma instalação do veículo (SPEC 6.2, Fase 3B). */
public enum TipoInstalacao {
    SOM("Som"),
    ALARME_RASTREADOR("Alarme ou rastreador"),
    ILUMINACAO("Iluminação"),
    VIDRO_TRAVA("Vidro ou trava"),
    INJECAO_PROGRAMAVEL("Injeção programável"),
    OUTRA("Outra");

    private final String rotulo;

    TipoInstalacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
