package br.com.calibra.core.orcamento;

/**
 * A categoria de um serviço do catálogo (SPEC 6.6.1, Fase 3B), para organizar a lista. Os serviços gravados antes de
 * a categoria existir são lidos como {@link #OUTRO} (decisão 215).
 */
public enum CategoriaServico {
    PREPARACAO("Preparação"),
    SOM("Som"),
    VIDROS_TRAVAS("Vidros e travas"),
    PARTIDA_CARGA("Partida e carga"),
    ILUMINACAO("Iluminação"),
    DIAGNOSTICO_ELETRICO("Diagnóstico elétrico"),
    INJECAO_PROGRAMAVEL("Injeção programável"),
    OUTRO("Outro");

    private final String rotulo;

    CategoriaServico(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
