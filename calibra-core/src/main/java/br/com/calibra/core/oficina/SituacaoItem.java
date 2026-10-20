package br.com.calibra.core.oficina;

/** A situação de um item do checklist da OS (SPEC 6.5, Fase 3B). */
public enum SituacaoItem {
    PENDENTE("pendente"),
    FEITO("feito"),
    NAO_SE_APLICA("não se aplica");

    private final String rotulo;

    SituacaoItem(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
