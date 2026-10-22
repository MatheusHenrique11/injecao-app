package br.com.calibra.app;

import static br.com.calibra.core.util.NumeroBr.formatar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosMapaBase;
import br.com.calibra.app.casos.CasosMapaBase.Escolhas;
import br.com.calibra.app.casos.CasosMapaBase.SituacaoMapa;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.mapa.AlertaMapa;
import br.com.calibra.core.mapa.CelulaMapa;
import br.com.calibra.core.mapa.EntradaMapaBase;
import br.com.calibra.core.mapa.FonteIgnicao;
import br.com.calibra.core.mapa.FuncaoTabela;
import br.com.calibra.core.mapa.GeradorMapaBase;
import br.com.calibra.core.mapa.JustificativaMapa;
import br.com.calibra.core.mapa.MapaBase;
import br.com.calibra.core.mapa.ModeloIgnicao;
import br.com.calibra.core.mapa.ModeloVe;
import br.com.calibra.core.mapa.OrientacaoTabela;
import br.com.calibra.core.mapa.PerfilTabela;
import br.com.calibra.core.mapa.RegrasMapaBase;
import br.com.calibra.core.mapa.Tabela;
import br.com.calibra.core.memoria.Aviso;
import br.com.calibra.core.memoria.Passo;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.ModeloCombustivel;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.projeto.CalculoSalvo;
import br.com.calibra.dados.projeto.MapaBaseSalvo;
import br.com.calibra.relatorios.Apresentacao;

/**
 * A aba "Mapa base" do projeto (fase 4, bloco 4.4; SPEC 7.5 e 8.3). Escolhe a injeção, o modelo de VE e a fonte da
 * ignição; gera em segundo plano (decisão 290); mostra os alertas, as travas e as tabelas com cores por faixa, com a
 * conta de qualquer célula; salva em versões, com a justificativa de cada vermelho (SPEC 2.4).
 */
public class MapaBaseController {

    /** O que a aba usa: os casos do mapa base, o catálogo e as regras de Configurações (lidas na hora de gerar). */
    public record Recursos(CasosMapaBase casos, CasosCatalogo catalogo, Supplier<RegrasMapaBase> regras) {
    }

    /** O mapa gerado na tela, ainda não salvo, com o que foi usado para gerar. */
    private record Gerado(long calculoId, Escolhas escolhas, RegrasMapaBase regras, EntradaMapaBase entrada,
            Resultado<MapaBase> resultado) {
    }

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label calculoUsado;
    @FXML private Label avisoCalculo;
    @FXML private ComboBox<ItemCatalogo<Injecao>> caixaInjecao;
    @FXML private ComboBox<ItemCatalogo<ModeloVe>> caixaModeloVe;
    @FXML private ComboBox<ItemCatalogo<ModeloIgnicao>> caixaModeloIgnicao;
    @FXML private CheckBox ignicaoDigitada;
    @FXML private TextField molaWastegate;
    @FXML private GridPane painelIgnicaoDigitada;
    @FXML private TextArea ignicaoRotacoes;
    @FXML private TextArea ignicaoCargas;
    @FXML private ComboBox<OrientacaoTabela.EixoDasColunas> ignicaoColunas;
    @FXML private ComboBox<OrientacaoTabela.OrdemDasLinhas> ignicaoLinhas;
    @FXML private TextArea ignicaoCelulas;
    @FXML private TextField ignicaoCombustivel;
    @FXML private TextField ignicaoTemperatura;
    @FXML private Button botaoGerar;
    @FXML private ProgressIndicator progressoGeracao;
    @FXML private Button botaoSalvarMapa;
    @FXML private Label estadoMapa;
    @FXML private VBox mensagensMapa;
    @FXML private VBox painelMapa;
    @FXML private TableView<SituacaoMapa> mapasSalvos;
    @FXML private TableColumn<SituacaoMapa, String> colunaVersaoMapa;
    @FXML private TableColumn<SituacaoMapa, String> colunaDataMapa;
    @FXML private TableColumn<SituacaoMapa, String> colunaMotorMapa;
    @FXML private TableColumn<SituacaoMapa, String> colunaAlertasMapa;
    @FXML private TableColumn<SituacaoMapa, String> colunaSituacaoMapa;
    @FXML private Button botaoVerSalvo;

