package br.com.calibra.dados.oficina;

import java.util.Set;

import br.com.calibra.core.oficina.StatusOs;

/**
 * Filtro da lista de OS.
 *
 * @param termo cliente, placa, nome da embarcação, veículo ou número da OS; sem acento e sem pontuação
 * @param status vazio = qualquer status
 * @param incluirFinalizadas com o status vazio, também as entregues e as canceladas
 */
public record FiltroOs(String termo, Set<StatusOs> status, boolean incluirFinalizadas) {

    public FiltroOs {
        termo = termo == null ? "" : termo;
        status = status == null ? Set.of() : Set.copyOf(status);
    }

    /** As que estão em andamento (sem as entregues e as canceladas). */
    public static FiltroOs emAndamento() {
        return new FiltroOs("", Set.of(), false);
    }

    /** Todas, inclusive as entregues e as canceladas. */
    public static FiltroOs todas() {
        return new FiltroOs("", Set.of(), true);
    }
}
