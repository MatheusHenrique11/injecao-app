package br.com.calibra.core.memoria;

import java.util.List;
import java.util.Objects;

/**
 * Todo cálculo do núcleo devolve isto (SPEC 5): o valor, a memória de cálculo, os avisos e a
 * versão do núcleo que fez a conta.
 */
public record Resultado<T>(T valor, List<Passo> memoria, List<Aviso> avisos, String versaoNucleo) {

    public Resultado {
        Objects.requireNonNull(versaoNucleo, "versaoNucleo");
        memoria = List.copyOf(memoria);
        avisos = List.copyOf(avisos);
    }
}
