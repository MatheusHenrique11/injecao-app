package br.com.calibra.core.orcamento;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Um serviço do catálogo de serviços (SPEC 6.5, bloco 3.3): a mão de obra padrão da oficina, com o tempo de
 * referência. O preço, quando há, é o preço de referência do item do catálogo (a informação comum de todo item).
 *
 * @param horas o tempo padrão; null = não informado (o serviço precisa ter preço)
 */
public record ServicoPadrao(String nome, Double horas, String fonte, LocalDate data) {

    public ServicoPadrao {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(fonte, "fonte");
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (horas != null && !(horas > 0)) {
            erros.add("As horas do serviço devem ser maiores que zero.");
        }
        return erros;
    }
}
