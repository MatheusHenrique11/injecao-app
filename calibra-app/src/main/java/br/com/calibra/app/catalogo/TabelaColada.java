package br.com.calibra.app.catalogo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tabela colada da área de transferência (Excel, PDF do fabricante): uma linha por ponto, colunas separadas por
 * tabulação ou ponto e vírgula. A vírgula fica de fora porque é o separador decimal.
 */
public final class TabelaColada {

    private TabelaColada() {
    }

    /**
     * @param colunas quantas colunas a tabela da tela tem: o que passar é ignorado e o que faltar fica vazio
     * @return as linhas com texto, sem as vazias
     */
    public static List<List<String>> ler(String texto, int colunas) {
        List<List<String>> linhas = new ArrayList<>();
        if (texto == null) {
            return linhas;
        }
        for (String linha : texto.split("\\R")) {
            if (linha.isBlank()) {
                continue;
            }
            List<String> celulas = new ArrayList<>(Arrays.stream(linha.split("[\\t;]", -1)).map(String::strip).toList());
            while (celulas.size() < colunas) {
                celulas.add("");
            }
            linhas.add(List.copyOf(celulas.subList(0, colunas)));
        }
        return linhas;
    }
}
