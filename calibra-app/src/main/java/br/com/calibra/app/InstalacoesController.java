package br.com.calibra.app;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosInstalacao;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.cadastro.InstalacaoSalva;
import br.com.calibra.dados.oficina.OrdemServicoSalva;

/**
 * As instalações do veículo (Fase 3B, SPEC 6.2): a lista, "Nova instalação…", editar (o duplo clique faz o mesmo), o
 * histórico, retirar (o item continua na lista, em cinza) e apagar o cadastrado por engano. Na ficha do veículo, mostra
 * todas e sempre muda; na aba Execução da OS, mostra as da OS e, com a OS entregue ou cancelada, fica só para ver (a
 * instalação continua mudando pela ficha).
 */
public class InstalacoesController {

    /** O que a área pede à tela onde está: ler tudo de novo e mostrar o resultado embaixo. */
    interface Tela {

        /** Leu tudo de novo depois de gravar: mostra a mensagem. */
        void gravado(String mensagem);

        void mostrarErros(List<String> erros);
    }

    /**
     * O que a área usa do resto do app.
     *
     * @param osId na aba Execução, a OS (a lista mostra as dela e a nova já vem com ela); null na ficha do veículo
     */
    record Recursos(CasosInstalacao casos, long veiculoId, Long osId, Clock relogio, Tela tela) {
    }

    private static final String RETIRAR = "Retirar…";
    private static final String DESFAZER = "Desfazer retirada";

    @FXML private Label titulo;
    @FXML private Label situacao;
    @FXML private Label vazia;
    @FXML private TableView<InstalacaoSalva> tabela;
    @FXML private TableColumn<InstalacaoSalva, String> colunaTipo;
    @FXML private TableColumn<InstalacaoSalva, String> colunaDescricao;
    @FXML private TableColumn<InstalacaoSalva, String> colunaMarcaModelo;
    @FXML private TableColumn<InstalacaoSalva, String> colunaEspecificacao;
    @FXML private TableColumn<InstalacaoSalva, String> colunaData;
    @FXML private TableColumn<InstalacaoSalva, String> colunaOs;
    @FXML private TableColumn<InstalacaoSalva, String> colunaSituacao;
    @FXML private Button botaoNovaInstalacao;
    @FXML private Button botaoEditarInstalacao;
    @FXML private Button botaoHistoricoInstalacao;
    @FXML private Button botaoRetirarInstalacao;
    @FXML private Button botaoApagarInstalacao;

    private Recursos recursos;
    private boolean leitura;

