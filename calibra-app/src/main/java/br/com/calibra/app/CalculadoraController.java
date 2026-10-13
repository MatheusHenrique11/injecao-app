package br.com.calibra.app;

import static br.com.calibra.core.util.NumeroBr.formatar;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import br.com.calibra.app.casos.CampoPadrao;
import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.FormularioPadroes;
import br.com.calibra.app.casos.SugestaoPecas;
import br.com.calibra.app.catalogo.PecasEscolhidas;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.CalculoNaoConvergiuException;
import br.com.calibra.core.calculo.CaminhoAr;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Intercooler;
import br.com.calibra.core.pecas.Recurso;
import br.com.calibra.core.pecas.Sensor;
import br.com.calibra.core.pecas.TipoSensor;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.PontoMedicao;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.projeto.PecaUsada;
import br.com.calibra.relatorios.DadosRelatorio;
import br.com.calibra.relatorios.IdentificacaoProjeto;
import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Tela da calculadora (fase 1): o motor vem do formulário {@link MotorFormController}; aqui ficam a meta, as
 * peças, o resultado e o PDF.
 */
public class CalculadoraController {

    /** Campos da calculadora que começam com um padrão de Configurações. */
    private static final Map<CampoPadrao, List<Campo>> CAMPOS_COM_PADRAO = Map.of(
            CampoPadrao.K_AR, List.of(Campo.K_AR),
            CampoPadrao.DC_MAX_PCT, List.of(Campo.DC_MAX_PCT),
            CampoPadrao.ETA_C, List.of(Campo.ETA_C),
            CampoPadrao.EPS, List.of(Campo.EPS),
            CampoPadrao.PERDA_DESCARGA_PSI, List.of(Campo.PERDA_DESCARGA_PSI),
            CampoPadrao.PERDA_ADMISSAO_PSI, List.of(Campo.PERDA_ADMISSAO_PSI),
            CampoPadrao.ACRESCIMO_TM, List.of(Campo.ACRESCIMO_TM),
            CampoPadrao.DENSIDADE_TESTE,
            List.of(Campo.DENSIDADE_TESTE_PADRAO, Campo.BICO_DENSIDADE_TESTE, Campo.BICO2_DENSIDADE_TESTE),
            CampoPadrao.TI_MINIMO, List.of(Campo.TI_MINIMO_MARCHA_LENTA));

    // ---------- Motor (motor.fxml) ----------
    @FXML private Tab abaMotor;
    @FXML private MotorFormController motorController;

    // ---------- Meta e peças ----------
    @FXML private Tab abaMeta;
    @FXML private TextField potencia;
    @FXML private ComboBox<PontoMedicao> pontoMedicao;
    @FXML private TextField perdaTransmissao;
    @FXML private TextField rpmPotencia;
    @FXML private TextField rpmTorque;
    @FXML private ComboBox<Uso> uso;
    @FXML private ComboBox<CaminhoAr> caminhoAr;
    @FXML private TextField kAr;
    @FXML private TextField bsfc;
    @FXML private TextField lambda;
    @FXML private TextField ve;
    @FXML private TextField temperaturaAmbiente;
    @FXML private TextField perdaDescarga;
    @FXML private TextField perdaAdmissao;
    @FXML private TextField etaC;
    @FXML private TextField eps;
    @FXML private TextField acrescimoTm;
    @FXML private TextField dcMax;
    @FXML private TextField densidadeTestePadrao;
    @FXML private TextField tiMinimoMarchaLenta;
    @FXML private TextField pressaoBase;
    @FXML private TextField bicoVazao;
    @FXML private TextField bicoPressaoTeste;
    @FXML private TextField bicoDensidadeTeste;
    @FXML private TextField bicoTempoMorto;
    @FXML private TextField bico2Vazao;
    @FXML private TextField bico2PressaoTeste;
    @FXML private TextField bico2DensidadeTeste;
    @FXML private TextField bico2TempoMorto;
    @FXML private TextField marchaLentaMap;
    @FXML private TextField marchaLentaVe;
    @FXML private TextField bombaVazao;
    @FXML private TextField sensorMapFaixa;
    @FXML private TextField injecaoNome;
    @FXML private TextField injecaoSaidasBico;
    @FXML private TextField injecaoSaidasIgnicao;

