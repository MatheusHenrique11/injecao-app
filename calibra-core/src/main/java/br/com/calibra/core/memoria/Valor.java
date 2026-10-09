package br.com.calibra.core.memoria;

import java.util.Objects;

/** Um número com a sua unidade, como aparece na memória de cálculo. Ex.: 241,0 kPa. */
public record Valor(double numero, String unidade) {

    public Valor {
        Objects.requireNonNull(unidade, "unidade");
    }
}
