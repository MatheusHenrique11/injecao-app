package br.com.calibra.app;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import br.com.calibra.core.mapa.OrientacaoTabela;
import br.com.calibra.core.mapa.Tabela;
import br.com.calibra.core.util.NumeroBr;

/**
 * Uma tabela do mapa base na tela (fase 4, bloco 4.4): a grade com os eixos, na orientação da tela da injeção, e cada
 * célula com a cor da faixa do valor. Célula vazia aparece com "—", em vermelho-claro. Clicar numa célula avisa quem
 * pediu, com a rotação e a carga dela.
 */
final class GradeMapa {

    /** A orientação quando a injeção não diz a dela: rotação nas colunas e a carga maior em cima, como nas injeções. */
    static final OrientacaoTabela PADRAO = new OrientacaoTabela(OrientacaoTabela.EixoDasColunas.ROTACAO,
            OrientacaoTabela.OrdemDasLinhas.CRESCE_PARA_CIMA);

    private static final String VAZIA = "-fx-background-color: #f6d5d5; -fx-text-fill: #8a1c1c;";

    private GradeMapa() {
    }

    /**
     * @param texto como escrever o valor da célula (ex.: "13,81")
     * @param cor a cor de fundo do valor; null usa a faixa do menor para o maior valor da tabela
     * @param aoClicar opcional: recebe a rotação e a carga da célula clicada
     */
    static ScrollPane montar(String id, Tabela tabela, OrientacaoTabela orientacao, DoubleFunction<String> texto,
            Function<Double, Color> cor, BiConsumer<Double, Double> aoClicar) {
        OrientacaoTabela o = orientacao == null ? PADRAO : orientacao;
        Function<Double, Color> corDoValor = cor != null ? cor : faixa(tabela);
        GridPane grade = new GridPane();
        grade.setId(id);
        grade.setHgap(1);
        grade.setVgap(1);
        boolean rotacaoNasColunas = o.colunas() == OrientacaoTabela.EixoDasColunas.ROTACAO;
        Label canto = cabecalho(rotacaoNasColunas ? "kPa \\ rpm" : "rpm \\ kPa");
        grade.add(canto, 0, 0);
        List<Double> colunas = o.eixoDasColunas(tabela);
        List<Double> linhas = o.eixoDasLinhas(tabela);
        for (int c = 0; c < colunas.size(); c++) {
            grade.add(cabecalho(NumeroBr.formatar(colunas.get(c), 0)), c + 1, 0);
        }
        List<List<Double>> naTela = o.naTela(tabela);
        for (int l = 0; l < linhas.size(); l++) {
            grade.add(cabecalho(NumeroBr.formatar(linhas.get(l), 0)), 0, l + 1);
            for (int c = 0; c < colunas.size(); c++) {
                Double valor = naTela.get(l).get(c);
                double rotacao = rotacaoNasColunas ? colunas.get(c) : linhas.get(l);
                double carga = rotacaoNasColunas ? linhas.get(l) : colunas.get(c);
                Label celula = new Label(valor == null ? "—" : texto.apply(valor));
                celula.setMinWidth(52);
                celula.setPrefWidth(52);
                celula.setAlignment(Pos.CENTER_RIGHT);
                celula.setStyle("-fx-padding: 2 4 2 4; " + (valor == null ? VAZIA : fundo(corDoValor.apply(valor))));
                Tooltip.install(celula, new Tooltip(NumeroBr.formatar(rotacao, 0) + " rpm · "
                        + NumeroBr.formatar(carga, 0) + " kPa"));
                if (aoClicar != null) {
                    celula.setOnMouseClicked(evento -> aoClicar.accept(rotacao, carga));
                }
                grade.add(celula, c + 1, l + 1);
            }
        }
        ScrollPane rolagem = new ScrollPane(grade);
        rolagem.setPrefViewportHeight(420);
        return rolagem;
    }

    /** Do menor (azul) para o maior (vermelho) valor da tabela, passando pelo verde e o amarelo. */
    static Function<Double, Color> faixa(Tabela tabela) {
        double menor = tabela.valores().stream().flatMap(List::stream).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).min().orElse(0);
        double maior = tabela.valores().stream().flatMap(List::stream).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).max().orElse(1);
        return valor -> {
            double fracao = maior > menor ? (valor - menor) / (maior - menor) : 0.5;
            return Color.hsb(240 * (1 - Math.clamp(fracao, 0, 1)), 0.45, 1.0);
        };
    }

    private static Label cabecalho(String texto) {
        Label rotulo = new Label(texto);
        rotulo.setMinWidth(52);
        rotulo.setAlignment(Pos.CENTER);
        rotulo.setStyle("-fx-padding: 2 4 2 4; -fx-font-weight: bold; -fx-background-color: #e4e4e4;");
        return rotulo;
    }

    private static String fundo(Color cor) {
        return String.format(java.util.Locale.ROOT, "-fx-background-color: rgb(%d, %d, %d);",
                (int) Math.round(cor.getRed() * 255), (int) Math.round(cor.getGreen() * 255),
                (int) Math.round(cor.getBlue() * 255));
    }
}
