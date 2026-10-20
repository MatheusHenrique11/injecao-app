package br.com.calibra.core.oficina;

import java.util.Objects;

/**
 * Um item do checklist da OS: a cópia do item do modelo (o texto, "obrigatório" e "a verificar", que não mudam) e a
 * marcação de agora (SPEC 6.5, Fase 3B).
 */
public record ItemDoChecklist(String texto, boolean obrigatorio, boolean aVerificar, Marcacao marcacao) {

    public ItemDoChecklist {
        Objects.requireNonNull(texto, "texto");
        Objects.requireNonNull(marcacao, "marcacao");
    }

    /** A cópia de um item do modelo, ainda pendente. */
    public static ItemDoChecklist copia(ChecklistModelo.Item item) {
        return new ItemDoChecklist(item.texto(), item.obrigatorio(), item.aVerificar(),
                new Marcacao(SituacaoItem.PENDENTE, "", ""));
    }

    /**
     * O item obrigatório que ainda trava o "pronto": pendente, ou "não se aplica" sem justificativa (que a marcação
     * não deixa gravar, mas a regra não depende disso).
     */
    public boolean pendente() {
        Marcacao m = marcacao;
        return obrigatorio && (m.situacao() == SituacaoItem.PENDENTE
                || (m.situacao() == SituacaoItem.NAO_SE_APLICA && m.justificativa().isEmpty()));
    }
}
