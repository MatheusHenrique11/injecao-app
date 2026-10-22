package br.com.calibra.core.pecas;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Injeção programável (SPEC 9): saídas, recursos e, desde a fase 4, o perfil completo (tabelas, eixos, modelo de
 * combustível...), que o mapa base usa. O perfil nunca é null: sem ele, vale {@link PerfilInjecao#vazio()}.
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
        PerfilInjecao perfil,
        String fonte,
        LocalDate data) {

    public Injecao {
        Objects.requireNonNull(marca, "marca");
        Objects.requireNonNull(modelo, "modelo");
        Objects.requireNonNull(fonte, "fonte");
        recursos = recursos == null ? Set.of() : Set.copyOf(recursos);
        perfil = perfil == null ? PerfilInjecao.vazio() : perfil;
    }

    /** Injeção sem o perfil (como era até a fase 3). */
    public Injecao(String marca, String modelo, int saidasBico, int saidasIgnicao, Integer maxCilindrosSequencial,
            Set<Recurso> recursos, String fonte, LocalDate data) {
        this(marca, modelo, saidasBico, saidasIgnicao, maxCilindrosSequencial, recursos, PerfilInjecao.vazio(), fonte,
                data);
    }

    /** Injeção digitada na calculadora: nome e saídas. */
    public static Injecao informada(String nome, int saidasBico, int saidasIgnicao) {
        return new Injecao(nome, "", saidasBico, saidasIgnicao, null, Set.of(), "informada na calculadora", null);
    }

    public String nome() {
        return (marca + " " + modelo).strip();
    }

    /** A mesma injeção com outro perfil. */
    public Injecao comPerfil(PerfilInjecao novo) {
        return new Injecao(marca, modelo, saidasBico, saidasIgnicao, maxCilindrosSequencial, recursos, novo, fonte,
                data);
    }
}
