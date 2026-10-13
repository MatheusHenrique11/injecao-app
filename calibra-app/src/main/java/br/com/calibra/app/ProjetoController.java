package br.com.calibra.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.CasosProjeto.ProjetoAberto;
import br.com.calibra.app.casos.CasosProjeto.SituacaoCalculo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.dimensionamento.SelecaoInjecao;
import br.com.calibra.core.motor.ComparacaoMotor;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.Validacao;
import br.com.calibra.core.motor.ValidadorMotor;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Recurso;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.PontoMedicao;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.cadastro.EventoAuditoria;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.projeto.CalculoSalvo;
import br.com.calibra.dados.projeto.MudancaStatus;
import br.com.calibra.dados.projeto.ResumoCalculo;
import br.com.calibra.dados.projeto.VersaoMotor;
import br.com.calibra.relatorios.DadosRelatorio;
import br.com.calibra.relatorios.IdentificacaoProjeto;

/**
 * Projeto (SPEC 8.3) em abas: Resumo, Veículo (editável, com histórico), Motor (versões e comparação) e
 * Dimensionamento (a calculadora com o motor do projeto, salvar cálculo e histórico).
 */
public class ProjetoController {

    // ---------- Cabeçalho ----------
    @FXML private Label titulo;
    @FXML private Label subtitulo;
    @FXML private Label status;
    @FXML private Button botaoArquivar;
    @FXML private TabPane abas;
    @FXML private Tab abaDimensionamento;

    // ---------- Resumo ----------
    @FXML private Label clienteNome;
    @FXML private Label clienteContato;
    @FXML private Label clienteConsentimento;
    @FXML private Button botaoEditarCliente;
    @FXML private Button botaoAnonimizar;
    @FXML private Label resumoVeiculo;
    @FXML private Label resumoMotor;
    @FXML private Label resumoMeta;
    @FXML private Label resumoInjecao;
    @FXML private Button botaoTirarInjecao;
    @FXML private Label resumoCalculo;
    @FXML private ListView<MudancaStatus> historicoStatus;
    @FXML private TextArea observacoes;
    @FXML private Label mensagemObservacoes;

    // ---------- Veículo ----------
    @FXML private VeiculoFormController formVeiculoController;
    @FXML private VBox mensagensVeiculo;
    @FXML private TableView<EventoAuditoria> historicoVeiculo;
    @FXML private TableColumn<EventoAuditoria, String> colunaVeiculoData;
    @FXML private TableColumn<EventoAuditoria, String> colunaVeiculoCampo;
    @FXML private TableColumn<EventoAuditoria, String> colunaVeiculoAntes;
    @FXML private TableColumn<EventoAuditoria, String> colunaVeiculoDepois;

    // ---------- Motor ----------
    @FXML private TextField descricaoMotor;
    @FXML private Label versaoAtual;
    @FXML private MotorFormController formMotorController;
    @FXML private VBox mensagensMotorProjeto;
    @FXML private TableView<VersaoMotor> versoes;
    @FXML private TableColumn<VersaoMotor, String> colunaVersao;
    @FXML private TableColumn<VersaoMotor, String> colunaVersaoData;
    @FXML private TableColumn<VersaoMotor, String> colunaVersaoDescricao;
    @FXML private TableColumn<VersaoMotor, String> colunaVersaoMudancas;
    @FXML private Button botaoPontoDePartida;
    @FXML private ComboBox<VersaoMotor> compararA;
    @FXML private ComboBox<VersaoMotor> compararB;
    @FXML private TableView<ComparacaoMotor.Diferenca> comparacao;
    @FXML private Label comparacaoVazia;
    @FXML private TableColumn<ComparacaoMotor.Diferenca, String> colunaComparacaoCampo;
    @FXML private TableColumn<ComparacaoMotor.Diferenca, String> colunaComparacaoA;
    @FXML private TableColumn<ComparacaoMotor.Diferenca, String> colunaComparacaoB;

    // ---------- Dimensionamento ----------
    @FXML private BorderPane painelDimensionamento;
    private CalculadoraController calculadora;
    private final Label faixaCalculo = new Label();
    private final Button botaoSalvarCalculo = new Button("_Salvar cálculo no projeto");
    private final TableView<SituacaoCalculo> calculos = new TableView<>();
    private Tab abaCalculosSalvos;