    private final CalculoEmFundo calculos = new CalculoEmFundo();
    private final BooleanProperty temCalculo = new SimpleBooleanProperty(false);
    private final Map<AlertaMapa.Codigo, TextField> justificativas = new EnumMap<>(AlertaMapa.Codigo.class);
    private CaixaCatalogo<Injecao> injecoes;
    private CaixaCatalogo<ModeloVe> modelosVe;
    private CaixaCatalogo<ModeloIgnicao> modelosIgnicao;
    private Recursos recursos;
    private long projetoId;
    private Supplier<Long> injecaoDoProjeto = () -> null;
    private Runnable aoSalvar = () -> { };
    private CalculoSalvo calculo;
    private Gerado gerado;
    private boolean escolhasIniciais = true;

    @FXML
    private void initialize() {
        injecoes = new CaixaCatalogo<>(caixaInjecao, "(nenhuma: eixos padrão da SPEC, em ms)");
        modelosVe = new CaixaCatalogo<>(caixaModeloVe, "(escolha o modelo de VE)");
        modelosIgnicao = new CaixaCatalogo<>(caixaModeloIgnicao, "(sem fonte: a ignição sai vazia, em vermelho)");
        CamposTela.configurar(ignicaoColunas, Arrays.asList(OrientacaoTabela.EixoDasColunas.values()),
                OrientacaoTabela.EixoDasColunas::rotulo);
        CamposTela.configurar(ignicaoLinhas, Arrays.asList(OrientacaoTabela.OrdemDasLinhas.values()),
                OrientacaoTabela.OrdemDasLinhas::rotulo);
        ignicaoColunas.setValue(GradeMapa.PADRAO.colunas());
        ignicaoLinhas.setValue(GradeMapa.PADRAO.linhas());
        ignicaoDigitada.selectedProperty().addListener((obs, antes, depois) -> {
            painelIgnicaoDigitada.setVisible(depois);
            painelIgnicaoDigitada.setManaged(depois);
            caixaModeloIgnicao.setDisable(depois);
        });
        botaoGerar.disableProperty().bind(calculos.calculando().or(temCalculo.not()));
        progressoGeracao.visibleProperty().bind(calculos.calculando());
        progressoGeracao.managedProperty().bind(calculos.calculando());
        botaoSalvarMapa.setDisable(true);
        // Qualquer mudança nas escolhas depois de gerar: o mapa da tela não vale mais para salvar.
        Runnable aoMudar = this::escolhasMudaram;
        injecoes.aoMudar(aoMudar);
        modelosVe.aoMudar(aoMudar);
        modelosIgnicao.aoMudar(aoMudar);
        ignicaoDigitada.selectedProperty().addListener((obs, antes, depois) -> aoMudar.run());
        for (TextArea area : List.of(ignicaoRotacoes, ignicaoCargas, ignicaoCelulas)) {
            area.textProperty().addListener((obs, antes, depois) -> aoMudar.run());
        }
        for (TextField campo : List.of(molaWastegate, ignicaoCombustivel, ignicaoTemperatura)) {
            campo.textProperty().addListener((obs, antes, depois) -> aoMudar.run());
        }
        ignicaoColunas.valueProperty().addListener((obs, antes, depois) -> aoMudar.run());
        ignicaoLinhas.valueProperty().addListener((obs, antes, depois) -> aoMudar.run());
        configurarLista();
        estadoMapa.setText("Escolha e clique em Gerar.");
    }

    /**
     * @param injecaoDoProjeto a injeção escolhida no projeto (resumo), que vem escolhida aqui na primeira vez
     * @param aoSalvar avisa o projeto (o status pode ter mudado para Mapa base)
     */
    void iniciar(Recursos recursos, long projetoId, Supplier<Long> injecaoDoProjeto, Runnable aoSalvar) {
        this.recursos = recursos;
        this.projetoId = projetoId;
        this.injecaoDoProjeto = injecaoDoProjeto;
        this.aoSalvar = aoSalvar;
        atualizar();
    }

