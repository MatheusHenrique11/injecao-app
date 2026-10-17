package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.oficina.PadroesOs;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.config.Chave;
import br.com.calibra.dados.config.ConfiguracaoRepositorio;
import br.com.calibra.dados.oficina.MecanicoSalvo;

/**
 * Configurações › OS (bloco 3.3): a garantia que vem na entrega, o texto acima das assinaturas na via do cliente, os
 * itens da vistoria (gravados com "Salvar padrões da OS") e os mecânicos (cada mudança vale na hora).
 */
public class ConfiguracoesOsController {

    @FXML private TextField garantiaDias;
    @FXML private TextArea textoVia;
    @FXML private ListView<String> itensVistoria;
    @FXML private Button botaoRenomearItem;
    @FXML private Button botaoRemoverItem;
    @FXML private Button botaoSubirItem;
    @FXML private Button botaoDescerItem;
    @FXML private VBox mensagensOs;
    @FXML private TableView<MecanicoSalvo> mecanicos;
    @FXML private TableColumn<MecanicoSalvo, String> colunaMecanico;
    @FXML private TableColumn<MecanicoSalvo, String> colunaSituacao;
    @FXML private Button botaoRenomearMecanico;
    @FXML private Button botaoAtivarMecanico;
    @FXML private VBox mensagensMecanicos;

    private ConfiguracaoRepositorio configuracao;
    private CasosOficina oficina;
    private PadroesOs salvos = PadroesOs.padrao();