    private CasosProjeto casos;
    private CasosCatalogo catalogo;
    private Navegacao navegacao;
    private HostServices servicos;
    private Parametros padroes;
    private long projetoId;
    private ProjetoAberto aberto;

    @FXML
    private void initialize() {
        coluna(colunaVeiculoData, e -> Datas.diaHora(e.dataHora()));
        coluna(colunaVeiculoCampo, EventoAuditoria::campo);
        coluna(colunaVeiculoAntes, e -> valor(e.valorAntigo()));
        coluna(colunaVeiculoDepois, e -> valor(e.valorNovo()));
        coluna(colunaVersao, v -> "v" + v.numero());
        coluna(colunaVersaoData, v -> Datas.diaHora(v.criadaEm()));
        coluna(colunaVersaoDescricao, VersaoMotor::descricao);
        coluna(colunaVersaoMudancas, this::mudancasDaVersao);
        coluna(colunaComparacaoCampo, ComparacaoMotor.Diferenca::campo);
        coluna(colunaComparacaoA, ComparacaoMotor.Diferenca::antes);
        coluna(colunaComparacaoB, ComparacaoMotor.Diferenca::depois);
        historicoStatus.setCellFactory(lista -> celula(m -> m.status().rotulo() + " — " + Datas.diaHora(m.dataHora())));
        StringConverter<VersaoMotor> nomeDaVersao = new StringConverter<>() {
            @Override
            public String toString(VersaoMotor versao) {
                return versao == null ? "" : "v" + versao.numero();
            }

            @Override
            public VersaoMotor fromString(String texto) {
                return null;
            }
        };
        compararA.setConverter(nomeDaVersao);
        compararB.setConverter(nomeDaVersao);
        compararA.valueProperty().addListener((obs, antes, depois) -> atualizarComparacao());
        compararB.valueProperty().addListener((obs, antes, depois) -> atualizarComparacao());
        botaoPontoDePartida.disableProperty().bind(versoes.getSelectionModel().selectedItemProperty().isNull());
    }

    void iniciar(CasosProjeto casos, CasosCatalogo catalogo, Navegacao navegacao, HostServices servicos,
            Parametros padroes, long projetoId) {
        this.casos = casos;
        this.catalogo = catalogo;
        this.navegacao = navegacao;
        this.servicos = servicos;
        this.padroes = padroes;
        this.projetoId = projetoId;
        aberto = casos.abrir(projetoId);
        formMotorController.usarCatalogo(catalogo);
        formMotorController.aoPreencherPeloCatalogo(item -> {
            if (descricaoMotor.getText().isBlank()) {
                descricaoMotor.setText(item.info().nome());
            }
        });
        formVeiculoController.usarCatalogo(catalogo);
        montarDimensionamento(padroes);
        recarregar();
        descartarVeiculo();
        descartarMotor();
    }

    /** Padrões de Configurações salvos com o projeto aberto. */
    void usarPadroes(Parametros padroes) {
        this.padroes = padroes;
        calculadora.usarPadroes(padroes);
    }

    /**
     * O catálogo pode ter mudado (tela Catálogos): as listas de peças e combustíveis são lidas de novo, e os cálculos
     * salvos são conferidos (uma peça com versão nova deixa o cálculo desatualizado). O que está sendo digitado fica.
     */
    void atualizarCatalogo() {
        calculadora.atualizarCatalogo();
        formMotorController.atualizarCombustiveis();
        formVeiculoController.atualizarCatalogo();
        mostrarInjecao();
        atualizarCalculos();
    }

