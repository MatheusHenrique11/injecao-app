package br.com.calibra.core.oficina;

import java.util.Set;

/**
 * Status da ordem de serviço (SPEC 6.5). Até APROVADA, quem move a OS é o orçamento (enviado, aprovado, recusado);
 * daí em diante, a execução. Só se executa com orçamento aprovado (Código de Defesa do Consumidor, art. 39, VI).
 * ENTREGUE e CANCELADA são finais: a OS não muda mais.
 */
public enum StatusOs {
    ABERTA("Aberta"),
    AGUARDANDO_APROVACAO("Aguardando aprovação"),
    APROVADA("Aprovada"),
    EM_EXECUCAO("Em execução"),
    AGUARDANDO_PECAS("Aguardando peças"),
    PRONTA("Pronta"),
    ENTREGUE("Entregue"),
    CANCELADA("Cancelada");

    private final String rotulo;

    StatusOs(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Os passos permitidos a partir deste status. Cancelar vale a qualquer momento antes de entregar. */
    public Set<StatusOs> proximos() {
        return switch (this) {
            case ABERTA -> Set.of(AGUARDANDO_APROVACAO, CANCELADA);
            case AGUARDANDO_APROVACAO -> Set.of(APROVADA, ABERTA, CANCELADA);
            case APROVADA -> Set.of(EM_EXECUCAO, CANCELADA);
            case EM_EXECUCAO -> Set.of(AGUARDANDO_PECAS, PRONTA, CANCELADA);
            case AGUARDANDO_PECAS -> Set.of(EM_EXECUCAO, CANCELADA);
            case PRONTA -> Set.of(ENTREGUE, EM_EXECUCAO, CANCELADA);
            case ENTREGUE, CANCELADA -> Set.of();
        };
    }

    public boolean podeIrPara(StatusOs destino) {
        return destino != null && proximos().contains(destino);
    }

    /** Entregue ou cancelada: a OS não muda mais (só os recebimentos e os anexos, que ficam à parte). */
    public boolean finalizada() {
        return this == ENTREGUE || this == CANCELADA;
    }

    /** Antes da aprovação, o orçamento ainda move a OS; depois, uma revisão nova não muda o status. */
    public boolean antesDaAprovacao() {
        return this == ABERTA || this == AGUARDANDO_APROVACAO;
    }
}
