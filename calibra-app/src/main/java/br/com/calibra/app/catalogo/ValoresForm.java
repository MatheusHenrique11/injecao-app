package br.com.calibra.app.catalogo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * O que está digitado num formulário de catálogo, como texto: um valor por campo (escolha = nome da constante;
 * marcar = "true" ou "false") e, nas tabelas, uma lista de linhas.
 */
public final class ValoresForm {

    private final Map<String, String> textos = new HashMap<>();
    private final Map<String, List<List<String>>> tabelas = new HashMap<>();

    /** Texto do campo; vazio se não houver. */
    public String texto(String chave) {
        String valor = textos.get(chave);
        return valor == null ? "" : valor;
    }

    public ValoresForm texto(String chave, String valor) {
        textos.put(chave, valor == null ? "" : valor);
        return this;
    }

    /** Linhas da tabela; vazia se não houver. */
    public List<List<String>> tabela(String chave) {
        return tabelas.getOrDefault(chave, List.of());
    }

    public ValoresForm tabela(String chave, List<List<String>> linhas) {
        List<List<String>> copia = new ArrayList<>();
        for (List<String> linha : linhas) {
            copia.add(List.copyOf(linha));
        }
        tabelas.put(chave, copia);
        return this;
    }
}