    /** Lê o projeto de novo e atualiza o que é só leitura (os formulários em edição ficam como estão). */
    void recarregar() {
        aberto = casos.abrir(projetoId);
        Cliente cliente = aberto.cliente().cliente();
        Veiculo veiculo = aberto.veiculo().veiculo();
        StatusProjeto atual = aberto.projeto().status();

        titulo.setText(cliente.nome() + " · " + veiculo.descricao()
                + (veiculo.identificacao().isEmpty() ? "" : " · " + veiculo.identificacao()));
        subtitulo.setText("Projeto #" + projetoId + " · criado em " + Datas.diaHora(aberto.projeto().criadoEm()));
        status.setText(atual.rotulo());
        botaoArquivar.setText(atual == StatusProjeto.ARQUIVADO ? "Reativar projeto" : "Arquivar projeto");

        clienteNome.setText(cliente.nome());
        clienteContato.setText(String.join(" · ", textosNaoVazios(cliente.telefone(), cliente.email(),
                cliente.documento())));
        Consentimento consentimento = cliente.consentimento();
        clienteConsentimento.setText(consentimento == null ? "não registrado"
                : Datas.dia(consentimento.data()) + " — " + consentimento.finalidade());
        botaoEditarCliente.setDisable(cliente.anonimizado());
        botaoAnonimizar.setDisable(cliente.anonimizado());

        resumoVeiculo.setText("Veículo: " + veiculo.tipo().rotulo() + " " + veiculo.descricao()
                + (veiculo.anoModelo() == null ? "" : " " + veiculo.anoModelo())
                + (veiculo.identificacao().isEmpty() ? "" : " · " + veiculo.identificacao()));
        VersaoMotor motor = aberto.motor();
        resumoMotor.setText("Motor: v" + motor.numero() + (motor.descricao().isEmpty() ? "" : " · " + motor.descricao())
                + " · " + resumo(motor.configuracao()) + " · salvo em " + Datas.diaHora(motor.criadaEm()));
        resumoMeta.setText("Meta: " + meta(aberto.projeto().meta()));
        mostrarInjecao();
        historicoStatus.getItems().setAll(casos.historicoStatus(projetoId).reversed());
        observacoes.setText(aberto.projeto().observacoes());
        mensagemObservacoes.setText("");

        historicoVeiculo.getItems().setAll(casos.historicoVeiculo(aberto.veiculo().id()));
        List<VersaoMotor> todas = casos.versoesMotor(projetoId);
        versoes.getItems().setAll(todas.reversed());
        versaoAtual.setText("Versão atual: v" + motor.numero() + " (" + Datas.diaHora(motor.criadaEm()) + ")");
        compararA.getItems().setAll(todas);
        compararB.getItems().setAll(todas);
        compararA.setValue(todas.size() > 1 ? todas.get(todas.size() - 2) : todas.getFirst());
        compararB.setValue(todas.getLast());

        calculadora.usarMotorDoProjeto(motor.configuracao());
        atualizarCalculos();
    }

    // ===================== Cabeçalho =====================

    @FXML
    private void voltar() {
        navegacao.voltarParaProjetos();
    }

    @FXML
    private void arquivarOuReativar() {
        try {
            if (aberto.projeto().status() == StatusProjeto.ARQUIVADO) {
                casos.reativar(projetoId);
            } else if (confirmar("Arquivar o projeto?", "O projeto sai da lista de projetos (marque \"Mostrar "
                    + "arquivados\" para vê-lo). Nada é apagado e ele pode ser reativado.")) {
                casos.arquivar(projetoId);
            }
            recarregar();
        } catch (ErroDados erro) {
            avisar(Alert.AlertType.ERROR, "Não foi possível mudar o status", erro.getMessage());
        }
    }

    // ===================== Resumo: injeção =====================

    private void mostrarInjecao() {
        Long id = aberto.projeto().injecaoItemId();
        Optional<ItemCatalogo<Injecao>> item = id == null ? Optional.empty()
                : catalogo.buscar(TipoCatalogo.INJECAO, id);
        resumoInjecao.setText("Injeção: " + item.map(CaixaCatalogo::rotulo).orElse("não escolhida"));
        botaoTirarInjecao.setDisable(item.isEmpty());
    }