    // ---------- Peças do catálogo ----------
    @FXML private VBox secaoCatalogo;
    @FXML private ComboBox<ItemCatalogo<Bico>> catalogoBico;
    @FXML private ComboBox<ItemCatalogo<Bico>> catalogoBico2;
    @FXML private ComboBox<ItemCatalogo<Bomba>> catalogoBomba;
    @FXML private ComboBox<ItemCatalogo<Intercooler>> catalogoIntercooler;
    @FXML private ComboBox<ItemCatalogo<Sensor>> catalogoSensorMap;
    @FXML private ComboBox<ItemCatalogo<Injecao>> catalogoInjecao;
    @FXML private Button botaoSugerirBico;
    @FXML private Button botaoSugerirBomba;
    @FXML private Button botaoSugerirInjecao;
    @FXML private TextField tensao;
    @FXML private VBox mensagensPecas;

    // ---------- Resultado e rodapé ----------
    @FXML private TabPane abas;
    @FXML private Tab abaResultado;
    @FXML private VBox painelResultado;
    @FXML private HBox rodape;
    @FXML private Button botaoCalcular;
    @FXML private Button botaoPdf;
    @FXML private Label rotuloEstado;

    private final CamposTela campos = new CamposTela();
    private final PainelResultado painel = new PainelResultado();
    private final List<Runnable> aoMudarResultado = new ArrayList<>();
    /** Padrões de cálculo e limiares do semáforo em uso; sem Configurações (teste), os da SPEC. */
    private Parametros padroes = Parametros.padrao();
    /** Motor da versão atual do projeto; null na calculadora avulsa, que lê o motor da aba 1. */
    private ConfiguracaoMotor motorDoProjeto;
    private Supplier<IdentificacaoProjeto> identificacao = () -> null;
    private HostServices servicos;
    private EntradaDimensionamento ultimaEntrada;
    private Resultado<Dimensionamento> ultimoResultado;
    private boolean desatualizado;
    private Label faixaDesatualizado;
    /** Catálogo ligado pela janela principal ou pelo projeto; sem ele (testes), a seção de peças fica escondida. */
    private CasosCatalogo catalogo;
    private CaixaCatalogo<Bico> caixaBico;
    private CaixaCatalogo<Bico> caixaBico2;
    private CaixaCatalogo<Bomba> caixaBomba;
    private CaixaCatalogo<Intercooler> caixaIntercooler;
    private CaixaCatalogo<Sensor> caixaSensorMap;
    private CaixaCatalogo<Injecao> caixaInjecao;
    /** Recursos da injeção que o projeto pede (ex.: câmbio controlado pela injeção). */
    private Supplier<Set<Recurso>> recursosExigidos = Set::of;
    private PecasEscolhidas ultimasPecas = PecasEscolhidas.NENHUMA;

    void usarServicos(HostServices hostServices) {
        this.servicos = hostServices;
    }

    @FXML
    private void initialize() {
        configurarEscolhas();
        registrarCampos();
        preencherPadroes();
        configurarPecasDoCatalogo();
        configurarReacoes();
        botaoPdf.setDisable(true);
        rotuloEstado.setText("Preencha o motor e a meta e clique em Calcular (Enter).");
    }

    // ===================== Montagem da tela =====================

    private void configurarEscolhas() {
        CamposTela.configurar(pontoMedicao, Arrays.asList(PontoMedicao.values()), PontoMedicao::rotulo);
        CamposTela.configurar(uso, Arrays.asList(Uso.values()), Uso::rotulo);
        CamposTela.configurar(caminhoAr, Arrays.asList(CaminhoAr.values()), CaminhoAr::rotulo);
    }

