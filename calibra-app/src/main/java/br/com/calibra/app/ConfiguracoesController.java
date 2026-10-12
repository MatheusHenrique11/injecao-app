package br.com.calibra.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import br.com.calibra.app.casos.CampoPadrao;
import br.com.calibra.app.casos.FormularioPadroes;
import br.com.calibra.app.casos.LogoOficina;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.config.Chave;
import br.com.calibra.dados.config.ConfiguracaoRepositorio;

/** Tela de Configurações (SPEC 8.7): dados da oficina, padrões de cálculo e limiares do semáforo. */
public class ConfiguracoesController {

    private static final Logger LOG = Logger.getLogger(ConfiguracoesController.class.getName());

    /** Campo com valor diferente do padrão da SPEC: aparece em azul. */
    private static final PseudoClass EDITADO = PseudoClass.getPseudoClass("editado");

    // ---------- Oficina ----------
    @FXML private TextField oficinaNome;
    @FXML private TextField oficinaCnpj;
    @FXML private TextField oficinaEndereco;
    @FXML private TextField oficinaTelefone;
    @FXML private TextField oficinaEmail;
    @FXML private ImageView logoImagem;
    @FXML private Label logoInfo;
    @FXML private Button botaoRemoverLogo;
    @FXML private VBox mensagensOficina;

    // ---------- Padrões de cálculo ----------
    @FXML private TextField padraoKAr;
    @FXML private TextField padraoDcMax;
    @FXML private TextField padraoEtaC;
    @FXML private TextField padraoEps;
    @FXML private TextField padraoPerdaDescarga;
    @FXML private TextField padraoPerdaAdmissao;
    @FXML private TextField padraoAcrescimoTm;
    @FXML private TextField padraoDensidadeTeste;
    @FXML private TextField padraoTiMinimo;

    // ---------- Limiares do semáforo ----------
    @FXML private TextField limiarDcVerde;
    @FXML private TextField limiarDcAmarelo;
    @FXML private TextField limiarFolgaVerde;
    @FXML private TextField limiarFolgaAmarelo;
    @FXML private TextField limiarPistaoVerde;
    @FXML private TextField limiarPistaoAmarelo;
    @FXML private TextField limiarSensorVerde;
    @FXML private TextField limiarSensorAmarelo;
    @FXML private TextField limiarAspiradoVerde;
    @FXML private TextField limiarAspiradoAmarelo;
    @FXML private TextField limiarBmepAspirado;
    @FXML private TextField limiarBmepSobrealimentado;
    @FXML private TextField limiarBmepAmarelo;
    @FXML private VBox mensagensPadroes;

    private final Map<CampoPadrao, TextField> camposPadrao = new EnumMap<>(CampoPadrao.class);
    private final Map<CampoPadrao, String> textosDaSpec = FormularioPadroes.textos(Parametros.padrao());

    private ConfiguracaoRepositorio configuracao;
    private Path anexos;
    private Consumer<Parametros> aoSalvarPadroes = padroes -> { };

    private DadosOficina oficinaSalva = DadosOficina.vazia();
    private Parametros padroesSalvos = Parametros.padrao();
    /** Imagem escolhida para o logo e ainda não salva; null = sem troca. */
    private Path logoEscolhido;
    private boolean logoRemovido;

