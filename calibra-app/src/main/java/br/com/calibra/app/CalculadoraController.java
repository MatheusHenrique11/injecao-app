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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.CalculoNaoConvergiuException;
import br.com.calibra.core.calculo.CaminhoAr;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.memoria.Aviso;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.memoria.VersaoNucleo;
import br.com.calibra.core.motor.Arranjo;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.core.motor.Construcao;
import br.com.calibra.core.motor.ModoIgnicao;
import br.com.calibra.core.motor.ModoInjecao;
import br.com.calibra.core.motor.SugestoesMotor;
import br.com.calibra.core.motor.Validacao;
import br.com.calibra.core.motor.ValidadorMotor;
import br.com.calibra.core.projeto.PontoMedicao;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.relatorios.DadosRelatorio;
import br.com.calibra.relatorios.RelatorioMemoria;
import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

/** Tela da calculadora (fase 1): lê os campos, chama o núcleo, mostra o resultado e gera o PDF. */
public class CalculadoraController {

    private static final DateTimeFormatter NOME_ARQUIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    // ---------- Motor ----------
    @FXML private ComboBox<Integer> cilindros;
    @FXML private ComboBox<Arranjo> arranjo;
    @FXML private TextField anguloBancadas;
    @FXML private TextField diametro;
    @FXML private TextField curso;
    @FXML private Label cilindradaCalculada;
    @FXML private TextField cilindradaInformada;
    @FXML private TextField taxa;
    @FXML private ComboBox<Integer> valvulas;
    @FXML private ComboBox<Comando> comando;
    @FXML private TextField duracaoComando;
    @FXML private TextField cruzamento;
    @FXML private ComboBox<Aspiracao> aspiracao;
    @FXML private ComboBox<Construcao> pistoes;
    @FXML private ComboBox<Construcao> bielas;
    @FXML private TextField rotacaoMaxima;
    @FXML private TextField marchaLentaRpm;
    @FXML private TextField ordemIgnicao;
    @FXML private Label dicaOrdem;
    @FXML private TextField angulosPms;
    @FXML private ComboBox<Integer> bicosPorCilindro;
    @FXML private TextField bicosExtras;
    @FXML private ComboBox<ModoInjecao> modoInjecao;
    @FXML private ComboBox<ModoIgnicao> modoIgnicao;
    @FXML private TextField dentesTotais;
    @FXML private TextField dentesFaltantes;
    @FXML private CheckBox sensorFase;
    @FXML private ComboBox<Combustivel> combustivel;
    @FXML private Label infoCombustivel;
    @FXML private CheckBox sensorEtanol;
    @FXML private TextField altitude;
    @FXML private VBox mensagensMotor;

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
    @FXML private Label rotuloVersao;

    /** Leitura e escrita de cada campo da tela, como texto. */
    private record CampoTela(Supplier<String> ler, Consumer<String> escrever) {
    }

