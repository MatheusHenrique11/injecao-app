package br.com.calibra.app.catalogo;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import br.com.calibra.core.util.NumeroBr;

/** Números e datas escritos nos campos do formulário e nas colunas da lista, no formato brasileiro. */
final class TextoForm {

    /** Casas decimais guardadas ao escrever: o bastante para não cortar o que foi digitado. */
    private static final int CASAS = 6;

    private TextoForm() {
    }

    /** 1.000 · 3 · 0,7 · 13,5 (sem zeros sobrando). */
    static String numero(double valor) {
        return Double.isFinite(valor) ? NumeroBr.formatar(valor, 0, CASAS) : "";
    }

    /** Vazio quando não informado. */
    static String numero(Double valor) {
        return valor == null ? "" : numero(valor.doubleValue());
    }

    static String inteiro(Integer valor) {
        return valor == null ? "" : String.valueOf(valor);
    }

    static String data(LocalDate data) {
        return data == null ? "" : LeituraForm.DIA.format(data);
    }

    /** Coluna da lista: "—" quando não informado. */
    static String coluna(Double valor, String unidade) {
        return valor == null || !Double.isFinite(valor) ? "—" : numero(valor) + (unidade.isEmpty() ? "" : " " + unidade);
    }

    /** "3 a 6" (ou só "3" quando os dois são iguais). */
    static String faixa(List<Double> valores, String unidade) {
        if (valores.isEmpty()) {
            return "—";
        }
        double menor = valores.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        double maior = valores.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        String texto = menor == maior ? numero(menor) : numero(menor) + " a " + numero(maior);
        return texto + " " + unidade;
    }

    /** "12 e 13,5", sem repetir. */
    static String lista(List<Double> valores, String unidade) {
        List<String> distintos = valores.stream().sorted().distinct().map(TextoForm::numero).toList();
        if (distintos.isEmpty()) {
            return "—";
        }
        String texto = distintos.size() == 1 ? distintos.getFirst()
                : distintos.subList(0, distintos.size() - 1).stream().collect(Collectors.joining("; "))
                        + " e " + distintos.getLast();
        return texto + " " + unidade;
    }
}
