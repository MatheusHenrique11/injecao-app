package br.com.calibra.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.app.casos.AnexosDaOficina;
import br.com.calibra.app.casos.CasosLaudo;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.app.casos.CasosOficina.NovoAnexo;
import br.com.calibra.app.casos.CasosOficina.OsAberta;
import br.com.calibra.app.casos.CasosOficina.Pendencia;
import br.com.calibra.app.casos.CasosOficina.ProjetoDoVeiculo;
import br.com.calibra.app.casos.CasosOficina.SituacaoDoPagamento;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.LogoOficina;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.GarantiaDoServico;
import br.com.calibra.core.oficina.PadroesOs;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.oficina.AnexoSalvo;
import br.com.calibra.dados.oficina.MecanicoSalvo;
import br.com.calibra.dados.oficina.MudancaStatusOs;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.dados.oficina.PagamentoSalvo;
import br.com.calibra.dados.oficina.LaudoSalvo;
import br.com.calibra.dados.projeto.VersaoMotor;
import br.com.calibra.relatorios.DadosLaudo;
import br.com.calibra.relatorios.DadosOs;
import br.com.calibra.relatorios.RelatorioLaudo;
import br.com.calibra.relatorios.RelatorioOs;
import br.com.calibra.relatorios.TextoWhatsAppOs;

/**
 * Tela da OS (SPEC 8.9): o cabeçalho com o cliente, o veículo, o status e os passos do serviço (começar, aguardar
 * peças, retomar, pronto, entregar, cancelar) e as abas Entrada, Orçamento, Preparação, Execução (Fase 3B), Entrega e
 * pagamento, Anexos e Histórico. Depois de cada ação, tudo é lido de novo do banco.
 */
public class OsController {

    /**
     * O que a tela usa do resto do app.
     *
     * @param padroes os padrões de Configurações › OS, lidos na hora (itens da vistoria, garantia e texto da via)
     * @param temporaria onde o PDF de uma impressão fica enquanto as páginas são desenhadas
     * @param tarefas a fila de segundo plano do app (a cópia dos anexos)
     */
    record Recursos(CasosOficina oficina, CasosLaudo laudos, CasosProjeto projetos,
            OrcamentoController.Recursos orcamento, HostServices servicos, Navegacao navegacao, Clock relogio,
            Supplier<PadroesOs> padroes, Path temporaria, TarefasFundo tarefas) {
    }

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
    @FXML private MenuButton menuDocumentos;
    @FXML private MenuItem itemViaPdf;
    @FXML private MenuItem itemViaImprimir;
    @FXML private MenuItem itemComprovantePdf;
    @FXML private MenuItem itemComprovanteImprimir;
    @FXML private MenuItem itemTextoAberta;
    @FXML private MenuItem itemTextoPronta;
    @FXML private MenuItem itemLaudoPdf;
    @FXML private MenuItem itemLaudoImprimir;
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
    @FXML private Tab abaPreparacao;
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
    @FXML private AnexosController anexosController;
    // ---------- Histórico ----------
    @FXML private TableView<MudancaStatusOs> historico;
    @FXML private TableColumn<MudancaStatusOs, String> colunaDataHora;
    @FXML private TableColumn<MudancaStatusOs, String> colunaStatus;
    @FXML private TableColumn<MudancaStatusOs, String> colunaHistoricoObservacao;
    @FXML private VBox mensagens;

