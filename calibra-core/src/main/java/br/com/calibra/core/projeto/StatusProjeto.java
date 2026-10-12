package br.com.calibra.core.projeto;

import java.util.Optional;

/**
 * Situação do projeto (SPEC 6.4): RASCUNHO → DIMENSIONADO → ORCADO → APROVADO → MAPA_BASE → EM_ACERTO →
 * CONCLUIDO, um passo de cada vez; ARQUIVADO a qualquer momento.
 */
public enum StatusProjeto {
    RASCUNHO("Rascunho"),
    DIMENSIONADO("Dimensionado"),
    ORCADO("Orçado"),
    APROVADO("Aprovado"),
    MAPA_BASE("Mapa base"),
    EM_ACERTO("Em acerto"),
    CONCLUIDO("Concluído"),
    ARQUIVADO("Arquivado");

    private final String rotulo;

    StatusProjeto(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** O passo seguinte na sequência; vazio em CONCLUIDO e ARQUIVADO. */
    public Optional<StatusProjeto> proximo() {
        if (this == CONCLUIDO || this == ARQUIVADO) {
            return Optional.empty();
        }
        return Optional.of(values()[ordinal() + 1]);
    }

    /**
     * Pode mudar para {@code destino}? Para a frente, só o passo seguinte. Arquivar, a qualquer momento.
     * Um projeto arquivado volta para um status da sequência: quem guarda qual era o status antes de arquivar
     * é o banco.
     */
    public boolean podeIrPara(StatusProjeto destino) {
        if (destino == null || destino == this) {
            return false;
        }
        if (destino == ARQUIVADO || this == ARQUIVADO) {
            return true;
        }
        return proximo().filter(destino::equals).isPresent();
    }
}
