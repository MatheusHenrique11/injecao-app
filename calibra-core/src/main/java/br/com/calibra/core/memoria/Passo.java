package br.com.calibra.core.memoria;

import java.util.List;
import java.util.Objects;

/**
 * Um passo da memória de cálculo (regra 2.5 da SPEC): a grandeza calculada, a fórmula usada,
 * as entradas e o resultado.
 */
public record Passo(String grandeza, String formula, List<Entrada> entradas, Valor resultado) {

    public Passo {
        Objects.requireNonNull(grandeza, "grandeza");
        Objects.requireNonNull(formula, "formula");
        Objects.requireNonNull(resultado, "resultado");
        entradas = List.copyOf(entradas);
    }
}
