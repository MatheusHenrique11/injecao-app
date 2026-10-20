package br.com.calibra.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Um editor de linhas com colunas de tipos diferentes: texto, escolha e marcar. Cada item é uma linha de campos, com o
 * ✕ para tirar, e o botão de acrescentar fica embaixo. Os campos ficam sempre editáveis, como no {@link EditorTabela}.
 * Serve às medições e aos códigos de falha do laudo (Fase 3B).
 *
 * <p>Os valores vão e voltam como texto: o que foi digitado, o nome da constante escolhida (vazio sem escolha) e
 * "true" ou "false" na marca.
 */
final class EditorLinhas extends VBox {

    enum Tipo {
        TEXTO, ESCOLHA, MARCAR
    }

    /** Uma opção da escolha: o valor gravado (o nome da constante) e o rótulo da tela. */
    record Opcao(String valor, String rotulo) {
    }

    /**
     * Uma coluna.
     *
     * @param largura a largura do campo, em pontos
     * @param dica o texto de exemplo no campo vazio; vazio se não houver
     * @param repetir a linha nova começa com o valor da linha de cima (ex.: a condição da medição)
     */
    record Coluna(String titulo, Tipo tipo, double largura, String dica, List<Opcao> opcoes, boolean repetir) {

        Coluna {
            Objects.requireNonNull(titulo, "titulo");
            dica = dica == null ? "" : dica;
            opcoes = List.copyOf(opcoes);
        }

        static Coluna texto(String titulo, double largura, String dica) {
            return new Coluna(titulo, Tipo.TEXTO, largura, dica, List.of(), false);
        }

        /** Escolha entre as constantes do enum, pelo rótulo; a linha nova repete a escolha da linha de cima. */
        static <E extends Enum<E>> Coluna escolha(String titulo, double largura, E[] valores,
                Function<E, String> rotulo) {
            List<Opcao> opcoes = Arrays.stream(valores).map(v -> new Opcao(v.name(), rotulo.apply(v))).toList();
            return new Coluna(titulo, Tipo.ESCOLHA, largura, "Escolha…", opcoes, true);
        }

        static Coluna marcar(String titulo) {
            return new Coluna(titulo, Tipo.MARCAR, Region.USE_COMPUTED_SIZE, "", List.of(), false);
        }
    }

    private final List<Coluna> colunas;
    private final VBox caixas = new VBox(4);
    private final List<List<Node>> linhas = new ArrayList<>();
    private final List<Button> remover = new ArrayList<>();
    private final HBox cabecalho = new HBox(6);
    private final Label vazio;
    private final Button acrescentar;

    /**
     * @param textoAcrescentar o botão de acrescentar (ex.: "+ Medição")
     * @param textoVazio o que aparece sem nenhuma linha
     */
    EditorLinhas(String textoAcrescentar, String textoVazio, List<Coluna> colunas) {
        super(6);
        this.colunas = List.copyOf(colunas);
        for (Coluna coluna : this.colunas) {
            Label titulo = new Label(coluna.tipo() == Tipo.MARCAR ? "" : coluna.titulo());
            titulo.getStyleClass().add("negrito");
            titulo.setPrefWidth(coluna.largura());
            titulo.setMinWidth(Region.USE_PREF_SIZE);
            cabecalho.getChildren().add(titulo);
        }
        vazio = new Label(textoVazio);
        vazio.getStyleClass().add("dica");
        acrescentar = new Button(textoAcrescentar);
        acrescentar.setOnAction(evento -> adicionar().requestFocus());
        getChildren().addAll(cabecalho, caixas, acrescentar);
        preencher(List.of());
    }

    /** As linhas como estão na tela (inclusive as vazias: quem lê decide o que fica de fora). */
    List<List<String>> linhas() {
        List<List<String>> valores = new ArrayList<>();
        for (List<Node> linha : linhas) {
            List<String> celulas = new ArrayList<>();
            for (int i = 0; i < colunas.size(); i++) {
                celulas.add(valor(colunas.get(i), linha.get(i)));
            }
            valores.add(celulas);
        }
        return valores;
    }

    /** Troca as linhas pelas dos valores. */
    void preencher(List<List<String>> valores) {
        linhas.clear();
        remover.clear();
        caixas.getChildren().clear();
        for (List<String> valor : valores) {
            acrescentar(valor);
        }
        mostrarVazio();
    }

    /** Acrescenta uma linha no fim (o que o botão faz); devolve o primeiro campo, para o foco. */
    Node adicionar() {
        List<String> inicio = new ArrayList<>();
        List<String> deCima = linhas.isEmpty() ? null : linhas().getLast();
        for (int i = 0; i < colunas.size(); i++) {
            inicio.add(colunas.get(i).repetir() && deCima != null ? deCima.get(i) : "");
        }
        List<Node> linha = acrescentar(inicio);
        mostrarVazio();
        return linha.getFirst();
    }

