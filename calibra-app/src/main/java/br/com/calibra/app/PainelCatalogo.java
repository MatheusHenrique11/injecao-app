package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.catalogo.FormularioCatalogo;
import br.com.calibra.app.catalogo.FormularioCatalogo.LinhaHistorico;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.catalogo.VersaoCatalogo;

/**
 * Uma aba da tela Catálogos: busca, lista e os botões de um tipo (cadastrar, editar, duplicar, arquivar ou
 * reativar e ver as versões). Editar grava uma versão nova; nada é apagado.
 */
final class PainelCatalogo<T> extends VBox {

    private final CasosCatalogo casos;
    private final FormularioCatalogo<T> formulario;
    private final TextField busca = new TextField();
    private final CheckBox mostrarArquivados = new CheckBox("Mostrar arquivados");
    private final TableView<ItemCatalogo<T>> tabela = new TableView<>();
    private final Label vazio = new Label();
    private final Button cadastrar = new Button("Cadastrar…");
    private final Button editar = new Button("Editar…");
    private final Button duplicar = new Button("Duplicar…");
    private final Button arquivar = new Button("Arquivar");
    private final Button versoes = new Button("Versões…");
    private final Label mensagem = new Label();
    private final HBox barra = new HBox(10);
    private final List<Button> acoesDoItem = new ArrayList<>();

    PainelCatalogo(CasosCatalogo casos, FormularioCatalogo<T> formulario) {
        super(8);
        this.casos = casos;
        this.formulario = formulario;
        String codigo = formulario.tipo().codigo();
        getStyleClass().add("pagina");
        setId("painel" + codigo);
        busca.setId("busca" + codigo);
        mostrarArquivados.setId("arquivados" + codigo);
        tabela.setId("tabela" + codigo);

        busca.setPromptText("Buscar por nome, fonte ou observações");
        HBox.setHgrow(busca, Priority.ALWAYS);
        cadastrar.setId("novo" + codigo);
        cadastrar.setOnAction(evento -> novo());
        editar.setOnAction(evento -> editar());
        duplicar.setOnAction(evento -> duplicar());
        arquivar.setOnAction(evento -> arquivarOuReativar());
        versoes.setOnAction(evento -> versoes());
        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.SOMETIMES);
        barra.getChildren().setAll(busca, mostrarArquivados, espaco, cadastrar, editar, duplicar, arquivar, versoes);
        barra.setAlignment(Pos.CENTER_LEFT);

