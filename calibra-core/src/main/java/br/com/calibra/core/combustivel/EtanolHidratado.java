package br.com.calibra.core.combustivel;

import java.util.List;

import br.com.calibra.core.memoria.Entrada;

/**
 * Etanol hidratado: a água não queima, então AFR = 9,0 × fração em massa de etanol (SPEC 7.2).
 *
 * @param fracaoMassicaEtanol de 0 a 1 (0,93 = 93% em massa)
 */
public record EtanolHidratado(double fracaoMassicaEtanol) implements Composicao {

    public EtanolHidratado {
        if (!(fracaoMassicaEtanol > 0 && fracaoMassicaEtanol <= 1)) {
            throw new IllegalArgumentException("A fração em massa de etanol deve ficar entre 0 e 1 (0,93 = 93%).");
        }
    }

    @Override
    public double afrEstequiometrico() {
        return AFR_ETANOL * fracaoMassicaEtanol;
    }

    @Override
    public String formula() {
        return "AFR = 9,0 × fração em massa de etanol";
    }

    @Override
    public List<Entrada> entradas() {
        return List.of(Entrada.de("x", fracaoMassicaEtanol, "fração em massa"));
    }
}
