package br.com.calibra.core.orcamento;

/** Um item do orçamento é peça ou serviço (os totais saem separados). */
public enum TipoItem {
    PECA("Peça"),
    SERVICO("Serviço");

    private final String rotulo;

    TipoItem(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