    /** Lê de novo o cálculo do projeto, o catálogo e os mapas salvos; as escolhas feitas ficam. */
    void atualizar() {
        if (recursos == null) {
            return;
        }
        CasosCatalogo catalogo = recursos.catalogo();
        injecoes.itens(catalogo.ativos(TipoCatalogo.INJECAO));
        modelosVe.itens(catalogo.ativos(TipoCatalogo.MODELO_VE));
        modelosIgnicao.itens(catalogo.ativos(TipoCatalogo.MODELO_IGNICAO));
        calculo = recursos.casos().calculoDoProjeto(projetoId).orElse(null);
        temCalculo.set(calculo != null);
        if (calculo == null) {
            calculoUsado.setText("Ainda não há cálculo salvo no projeto: calcule e salve no Dimensionamento (com o "
                    + "bico e a rotação de torque máximo) antes do mapa base.");
            mostrarAviso("");
        } else {
            calculoUsado.setText("Cálculo usado: #" + calculo.id() + ", salvo em " + Datas.diaHora(calculo.criadoEm())
                    + " (motor v" + calculo.numeroVersaoMotor() + ").");
            List<String> motivos = recursos.casos().motivosDoCalculo(projetoId);
            mostrarAviso(motivos.isEmpty() ? "" : "O último cálculo está desatualizado (" + String.join("; ", motivos)
                    + "): salve um cálculo novo no Dimensionamento antes de salvar o mapa.");
        }
        if (escolhasIniciais && calculo != null) {
            escolhasIniciais = false;
            Long injecao = injecaoDoProjeto.get();
            if (injecao != null) {
                catalogo.buscar(TipoCatalogo.INJECAO, injecao).ifPresent(injecoes::escolher);
            }
            recursos.casos().modeloVeSugerido(calculo.entrada().motor()).ifPresent(modelosVe::escolher);
        }
        mapasSalvos.getItems().setAll(recursos.casos().mapas(projetoId, recursos.regras().get()));
    }

    /** Se a geração ainda está rodando (os testes de tela esperam ela terminar). */
    ReadOnlyBooleanProperty gerando() {
        return calculos.calculando();
    }

    // ===================== Gerar e salvar =====================

    @FXML
    private void gerar() {
        mensagensMapa.getChildren().clear();
        List<String> erros = new ArrayList<>();
        Escolhas escolhas = lerEscolhas(erros);
        if (!erros.isEmpty() || calculo == null) {
            mostrarErros(erros);
            return;
        }
        RegrasMapaBase regras = recursos.regras().get();
        long calculoId = calculo.id();
        CasosMapaBase casos = recursos.casos();
        estadoMapa.setText("Gerando…");
        calculos.executar(() -> {
            EntradaMapaBase entrada = casos.entrada(projetoId, calculoId, escolhas, regras);
            return new Gerado(calculoId, escolhas, regras, entrada, GeradorMapaBase.gerar(entrada));
        }, this::mostrarGerado, this::falhou);
    }

    private void mostrarGerado(Gerado novo) {
        gerado = novo;
        mostrarMapa(novo.entrada(), novo.resultado(), null);
        long vermelhos = novo.resultado().valor().vermelhos().stream().map(AlertaMapa::codigo).distinct().count();
        botaoSalvarMapa.setDisable(false);
        estadoMapa.setText("Gerado às " + LocalDateTime.now().format(HORA)
                + (vermelhos > 0 ? " · " + vermelhos + " vermelho(s) precisam de justificativa para salvar" : ""));
    }

    private void falhou(Throwable erro) {
        List<String> textos = TarefasFundo.mensagens(erro);
        estadoMapa.setText("O mapa não foi gerado.");
        if (!textos.isEmpty()) {
            mostrarErros(textos);
        }
    }

