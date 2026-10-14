package br.com.calibra.core.orcamento;

import java.util.Set;

/**
 * Situação de uma revisão do orçamento: RASCUNHO → ENVIADO → APROVADO ou RECUSADO. Uma revisão nova (de um enviado,
 * aprovado ou recusado) marca a anterior como SUBSTITUIDO. Só o rascunho se edita: depois de enviado, mudar o
 * orçamento exige uma revisão nova.
 */
public enum StatusOrcamento {
    RASCUNHO("Rascunho"),
    ENVIADO("Enviado"),
    APROVADO("Aprovado"),
    RECUSADO("Recusado"),
    SUBSTITUIDO("Substituído");

    private final String rotulo;

    StatusOrcamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Os passos permitidos a partir deste status. */
    public Set<StatusOrcamento> proximos() {
        return switch (this) {
            case RASCUNHO -> Set.of(ENVIADO);
            case ENVIADO -> Set.of(APROVADO, RECUSADO, SUBSTITUIDO);
            case APROVADO, RECUSADO -> Set.of(SUBSTITUIDO);
            case SUBSTITUIDO -> Set.of();
        };
    }

    public boolean podeIrPara(StatusOrcamento destino) {
        return destino != null && proximos().contains(destino);
    }

    /** Só o rascunho muda; o resto, só com revisão nova. */
    public boolean editavel() {
        return this == RASCUNHO;
    }

    /** Dá para abrir uma revisão nova a partir deste (enviado, aprovado ou recusado). */
    public boolean aceitaRevisao() {
        return podeIrPara(SUBSTITUIDO);
    }
}
