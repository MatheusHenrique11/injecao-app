package br.com.calibra.core.oficina;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Um modelo de checklist do catálogo (SPEC 6.6.2, Fase 3B): o nome e os itens, na ordem em que se confere. Ao entrar
 * em execução, a OS copia o modelo de cada serviço (a cópia não muda depois, mesmo que o modelo mude).
 */
public record ChecklistModelo(String nome, List<Item> itens, String fonte, LocalDate data) {

    /**
     * Um item do modelo.
     *
     * @param obrigatorio pendente, trava o "pronto" da OS até uma justificativa
     * @param aVerificar o texto ainda não foi conferido pelo dono (regra 2.7)
     */
    public record Item(String texto, boolean obrigatorio, boolean aVerificar) {

        public Item {
            texto = texto == null ? "" : texto.strip();
        }
    }

    public ChecklistModelo {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(fonte, "fonte");
        itens = itens == null ? List.of() : List.copyOf(itens);
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (itens.isEmpty()) {
            erros.add("O checklist precisa de pelo menos um item.");
        }
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < itens.size(); i++) {
            String texto = itens.get(i).texto();
            if (texto.isEmpty()) {
                erros.add("Item " + (i + 1) + ": escreva o que conferir.");
            } else if (!vistos.add(texto.toLowerCase(Locale.ROOT))) {
                erros.add("Item " + (i + 1) + " (" + texto + "): já está no checklist.");
            }
        }
        return erros;
    }
}