    @FXML
    private void salvar() {
        if (gerado == null) {
            return;
        }
        mensagensMapa.getChildren().clear();
        LocalDateTime agora = recursos.casos().agora();
        List<JustificativaMapa> escritas = new ArrayList<>();
        justificativas.forEach((codigo, campo) -> {
            if (!campo.getText().isBlank()) {
                escritas.add(new JustificativaMapa(codigo, campo.getText(), agora));
            }
        });
        try {
            long id = recursos.casos().salvar(projetoId, gerado.calculoId(), gerado.escolhas(), gerado.regras(),
                    escritas);
            int numero = recursos.casos().mapa(id).map(MapaBaseSalvo::numero).orElse(0);
            gerado = null;
            botaoSalvarMapa.setDisable(true);
            estadoMapa.setText("Mapa base v" + numero + " salvo às " + LocalDateTime.now().format(HORA) + ".");
            atualizar();
            aoSalvar.run();
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
    }

    @FXML
    private void verSalvo() {
        SituacaoMapa escolhida = mapasSalvos.getSelectionModel().getSelectedItem();
        if (escolhida == null) {
            return;
        }
        recursos.casos().mapa(escolhida.mapa().id()).ifPresent(salvo -> {
            gerado = null;
            botaoSalvarMapa.setDisable(true);
            mostrarMapa(salvo.entrada(), salvo.resultado(), salvo.justificativas());
            estadoMapa.setText("Mostrando o mapa salvo v" + salvo.numero() + " (" + Datas.diaHora(salvo.criadoEm())
                    + ")" + (escolhida.desatualizado() ? ", desatualizado: gere de novo." : "."));
        });
    }

    private void escolhasMudaram() {
        if (gerado != null) {
            gerado = null;
            botaoSalvarMapa.setDisable(true);
            estadoMapa.setText("As escolhas mudaram depois de gerar: gere de novo para salvar.");
        }
    }

    /** As escolhas da tela; o que estiver errado vai para {@code erros}. */
    private Escolhas lerEscolhas(List<String> erros) {
        ItemCatalogo<ModeloVe> modelo = modelosVe.escolhido();
        if (modelo == null) {
            erros.add("Escolha o modelo de VE (Catálogos › Modelos de VE).");
        }
        Double mola = null;
        if (!molaWastegate.getText().isBlank()) {
            try {
                mola = NumeroBr.ler(molaWastegate.getText()) * 100.0;
                if (!(mola > 0)) {
                    erros.add("A mola da wastegate precisa ser maior que zero (ou fique em branco).");
                }
            } catch (NumberFormatException erro) {
                erros.add("Mola da wastegate: " + erro.getMessage());
            }
        }
        FonteIgnicao digitada = ignicaoDigitada.isSelected() ? lerIgnicaoDigitada(erros) : null;
        Long modeloIgnicao = ignicaoDigitada.isSelected() || modelosIgnicao.escolhido() == null ? null
                : modelosIgnicao.escolhido().id();
        Long injecao = injecoes.escolhido() == null ? null : injecoes.escolhido().id();
        return modelo == null ? null : new Escolhas(injecao, modelo.id(), modeloIgnicao, digitada, mola);
    }

    private FonteIgnicao lerIgnicaoDigitada(List<String> erros) {
        try {
            List<Double> rotacoes = NumeroBr.lerLista(ignicaoRotacoes.getText());
            List<Double> cargas = NumeroBr.lerLista(ignicaoCargas.getText());
            List<List<Double>> celulas = NumeroBr.lerGrade(ignicaoCelulas.getText());
            Tabela avanco = new OrientacaoTabela(ignicaoColunas.getValue(), ignicaoLinhas.getValue())
                    .daTela(rotacoes, cargas, celulas);
            Double temperatura = ignicaoTemperatura.getText().isBlank() ? null
                    : NumeroBr.ler(ignicaoTemperatura.getText());
            ModeloIgnicao conferir = new ModeloIgnicao("Tabela digitada", "", ignicaoCombustivel.getText(), temperatura,
                    avanco, "projeto", null);
            List<String> problemas = conferir.validar();
            if (!problemas.isEmpty()) {
                problemas.forEach(p -> erros.add("Tabela de ignição digitada: " + p));
                return null;
            }
            return FonteIgnicao.digitada(avanco, ignicaoCombustivel.getText().toUpperCase(Locale.ROOT), temperatura);
        } catch (IllegalArgumentException erro) {
            erros.add("Tabela de ignição digitada: " + erro.getMessage());
            return null;
        }
    }

    // ===================== O mapa na tela =====================

    /**
     * @param justificadas as justificativas de um mapa salvo; null num mapa recém-gerado (os campos ficam para
     *     escrever)
     */
    private void mostrarMapa(EntradaMapaBase entrada, Resultado<MapaBase> resultado,
            List<JustificativaMapa> justificadas) {
        MapaBase mapa = resultado.valor();
        justificativas.clear();
        VBox conta = new VBox(2);
        conta.setId("contaDaCelula");
        List<Node> partes = new ArrayList<>();
        partes.add(secao("Alertas"));
        partes.add(alertas(mapa, justificadas));
        partes.add(secao("Travas"));
        partes.add(travas(mapa, entrada));
        partes.add(secao("Tabelas (clique numa célula para ver a conta)"));
        partes.add(tabelas(entrada, mapa, conta));
        partes.add(conta);
        if (!resultado.avisos().isEmpty()) {
            partes.add(secao("Informações"));
            VBox avisos = new VBox(2);
            for (Aviso aviso : resultado.avisos()) {
                Label rotulo = new Label("• " + aviso.mensagem());
                rotulo.setWrapText(true);
                avisos.getChildren().add(rotulo);
            }
            partes.add(avisos);
        }
        TitledPane memoria = new TitledPane("Memória de cálculo (núcleo " + resultado.versaoNucleo() + ")",
                passos(resultado.memoria()));
        memoria.setExpanded(false);
        partes.add(memoria);
        painelMapa.getChildren().setAll(partes);
    }

    private VBox alertas(MapaBase mapa, List<JustificativaMapa> justificadas) {
        VBox caixa = new VBox(8);
        if (mapa.alertas().isEmpty()) {
            Label nenhum = new Label("Nenhum alerta.");
            nenhum.getStyleClass().add("ok");
            caixa.getChildren().add(nenhum);
        }
        List<AlertaMapa> ordenados = mapa.alertas().stream()
                .sorted((a, b) -> b.cor().compareTo(a.cor())).toList();
        for (AlertaMapa alerta : ordenados) {
            Label etiqueta = new Label(alerta.cor().name());
            etiqueta.getStyleClass().addAll("etiqueta-cor", "cor-" + alerta.cor().name().toLowerCase(Locale.ROOT));
            etiqueta.setMinWidth(100);
            Label titulo = new Label(alerta.codigo().rotulo());
            titulo.getStyleClass().add("negrito");
            Label mensagem = new Label(alerta.mensagem());
            mensagem.setWrapText(true);
            VBox textos = new VBox(2, titulo, mensagem);
            if (alerta.cor() == Cor.VERMELHO && !justificativas.containsKey(alerta.codigo())) {
                if (justificadas == null) {
                    TextField campo = new TextField();
                    campo.setPromptText("Justificativa obrigatória para salvar");
                    campo.setId("justificativa-" + alerta.codigo().name());
                    justificativas.put(alerta.codigo(), campo);
                    textos.getChildren().add(campo);
                } else {
                    justificadas.stream().filter(j -> j.alerta() == alerta.codigo()).findFirst().ifPresent(j -> {
                        Label texto = new Label("Justificativa (" + Datas.diaHora(j.dataHora()) + "): " + j.texto());
                        texto.setWrapText(true);
                        textos.getChildren().add(texto);
                    });
                }
            }
            HBox.setHgrow(textos, Priority.ALWAYS);
            caixa.getChildren().add(new HBox(10, etiqueta, textos));
        }
        return caixa;
    }

    private static GridPane travas(MapaBase mapa, EntradaMapaBase entrada) {
        GridPane grade = new GridPane();
        grade.getStyleClass().add("grade");
        MapaBase.Travas travas = mapa.travas();
        int linha = 0;
        if (Double.isFinite(travas.pressaoAlvoKpa())) {
            linha = linha(grade, linha, "Pressão do turbo alvo", Apresentacao.pressaoManometrica(travas.pressaoAlvoKpa())
                    + (travas.molaWastegateKpa() == null ? " (50% da prevista; sem a mola informada)" : ""));
            linha = linha(grade, linha, "Pressão prevista no dimensionamento",
                    Apresentacao.pressaoManometrica(travas.pressaoPrevistaKpa()));
        } else {
            linha = linha(grade, linha, "Pressão do turbo", "aspirado: sem pressão alvo");
        }
        linha = linha(grade, linha, "Limitador de rotação", formatar(travas.limitadorRpm(), 0) + " rpm");
        linha = linha(grade, linha, "Tabela de combustível", rotuloCombustivel(mapa) + (mapa.modeloCombustivel()
                == ModeloCombustivel.MS ? (mapa.tempoMortoNaTabela() ? ", com o tempo morto" : ", sem o tempo morto")
                        : ""));
        linha(grade, linha, "Ar de referência da tabela", Apresentacao.temperatura(mapa.temperaturaReferenciaC())
                + (entrada.injecao() == null ? " (sem injeção escolhida)" : ""));
        return grade;
    }

    private static int linha(GridPane grade, int linha, String rotulo, String valor) {
        Label nome = new Label(rotulo);
        nome.getStyleClass().add("negrito");
        Label texto = new Label(valor);
        texto.setWrapText(true);
        grade.add(nome, 0, linha);
        grade.add(texto, 1, linha);
        return linha + 1;
    }

    private TabPane tabelas(EntradaMapaBase entrada, MapaBase mapa, VBox conta) {
        LimiaresSemaforo limiares = entrada.dimensionamento().parametros().limiares();
        OrientacaoTabela combustivel = orientacao(entrada, FuncaoTabela.COMBUSTIVEL);
        OrientacaoTabela lambda = orientacao(entrada, FuncaoTabela.LAMBDA_ALVO);
        OrientacaoTabela ignicao = orientacao(entrada, FuncaoTabela.IGNICAO);
        BiConsumer<Double, Double> mostrarConta = (rotacao, carga) -> conta(entrada, rotacao, carga,
                conta);
        boolean emMs = mapa.modeloCombustivel() == ModeloCombustivel.MS;
        TabPane abas = new TabPane();
        abas.setId("tabelasDoMapa");
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        abas.getTabs().addAll(
                aba("tabelaCombustivel", rotuloCombustivel(mapa), mapa.combustivel(), combustivel,
                        v -> formatar(v, emMs ? 2 : 1), null, mostrarConta),
                aba("tabelaVe", "VE (%)", vezes(mapa.ve(), 100), combustivel, v -> formatar(v, 1), null, mostrarConta),
                aba("tabelaLambda", "Lambda alvo", mapa.lambda(), lambda, v -> formatar(v, 3), null, null),
                aba("tabelaIgnicao", "Ignição (°)", mapa.ignicao(), ignicao, v -> formatar(v, 1), null, null),
                aba("tabelaReducao", "Redução da ignição (°)", reducao(mapa), ignicao, v -> formatar(v, 1),
                        v -> Color.hsb(30, Math.clamp(v / 10.0, 0, 0.8), 1.0), null),
                aba("tabelaDc", "DC do bico (%)", vezes(mapa.dutyCycle(), 100), combustivel, v -> formatar(v, 0),
                        v -> v / 100 > limiares.dcAmareloAte() ? Color.web("#ef9a9a")
                                : v / 100 > limiares.dcVerdeAte() ? Color.web("#ffe082") : Color.web("#c8e6c9"),
                        mostrarConta));
        return abas;
    }

    private static Tab aba(String id, String titulo, Tabela tabela, OrientacaoTabela orientacao,
            DoubleFunction<String> texto, Function<Double, Color> cor,
            BiConsumer<Double, Double> aoClicar) {
        Tab aba = new Tab(titulo, GradeMapa.montar(id, tabela, orientacao, texto, cor, aoClicar));
        aba.setId("aba-" + id);
        return aba;
    }

    /** A orientação da tabela na tela da injeção, se o perfil diz; senão, a padrão. */
    private static OrientacaoTabela orientacao(EntradaMapaBase entrada, FuncaoTabela funcao) {
        Injecao injecao = entrada.injecao();
        Optional<PerfilTabela> tabela = injecao == null ? Optional.empty() : injecao.perfil().tabela(funcao);
        return tabela.map(PerfilTabela::orientacao).orElse(GradeMapa.PADRAO);
    }

    private static String rotuloCombustivel(MapaBase mapa) {
        return mapa.modeloCombustivel() == ModeloCombustivel.MS ? "Combustível (ms)" : "Combustível (VE %)";
    }

    private static Tabela vezes(Tabela tabela, double fator) {
        return Tabela.preencher(tabela.rotacoes(), tabela.cargas(), (rotacao, carga) -> {
            Double valor = tabela.valor(tabela.cargas().indexOf(carga), tabela.rotacoes().indexOf(rotacao));
            return valor == null ? null : valor * fator;
        });
    }

    /** Quanto as reduções tiraram de cada célula (fonte − mapa); vazia onde a fonte não cobre. */
    private static Tabela reducao(MapaBase mapa) {
        Tabela fonte = mapa.ignicaoFonte();
        Tabela comReducoes = mapa.ignicao();
        return Tabela.preencher(fonte.rotacoes(), fonte.cargas(), (rotacao, carga) -> {
            int i = fonte.cargas().indexOf(carga);
            int j = fonte.rotacoes().indexOf(rotacao);
            Double antes = fonte.valor(i, j);
            Double depois = comReducoes.valor(i, j);
            return antes == null || depois == null ? null : antes - depois;
        });
    }

    /** A conta da célula clicada (SPEC 7.5: memória em todas as células). */
    private static void conta(EntradaMapaBase entrada, double rotacao, double carga, VBox conta) {
        Resultado<CelulaMapa> celula = GeradorMapaBase.memoriaDaCelula(entrada, rotacao, carga);
        Label titulo = new Label("Conta da célula " + formatar(rotacao, 0) + " rpm · " + formatar(carga, 0) + " kPa");
        titulo.getStyleClass().add("titulo-secao");
        conta.getChildren().setAll(titulo, passos(celula.memoria()));
    }

    private static VBox passos(List<Passo> passos) {
        VBox caixa = new VBox(2);
        for (Passo passo : passos) {
            Label linha = new Label(passo.grandeza() + " = " + NumeroBr.formatar(passo.resultado().numero()) + " "
                    + passo.resultado().unidade() + "    (" + passo.formula() + ")");
            linha.setWrapText(true);
            caixa.getChildren().add(linha);
        }
        return caixa;
    }

    // ===================== Lista dos mapas salvos =====================

    private void configurarLista() {
        colunaVersaoMapa.setCellValueFactory(c -> new SimpleStringProperty("v" + c.getValue().mapa().numero()));
        colunaDataMapa.setCellValueFactory(c -> new SimpleStringProperty(Datas.diaHora(c.getValue().mapa().criadoEm())));
        colunaMotorMapa.setCellValueFactory(c -> new SimpleStringProperty("v" + c.getValue().mapa().numeroVersaoMotor()));
        colunaAlertasMapa.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mapa().vermelhos()
                + " vermelho(s), " + c.getValue().mapa().amarelos() + " amarelo(s)"));
        colunaSituacaoMapa.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().desatualizado()
                ? "desatualizado — gerar de novo: " + String.join("; ", c.getValue().motivos()) : "em dia"));
        mapasSalvos.setPlaceholder(new Label("Nenhum mapa salvo ainda."));
        botaoVerSalvo.disableProperty().bind(mapasSalvos.getSelectionModel().selectedItemProperty().isNull());
    }

    // ===================== Mensagens =====================

    private void mostrarAviso(String texto) {
        avisoCalculo.setText(texto);
        avisoCalculo.setVisible(!texto.isEmpty());
        avisoCalculo.setManaged(!texto.isEmpty());
    }

    private void mostrarErros(List<String> erros) {
        mensagensMapa.getChildren().clear();
        for (String erro : erros) {
            Label rotulo = new Label("• " + erro);
            rotulo.getStyleClass().add("erro");
            rotulo.setWrapText(true);
            mensagensMapa.getChildren().add(rotulo);
        }
    }

    private static Label secao(String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("titulo-secao");
        return rotulo;
    }

    /** As mensagensMapa de erro na tela (para os testes). */
    List<String> mensagensMapa() {
        return mensagensMapa.getChildren().stream().map(no -> ((Label) no).getText()).toList();
    }
}
