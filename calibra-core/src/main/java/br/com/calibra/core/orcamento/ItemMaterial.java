package br.com.calibra.core.orcamento;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Um item de material do catálogo (SPEC 6.6.4, Fase 3B): cabo, fusível, relé, conector, a ECU... Só para o orçamento:
 * não tem saldo nem estoque. O preço, quando há, é o preço de referência do item do catálogo (a informação comum de
 * todo item).
 *
 * @param unidade como o item se conta no orçamento (un, m, par, kit...)
 */
public record ItemMaterial(String nome, String unidade, String fonte, LocalDate data) {

    public ItemMaterial {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(fonte, "fonte");
        unidade = unidade == null ? "" : unidade.strip();
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (unidade.isEmpty()) {
            erros.add("Informe a unidade do item (un, m, par, kit...).");
        }
        return erros;
    }
}
