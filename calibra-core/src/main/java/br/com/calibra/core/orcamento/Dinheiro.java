package br.com.calibra.core.orcamento;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Valor em reais guardado em centavos inteiros: soma e multiplicação sem os erros de arredondamento do ponto
 * flutuante. Lê e escreve no formato brasileiro ("1.234,56").
 */
public record Dinheiro(long centavos) implements Comparable<Dinheiro> {

    public static final Dinheiro ZERO = new Dinheiro(0);

    /** Inteiro com pontos de milhar opcionais e até duas casas depois da vírgula. */
    private static final Pattern FORMATO = Pattern.compile("-?(\\d{1,3}(\\.\\d{3})+|\\d+)(,\\d{1,2})?");

    /**
     * Lê um valor digitado: "1.234,56", "1234,5", "R$ 1.234,56" ou "1234".
     *
     * @throws NumberFormatException com mensagem em português se o texto não for um valor em reais
     */
    public static Dinheiro ler(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new NumberFormatException("Campo vazio.");
        }
        String limpo = texto.strip().replace("R$", "").strip();
        if (!FORMATO.matcher(limpo).matches()) {
            throw new NumberFormatException("\"" + texto.strip() + "\" não é um valor em reais: use vírgula para os "
                    + "centavos (ex.: 1.234,56).");
        }
        BigDecimal reais = new BigDecimal(limpo.replace(".", "").replace(',', '.'));
        return new Dinheiro(reais.movePointRight(2).longValueExact());
    }

    /** "R$ 1.234,56". */
    public String formatar() {
        return "R$ " + formatarSemSimbolo();
    }

    /** "1.234,56", para os campos de texto. */
    public String formatarSemSimbolo() {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));
        return formato.format(BigDecimal.valueOf(centavos, 2));
    }

    public Dinheiro mais(Dinheiro outro) {
        return new Dinheiro(Math.addExact(centavos, outro.centavos));
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return Long.compare(centavos, outro.centavos);
    }
}