    /** Só para ver (OS entregue ou cancelada): os campos, o ✕ e o botão de acrescentar ficam desligados. */
    void somenteLeitura(boolean leitura) {
        linhas.forEach(linha -> linha.forEach(campo -> campo.setDisable(leitura)));
        remover.forEach(botao -> botao.setDisable(leitura));
        acrescentar.setDisable(leitura);
    }

    /** Quantas linhas há na tela, para os testes. */
    int quantas() {
        return linhas.size();
    }

    /** O campo da linha e da coluna (a partir de 0): TextField, ComboBox ou CheckBox, para os testes. */
    Node campo(int linha, int coluna) {
        return linhas.get(linha).get(coluna);
    }

    private List<Node> acrescentar(List<String> valores) {
        List<Node> campos = new ArrayList<>();
        HBox caixa = new HBox(6);
        caixa.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < colunas.size(); i++) {
            Node campo = campo(colunas.get(i), i < valores.size() ? valores.get(i) : "");
            campos.add(campo);
            caixa.getChildren().add(campo);
        }
        Button tirar = new Button("✕");
        tirar.setTooltip(new Tooltip("Tirar esta linha"));
        tirar.setAccessibleText("Tirar esta linha");
        tirar.setMinWidth(Region.USE_PREF_SIZE);
        tirar.setOnAction(evento -> {
            int indice = linhas.indexOf(campos);
            linhas.remove(indice);
            remover.remove(indice);
            caixas.getChildren().remove(caixa);
            mostrarVazio();
        });
        caixa.getChildren().add(tirar);
        linhas.add(campos);
        remover.add(tirar);
        caixas.getChildren().add(caixa);
        return campos;
    }

    private static Node campo(Coluna coluna, String valor) {
        String texto = valor == null ? "" : valor;
        return switch (coluna.tipo()) {
            case TEXTO -> {
                TextField campo = new TextField(texto);
                campo.setPrefWidth(coluna.largura());
                campo.setMinWidth(Region.USE_PREF_SIZE);
                campo.setPromptText(coluna.dica());
                campo.setAccessibleText(coluna.titulo());
                yield campo;
            }
            case ESCOLHA -> escolha(coluna, texto);
            case MARCAR -> {
                CheckBox marcar = new CheckBox(coluna.titulo());
                marcar.setSelected(Boolean.parseBoolean(texto));
                marcar.setMinWidth(Region.USE_PREF_SIZE);
                yield marcar;
            }
        };
    }

    private static ComboBox<Opcao> escolha(Coluna coluna, String valor) {
        ComboBox<Opcao> escolha = new ComboBox<>();
        escolha.getItems().setAll(coluna.opcoes());
        StringConverter<Opcao> conversor = new StringConverter<>() {
            @Override
            public String toString(Opcao opcao) {
                return opcao == null ? "" : opcao.rotulo();
            }

            @Override
            public Opcao fromString(String texto) {
                return coluna.opcoes().stream().filter(o -> o.rotulo().equals(texto)).findFirst().orElse(null);
            }
        };
        escolha.setConverter(conversor);
        escolha.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Opcao opcao, boolean vazia) {
                super.updateItem(opcao, vazia);
                setText(vazia ? null : conversor.toString(opcao));
            }
        });
        escolha.setButtonCell(escolha.getCellFactory().call(null));
        escolha.setPromptText(coluna.dica());
        escolha.setPrefWidth(coluna.largura());
        escolha.setMinWidth(Region.USE_PREF_SIZE);
        escolha.setAccessibleText(coluna.titulo());
        escolha.setValue(coluna.opcoes().stream().filter(o -> o.valor().equals(valor)).findFirst().orElse(null));
        return escolha;
    }

    @SuppressWarnings("unchecked")
    private static String valor(Coluna coluna, Node campo) {
        return switch (coluna.tipo()) {
            case TEXTO -> ((TextField) campo).getText();
            // O campo da escolha é sempre o ComboBox<Opcao> montado em escolha().
            case ESCOLHA -> {
                Opcao escolhida = ((ComboBox<Opcao>) campo).getValue();
                yield escolhida == null ? "" : escolhida.valor();
            }
            case MARCAR -> Boolean.toString(((CheckBox) campo).isSelected());
        };
    }

    private void mostrarVazio() {
        boolean semLinhas = linhas.isEmpty();
        cabecalho.setVisible(!semLinhas);
        cabecalho.setManaged(!semLinhas);
        if (semLinhas) {
            caixas.getChildren().setAll(vazio);
        } else {
            caixas.getChildren().remove(vazio);
        }
    }
}