    /** Escolhe a injeção do projeto no catálogo, vendo o que cada uma atende para este motor. */
    @FXML
    private void trocarInjecao() {
        ConfiguracaoMotor motor = aberto.motor().configuracao();
        Set<Recurso> exigidos = recursosExigidos(motor);
        TableColumn<ItemCatalogo<Injecao>, String> atende = JanelaEscolhaCatalogo.coluna("Para este motor", 260,
                item -> {
                    List<String> faltas = SelecaoInjecao.faltas(item.dados(), motor, padroes.gruposSimultaneo(),
                            exigidos);
                    return faltas.isEmpty() ? "atende" : "não: " + String.join("; ", faltas);
                });
        new JanelaEscolhaCatalogo<>(abas.getScene() == null ? null : abas.getScene().getWindow(), catalogo,
                TipoCatalogo.INJECAO, "Injeção do projeto", "A coluna \"Para este motor\" confere saídas, cilindros "
                        + "em sequencial e recursos pedidos (sensor de etanol, controle do câmbio).",
                item -> true, atende).escolher().ifPresent(item -> {
                    try {
                        casos.trocarInjecao(projetoId, item.id());
                        recarregar();
                        calculadora.escolherInjecao(item.id());
                    } catch (ErroDados erro) {
                        avisar(Alert.AlertType.ERROR, "Não foi possível trocar a injeção", erro.getMessage());
                    }
                });
    }

    @FXML
    private void tirarInjecao() {
        try {
            casos.trocarInjecao(projetoId, null);
            recarregar();
        } catch (ErroDados erro) {
            avisar(Alert.AlertType.ERROR, "Não foi possível tirar a injeção", erro.getMessage());
        }
    }

    /** O que este projeto pede da injeção: sensor de etanol (motor) e controle do câmbio (veículo). */
    private Set<Recurso> recursosExigidos(ConfiguracaoMotor motor) {
        Set<Recurso> exigidos = EnumSet.noneOf(Recurso.class);
        if (motor.sensorEtanol()) {
            exigidos.add(Recurso.SENSOR_ETANOL);
        }
        if (aberto.veiculo().veiculo().cambioControladoPelaInjecao()) {
            exigidos.add(Recurso.CAMBIO);
        }
        return exigidos;
    }

    // ===================== Resumo: cliente =====================

    @FXML
    private void editarCliente() {
        FXMLLoader carregador = new FXMLLoader(ProjetoController.class.getResource("cliente.fxml"));
        Parent formulario = carregar(carregador);
        ClienteFormController form = carregador.getController();
        form.preencher(aberto.cliente().cliente());
        VBox erros = new VBox(4);
        Dialog<ButtonType> dialogo = dialogo("Editar cliente", new VBox(10, formulario, erros));
        ButtonType salvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().setAll(salvar, new ButtonType("Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().lookupButton(salvar).addEventFilter(ActionEvent.ACTION, evento -> {
            try {
                casos.salvarCliente(aberto.cliente().id(), form.ler());
            } catch (EntradaInvalidaException erro) {
                mostrarErros(erros, erro.erros());
                evento.consume();
            } catch (ErroDados erro) {
                mostrarErros(erros, List.of(erro.getMessage()));
                evento.consume();
            }
        });
        if (dialogo.showAndWait().filter(salvar::equals).isPresent()) {
            recarregar();
        }
    }

