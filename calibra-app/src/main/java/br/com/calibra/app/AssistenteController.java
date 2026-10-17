package br.com.calibra.app;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.SugestaoPecas;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.dimensionamento.SelecaoInjecao;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.Validacao;
import br.com.calibra.core.motor.ValidadorMotor;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Recurso;
import br.com.calibra.core.projeto.PontoMedicao;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Assistente de novo projeto (SPEC 8.2): Cliente → Veículo → Motor → Meta e uso → Injeção (opcional). Cada etapa
 * é conferida antes de seguir; no fim, o caso de uso confere tudo de novo e grava tudo ou nada.
 */
public class AssistenteController {

    private static final int CLIENTE = 0;
    private static final int VEICULO = 1;
    private static final int MOTOR = 2;
    private static final int META = 3;
    private static final int INJECAO = 4;

    @FXML private Label etapa1;
    @FXML private Label etapa2;
    @FXML private Label etapa3;
    @FXML private Label etapa4;
    @FXML private Label etapa5;
    @FXML private ScrollPane rolagem;
    @FXML private VBox painelCliente;
    @FXML private EscolhaClienteController escolhaClienteController;
    @FXML private VBox painelVeiculo;
    @FXML private EscolhaVeiculoController escolhaVeiculoController;
    @FXML private VBox painelMotor;
    @FXML private TextField descricaoMotor;
    @FXML private MotorFormController formMotorController;
    @FXML private VBox painelMeta;
    @FXML private ComboBox<TipoMeta> tipoMeta;
    @FXML private TextField potencia;
    @FXML private TextField pressaoMeta;
    @FXML private ComboBox<TipoMeta.Unidade> unidadePressaoMeta;
    @FXML private ComboBox<PontoMedicao> pontoMedicao;
    @FXML private TextField perdaTransmissao;
    @FXML private TextField rpmPotencia;
    @FXML private TextField rpmTorque;
    @FXML private ComboBox<Uso> uso;
    @FXML private VBox painelInjecao;
    @FXML private ComboBox<ItemCatalogo<Injecao>> injecao;
    @FXML private VBox mensagensInjecao;
    @FXML private VBox mensagens;
    @FXML private Button botaoVoltar;
    @FXML private Button botaoProximo;
    @FXML private Button botaoCriar;

    private final CamposTela camposMeta = new CamposTela();
    private CaixaCatalogo<Injecao> caixaInjecao;
    private CasosProjeto casos;
    private CasosCatalogo catalogo;
    private Parametros padroes = Parametros.padrao();
    private Navegacao navegacao;
    private Clock relogio;
    private int etapa;

    @FXML
    private void initialize() {
        CamposTela.configurar(tipoMeta, Arrays.asList(TipoMeta.values()), TipoMeta::rotulo);
        CamposTela.configurar(unidadePressaoMeta, Arrays.asList(TipoMeta.Unidade.values()), TipoMeta.Unidade::simbolo);
        CamposTela.configurar(pontoMedicao, Arrays.asList(PontoMedicao.values()), PontoMedicao::rotulo);
        CamposTela.configurar(uso, Arrays.asList(Uso.values()), Uso::rotulo);
        tipoMeta.setValue(TipoMeta.POTENCIA);
        unidadePressaoMeta.setValue(TipoMeta.Unidade.BAR);
        pontoMedicao.setValue(PontoMedicao.VIRABREQUIM);
        camposMeta.registrar(Campo.TIPO_META, tipoMeta, Enum::name);
        camposMeta.registrar(Campo.POTENCIA_CV, potencia);
        camposMeta.registrar(Campo.PRESSAO_META, pressaoMeta);
        camposMeta.registrar(Campo.UNIDADE_PRESSAO_META, unidadePressaoMeta, Enum::name);
        camposMeta.registrar(Campo.PONTO_MEDICAO, pontoMedicao, Enum::name);
        camposMeta.registrar(Campo.PERDA_TRANSMISSAO, perdaTransmissao);
        camposMeta.registrar(Campo.RPM_POTENCIA, rpmPotencia);
        camposMeta.registrar(Campo.RPM_TORQUE, rpmTorque);
        camposMeta.registrar(Campo.USO, uso, Enum::name);
        perdaTransmissao.disableProperty().bind(pontoMedicao.valueProperty().isNotEqualTo(PontoMedicao.RODA));
        potencia.disableProperty().bind(tipoMeta.valueProperty().isEqualTo(TipoMeta.PRESSAO));
        pressaoMeta.disableProperty().bind(tipoMeta.valueProperty().isNotEqualTo(TipoMeta.PRESSAO));
        unidadePressaoMeta.disableProperty().bind(tipoMeta.valueProperty().isNotEqualTo(TipoMeta.PRESSAO));

        caixaInjecao = new CaixaCatalogo<>(injecao, "(nenhuma por enquanto)");
        caixaInjecao.aoMudar(this::conferirInjecao);
        formMotorController.aoPreencherPeloCatalogo(item -> {
            if (descricaoMotor.getText().isBlank()) {
                descricaoMotor.setText(item.info().nome());
            }
        });
    }