        montarTabela();
        mensagem.getStyleClass().add("ok");
        Label dica = new Label("Duplo clique ou Enter edita o item. Cada gravação com mudança vira uma versão nova; "
                + "as anteriores ficam guardadas. Arquivar tira o item das escolhas, mas ele continua nos cálculos "
                + "antigos.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);
        VBox.setVgrow(tabela, Priority.ALWAYS);
        getChildren().addAll(barra, tabela, mensagem, dica);

        busca.textProperty().addListener((obs, antes, depois) -> atualizar());
        mostrarArquivados.selectedProperty().addListener((obs, antes, depois) -> atualizar());
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) -> atualizarBotoes());
        atualizar();
    }

    TipoCatalogo<T> tipo() {
        return formulario.tipo();
    }

    /** Lê a lista de novo (com a busca e o filtro da tela), mantendo o item escolhido. */
    void atualizar() {
        ItemCatalogo<T> escolhido = tabela.getSelectionModel().getSelectedItem();
        List<ItemCatalogo<T>> itens = casos.listar(formulario.tipo(), busca.getText(), mostrarArquivados.isSelected());
        tabela.getItems().setAll(itens);
        if (escolhido != null) {
            selecionar(escolhido.id());
        }
        vazio.setText(busca.getText().isBlank()
                ? "Nada cadastrado em " + formulario.tipo().plural() + " ainda. Clique em \"Cadastrar\" e informe os "
                        + "dados com a fonte (manual, site do fabricante, medição na bancada...)."
                : "Nenhum item com essa busca.");
        atualizarBotoes();
    }

    TableView<ItemCatalogo<T>> tabela() {
        return tabela;
    }

    /** A janela de cadastro de um item novo, sem mostrar (para os testes). */
    JanelaItemCatalogo<T> janelaNovo() {
        return new JanelaItemCatalogo<>(janela(), casos, formulario, null, formulario.novo());
    }

    /** A janela de edição do item escolhido, sem mostrar (para os testes). */
    JanelaItemCatalogo<T> janelaEditar(ItemCatalogo<T> item) {
        return new JanelaItemCatalogo<>(janela(), casos, formulario, item, formulario.escrever(item.dados(), item.info()));
    }

    /** A comparação entre as versões do item, sem mostrar (para os testes). */
    Dialog<ButtonType> janelaVersoes(ItemCatalogo<T> item) {
        List<VersaoCatalogo<T>> todas = casos.versoes(formulario.tipo(), item.id());
        TableView<LinhaHistorico> comparacao = new TableView<>();
        comparacao.setId("versoes");
        TableColumn<LinhaHistorico, String> campo = new TableColumn<>("Campo");
        campo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().campo()));
        campo.setPrefWidth(230);
        comparacao.getColumns().add(campo);
        for (int i = 0; i < todas.size(); i++) {
            int indice = i;
            VersaoCatalogo<T> versao = todas.get(i);
            TableColumn<LinhaHistorico, String> coluna = new TableColumn<>("v" + versao.versao() + " · "
                    + Datas.diaHora(versao.criadaEm()));
            coluna.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().valores().get(indice)));
            coluna.setCellFactory(c -> celulaDoHistorico(indice));
            coluna.setPrefWidth(240);
            comparacao.getColumns().add(coluna);
        }
        comparacao.getItems().setAll(formulario.historico(todas));
        comparacao.setPrefSize(Math.min(1200, 260 + 250 * todas.size()), 520);
        Label legenda = new Label("Em amarelo, o que mudou em relação à versão anterior. Os cálculos guardam a versão "
                + "que usaram.");
        legenda.getStyleClass().add("dica");
        legenda.setWrapText(true);
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (janela() != null) {
            dialogo.initOwner(janela());
        }
        dialogo.setTitle("Versões de " + item.info().nome());
        dialogo.setHeaderText("Versões de " + item.info().nome() + " (" + todas.size() + ")");
        dialogo.getDialogPane().setContent(new VBox(8, comparacao, legenda));
        dialogo.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        dialogo.getDialogPane().getButtonTypes().setAll(new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.setResizable(true);
        return dialogo;
    }

    // ===================== Botões =====================

    private void novo() {
        abrir(janelaNovo());
    }

    private void editar() {
        escolhido().ifPresent(item -> abrir(janelaEditar(item)));
    }

    private void duplicar() {
        escolhido().ifPresent(item -> abrir(new JanelaItemCatalogo<>(janela(), casos, formulario, null,
                formulario.duplicar(item.dados(), item.info()))));
    }

    private void abrir(JanelaItemCatalogo<T> janela) {
        janela.mostrar().ifPresent(gravado -> {
            atualizar();
            selecionar(gravado.id());
            mensagem.setText(janela.mensagem());
        });
    }

    private void arquivarOuReativar() {
        Optional<ItemCatalogo<T>> escolhido = escolhido();
        if (escolhido.isEmpty()) {
            return;
        }
        ItemCatalogo<T> item = escolhido.get();
        if (!item.arquivado() && !confirmar("Arquivar " + item.info().nome() + "?",
                "O item sai das escolhas (dimensionamento, motor, veículo), mas continua nos cálculos antigos e no "
                        + "histórico. Dá para reativar depois, com \"Mostrar arquivados\".")) {
            return;
        }
        try {
            casos.arquivar(item.id(), !item.arquivado());
            mensagem.setText((item.arquivado() ? "Reativado: " : "Arquivado: ") + item.info().nome() + ".");
            atualizar();
        } catch (ErroDados erro) {
            Alert alerta = Alertas.criar(Alert.AlertType.ERROR, erro.getMessage(), ButtonType.OK);
            if (janela() != null) {
                alerta.initOwner(janela());
            }
            alerta.setHeaderText("Não foi possível arquivar");
            alerta.showAndWait();
        }
    }

    private void versoes() {
        escolhido().ifPresent(item -> janelaVersoes(item).showAndWait());
    }

    // ===================== Tabela =====================

    private void montarTabela() {
        tabela.setPlaceholder(vazio);
        vazio.getStyleClass().add("dica");
        vazio.setWrapText(true);
        tabela.getColumns().add(coluna("Nome", 300, item -> item.info().nome()));
        List<String> colunas = formulario.colunas();
        for (int i = 0; i < colunas.size(); i++) {
            int indice = i;
            tabela.getColumns().add(coluna(colunas.get(i), 150, item -> formulario.resumo(item.dados()).get(indice)));
        }
        tabela.getColumns().add(coluna("Versão", 70, item -> "v" + item.versao()));
        tabela.getColumns().add(coluna("Conferência", 110, item -> item.info().aVerificar() ? "a verificar" : ""));
        tabela.getColumns().add(coluna("Preço de referência", 150, item -> item.info().precoReferencia() == null ? ""
                : item.info().precoReferencia().formatar()));
        tabela.getColumns().add(coluna("Fonte", 260, item -> item.info().fonte()));
        tabela.getColumns().add(coluna("Situação", 100, item -> item.arquivado() ? "arquivado" : ""));
        tabela.setRowFactory(lista -> {
            TableRow<ItemCatalogo<T>> linha = new TableRow<>() {
                @Override
                protected void updateItem(ItemCatalogo<T> item, boolean vazia) {
                    super.updateItem(item, vazia);
                    getStyleClass().remove("linha-arquivada");
                    if (!vazia && item != null && item.arquivado()) {
                        getStyleClass().add("linha-arquivada");
                    }
                }
            };
            linha.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2 && !linha.isEmpty()) {
                    editar();
                }
            });
            return linha;
        });
        tabela.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER) {
                editar();
            }
        });
    }

    private TableColumn<ItemCatalogo<T>, String> coluna(String titulo, double largura,
            Function<ItemCatalogo<T>, String> valor) {
        TableColumn<ItemCatalogo<T>, String> coluna = new TableColumn<>(titulo);
        coluna.setCellValueFactory(celula -> new ReadOnlyStringWrapper(valor.apply(celula.getValue())));
        coluna.setPrefWidth(largura);
        return coluna;
    }

    /**
     * Célula com várias linhas (a tabela do tempo morto). Um {@link Text} dentro dela faz a linha da tabela crescer
     * até caber tudo; o texto da própria célula cortaria com "...".
     */
    private static TableCell<LinhaHistorico, String> celulaDoHistorico(int indice) {
        return new TableCell<>() {
            private final Text conteudo = new Text();

            {
                conteudo.wrappingWidthProperty().bind(widthProperty().subtract(12));
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String texto, boolean vazia) {
                super.updateItem(texto, vazia);
                getStyleClass().remove("celula-mudou");
                conteudo.setText(vazia ? null : texto);
                setGraphic(vazia ? null : conteudo);
                if (!vazia && getTableRow() != null && getTableRow().getItem() != null
                        && getTableRow().getItem().mudou().get(indice)) {
                    getStyleClass().add("celula-mudou");
                }
            }
        };
    }

    private void atualizarBotoes() {
        ItemCatalogo<T> item = tabela.getSelectionModel().getSelectedItem();
        boolean nenhum = item == null;
        editar.setDisable(nenhum);
        duplicar.setDisable(nenhum);
        arquivar.setDisable(nenhum);
        versoes.setDisable(nenhum);
        acoesDoItem.forEach(botao -> botao.setDisable(nenhum));
        arquivar.setText(item != null && item.arquivado() ? "Reativar" : "Arquivar");
    }

    /**
     * Botão a mais na barra, que age sobre o item escolhido (ex.: "Mapa do compressor…" nos turbos). Se a ação gravar
     * uma versão nova, a lista é lida de novo.
     */
    void adicionarAcao(String texto, String id, Function<ItemCatalogo<T>, Optional<ItemCatalogo<T>>> acao) {
        Button botao = new Button(texto);
        botao.setId(id);
        botao.setOnAction(evento -> escolhido().flatMap(acao).ifPresent(gravado -> {
            atualizar();
            selecionar(gravado.id());
            mensagem.setText("Gravado: " + gravado.info().nome() + " (v" + gravado.versao() + ").");
        }));
        acoesDoItem.add(botao);
        barra.getChildren().add(botao);
        atualizarBotoes();
    }

    /**
     * Botão a mais ao lado de "Cadastrar…", que não depende do item escolhido (ex.: "Serviços sugeridos…" nos
     * serviços). Se a ação devolver uma mensagem, a lista é lida de novo e a mensagem aparece embaixo dela.
     */
    void adicionarAcaoGeral(String texto, String id, Supplier<Optional<String>> acao) {
        Button botao = new Button(texto);
        botao.setId(id);
        botao.setOnAction(evento -> acao.get().ifPresent(feito -> {
            atualizar();
            mensagem.setText(feito);
        }));
        barra.getChildren().add(barra.getChildren().indexOf(cadastrar) + 1, botao);
    }

    private void selecionar(long id) {
        tabela.getItems().stream().filter(item -> item.id() == id).findFirst().ifPresent(item -> {
            tabela.getSelectionModel().select(item);
            tabela.scrollTo(item);
        });
    }

    private Optional<ItemCatalogo<T>> escolhido() {
        return Optional.ofNullable(tabela.getSelectionModel().getSelectedItem());
    }

    private Window janela() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private boolean confirmar(String titulo, String texto) {
        ButtonType sim = new ButtonType("Arquivar", ButtonBar.ButtonData.OK_DONE);
        ButtonType nao = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = Alertas.criar(Alert.AlertType.CONFIRMATION, texto, sim, nao);
        if (janela() != null) {
            alerta.initOwner(janela());
        }
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        return alerta.showAndWait().filter(sim::equals).isPresent();
    }
}