    @FXML
    private void historicoCliente() {
        TableView<EventoAuditoria> tabela = new TableView<>();
        tabela.getColumns().setAll(List.of(
                coluna("Data e hora", 140, e -> Datas.diaHora(e.dataHora())),
                coluna("Campo", 210, EventoAuditoria::campo),
                coluna("Antes", 240, e -> valor(e.valorAntigo())),
                coluna("Depois", 240, e -> valor(e.valorNovo()))));
        tabela.getItems().setAll(casos.historicoCliente(aberto.cliente().id()));
        tabela.setPrefSize(860, 360);
        Dialog<ButtonType> dialogo = dialogo("Histórico do cliente", tabela);
        dialogo.getDialogPane().getButtonTypes().setAll(new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.showAndWait();
    }

    @FXML
    private void anonimizarCliente() {
        if (!confirmar("Anonimizar o cliente?", "O nome vira \"Cliente anonimizado #" + aberto.cliente().id()
                + "\" e telefone, e-mail, documento e observações são apagados, também no histórico. Os veículos "
                + "dele perdem placa, chassi e identificação da embarcação. Projetos, motores e cálculos ficam.\n\n"
                + "Isto não pode ser desfeito.")) {
            return;
        }
        try {
            casos.anonimizarCliente(aberto.cliente().id());
            recarregar();
            descartarVeiculo();
        } catch (ErroDados erro) {
            avisar(Alert.AlertType.ERROR, "Não foi possível anonimizar", erro.getMessage());
        }
    }

    @FXML
    private void salvarObservacoes() {
        try {
            casos.salvarObservacoes(projetoId, observacoes.getText());
            mensagemObservacoes.setText("Observações salvas.");
        } catch (ErroDados erro) {
            avisar(Alert.AlertType.ERROR, "Não foi possível salvar", erro.getMessage());
        }
    }

    // ===================== Veículo =====================

    @FXML
    private void salvarVeiculo() {
        VeiculoFormController.Leitura leitura = formVeiculoController.ler();
        if (!leitura.erros().isEmpty()) {
            mostrarErros(mensagensVeiculo, leitura.erros());
            return;
        }
        try {
            casos.salvarVeiculo(aberto.veiculo().id(), leitura.veiculo(), formVeiculoController.cambioItemId());
            recarregar();
            descartarVeiculo();
            mostrarOk(mensagensVeiculo, "Veículo salvo. As mudanças estão no histórico abaixo.");
        } catch (EntradaInvalidaException erro) {
            mostrarErros(mensagensVeiculo, erro.erros());
        } catch (ErroDados erro) {
            mostrarErros(mensagensVeiculo, List.of(erro.getMessage()));
        }
    }

    @FXML
    private void descartarVeiculo() {
        formVeiculoController.preencher(aberto.veiculo().veiculo(), aberto.veiculo().cambioItemId());
        mensagensVeiculo.getChildren().clear();
    }

    // ===================== Motor =====================

    @FXML
    private void salvarMotor() {
        LeitorFormulario.LeituraMotor leitura = formMotorController.ler();
        if (!leitura.erros().isEmpty()) {
            mostrarErros(mensagensMotorProjeto, leitura.erros());
            return;
        }
        Validacao validacao = ValidadorMotor.validar(leitura.motor());
        formMotorController.mostrarMensagens(validacao.erros(), validacao.avisos());
        if (!validacao.erros().isEmpty()) {
            mostrarErros(mensagensMotorProjeto, validacao.erros());
            return;
        }
        try {
            Optional<VersaoMotor> nova = casos.salvarMotor(projetoId, descricaoMotor.getText(), leitura.motor());
            recarregar();
            descartarMotor();
            mostrarOk(mensagensMotorProjeto, nova.map(v -> "Motor salvo como v" + v.numero()
                    + ". Cálculos feitos com versões anteriores aparecem como desatualizados.")
                    .orElse("Nada mudou no motor: nenhuma versão nova."));
        } catch (EntradaInvalidaException erro) {
            mostrarErros(mensagensMotorProjeto, erro.erros());
        } catch (ErroDados erro) {
            mostrarErros(mensagensMotorProjeto, List.of(erro.getMessage()));
        }
    }

    @FXML
    private void descartarMotor() {
        mostrarVersaoNoFormulario(aberto.motor());
        mensagensMotorProjeto.getChildren().clear();
    }

    /** "Usar v1 como ponto de partida" (SPEC 6.3): o formulário recebe a versão; salvar cria uma versão nova. */
    @FXML
    private void usarComoPontoDePartida() {
        VersaoMotor escolhida = versoes.getSelectionModel().getSelectedItem();
        if (escolhida == null) {
            return;
        }
        mostrarVersaoNoFormulario(escolhida);
        mostrarOk(mensagensMotorProjeto, "O formulário está com a v" + escolhida.numero() + ". Ajuste o que precisar "
                + "e clique em \"Salvar como versão nova\" (a v" + escolhida.numero() + " continua guardada).");
    }

    private void mostrarVersaoNoFormulario(VersaoMotor versao) {
        descricaoMotor.setText(versao.descricao());
        formMotorController.preencher(versao.configuracao());
    }

    private void atualizarComparacao() {
        VersaoMotor a = compararA.getValue();
        VersaoMotor b = compararB.getValue();
        if (a == null || b == null) {
            comparacao.getItems().clear();
            comparacaoVazia.setText("Escolha duas versões.");
            return;
        }
        colunaComparacaoA.setText("v" + a.numero());
        colunaComparacaoB.setText("v" + b.numero());
        List<ComparacaoMotor.Diferenca> diferencas = new ArrayList<>();
        if (!a.descricao().equals(b.descricao())) {
            diferencas.add(new ComparacaoMotor.Diferenca("Descrição", a.descricao(), b.descricao()));
        }
        diferencas.addAll(ComparacaoMotor.comparar(a.configuracao(), b.configuracao()));
        comparacao.getItems().setAll(diferencas);
        comparacaoVazia.setText(a.id() == b.id() ? "É a mesma versão." : "As duas versões são iguais.");
    }

    /** O que mudou desta versão para a anterior, numa linha. */
    private String mudancasDaVersao(VersaoMotor versao) {
        if (versao.numero() == 1) {
            return "primeira versão";
        }
        Optional<VersaoMotor> anterior = versoes.getItems().stream()
                .filter(v -> v.numero() == versao.numero() - 1).findFirst();
        if (anterior.isEmpty()) {
            return "";
        }
        List<String> campos = new ArrayList<>();
        if (!anterior.get().descricao().equals(versao.descricao())) {
            campos.add("Descrição");
        }
        ComparacaoMotor.comparar(anterior.get().configuracao(), versao.configuracao())
                .forEach(diferenca -> campos.add(diferenca.campo()));
        return String.join(", ", campos);
    }

    // ===================== Dimensionamento =====================

    /** A calculadora com o motor do projeto, mais "Salvar cálculo" e a aba de cálculos salvos. */
    private void montarDimensionamento(Parametros padroes) {
        FXMLLoader carregador = new FXMLLoader(ProjetoController.class.getResource("calculadora.fxml"));
        Parent tela = carregar(carregador);
        calculadora = carregador.getController();
        calculadora.usarServicos(servicos);
        calculadora.usarPadroes(padroes);
        calculadora.usarMotorDoProjeto(aberto.motor().configuracao());
        calculadora.usarIdentificacao(() -> identificacao(aberto.motor()));
        calculadora.usarCatalogo(catalogo);
        calculadora.usarRecursosExigidos(() -> aberto.veiculo().veiculo().cambioControladoPelaInjecao()
                ? Set.of(Recurso.CAMBIO) : Set.of());
        casos.ultimoCalculo(projetoId).ifPresentOrElse(
                ultimo -> {
                    calculadora.preencherEntrada(ultimo.entrada());
                    calculadora.escolherPecas(ultimo.pecas());
                },
                () -> {
                    calculadora.preencherMeta(aberto.projeto().meta());
                    if (aberto.projeto().injecaoItemId() != null) {
                        calculadora.escolherInjecao(aberto.projeto().injecaoItemId());
                    }
                });

        botaoSalvarCalculo.setId("botaoSalvarCalculo");
        botaoSalvarCalculo.setOnAction(evento -> salvarCalculo());
        botaoSalvarCalculo.setDisable(true);
        calculadora.adicionarAcao(botaoSalvarCalculo);
        calculadora.aoMudarResultado(() -> botaoSalvarCalculo.setDisable(calculadora.calculoNaTela().isEmpty()));

        calculos.getColumns().setAll(List.of(
                coluna("Salvo em", 140, s -> Datas.diaHora(s.calculo().criadoEm())),
                coluna("Motor", 70, s -> "v" + s.calculo().numeroVersaoMotor()),
                coluna("Núcleo", 70, s -> s.calculo().versaoNucleo()),
                coluna("Semáforo", 190, s -> semaforo(s.calculo())),
                coluna("Situação", 640, s -> s.desatualizado()
                        ? "desatualizado — recalcular (" + String.join("; ", s.motivos()) + ")" : "em dia")));
        calculos.setPlaceholder(new Label("Nenhum cálculo salvo ainda. Calcule e clique em \"Salvar cálculo no "
                + "projeto\"."));
        Button pdf = new Button("PDF do cálculo escolhido…");
        pdf.disableProperty().bind(calculos.getSelectionModel().selectedItemProperty().isNull());
        pdf.setOnAction(evento -> pdfDoCalculoEscolhido());
        Label dica = new Label("Cálculo salvo não muda nem é recalculado sozinho (regra 2.5): se o motor mudar, ele "
                + "aparece como desatualizado e o novo cálculo fica salvo ao lado.");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);
        VBox conteudo = new VBox(8, dica, calculos, new HBox(10, pdf));
        conteudo.getStyleClass().add("pagina");
        VBox.setVgrow(calculos, Priority.ALWAYS);
        abaCalculosSalvos = new Tab("Cálculos salvos", conteudo);
        abaCalculosSalvos.setId("abaCalculosSalvos");
        calculadora.adicionarAba(abaCalculosSalvos);

        faixaCalculo.getStyleClass().add("faixa-desatualizado");
        faixaCalculo.setWrapText(true);
        faixaCalculo.setMaxWidth(Double.MAX_VALUE);
        painelDimensionamento.setTop(faixaCalculo);
        painelDimensionamento.setCenter(tela);
    }