    @FXML
    private void initialize() {
        coluna(colunaTipo, i -> i.instalacao().tipo().rotulo());
        coluna(colunaDescricao, i -> LaudosController.resumo(i.instalacao().descricao()));
        coluna(colunaMarcaModelo, i -> traco(i.instalacao().marcaEModelo()));
        coluna(colunaEspecificacao, i -> traco(i.instalacao().potenciaOuEspecificacao()));
        coluna(colunaData, i -> i.instalacao().dataInstalacao() == null ? "—"
                : Datas.dia(i.instalacao().dataInstalacao()));
        coluna(colunaOs, i -> i.numeroOs() == null ? "—" : i.numeroOs().texto());
        coluna(colunaSituacao, InstalacoesController::situacao);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabela.setRowFactory(lista -> {
            TableRow<InstalacaoSalva> linha = new TableRow<>() {
                @Override
                protected void updateItem(InstalacaoSalva instalacao, boolean vazio) {
                    super.updateItem(instalacao, vazio);
                    getStyleClass().remove("linha-retirada");
                    if (!vazio && instalacao != null && instalacao.retirada()) {
                        getStyleClass().add("linha-retirada");
                    }
                }
            };
            linha.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2 && !linha.isEmpty()
                        && !leitura) {
                    editar(linha.getItem());
                }
            });
            return linha;
        });
        for (Button botao : List.of(botaoEditarInstalacao, botaoHistoricoInstalacao, botaoRetirarInstalacao,
                botaoApagarInstalacao)) {
            botao.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        }
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) ->
                botaoRetirarInstalacao.setText(depois != null && depois.retirada() ? DESFAZER : RETIRAR));
    }

    void iniciar(Recursos recursos) {
        this.recursos = Objects.requireNonNull(recursos, "recursos");
        boolean naOs = recursos.osId() != null;
        titulo.setText(naOs ? "Instalações desta OS" : "Instalações");
        botaoNovaInstalacao.setText(naOs ? "Registrar instalação…" : "Nova instalação…");
        vazia.setText(naOs ? "Nenhuma instalação registrada nesta OS."
                : "Nenhuma instalação registrada neste veículo.");
        colunaOs.setVisible(!naOs);
        atualizar();
    }

    /** Na OS: lê de novo e mostra conforme o status (entregue ou cancelada: só para ver). */
    void atualizar(OrdemServicoSalva os) {
        leitura = os.status().finalizada();
        situacao.setText(leitura ? "OS " + os.status().rotulo().toLowerCase(Locale.ROOT) + ": as instalações se mudam "
                + "pela ficha do veículo." : "O que foi instalado neste serviço (marca, modelo, potência ou firmware). "
                        + "Aparece também na ficha do veículo.");
        for (Node botao : List.of(botaoNovaInstalacao, botaoEditarInstalacao, botaoRetirarInstalacao,
                botaoApagarInstalacao)) {
            botao.setVisible(!leitura);
            botao.setManaged(!leitura);
        }
        atualizar();
    }

    /** Lê as instalações de novo; a escolhida continua escolhida. */
    void atualizar() {
        if (recursos == null) {
            return;
        }
        if (recursos.osId() == null) {
            situacao.setText("O que foi instalado ou mexido no veículo, pela oficina (com a OS de origem) ou informado "
                    + "pelo cliente. O item que saiu do carro se retira: ele continua na lista, em cinza.");
        }
        InstalacaoSalva escolhida = tabela.getSelectionModel().getSelectedItem();
        List<InstalacaoSalva> lidas = recursos.osId() == null ? recursos.casos().doVeiculo(recursos.veiculoId())
                : recursos.casos().daOs(recursos.osId());
        tabela.getItems().setAll(lidas);
        if (escolhida != null) {
            lidas.stream().filter(i -> i.id() == escolhida.id()).findFirst()
                    .ifPresent(i -> tabela.getSelectionModel().select(i));
        }
    }

    /** As instalações como estão na lista. */
    List<InstalacaoSalva> instalacoes() {
        return List.copyOf(tabela.getItems());
    }

    // ===================== Ações =====================

    @FXML
    private void nova() {
        try {
            JanelaInstalacao janela = janelaNova();
            if (janela.mostrar()) {
                aposGravar(janela);
            }
        } catch (ErroDados erro) {
            recursos.tela().mostrarErros(List.of(erro.getMessage()));
        }
    }

    @FXML
    private void editar() {
        escolhida().ifPresent(this::editar);
    }

    private void editar(InstalacaoSalva instalacao) {
        JanelaInstalacao janela = janelaEditar(instalacao);
        if (janela.mostrar()) {
            aposGravar(janela);
        }
    }

    @FXML
    private void historico() {
        escolhida().ifPresent(instalacao -> JanelasCadastro.historicoInstalacao(janela(), recursos.casos(),
                instalacao));
    }

    @FXML
    private void retirarOuDesfazer() {
        Optional<InstalacaoSalva> instalacao = escolhida();
        if (instalacao.isEmpty()) {
            return;
        }
        if (instalacao.get().retirada()) {
            desfazerRetirada(instalacao.get());
        } else if (JanelasCadastro.retirarInstalacao(janela(), recursos.casos(), instalacao.get(),
                LocalDate.now(recursos.relogio()))) {
            recursos.tela().gravado("Instalação retirada: ela continua na lista, marcada.");
        }
    }

    /** Retira sem perguntar a data (a janela fica no botão; para os testes). */
    void retirar(InstalacaoSalva instalacao, LocalDate data) {
        executar(() -> recursos.casos().retirar(instalacao.id(), data),
                "Instalação retirada: ela continua na lista, marcada.");
    }

    void desfazerRetirada(InstalacaoSalva instalacao) {
        executar(() -> recursos.casos().desfazerRetirada(instalacao.id()),
                "Retirada desfeita: o item volta a estar no veículo.");
    }

    @FXML
    private void apagar() {
        Optional<InstalacaoSalva> instalacao = escolhida();
        if (instalacao.isEmpty()) {
            return;
        }
        Alert alerta = Alertas.criar(Alert.AlertType.CONFIRMATION, "A instalação \"" + nome(instalacao.get())
                + "\" sai da lista, com o histórico dela. Use só para o cadastro feito por engano: o item que saiu do "
                + "carro se marca com Retirar.", ButtonType.YES, ButtonType.NO);
        alerta.initOwner(janela());
        alerta.setTitle("Apagar instalação");
        alerta.setHeaderText("Apagar a instalação?");
        if (alerta.showAndWait().filter(ButtonType.YES::equals).isPresent()) {
            apagar(instalacao.get());
        }
    }

    /** Apaga sem perguntar (a confirmação fica no botão; para os testes). */
    void apagar(InstalacaoSalva instalacao) {
        executar(() -> recursos.casos().apagar(instalacao.id()), "Instalação apagada.");
    }

    // ===================== Janelas (sem mostrar, para os testes) =====================

    /** A janela da instalação nova: pela ficha, vazia; pela OS, com a OS e a data de hoje. */
    JanelaInstalacao janelaNova() {
        CasosInstalacao.Rascunho rascunho = recursos.osId() == null ? recursos.casos().nova(recursos.veiculoId())
                : recursos.casos().novaDaOs(recursos.osId());
        return JanelaInstalacao.nova(janela(), recursos.casos(), rascunho);
    }

    JanelaInstalacao janelaEditar(InstalacaoSalva instalacao) {
        return JanelaInstalacao.editar(janela(), recursos.casos(), instalacao);
    }

    /** Depois de gravar pela janela: a tela lê tudo de novo e mostra a mensagem. */
    void aposGravar(JanelaInstalacao janela) {
        recursos.tela().gravado(janela.mensagem());
    }

    /** O botão de retirar, que vira "Desfazer retirada" com a retirada escolhida (para os testes). */
    Button botaoRetirar() {
        return botaoRetirarInstalacao;
    }

    TableView<InstalacaoSalva> tabela() {
        return tabela;
    }

    // ===================== Utilidades =====================

    /** "no veículo" ou "retirado em 20/10/2026". */
    static String situacao(InstalacaoSalva instalacao) {
        return instalacao.retirada() ? "retirado em " + Datas.dia(instalacao.retiradaEm()) : "no veículo";
    }

    private void executar(Runnable acao, String mensagem) {
        try {
            acao.run();
            recursos.tela().gravado(mensagem);
        } catch (EntradaInvalidaException erro) {
            recursos.tela().mostrarErros(erro.erros());
        } catch (ErroDados erro) {
            recursos.tela().mostrarErros(List.of(erro.getMessage()));
        }
    }

    private static String nome(InstalacaoSalva instalacao) {
        return instalacao.instalacao().tipo().rotulo() + " · " + LaudosController.resumo(instalacao.instalacao()
                .descricao());
    }

    private static String traco(String texto) {
        return texto.isEmpty() ? "—" : texto;
    }

    private Optional<InstalacaoSalva> escolhida() {
        return Optional.ofNullable(tabela.getSelectionModel().getSelectedItem());
    }

    private static <T> void coluna(TableColumn<T, String> coluna, Function<T, String> texto) {
        coluna.setCellValueFactory(celula -> new ReadOnlyStringWrapper(texto.apply(celula.getValue())));
    }

    private Window janela() {
        return tabela.getScene() == null ? null : tabela.getScene().getWindow();
    }
}
