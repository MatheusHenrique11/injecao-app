package br.com.calibra.relatorios;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.font.PDFont;

/**
 * As fontes padrão do PDF (Helvetica) não têm todos os caracteres. Este utilitário troca os que
 * faltam por texto equivalente (λ → "lambda", ≤ → "<=") e o resto por "?".
 */
final class TextoPdf {

    private static final Map<Integer, String> TROCAS = Map.ofEntries(
            Map.entry((int) 'λ', "lambda"),
            Map.entry((int) '≤', "<="),
            Map.entry((int) '≥', ">="),
            Map.entry((int) 'Δ', "d"),
            Map.entry((int) '↔', "<->"),
            Map.entry((int) '→', "->"),
            Map.entry((int) '−', "-"),
            Map.entry((int) 'π', "pi"),
            Map.entry((int) '√', "raiz"),
            Map.entry((int) 'η', "eta"),
            Map.entry((int) 'ε', "eps"),
            Map.entry((int) '≈', "~"));

    private final PDFont fonte;
    private final Map<Integer, Boolean> suportados = new HashMap<>();

    TextoPdf(PDFont fonte) {
        this.fonte = fonte;
    }

    String limpar(String texto) {
        StringBuilder limpo = new StringBuilder(texto.length());
        texto.codePoints().forEach(codigo -> {
            String troca = TROCAS.get(codigo);
            if (troca != null) {
                limpo.append(troca);
            } else if (codigo == '\t') {
                limpo.append(' ');
            } else if (suporta(codigo)) {
                limpo.appendCodePoint(codigo);
            } else {
                limpo.append('?');
            }
        });
        return limpo.toString();
    }

    private boolean suporta(int codigo) {
        return suportados.computeIfAbsent(codigo, c -> {
            try {
                fonte.encode(new String(Character.toChars(c)));
                return true;
            } catch (IllegalArgumentException | IOException erro) {
                return false;
            }
        });
    }
}
