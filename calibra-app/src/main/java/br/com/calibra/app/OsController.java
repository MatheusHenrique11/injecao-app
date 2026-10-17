package br.com.calibra.app;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import br.com.calibra.app.casos.AnexosDaOficina;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.app.casos.CasosOficina.NovoAnexo;
import br.com.calibra.app.casos.CasosOficina.OsAberta;
import br.com.calibra.app.casos.CasosOficina.ProjetoDoVeiculo;
import br.com.calibra.app.casos.CasosOficina.SituacaoDoPagamento;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.oficina.AnexoSalvo;
import br.com.calibra.dados.oficina.MecanicoSalvo;
import br.com.calibra.dados.oficina.MudancaStatusOs;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.dados.oficina.PagamentoSalvo;
import br.com.calibra.dados.projeto.VersaoMotor;

/**
 * Tela da OS (SPEC 8.9): o cabeçalho com o cliente, o veículo, o status e os passos do serviço (começar, aguardar
 * peças, retomar, pronto, entregar, cancelar) e as abas Entrada, Orçamento, Preparação, Entrega e pagamento, Anexos e
 * Histórico. Depois de cada ação, tudo é lido de novo do banco.
 */
public class OsController {

    /** O que a tela usa do resto do app. */
    record Recursos(CasosOficina oficina, CasosProjeto projetos, OrcamentoController.Recursos orcamento,
            HostServices servicos, Navegacao navegacao, Clock relogio) {
    }

    private static final double LARGURA_MINIATURA = 120;
    private static final double ALTURA_MINIATURA = 90;

    @FXML private Label titulo;
    @FXML private Label status;
    @FXML private Label subtitulo;
    @FXML private Label proximoPasso;
    @FXML private Button botaoComecar;
    @FXML private Button botaoAguardarPecas;
    @FXML private Button botaoRetomar;
    @FXML private Button botaoPronta;
    @FXML private Button botaoEntregar;
    @FXML private Button botaoCancelarOs;
    @FXML private TabPane abas;
    @FXML private Tab abaEntrada;
    // ---------- Entrada ----------
    @FXML private TextArea servicoPedido;
    @FXML private ComboBox<MecanicoSalvo> mecanico;
    @FXML private Button botaoNovoMecanico;
    @FXML private DatePicker previsao;
    @FXML private TextArea observacoes;
    @FXML private Button botaoSalvarAbertura;
    @FXML private Label situacaoEntrada;
    @FXML private EntradaFormController formEntradaController;
    @FXML private Button botaoSalvarEntrada;
    // ---------- Preparação ----------
    @FXML private Label resumoPreparacao;
    @FXML private HBox caixaComProjeto;
    @FXML private VBox caixaSemProjeto;
    @FXML private ComboBox<ProjetoDoVeiculo> projetosDoVeiculo;
    @FXML private Button botaoLigarProjeto;
    // ---------- Entrega e pagamento ----------
    @FXML private Label situacaoEntrega;
    @FXML private Button botaoEntregarAba;
    @FXML private Label valorOs;
    @FXML private Label recebido;
    @FXML private Label falta;
    @FXML private TableView<PagamentoSalvo> recebimentos;
    @FXML private TableColumn<PagamentoSalvo, String> colunaRecebidoEm;
    @FXML private TableColumn<PagamentoSalvo, String> colunaForma;
    @FXML private TableColumn<PagamentoSalvo, String> colunaValor;
    @FXML private TableColumn<PagamentoSalvo, String> colunaObservacao;
    @FXML private TableColumn<PagamentoSalvo, String> colunaSituacao;
    @FXML private Button botaoRegistrarRecebimento;
    @FXML private Button botaoCancelarRecebimento;
    // ---------- Anexos ----------
    @FXML private VBox caixaAnexos;
    @FXML private ListView<AnexoSalvo> listaAnexos;
    @FXML private Button botaoAdicionarAnexos;
    @FXML private Button botaoAbrirAnexo;
    @FXML private Button botaoEditarAnexo;
    @FXML private Button botaoRemoverAnexo;
    // ---------- Histórico ----------
    @FXML private TableView<MudancaStatusOs> historico;
    @FXML private TableColumn<MudancaStatusOs, String> colunaDataHora;
    @FXML private TableColumn<MudancaStatusOs, String> colunaStatus;
    @FXML private TableColumn<MudancaStatusOs, String> colunaHistoricoObservacao;
    @FXML private VBox mensagens;