    @FXML
    private void initialize() {
        colunaMecanico.setCellValueFactory(celula -> new ReadOnlyStringWrapper(celula.getValue().nome()));
        colunaSituacao.setCellValueFactory(celula -> new ReadOnlyStringWrapper(celula.getValue().ativo() ? "Ativo"
                : "Inativo"));
        mecanicos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        itensVistoria.getSelectionModel().selectedIndexProperty().addListener((obs, antes, depois) -> mostrarBotoes());
        itensVistoria.getItems().addListener((ListChangeListener<String>) mudanca -> mostrarBotoes());
        mecanicos.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) -> mostrarBotoes());
        mostrarBotoes();
    }

    void iniciar(ConfiguracaoRepositorio configuracao, CasosOficina oficina) {
        this.configuracao = configuracao;
        this.oficina = oficina;
        salvos = configuracao.ler(Chave.OS);
        descartar();
        carregarMecanicos(null);
    }

    // ===================== Padrões da OS =====================

    @FXML
    void salvar() {
        Integer dias = dias(garantiaDias.getText());
        if (dias == null) {
            mostrarErros(mensagensOs, List.of("Garantia padrão: informe os dias, em número inteiro (0 = sem garantia)."));
            return;
        }
        PadroesOs novos = new PadroesOs(dias, List.copyOf(itensVistoria.getItems()), textoVia.getText());
        List<String> erros = novos.validar();
        if (!erros.isEmpty()) {
            mostrarErros(mensagensOs, erros);
            return;
        }
        try {
            configuracao.gravar(Chave.OS, novos);
        } catch (ErroDados erro) {
            mostrarErros(mensagensOs, List.of(erro.getMessage()));
            return;
        }
        salvos = configuracao.ler(Chave.OS);
        descartar();
        mostrarOk(mensagensOs, "Padrões da OS salvos. Valem para as próximas entradas e entregas.");
    }

    @FXML
    void descartar() {
        preencher(salvos);
        mensagensOs.getChildren().clear();
    }

    /** Só preenche os campos: grava ao salvar. */
    @FXML
    private void padroesDeFabrica() {
        preencher(PadroesOs.padrao());
        mostrarOk(mensagensOs, "Os padrões de fábrica estão nos campos. Clique em \"Salvar padrões da OS\" para gravar.");
    }

    private void preencher(PadroesOs padroes) {
        garantiaDias.setText(Integer.toString(padroes.garantiaDias()));
        textoVia.setText(padroes.textoVia());
        itensVistoria.getItems().setAll(padroes.itensVistoria());
    }

    // ===================== Itens da vistoria =====================

    @FXML
    private void adicionarItem() {
        perguntar("Novo item da vistoria", "Nome do item (ex.: Motor e cofre)", "").ifPresent(this::adicionarItem);
    }

    /** Acrescenta o item depois do escolhido (ou no fim); repetido não entra. */
    void adicionarItem(String nome) {
        String limpo = nome.strip();
        if (limpo.isEmpty()) {
            return;
        }
        if (repetido(limpo, -1)) {
            mostrarErros(mensagensOs, List.of("O item \"" + limpo + "\" já está na vistoria."));
            return;
        }
        int escolhido = itensVistoria.getSelectionModel().getSelectedIndex();
        int posicao = escolhido < 0 ? itensVistoria.getItems().size() : escolhido + 1;
        itensVistoria.getItems().add(posicao, limpo);
        itensVistoria.getSelectionModel().select(posicao);
        mensagensOs.getChildren().clear();
    }

    @FXML
    private void renomearItem() {
        int escolhido = itensVistoria.getSelectionModel().getSelectedIndex();
        if (escolhido < 0) {
            return;
        }
        perguntar("Renomear item da vistoria", "Novo nome do item", itensVistoria.getItems().get(escolhido))
                .map(String::strip).filter(nome -> !nome.isEmpty()).ifPresent(nome -> {
                    if (repetido(nome, escolhido)) {
                        mostrarErros(mensagensOs, List.of("O item \"" + nome + "\" já está na vistoria."));
                    } else {
                        itensVistoria.getItems().set(escolhido, nome);
                        mensagensOs.getChildren().clear();
                    }
                });
    }

    @FXML
    private void removerItem() {
        int escolhido = itensVistoria.getSelectionModel().getSelectedIndex();
        if (escolhido >= 0) {
            itensVistoria.getItems().remove(escolhido);
        }
    }

    @FXML
    private void subirItem() {
        mover(-1);
    }

    @FXML
    private void descerItem() {
        mover(1);
    }

    private void mover(int passo) {
        int escolhido = itensVistoria.getSelectionModel().getSelectedIndex();
        int destino = escolhido + passo;
        if (escolhido < 0 || destino < 0 || destino >= itensVistoria.getItems().size()) {
            return;
        }
        List<String> itens = new ArrayList<>(itensVistoria.getItems());
        itens.add(destino, itens.remove(escolhido));
        itensVistoria.getItems().setAll(itens);
        itensVistoria.getSelectionModel().select(destino);
    }

    private boolean repetido(String nome, int ignorar) {
        for (int i = 0; i < itensVistoria.getItems().size(); i++) {
            if (i != ignorar && itensVistoria.getItems().get(i).toLowerCase(Locale.ROOT)
                    .equals(nome.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    // ===================== Mecânicos =====================

    @FXML
    private void novoMecanico() {
        perguntar("Novo mecânico", "Nome do mecânico", "").ifPresent(nome -> executarMecanico(() -> carregarMecanicos(
                oficina.novoMecanico(nome)), "Mecânico cadastrado."));
    }

    @FXML
    private void renomearMecanico() {
        MecanicoSalvo escolhido = mecanicos.getSelectionModel().getSelectedItem();
        if (escolhido != null) {
            perguntar("Renomear mecânico", "Novo nome do mecânico", escolhido.nome()).ifPresent(nome ->
                    executarMecanico(() -> {
                        oficina.renomearMecanico(escolhido.id(), nome);
                        carregarMecanicos(escolhido.id());
                    }, "Nome corrigido; as OS dele mostram o nome novo."));
        }
    }

    @FXML
    private void ativarOuDesativar() {
        MecanicoSalvo escolhido = mecanicos.getSelectionModel().getSelectedItem();
        if (escolhido != null) {
            executarMecanico(() -> {
                oficina.ativarMecanico(escolhido.id(), !escolhido.ativo());
                carregarMecanicos(escolhido.id());
            }, escolhido.ativo() ? escolhido.nome() + " ficou inativo." : escolhido.nome() + " voltou a ficar ativo.");
        }
    }

    private void executarMecanico(Runnable acao, String sucesso) {
        try {
            acao.run();
            mostrarOk(mensagensMecanicos, sucesso);
        } catch (EntradaInvalidaException erro) {
            mostrarErros(mensagensMecanicos, erro.erros());
        } catch (ErroDados erro) {
            mostrarErros(mensagensMecanicos, List.of(erro.getMessage()));
        }
    }

    /** Todos, também os inativos; {@code escolher} fica escolhido na lista. */
    private void carregarMecanicos(Long escolher) {
        List<MecanicoSalvo> todos = oficina.mecanicos(true);
        mecanicos.getItems().setAll(todos);
        todos.stream().filter(mecanico -> escolher != null && mecanico.id() == escolher).findFirst()
                .ifPresent(mecanicos.getSelectionModel()::select);
        mostrarBotoes();
    }

    // ===================== Utilidades =====================

    private void mostrarBotoes() {
        int item = itensVistoria.getSelectionModel().getSelectedIndex();
        botaoRenomearItem.setDisable(item < 0);
        botaoRemoverItem.setDisable(item < 0);
        botaoSubirItem.setDisable(item <= 0);
        botaoDescerItem.setDisable(item < 0 || item >= itensVistoria.getItems().size() - 1);
        MecanicoSalvo mecanico = mecanicos.getSelectionModel().getSelectedItem();
        botaoRenomearMecanico.setDisable(mecanico == null);
        botaoAtivarMecanico.setDisable(mecanico == null);
        botaoAtivarMecanico.setText(mecanico == null || mecanico.ativo() ? "Desativar" : "Reativar");
    }

    private Optional<String> perguntar(String titulo, String pergunta, String atual) {
        TextInputDialog dialogo = new TextInputDialog(atual);
        Window dono = mecanicos.getScene() == null ? null : mecanicos.getScene().getWindow();
        if (dono != null) {
            dialogo.initOwner(dono);
        }
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(pergunta);
        dialogo.setContentText("Nome:");
        return dialogo.showAndWait().map(String::strip).filter(nome -> !nome.isEmpty());
    }

    /** Os dias digitados; null se não for um número inteiro. */
    private static Integer dias(String texto) {
        try {
            return Integer.valueOf(texto == null ? "" : texto.strip());
        } catch (NumberFormatException erro) {
            return null;
        }
    }

    private static void mostrarErros(VBox caixa, List<String> erros) {
        caixa.getChildren().clear();
        for (String erro : erros) {
            Label rotulo = new Label("• " + erro);
            rotulo.getStyleClass().add("erro");
            rotulo.setWrapText(true);
            caixa.getChildren().add(rotulo);
        }
    }

    private static void mostrarOk(VBox caixa, String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("ok");
        rotulo.setWrapText(true);
        caixa.getChildren().setAll(rotulo);
    }
}