    private void registrarCampos() {
        campos.registrar(Campo.POTENCIA_CV, potencia);
        campos.registrar(Campo.PONTO_MEDICAO, pontoMedicao, Enum::name);
        campos.registrar(Campo.PERDA_TRANSMISSAO, perdaTransmissao);
        campos.registrar(Campo.RPM_POTENCIA, rpmPotencia);
        campos.registrar(Campo.RPM_TORQUE, rpmTorque);
        campos.registrar(Campo.USO, uso, Enum::name);
        campos.registrar(Campo.CAMINHO_AR, caminhoAr, Enum::name);
        campos.registrar(Campo.K_AR, kAr);
        campos.registrar(Campo.BSFC, bsfc);
        campos.registrar(Campo.LAMBDA, lambda);
        campos.registrar(Campo.VE, ve);
        campos.registrar(Campo.TEMPERATURA_AMBIENTE, temperaturaAmbiente);
        campos.registrar(Campo.PERDA_DESCARGA_PSI, perdaDescarga);
        campos.registrar(Campo.PERDA_ADMISSAO_PSI, perdaAdmissao);
        campos.registrar(Campo.ETA_C, etaC);
        campos.registrar(Campo.EPS, eps);
        campos.registrar(Campo.ACRESCIMO_TM, acrescimoTm);
        campos.registrar(Campo.DC_MAX_PCT, dcMax);
        campos.registrar(Campo.DENSIDADE_TESTE_PADRAO, densidadeTestePadrao);
        campos.registrar(Campo.TI_MINIMO_MARCHA_LENTA, tiMinimoMarchaLenta);
        campos.registrar(Campo.PRESSAO_BASE_BAR, pressaoBase);
        campos.registrar(Campo.BICO_VAZAO, bicoVazao);
        campos.registrar(Campo.BICO_PRESSAO_TESTE_BAR, bicoPressaoTeste);
        campos.registrar(Campo.BICO_DENSIDADE_TESTE, bicoDensidadeTeste);
        campos.registrar(Campo.BICO_TEMPO_MORTO, bicoTempoMorto);
        campos.registrar(Campo.BICO2_VAZAO, bico2Vazao);
        campos.registrar(Campo.BICO2_PRESSAO_TESTE_BAR, bico2PressaoTeste);
        campos.registrar(Campo.BICO2_DENSIDADE_TESTE, bico2DensidadeTeste);
        campos.registrar(Campo.BICO2_TEMPO_MORTO, bico2TempoMorto);
        campos.registrar(Campo.MARCHA_LENTA_MAP, marchaLentaMap);
        campos.registrar(Campo.MARCHA_LENTA_VE, marchaLentaVe);
        campos.registrar(Campo.BOMBA_VAZAO, bombaVazao);
        campos.registrar(Campo.SENSOR_MAP_KPA, sensorMapFaixa);
        campos.registrar(Campo.INJECAO_NOME, injecaoNome);
        campos.registrar(Campo.INJECAO_SAIDAS_BICO, injecaoSaidasBico);
        campos.registrar(Campo.INJECAO_SAIDAS_IGNICAO, injecaoSaidasIgnicao);
        campos.registrar(Campo.TENSAO_V, tensao);
    }

    private void configurarPecasDoCatalogo() {
        caixaBico = new CaixaCatalogo<>(catalogoBico, "(digitar os dados)");
        caixaBico2 = new CaixaCatalogo<>(catalogoBico2, "(digitar os dados)");
        caixaBomba = new CaixaCatalogo<>(catalogoBomba, "(digitar a vazão)");
        caixaIntercooler = new CaixaCatalogo<>(catalogoIntercooler, "(usar o eps digitado)");
        caixaSensorMap = new CaixaCatalogo<>(catalogoSensorMap, "(digitar a faixa)");
        caixaInjecao = new CaixaCatalogo<>(catalogoInjecao, "(digitar os dados)");
        caixaBico.aoMudar(() -> peca(() -> mostrarBico(caixaBico.escolhido(), bicoVazao, bicoPressaoTeste,
                bicoDensidadeTeste, bicoTempoMorto)));
        caixaBico2.aoMudar(() -> peca(() -> mostrarBico(caixaBico2.escolhido(), bico2Vazao, bico2PressaoTeste,
                bico2DensidadeTeste, bico2TempoMorto)));
        caixaBomba.aoMudar(() -> peca(() -> {
            if (caixaBomba.escolhido() != null) {
                bombaVazao.setText("");
            }
            bombaVazao.setPromptText(caixaBomba.escolhido() != null ? "pela curva da bomba do catálogo"
                    : "na pressão exigida (ver resultado)");
        }));
        caixaIntercooler.aoMudar(() -> peca(() -> {
            ItemCatalogo<Intercooler> item = caixaIntercooler.escolhido();
            if (item != null && item.dados().eficiencia() != null) {
                eps.setText(numero(item.dados().eficiencia()));
            }
        }));
        caixaSensorMap.aoMudar(() -> peca(() -> {
            if (caixaSensorMap.escolhido() != null) {
                sensorMapFaixa.setText(numero(caixaSensorMap.escolhido().dados().faixaMaxima()));
            }
        }));
        caixaInjecao.aoMudar(() -> peca(() -> {
            ItemCatalogo<Injecao> item = caixaInjecao.escolhido();
            if (item != null) {
                injecaoNome.setText(item.dados().nome());
                injecaoSaidasBico.setText(String.valueOf(item.dados().saidasBico()));
                injecaoSaidasIgnicao.setText(String.valueOf(item.dados().saidasIgnicao()));
            }
        }));
    }

