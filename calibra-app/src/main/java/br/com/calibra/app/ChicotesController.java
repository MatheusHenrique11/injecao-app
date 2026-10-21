package br.com.calibra.app;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosChicote;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.oficina.FichaChicoteSalva;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.relatorios.DadosFichaChicote;
import br.com.calibra.relatorios.RelatorioChicote;

/**
 * As fichas de chicote da OS, na aba Execução (Fase 3B, SPEC 6.6.5): a lista, "Nova ficha…", abrir (o duplo clique
 * faz o mesmo), a versão nova (a anterior fica só para ver, em cinza), o PDF, a impressão e apagar a feita por engano.
 * Com a OS entregue ou cancelada, as fichas ficam só para ver, e o PDF continua saindo.
 */
public class ChicotesController {

    /** O que a área pede à tela da OS: ler tudo de novo e mostrar o resultado embaixo. */
    interface Tela {

        /** Leu tudo de novo depois de gravar: mostra a mensagem. */
        void gravado(String mensagem);

        /** Uma mensagem para a barra de baixo (ex.: "PDF salvo em…"). */
        void mostrarOk(String mensagem);

        void mostrarErros(List<String> erros);
    }

    /**
     * O que a área usa do resto do app.
     *
     * @param documento os dados do PDF de cada ficha, pelo id (com a oficina e o logo de agora)
     * @param temporaria onde o PDF de uma impressão fica enquanto as páginas são desenhadas
     */
    record Recursos(CasosChicote chicotes, long osId, HostServices servicos, Path temporaria,
            Function<Long, DadosFichaChicote> documento, Tela tela) {
    }

    @FXML private Label situacaoFichas;
    @FXML private TableView<FichaChicoteSalva> tabelaFichas;
    @FXML private TableColumn<FichaChicoteSalva, String> colunaVersao;
    @FXML private TableColumn<FichaChicoteSalva, String> colunaInjecao;
    @FXML private TableColumn<FichaChicoteSalva, String> colunaPontos;
    @FXML private TableColumn<FichaChicoteSalva, String> colunaSituacaoFicha;
    @FXML private TableColumn<FichaChicoteSalva, String> colunaAtualizada;
    @FXML private Button botaoNovaFicha;
    @FXML private Button botaoAbrirFicha;
    @FXML private Button botaoVersaoNova;
    @FXML private Button botaoPdfFicha;
    @FXML private Button botaoImprimirFicha;
    @FXML private Button botaoApagarFicha;

    private Recursos recursos;
    private String numeroOs = "";
    private boolean leitura;

