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

import br.com.calibra.app.casos.CasosLaudo;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.oficina.LaudoSalvo;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.relatorios.DadosLaudo;
import br.com.calibra.relatorios.RelatorioLaudo;

/**
 * Os laudos de diagnóstico elétrico da OS, na aba Execução (Fase 3B, SPEC 6.6.3): a lista, "Novo laudo…", abrir (o
 * duplo clique faz o mesmo), o PDF, a impressão e apagar. Com a OS entregue ou cancelada, os laudos ficam só para ver,
 * e o PDF continua saindo.
 */
public class LaudosController {

    /** O que a área pede à tela da OS: ler tudo de novo e mostrar o resultado embaixo. */
    interface Tela {

        /** Leu tudo de novo depois de gravar: mostra a mensagem e os avisos (em amarelo). */
        void gravado(String mensagem, List<String> avisos);

        /** Uma mensagem para a barra de baixo (ex.: "PDF salvo em…"). */
        void mostrarOk(String mensagem);

        void mostrarErros(List<String> erros);
    }

    /**
     * O que a área usa do resto do app.
     *
     * @param documento os dados do PDF de cada laudo, pelo id (com a oficina e o logo de agora)
     * @param temporaria onde o PDF de uma impressão fica enquanto as páginas são desenhadas
     */
    record Recursos(CasosLaudo laudos, CasosOficina oficina, long osId, HostServices servicos, Path temporaria,
            Function<Long, DadosLaudo> documento, Tela tela) {
    }

    @FXML private Label situacao;
    @FXML private TableView<LaudoSalvo> tabela;
    @FXML private TableColumn<LaudoSalvo, String> colunaQuando;
    @FXML private TableColumn<LaudoSalvo, String> colunaMecanico;
    @FXML private TableColumn<LaudoSalvo, String> colunaQueixa;
    @FXML private TableColumn<LaudoSalvo, String> colunaResultado;
    @FXML private Button botaoNovo;
    @FXML private Button botaoAbrir;
    @FXML private Button botaoPdf;
    @FXML private Button botaoImprimir;
    @FXML private Button botaoApagar;

    private Recursos recursos;
    private String numeroOs = "";
    private boolean leitura;

