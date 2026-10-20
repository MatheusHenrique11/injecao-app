package br.com.calibra.core.oficina;

/** A situação de um código de falha lido no diagnóstico (SPEC 6.6.3, Fase 3B). */
public enum SituacaoCodigo {
    ATIVO("ativo"),
    HISTORICO("histórico"),
    APAGADO("apagado");

    private final String rotulo;

    SituacaoCodigo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