    private Recursos recursos;
    private CaixaMecanico caixaMecanico;
    private OrcamentoController orcamento;
    private ExecucaoController execucao;
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
    }

    void iniciar(Recursos recursos, long osId) {
        this.recursos = recursos;
        this.osId = osId;
        caixaMecanico = new CaixaMecanico(mecanico, recursos.oficina());
        anexosController.iniciar(recursos.oficina(), recursos.servicos(), recursos.tarefas(), new AnexosDaOs(),
                this::mostrarOk,
                this::mostrarErros);
        atualizar();
        preencherFormularios();
        montarOrcamento();
        montarExecucao();
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
     * A aba Execução (Fase 3B), depois da Preparação: os checklists dos serviços. "Quem está marcando" já vem com o
     * mecânico da OS, se ele estiver ativo.
     */
    private void montarExecucao() {
        FXMLLoader carregador = new FXMLLoader(OsController.class.getResource("execucao.fxml"));
        Parent tela;
        try {
            tela = carregador.load();
        } catch (IOException erro) {
            throw new UncheckedIOException("Tela do app não abriu: execucao.fxml", erro);
        }
        Tab aba = new Tab("Execução", tela);
        aba.setId("abaExecucao");
        abas.getTabs().add(abas.getTabs().indexOf(abaPreparacao) + 1, aba);
        execucao = carregador.getController();
        MecanicoSalvo mecanicoDaOs = aberta.mecanico();
        execucao.iniciar(recursos.oficina(), osId, this::executarComMensagem,
                mecanicoDaOs != null && mecanicoDaOs.ativo() ? mecanicoDaOs.id() : null);
        execucao.laudos().iniciar(new LaudosController.Recursos(recursos.laudos(), recursos.oficina(), osId,
                recursos.servicos(), recursos.temporaria(), this::documentoDoLaudo, new TelaDosLaudos()));
        execucao.atualizar(aberta.os());
        mostrarMenuDosLaudos();
    }

    /** O que a área dos laudos pede: ler tudo de novo e mostrar o resultado embaixo. */
    private final class TelaDosLaudos implements LaudosController.Tela {

        @Override
        public void gravado(String mensagem, List<String> avisos) {
            atualizar();
            mensagens.getChildren().clear();
            mensagem(mensagem, "ok");
            avisos.forEach(aviso -> mensagem(aviso, "aviso-amarelo"));
        }

        @Override
        public void mostrarOk(String mensagem) {
            OsController.this.mostrarOk(mensagem);
        }

        @Override
        public void mostrarErros(List<String> erros) {
            OsController.this.mostrarErros(erros);
        }
    }

    /** Os dados do PDF do laudo, com a oficina e o logo de Configurações, lidos na hora. */
    private DadosLaudo documentoDoLaudo(long laudoId) {
        DadosOficina oficina = recursos.orcamento().oficina().get();
        Path logo = LogoOficina.arquivo(oficina, recursos.orcamento().anexos()).orElse(null);
        return recursos.laudos().documento(laudoId, oficina, logo, recursos.orcamento().versaoApp());
    }

    /** O laudo no menu Documentos: desligado sem laudo. */
    private void mostrarMenuDosLaudos() {
        boolean semLaudo = execucao == null || !execucao.laudos().temLaudo();
        itemLaudoPdf.setDisable(semLaudo);
        itemLaudoImprimir.setDisable(semLaudo);
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
        anexosController.atualizar();
        historico.getItems().setAll(mudancas.reversed());
        if (orcamento != null) {
            orcamento.atualizar();
        }
        if (execucao != null) {
            execucao.atualizar(aberta.os());
            mostrarMenuDosLaudos();
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

        // A via não sai da OS cancelada; o comprovante, só depois da entrega; cada texto, no momento dele.
        itemViaPdf.setDisable(atual == StatusOs.CANCELADA);
        itemViaImprimir.setDisable(atual == StatusOs.CANCELADA);
        itemComprovantePdf.setDisable(atual != StatusOs.ENTREGUE);
        itemComprovanteImprimir.setDisable(atual != StatusOs.ENTREGUE);
        itemTextoAberta.setDisable(atual.finalizada());
        itemTextoPronta.setDisable(atual != StatusOs.PRONTA);
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
        if (passo(StatusOs.EM_EXECUCAO, "", "Serviço começado.") && execucao.temChecklist()) {
            mensagem("Os checklists dos serviços estão na aba Execução.", "ok");
        }
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

    /**
     * O serviço pronto. Com item obrigatório do checklist pendente, a janela mostra o que falta e pede a justificativa
     * (SPEC 6.5, Fase 3B); com marcações não salvas na aba Execução, avisa antes.
     */
    @FXML
    void pronta() {
        if (!tudoSalvo(Momento.PRONTO)) {
            return;
        }
        List<Pendencia> pendencias = recursos.oficina().pendencias(osId);
        if (pendencias.isEmpty()) {
            passo(StatusOs.PRONTA, "", "Serviço pronto. Avise o cliente que o veículo pode ser retirado.");
        } else {
            JanelasOs.prontaComPendencias(janela(), pendencias.stream().map(Pendencia::texto).toList())
                    .ifPresent(this::pronta);
        }
    }

    /** O "pronto" com itens obrigatórios pendentes: a justificativa fica no histórico da OS, com a data e a hora. */
    void pronta(String justificativa) {
        if (tudoSalvo(Momento.PRONTO)) {
            passo(StatusOs.PRONTA, justificativa, "Serviço pronto; o que faltou no checklist e a justificativa ficaram "
                    + "no histórico da OS. Avise o cliente que o veículo pode ser retirado.");
        }
    }

    private boolean passo(StatusOs destino, String observacao, String sucesso) {
        return executar(() -> recursos.oficina().mudarStatus(osId, destino, observacao), sucesso);
    }

    @FXML
    private void entregar() {
        if (tudoSalvo(Momento.ENTREGA)) {
            JanelasOs.entrega(janela(), LocalDateTime.now(recursos.relogio()), nautico() ? "Horas de motor" : "Km",
                    recursos.padroes().get().garantiaDias(), aberta.garantias()).ifPresent(this::entregar);
        }
    }

    /** A entrega; com a aba Entrada mudada e não salva, avisa e não entrega (depois, a OS não muda mais). */
    void entregar(Entrega entrega) {
        if (tudoSalvo(Momento.ENTREGA) && executarComAvisos(() -> recursos.oficina().entregar(osId, entrega)
                .avisos(), "Veículo entregue. A OS não muda mais; os recebimentos e os anexos continuam valendo.")) {
            preencherFormularios();
        }
    }

    @FXML
    private void cancelarOs() {
        if (!tudoSalvo(Momento.CANCELAMENTO)) {
            return;
        }
        JanelasOs.motivo(janela(), "Cancelar a OS", "Por que a OS " + numero() + " vai ser cancelada? Ela fica no "
                + "histórico, com o motivo, e não muda mais.", "ex.: o cliente desistiu do serviço", true)
                .ifPresent(this::cancelarOs);
    }

    void cancelarOs(String motivo) {
        if (tudoSalvo(Momento.CANCELAMENTO) && executar(() -> recursos.oficina().cancelar(osId, motivo),
                "OS cancelada.")) {
            preencherFormularios();
        }
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
            formEntradaController.limpar(LocalDateTime.now(recursos.relogio()), recursos.padroes().get()
                    .itensVistoria());
        } else {
            formEntradaController.preencher(os.entrada(), recursos.padroes().get().itensVistoria());
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
                    + ", " + garantia(os) + "." + garantiasProprias(os, aberta.garantias());
            case CANCELADA -> "OS cancelada: não há entrega.";
            case PRONTA -> "Pronto para retirar: registre a entrega quando o cliente buscar.";
            default -> "A entrega fica liberada quando o serviço estiver pronto.";
        });
        mostrar(botaoEntregarAba, os.status() == StatusOs.PRONTA);
        SituacaoDoPagamento pagamento = aberta.pagamento();
        valorOs.setText(pagamento.valor() == null ? "sem orçamento aprovado" : pagamento.valor().formatar());
        recebido.setText(pagamento.recebido().formatar());
        Dinheiro aMais = pagamento.aMais();
        falta.setText(pagamento.falta() == null ? "—" : pagamento.falta().formatar() + " ("
                + pagamento.situacao().rotulo() + ")" + (aMais.centavos() > 0 ? " · recebido a mais: "
                        + aMais.formatar() + " (confira se nenhum recebimento foi lançado duas vezes)" : ""));
        recebimentos.getItems().setAll(pagamento.recebimentos());
        botaoRegistrarRecebimento.setDisable(os.status() == StatusOs.CANCELADA);
    }

    /** "com garantia de 90 dias, até 14/01/2027" ou "sem garantia". */
    private static String garantia(OrdemServicoSalva os) {
        int dias = os.entrega().garantia().dias();
        return dias == 0 ? "sem garantia" : "com garantia de " + dias + (dias == 1 ? " dia" : " dias") + ", até "
                + Datas.dia(os.garantiaAte());
    }

    /**
     * Os serviços com garantia própria (do catálogo), com o dia em que ela vence: " Garantia própria: Reparo ou troca
     * de alternador, 365 dias, até 14/10/2027." Vazio se nenhum tiver; os outros ficam com a garantia da OS.
     */
    static String garantiasProprias(OrdemServicoSalva os, List<GarantiaDoServico> garantias) {
        LocalDate entrega = os.entrega().quando().toLocalDate();
        String proprias = garantias.stream().filter(GarantiaDoServico::propria)
                .map(servico -> servico.servico() + ", " + JanelasOs.diasDeGarantia(servico.diasProprios())
                        + (servico.diasProprios() == 0 ? ""
                                : ", até " + Datas.dia(servico.garantia(os.entrega().garantia()).vence(entrega))))
                .collect(Collectors.joining("; "));
        return proprias.isEmpty() ? "" : " Garantia própria: " + proprias + ".";
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

    // ===================== Anexos e ficha do veículo =====================

    /** Os anexos da OS: a foto vira foto da entrada antes de o serviço começar, e foto do serviço depois. */
    private final class AnexosDaOs implements AnexosController.Fonte {

        @Override
        public List<AnexoSalvo> anexos() {
            return recursos.oficina().anexosDaOs(osId);
        }

        /** PDF vira documento. O tipo e a descrição mudam em "Editar…". */
        @Override
        public Callable<String> anexar(List<Path> arquivos) {
            StatusOs atual = aberta.os().status();
            boolean antesDoServico = atual.antesDaAprovacao() || atual == StatusOs.APROVADA;
            List<NovoAnexo> novos = arquivos.stream().map(arquivo -> {
                TipoAnexo tipo = !AnexosDaOficina.imagem(arquivo.getFileName().toString()) ? TipoAnexo.DOCUMENTO
                        : antesDoServico ? TipoAnexo.FOTO_ENTRADA : TipoAnexo.FOTO_SERVICO;
                return new NovoAnexo(arquivo, tipo, "", null, tipo.pessoalPorPadrao());
            }).toList();
            CasosOficina oficina = recursos.oficina();
            long os = osId;
            return () -> {
                oficina.anexar(os, novos);
                return (novos.size() == 1 ? "Arquivo anexado." : novos.size() + " arquivos anexados.")
                        + " Para mudar o tipo ou pôr uma descrição, use Editar.";
            };
        }

        /** Remover só com a OS em andamento: o que ficou registrado na entrega não some. */
        @Override
        public boolean podeRemover(AnexoSalvo anexo) {
            return aberta != null && !aberta.os().status().finalizada();
        }

        @Override
        public String tituloEscolha() {
            return "Anexar à " + numero();
        }
    }

    /** Anexa os arquivos à OS (do botão ou arrastados do Windows Explorer). */
    void adicionarAnexos(List<Path> arquivos) {
        anexosController.adicionarAnexos(arquivos);
    }

    void removerAnexo(long anexoId) {
        anexosController.removerAnexo(anexoId);
    }

    /** A ficha do veículo da OS; o botão de voltar dela traz de volta a esta OS. */
    @FXML
    private void abrirFichaDoVeiculo() {
        recursos.navegacao().abrirFichaDoVeiculo(aberta.os().veiculoId(), numero(),
                () -> recursos.navegacao().abrirOs(osId));
    }

    // ===================== Documentos =====================

    /** Um documento da OS em PDF: a via do cliente ou o comprovante de entrega. */
    @FunctionalInterface
    private interface DocumentoOs {
        void escrever(DadosOs dados, Path destino) throws IOException;
    }

    @FXML
    private void viaPdf() {
        if (tudoSalvo(Momento.DOCUMENTO)) {
            gerarPdf("Via do cliente", "via-do-cliente", RelatorioOs::viaDoCliente);
        }
    }

    @FXML
    private void viaImprimir() {
        if (tudoSalvo(Momento.DOCUMENTO)) {
            imprimir("Via do cliente", RelatorioOs::viaDoCliente);
        }
    }

    @FXML
    private void comprovantePdf() {
        gerarPdf("Comprovante de entrega", "comprovante-de-entrega", RelatorioOs::comprovanteDeEntrega);
    }

    @FXML
    private void comprovanteImprimir() {
        imprimir("Comprovante de entrega", RelatorioOs::comprovanteDeEntrega);
    }

    @FXML
    private void copiarTextoAberta() {
        if (tudoSalvo(Momento.DOCUMENTO)) {
            copiar(TextoWhatsAppOs.osAberta(documento()));
        }
    }

    @FXML
    private void copiarTextoPronta() {
        copiar(TextoWhatsAppOs.prontaParaRetirar(documento()));
    }

    @FXML
    private void laudoPdf() {
        escolherLaudo().ifPresent(execucao.laudos()::pdf);
    }

    @FXML
    private void laudoImprimir() {
        escolherLaudo().ifPresent(execucao.laudos()::imprimir);
    }

    /** O laudo do menu Documentos: o único da OS, ou o escolhido numa lista (o mais novo já vem marcado). */
    private Optional<LaudoSalvo> escolherLaudo() {
        List<LaudoSalvo> laudos = execucao.laudos().laudos();
        if (laudos.size() <= 1) {
            return laudos.stream().findFirst();
        }
        List<String> rotulos = new ArrayList<>();
        for (int i = 0; i < laudos.size(); i++) {
            LaudoSalvo laudo = laudos.get(i);
            rotulos.add((i + 1) + ". " + Datas.diaHora(laudo.laudo().dataHora()) + " · "
                    + LaudosController.resumo(laudo.laudo().queixa()));
        }
        ChoiceDialog<String> escolha = new ChoiceDialog<>(rotulos.getLast(), rotulos);
        escolha.initOwner(janela());
        escolha.setTitle("Laudo elétrico");
        escolha.setHeaderText("A OS " + numero() + " tem " + laudos.size() + " laudos. Qual?");
        escolha.setContentText("Laudo:");
        escolha.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        return escolha.showAndWait().map(rotulos::indexOf).filter(indice -> indice >= 0).map(laudos::get);
    }

    private void gerarPdf(String titulo, String nome, DocumentoOs documento) {
        DadosOs dados = documento();
        ArquivoPdf.gerar(janela(), recursos.servicos(), titulo, numero() + "-" + nome + ".pdf",
                destino -> documento.escrever(dados, destino), this::mostrarOk, () -> { });
    }

    private void imprimir(String titulo, DocumentoOs documento) {
        DadosOs dados = documento();
        Impressao.imprimir(janela(), titulo + " " + numero(), recursos.temporaria(),
                destino -> documento.escrever(dados, destino), this::mostrarOk);
    }

    private void copiar(String texto) {
        if (AreaDeTransferencia.copiar(texto)) {
            mostrarOk(AreaDeTransferencia.COPIADO);
        } else {
            mostrarErros(List.of(AreaDeTransferencia.NAO_COPIOU));
        }
    }

    /** Os dados dos documentos, lidos na hora: a oficina e o texto da via de Configurações e a OS como está gravada. */
    private DadosOs documento() {
        DadosOficina oficina = recursos.orcamento().oficina().get();
        Path logo = LogoOficina.arquivo(oficina, recursos.orcamento().anexos()).orElse(null);
        return recursos.oficina().documento(osId, oficina, logo, recursos.orcamento().versaoApp(),
                recursos.padroes().get().textoVia());
    }

    /**
     * Quando a aba Entrada e as marcações do checklist (aba Execução) precisam estar salvas, com o começo e o fim do
     * aviso.
     */
    private enum Momento {
        DOCUMENTO("Antes do documento", "O documento usa o que está gravado.", true, false),
        PRONTO("Antes do pronto", "A trava do pronto confere as marcações gravadas.", false, true),
        ENTREGA("Antes de entregar", "Depois da entrega, a OS não muda mais.", true, true),
        CANCELAMENTO("Antes de cancelar", "Depois do cancelamento, a OS não muda mais.", true, true);

        private final String antes;
        private final String porque;
        private final boolean entrada;
        private final boolean marcacoes;

        Momento(String antes, String porque, boolean entrada, boolean marcacoes) {
            this.antes = antes;
            this.porque = porque;
            this.entrada = entrada;
            this.marcacoes = marcacoes;
        }
    }

    /**
     * Os documentos usam o que está gravado: com a abertura ou a entrada mudadas na tela e não salvas, a via sairia
     * sem elas. Na entrega e no cancelamento, a mudança se perderia (depois, a OS não muda mais); no pronto, a trava
     * olha as marcações gravadas. Nesses casos, avisa o que salvar e não continua.
     */
    private boolean tudoSalvo(Momento momento) {
        List<String> pendentes = momento.entrada ? pendentes() : List.of();
        if (!pendentes.isEmpty()) {
            mostrarErros(List.of(momento.antes + ", salve o que mudou na aba Entrada: " + String.join(" e ", pendentes)
                    + ". " + momento.porque));
            return false;
        }
        if (momento.marcacoes && marcacoesPendentes()) {
            mostrarErros(List.of(momento.antes + ", salve as marcações do checklist (aba Execução, Salvar "
                    + "marcações). " + momento.porque));
            return false;
        }
        return true;
    }

    /** Marcações do checklist mudadas na aba Execução e não salvas (com a OS finalizada, não há o que salvar). */
    boolean marcacoesPendentes() {
        return execucao != null && !aberta.os().status().finalizada() && execucao.mudou();
    }

    /**
     * O que mudou na aba Entrada e não foi salvo, com o botão que salva; vazio = a tela está igual ao gravado. Com a OS
     * entregue ou cancelada, vazio: não há mais o que salvar, e os documentos usam o gravado.
     */
    List<String> pendentes() {
        OrdemServicoSalva os = aberta.os();
        if (os.status().finalizada()) {
            return List.of();
        }
        List<String> pendentes = new ArrayList<>();
        if (!servicoPedido.getText().strip().equals(os.servicoPedido())
                || !Objects.equals(caixaMecanico.escolhido(), os.mecanicoId())
                || !Objects.equals(previsao.getValue(), os.previsaoEntrega())
                || !observacoes.getText().strip().equals(os.observacoes())) {
            pendentes.add("a abertura (Salvar abertura)");
        }
        if (formEntradaController.diferenteDe(os.entrada())) {
            pendentes.add("a entrada (" + botaoSalvarEntrada.getText() + ")");
        }
        return pendentes;
    }

    // ===================== Para os testes =====================

    /** A aba Orçamento. */
    OrcamentoController orcamento() {
        return orcamento;
    }

    /** A aba Execução. */
    ExecucaoController execucao() {
        return execucao;
    }

    /** O menu Documentos do cabeçalho. */
    MenuButton menuDocumentos() {
        return menuDocumentos;
    }

    /** A via do cliente em PDF, sem perguntar onde salvar (para os testes e a evidência). */
    void escreverViaDoCliente(Path destino) throws IOException {
        RelatorioOs.viaDoCliente(documento(), destino);
    }

    /** O comprovante de entrega em PDF, sem perguntar onde salvar (para os testes e a evidência). */
    void escreverComprovante(Path destino) throws IOException {
        RelatorioOs.comprovanteDeEntrega(documento(), destino);
    }

    /** O PDF do laudo, sem perguntar onde salvar (para os testes e a evidência). */
    void escreverLaudo(long laudoId, Path destino) throws IOException {
        RelatorioLaudo.gerar(documentoDoLaudo(laudoId), destino);
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

    /** Para as abas: faz a ação, lê tudo de novo e mostra a mensagem que ela devolveu; com erro, a mensagem do erro. */
    private boolean executarComMensagem(Supplier<String> acao) {
        String sucesso;
        try {
            sucesso = acao.get();
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
            return false;
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
            return false;
        }
        atualizar();
        mostrarOk(sucesso);
        return true;
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

    private void mostrarOk(String texto) {
        mensagens.getChildren().clear();
        mensagem(texto, "ok");
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