    /**
     * @param padroes padrões de Configurações: os grupos do modo simultâneo, para conferir as saídas da injeção
     */
    void iniciar(CasosProjeto casos, CasosCatalogo catalogo, Parametros padroes, Navegacao navegacao, Clock relogio) {
        this.casos = casos;
        this.catalogo = catalogo;
        this.padroes = padroes;
        this.navegacao = navegacao;
        this.relogio = relogio;
        formMotorController.usarCatalogo(catalogo);
        escolhaVeiculoController.iniciar(casos, catalogo);
        caixaInjecao.itens(catalogo.ativos(TipoCatalogo.INJECAO));
        escolhaClienteController.iniciar(casos, LocalDate.now(relogio));
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
            escolhaVeiculoController.prepararPara(escolhaClienteController.existente(),
                    escolhaClienteController.busca());
        }
        if (etapa < INJECAO) {
            mostrarEtapa(etapa + 1);
        }
        if (etapa == INJECAO) {
            conferirInjecao();
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
            ItemCatalogo<Injecao> escolhida = caixaInjecao.escolhido();
            long projeto = casos.criarProjeto(new NovoProjeto(escolhaClienteController.escolha(),
                    escolhaVeiculoController.escolha(), descricaoMotor.getText(), formMotorController.ler().motor(),
                    lerMeta().meta(),
                    escolhida == null ? null : escolhida.id()));
            navegacao.abrirProjeto(projeto);
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
    }

    @FXML
    private void cancelar() {
        Alert alerta = Alertas.criar(Alert.AlertType.CONFIRMATION, "O que foi digitado no assistente será perdido.",
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
        List<Node> paineis = List.of(painelCliente, painelVeiculo, painelMotor, painelMeta, painelInjecao);
        for (int i = 0; i < paineis.size(); i++) {
            paineis.get(i).setVisible(i == etapa);
            paineis.get(i).setManaged(i == etapa);
        }
        List<Label> rotulos = List.of(etapa1, etapa2, etapa3, etapa4, etapa5);
        for (int i = 0; i < rotulos.size(); i++) {
            rotulos.get(i).getStyleClass().removeAll("etapa-atual", "etapa");
            rotulos.get(i).getStyleClass().add(i == etapa ? "etapa-atual" : "etapa");
        }
        botaoVoltar.setDisable(etapa == CLIENTE);
        botaoProximo.setVisible(etapa < INJECAO);
        botaoProximo.setManaged(etapa < INJECAO);
        botaoProximo.setDefaultButton(etapa < INJECAO);
        botaoCriar.setVisible(etapa == INJECAO);
        botaoCriar.setManaged(etapa == INJECAO);
        botaoCriar.setDefaultButton(etapa == INJECAO);
        mensagens.getChildren().clear();
        rolagem.setVvalue(0);
    }

    /** Erros da etapa atual, pelas mesmas regras que o caso de uso confere no fim. */
    private List<String> conferirEtapa() {
        LocalDate hoje = LocalDate.now(relogio);
        List<String> erros = new ArrayList<>();
        switch (etapa) {
            case CLIENTE -> erros.addAll(escolhaClienteController.conferir(hoje));
            case VEICULO -> erros.addAll(escolhaVeiculoController.conferir(hoje.getYear()));
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
            case INJECAO -> {
                // Opcional; uma injeção que não atende ao motor só gera aviso.
            }
            default -> throw new IllegalStateException("etapa " + etapa);
        }
        return erros;
    }

    // ===================== Injeção =====================

    @FXML
    private void sugerirInjecao() {
        ConfiguracaoMotor motor = formMotorController.ler().motor();
        if (motor == null) {
            return;
        }
        SugestaoPecas.Sugestao<Injecao> sugestao = SugestaoPecas.injecao(catalogo.ativos(TipoCatalogo.INJECAO), motor,
                padroes.gruposSimultaneo(), recursosExigidos(motor));
        sugestao.item().ifPresent(caixaInjecao::escolher);
        mostrarInjecao(sugestao.mensagem(), sugestao.avisos(), sugestao.item().isPresent() ? "ok" : "aviso-amarelo");
    }

    /** O que falta na injeção escolhida para este motor (sensor de etanol e câmbio controlado, se pedidos). */
    private void conferirInjecao() {
        ItemCatalogo<Injecao> escolhida = caixaInjecao.escolhido();
        ConfiguracaoMotor motor = formMotorController.ler().motor();
        if (escolhida == null || motor == null) {
            mensagensInjecao.getChildren().clear();
            return;
        }
        List<String> faltas = SelecaoInjecao.faltas(escolhida.dados(), motor, padroes.gruposSimultaneo(),
                recursosExigidos(motor));
        if (faltas.isEmpty()) {
            mostrarInjecao(escolhida.info().nome() + " atende a este motor.", List.of(), "ok");
        } else {
            mostrarInjecao(escolhida.info().nome() + " não atende a este motor:", faltas, "aviso-amarelo");
        }
    }

    /** Recursos que o projeto pede da injeção: sensor de etanol (motor) e controle do câmbio (veículo). */
    private Set<Recurso> recursosExigidos(ConfiguracaoMotor motor) {
        Set<Recurso> exigidos = EnumSet.noneOf(Recurso.class);
        if (motor.sensorEtanol()) {
            exigidos.add(Recurso.SENSOR_ETANOL);
        }
        Veiculo veiculo = escolhaVeiculoController.veiculo();
        if (veiculo != null && veiculo.cambioControladoPelaInjecao()) {
            exigidos.add(Recurso.CAMBIO);
        }
        return exigidos;
    }

    private void mostrarInjecao(String titulo, List<String> itens, String estilo) {
        mensagensInjecao.getChildren().clear();
        Label principal = new Label(titulo);
        principal.getStyleClass().add(estilo);
        principal.setWrapText(true);
        mensagensInjecao.getChildren().add(principal);
        for (String item : itens) {
            Label rotulo = new Label("• " + item);
            rotulo.getStyleClass().add("aviso-amarelo");
            rotulo.setWrapText(true);
            mensagensInjecao.getChildren().add(rotulo);
        }
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
}
