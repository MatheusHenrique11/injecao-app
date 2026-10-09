package br.com.calibra.core.combustivel;

import java.util.List;

import br.com.calibra.core.memoria.Entrada;

/**
 * Gasolina C: gasolina pura com etanol anidro. O AFR sai da fração em massa de etanol (SPEC 7.2):
 * x = v × 0,789 / (v × 0,789 + (1 − v) × 0,74) e AFR = (1 − x) × 14,7 + x × 9,0.
 *
 * @param fracaoVolumetricaEtanol v, de 0 a 1 (0,32 = E32)
 */
public record GasolinaComEtanol(double fracaoVolumetricaEtanol) implements Composicao {

    public GasolinaComEtanol {
        if (!(fracaoVolumetricaEtanol >= 0 && fracaoVolumetricaEtanol <= 1)) {
            throw new IllegalArgumentException("A fração de etanol deve ficar entre 0 e 1 (0,32 = E32).");
        }
    }

    /** x: fração em massa de etanol na mistura. */
    public double fracaoMassicaEtanol() {
        double v = fracaoVolumetricaEtanol;
        double massaEtanol = v * DENSIDADE_ETANOL;
        return massaEtanol / (massaEtanol + (1 - v) * DENSIDADE_GASOLINA);
    }

    @Override
    public double afrEstequiometrico() {
        double x = fracaoMassicaEtanol();
        return (1 - x) * AFR_GASOLINA + x * AFR_ETANOL;
    }

    @Override
    public String formula() {
        return "x = v × 0,789 / (v × 0,789 + (1 − v) × 0,74); AFR = (1 − x) × 14,7 + x × 9,0";
    }

    @Override
    public List<Entrada> entradas() {
        return List.of(
                Entrada.de("v", fracaoVolumetricaEtanol, "fração em volume"),
                Entrada.de("x", fracaoMassicaEtanol(), "fração em massa"));
    }
}
