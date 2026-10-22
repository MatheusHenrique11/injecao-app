package br.com.calibra.core.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Números no formato brasileiro: vírgula decimal e ponto de milhar (SPEC 2.9).
 *
 * <p>A leitura é rígida de propósito: "3.5" é recusado, porque no Brasil o ponto separa milhar
 * e o número poderia virar 35 sem ninguém perceber.
 */
public final class NumeroBr {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    /** Inteiro com ou sem pontos de milhar bem colocados, e parte decimal opcional com vírgula. */
    private static final Pattern FORMATO = Pattern.compile("-?(\\d{1,3}(\\.\\d{3})+|\\d+)(,\\d+)?");

    private NumeroBr() {
    }

    /**
     * Lê um número digitado na tela. Aceita "1050", "1.050", "3,5", "1.050,5" e "-2,5".
     *
     * @throws NumberFormatException com mensagem em português se o texto não for um número válido
     */
    public static double ler(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new NumberFormatException("Campo vazio.");
        }
        String limpo = texto.strip();
        if (!FORMATO.matcher(limpo).matches()) {
            throw new NumberFormatException(
                    "\"" + limpo + "\" não é um número válido: use vírgula para decimais (ex.: 3,5).");
        }
        return Double.parseDouble(limpo.replace(".", "").replace(',', '.'));
    }

    /**
     * Lê uma lista de números, como o eixo de uma tabela copiado do software da injeção: separados por espaço,
     * tabulação, ponto e vírgula ou um por linha (a vírgula fica de fora: é o separador decimal). Ex.: "500 1.000
     * 1.500" ou uma coluna colada. Texto vazio dá a lista vazia.
     *
     * @throws NumberFormatException com a posição e a explicação, em português, no primeiro número inválido
     */
    public static List<Double> lerLista(String texto) {
        List<Double> numeros = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return numeros;
        }
        String[] partes = texto.strip().split("[\\s;]+");
        for (int i = 0; i < partes.length; i++) {
            try {
                numeros.add(ler(partes[i]));
            } catch (NumberFormatException erro) {
                throw new NumberFormatException("número " + (i + 1) + ": " + erro.getMessage());
            }
        }
        return numeros;
    }

    /**
     * Como {@link #ler(String)}, mas exato (sem o arredondamento do ponto flutuante): para quantidades de orçamento,
     * que multiplicam dinheiro. "2,5" vira 2.5 exatamente.
     *
     * @throws NumberFormatException com mensagem em português se o texto não for um número válido
     */
    public static BigDecimal lerDecimal(String texto) {
        ler(texto);
        return new BigDecimal(texto.strip().replace(".", "").replace(',', '.'));
    }

    /** Formata com um número fixo de casas decimais e ponto de milhar. Ex.: 1057,8 com 1 casa → "1.057,8". */
    public static String formatar(double valor, int casasDecimais) {
        return formatar(valor, casasDecimais, casasDecimais);
    }

    /**
     * Formata com pelo menos {@code minimoCasas} e no máximo {@code maximoCasas} casas, sem zeros sobrando:
     * com 1 a 3 casas, 10,5 → "10,5", 9,75 → "9,75" e 2 → "2,0". Serve para mostrar um valor salvo sem
     * cortar o que foi digitado.
     */
    public static String formatar(double valor, int minimoCasas, int maximoCasas) {
        if (!Double.isFinite(valor)) {
            return "—";
        }
        DecimalFormat formato = new DecimalFormat();
        formato.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(PT_BR));
        formato.setGroupingUsed(true);
        formato.setGroupingSize(3);
        formato.setMinimumFractionDigits(minimoCasas);
        formato.setMaximumFractionDigits(maximoCasas);
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(valor);
    }

    /**
     * Formata escolhendo as casas pela grandeza do número:
     * 1.058 · 241,0 · 37,57 · 0,809 · 0,000500.
     */
    public static String formatar(double valor) {
        if (!Double.isFinite(valor)) {
            return "—";
        }
        double absoluto = Math.abs(valor);
        if (absoluto == 0.0) {
            return "0";
        }
        if (absoluto >= 1000) {
            return formatar(valor, 0);
        }
        if (absoluto >= 100) {
            return formatar(valor, 1);
        }
        if (absoluto >= 1) {
            return formatar(valor, 2);
        }
        if (absoluto >= 0.01) {
            return formatar(valor, 3);
        }
        // Números muito pequenos: 3 algarismos significativos.
        String texto = new BigDecimal(valor).round(new MathContext(3)).toPlainString();
        return texto.replace('.', ',');
    }

    /** Tamanho de arquivo: abaixo de 1 MB, em KB inteiros (no mínimo "1 KB"); daí para cima, em MB com 1 casa. */
    public static String tamanhoArquivo(long bytes) {
        if (bytes < 1024 * 1024) {
            return formatar(Math.max(1, Math.round(bytes / 1024.0)), 0) + " KB";
        }
        return formatar(bytes / (1024.0 * 1024.0), 1) + " MB";
    }

    /** Fração como percentual. Ex.: 0,8633 com 1 casa → "86,3%". */
    public static String percentual(double fracao, int casasDecimais) {
        if (!Double.isFinite(fracao)) {
            return "—";
        }
        return formatar(fracao * 100.0, casasDecimais) + "%";
    }
}
