package br.com.calibra.dados.oficina;

import java.time.LocalDateTime;
import java.util.Objects;

import br.com.calibra.core.oficina.ItemDoChecklist;

/**
 * Um item da cópia do checklist na OS, com a marcação e quem marcou e quando (vazios enquanto ninguém marcou).
 *
 * @param marcadoPor o nome do mecânico, ou null se a marcação foi gravada sem mecânico
 */
public record ItemChecklistSalvo(long id, int ordem, ItemDoChecklist item, Long mecanicoId, String marcadoPor,
        LocalDateTime marcadoEm) {

    public ItemChecklistSalvo {
        Objects.requireNonNull(item, "item");
    }
}