    private void atualizarCalculos() {
        List<SituacaoCalculo> situacoes = casos.calculos(projetoId);
        calculos.getItems().setAll(situacoes);
        Optional<SituacaoCalculo> ultimo = situacoes.stream().findFirst();
        boolean desatualizado = ultimo.map(SituacaoCalculo::desatualizado).orElse(false);
        mostrar(faixaCalculo, desatualizado);
        ultimo.ifPresent(s -> faixaCalculo.setText("O último cálculo salvo está desatualizado — recalcular: "
                + String.join("; ", s.motivos()) + ". Clique em Calcular e salve de novo."));
        resumoCalculo.setText(ultimo.map(s -> "Salvo em " + Datas.diaHora(s.calculo().criadoEm()) + " com o motor v"
                + s.calculo().numeroVersaoMotor() + " · " + semaforo(s.calculo())
                + (s.desatualizado() ? " · DESATUALIZADO — recalcular (" + String.join("; ", s.motivos()) + ")" : ""))
                .orElse("Nenhum cálculo salvo. Use a aba Dimensionamento."));
        abaDimensionamento.setText(desatualizado ? "Dimensionamento (desatualizado)" : "Dimensionamento");
    }

    private void salvarCalculo() {
        Optional<CalculadoraController.CalculoNaTela> naTela = calculadora.calculoNaTela();
        if (naTela.isEmpty()) {
            return;
        }
        LocalDateTime agora = casos.agora();
        List<Justificativa> justificativas = new ArrayList<>();
        for (Map.Entry<Indicador, String> texto : naTela.get().justificativas().entrySet()) {
            if (texto.getValue() != null && !texto.getValue().isBlank()) {
                justificativas.add(new Justificativa(texto.getKey(), texto.getValue(), agora));
            }
        }
        try {
            casos.salvarCalculo(projetoId, naTela.get().entrada(), justificativas, naTela.get().pecas());
            botaoSalvarCalculo.setDisable(true);
            recarregar();
            calculadora.mostrarEstado("Cálculo salvo no projeto com o motor v" + aberto.motor().numero()
                    + " (aba \"Cálculos salvos\").");
            calculadora.mostrarAba(abaCalculosSalvos);
        } catch (EntradaInvalidaException erro) {
            avisar(Alert.AlertType.WARNING, "O cálculo não foi salvo", String.join("\n", erro.erros()));
            naTela.get().justificativas().entrySet().stream()
                    .filter(texto -> texto.getValue() == null || texto.getValue().isBlank())
                    .findFirst()
                    .ifPresent(texto -> calculadora.focarJustificativa(texto.getKey()));
        } catch (ErroDados erro) {
            avisar(Alert.AlertType.ERROR, "O cálculo não foi salvo", erro.getMessage());
        }
    }

