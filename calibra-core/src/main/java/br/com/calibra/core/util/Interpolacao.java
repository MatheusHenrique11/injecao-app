package br.com.calibra.core.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;

/** Interpolação linear entre pontos cadastrados. Nunca extrapola: fora da faixa, devolve vazio. */
public final class Interpolacao {

    /** Diferença abaixo da qual dois valores de x são considerados iguais. */
    private static final double TOLERANCIA = 1e-9;

    /** Um ponto (x, y) de uma curva. */
    public record Ponto(double x, double y) {
    }

    /**
     * Um ponto de uma família de curvas: cada valor de {@code curva} (ex.: tensão) tem a sua curva
     * de y contra x (ex.: vazão contra pressão).
     */
    public record PontoDeCurva(double curva, double x, double y) {
    }

    private Interpolacao() {
    }

    /**
     * Interpola y no valor x. Com um ponto só, devolve y apenas se x for igual ao do ponto.
     *
     * @return vazio se x estiver fora da faixa dos pontos ou se a lista estiver vazia
     */
    public static OptionalDouble linear(List<Ponto> pontos, double x) {
        if (pontos.isEmpty() || !Double.isFinite(x)) {
            return OptionalDouble.empty();
        }
        List<Ponto> ordenados = new ArrayList<>(pontos);
        ordenados.sort(Comparator.comparingDouble(Ponto::x));

        for (Ponto ponto : ordenados) {
            if (Math.abs(ponto.x() - x) <= TOLERANCIA) {
                return OptionalDouble.of(ponto.y());
            }
        }
        for (int i = 0; i < ordenados.size() - 1; i++) {
            Ponto a = ordenados.get(i);
            Ponto b = ordenados.get(i + 1);
            if (x > a.x() && x < b.x()) {
                double fracao = (x - a.x()) / (b.x() - a.x());
                return OptionalDouble.of(a.y() + fracao * (b.y() - a.y()));
            }
        }
        return OptionalDouble.empty();
    }

    /**
     * Interpola numa família de curvas: primeiro em x dentro de cada curva, depois entre as curvas.
     * Nunca extrapola.
     *
     * @param curvaUnicaValeParaTodas com uma curva só: true = usa essa curva para qualquer valor de
     *     {@code curva} (ex.: tempo morto medido numa pressão só); false = só vale no próprio valor
     */
    public static OptionalDouble porCurvas(List<PontoDeCurva> pontos, double curva, double x,
            boolean curvaUnicaValeParaTodas) {
        TreeMap<Double, List<Ponto>> curvas = new TreeMap<>();
        for (PontoDeCurva p : pontos) {
            curvas.computeIfAbsent(p.curva(), chave -> new ArrayList<>()).add(new Ponto(p.x(), p.y()));
        }
        if (curvas.isEmpty()) {
            return OptionalDouble.empty();
        }
        if (curvas.size() == 1) {
            if (!curvaUnicaValeParaTodas && Math.abs(curvas.firstKey() - curva) > TOLERANCIA) {
                return OptionalDouble.empty();
            }
            return linear(curvas.firstEntry().getValue(), x);
        }
        List<Ponto> naCurvaPedida = new ArrayList<>();
        for (Map.Entry<Double, List<Ponto>> entrada : curvas.entrySet()) {
            OptionalDouble y = linear(entrada.getValue(), x);
            if (y.isPresent()) {
                naCurvaPedida.add(new Ponto(entrada.getKey(), y.getAsDouble()));
            }
        }
        return linear(naCurvaPedida, curva);
    }
}