    @FXML
    private void initialize() {
        colunaQuando.setCellValueFactory(c -> texto(c.getValue().laudo().dataHora() == null ? "—"
                : Datas.diaHora(c.getValue().laudo().dataHora())));
        colunaMecanico.setCellValueFactory(c -> texto(c.getValue().mecanico() == null ? "—" : c.getValue().mecanico()));
        colunaQueixa.setCellValueFactory(c -> texto(resumo(c.getValue().laudo().queixa())));
        colunaResultado.setCellValueFactory(c -> texto(JanelaLaudo.maiuscula(c.getValue().laudo()
                .rotuloDoResultado())));
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tabela.setRowFactory(lista -> {
            TableRow<LaudoSalvo> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2 && !linha.isEmpty()) {
                    abrir(linha.getItem());
                }
            });
            return linha;
        });
        for (Button botao : List.of(botaoAbrir, botaoPdf, botaoImprimir, botaoApagar)) {
            botao.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        }
    }

    void iniciar(Recursos recursos) {
        this.recursos = Objects.requireNonNull(recursos, "recursos");
    }

    /** Lê os laudos de novo e mostra conforme o status da OS. */
    void atualizar(OrdemServicoSalva os) {
        if (recursos == null) {
            return;
        }
        numeroOs = os.numero().texto();
        leitura = os.status().finalizada();
        LaudoSalvo escolhido = tabela.getSelectionModel().getSelectedItem();
        List<LaudoSalvo> laudos = recursos.laudos().daOs(recursos.osId());
        tabela.getItems().setAll(laudos);
        if (escolhido != null) {
            laudos.stream().filter(l -> l.id() == escolhido.id()).findFirst()
                    .ifPresent(l -> tabela.getSelectionModel().select(l));
        }
        botaoNovo.setDisable(leitura);
        botaoApagar.setVisible(!leitura);
        botaoApagar.setManaged(!leitura);
        situacao.setText(leitura ? "OS " + os.status().rotulo().toLowerCase(Locale.ROOT)
                + ": os laudos ficam só para ver; o PDF continua saindo."
                : "Um laudo para cada diagnóstico: a queixa, as medições, os códigos de falha, o que foi feito e o "
                        + "resultado. Dá para fazer em qualquer etapa antes da entrega.");
    }

    /** Os laudos da OS, como estão na lista. */
    List<LaudoSalvo> laudos() {
        return List.copyOf(tabela.getItems());
    }

    boolean temLaudo() {
        return !tabela.getItems().isEmpty();
    }

    // ===================== Ações =====================

    @FXML
    void novo() {
        JanelaLaudo janela = janelaNovo();
        if (janela.mostrar()) {
            aposGravar(janela);
        }
    }

    @FXML
    private void abrir() {
        escolhido().ifPresent(this::abrir);
    }

    private void abrir(LaudoSalvo laudo) {
        JanelaLaudo janela = janelaAbrir(laudo);
        if (janela.mostrar()) {
            aposGravar(janela);
        }
    }

    @FXML
    private void pdf() {
        escolhido().ifPresent(this::pdf);
    }

    /** O PDF do laudo, perguntando onde salvar. */
    void pdf(LaudoSalvo laudo) {
        DadosLaudo dados = recursos.documento().apply(laudo.id());
        ArquivoPdf.gerar(janela(), recursos.servicos(), "Laudo elétrico", numeroOs + "-laudo-" + posicao(laudo)
                + ".pdf", destino -> RelatorioLaudo.gerar(dados, destino), recursos.tela()::mostrarOk, () -> { });
    }

    @FXML
    private void imprimir() {
        escolhido().ifPresent(this::imprimir);
    }

    /** A janela de impressão do Windows, com o laudo. */
    void imprimir(LaudoSalvo laudo) {
        DadosLaudo dados = recursos.documento().apply(laudo.id());
        Impressao.imprimir(janela(), "Laudo elétrico " + numeroOs, recursos.temporaria(),
                destino -> RelatorioLaudo.gerar(dados, destino), recursos.tela()::mostrarOk);
    }

    @FXML
    private void apagar() {
        Optional<LaudoSalvo> laudo = escolhido();
        if (laudo.isEmpty()) {
            return;
        }
        Alert alerta = Alertas.criar(Alert.AlertType.CONFIRMATION, "O laudo de " + Datas.diaHora(laudo.get().laudo()
                .dataHora()) + " (" + resumo(laudo.get().laudo().queixa()) + ") sai da OS. Use só para o laudo feito "
                + "por engano.", ButtonType.YES, ButtonType.NO);
        alerta.initOwner(janela());
        alerta.setTitle("Apagar laudo");
        alerta.setHeaderText("Apagar o laudo?");
        if (alerta.showAndWait().filter(ButtonType.YES::equals).isPresent()) {
            apagar(laudo.get());
        }
    }

    /** Apaga o laudo, sem perguntar (a confirmação fica no botão; para os testes). */
    void apagar(LaudoSalvo laudo) {
        try {
            recursos.laudos().apagar(laudo.id());
            recursos.tela().gravado("Laudo apagado.", List.of());
        } catch (EntradaInvalidaException erro) {
            recursos.tela().mostrarErros(erro.erros());
        } catch (ErroDados erro) {
            recursos.tela().mostrarErros(List.of(erro.getMessage()));
        }
    }

    // ===================== Janelas (sem mostrar, para os testes) =====================

    /** A janela do laudo novo, já preenchida. */
    JanelaLaudo janelaNovo() {
        return new JanelaLaudo(janela(), recursos.laudos(), recursos.oficina(), recursos.osId(), numeroOs, null,
                false);
    }

    /** A janela de um laudo gravado (só para ver, com a OS entregue ou cancelada). */
    JanelaLaudo janelaAbrir(LaudoSalvo laudo) {
        return new JanelaLaudo(janela(), recursos.laudos(), recursos.oficina(), recursos.osId(), numeroOs, laudo,
                leitura);
    }

    /** Depois de gravar pela janela: a tela da OS lê tudo de novo e mostra a mensagem e os avisos. */
    void aposGravar(JanelaLaudo janela) {
        recursos.tela().gravado(janela.mensagem(), janela.avisos());
    }

    // ===================== Utilidades =====================

    /** A primeira linha da queixa, cortada em 90 letras (para a lista e a confirmação). */
    static String resumo(String queixa) {
        String linha = queixa == null ? "" : queixa.strip().lines().findFirst().orElse("");
        return linha.length() <= 90 ? linha : linha.substring(0, 89) + "…";
    }

    /** A posição do laudo na OS (1, 2...), para o nome do arquivo. */
    private int posicao(LaudoSalvo laudo) {
        return tabela.getItems().stream().map(LaudoSalvo::id).toList().indexOf(laudo.id()) + 1;
    }

    private Optional<LaudoSalvo> escolhido() {
        return Optional.ofNullable(tabela.getSelectionModel().getSelectedItem());
    }

    private static ReadOnlyStringWrapper texto(String valor) {
        return new ReadOnlyStringWrapper(valor);
    }

    private Window janela() {
        return tabela.getScene() == null ? null : tabela.getScene().getWindow();
    }
}