    private final Map<Campo, CampoTela> campos = new EnumMap<>(Campo.class);
    private final PainelResultado painel = new PainelResultado();
    private HostServices servicos;
    private EntradaDimensionamento ultimaEntrada;
    private Resultado<Dimensionamento> ultimoResultado;
    private boolean desatualizado;
    private boolean preenchendo;
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
        rotuloVersao.setText("Calibra " + CalibraApp.VERSAO + " · núcleo " + VersaoNucleo.ATUAL + " · offline");
        botaoPdf.setDisable(true);
        rotuloEstado.setText("Preencha o motor e a meta e clique em Calcular (Enter).");
    }

    // ===================== Montagem da tela =====================

    private void configurarEscolhas() {
        cilindros.getItems().setAll(Arrays.asList(3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        configurar(arranjo, Arrays.asList(Arranjo.values()), Arranjo::rotulo);
        valvulas.getItems().setAll(Arrays.asList(2, 3, 4, 5));
        configurar(comando, Arrays.asList(Comando.values()), Comando::rotulo);
        configurar(aspiracao, Arrays.asList(Aspiracao.values()), Aspiracao::rotulo);
        configurar(pistoes, Arrays.asList(Construcao.values()), Construcao::rotulo);
        configurar(bielas, Arrays.asList(Construcao.values()), Construcao::rotulo);
        bicosPorCilindro.getItems().setAll(Arrays.asList(1, 2));
        configurar(modoInjecao, Arrays.asList(ModoInjecao.values()), ModoInjecao::rotulo);
        configurar(modoIgnicao, Arrays.asList(ModoIgnicao.values()), ModoIgnicao::rotulo);
        configurar(combustivel, CombustiveisPadrao.todos(), Combustivel::nome);
        configurar(pontoMedicao, Arrays.asList(PontoMedicao.values()), PontoMedicao::rotulo);
        configurar(uso, Arrays.asList(Uso.values()), Uso::rotulo);
        configurar(caminhoAr, Arrays.asList(CaminhoAr.values()), CaminhoAr::rotulo);
    }

    private static <T> void configurar(ComboBox<T> combo, List<T> itens, Function<T, String> rotulo) {
        combo.getItems().setAll(itens);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : rotulo.apply(item);
            }

            @Override
            public T fromString(String texto) {
                return null;
            }
        });
    }

    private void registrarCampos() {
        registrar(Campo.CILINDROS, cilindros, String::valueOf);
        registrar(Campo.ARRANJO, arranjo, Enum::name);
        registrar(Campo.ANGULO_BANCADAS, anguloBancadas);
        registrar(Campo.DIAMETRO, diametro);
        registrar(Campo.CURSO, curso);
        registrar(Campo.CILINDRADA_INFORMADA, cilindradaInformada);
        registrar(Campo.TAXA, taxa);
        registrar(Campo.VALVULAS, valvulas, String::valueOf);
        registrar(Campo.COMANDO, comando, Enum::name);
        registrar(Campo.DURACAO_COMANDO, duracaoComando);
        registrar(Campo.CRUZAMENTO, cruzamento);
        registrar(Campo.ASPIRACAO, aspiracao, Enum::name);
        registrar(Campo.PISTOES, pistoes, Enum::name);
        registrar(Campo.BIELAS, bielas, Enum::name);
        registrar(Campo.ROTACAO_MAXIMA, rotacaoMaxima);
        registrar(Campo.MARCHA_LENTA_RPM, marchaLentaRpm);
        registrar(Campo.ORDEM_IGNICAO, ordemIgnicao);
        registrar(Campo.ANGULOS_PMS, angulosPms);
        registrar(Campo.BICOS_POR_CILINDRO, bicosPorCilindro, String::valueOf);
        registrar(Campo.BICOS_EXTRAS, bicosExtras);
        registrar(Campo.MODO_INJECAO, modoInjecao, Enum::name);
        registrar(Campo.MODO_IGNICAO, modoIgnicao, Enum::name);
        registrar(Campo.DENTES_TOTAIS, dentesTotais);
        registrar(Campo.DENTES_FALTANTES, dentesFaltantes);
        registrar(Campo.SENSOR_FASE, sensorFase);
        registrar(Campo.COMBUSTIVEL, combustivel, Combustivel::codigo);
        registrar(Campo.SENSOR_ETANOL, sensorEtanol);
        registrar(Campo.ALTITUDE, altitude);
        registrar(Campo.POTENCIA_CV, potencia);
        registrar(Campo.PONTO_MEDICAO, pontoMedicao, Enum::name);
        registrar(Campo.PERDA_TRANSMISSAO, perdaTransmissao);
        registrar(Campo.RPM_POTENCIA, rpmPotencia);
        registrar(Campo.RPM_TORQUE, rpmTorque);
        registrar(Campo.USO, uso, Enum::name);
        registrar(Campo.CAMINHO_AR, caminhoAr, Enum::name);
        registrar(Campo.K_AR, kAr);
        registrar(Campo.BSFC, bsfc);
        registrar(Campo.LAMBDA, lambda);
        registrar(Campo.VE, ve);
        registrar(Campo.TEMPERATURA_AMBIENTE, temperaturaAmbiente);
        registrar(Campo.PERDA_DESCARGA_PSI, perdaDescarga);
        registrar(Campo.PERDA_ADMISSAO_PSI, perdaAdmissao);
        registrar(Campo.ETA_C, etaC);
        registrar(Campo.EPS, eps);
        registrar(Campo.ACRESCIMO_TM, acrescimoTm);
        registrar(Campo.DC_MAX_PCT, dcMax);
        registrar(Campo.DENSIDADE_TESTE_PADRAO, densidadeTestePadrao);
        registrar(Campo.TI_MINIMO_MARCHA_LENTA, tiMinimoMarchaLenta);
        registrar(Campo.PRESSAO_BASE_BAR, pressaoBase);
        registrar(Campo.BICO_VAZAO, bicoVazao);
        registrar(Campo.BICO_PRESSAO_TESTE_BAR, bicoPressaoTeste);
        registrar(Campo.BICO_DENSIDADE_TESTE, bicoDensidadeTeste);
        registrar(Campo.BICO_TEMPO_MORTO, bicoTempoMorto);
        registrar(Campo.BICO2_VAZAO, bico2Vazao);
        registrar(Campo.BICO2_PRESSAO_TESTE_BAR, bico2PressaoTeste);
        registrar(Campo.BICO2_DENSIDADE_TESTE, bico2DensidadeTeste);
        registrar(Campo.BICO2_TEMPO_MORTO, bico2TempoMorto);
        registrar(Campo.MARCHA_LENTA_MAP, marchaLentaMap);
        registrar(Campo.MARCHA_LENTA_VE, marchaLentaVe);
        registrar(Campo.BOMBA_VAZAO, bombaVazao);
        registrar(Campo.SENSOR_MAP_KPA, sensorMapFaixa);
        registrar(Campo.INJECAO_NOME, injecaoNome);
        registrar(Campo.INJECAO_SAIDAS_BICO, injecaoSaidasBico);
        registrar(Campo.INJECAO_SAIDAS_IGNICAO, injecaoSaidasIgnicao);
    }

    private void registrar(Campo campo, TextField texto) {
        campos.put(campo, new CampoTela(texto::getText, texto::setText));
        texto.textProperty().addListener((obs, antes, depois) -> marcarDesatualizado());
    }

    private void registrar(Campo campo, CheckBox caixa) {
        campos.put(campo, new CampoTela(() -> String.valueOf(caixa.isSelected()),
                valor -> caixa.setSelected(Boolean.parseBoolean(valor))));
        caixa.selectedProperty().addListener((obs, antes, depois) -> marcarDesatualizado());
    }

    private <T> void registrar(Campo campo, ComboBox<T> combo, Function<T, String> chave) {
        campos.put(campo, new CampoTela(
                () -> combo.getValue() == null ? "" : chave.apply(combo.getValue()),
                valor -> combo.getItems().stream()
                        .filter(item -> chave.apply(item).equals(valor))
                        .findFirst()
                        .ifPresentOrElse(combo::setValue, () -> combo.setValue(null))));
        combo.valueProperty().addListener((obs, antes, depois) -> marcarDesatualizado());
    }

    /** Padrões da SPEC 7.1 e campos neutros. Dados técnicos do motor ficam em branco. */
    private void preencherPadroes() {
        Parametros p = Parametros.padrao();
        comando.setValue(Comando.ORIGINAL);
        pistoes.setValue(Construcao.ORIGINAL);
        bielas.setValue(Construcao.ORIGINAL);
        bicosPorCilindro.setValue(1);
        bicosExtras.setText("0");
        dentesFaltantes.setText("0");
        pontoMedicao.setValue(PontoMedicao.VIRABREQUIM);
        caminhoAr.setValue(CaminhoAr.K_AR);
        kAr.setText(formatar(p.kAr(), 1));
        temperaturaAmbiente.setText("25");
        perdaDescarga.setText(formatar(Conversoes.kpaParaPsi(p.perdaDescargaKpa()), 1));
        perdaAdmissao.setText(formatar(Conversoes.kpaParaPsi(p.perdaAdmissaoKpa()), 1));
        etaC.setText(formatar(p.etaC(), 2));
        eps.setText(formatar(p.eps(), 2));
        acrescimoTm.setText(formatar(p.acrescimoTmAspiradoC(), 0));
        dcMax.setText(formatar(p.dcMax() * 100, 0));
        densidadeTestePadrao.setText(formatar(p.densidadeFluidoTesteKgL(), 2));
        tiMinimoMarchaLenta.setText(formatar(p.tiMinimoMarchaLentaMs(), 1));
        bicoDensidadeTeste.setText(formatar(p.densidadeFluidoTesteKgL(), 2));
        bico2DensidadeTeste.setText(formatar(p.densidadeFluidoTesteKgL(), 2));
        cilindros.setValue(4);
    }

    private void configurarReacoes() {
        cilindros.valueProperty().addListener((obs, antes, depois) -> aoMudarCilindros(antes, depois));
        aplicarSugestoes(4);
        arranjo.valueProperty().addListener((obs, antes, depois) -> atualizarHabilitados());
        pontoMedicao.valueProperty().addListener((obs, antes, depois) -> atualizarHabilitados());
        caminhoAr.valueProperty().addListener((obs, antes, depois) -> atualizarHabilitados());
        bicosPorCilindro.valueProperty().addListener((obs, antes, depois) -> atualizarHabilitados());
        combustivel.valueProperty().addListener((obs, antes, depois) -> aoMudarCombustivelOuAspiracao());
        aspiracao.valueProperty().addListener((obs, antes, depois) -> aoMudarCombustivelOuAspiracao());
        diametro.textProperty().addListener((obs, antes, depois) -> atualizarCilindrada());
        curso.textProperty().addListener((obs, antes, depois) -> atualizarCilindrada());
        atualizarHabilitados();
        atualizarCilindrada();
    }

    // ===================== Reações =====================

    /** Mudou o número de cilindros: refaz as sugestões, perguntando se o usuário já tinha editado. */
    private void aoMudarCilindros(Integer antes, Integer depois) {
        if (depois == null) {
            return;
        }
        boolean editado = antes != null
                && !(ordemIgnicao.getText().equals(textoOrdem(SugestoesMotor.ordemIgnicao(antes)))
                        && angulosPms.getText().equals(textoAngulos(SugestoesMotor.angulosPms(antes))))
                && !ordemIgnicao.getText().isBlank();
        boolean trocar = preenchendo || !editado || confirmar("Trocar a ordem de ignição e os ângulos?",
                "Você mudou para " + depois + " cilindros. Trocar a ordem de ignição e os ângulos dos PMS pelas "
                        + "sugestões para " + depois + " cilindros?");
        if (trocar) {
            aplicarSugestoes(depois);
        } else {
            atualizarModosPermitidos(depois);
        }
        atualizarCilindrada();
    }

    private void aplicarSugestoes(int n) {
        ordemIgnicao.setText(textoOrdem(SugestoesMotor.ordemIgnicao(n)));
        angulosPms.setText(textoAngulos(SugestoesMotor.angulosPms(n)));
        arranjo.setValue(SugestoesMotor.arranjo(n));
        Double angulo = SugestoesMotor.anguloEntreBancadas(n);
        anguloBancadas.setText(angulo == null ? "" : formatar(angulo, 0));
        atualizarModosPermitidos(n);
        dicaOrdem.setText(SugestoesMotor.CONFIRMAR
                + (SugestoesMotor.ordemGenerica(n) ? " Ordem genérica para " + n + " cilindros." : ""));
    }

    /** Número ímpar de cilindros: só sequencial ou distribuidor na ignição e sequencial ou simultâneo na injeção. */
    private void atualizarModosPermitidos(int n) {
        boolean impar = n % 2 != 0;
        List<ModoInjecao> injecoes = impar
                ? List.of(ModoInjecao.SEQUENCIAL, ModoInjecao.SIMULTANEO)
                : Arrays.asList(ModoInjecao.values());
        List<ModoIgnicao> ignicoes = impar
                ? List.of(ModoIgnicao.SEQUENCIAL, ModoIgnicao.DISTRIBUIDOR)
                : Arrays.asList(ModoIgnicao.values());
        ModoInjecao injecaoAtual = modoInjecao.getValue();
        ModoIgnicao ignicaoAtual = modoIgnicao.getValue();
        modoInjecao.getItems().setAll(injecoes);
        modoIgnicao.getItems().setAll(ignicoes);
        modoInjecao.setValue(injecoes.contains(injecaoAtual) ? injecaoAtual : ModoInjecao.SEQUENCIAL);
        modoIgnicao.setValue(ignicoes.contains(ignicaoAtual) ? ignicaoAtual : ModoIgnicao.SEQUENCIAL);
    }

    private void atualizarHabilitados() {
        Arranjo a = arranjo.getValue();
        anguloBancadas.setDisable(a == null || !a.exigeAnguloEntreBancadas());
        perdaTransmissao.setDisable(pontoMedicao.getValue() != PontoMedicao.RODA);
        kAr.setDisable(caminhoAr.getValue() != CaminhoAr.K_AR);
        bsfc.setDisable(caminhoAr.getValue() != CaminhoAr.BSFC);
        boolean duasBancadas = Integer.valueOf(2).equals(bicosPorCilindro.getValue());
        bico2Vazao.setDisable(!duasBancadas);
        bico2PressaoTeste.setDisable(!duasBancadas);
        bico2DensidadeTeste.setDisable(!duasBancadas);
        bico2TempoMorto.setDisable(!duasBancadas);
    }

    /** Combustível ou aspiração mudou: mostra os dados do combustível e preenche a lambda padrão. */
    private void aoMudarCombustivelOuAspiracao() {
        Combustivel c = combustivel.getValue();
        if (c == null) {
            infoCombustivel.setText("");
            return;
        }
        String vigencia = c.vigenteDesde() == null ? ""
                : " · vigente desde " + c.vigenteDesde().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        infoCombustivel.setText("AFR " + formatar(c.afrEstequiometrico(), 2) + ":1 · " + formatar(c.densidadeKgL(), 3)
                + " kg/L" + vigencia + " · fonte: " + c.fonte()
                + (c.avisoLambda() != null ? "\n" + c.avisoLambda() : ""));
        Aspiracao a = aspiracao.getValue();
        if (a != null && !preenchendo) {
            lambda.setText(formatar(c.lambdaPadrao(a.sobrealimentado()), 2));
        }
    }

    private void atualizarCilindrada() {
        try {
            double d = NumeroBr.ler(diametro.getText());
            double s = NumeroBr.ler(curso.getText());
            Integer n = cilindros.getValue();
            if (n == null || d <= 0 || s <= 0) {
                cilindradaCalculada.setText("—");
                return;
            }
            double cc = Math.PI / 4 * d * d * s * n / 1000.0;
            cilindradaCalculada.setText(formatar(cc, 0) + " cm³ (" + formatar(cc / 1000.0, 2) + " L)");
        } catch (NumberFormatException erro) {
            cilindradaCalculada.setText("—");
        }
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
    private void conferirMotor() {
        mensagensMotor.getChildren().clear();
        LeitorFormulario.LeituraMotor leitura = LeitorFormulario.lerMotor(valoresDosCampos());
        if (!leitura.erros().isEmpty()) {
            mostrarMensagensMotor(leitura.erros(), List.of());
            return;
        }
        Validacao validacao = ValidadorMotor.validar(leitura.motor());
        mostrarMensagensMotor(validacao.erros(), validacao.avisos());
    }

    private void mostrarMensagensMotor(List<String> erros, List<Aviso> avisos) {
        if (erros.isEmpty() && avisos.isEmpty()) {
            Label ok = new Label("Motor válido.");
            ok.getStyleClass().add("ok");
            mensagensMotor.getChildren().add(ok);
        }
        for (String erro : erros) {
            Label rotulo = new Label("• " + erro);
            rotulo.getStyleClass().add("erro");
            rotulo.setWrapText(true);
            mensagensMotor.getChildren().add(rotulo);
        }
        for (Aviso aviso : avisos) {
            Label rotulo = new Label("• [" + aviso.nivel().rotulo() + "] " + aviso.mensagem());
            rotulo.getStyleClass().add("aviso-amarelo");
            rotulo.setWrapText(true);
            mensagensMotor.getChildren().add(rotulo);
        }
    }

    @FXML
    private void calcular() {
        LeitorFormulario.Leitura leitura = LeitorFormulario.ler(valoresDosCampos());
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
        DadosRelatorio dados = new DadosRelatorio(ultimaEntrada, ultimoResultado, justificativas, CalibraApp.VERSAO,
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

    /** Texto de todos os campos, para o {@link LeitorFormulario}. */
    Map<Campo, String> valoresDosCampos() {
        Map<Campo, String> valores = new EnumMap<>(Campo.class);
        campos.forEach((campo, tela) -> valores.put(campo, tela.ler().get()));
        return valores;
    }

    /** Preenche a tela a partir de um mapa campo → texto (usado pelo teste de interface). */
    void preencher(Map<Campo, String> valores) {
        preenchendo = true;
        try {
            new EnumMap<>(valores).forEach((campo, valor) -> {
                CampoTela tela = campos.get(campo);
                if (tela != null) {
                    tela.escrever().accept(valor);
                }
            });
        } finally {
            preenchendo = false;
        }
    }

    private static String textoOrdem(List<Integer> ordem) {
        return ordem.stream().map(String::valueOf).collect(Collectors.joining("-"));
    }

    private static String textoAngulos(List<Double> angulos) {
        return angulos.stream()
                .map(a -> a == Math.rint(a) ? formatar(a, 0) : formatar(a, 1))
                .collect(Collectors.joining("; "));
    }

    private static boolean confirmar(String titulo, String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, mensagem, ButtonType.YES, ButtonType.NO);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        return alerta.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    private static void avisar(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alerta = new Alert(tipo, mensagem, ButtonType.OK);
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }
}
