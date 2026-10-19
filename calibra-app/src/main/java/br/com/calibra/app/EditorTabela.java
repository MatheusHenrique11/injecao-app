package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import br.com.calibra.app.catalogo.TabelaColada;

/**
 * Editor de tabela de números (tempo morto do bico, curva da bomba): uma linha de campos por ponto, com botões
 * para acrescentar, remover e colar a tabela copiada do Excel ou do PDF. Os campos ficam sempre editáveis (numa
 * célula de TableView, o que foi digitado se perde ao clicar fora).
 */
final class EditorTabela extends VBox implements EditorDeLinhas {

    private static final double LARGURA_COLUNA = 120;

    private final List<String> colunas;
    private final VBox linhas = new VBox(4);
    private final String nome;

    EditorTabela(String nome, List<String> colunas) {
        super(6);
        this.nome = nome;
        this.colunas = List.copyOf(colunas);
        HBox cabecalho = new HBox(6);
        for (String coluna : colunas) {
            Label titulo = new Label(coluna);
            titulo.getStyleClass().add("negrito");
            titulo.setPrefWidth(LARGURA_COLUNA);
            cabecalho.getChildren().add(titulo);
        }
        Button acrescentar = new Button("+ Linha");
        acrescentar.setOnAction(evento -> acrescentar(List.of()).getFirst().requestFocus());
        Button colar = new Button("Colar tabela");
        colar.setTooltip(new Tooltip("Troca as linhas pelo texto copiado: colunas separadas por tabulação "
                + "(Excel) ou ponto e vírgula, na ordem " + String.join(", ", colunas) + "."));
        colar.setOnAction(evento -> colar());
        getChildren().addAll(cabecalho, linhas, new HBox(8, acrescentar, colar));
        preencher(List.of());
    }

    /** As linhas como estão digitadas (inclusive as vazias, que a leitura deixa de lado). */
    @Override
    public List<List<String>> linhas() {
        List<List<String>> valores = new ArrayList<>();
        for (Node no : linhas.getChildren()) {
            HBox linha = (HBox) no;
            List<String> celulas = new ArrayList<>();
            for (int coluna = 0; coluna < colunas.size(); coluna++) {
                celulas.add(((TextField) linha.getChildren().get(coluna)).getText());
            }
            valores.add(celulas);
        }
        return valores;
    }

    /** Troca as linhas; sem nenhuma, deixa uma vazia para digitar. */
    @Override
    public void preencher(List<List<String>> valores) {
        linhas.getChildren().clear();
        for (List<String> linha : valores) {
            acrescentar(linha);
        }
        if (valores.isEmpty()) {
            acrescentar(List.of());
        }
    }

    void colar() {
        String texto = Clipboard.getSystemClipboard().getString();
        List<List<String>> coladas = TabelaColada.ler(texto, colunas.size());
        if (!coladas.isEmpty()) {
            preencher(coladas);
        }
    }

    /** Acrescenta uma linha no fim e devolve os campos dela. */
    private List<TextField> acrescentar(List<String> valores) {
        HBox linha = new HBox(6);
        linha.setAlignment(Pos.CENTER_LEFT);
        List<TextField> campos = new ArrayList<>();
        int numero = linhas.getChildren().size() + 1;
        for (int coluna = 0; coluna < colunas.size(); coluna++) {
            TextField campo = new TextField(coluna < valores.size() ? valores.get(coluna) : "");
            campo.setPrefWidth(LARGURA_COLUNA);
            campo.setAccessibleText(nome + ", " + colunas.get(coluna) + ", linha " + numero);
            campos.add(campo);
        }
        Button remover = new Button("✕");
        remover.setTooltip(new Tooltip("Remover esta linha"));
        remover.setAccessibleText("Remover linha");
        remover.setOnAction(evento -> {
            linhas.getChildren().remove(linha);
            if (linhas.getChildren().isEmpty()) {
                acrescentar(List.of());
            }
        });
        linha.getChildren().addAll(campos);
        linha.getChildren().add(remover);
        linhas.getChildren().add(linha);
        return campos;
    }
}