    /** Escolheu (ou tirou) uma peça do catálogo: mostra os dados dela, trava os campos e desatualiza o resultado. */
    private void peca(Runnable mostrar) {
        mostrar.run();
        atualizarHabilitados();
        marcarDesatualizado();
    }

    /** Os campos do bico mostram o bico do catálogo (a tabela de tempo morto não cabe num campo só). */
    private static void mostrarBico(ItemCatalogo<Bico> item, TextField vazao, TextField pressao, TextField densidade,
            TextField tempoMorto) {
        if (item == null) {
            tempoMorto.setPromptText("na tensão de trabalho");
            return;
        }
        Bico bico = item.dados();
        vazao.setText(numero(bico.vazaoNominalCm3Min()));
        pressao.setText(numero(Conversoes.kpaParaBar(bico.pressaoTesteKpa())));
        densidade.setText(numero(bico.densidadeFluidoTesteKgL()));
        boolean fixo = bico.tempoMorto().eFixo();
        tempoMorto.setText(fixo ? numero(bico.tempoMorto().pontos().getFirst().tempoMs()) : "");
        tempoMorto.setPromptText(fixo ? "na tensão de trabalho"
                : "tabela do catálogo (" + bico.tempoMorto().pontos().size() + " pontos)");
    }

    private static String numero(double valor) {
        return Double.isFinite(valor) ? formatar(valor, 0, 4) : "";
    }

    /** Padrões de cálculo e campos neutros. */
    private void preencherPadroes() {
        pontoMedicao.setValue(PontoMedicao.VIRABREQUIM);
        caminhoAr.setValue(CaminhoAr.K_AR);
        temperaturaAmbiente.setText("25");
        Map<CampoPadrao, String> textos = FormularioPadroes.textos(padroes);
        CAMPOS_COM_PADRAO.forEach((padrao, alvos) -> alvos.forEach(campo -> campos.escrever(campo, textos.get(padrao))));
    }

    private void configurarReacoes() {
        pontoMedicao.valueProperty().addListener((obs, antes, depois) -> atualizarHabilitados());
        caminhoAr.valueProperty().addListener((obs, antes, depois) -> atualizarHabilitados());
        motorController.aoMudarBicosPorCilindro(this::atualizarHabilitados);
        motorController.aoMudarCombustivelOuAspiracao(
                (combustivel, aspiracao) -> lambda.setText(formatar(combustivel.lambdaPadrao(aspiracao.sobrealimentado()), 2)));
        motorController.aoMudar(this::marcarDesatualizado);
        campos.aoMudar(this::marcarDesatualizado);
        atualizarHabilitados();
    }

    /**
     * Passa a usar os padrões salvos em Configurações. Campo que ainda mostra o padrão anterior recebe o
     * novo; campo que o usuário já mudou fica como está. Limiares novos deixam o resultado na tela
     * desatualizado, porque as cores foram calculadas com os antigos.
     */
    void usarPadroes(Parametros novos) {
        Map<CampoPadrao, String> antes = FormularioPadroes.textos(padroes);
        Map<CampoPadrao, String> depois = FormularioPadroes.textos(novos);
        boolean limiaresMudaram = !novos.limiares().equals(padroes.limiares());
        padroes = novos;
        CAMPOS_COM_PADRAO.forEach((padrao, alvos) -> alvos.forEach(campo -> {
            if (campos.ler(campo).equals(antes.get(padrao))) {
                campos.escrever(campo, depois.get(padrao));
            }
        }));
        if (limiaresMudaram) {
            marcarDesatualizado();
        }
    }

    // ===================== Reações =====================

    private void atualizarHabilitados() {
        perdaTransmissao.setDisable(pontoMedicao.getValue() != PontoMedicao.RODA);
        kAr.setDisable(caminhoAr.getValue() != CaminhoAr.K_AR);
        bsfc.setDisable(caminhoAr.getValue() != CaminhoAr.BSFC);
        boolean duasBancadas = bicosPorCilindro() == 2;
        boolean bicoDoCatalogo = caixaBico.escolhido() != null;
        boolean bico2DoCatalogo = caixaBico2.escolhido() != null;
        for (TextField campo : List.of(bicoVazao, bicoPressaoTeste, bicoDensidadeTeste, bicoTempoMorto)) {
            campo.setDisable(bicoDoCatalogo);
        }
        for (TextField campo : List.of(bico2Vazao, bico2PressaoTeste, bico2DensidadeTeste, bico2TempoMorto)) {
            campo.setDisable(!duasBancadas || bico2DoCatalogo);
        }
        catalogoBico2.setDisable(!duasBancadas);
        botaoSugerirBico.setDisable(duasBancadas);
        bombaVazao.setDisable(caixaBomba.escolhido() != null);
        ItemCatalogo<Intercooler> intercooler = caixaIntercooler.escolhido();
        eps.setDisable(intercooler != null && intercooler.dados().eficiencia() != null);
        sensorMapFaixa.setDisable(caixaSensorMap.escolhido() != null);
        boolean injecaoDoCatalogo = caixaInjecao.escolhido() != null;
        injecaoNome.setDisable(injecaoDoCatalogo);
        injecaoSaidasBico.setDisable(injecaoDoCatalogo);
        injecaoSaidasIgnicao.setDisable(injecaoDoCatalogo);
    }