    @FXML
    private void initialize() {
        camposPadrao.put(CampoPadrao.K_AR, padraoKAr);
        camposPadrao.put(CampoPadrao.DC_MAX_PCT, padraoDcMax);
        camposPadrao.put(CampoPadrao.ETA_C, padraoEtaC);
        camposPadrao.put(CampoPadrao.EPS, padraoEps);
        camposPadrao.put(CampoPadrao.PERDA_DESCARGA_PSI, padraoPerdaDescarga);
        camposPadrao.put(CampoPadrao.PERDA_ADMISSAO_PSI, padraoPerdaAdmissao);
        camposPadrao.put(CampoPadrao.ACRESCIMO_TM, padraoAcrescimoTm);
        camposPadrao.put(CampoPadrao.DENSIDADE_TESTE, padraoDensidadeTeste);
        camposPadrao.put(CampoPadrao.TI_MINIMO, padraoTiMinimo);
        camposPadrao.put(CampoPadrao.DC_VERDE_PCT, limiarDcVerde);
        camposPadrao.put(CampoPadrao.DC_AMARELO_PCT, limiarDcAmarelo);
        camposPadrao.put(CampoPadrao.FOLGA_VERDE_PCT, limiarFolgaVerde);
        camposPadrao.put(CampoPadrao.FOLGA_AMARELO_PCT, limiarFolgaAmarelo);
        camposPadrao.put(CampoPadrao.PISTAO_VERDE, limiarPistaoVerde);
        camposPadrao.put(CampoPadrao.PISTAO_AMARELO, limiarPistaoAmarelo);
        camposPadrao.put(CampoPadrao.SENSOR_VERDE, limiarSensorVerde);
        camposPadrao.put(CampoPadrao.SENSOR_AMARELO, limiarSensorAmarelo);
        camposPadrao.put(CampoPadrao.ASPIRADO_VERDE, limiarAspiradoVerde);
        camposPadrao.put(CampoPadrao.ASPIRADO_AMARELO, limiarAspiradoAmarelo);
        camposPadrao.put(CampoPadrao.BMEP_ASPIRADO, limiarBmepAspirado);
        camposPadrao.put(CampoPadrao.BMEP_SOBREALIMENTADO, limiarBmepSobrealimentado);
        camposPadrao.put(CampoPadrao.BMEP_AMARELO_PCT, limiarBmepAmarelo);
        camposPadrao.forEach((campo, texto) -> texto.textProperty().addListener((obs, antes, depois) ->
                texto.pseudoClassStateChanged(EDITADO, !Objects.equals(depois, textosDaSpec.get(campo)))));
    }

    /**
     * Liga a tela aos dados salvos.
     *
     * @param aoSalvarPadroes avisado quando os padrões de cálculo são salvos (a calculadora passa a usá-los)
     */
    void iniciar(ConfiguracaoRepositorio configuracao, Path anexos, Consumer<Parametros> aoSalvarPadroes) {
        this.configuracao = configuracao;
        this.anexos = anexos;
        this.aoSalvarPadroes = aoSalvarPadroes;
        oficinaSalva = configuracao.ler(Chave.OFICINA);
        padroesSalvos = configuracao.ler(Chave.CALCULO);
        descartarOficina();
        descartarPadroes();
    }

    // ===================== Oficina =====================

    @FXML
    private void descartarOficina() {
        oficinaNome.setText(oficinaSalva.nome());
        oficinaCnpj.setText(oficinaSalva.cnpj());
        oficinaEndereco.setText(oficinaSalva.endereco());
        oficinaTelefone.setText(oficinaSalva.telefone());
        oficinaEmail.setText(oficinaSalva.email());
        logoEscolhido = null;
        logoRemovido = false;
        mostrarLogo(anexos == null ? null : LogoOficina.arquivo(oficinaSalva, anexos).orElse(null));
        if (oficinaSalva.logo() != null && logoImagem.getImage() == null) {
            logoInfo.setText("O arquivo do logo não foi encontrado na pasta de anexos. Escolha a imagem de novo.");
        }
        mensagensOficina.getChildren().clear();
    }

