package br.com.calibra.core.cadastro;

import java.util.regex.Pattern;

/** Conferências simples de contato: e-mail com formato de e-mail e telefone com números suficientes. */
public final class Contato {

    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    /** Menor telefone aceito: 8 números (fixo sem DDD). */
    public static final int MINIMO_DIGITOS_TELEFONE = 8;

    private Contato() {
    }

    public static boolean emailValido(String email) {
        return email != null && EMAIL.matcher(email.strip()).matches();
    }

    /** Quantos números o texto tem: "(11) 98765-4321" tem 11. */
    public static int digitos(String texto) {
        return texto == null ? 0 : (int) texto.chars().filter(Character::isDigit).count();
    }

    static String limpo(String texto) {
        return texto == null ? "" : texto.strip();
    }
}
