package br.com.calibra.app;

import static br.com.calibra.core.util.NumeroBr.formatar;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.memoria.Aviso;
import br.com.calibra.core.motor.Arranjo;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.Construcao;
import br.com.calibra.core.motor.ModoIgnicao;
import br.com.calibra.core.motor.ModoInjecao;
import br.com.calibra.core.motor.SugestoesMotor;
import br.com.calibra.core.motor.Validacao;
import br.com.calibra.core.motor.ValidadorMotor;
import br.com.calibra.core.util.NumeroBr;

/**
 * Formulário do motor (SPEC 6.3), usado na calculadora, no assistente de novo projeto e na aba Motor. Ao mudar
 * o número de cilindros, sugere ordem de ignição e ângulos de novo e pede confirmação se já foram editados.
 */
public class MotorFormController {

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

    private final CamposTela campos = new CamposTela();
    private BiConsumer<Combustivel, Aspiracao> aoMudarCombustivel = (c, a) -> { };
    private boolean preenchendo;

    @FXML
    private void initialize() {
        configurarEscolhas();
        registrarCampos();
        preencherPadroes();
        configurarReacoes();
    }

    // ===================== Para quem usa o formulário =====================

    /** Texto de todos os campos do motor, para o {@link LeitorFormulario}. */
    Map<Campo, String> valores() {
        return campos.valores();
    }

    /** Escreve os campos do motor que vierem no mapa; os outros ficam. Não pergunta nada. */
    void preencher(Map<Campo, String> valores) {
        preenchendo = true;
        try {
            campos.preencher(valores);
        } finally {
            preenchendo = false;
        }
    }

    /** Mostra um motor gravado (versão do projeto ou do catálogo). */
    void preencher(ConfiguracaoMotor motor) {
        preencher(EscritorFormulario.motor(motor));
        mensagensMotor.getChildren().clear();
    }

    /** Lê o motor dos campos; erros de digitação viram mensagens. */
    LeitorFormulario.LeituraMotor ler() {
        return LeitorFormulario.lerMotor(valores());
    }

    /** Avisa quando qualquer campo do motor muda. */
    void aoMudar(Runnable acao) {
        campos.aoMudar(acao);
    }

    /** Avisa quando o usuário troca o combustível ou a aspiração (a calculadora sugere a lambda). */
    void aoMudarCombustivelOuAspiracao(BiConsumer<Combustivel, Aspiracao> acao) {
        aoMudarCombustivel = acao;
    }

    /** Quantos bicos por cilindro estão escolhidos agora (1 ou 2). */
    int bicosPorCilindro() {
        return bicosPorCilindro.getValue() == null ? 1 : bicosPorCilindro.getValue();
    }

    void aoMudarBicosPorCilindro(Runnable acao) {
        bicosPorCilindro.valueProperty().addListener((obs, antes, depois) -> acao.run());
    }

    // ===================== Montagem =====================

