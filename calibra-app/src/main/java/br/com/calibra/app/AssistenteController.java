package br.com.calibra.app;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.CasosProjeto.ClienteExistente;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.EscolhaCliente;
import br.com.calibra.app.casos.CasosProjeto.EscolhaVeiculo;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.CasosProjeto.VeiculoExistente;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.motor.Validacao;
import br.com.calibra.core.motor.ValidadorMotor;
import br.com.calibra.core.projeto.PontoMedicao;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.cadastro.ClienteSalvo;
import br.com.calibra.dados.cadastro.VeiculoSalvo;

/**
 * Assistente de novo projeto (SPEC 8.2): Cliente → Veículo → Motor → Meta e uso. Cada etapa é conferida antes
 * de seguir; no fim, o caso de uso confere tudo de novo e grava tudo ou nada.
 */
public class AssistenteController {

    private static final int CLIENTE = 0;
    private static final int VEICULO = 1;
    private static final int MOTOR = 2;
    private static final int META = 3;

    @FXML private Label etapa1;
    @FXML private Label etapa2;
    @FXML private Label etapa3;
    @FXML private Label etapa4;
    @FXML private ScrollPane rolagem;
    @FXML private VBox painelCliente;
    @FXML private RadioButton clienteExistente;
    @FXML private VBox caixaClienteExistente;
    @FXML private TextField buscaCliente;
    @FXML private ListView<ClienteSalvo> listaClientes;
    @FXML private RadioButton clienteNovo;
    @FXML private VBox caixaClienteNovo;
    @FXML private ClienteFormController formClienteController;
    @FXML private VBox painelVeiculo;
    @FXML private RadioButton veiculoExistente;
    @FXML private ListView<VeiculoSalvo> listaVeiculos;
    @FXML private RadioButton veiculoNovo;
    @FXML private VBox caixaVeiculoNovo;
    @FXML private VeiculoFormController formVeiculoController;
    @FXML private VBox painelMotor;
    @FXML private TextField descricaoMotor;
    @FXML private MotorFormController formMotorController;
    @FXML private VBox painelMeta;
    @FXML private TextField potencia;
    @FXML private ComboBox<PontoMedicao> pontoMedicao;
    @FXML private TextField perdaTransmissao;
    @FXML private TextField rpmPotencia;
    @FXML private TextField rpmTorque;
    @FXML private ComboBox<Uso> uso;
    @FXML private VBox mensagens;
    @FXML private Button botaoVoltar;
    @FXML private Button botaoProximo;
    @FXML private Button botaoCriar;

    private final CamposTela camposMeta = new CamposTela();
    private CasosProjeto casos;
    private Navegacao navegacao;
    private Clock relogio;
    private int etapa;

