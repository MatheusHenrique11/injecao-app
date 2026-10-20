package br.com.calibra.dados.oficina;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * A cópia (instantâneo) do checklist de um serviço na OS (SPEC 6.5): de qual serviço e de qual versão do modelo ela
 * veio, com os nomes como eram na hora da cópia, e os itens na ordem do modelo.
 */
public record ChecklistSalvo(long id, long osId, long servicoItemId, String servico, long modeloItemId,
        int modeloVersao, String modeloNome, LocalDateTime criadoEm, List<ItemChecklistSalvo> itens) {

    public ChecklistSalvo {
        Objects.requireNonNull(servico, "servico");
        Objects.requireNonNull(modeloNome, "modeloNome");
        itens = List.copyOf(itens);
    }
}
