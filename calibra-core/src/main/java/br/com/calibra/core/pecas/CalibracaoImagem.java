package br.com.calibra.core.pecas;

import java.util.ArrayList;
import java.util.List;

/**
 * Como a imagem do mapa do fabricante se liga aos eixos: dois pontos conhecidos em cada eixo, com o pixel e o valor.
 * Os eixos dos mapas de compressor são lineares. Fica guardada com o turbo para o digitalizador reabrir o desenho por
 * cima da mesma imagem.
 *
 * @param xValor1 vazão no eixo original do mapa (lb/min ou kg/s, ver {@link MapaCompressor#eixoOriginal()})
 * @param yValor1 razão de pressão
 */
public record CalibracaoImagem(double xPixel1, double xValor1, double xPixel2, double xValor2, double yPixel1,
        double yValor1, double yPixel2, double yValor2) {

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (xPixel1 == xPixel2 || xValor1 == xValor2) {
            erros.add("Eixo de vazão: marque dois pontos diferentes, com valores diferentes.");
        }
        if (yPixel1 == yPixel2 || yValor1 == yValor2) {
            erros.add("Eixo de razão de pressão: marque dois pontos diferentes, com valores diferentes.");
        }
        return erros;
    }

    /** Vazão, no eixo original, de uma coluna de pixels. */
    public double vazao(double xPixel) {
        return linear(xPixel, xPixel1, xValor1, xPixel2, xValor2);
    }

    public double xPixel(double vazao) {
        return linear(vazao, xValor1, xPixel1, xValor2, xPixel2);
    }

    /** Razão de pressão de uma linha de pixels. */
    public double razaoPressao(double yPixel) {
        return linear(yPixel, yPixel1, yValor1, yPixel2, yValor2);
    }

    public double yPixel(double razaoPressao) {
        return linear(razaoPressao, yValor1, yPixel1, yValor2, yPixel2);
    }

    private static double linear(double x, double x1, double y1, double x2, double y2) {
        return y1 + (x - x1) * (y2 - y1) / (x2 - x1);
    }
}