    private int bicosPorCilindro() {
        return motorDoProjeto != null ? motorDoProjeto.bicosPorCilindro() : motorController.bicosPorCilindro();
    }

    /** Qualquer mudança depois de um cálculo deixa o resultado desatualizado (regra 2.5). */
    private void marcarDesatualizado() {
        if (ultimoResultado == null || desatualizado) {
            return;
        }
        desatualizado = true;
        botaoPdf.setDisable(true);
        faixaDesatualizado = new Label("Entradas alteradas depois do cálculo: o resultado abaixo está "
                + "desatualizado. Clique em Calcular de novo.");
        faixaDesatualizado.getStyleClass().add("faixa-desatualizado");
        faixaDesatualizado.setWrapText(true);
        faixaDesatualizado.setMaxWidth(Double.MAX_VALUE);
        painelResultado.getChildren().addFirst(faixaDesatualizado);
        rotuloEstado.setText("Resultado desatualizado: calcule de novo.");
        avisarMudancaDeResultado();
    }

    // ===================== Ações =====================

    @FXML
    private void calcular() {
        PecasEscolhidas pecas = pecasEscolhidas();
        LeitorFormulario.Leitura leitura = ler(pecas, Map.of());
        if (!leitura.erros().isEmpty()) {
            mostrarErros(leitura.erros());
            return;
        }
        try {
            ultimoResultado = CalculoDimensionamento.calcular(leitura.entrada());
            ultimaEntrada = leitura.entrada();
            ultimasPecas = pecas;
            desatualizado = false;
            painelResultado.getChildren().setAll(painel.montar(ultimoResultado));
            botaoPdf.setDisable(false);
            long vermelhos = ultimoResultado.valor().semaforo().stream()
                    .filter(a -> a.cor() == Cor.VERMELHO).count();
            rotuloEstado.setText("Calculado às " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    + (vermelhos > 0 ? " · " + vermelhos + " item(ns) vermelho(s) precisam de justificativa para o PDF" : ""));
            abas.getSelectionModel().select(abaResultado);
            avisarMudancaDeResultado();
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
        } catch (CalculoNaoConvergiuException erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
    }

    /**
     * Lê a tela com estas peças do catálogo.
     *
     * @param semValor campos lidos como vazios (para sugerir uma peça sem a que está digitada)
     */
    private LeitorFormulario.Leitura ler(PecasEscolhidas pecas, Map<Campo, String> semValor) {
        Map<Campo, String> valores = motorDoProjeto == null ? valoresDosCampos() : campos.valores();
        valores.putAll(semValor);
        return motorDoProjeto == null
                ? LeitorFormulario.ler(valores, padroes, motorController.combustiveis(), pecas)
                : LeitorFormulario.lerComMotor(valores, padroes, motorDoProjeto, pecas);
    }

    // ===================== Peças do catálogo =====================

    /** Liga a calculadora ao catálogo: combustíveis do motor, peças e "Sugerir". */
    void usarCatalogo(CasosCatalogo casos) {
        this.catalogo = casos;
        motorController.usarCatalogo(casos);
        secaoCatalogo.setVisible(true);
        secaoCatalogo.setManaged(true);
        atualizarCatalogo();
    }

    /** Lê de novo as peças e os combustíveis (o catálogo pode ter mudado); as escolhas ficam, na versão atual. */
    void atualizarCatalogo() {
        if (catalogo == null) {
            return;
        }
        caixaBico.itens(catalogo.ativos(TipoCatalogo.BICO));
        caixaBico2.itens(catalogo.ativos(TipoCatalogo.BICO));
        caixaBomba.itens(catalogo.ativos(TipoCatalogo.BOMBA));
        caixaIntercooler.itens(catalogo.ativos(TipoCatalogo.INTERCOOLER));
        caixaSensorMap.itens(catalogo.ativos(TipoCatalogo.SENSOR).stream()
                .filter(item -> item.dados().tipo() == TipoSensor.MAP).toList());
        caixaInjecao.itens(catalogo.ativos(TipoCatalogo.INJECAO));
        motorController.atualizarCombustiveis();
    }

    /** As peças do catálogo escolhidas agora (o bico secundário só com duas bancadas). */
    PecasEscolhidas pecasEscolhidas() {
        return new PecasEscolhidas(caixaBico.escolhido(), bicosPorCilindro() == 2 ? caixaBico2.escolhido() : null,
                caixaBomba.escolhido(), caixaIntercooler.escolhido(), caixaSensorMap.escolhido(),
                caixaInjecao.escolhido());
    }

    /**
     * Escolhe as peças de um cálculo salvo, na versão atual do catálogo; uma peça arquivada depois do cálculo
     * continua escolhida (marcada "arquivado").
     */
    void escolherPecas(List<PecaUsada> pecas) {
        if (catalogo == null) {
            return;
        }
        for (PecaUsada peca : pecas) {
            switch (peca.papel()) {
                case BICO -> catalogo.buscar(TipoCatalogo.BICO, peca.itemId()).ifPresent(caixaBico::escolher);
                case BICO_SECUNDARIO -> catalogo.buscar(TipoCatalogo.BICO, peca.itemId()).ifPresent(caixaBico2::escolher);
                case BOMBA -> catalogo.buscar(TipoCatalogo.BOMBA, peca.itemId()).ifPresent(caixaBomba::escolher);
                case INTERCOOLER -> catalogo.buscar(TipoCatalogo.INTERCOOLER, peca.itemId())
                        .ifPresent(caixaIntercooler::escolher);
                case SENSOR_MAP -> catalogo.buscar(TipoCatalogo.SENSOR, peca.itemId()).ifPresent(caixaSensorMap::escolher);
                case INJECAO -> catalogo.buscar(TipoCatalogo.INJECAO, peca.itemId()).ifPresent(caixaInjecao::escolher);
                case COMBUSTIVEL -> {
                    // Vem com o motor do projeto.
                }
            }
        }
    }

    /** A injeção escolhida para o projeto (no assistente), enquanto não há cálculo salvo. */
    void escolherInjecao(long itemId) {
        if (catalogo != null) {
            catalogo.buscar(TipoCatalogo.INJECAO, itemId).ifPresent(caixaInjecao::escolher);
        }
    }

    /** Recursos da injeção que o projeto pede, além do sensor de etanol do motor (ex.: controle do câmbio). */
    void usarRecursosExigidos(Supplier<Set<Recurso>> recursos) {
        this.recursosExigidos = recursos;
    }

    @FXML
    private void sugerirBico() {
        Map<Campo, String> semBico = new EnumMap<>(Campo.class);
        for (Campo campo : List.of(Campo.BICO_VAZAO, Campo.BICO_PRESSAO_TESTE_BAR, Campo.BICO_DENSIDADE_TESTE,
                Campo.BICO_TEMPO_MORTO, Campo.BICO2_VAZAO, Campo.BICO2_PRESSAO_TESTE_BAR, Campo.BICO2_DENSIDADE_TESTE,
                Campo.BICO2_TEMPO_MORTO)) {
            semBico.put(campo, "");
        }
        entradaParaSugerir(pecasEscolhidas().semBicos(), semBico).ifPresent(entrada -> sugerir(() -> {
            SugestaoPecas.Sugestao<Bico> sugestao = SugestaoPecas.bico(catalogo.ativos(TipoCatalogo.BICO), entrada);
            sugestao.item().ifPresent(caixaBico::escolher);
            return sugestao;
        }));
    }

    @FXML
    private void sugerirBomba() {
        entradaParaSugerir(pecasEscolhidas().semBomba(), Map.of(Campo.BOMBA_VAZAO, ""))
                .ifPresent(entrada -> sugerir(() -> {
                    SugestaoPecas.Sugestao<Bomba> sugestao =
                            SugestaoPecas.bomba(catalogo.ativos(TipoCatalogo.BOMBA), entrada);
                    sugestao.item().ifPresent(caixaBomba::escolher);
                    return sugestao;
                }));
    }

    @FXML
    private void sugerirInjecao() {
        ConfiguracaoMotor motor = motorDoProjeto;
        if (motor == null) {
            LeitorFormulario.LeituraMotor leitura = motorController.ler();
            if (!leitura.erros().isEmpty()) {
                mostrarMensagensPecas("Para sugerir a injeção, preencha o motor antes:", leitura.erros(), "erro");
                return;
            }
            motor = leitura.motor();
        }
        Set<Recurso> exigidos = EnumSet.noneOf(Recurso.class);
        exigidos.addAll(recursosExigidos.get());
        if (motor.sensorEtanol()) {
            exigidos.add(Recurso.SENSOR_ETANOL);
        }
        ConfiguracaoMotor doMotor = motor;
        sugerir(() -> {
            SugestaoPecas.Sugestao<Injecao> sugestao = SugestaoPecas.injecao(catalogo.ativos(TipoCatalogo.INJECAO),
                    doMotor, padroes.gruposSimultaneo(), exigidos);
            sugestao.item().ifPresent(caixaInjecao::escolher);
            return sugestao;
        });
    }

    /** A entrada da tela sem a peça a sugerir; com erro de digitação, mostra o que corrigir. */
    private Optional<EntradaDimensionamento> entradaParaSugerir(PecasEscolhidas pecas, Map<Campo, String> semValor) {
        if (catalogo == null) {
            return Optional.empty();
        }
        LeitorFormulario.Leitura leitura = ler(pecas, semValor);
        if (!leitura.erros().isEmpty()) {
            mostrarMensagensPecas("Para sugerir, corrija antes:", leitura.erros(), "erro");
            return Optional.empty();
        }
        return Optional.of(leitura.entrada());
    }

    private void sugerir(Supplier<SugestaoPecas.Sugestao<?>> sugestao) {
        try {
            SugestaoPecas.Sugestao<?> feita = sugestao.get();
            mostrarMensagensPecas(feita.mensagem(), feita.avisos(), feita.item().isPresent() ? "ok" : "aviso-amarelo");
        } catch (EntradaInvalidaException erro) {
            mostrarMensagensPecas("Para sugerir, corrija antes:", erro.erros(), "erro");
        } catch (CalculoNaoConvergiuException erro) {
            mostrarMensagensPecas(erro.getMessage(), List.of(), "erro");
        }
    }

    private void mostrarMensagensPecas(String titulo, List<String> itens, String estilo) {
        mensagensPecas.getChildren().clear();
        Label principal = new Label(titulo);
        principal.getStyleClass().add(estilo);
        principal.setWrapText(true);
        mensagensPecas.getChildren().add(principal);
        for (String item : itens) {
            Label rotulo = new Label("• " + item);
            rotulo.getStyleClass().add(estilo.equals("ok") ? "aviso-amarelo" : estilo);
            rotulo.setWrapText(true);
            mensagensPecas.getChildren().add(rotulo);
        }
    }

    /** A mensagem do último "Sugerir" (para os testes). */
    List<String> mensagensPecas() {
        return mensagensPecas.getChildren().stream().map(no -> ((Label) no).getText()).toList();
    }

    private void mostrarErros(List<String> erros) {
        ultimoResultado = null;
        ultimaEntrada = null;
        botaoPdf.setDisable(true);
        painelResultado.getChildren().setAll(PainelResultado.erros(erros));
        rotuloEstado.setText(erros.size() + " problema(s) para corrigir.");
        abas.getSelectionModel().select(abaResultado);
        avisarMudancaDeResultado();
    }

    @FXML
    private void gerarPdf() {
        if (ultimoResultado == null || desatualizado) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now().withSecond(0).withNano(0);
        List<Justificativa> justificativas = new ArrayList<>();
        List<Indicador> faltando = new ArrayList<>();
        for (Map.Entry<Indicador, TextField> campo : painel.justificativas().entrySet()) {
            String texto = campo.getValue().getText();
            if (texto == null || texto.isBlank()) {
                faltando.add(campo.getKey());
            } else {
                justificativas.add(new Justificativa(campo.getKey(), texto, agora));
            }
        }
        if (!faltando.isEmpty()) {
            avisar(Alert.AlertType.WARNING, "Justifique os itens vermelhos",
                    "Para gerar o PDF, escreva a justificativa de: "
                            + faltando.stream().map(Indicador::rotulo).collect(Collectors.joining(", ")) + ".");
            painel.justificativas().get(faltando.getFirst()).requestFocus();
            return;
        }
        DadosRelatorio dados = new DadosRelatorio(ultimaEntrada, ultimoResultado, justificativas, Versao.atual(),
                agora, identificacao.get());
        botaoPdf.setDisable(true);
        boolean gerando = PdfMemoria.gerar(painelResultado.getScene().getWindow(), servicos, dados,
                "memoria-calibra", rotuloEstado::setText, () -> botaoPdf.setDisable(desatualizado));
        if (!gerando) {
            botaoPdf.setDisable(desatualizado);
        }
    }

    // ===================== Utilidades =====================

    // ===================== Modo projeto =====================

    /**
     * O cálculo que está na tela, em dia com os campos, o que foi escrito nas justificativas dos vermelhos e as peças
     * do catálogo usadas.
     */
    record CalculoNaTela(EntradaDimensionamento entrada, Resultado<Dimensionamento> resultado,
            Map<Indicador, String> justificativas, PecasEscolhidas pecas) {
    }

    /**
     * Modo projeto (aba Dimensionamento): tira a aba do motor e passa a calcular com este motor, a versão atual
     * do projeto. Se o motor mudar depois de um cálculo, o resultado da tela fica desatualizado.
     */
    void usarMotorDoProjeto(ConfiguracaoMotor motor) {
        boolean mudou = motorDoProjeto != null && !motorDoProjeto.equals(motor);
        motorDoProjeto = motor;
        abas.getTabs().remove(abaMotor);
        abaMeta.setText("Meta e peças");
        abaResultado.setText("Resultado");
        if (lambda.getText().isBlank() && motor.combustivel() != null && motor.aspiracao() != null) {
            lambda.setText(formatar(motor.combustivel().lambdaPadrao(motor.aspiracao().sobrealimentado()), 2));
        }
        atualizarHabilitados();
        if (mudou) {
            marcarDesatualizado();
        }
    }

    /** Cliente, veículo e motor que vão no cabeçalho do PDF. */
    void usarIdentificacao(Supplier<IdentificacaoProjeto> fornecedor) {
        identificacao = fornecedor;
    }

    /** Preenche meta, ar, parâmetros e peças com as entradas de um cálculo salvo; o motor fica o do projeto. */
    void preencherEntrada(EntradaDimensionamento entrada) {
        campos.preencher(EscritorFormulario.semMotor(entrada));
    }

    /** Preenche só a meta (projeto sem cálculo salvo). */
    void preencherMeta(Meta meta) {
        campos.preencher(EscritorFormulario.meta(meta));
    }

    /** O cálculo da tela, se houver e se os campos não mudaram depois dele. */
    Optional<CalculoNaTela> calculoNaTela() {
        if (ultimoResultado == null || desatualizado) {
            return Optional.empty();
        }
        Map<Indicador, String> textos = new EnumMap<>(Indicador.class);
        painel.justificativas().forEach((indicador, campo) -> textos.put(indicador, campo.getText()));
        return Optional.of(new CalculoNaTela(ultimaEntrada, ultimoResultado, textos, ultimasPecas));
    }

    /** Põe o cursor na justificativa de um vermelho. */
    void focarJustificativa(Indicador indicador) {
        abas.getSelectionModel().select(abaResultado);
        TextField campo = painel.justificativas().get(indicador);
        if (campo != null) {
            campo.requestFocus();
        }
    }

    /** Botão a mais no rodapé (ex.: "Salvar cálculo no projeto"). */
    void adicionarAcao(Node acao) {
        rodape.getChildren().add(rodape.getChildren().indexOf(botaoPdf) + 1, acao);
    }

    /** Aba a mais (ex.: o histórico de cálculos do projeto). */
    void adicionarAba(Tab aba) {
        abas.getTabs().add(aba);
    }

    void mostrarAba(Tab aba) {
        abas.getSelectionModel().select(aba);
    }

    /** Mensagem na barra de baixo (ex.: "Cálculo salvo no projeto"). */
    void mostrarEstado(String texto) {
        rotuloEstado.setText(texto);
    }

    /** Avisa quando o resultado da tela muda: calculado, com erro ou desatualizado. */
    void aoMudarResultado(Runnable acao) {
        aoMudarResultado.add(acao);
    }

    private void avisarMudancaDeResultado() {
        aoMudarResultado.forEach(Runnable::run);
    }

    /** Texto de todos os campos (motor, meta e peças), para o {@link LeitorFormulario}. */
    Map<Campo, String> valoresDosCampos() {
        Map<Campo, String> valores = new EnumMap<>(Campo.class);
        valores.putAll(motorController.valores());
        valores.putAll(campos.valores());
        return valores;
    }

    /** Preenche a tela a partir de um mapa campo → texto (usado pelo teste de interface). */
    void preencher(Map<Campo, String> valores) {
        motorController.preencher(valores);
        campos.preencher(valores);
    }

    private static void avisar(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alerta = new Alert(tipo, mensagem, ButtonType.OK);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }
}
