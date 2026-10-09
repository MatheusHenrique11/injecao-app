package br.com.calibra.core.memoria;

import java.util.Objects;

/** Uma entrada usada numa fórmula, com o símbolo que aparece na fórmula. Ex.: P_hp = 394,5 hp. */
public record Entrada(String simbolo, Valor valor) {

    public Entrada {
        Objects.requireNonNull(simbolo, "simbolo");
        Objects.requireNonNull(valor, "valor");
    }

    public static Entrada de(String simbolo, double numero, String unidade) {
        return new Entrada(simbolo, new Valor(numero, unidade));
    }
}