    @FXML
    private void initialize() {
        coluna(colunaVersao, f -> f.ficha().versaoChicote());
        coluna(colunaInjecao, f -> f.ficha().injecao().isEmpty() ? "—" : f.ficha().injecao());
        coluna(colunaPontos, f -> Integer.toString(f.ficha().pontos().size()));
        coluna(colunaSituacaoFicha, ChicotesController::situacao);
        coluna(colunaAtualizada, f -> Datas.diaHora(f.atualizadoEm()));
        tabelaFichas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabelaFichas.setRowFactory(lista -> {
            TableRow<FichaChicoteSalva> linha = new TableRow<>() {
                @Override
                protected void updateItem(FichaChicoteSalva ficha, boolean vazio) {
                    super.updateItem(ficha, vazio);
                    getStyleClass().remove("linha-retirada");
                    if (!vazio && ficha != null && ficha.temVersaoNova()) {
                        getStyleClass().add("linha-retirada");
                    }
                }
            };
            linha.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2 && !linha.isEmpty()) {
                    abrir(linha.getItem());
                }
            });
            return linha;
        });
        for (Button botao : List.of(botaoAbrirFicha, botaoPdfFicha, botaoImprimirFicha)) {
            botao.disableProperty().bind(tabelaFichas.getSelectionModel().selectedItemProperty().isNull());
        }
        tabelaFichas.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) -> {
            boolean anterior = depois == null || depois.temVersaoNova();
            botaoVersaoNova.setDisable(anterior);
            botaoApagarFicha.setDisable(anterior);
        });
        botaoVersaoNova.setDisable(true);
        botaoApagarFicha.setDisable(true);
    }

    void iniciar(Recursos recursos) {
        this.recursos = Objects.requireNonNull(recursos, "recursos");
    }

    /** Lê as fichas de novo e mostra conforme o status da OS. */
    void atualizar(OrdemServicoSalva os) {
        if (recursos == null) {
            return;
        }
        numeroOs = os.numero().texto();
        leitura = os.status().finalizada();
        FichaChicoteSalva escolhida = tabelaFichas.getSelectionModel().getSelectedItem();
        List<FichaChicoteSalva> fichas = recursos.chicotes().daOs(recursos.osId());
        tabelaFichas.getItems().setAll(fichas);
        if (escolhida != null) {
            fichas.stream().filter(f -> f.id() == escolhida.id()).findFirst()
                    .ifPresent(f -> tabelaFichas.getSelectionModel().select(f));
        }
        botaoNovaFicha.setDisable(leitura);
        for (Button botao : List.of(botaoVersaoNova, botaoApagarFicha)) {
            botao.setVisible(!leitura);
            botao.setManaged(!leitura);
        }
        situacaoFichas.setText(leitura ? "OS " + os.status().rotulo().toLowerCase(Locale.ROOT)
                + ": as fichas ficam só para ver; o PDF continua saindo."
                : "Uma ficha para cada chicote: a injeção, a fonte do pinout e os pontos. A versão nova (ex.: o "
                        + "cliente pediu mais um sensor) não apaga a anterior, que fica só para ver.");
    }

    /** As fichas da OS, como estão na lista. */
    List<FichaChicoteSalva> fichas() {
        return List.copyOf(tabelaFichas.getItems());
    }

    boolean temFicha() {
        return !tabelaFichas.getItems().isEmpty();
    }

    // ===================== Ações =====================

    @FXML
    private void nova() {
        try {
            JanelaChicote janela = janelaNova();
            if (janela.mostrar()) {
                aposGravar(janela);
            }
        } catch (ErroDados erro) {
            recursos.tela().mostrarErros(List.of(erro.getMessage()));
        }
    }

    @FXML
    private void abrir() {
        escolhida().ifPresent(this::abrir);
    }

    private void abrir(FichaChicoteSalva ficha) {
        JanelaChicote janela = janelaAbrir(ficha);
        if (janela.mostrar()) {
            aposGravar(janela);
        }
    }

    @FXML
    private void versaoNova() {
        Optional<FichaChicoteSalva> ficha = escolhida();
        if (ficha.isEmpty()) {
            return;
        }
        try {
            JanelaChicote janela = janelaVersaoNova(ficha.get());
            if (janela.mostrar()) {
                aposGravar(janela);
            }
        } catch (ErroDados erro) {
            recursos.tela().mostrarErros(List.of(erro.getMessage()));
        }
    }

    @FXML
    private void pdf() {
        escolhida().ifPresent(this::pdf);
    }

    /** O PDF da ficha, perguntando onde salvar. */
    void pdf(FichaChicoteSalva ficha) {
        DadosFichaChicote dados = recursos.documento().apply(ficha.id());
        ArquivoPdf.gerar(janela(), recursos.servicos(), "Ficha do chicote", numeroOs + "-chicote-"
                + ficha.ficha().versaoChicote().replaceAll("[^\\p{L}\\p{N}.-]", "") + ".pdf",
                destino -> RelatorioChicote.gerar(dados, destino), recursos.tela()::mostrarOk, () -> { });
    }

    @FXML
    private void imprimir() {
        escolhida().ifPresent(this::imprimir);
    }

    /** A janela de impressão do Windows, com a ficha. */
    void imprimir(FichaChicoteSalva ficha) {
        DadosFichaChicote dados = recursos.documento().apply(ficha.id());
        Impressao.imprimir(janela(), "Ficha do chicote " + numeroOs, recursos.temporaria(),
                destino -> RelatorioChicote.gerar(dados, destino), recursos.tela()::mostrarOk);
    }

    @FXML
    private void apagar() {
        Optional<FichaChicoteSalva> ficha = escolhida();
        if (ficha.isEmpty()) {
            return;
        }
        Alert alerta = Alertas.criar(Alert.AlertType.CONFIRMATION, "A ficha do chicote " + ficha.get().ficha()
                .versaoChicote() + " sai da OS. Use só para a ficha feita por engano: para mudar o chicote, use Nova "
                + "versão (a anterior fica guardada).", ButtonType.YES, ButtonType.NO);
        alerta.initOwner(janela());
        alerta.setTitle("Apagar ficha do chicote");
        alerta.setHeaderText("Apagar a ficha do chicote?");
        if (alerta.showAndWait().filter(ButtonType.YES::equals).isPresent()) {
            apagar(ficha.get());
        }
    }

    /** Apaga a ficha, sem perguntar (a confirmação fica no botão; para os testes). */
    void apagar(FichaChicoteSalva ficha) {
        try {
            recursos.chicotes().apagar(ficha.id());
            recursos.tela().gravado("Ficha do chicote apagada.");
        } catch (EntradaInvalidaException erro) {
            recursos.tela().mostrarErros(erro.erros());
        } catch (ErroDados erro) {
            recursos.tela().mostrarErros(List.of(erro.getMessage()));
        }
    }

    // ===================== Janelas (sem mostrar, para os testes) =====================

    /** A janela da ficha nova, já preenchida. */
    JanelaChicote janelaNova() {
        return JanelaChicote.nova(janela(), recursos.chicotes(), recursos.osId(), numeroOs);
    }

    /** A janela de uma ficha gravada (só para ver com a OS entregue ou cancelada, ou com versão nova). */
    JanelaChicote janelaAbrir(FichaChicoteSalva ficha) {
        return JanelaChicote.abrir(janela(), recursos.chicotes(), ficha, numeroOs, leitura);
    }

    /** A janela da versão nova, começando da escolhida. */
    JanelaChicote janelaVersaoNova(FichaChicoteSalva anterior) {
        return JanelaChicote.versaoNova(janela(), recursos.chicotes(), anterior, numeroOs);
    }

    /** Depois de gravar pela janela: a tela da OS lê tudo de novo e mostra a mensagem. */
    void aposGravar(JanelaChicote janela) {
        recursos.tela().gravado(janela.mensagem());
    }

    TableView<FichaChicoteSalva> tabela() {
        return tabelaFichas;
    }

    // ===================== Utilidades =====================

    /** "testado", ou "testado (versão anterior, só para ver)". */
    static String situacao(FichaChicoteSalva ficha) {
        String rotulo = ficha.ficha().rotuloDaSituacao();
        return ficha.temVersaoNova() ? rotulo + " (versão anterior, só para ver)" : rotulo;
    }

    private Optional<FichaChicoteSalva> escolhida() {
        return Optional.ofNullable(tabelaFichas.getSelectionModel().getSelectedItem());
    }

    private static <T> void coluna(TableColumn<T, String> coluna, Function<T, String> texto) {
        coluna.setCellValueFactory(celula -> new ReadOnlyStringWrapper(texto.apply(celula.getValue())));
    }

    private Window janela() {
        return tabelaFichas.getScene() == null ? null : tabelaFichas.getScene().getWindow();
    }
}
