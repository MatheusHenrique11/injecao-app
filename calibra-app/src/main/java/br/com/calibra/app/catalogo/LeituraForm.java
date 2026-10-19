package br.com.calibra.app.catalogo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.util.NumeroBr;

/**
 * Lê os valores de um formulário de catálogo. Erro de digitação vira mensagem em português na lista de erros
 * (o campo volta vazio); as regras de cada peça ficam no record do núcleo ou na forma do tipo.
 */
public final class LeituraForm {

    /** Estrito: 31/02 é recusado em vez de virar 28/02. */
    static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /**
     * Uma linha preenchida de uma tabela.
     *
     * @param numero posição na tela, a partir de 1 (para as mensagens)
     * @param celulas null nas células vazias
     */
    public record LinhaTabela(int numero, Double[] celulas) {

        public Double celula(int coluna) {
            return coluna < celulas.length ? celulas[coluna] : null;
        }

        public boolean completa(int colunas) {
            for (int coluna = 0; coluna < colunas; coluna++) {
                if (celula(coluna) == null) {
                    return false;
                }
            }
            return true;
        }
    }

    private final ValoresForm valores;
    private final Map<String, String> rotulos = new HashMap<>();
    private final List<String> erros;

    LeituraForm(ValoresForm valores, List<CampoForm> campos, List<String> erros) {
        this.valores = valores;
        this.erros = erros;
        campos.forEach(campo -> rotulos.put(campo.chave(), campo.rotulo()));
    }

    /** Texto sem espaços nas pontas. */
    public String texto(String chave) {
        return valores.texto(chave).strip();
    }

    public String textoObrigatorio(String chave) {
        String texto = texto(chave);
        if (texto.isEmpty()) {
            erros.add("Preencha \"" + rotulo(chave) + "\".");
        }
        return texto;
    }

    /** Número com vírgula decimal; vazio = null (com erro, se obrigatório). */
    public Double numero(String chave, boolean obrigatorio) {
        String texto = texto(chave);
        if (texto.isEmpty()) {
            if (obrigatorio) {
                erros.add("Preencha \"" + rotulo(chave) + "\".");
            }
            return null;
        }
        try {
            return NumeroBr.ler(texto);
        } catch (NumberFormatException erro) {
            erros.add("\"" + rotulo(chave) + "\": " + erro.getMessage());
            return null;
        }
    }

    public Integer inteiro(String chave, boolean obrigatorio) {
        Double valor = numero(chave, obrigatorio);
        if (valor == null) {
            return null;
        }
        if (valor != Math.rint(valor)) {
            erros.add("\"" + rotulo(chave) + "\": use um número inteiro.");
            return null;
        }
        return valor.intValue();
    }

    /** Valor em reais ("1.234,56"); vazio = null. */
    public Dinheiro dinheiro(String chave) {
        String texto = texto(chave);
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return Dinheiro.ler(texto);
        } catch (NumberFormatException erro) {
            erros.add("\"" + rotulo(chave) + "\": " + erro.getMessage());
            return null;
        }
    }

    /** Constante escolhida (pelo nome); vazio = null (com erro, se obrigatório). */
    public <E extends Enum<E>> E escolha(String chave, Class<E> tipo, boolean obrigatorio) {
        String texto = texto(chave);
        if (texto.isEmpty()) {
            if (obrigatorio) {
                erros.add("Escolha \"" + rotulo(chave) + "\".");
            }
            return null;
        }
        try {
            return Enum.valueOf(tipo, texto);
        } catch (IllegalArgumentException erro) {
            erros.add("\"" + rotulo(chave) + "\": opção desconhecida.");
            return null;
        }
    }

    public boolean marcado(String chave) {
        return Boolean.parseBoolean(texto(chave));
    }

    /** Data dd/mm/aaaa; vazio = null. */
    public LocalDate data(String chave) {
        String texto = texto(chave);
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(texto, DIA);
        } catch (DateTimeParseException erro) {
            erros.add("\"" + rotulo(chave) + "\": use a data no formato dd/mm/aaaa.");
            return null;
        }
    }

    /** Linhas preenchidas de uma tabela de números; as totalmente vazias ficam de fora. */
    public List<LinhaTabela> tabela(String chave) {
        List<LinhaTabela> linhas = new ArrayList<>();
        int numero = 0;
        for (List<String> linha : valores.tabela(chave)) {
            numero++;
            if (linha.stream().allMatch(celula -> celula == null || celula.isBlank())) {
                continue;
            }
            Double[] lida = new Double[linha.size()];
            for (int coluna = 0; coluna < linha.size(); coluna++) {
                String celula = linha.get(coluna) == null ? "" : linha.get(coluna).strip();
                if (celula.isEmpty()) {
                    continue;
                }
                try {
                    lida[coluna] = NumeroBr.ler(celula);
                } catch (NumberFormatException erro) {
                    erros.add("\"" + rotulo(chave) + "\", linha " + numero + ": " + erro.getMessage());
                }
            }
            linhas.add(new LinhaTabela(numero, lida));
        }
        return linhas;
    }

    /**
     * As linhas de um campo de linhas do catálogo (as linhas do kit), como texto e sem espaços nas pontas; as linhas
     * totalmente vazias ficam de fora.
     */
    public List<List<String>> linhasDeTexto(String chave) {
        return valores.tabela(chave).stream()
                .filter(linha -> linha.stream().anyMatch(celula -> celula != null && !celula.isBlank()))
                .map(linha -> linha.stream().map(celula -> celula == null ? "" : celula.strip()).toList())
                .toList();
    }

    /** Anota um erro de regra (ex.: "Tempo morto, linha 2: preencha a tensão"). */
    public void erro(String mensagem) {
        erros.add(mensagem);
    }

    public boolean temErros() {
        return !erros.isEmpty();
    }

    String rotulo(String chave) {
        return rotulos.getOrDefault(chave, chave);
    }
}
