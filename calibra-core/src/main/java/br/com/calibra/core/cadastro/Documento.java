package br.com.calibra.core.cadastro;

import java.util.Locale;

/**
 * CPF e CNPJ: confere só os dígitos verificadores, sem consultar nada (regra 2.1, zero rede).
 * O CNPJ aceita o formato alfanumérico da Receita Federal (letras nas 12 primeiras posições, a partir
 * de julho de 2026); cada caractere vale o código ASCII menos 48, então os números continuam iguais.
 */
public final class Documento {

    private static final int[] PESOS_CNPJ_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Documento() {
    }

    /** Tira pontos, traços, barras e espaços; letras ficam maiúsculas. */
    public static String normalizar(String texto) {
        return texto == null ? "" : texto.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
    }

    /** CPF: 11 dígitos com os dois verificadores certos. Rejeita os repetidos (111.111.111-11). */
    public static boolean cpfValido(String texto) {
        String cpf = normalizar(texto);
        if (!cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return digitoCpf(cpf, 9) == cpf.charAt(9) - '0' && digitoCpf(cpf, 10) == cpf.charAt(10) - '0';
    }

    /** CNPJ numérico ou alfanumérico: 12 caracteres (números ou letras) e 2 dígitos verificadores. */
    public static boolean cnpjValido(String texto) {
        String cnpj = normalizar(texto);
        if (!cnpj.matches("[0-9A-Z]{12}\\d{2}") || cnpj.chars().distinct().count() == 1) {
            return false;
        }
        return digitoCnpj(cnpj, PESOS_CNPJ_1) == cnpj.charAt(12) - '0'
                && digitoCnpj(cnpj, PESOS_CNPJ_2) == cnpj.charAt(13) - '0';
    }

    /** CPF (11 caracteres) ou CNPJ (14 caracteres). */
    public static boolean valido(String texto) {
        int tamanho = normalizar(texto).length();
        return tamanho == 11 ? cpfValido(texto) : tamanho == 14 && cnpjValido(texto);
    }

    /** 000.000.000-00 ou 00.000.000/0000-00; se não tiver o tamanho certo, devolve o texto como veio. */
    public static String formatar(String texto) {
        String d = normalizar(texto);
        if (d.length() == 11) {
            return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
        }
        if (d.length() == 14) {
            return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/" + d.substring(8, 12)
                    + "-" + d.substring(12);
        }
        return texto == null ? "" : texto.strip();
    }

    /** Dígito verificador do CPF sobre as {@code quantos} primeiras posições (pesos decrescentes até 2). */
    private static int digitoCpf(String cpf, int quantos) {
        int soma = 0;
        for (int i = 0; i < quantos; i++) {
            soma += (cpf.charAt(i) - '0') * (quantos + 1 - i);
        }
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }

    private static int digitoCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
