package br.com.calibra.core.pecas;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Injeção programável, na forma mínima que a fase 1 usa: saídas e recursos. O perfil completo
 * (tabelas, eixos, modelo de combustível) entra na fase 3 (SPEC 9).
 *
 * @param maxCilindrosSequencial opcional: quantos cilindros a injeção controla em sequencial
 */
public record Injecao(
        String marca,
        String modelo,
        int saidasBico,
        int saidasIgnicao,
        Integer maxCilindrosSequencial,
        Set<Recurso> recursos,
        String fonte,
        LocalDate data) {

    public Injecao {
        Objects.requireNonNull(marca, "marca");
        Objects.requireNonNull(modelo, "modelo");
        Objects.requireNonNull(fonte, "fonte");
        recursos = recursos == null ? Set.of() : Set.copyOf(recursos);
    }

    /** Injeção digitada na calculadora: nome e saídas. */
    public static Injecao informada(String nome, int saidasBico, int saidasIgnicao) {
        return new Injecao(nome, "", saidasBico, saidasIgnicao, null, Set.of(), "informada na calculadora", null);
    }

    public String nome() {
        return (marca + " " + modelo).strip();
    }
}