    private void pdfDoCalculoEscolhido() {
        SituacaoCalculo escolhido = calculos.getSelectionModel().getSelectedItem();
        if (escolhido == null) {
            return;
        }
        CalculoSalvo calculo = casos.calculo(escolhido.calculo().id()).orElseThrow();
        VersaoMotor versao = casos.versoesMotor(projetoId).stream()
                .filter(v -> v.id() == calculo.versaoMotorId()).findFirst().orElse(aberto.motor());
        DadosRelatorio dados = new DadosRelatorio(calculo.entrada(), calculo.resultado(), calculo.justificativas(),
                Versao.atual(), casos.agora().withSecond(0), identificacao(versao));
        PdfMemoria.gerar(calculos.getScene().getWindow(), servicos, dados, "memoria-projeto-" + projetoId,
                texto -> { }, () -> { });
    }

    private IdentificacaoProjeto identificacao(VersaoMotor versao) {
        Veiculo veiculo = aberto.veiculo().veiculo();
        return new IdentificacaoProjeto(projetoId, aberto.cliente().cliente().nome(),
                veiculo.descricao() + (veiculo.identificacao().isEmpty() ? "" : " · " + veiculo.identificacao()),
                "v" + versao.numero() + (versao.descricao().isEmpty() ? "" : " · " + versao.descricao()));
    }

