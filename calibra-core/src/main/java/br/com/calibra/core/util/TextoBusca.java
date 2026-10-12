package br.com.calibra.core.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Texto para busca: sem acento, em minúsculas e sem pontuação, para "joao" achar "João" e "abc1234" achar
 * "ABC-1234". O banco guarda o texto já assim numa coluna própria (SPEC 5: campos de busca em colunas
 * próprias).
 */
public final class TextoBusca {

    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");
    private static final Pattern PONTUACAO = Pattern.compile("[^a-z0-9 ]");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private TextoBusca() {
    }

    /** "Ação Turbo AP-2.0" → "acao turbo ap20". */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = ACENTOS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        String minusculo = ESPACOS.matcher(semAcento.toLowerCase(Locale.ROOT)).replaceAll(" ");
        String semPontuacao = PONTUACAO.matcher(minusculo).replaceAll("");
        return ESPACOS.matcher(semPontuacao).replaceAll(" ").strip();
    }

    /** Junta vários textos (nome, placa, modelo...) numa linha só de busca. */
    public static String juntar(String... textos) {
        return normalizar(String.join(" ", Arrays.stream(textos).map(t -> t == null ? "" : t).toList()));
    }

    /** As palavras do que foi digitado na busca, já normalizadas; vazio se não sobrou nada. */
    public static List<String> palavras(String termo) {
        String normalizado = normalizar(termo);
        return normalizado.isEmpty() ? List.of() : List.of(normalizado.split(" "));
    }
}
