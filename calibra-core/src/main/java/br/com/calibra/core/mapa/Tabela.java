package br.com.calibra.core.mapa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.BiFunction;

/**
 * Uma tabela de rotação × carga (SPEC 7.5 e 8.4): os pontos dos dois eixos, do menor para o maior, e o valor de cada
 * célula. {@code valores.get(i).get(j)} é a célula da carga {@code i} e da rotação {@code j}; null é a célula vazia
 * (ex.: a ignição sem fonte). As unidades são as de quem usa a tabela: rpm e kPa no mapa base; rotação e carga
 * relativas no modelo de VE.
 */
public record Tabela(List<Double> rotacoes, List<Double> cargas, List<List<Double>> valores) {

    public Tabela {
        rotacoes = List.copyOf(rotacoes);
        cargas = List.copyOf(cargas);
        if (valores.size() != cargas.size()) {
            throw new IllegalArgumentException("A tabela tem " + valores.size() + " linhas, e o eixo de carga tem "
                    + cargas.size() + " pontos.");
        }
        List<List<Double>> copia = new ArrayList<>();
        for (List<Double> linha : valores) {
            if (linha.size() != rotacoes.size()) {
                throw new IllegalArgumentException("Uma linha da tabela tem " + linha.size() + " células, e o eixo "
                        + "de rotação tem " + rotacoes.size() + " pontos.");
            }
            copia.add(Collections.unmodifiableList(new ArrayList<>(linha)));
        }
        valores = Collections.unmodifiableList(copia);
    }

    /** A tabela com o valor de cada célula dado por {@code valor(rotação, carga)} (null deixa a célula vazia). */
    public static Tabela preencher(List<Double> rotacoes, List<Double> cargas,
            BiFunction<Double, Double, Double> valor) {
        List<List<Double>> linhas = new ArrayList<>();
        for (double carga : cargas) {
            List<Double> linha = new ArrayList<>();
            for (double rotacao : rotacoes) {
                linha.add(valor.apply(rotacao, carga));
            }
            linhas.add(linha);
        }
        return new Tabela(rotacoes, cargas, linhas);
    }

    /** A tabela com todas as células vazias. */
    public static Tabela vazia(List<Double> rotacoes, List<Double> cargas) {
        return preencher(rotacoes, cargas, (rotacao, carga) -> null);
    }

    /** A célula da carga {@code carga} e da rotação {@code rotacao} (índices); null se vazia. */
    public Double valor(int carga, int rotacao) {
        return valores.get(carga).get(rotacao);
    }

    /** Nenhuma célula vazia. */
    public boolean completa() {
        return valores.stream().allMatch(linha -> linha.stream().allMatch(Objects::nonNull));
    }

    /** Todas as células vazias. */
    public boolean semValores() {
        return valores.stream().allMatch(linha -> linha.stream().allMatch(Objects::isNull));
    }

    /** O ponto está dentro dos dois eixos (nas bordas, inclusive). */
    public boolean cobre(double rotacao, double carga) {
        return dentro(rotacoes, rotacao) && dentro(cargas, carga);
    }

    /**
     * Interpolação bilinear no ponto. Nunca extrapola (fora dos eixos, vazio) e não inventa célula: se uma célula
     * que entra na conta estiver vazia, o resultado é vazio. Num ponto exato do eixo, a célula vizinha nem entra.
     */
    public OptionalDouble interpolar(double rotacao, double carga) {
        if (!cobre(rotacao, carga)) {
            return OptionalDouble.empty();
        }
        return bilinear(rotacao, carga);
    }

    /**
     * Interpolação bilinear com o ponto levado para dentro dos eixos: fora da faixa vale a borda (o valor não continua
     * subindo nem descendo). Para o modelo de VE, que precisa de um valor em todo o mapa.
     *
     * @throws IllegalStateException se uma célula que entra na conta estiver vazia
     */
    public double interpolarNaBorda(double rotacao, double carga) {
        double r = Math.clamp(rotacao, rotacoes.getFirst(), rotacoes.getLast());
        double c = Math.clamp(carga, cargas.getFirst(), cargas.getLast());
        return bilinear(r, c).orElseThrow(() -> new IllegalStateException("Tabela com célula vazia."));
    }

    private OptionalDouble bilinear(double rotacao, double carga) {
        int j = indice(rotacoes, rotacao);
        int i = indice(cargas, carga);
        double fr = fracao(rotacoes, j, rotacao);
        double fc = fracao(cargas, i, carga);
        double soma = 0.0;
        double[][] pesos = {{(1 - fc) * (1 - fr), (1 - fc) * fr}, {fc * (1 - fr), fc * fr}};
        for (int di = 0; di < 2; di++) {
            for (int dj = 0; dj < 2; dj++) {
                double peso = pesos[di][dj];
                if (peso == 0.0) {
                    continue;
                }
                Double celula = valor(Math.min(i + di, cargas.size() - 1), Math.min(j + dj, rotacoes.size() - 1));
                if (celula == null) {
                    return OptionalDouble.empty();
                }
                soma += peso * celula;
            }
        }
        return OptionalDouble.of(soma);
    }

    /** O índice do ponto do eixo logo antes (ou igual) ao valor; o penúltimo para o último ponto. */
    private static int indice(List<Double> eixo, double valor) {
        if (eixo.size() == 1) {
            return 0;
        }
        for (int k = 0; k < eixo.size() - 1; k++) {
            if (valor < eixo.get(k + 1)) {
                return k;
            }
        }
        return eixo.size() - 2;
    }

    private static double fracao(List<Double> eixo, int k, double valor) {
        if (eixo.size() == 1) {
            return 0.0;
        }
        return Math.clamp((valor - eixo.get(k)) / (eixo.get(k + 1) - eixo.get(k)), 0.0, 1.0);
    }

    private static boolean dentro(List<Double> eixo, double valor) {
        return !eixo.isEmpty() && valor >= eixo.getFirst() && valor <= eixo.getLast();
    }
}