    @FXML
    private void escolherLogo() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Logo da oficina");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens PNG ou JPG", "*.png", "*.jpg", "*.jpeg"));
        File arquivo = escolha.showOpenDialog(logoImagem.getScene().getWindow());
        if (arquivo == null) {
            return;
        }
        Path imagem = arquivo.toPath();
        try {
            if (Files.size(imagem) > LogoOficina.TAMANHO_MAXIMO_BYTES) {
                mostrarErros(mensagensOficina, List.of("A imagem do logo passa de 5 MB. Use uma imagem menor."));
                return;
            }
        } catch (IOException erro) {
            mostrarErros(mensagensOficina, List.of("Não foi possível ler a imagem: " + erro.getMessage()));
            return;
        }
        if (!mostrarLogo(imagem)) {
            mostrarErros(mensagensOficina, List.of("Não foi possível abrir essa imagem. Use um arquivo PNG ou JPG."));
            return;
        }
        logoEscolhido = imagem;
        logoRemovido = false;
        mostrarOk(mensagensOficina, "Logo escolhido. Clique em Salvar para gravar.");
    }

    @FXML
    private void removerLogo() {
        logoEscolhido = null;
        logoRemovido = true;
        mostrarLogo(null);
        mostrarOk(mensagensOficina, "Logo removido. Clique em Salvar para gravar.");
    }

    @FXML
    private void salvarOficina() {
        DadosOficina digitada = new DadosOficina(oficinaNome.getText(), oficinaCnpj.getText(),
                oficinaEndereco.getText(), oficinaTelefone.getText(), oficinaEmail.getText(), null);
        List<String> erros = digitada.validar();
        if (!erros.isEmpty()) {
            mostrarErros(mensagensOficina, erros);
            return;
        }
        String logo;
        try {
            logo = logoRemovido ? null
                    : logoEscolhido != null ? LogoOficina.guardar(logoEscolhido, anexos) : oficinaSalva.logo();
        } catch (IllegalArgumentException erro) {
            mostrarErros(mensagensOficina, List.of(erro.getMessage()));
            return;
        } catch (IOException erro) {
            LOG.log(Level.WARNING, "Não foi possível copiar o logo", erro);
            mostrarErros(mensagensOficina, List.of("Não foi possível copiar o logo: " + erro.getMessage()));
            return;
        }
        DadosOficina nova = new DadosOficina(digitada.nome(), digitada.cnpj(), digitada.endereco(),
                digitada.telefone(), digitada.email(), logo);
        if (gravar(Chave.OFICINA, nova, mensagensOficina)) {
            oficinaSalva = nova;
            descartarOficina();
            mostrarOk(mensagensOficina, "Dados da oficina salvos.");
        }
    }

    /** Mostra o logo; devolve false se a imagem não abrir. */
    private boolean mostrarLogo(Path arquivo) {
        Image imagem = null;
        if (arquivo != null) {
            imagem = new Image(arquivo.toUri().toString(), 400, 160, true, true);
            if (imagem.isError()) {
                return false;
            }
        }
        logoImagem.setImage(imagem);
        logoImagem.setVisible(imagem != null);
        logoImagem.setManaged(imagem != null);
        logoInfo.setText(arquivo == null ? "Sem logo." : arquivo.getFileName().toString());
        botaoRemoverLogo.setDisable(arquivo == null);
        return true;
    }

    // ===================== Padrões de cálculo e limiares =====================

    @FXML
    private void descartarPadroes() {
        preencherPadroes(padroesSalvos);
        mensagensPadroes.getChildren().clear();
    }

    @FXML
    private void voltarAosPadroesDaSpec() {
        preencherPadroes(Parametros.padrao());
        mostrarOk(mensagensPadroes, "Campos com os padrões da SPEC. Clique em Salvar para gravar.");
    }

    @FXML
    private void salvarPadroes() {
        Map<CampoPadrao, String> textos = new EnumMap<>(CampoPadrao.class);
        camposPadrao.forEach((campo, texto) -> textos.put(campo, texto.getText()));
        FormularioPadroes.Leitura leitura = FormularioPadroes.ler(textos, padroesSalvos);
        if (!leitura.erros().isEmpty()) {
            mostrarErros(mensagensPadroes, leitura.erros());
            return;
        }
        if (gravar(Chave.CALCULO, leitura.parametros(), mensagensPadroes)) {
            padroesSalvos = leitura.parametros();
            preencherPadroes(padroesSalvos);
            aoSalvarPadroes.accept(padroesSalvos);
            long editados = camposPadrao.keySet().stream()
                    .filter(campo -> !camposPadrao.get(campo).getText().equals(textosDaSpec.get(campo))).count();
            mostrarOk(mensagensPadroes, "Padrões salvos. A calculadora já usa os valores novos"
                    + (editados == 0 ? " (todos iguais aos da SPEC)." : "; " + editados
                            + " valor(es) diferente(s) da SPEC, em azul."));
        }
    }

    private void preencherPadroes(Parametros parametros) {
        Map<CampoPadrao, String> textos = FormularioPadroes.textos(parametros);
        camposPadrao.forEach((campo, texto) -> texto.setText(textos.get(campo)));
    }

    // ===================== Utilidades =====================

    private <T> boolean gravar(Chave<T> chave, T valor, VBox mensagens) {
        try {
            configuracao.gravar(chave, valor);
            return true;
        } catch (ErroDados erro) {
            LOG.log(Level.SEVERE, "Não foi possível salvar a configuração " + chave.nome(), erro);
            mostrarErros(mensagens, List.of(erro.getMessage()));
            return false;
        }
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
}