    @FXML
    private void initialize() {
        CamposTela.configurar(pontoMedicao, Arrays.asList(PontoMedicao.values()), PontoMedicao::rotulo);
        CamposTela.configurar(uso, Arrays.asList(Uso.values()), Uso::rotulo);
        pontoMedicao.setValue(PontoMedicao.VIRABREQUIM);
        camposMeta.registrar(Campo.POTENCIA_CV, potencia);
        camposMeta.registrar(Campo.PONTO_MEDICAO, pontoMedicao, Enum::name);
        camposMeta.registrar(Campo.PERDA_TRANSMISSAO, perdaTransmissao);
        camposMeta.registrar(Campo.RPM_POTENCIA, rpmPotencia);
        camposMeta.registrar(Campo.RPM_TORQUE, rpmTorque);
        camposMeta.registrar(Campo.USO, uso, Enum::name);
        perdaTransmissao.disableProperty().bind(pontoMedicao.valueProperty().isNotEqualTo(PontoMedicao.RODA));

        listaClientes.setCellFactory(lista -> celula(c -> c.cliente().nome() + " · " + c.cliente().telefone()));
        listaVeiculos.setCellFactory(lista -> celula(v -> v.veiculo().descricao()
                + (v.veiculo().identificacao().isEmpty() ? "" : " · " + v.veiculo().identificacao())));
        buscaCliente.textProperty().addListener((obs, antes, depois) -> pesquisarClientes());
        caixaClienteExistente.disableProperty().bind(clienteExistente.selectedProperty().not());
        caixaClienteNovo.disableProperty().bind(clienteNovo.selectedProperty().not());
        listaVeiculos.disableProperty().bind(veiculoExistente.selectedProperty().not());
        caixaVeiculoNovo.disableProperty().bind(veiculoNovo.selectedProperty().not());
        listaClientes.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) -> {
            if (depois != null) {
                clienteExistente.setSelected(true);
            }
        });
    }

    void iniciar(CasosProjeto casos, Navegacao navegacao, Clock relogio) {
        this.casos = casos;
        this.navegacao = navegacao;
        this.relogio = relogio;
        formClienteController.limpar(LocalDate.now(relogio));
        pesquisarClientes();
        boolean temClientes = !listaClientes.getItems().isEmpty();
        clienteExistente.setDisable(!temClientes);
        (temClientes ? clienteExistente : clienteNovo).setSelected(true);
        mostrarEtapa(CLIENTE);
    }

    // ===================== Botões =====================

    @FXML
    private void proximo() {
        List<String> erros = conferirEtapa();
        if (!erros.isEmpty()) {
            mostrarErros(erros);
            return;
        }
        if (etapa == CLIENTE) {
            prepararVeiculo();
        }
        if (etapa < META) {
            mostrarEtapa(etapa + 1);
        }
    }

    @FXML
    private void voltar() {
        if (etapa > CLIENTE) {
            mostrarEtapa(etapa - 1);
        }
    }

    @FXML
    private void criar() {
        List<String> erros = conferirEtapa();
        if (!erros.isEmpty()) {
            mostrarErros(erros);
            return;
        }
        try {
            long projeto = casos.criarProjeto(new NovoProjeto(escolhaCliente(), escolhaVeiculo(),
                    descricaoMotor.getText(), formMotorController.ler().motor(), lerMeta().meta()));
            navegacao.abrirProjeto(projeto);
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
    }

    @FXML
    private void cancelar() {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, "O que foi digitado no assistente será perdido.",
                ButtonType.YES, ButtonType.NO);
        alerta.setTitle("Cancelar o novo projeto");
        alerta.setHeaderText("Cancelar o novo projeto?");
        if (alerta.showAndWait().filter(ButtonType.YES::equals).isPresent()) {
            navegacao.voltarParaProjetos();
        }
    }

    // ===================== Etapas =====================

    private void mostrarEtapa(int nova) {
        etapa = nova;
        List<Node> paineis = List.of(painelCliente, painelVeiculo, painelMotor, painelMeta);
        for (int i = 0; i < paineis.size(); i++) {
            paineis.get(i).setVisible(i == etapa);
            paineis.get(i).setManaged(i == etapa);
        }
        List<Label> rotulos = List.of(etapa1, etapa2, etapa3, etapa4);
        for (int i = 0; i < rotulos.size(); i++) {
            rotulos.get(i).getStyleClass().removeAll("etapa-atual", "etapa");
            rotulos.get(i).getStyleClass().add(i == etapa ? "etapa-atual" : "etapa");
        }
        botaoVoltar.setDisable(etapa == CLIENTE);
        botaoProximo.setVisible(etapa < META);
        botaoProximo.setManaged(etapa < META);
        botaoProximo.setDefaultButton(etapa < META);
        botaoCriar.setVisible(etapa == META);
        botaoCriar.setManaged(etapa == META);
        botaoCriar.setDefaultButton(etapa == META);
        mensagens.getChildren().clear();
        rolagem.setVvalue(0);
    }

    /** Erros da etapa atual, pelas mesmas regras que o caso de uso confere no fim. */
    private List<String> conferirEtapa() {
        LocalDate hoje = LocalDate.now(relogio);
        List<String> erros = new ArrayList<>();
        switch (etapa) {
            case CLIENTE -> {
                if (clienteExistente.isSelected() && listaClientes.getSelectionModel().getSelectedItem() == null) {
                    erros.add("Escolha o cliente na lista ou marque \"Cliente novo\".");
                } else if (clienteNovo.isSelected()) {
                    erros.addAll(formClienteController.ler().validar(hoje));
                }
            }
            case VEICULO -> {
                if (veiculoExistente.isSelected() && listaVeiculos.getSelectionModel().getSelectedItem() == null) {
                    erros.add("Escolha o veículo na lista ou marque \"Veículo novo\".");
                } else if (veiculoNovo.isSelected()) {
                    VeiculoFormController.Leitura leitura = formVeiculoController.ler();
                    erros.addAll(leitura.erros());
                    if (leitura.veiculo() != null) {
                        erros.addAll(leitura.veiculo().validar(hoje.getYear()));
                    }
                }
            }
            case MOTOR -> {
                LeitorFormulario.LeituraMotor leitura = formMotorController.ler();
                if (!leitura.erros().isEmpty()) {
                    erros.addAll(leitura.erros());
                } else {
                    Validacao validacao = ValidadorMotor.validar(leitura.motor());
                    formMotorController.mostrarMensagens(validacao.erros(), validacao.avisos());
                    erros.addAll(validacao.erros());
                }
            }
            case META -> {
                LeitorFormulario.LeituraMeta leitura = lerMeta();
                erros.addAll(leitura.erros());
                if (leitura.meta() != null) {
                    erros.addAll(CalculoDimensionamento.validarMeta(leitura.meta(), formMotorController.ler().motor()));
                }
            }
            default -> throw new IllegalStateException("etapa " + etapa);
        }
        return erros;
    }

    /** Ao sair da etapa Cliente: os veículos do cliente escolhido, ou só "Veículo novo" para cliente novo. */
    private void prepararVeiculo() {
        ClienteSalvo escolhido = clienteExistente.isSelected() ? listaClientes.getSelectionModel().getSelectedItem()
                : null;
        List<VeiculoSalvo> veiculos = escolhido == null ? List.of() : casos.veiculosDoCliente(escolhido.id());
        listaVeiculos.getItems().setAll(veiculos);
        veiculoExistente.setDisable(veiculos.isEmpty());
        if (veiculos.isEmpty()) {
            veiculoNovo.setSelected(true);
        } else {
            veiculoExistente.setSelected(true);
            listaVeiculos.getSelectionModel().selectFirst();
        }
    }

    private void pesquisarClientes() {
        if (casos != null) {
            listaClientes.getItems().setAll(casos.pesquisarClientes(buscaCliente.getText()));
        }
    }

    private EscolhaCliente escolhaCliente() {
        if (clienteExistente.isSelected()) {
            return new ClienteExistente(listaClientes.getSelectionModel().getSelectedItem().id());
        }
        Cliente novo = formClienteController.ler();
        return new ClienteNovo(novo);
    }

    private EscolhaVeiculo escolhaVeiculo() {
        if (veiculoExistente.isSelected()) {
            return new VeiculoExistente(listaVeiculos.getSelectionModel().getSelectedItem().id());
        }
        return new VeiculoNovo(formVeiculoController.ler().veiculo());
    }

    private LeitorFormulario.LeituraMeta lerMeta() {
        return LeitorFormulario.lerMeta(camposMeta.valores());
    }

    private void mostrarErros(List<String> erros) {
        mensagens.getChildren().clear();
        for (String erro : erros) {
            Label rotulo = new Label("• " + erro);
            rotulo.getStyleClass().add("erro");
            rotulo.setWrapText(true);
            mensagens.getChildren().add(rotulo);
        }
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
}