    private void configurarEscolhas() {
        cilindros.getItems().setAll(Arrays.asList(3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        CamposTela.configurar(arranjo, Arrays.asList(Arranjo.values()), Arranjo::rotulo);
        valvulas.getItems().setAll(Arrays.asList(2, 3, 4, 5));
        CamposTela.configurar(comando, Arrays.asList(Comando.values()), Comando::rotulo);
        CamposTela.configurar(aspiracao, Arrays.asList(Aspiracao.values()), Aspiracao::rotulo);
        CamposTela.configurar(pistoes, Arrays.asList(Construcao.values()), Construcao::rotulo);
        CamposTela.configurar(bielas, Arrays.asList(Construcao.values()), Construcao::rotulo);
        bicosPorCilindro.getItems().setAll(Arrays.asList(1, 2));
        CamposTela.configurar(modoInjecao, Arrays.asList(ModoInjecao.values()), ModoInjecao::rotulo);
        CamposTela.configurar(modoIgnicao, Arrays.asList(ModoIgnicao.values()), ModoIgnicao::rotulo);
        CamposTela.configurar(combustivel, CombustiveisPadrao.todos(), Combustivel::nome);
    }

    private void registrarCampos() {
        campos.registrar(Campo.CILINDROS, cilindros, String::valueOf);
        campos.registrar(Campo.ARRANJO, arranjo, Enum::name);
        campos.registrar(Campo.ANGULO_BANCADAS, anguloBancadas);
        campos.registrar(Campo.DIAMETRO, diametro);
        campos.registrar(Campo.CURSO, curso);
        campos.registrar(Campo.CILINDRADA_INFORMADA, cilindradaInformada);
        campos.registrar(Campo.TAXA, taxa);
        campos.registrar(Campo.VALVULAS, valvulas, String::valueOf);
        campos.registrar(Campo.COMANDO, comando, Enum::name);
        campos.registrar(Campo.DURACAO_COMANDO, duracaoComando);
        campos.registrar(Campo.CRUZAMENTO, cruzamento);
        campos.registrar(Campo.ASPIRACAO, aspiracao, Enum::name);
        campos.registrar(Campo.PISTOES, pistoes, Enum::name);
        campos.registrar(Campo.BIELAS, bielas, Enum::name);
        campos.registrar(Campo.ROTACAO_MAXIMA, rotacaoMaxima);
        campos.registrar(Campo.MARCHA_LENTA_RPM, marchaLentaRpm);
        campos.registrar(Campo.ORDEM_IGNICAO, ordemIgnicao);
        campos.registrar(Campo.ANGULOS_PMS, angulosPms);
        campos.registrar(Campo.BICOS_POR_CILINDRO, bicosPorCilindro, String::valueOf);
        campos.registrar(Campo.BICOS_EXTRAS, bicosExtras);
        campos.registrar(Campo.MODO_INJECAO, modoInjecao, Enum::name);
        campos.registrar(Campo.MODO_IGNICAO, modoIgnicao, Enum::name);
        campos.registrar(Campo.DENTES_TOTAIS, dentesTotais);
        campos.registrar(Campo.DENTES_FALTANTES, dentesFaltantes);
        campos.registrar(Campo.SENSOR_FASE, sensorFase);
        campos.registrar(Campo.COMBUSTIVEL, combustivel, Combustivel::codigo);
        campos.registrar(Campo.SENSOR_ETANOL, sensorEtanol);
        campos.registrar(Campo.ALTITUDE, altitude);
    }

    /** Campos neutros. Dados técnicos do motor ficam em branco (regra: não inventar dado técnico). */
    private void preencherPadroes() {
        comando.setValue(Comando.ORIGINAL);
        pistoes.setValue(Construcao.ORIGINAL);
        bielas.setValue(Construcao.ORIGINAL);
        bicosPorCilindro.setValue(1);
        bicosExtras.setText("0");
        dentesFaltantes.setText("0");
        cilindros.setValue(4);
    }

    private void configurarReacoes() {
        cilindros.valueProperty().addListener((obs, antes, depois) -> aoMudarCilindros(antes, depois));
        aplicarSugestoes(4);
        arranjo.valueProperty().addListener((obs, antes, depois) -> atualizarHabilitados());
        combustivel.valueProperty().addListener((obs, antes, depois) -> aoMudarCombustivelOuAspiracao());
        aspiracao.valueProperty().addListener((obs, antes, depois) -> aoMudarCombustivelOuAspiracao());
        diametro.textProperty().addListener((obs, antes, depois) -> atualizarCilindrada());
        curso.textProperty().addListener((obs, antes, depois) -> atualizarCilindrada());
        cilindros.valueProperty().addListener((obs, antes, depois) -> atualizarCilindrada());
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
    }

    /** Mostra os dados do combustível e avisa quem usa o formulário (a calculadora sugere a lambda). */
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
        if (!preenchendo && aspiracao.getValue() != null) {
            aoMudarCombustivel.accept(c, aspiracao.getValue());
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

    // ===================== Conferir =====================

    @FXML
    private void conferirMotor() {
        mensagensMotor.getChildren().clear();
        LeitorFormulario.LeituraMotor leitura = ler();
        if (!leitura.erros().isEmpty()) {
            mostrarMensagens(leitura.erros(), List.of());
            return;
        }
        Validacao validacao = ValidadorMotor.validar(leitura.motor());
        mostrarMensagens(validacao.erros(), validacao.avisos());
    }

    /** Mostra erros e avisos do motor embaixo do formulário (também usado ao salvar no projeto). */
    void mostrarMensagens(List<String> erros, List<Aviso> avisos) {
        mensagensMotor.getChildren().clear();
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

    // ===================== Utilidades =====================

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
}
