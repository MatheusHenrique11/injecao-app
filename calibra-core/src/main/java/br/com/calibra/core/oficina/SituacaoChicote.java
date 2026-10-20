package br.com.calibra.core.oficina;

/**
 * Onde está o chicote da ficha (SPEC 6.6.5, Fase 3B). Em confecção e testado se escolhem na ficha; entregue vem sozinho
 * quando a OS é entregue.
 */
public enum SituacaoChicote {
    EM_CONFECCAO("em confecção"),
    TESTADO("testado"),
    ENTREGUE("entregue");

    private final String rotulo;

    SituacaoChicote(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
