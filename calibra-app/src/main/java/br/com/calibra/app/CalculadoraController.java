package br.com.calibra.app;

import static br.com.calibra.core.util.NumeroBr.formatar;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import br.com.calibra.app.casos.CampoPadrao;
import br.com.calibra.app.casos.FormularioPadroes;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.CalculoNaoConvergiuException;
import br.com.calibra.core.calculo.CaminhoAr;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.projeto.PontoMedicao;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.relatorios.DadosRelatorio;
import br.com.calibra.relatorios.RelatorioMemoria;
import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Tela da calculadora (fase 1): o motor vem do formulário {@link MotorFormController}; aqui ficam a meta, as
 * peças, o resultado e o PDF.
 */
public class CalculadoraController {

    private static final DateTimeFormatter NOME_ARQUIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

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

    // ---------- Resultado e rodapé ----------
    @FXML private TabPane abas;
    @FXML private Tab abaResultado;
    @FXML private VBox painelResultado;
    @FXML private Button botaoCalcular;
    @FXML private Button botaoPdf;
    @FXML private Label rotuloEstado;

    private final CamposTela campos = new CamposTela();
    private final PainelResultado painel = new PainelResultado();
    /** Padrões de cálculo e limiares do semáforo em uso; sem Configurações (teste), os da SPEC. */
    private Parametros padroes = Parametros.padrao();
    private HostServices servicos;
    private EntradaDimensionamento ultimaEntrada;
    private Resultado<Dimensionamento> ultimoResultado;
    private boolean desatualizado;
    private Label faixaDesatualizado;

    void usarServicos(HostServices hostServices) {
        this.servicos = hostServices;
    }

    @FXML
    private void initialize() {
        configurarEscolhas();
        registrarCampos();
        preencherPadroes();
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
        boolean duasBancadas = motorController.bicosPorCilindro() == 2;
        bico2Vazao.setDisable(!duasBancadas);
        bico2PressaoTeste.setDisable(!duasBancadas);
        bico2DensidadeTeste.setDisable(!duasBancadas);
        bico2TempoMorto.setDisable(!duasBancadas);
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
    }

    // ===================== Ações =====================

    @FXML
    private void calcular() {
        LeitorFormulario.Leitura leitura = LeitorFormulario.ler(valoresDosCampos(), padroes);
        if (!leitura.erros().isEmpty()) {
            mostrarErros(leitura.erros());
            return;
        }
        try {
            ultimoResultado = CalculoDimensionamento.calcular(leitura.entrada());
            ultimaEntrada = leitura.entrada();
            desatualizado = false;
            painelResultado.getChildren().setAll(painel.montar(ultimoResultado));
            botaoPdf.setDisable(false);
            long vermelhos = ultimoResultado.valor().semaforo().stream()
                    .filter(a -> a.cor() == Cor.VERMELHO).count();
            rotuloEstado.setText("Calculado às " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    + (vermelhos > 0 ? " · " + vermelhos + " item(ns) vermelho(s) precisam de justificativa para o PDF" : ""));
            abas.getSelectionModel().select(abaResultado);
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
        } catch (CalculoNaoConvergiuException erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
    }

    private void mostrarErros(List<String> erros) {
        ultimoResultado = null;
        ultimaEntrada = null;
        botaoPdf.setDisable(true);
        painelResultado.getChildren().setAll(PainelResultado.erros(erros));
        rotuloEstado.setText(erros.size() + " problema(s) para corrigir.");
        abas.getSelectionModel().select(abaResultado);
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
        Optional<File> arquivo = escolherArquivo(agora);
        if (arquivo.isEmpty()) {
            return;
        }
        DadosRelatorio dados = new DadosRelatorio(ultimaEntrada, ultimoResultado, justificativas, Versao.atual(),
                agora);
        Path destino = arquivo.get().toPath();
        Task<Void> tarefa = new Task<>() {
            @Override
            protected Void call() throws Exception {
                RelatorioMemoria.gerar(dados, destino);
                return null;
            }
        };
        tarefa.setOnSucceeded(evento -> {
            botaoPdf.setDisable(desatualizado);
            rotuloEstado.setText("PDF salvo em " + destino);
            oferecerAbrir(destino);
        });
        tarefa.setOnFailed(evento -> {
            botaoPdf.setDisable(desatualizado);
            rotuloEstado.setText("O PDF não foi gerado.");
            Throwable erro = tarefa.getException();
            avisar(Alert.AlertType.ERROR, "Não foi possível gerar o PDF",
                    (erro == null ? "Erro desconhecido." : erro.getMessage())
                            + "\nSe o arquivo estiver aberto em outro programa, feche-o e tente de novo.");
        });
        botaoPdf.setDisable(true);
        rotuloEstado.setText("Gerando o PDF…");
        Thread linha = new Thread(tarefa, "gerar-pdf");
        linha.setDaemon(true);
        linha.start();
    }

    private Optional<File> escolherArquivo(LocalDateTime agora) {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Salvar memória de cálculo");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        escolha.setInitialFileName("memoria-calibra-" + agora.format(NOME_ARQUIVO) + ".pdf");
        Path documentos = Path.of(System.getProperty("user.home"), "Documents");
        if (Files.isDirectory(documentos)) {
            escolha.setInitialDirectory(documentos.toFile());
        }
        return Optional.ofNullable(escolha.showSaveDialog(painelResultado.getScene().getWindow()));
    }

    private void oferecerAbrir(Path arquivo) {
        ButtonType abrir = new ButtonType("Abrir", ButtonBar.ButtonData.OK_DONE);
        ButtonType fechar = new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, "PDF salvo em:\n" + arquivo, abrir, fechar);
        alerta.setTitle("Memória de cálculo");
        alerta.setHeaderText("PDF gerado.");
        Optional<ButtonType> resposta = alerta.showAndWait();
        if (resposta.isPresent() && resposta.get() == abrir && servicos != null) {
            servicos.showDocument(arquivo.toUri().toString());
        }
    }

    // ===================== Utilidades =====================

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
