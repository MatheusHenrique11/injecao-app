package br.com.calibra.core.oficina;

import br.com.calibra.core.orcamento.Dinheiro;

/** Situação do pagamento da OS, comparando o que foi recebido com o valor da OS (o orçamento aprovado). */
public enum SituacaoPagamento {
    PENDENTE("Pendente"),
    PARCIAL("Parcial"),
    PAGO("Pago");

    private final String rotulo;

    SituacaoPagamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /**
     * @param valor o valor da OS (total da última revisão aprovada do orçamento)
     * @param recebido a soma dos recebimentos que não foram cancelados
     */
    public static SituacaoPagamento de(Dinheiro valor, Dinheiro recebido) {
        if (recebido.compareTo(valor) >= 0) {
            return PAGO;
        }
        return recebido.compareTo(Dinheiro.ZERO) > 0 ? PARCIAL : PENDENTE;
    }

    /** Quanto falta receber (nunca negativo). */
    public static Dinheiro falta(Dinheiro valor, Dinheiro recebido) {
        Dinheiro falta = valor.menos(recebido);
        return falta.negativo() ? Dinheiro.ZERO : falta;
    }
}
