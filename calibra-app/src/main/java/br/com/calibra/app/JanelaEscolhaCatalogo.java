package br.com.calibra.app;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.catalogo.FormasCatalogo;
import br.com.calibra.app.catalogo.FormularioCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Janela para procurar e escolher um item ativo do catálogo (ex.: o motor de fábrica para preencher o formulário,
 * a injeção do projeto). Mostra as mesmas colunas da tela Catálogos.
 */
final class JanelaEscolhaCatalogo<T> {

    private final CasosCatalogo casos;
    private final TipoCatalogo<T> tipo;
    private final Predicate<ItemCatalogo<T>> filtro;
    private final TextField busca = new TextField();
    private final TableView<ItemCatalogo<T>> tabela = new TableView<>();
    private final Dialog<ButtonType> dialogo = new Dialog<>();
    private final ButtonType usar = new ButtonType("Usar este", ButtonBar.ButtonData.OK_DONE);

    /**
     * @param filtro quais itens ativos aparecem (ex.: só sensores MAP)
     * @param extra coluna a mais no começo (ex.: "Atende?" na injeção); null se não houver
     */
    JanelaEscolhaCatalogo(Window dono, CasosCatalogo casos, TipoCatalogo<T> tipo, String titulo, String explicacao,
            Predicate<ItemCatalogo<T>> filtro, TableColumn<ItemCatalogo<T>, String> extra) {
        this.casos = casos;
        this.tipo = tipo;
        this.filtro = filtro;
        FormularioCatalogo<T> formulario = FormasCatalogo.de(tipo);
        busca.setPromptText("Buscar por nome, fonte ou observações");
        busca.textProperty().addListener((obs, antes, depois) -> atualizar());
        tabela.setId("escolhaCatalogo");
        if (extra != null) {
            tabela.getColumns().add(extra);
        }
        tabela.getColumns().add(coluna("Nome", 320, item -> item.info().nome()));
        List<String> colunas = formulario.colunas();
        for (int i = 0; i < colunas.size(); i++) {
            int indice = i;
            tabela.getColumns().add(coluna(colunas.get(i), 140, item -> formulario.resumo(item.dados()).get(indice)));
        }
        tabela.getColumns().add(coluna("Conferência", 110, item -> item.info().aVerificar() ? "a verificar" : ""));
        tabela.setPrefSize(1000, 420);
        Label vazio = new Label("Nada cadastrado em " + tipo.plural() + ". Cadastre na tela Catálogos.");
        vazio.getStyleClass().add("dica");
        tabela.setPlaceholder(vazio);
        tabela.setRowFactory(lista -> {
            TableRow<ItemCatalogo<T>> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2 && !linha.isEmpty()) {
                    dialogo.setResult(usar);
                    dialogo.close();
                }
            });
            return linha;
        });
        Label dica = new Label(explicacao);
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);

        if (dono != null) {
            dialogo.initOwner(dono);
        }
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.getDialogPane().setContent(new VBox(8, busca, tabela, dica));
        dialogo.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        dialogo.getDialogPane().getButtonTypes().setAll(usar, new ButtonType("Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().lookupButton(usar).disableProperty()
                .bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        dialogo.setResizable(true);
        atualizar();
    }

    /** Mostra e espera; devolve o item escolhido (vazio se cancelou). */
    Optional<ItemCatalogo<T>> escolher() {
        Platform.runLater(busca::requestFocus);
        return dialogo.showAndWait().filter(usar::equals).map(botao -> tabela.getSelectionModel().getSelectedItem());
    }

    TableView<ItemCatalogo<T>> tabela() {
        return tabela;
    }

    Dialog<ButtonType> dialogo() {
        return dialogo;
    }

    private void atualizar() {
        tabela.getItems().setAll(casos.listar(tipo, busca.getText(), false).stream().filter(filtro).toList());
    }

    static <T> TableColumn<ItemCatalogo<T>, String> coluna(String titulo, double largura,
            Function<ItemCatalogo<T>, String> valor) {
        TableColumn<ItemCatalogo<T>, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(celula -> new ReadOnlyStringWrapper(valor.apply(celula.getValue())));
        coluna.setPrefWidth(largura);
        return coluna;
    }
}
