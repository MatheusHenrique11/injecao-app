package br.com.calibra.core.oficina;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A garantia do serviço, em dias corridos a partir da entrega (SPEC 6.5). O padrão fica em Configurações, com o
 * valor que o dono definir; 0 = sem garantia.
 */
public record Garantia(int dias) {

    public static final Garantia SEM_GARANTIA = new Garantia(0);
    /** 10 anos: mais que isso é quase sempre erro de digitação (ex.: 900 no lugar de 90), e a entrega não muda depois. */
    public static final int MAXIMO_DIAS = 3650;

    /** Tudo o que está errado, em português; vazio = pode gravar. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (dias < 0) {
            erros.add("A garantia não pode ser negativa (0 = sem garantia).");
        } else if (dias > MAXIMO_DIAS) {
            erros.add("A garantia vai até " + MAXIMO_DIAS + " dias (10 anos): confira o número digitado.");
        }
        return erros;
    }

    /** O último dia da garantia para uma entrega nesta data; com 0 dias, o próprio dia da entrega. */
    public LocalDate vence(LocalDate entrega) {
        return entrega.plusDays(dias);
    }
}