    // ===================== Textos =====================

    private static String resumo(ConfiguracaoMotor m) {
        List<String> partes = new ArrayList<>();
        partes.add(m.numeroCilindros() + " cil.");
        if (Double.isFinite(m.cilindradaCc())) {
            partes.add(NumeroBr.formatar(m.cilindradaCc() / 1000.0, 1) + " L");
        }
        if (m.aspiracao() != null) {
            partes.add(m.aspiracao().rotulo());
        }
        if (m.combustivel() != null) {
            partes.add(m.combustivel().nome());
        }
        return String.join(" · ", partes);
    }

    private static String meta(Meta meta) {
        String onde = meta.pontoMedicao() == PontoMedicao.RODA
                ? "na roda (perda de " + NumeroBr.formatar(meta.perdaTransmissaoPct(), 0, 1) + "%)"
                : "no virabrequim";
        return NumeroBr.formatar(Conversoes.kwParaCv(meta.potenciaKw()), 0) + " cv " + onde + " a "
                + NumeroBr.formatar(meta.rotacaoPotenciaMaxima(), 0) + " rpm"
                + (meta.rotacaoTorqueMaximo() == null ? ""
                        : " · torque máximo a " + NumeroBr.formatar(meta.rotacaoTorqueMaximo(), 0) + " rpm")
                + " · uso " + meta.uso().rotulo();
    }

    private static String semaforo(ResumoCalculo calculo) {
        return calculo.vermelhos() + " vermelho(s) · " + calculo.amarelos() + " amarelo(s)";
    }

    private static String valor(String texto) {
        return texto == null || texto.isEmpty() ? "—" : texto;
    }

    private static List<String> textosNaoVazios(String... textos) {
        List<String> lista = new ArrayList<>();
        for (String texto : textos) {
            if (texto != null && !texto.isBlank()) {
                lista.add(texto);
            }
        }
        return lista;
    }

    // ===================== Utilidades =====================

    private static <T> void coluna(TableColumn<T, String> coluna, Function<T, String> valor) {
        coluna.setCellValueFactory(celula -> new ReadOnlyStringWrapper(valor.apply(celula.getValue())));
    }

    private static <T> TableColumn<T, String> coluna(String titulo, double largura, Function<T, String> valor) {
        TableColumn<T, String> coluna = new TableColumn<>(titulo);
        coluna.setPrefWidth(largura);
        coluna(coluna, valor);
        return coluna;
    }

    private static <T> ListCell<T> celula(Function<T, String> texto) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : texto.apply(item));
            }
        };
    }

    private Dialog<ButtonType> dialogo(String titulo, Node conteudo) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (abas.getScene() != null) {
            dialogo.initOwner(abas.getScene().getWindow());
        }
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.getDialogPane().setContent(conteudo);
        dialogo.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        return dialogo;
    }

    private static Parent carregar(FXMLLoader carregador) {
        try {
            return carregador.load();
        } catch (IOException erro) {
            throw new UncheckedIOException("Tela do app não abriu: " + carregador.getLocation(), erro);
        }
    }

    private static void mostrar(Node no, boolean visivel) {
        no.setVisible(visivel);
        no.setManaged(visivel);
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

    private boolean confirmar(String titulo, String mensagem) {
        ButtonType sim = new ButtonType("Sim", ButtonBar.ButtonData.OK_DONE);
        ButtonType nao = new ButtonType("Não", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensagem, sim, nao);
        if (abas.getScene() != null) {
            alerta.initOwner(abas.getScene().getWindow());
        }
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        return alerta.showAndWait().filter(sim::equals).isPresent();
    }

    private void avisar(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alerta = new Alert(tipo, mensagem, ButtonType.OK);
        if (abas.getScene() != null) {
            alerta.initOwner(abas.getScene().getWindow());
        }
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }
}
