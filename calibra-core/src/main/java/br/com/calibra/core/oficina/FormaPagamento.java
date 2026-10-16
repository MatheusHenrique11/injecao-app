package br.com.calibra.core.oficina;

/** Como o cliente pagou. O Calibra só registra: não recebe nem cobra nada (SPEC 1). */
public enum FormaPagamento {
    DINHEIRO("Dinheiro"),
    PIX("PIX"),
    CARTAO_DEBITO("Cartão de débito"),
    CARTAO_CREDITO("Cartão de crédito"),
    TRANSFERENCIA("Transferência"),
    OUTRO("Outro");

    private final String rotulo;

    FormaPagamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
