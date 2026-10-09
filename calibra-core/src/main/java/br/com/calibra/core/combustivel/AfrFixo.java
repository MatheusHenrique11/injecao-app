package br.com.calibra.core.combustivel;

import java.util.List;

import br.com.calibra.core.memoria.Entrada;

/** Combustível com AFR estequiométrico tabelado no catálogo (etanol anidro, E85, metanol). */
public record AfrFixo(double afr) implements Composicao {

    public AfrFixo {
        if (!(afr > 0)) {
            throw new IllegalArgumentException("O AFR estequiométrico deve ser maior que zero.");
        }
    }

    @Override
    public double afrEstequiometrico() {
        return afr;
    }

    @Override
    public String formula() {
        return "AFR = valor do catálogo";
    }

    @Override
    public List<Entrada> entradas() {
        return List.of();
    }
}