    /** As miniaturas já carregadas, pelo id do anexo (a lista reaproveita as linhas ao rolar). */
    private final Map<Long, Image> miniaturas = new HashMap<>();
    private Recursos recursos;
    private CaixaMecanico caixaMecanico;
    private OrcamentoController orcamento;
    private long osId;
    private OsAberta aberta;

    @FXML
    private void initialize() {
        Datas.emPortugues(previsao);
        coluna(colunaRecebidoEm, p -> Datas.diaHora(p.pagamento().recebidoEm()));
        coluna(colunaForma, p -> p.pagamento().forma().rotulo());
        coluna(colunaValor, p -> p.pagamento().valor().formatar());
        coluna(colunaObservacao, p -> p.pagamento().observacao());
        coluna(colunaSituacao, p -> p.cancelado() ? "Cancelado em " + Datas.diaHora(p.canceladoEm()) + ": "
                + p.motivoCancelamento() : "Vale");
        recebimentos.setRowFactory(tabela -> new TableRow<>() {
            @Override
            protected void updateItem(PagamentoSalvo pagamento, boolean vazio) {
                super.updateItem(pagamento, vazio);
                getStyleClass().remove("linha-arquivada");
                if (!vazio && pagamento != null && pagamento.cancelado()) {
                    getStyleClass().add("linha-arquivada");
                }
            }
        });
        botaoCancelarRecebimento.disableProperty().bind(recebimentos.getSelectionModel().selectedItemProperty()
                .isNull());
        coluna(colunaDataHora, m -> Datas.diaHora(m.dataHora()));
        coluna(colunaStatus, m -> m.status().rotulo());
        coluna(colunaHistoricoObservacao, MudancaStatusOs::observacao);
        recebimentos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        historico.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        CamposTela.configurar(projetosDoVeiculo, List.of(), p -> "Projeto #" + p.projetoId() + " · " + p.motor()
                + " · " + p.status().rotulo());

        listaAnexos.setCellFactory(lista -> new CelulaAnexo());
        listaAnexos.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2
                    && listaAnexos.getSelectionModel().getSelectedItem() != null) {
                abrirAnexo();
            }
        });
        botaoAbrirAnexo.disableProperty().bind(listaAnexos.getSelectionModel().selectedItemProperty().isNull());
        botaoEditarAnexo.disableProperty().bind(listaAnexos.getSelectionModel().selectedItemProperty().isNull());
        listaAnexos.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) -> mostrarBotoesAnexo());
        caixaAnexos.setOnDragOver(evento -> {
            if (evento.getDragboard().hasFiles()) {
                evento.acceptTransferModes(TransferMode.COPY);
            }
            evento.consume();
        });
        caixaAnexos.setOnDragDropped(evento -> {
            boolean temArquivos = evento.getDragboard().hasFiles();
            if (temArquivos) {
                adicionarAnexos(evento.getDragboard().getFiles().stream().map(File::toPath).toList());
            }
            evento.setDropCompleted(temArquivos);
            evento.consume();
        });
    }

    void iniciar(Recursos recursos, long osId) {
        this.recursos = recursos;
        this.osId = osId;
        caixaMecanico = new CaixaMecanico(mecanico, recursos.oficina());
        atualizar();
        preencherFormularios();
        montarOrcamento();
    }

    /** A aba Orçamento, logo depois da Entrada: o mesmo editor do projeto, com o orçamento da OS. */
    private void montarOrcamento() {
        FXMLLoader carregador = new FXMLLoader(OsController.class.getResource("orcamento.fxml"));
        Parent tela;
        try {
            tela = carregador.load();
        } catch (IOException erro) {
            throw new UncheckedIOException("Tela do app não abriu: orcamento.fxml", erro);
        }
        Tab aba = new Tab("Orçamento", tela);
        aba.setId("abaOrcamento");
        abas.getTabs().add(abas.getTabs().indexOf(abaEntrada) + 1, aba);
        orcamento = carregador.getController();
        orcamento.iniciar(recursos.orcamento(), recursos.servicos(), FonteOrcamento.daOs(recursos.orcamento().casos(),
                osId, () -> aberta.os().projetoId() != null), this::atualizar);
        aba.setOnSelectionChanged(evento -> {
            if (aba.isSelected()) {
                orcamento.atualizar();
            }
        });
    }

    /**
     * Lê a OS de novo e mostra tudo. Os campos da abertura e da entrada ficam como estão (podem estar sendo editados);
     * eles voltam ao gravado depois de salvar.
     */
    void atualizar() {
        aberta = recursos.oficina().buscar(osId);
        List<MudancaStatusOs> mudancas = recursos.oficina().historico(osId);
        mostrarCabecalho(mudancas);
        mostrarEntrada();
        mostrarPreparacao();
        mostrarEntregaEPagamento();
        mostrarAnexos();
        historico.getItems().setAll(mudancas.reversed());
        if (orcamento != null) {
            orcamento.atualizar();
        }
    }

    /** O número da OS (o título da janela). */
    String numero() {
        return aberta.os().numero().texto();
    }

    // ===================== Cabeçalho e passos do serviço =====================

    private void mostrarCabecalho(List<MudancaStatusOs> mudancas) {
        OrdemServicoSalva os = aberta.os();
        Veiculo veiculo = aberta.veiculo().veiculo();
        titulo.setText(os.numero().texto() + " · " + aberta.cliente().cliente().nome() + " · " + veiculo.descricao()
                + (veiculo.identificacao().isEmpty() ? "" : " · " + veiculo.identificacao()));
        status.setText(os.status().rotulo());
        List<String> partes = new ArrayList<>();
        partes.add("Aberta em " + Datas.diaHora(os.criadoEm()));
        partes.add(os.entrada() == null ? "o veículo ainda não chegou" : "chegou em " + Datas.diaHora(os.entrada()
                .quando()));
        if (os.previsaoEntrega() != null) {
            partes.add("previsão de entrega " + Datas.dia(os.previsaoEntrega()));
        }
        if (aberta.mecanico() != null) {
            partes.add("mecânico: " + aberta.mecanico().nome());
        }
        SituacaoDoPagamento pagamento = aberta.pagamento();
        partes.add(pagamento.valor() == null ? "sem orçamento aprovado" : "valor " + pagamento.valor().formatar()
                + ", recebido " + pagamento.recebido().formatar() + " (" + pagamento.situacao().rotulo() + ")");
        subtitulo.setText(String.join(" · ", partes));
        proximoPasso.setText(proximoPasso(mudancas));

        StatusOs atual = os.status();
        mostrar(botaoComecar, atual.antesDaAprovacao() || atual == StatusOs.APROVADA);
        botaoComecar.setDisable(atual != StatusOs.APROVADA);
        mostrar(botaoAguardarPecas, atual == StatusOs.EM_EXECUCAO);
        mostrar(botaoRetomar, atual == StatusOs.AGUARDANDO_PECAS || atual == StatusOs.PRONTA);
        mostrar(botaoPronta, atual == StatusOs.EM_EXECUCAO);
        mostrar(botaoEntregar, atual == StatusOs.PRONTA);
        mostrar(botaoCancelarOs, !atual.finalizada());
    }

    /** O que fazer agora, em português, pelo status (e pelo que falta: o orçamento aprovado, a entrada). */
    private String proximoPasso(List<MudancaStatusOs> mudancas) {
        OrdemServicoSalva os = aberta.os();
        boolean chegou = os.veiculoChegou();
        return switch (os.status()) {
            case ABERTA -> "Próximo passo: monte o orçamento e marque como enviado (aba Orçamento)."
                    + (chegou ? "" : " O veículo ainda não chegou: registre a entrada quando ele chegar (aba Entrada).");
            case AGUARDANDO_APROVACAO -> "Aguardando a resposta do cliente ao orçamento: registre-a na aba Orçamento.";
            case APROVADA -> chegou ? "Orçamento aprovado: pode começar o serviço."
                    : "Orçamento aprovado. Registre a entrada do veículo (aba Entrada) para começar o serviço.";
            case EM_EXECUCAO -> "Em execução. Quando terminar, marque o serviço como pronto.";
            case AGUARDANDO_PECAS -> {
                String quais = mudancas.isEmpty() ? "" : mudancas.getLast().observacao();
                yield "Aguardando peças" + (quais == null || quais.isBlank() ? "" : ": " + quais)
                        + ". Quando chegarem, retome o serviço.";
            }
            case PRONTA -> "Pronto para retirar. Avise o cliente e registre a entrega.";
            case ENTREGUE -> "Entregue em " + Datas.diaHora(os.entrega().quando()) + ", " + garantia(os) + ".";
            case CANCELADA -> "Cancelada: " + os.motivoCancelamento();
        };
    }

    @FXML
    void comecar() {
        passo(StatusOs.EM_EXECUCAO, "", "Serviço começado.");
    }

    @FXML
    private void aguardarPecas() {
        JanelasOs.motivo(janela(), "Aguardando peças", "Quais peças a OS está esperando? (opcional)",
                "ex.: amortecedor dianteiro, chega na quinta", false).ifPresent(this::aguardarPecas);
    }

    void aguardarPecas(String quais) {
        passo(StatusOs.AGUARDANDO_PECAS, quais, "OS aguardando peças.");
    }

    @FXML
    void retomar() {
        passo(StatusOs.EM_EXECUCAO, "", "Serviço retomado.");
    }

    @FXML
    void pronta() {
        passo(StatusOs.PRONTA, "", "Serviço pronto. Avise o cliente que o veículo pode ser retirado.");
    }

    private void passo(StatusOs destino, String observacao, String sucesso) {
        executar(() -> recursos.oficina().mudarStatus(osId, destino, observacao), sucesso);
    }

    @FXML
    private void entregar() {
        JanelasOs.entrega(janela(), LocalDateTime.now(recursos.relogio()), nautico() ? "Horas de motor" : "Km")
                .ifPresent(this::entregar);
    }

    void entregar(Entrega entrega) {
        executarComAvisos(() -> recursos.oficina().entregar(osId, entrega).avisos(),
                "Veículo entregue. A OS não muda mais; os recebimentos e os anexos continuam valendo.");
    }

    @FXML
    private void cancelarOs() {
        JanelasOs.motivo(janela(), "Cancelar a OS", "Por que a OS " + numero() + " vai ser cancelada? Ela fica no "
                + "histórico, com o motivo, e não muda mais.", "ex.: o cliente desistiu do serviço", true)
                .ifPresent(this::cancelarOs);
    }

    void cancelarOs(String motivo) {
        executar(() -> recursos.oficina().cancelar(osId, motivo), "OS cancelada.");
    }

    @FXML
    private void voltar() {
        recursos.navegacao().voltarParaOficina();
    }

    // ===================== Entrada =====================

    /** Os campos da abertura e da entrada com o que está gravado. */
    private void preencherFormularios() {
        OrdemServicoSalva os = aberta.os();
        servicoPedido.setText(os.servicoPedido());
        caixaMecanico.carregar(os.mecanicoId());
        previsao.setValue(os.previsaoEntrega());
        observacoes.setText(os.observacoes());
        if (os.entrada() == null) {
            formEntradaController.limpar(LocalDateTime.now(recursos.relogio()));
        } else {
            formEntradaController.preencher(os.entrada());
        }
    }

    private void mostrarEntrada() {
        OrdemServicoSalva os = aberta.os();
        boolean emAndamento = !os.status().finalizada();
        for (Node campo : List.of(servicoPedido, mecanico, botaoNovoMecanico, previsao, observacoes,
                botaoSalvarAbertura, botaoSalvarEntrada)) {
            campo.setDisable(!emAndamento);
        }
        formEntradaController.habilitar(emAndamento);
        formEntradaController.usarVeiculo(aberta.veiculo().veiculo());
        situacaoEntrada.setText(os.entrada() == null
                ? "O veículo ainda não chegou. Quando ele chegar, preencha e registre a entrada."
                : "Registrada. Se algo foi anotado errado, corrija e salve.");
        botaoSalvarEntrada.setText(os.entrada() == null ? "Registrar a entrada" : "Salvar a correção da entrada");
    }

    /** Depois de salvar, os campos voltam ao que ficou gravado (ex.: o texto sem espaços sobrando). */
    @FXML
    void salvarAbertura() {
        if (executar(() -> recursos.oficina().editarAbertura(osId, servicoPedido.getText(),
                caixaMecanico.escolhido(), previsao.getValue(), observacoes.getText()), "Abertura salva.")) {
            preencherFormularios();
        }
    }

    @FXML
    void salvarEntrada() {
        List<String> erros = new ArrayList<>();
        EntradaVeiculo entrada = formEntradaController.ler(erros);
        if (!erros.isEmpty()) {
            mostrarErros(erros);
            return;
        }
        boolean primeira = aberta.os().entrada() == null;
        if (executarComAvisos(() -> recursos.oficina().registrarEntrada(osId, entrada).avisos(),
                primeira ? "Entrada registrada." : "Entrada corrigida.")) {
            preencherFormularios();
        }
    }

    @FXML
    private void novoMecanico() {
        caixaMecanico.novo(janela(), this::mostrarErros);
    }

    // ===================== Preparação =====================

    private void mostrarPreparacao() {
        OrdemServicoSalva os = aberta.os();
        boolean comProjeto = os.projetoId() != null;
        mostrar(caixaComProjeto, comProjeto);
        mostrar(caixaSemProjeto, !comProjeto && !os.status().finalizada());
        if (comProjeto) {
            CasosProjeto.ProjetoAberto projeto = recursos.projetos().abrir(os.projetoId());
            VersaoMotor motor = projeto.motor();
            resumoPreparacao.setText("Projeto de preparação #" + os.projetoId() + " · motor v" + motor.numero()
                    + (motor.descricao().isEmpty() ? "" : " " + motor.descricao()) + " · "
                    + projeto.projeto().status().rotulo() + ". O orçamento desta OS traz as peças do cálculo dele.");
            return;
        }
        List<ProjetoDoVeiculo> doVeiculo = recursos.oficina().projetosDoVeiculo(os.veiculoId());
        ProjetoDoVeiculo escolhido = projetosDoVeiculo.getValue();
        projetosDoVeiculo.getItems().setAll(doVeiculo);
        projetosDoVeiculo.setValue(doVeiculo.contains(escolhido) ? escolhido
                : doVeiculo.isEmpty() ? null : doVeiculo.getFirst());
        botaoLigarProjeto.setDisable(doVeiculo.isEmpty());
        resumoPreparacao.setText(os.status().finalizada() ? "Esta OS não teve projeto de preparação."
                : doVeiculo.isEmpty() ? "Esta OS não tem projeto de preparação, e o veículo não tem nenhum projeto."
                        : "Esta OS não tem projeto de preparação.");
    }

    @FXML
    private void ligarProjeto() {
        ProjetoDoVeiculo escolhido = projetosDoVeiculo.getValue();
        if (escolhido != null) {
            executar(() -> recursos.oficina().ligarProjeto(osId, escolhido.projetoId()), "Projeto #"
                    + escolhido.projetoId() + " ligado à OS.");
        }
    }

    @FXML
    private void abrirProjeto() {
        if (aberta.os().projetoId() != null) {
            recursos.navegacao().abrirProjeto(aberta.os().projetoId());
        }
    }

    @FXML
    void criarProjeto() {
        recursos.navegacao().novoProjetoDaOs(osId);
    }

    // ===================== Entrega e pagamento =====================

    private void mostrarEntregaEPagamento() {
        OrdemServicoSalva os = aberta.os();
        situacaoEntrega.setText(switch (os.status()) {
            case ENTREGUE -> "Entregue em " + Datas.diaHora(os.entrega().quando())
                    + (os.entrega().usoSaida() == null ? "" : ", com " + NumeroBr.formatar(os.entrega().usoSaida(), 0)
                            + (nautico() ? " h" : " km") + " na saída")
                    + ", " + garantia(os) + ".";
            case CANCELADA -> "OS cancelada: não há entrega.";
            case PRONTA -> "Pronto para retirar: registre a entrega quando o cliente buscar.";
            default -> "A entrega fica liberada quando o serviço estiver pronto.";
        });
        mostrar(botaoEntregarAba, os.status() == StatusOs.PRONTA);
        SituacaoDoPagamento pagamento = aberta.pagamento();
        valorOs.setText(pagamento.valor() == null ? "sem orçamento aprovado" : pagamento.valor().formatar());
        recebido.setText(pagamento.recebido().formatar());
        falta.setText(pagamento.falta() == null ? "—" : pagamento.falta().formatar() + " ("
                + pagamento.situacao().rotulo() + ")");
        recebimentos.getItems().setAll(pagamento.recebimentos());
        botaoRegistrarRecebimento.setDisable(os.status() == StatusOs.CANCELADA);
    }

    /** "com garantia de 90 dias, até 14/01/2027" ou "sem garantia". */
    private static String garantia(OrdemServicoSalva os) {
        int dias = os.entrega().garantia().dias();
        return dias == 0 ? "sem garantia" : "com garantia de " + dias + (dias == 1 ? " dia" : " dias") + ", até "
                + Datas.dia(os.garantiaAte());
    }

    @FXML
    private void registrarRecebimento() {
        JanelasOs.recebimento(janela(), LocalDateTime.now(recursos.relogio()), aberta.pagamento().falta())
                .ifPresent(this::registrarRecebimento);
    }

    void registrarRecebimento(Pagamento pagamento) {
        executar(() -> recursos.oficina().registrarPagamento(osId, pagamento), "Recebimento registrado.");
    }

    @FXML
    private void cancelarRecebimento() {
        PagamentoSalvo escolhido = recebimentos.getSelectionModel().getSelectedItem();
        if (escolhido == null) {
            return;
        }
        if (escolhido.cancelado()) {
            mostrarErros(List.of("Esse recebimento já foi cancelado."));
            return;
        }
        JanelasOs.motivo(janela(), "Cancelar recebimento", "Por que cancelar o recebimento de "
                + escolhido.pagamento().valor().formatar() + "? Ele fica na lista, com o motivo, e deixa de contar.",
                "ex.: lançado duas vezes", true).ifPresent(motivo -> cancelarRecebimento(escolhido.id(), motivo));
    }

    void cancelarRecebimento(long pagamentoId, String motivo) {
        executar(() -> recursos.oficina().cancelarPagamento(pagamentoId, motivo), "Recebimento cancelado.");
    }

    // ===================== Anexos =====================

    private void mostrarAnexos() {
        listaAnexos.getItems().setAll(recursos.oficina().anexosDaOs(osId));
        mostrarBotoesAnexo();
    }

    /** Remover só com a OS em andamento: o que ficou registrado na entrega não some. */
    private void mostrarBotoesAnexo() {
        boolean escolhido = listaAnexos.getSelectionModel().getSelectedItem() != null;
        botaoRemoverAnexo.setDisable(!escolhido || aberta == null || aberta.os().status().finalizada());
    }

    @FXML
    private void adicionarAnexos() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Anexar à " + numero());
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fotos (JPG ou PNG) e PDF", "*.jpg",
                "*.jpeg", "*.png", "*.pdf"));
        List<File> arquivos = escolha.showOpenMultipleDialog(janela());
        if (arquivos != null) {
            adicionarAnexos(arquivos.stream().map(File::toPath).toList());
        }
    }

    /**
     * Anexa os arquivos (do botão ou arrastados do Windows Explorer). Foto vira foto da entrada antes de o serviço
     * começar, e foto do serviço depois; PDF vira documento. O tipo e a descrição mudam em "Editar…".
     */
    void adicionarAnexos(List<Path> arquivos) {
        StatusOs atual = aberta.os().status();
        boolean antesDoServico = atual.antesDaAprovacao() || atual == StatusOs.APROVADA;
        List<NovoAnexo> novos = arquivos.stream().map(arquivo -> {
            TipoAnexo tipo = !AnexosDaOficina.imagem(arquivo.getFileName().toString()) ? TipoAnexo.DOCUMENTO
                    : antesDoServico ? TipoAnexo.FOTO_ENTRADA : TipoAnexo.FOTO_SERVICO;
            return new NovoAnexo(arquivo, tipo, "", null, tipo.pessoalPorPadrao());
        }).toList();
        executar(() -> recursos.oficina().anexar(osId, novos), (novos.size() == 1 ? "Arquivo anexado."
                : novos.size() + " arquivos anexados.") + " Para mudar o tipo ou pôr uma descrição, use Editar.");
    }

    @FXML
    private void abrirAnexo() {
        AnexoSalvo anexo = listaAnexos.getSelectionModel().getSelectedItem();
        if (anexo == null) {
            return;
        }
        Optional<Path> arquivo = recursos.oficina().arquivoDoAnexo(anexo);
        if (arquivo.isEmpty()) {
            mostrarErros(List.of("O arquivo de " + anexo.nomeOriginal() + " não está na pasta de anexos (foi apagado "
                    + "fora do Calibra?). Restaure um backup ou anexe de novo."));
        } else if (recursos.servicos() != null) {
            recursos.servicos().showDocument(arquivo.get().toUri().toString());
        }
    }

    @FXML
    private void editarAnexo() {
        AnexoSalvo anexo = listaAnexos.getSelectionModel().getSelectedItem();
        if (anexo != null) {
            JanelasOs.anexo(janela(), anexo).ifPresent(dados -> executar(() -> recursos.oficina().editarAnexo(
                    anexo.id(), dados.tipo(), dados.descricao(), dados.validade(), dados.pessoal()), "Anexo alterado."));
        }
    }

    @FXML
    private void removerAnexo() {
        AnexoSalvo anexo = listaAnexos.getSelectionModel().getSelectedItem();
        if (anexo == null) {
            return;
        }
        Alert alerta = Alertas.criar(Alert.AlertType.CONFIRMATION, anexo.tipo().rotulo() + ": "
                + anexo.nomeOriginal() + ". O arquivo sai da pasta de anexos se nenhum outro anexo usar o mesmo.",
                ButtonType.YES, ButtonType.NO);
        alerta.initOwner(janela());
        alerta.setTitle("Remover anexo");
        alerta.setHeaderText("Remover o anexo da OS?");
        if (alerta.showAndWait().filter(ButtonType.YES::equals).isPresent()) {
            removerAnexo(anexo.id());
        }
    }

    void removerAnexo(long anexoId) {
        executar(() -> recursos.oficina().removerAnexo(anexoId), "Anexo removido.");
        miniaturas.remove(anexoId);
    }

    /** Uma linha da lista de anexos: a miniatura (ou "PDF") e o tipo, a descrição, o arquivo e a validade. */
    private final class CelulaAnexo extends ListCell<AnexoSalvo> {

        private final ImageView imagem = new ImageView();
        private final Label documento = new Label();
        private final StackPane quadro = new StackPane();
        private final Label nome = new Label();
        private final Label detalhes = new Label();
        private final HBox linha;

        CelulaAnexo() {
            quadro.setMinSize(LARGURA_MINIATURA, ALTURA_MINIATURA);
            quadro.setPrefSize(LARGURA_MINIATURA, ALTURA_MINIATURA);
            quadro.getStyleClass().add("miniatura");
            documento.getStyleClass().add("negrito");
            nome.getStyleClass().add("negrito");
            detalhes.getStyleClass().add("dica");
            linha = new HBox(12.0, quadro, new VBox(4.0, nome, detalhes));
            linha.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(AnexoSalvo anexo, boolean vazio) {
            super.updateItem(anexo, vazio);
            setText(null);
            if (vazio || anexo == null) {
                setGraphic(null);
                return;
            }
            Optional<Path> arquivo = recursos.oficina().arquivoDoAnexo(anexo);
            if (arquivo.isPresent() && AnexosDaOficina.imagem(anexo.arquivo())) {
                // A imagem carrega em segundo plano, já no tamanho da miniatura: a lista não trava com fotos grandes.
                imagem.setImage(miniaturas.computeIfAbsent(anexo.id(), id -> new Image(arquivo.get().toUri()
                        .toString(), LARGURA_MINIATURA, ALTURA_MINIATURA, true, true, true)));
                quadro.getChildren().setAll(imagem);
            } else {
                documento.setText(arquivo.isPresent() ? "PDF" : "arquivo\nnão achado");
                quadro.getChildren().setAll(documento);
            }
            nome.setText(anexo.tipo().rotulo() + (anexo.descricao().isBlank() ? "" : " · " + anexo.descricao()));
            detalhes.setText(anexo.nomeOriginal() + " · " + NumeroBr.tamanhoArquivo(anexo.tamanhoBytes()) + " · "
                    + Datas.diaHora(anexo.criadoEm()) + (anexo.validade() == null ? ""
                            : " · válido até " + Datas.dia(anexo.validade()))
                    + (anexo.pessoal() ? " · documento pessoal" : ""));
            setGraphic(linha);
        }
    }

    // ===================== Para os testes =====================

    /** A aba Orçamento. */
    OrcamentoController orcamento() {
        return orcamento;
    }

    /** As mensagens da última ação. */
    List<String> mensagens() {
        return mensagens.getChildren().stream().map(no -> ((Label) no).getText()).toList();
    }

    // ===================== Utilidades =====================

    /**
     * Faz a ação, lê tudo de novo e mostra o resultado; com erro, a mensagem.
     *
     * @return se a ação deu certo
     */
    private boolean executar(Runnable acao, String sucesso) {
        return executarComAvisos(() -> {
            acao.run();
            return List.of();
        }, sucesso);
    }

    /** Como {@link #executar}, para a ação que devolve avisos (gravou, mas vale conferir). */
    private boolean executarComAvisos(Supplier<List<String>> acao, String sucesso) {
        List<String> avisos;
        try {
            avisos = acao.get();
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
            return false;
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
            return false;
        }
        atualizar();
        mensagens.getChildren().clear();
        mensagem(sucesso, "ok");
        avisos.forEach(aviso -> mensagem(aviso, "aviso-amarelo"));
        return true;
    }

    /** Embarcação: o uso é em horas de motor, não em km. */
    private boolean nautico() {
        Veiculo veiculo = aberta.veiculo().veiculo();
        return veiculo.tipo() != null && veiculo.tipo().nautico();
    }

    private void mostrarErros(List<String> erros) {
        mensagens.getChildren().clear();
        erros.forEach(erro -> mensagem("• " + erro, "erro"));
    }

    private void mensagem(String texto, String estilo) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add(estilo);
        rotulo.setWrapText(true);
        mensagens.getChildren().add(rotulo);
    }

    private static void mostrar(Node no, boolean visivel) {
        no.setVisible(visivel);
        no.setManaged(visivel);
    }

    private static <T> void coluna(TableColumn<T, String> coluna, Function<T, String> texto) {
        coluna.setCellValueFactory(celula -> new ReadOnlyStringWrapper(texto.apply(celula.getValue())));
    }

    private Window janela() {
        return titulo.getScene() == null ? null : titulo.getScene().getWindow();
    }
}
