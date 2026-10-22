package br.com.calibra.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.HostServices;
import javafx.beans.binding.BooleanBinding;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import br.com.calibra.app.casos.CampoPadrao;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.app.casos.FormularioOrcamento;
import br.com.calibra.app.casos.FormularioPadroes;
import br.com.calibra.app.casos.LogoOficina;
import br.com.calibra.app.casos.ServicoBackup;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.backup.BackupInvalidoException;
import br.com.calibra.dados.backup.InfoBackup;
import br.com.calibra.dados.backup.MotivoBackup;
import br.com.calibra.dados.config.Chave;
import br.com.calibra.dados.config.ConfiguracaoRepositorio;

/**
 * Tela de Configurações (SPEC 8.7): dados da oficina, padrões de cálculo, limiares do semáforo, padrões do orçamento
 * e backup.
 */
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
    @FXML private TextField limiarCambioVerde;
    @FXML private TextField limiarCambioAmarelo;
    @FXML private TextField limiarCompressorSurge;
    @FXML private TextField limiarCompressorChoke;
    @FXML private TextField limiarCompressorRotacao;
    @FXML private TextField limiarCompressorEficiencia;
    @FXML private VBox mensagensPadroes;

    // ---------- Orçamento ----------
    @FXML private ComboBox<FormularioOrcamento.TipoDesconto> orcamentoTipoDesconto;
    @FXML private TextField orcamentoDesconto;
    @FXML private TextField orcamentoValorHora;
    @FXML private TextField orcamentoPrazoDias;
    @FXML private TextField orcamentoValidadeDias;
    @FXML private TextField orcamentoPrazoObservacao;
    @FXML private TextArea orcamentoCondicoes;
    @FXML private VBox mensagensOrcamento;

    // ---------- Backup ----------
    @FXML private TabPane abasConfiguracoes;
    @FXML private Tab abaBackup;
    @FXML private ConfiguracoesOsController configuracoesOsController;
    @FXML private Label backupPasta;
    @FXML private Label backupAvisoPasta;
    @FXML private Label backupUltimo;
    @FXML private Label backupAnexos;
    @FXML private Button botaoEscolherPasta;
    @FXML private Button botaoPastaPadrao;
    @FXML private Button botaoBackupAgora;
    @FXML private Button botaoRestaurar;
    @FXML private VBox mensagensBackup;

    private final Map<CampoPadrao, TextField> camposPadrao = new EnumMap<>(CampoPadrao.class);
    private final Map<CampoPadrao, String> textosDaSpec = FormularioPadroes.textos(Parametros.padrao());

    private ConfiguracaoRepositorio configuracao;
    private Path anexos;
    private Consumer<Parametros> aoSalvarPadroes = padroes -> { };
    private ServicoBackup servicoBackup;
    private TarefasFundo tarefas;
    private HostServices servicos;
    private Runnable aposRestaurar = () -> { };

    private DadosOficina oficinaSalva = DadosOficina.vazia();
    private Parametros padroesSalvos = Parametros.padrao();
    private PadroesOrcamento orcamentoSalvo = PadroesOrcamento.padrao();
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
        camposPadrao.put(CampoPadrao.CAMBIO_VERDE_PCT, limiarCambioVerde);
        camposPadrao.put(CampoPadrao.CAMBIO_AMARELO_PCT, limiarCambioAmarelo);
        camposPadrao.put(CampoPadrao.COMPRESSOR_SURGE_PCT, limiarCompressorSurge);
        camposPadrao.put(CampoPadrao.COMPRESSOR_CHOKE_PCT, limiarCompressorChoke);
        camposPadrao.put(CampoPadrao.COMPRESSOR_ROTACAO_PCT, limiarCompressorRotacao);
        camposPadrao.put(CampoPadrao.COMPRESSOR_EFICIENCIA_PONTOS, limiarCompressorEficiencia);
        camposPadrao.forEach((campo, texto) -> texto.textProperty().addListener((obs, antes, depois) ->
                texto.pseudoClassStateChanged(EDITADO, !Objects.equals(depois, textosDaSpec.get(campo)))));
        CamposTela.configurar(orcamentoTipoDesconto, Arrays.asList(FormularioOrcamento.TipoDesconto.values()),
                FormularioOrcamento.TipoDesconto::rotulo);
        orcamentoDesconto.disableProperty().bind(orcamentoTipoDesconto.valueProperty()
                .isEqualTo(FormularioOrcamento.TipoDesconto.NENHUM));
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
        orcamentoSalvo = configuracao.ler(Chave.ORCAMENTO);
        descartarOficina();
        descartarPadroes();
        descartarPadroesOrcamento();
    }

    /**
     * Liga a aba Backup.
     *
     * @param aposRestaurar chamado depois de restaurar um backup, para as telas lerem os dados restaurados
     */
    void usarBackup(ServicoBackup servicoBackup, TarefasFundo tarefas, HostServices servicos, Runnable aposRestaurar) {
        this.servicoBackup = servicoBackup;
        this.tarefas = tarefas;
        this.servicos = servicos;
        this.aposRestaurar = aposRestaurar;
        BooleanBinding ocupado = tarefas.emAndamento().greaterThan(0);
        botaoBackupAgora.disableProperty().bind(ocupado);
        botaoRestaurar.disableProperty().bind(ocupado);
        botaoEscolherPasta.disableProperty().bind(ocupado);
        atualizarBackup();
    }

    /** Liga a aba OS (bloco 3.3): os padrões das OS e os mecânicos. Chamar depois de {@link #iniciar}. */
    void usarOficina(CasosOficina oficina) {
        configuracoesOsController.iniciar(configuracao, oficina);
    }

    /** Abre na aba Backup (depois de restaurar). */
    void mostrarAbaBackup() {
        abasConfiguracoes.getSelectionModel().select(abaBackup);
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

    // ===================== Orçamento =====================

    @FXML
    private void descartarPadroesOrcamento() {
        FormularioOrcamento.TextosPadroes textos = FormularioOrcamento.escrever(orcamentoSalvo);
        orcamentoTipoDesconto.setValue(textos.tipoDesconto());
        orcamentoDesconto.setText(textos.desconto());
        orcamentoValorHora.setText(textos.valorHora());
        orcamentoPrazoDias.setText(textos.prazoDias());
        orcamentoValidadeDias.setText(textos.validadeDias());
        orcamentoPrazoObservacao.setText(textos.prazoObservacao());
        orcamentoCondicoes.setText(textos.condicoes());
        mensagensOrcamento.getChildren().clear();
    }

    @FXML
    private void salvarPadroesOrcamento() {
        FormularioOrcamento.LeituraPadroes leitura = FormularioOrcamento.ler(new FormularioOrcamento.TextosPadroes(
                orcamentoTipoDesconto.getValue(), orcamentoDesconto.getText(), orcamentoPrazoDias.getText(),
                orcamentoPrazoObservacao.getText(), orcamentoValidadeDias.getText(), orcamentoCondicoes.getText(),
                orcamentoValorHora.getText()));
        if (!leitura.erros().isEmpty()) {
            mostrarErros(mensagensOrcamento, leitura.erros());
            return;
        }
        if (gravar(Chave.ORCAMENTO, leitura.padroes(), mensagensOrcamento)) {
            orcamentoSalvo = leitura.padroes();
            descartarPadroesOrcamento();
            mostrarOk(mensagensOrcamento, "Padrões do orçamento salvos. Valem para os próximos orçamentos.");
        }
    }

    // ===================== Backup =====================

    /** Mostra a pasta, o aviso de pen drive desligado, o último backup e o espaço dos anexos guardados. */
    void atualizarBackup() {
        if (servicoBackup == null) {
            return;
        }
        Path pasta = servicoBackup.pasta();
        backupPasta.setText(pasta + (pasta.equals(servicoBackup.pastaPadrao()) ? " (pasta padrão)" : ""));
        String aviso = servicoBackup.avisoDaPasta().orElse("");
        backupAvisoPasta.setText(aviso);
        backupAvisoPasta.setVisible(!aviso.isEmpty());
        backupAvisoPasta.setManaged(!aviso.isEmpty());
        botaoPastaPadrao.setDisable(servicoBackup.configuracaoAtual().usaPastaPadrao());
        backupUltimo.setText(servicoBackup.ultimo().map(ServicoBackup::descrever).orElse("nenhum nesta pasta"));
        long anexos = servicoBackup.tamanhoDosAnexos();
        backupAnexos.setText(anexos == 0 ? "nenhum" : NumeroBr.tamanhoArquivo(anexos) + ", cada arquivo uma vez só");
    }

    @FXML
    private void escolherPastaBackup() {
        DirectoryChooser escolha = new DirectoryChooser();
        escolha.setTitle("Pasta de backup do GarageTech");
        File pasta = escolha.showDialog(janela());
        if (pasta == null) {
            return;
        }
        try {
            servicoBackup.escolherPasta(pasta.toString());
        } catch (IllegalArgumentException | ErroDados erro) {
            mostrarErros(mensagensBackup, List.of(erro.getMessage()));
            return;
        }
        atualizarBackup();
        mostrarOk(mensagensBackup, "Os próximos backups vão para " + servicoBackup.pasta()
                + ". Para ter uma cópia lá agora, clique em \"Fazer backup agora\".");
    }

    @FXML
    private void usarPastaPadrao() {
        try {
            servicoBackup.usarPastaPadrao();
        } catch (ErroDados erro) {
            mostrarErros(mensagensBackup, List.of(erro.getMessage()));
            return;
        }
        atualizarBackup();
        mostrarOk(mensagensBackup, "Os próximos backups vão para a pasta padrão.");
    }

    @FXML
    private void abrirPastaBackup() {
        if (servicos != null) {
            servicos.showDocument(servicoBackup.pasta().toUri().toString());
        }
    }

    @FXML
    private void fazerBackupAgora() {
        mensagensBackup.getChildren().clear();
        tarefas.executar("Fazendo backup", () -> servicoBackup.fazer(MotivoBackup.MANUAL),
                feito -> mostrarOk(mensagensBackup, "Backup feito: " + feito.arquivo()),
                erro -> mostrarErros(mensagensBackup, List.of(mensagemDeErro(erro))));
    }

    @FXML
    private void restaurarBackup() {
        mensagensBackup.getChildren().clear();
        Optional<Path> escolhido = EscolhaBackup.mostrar(janela(), servicoBackup.listar(), servicoBackup.pasta());
        if (escolhido.isEmpty()) {
            return;
        }
        Path zip = escolhido.get();
        Optional<InfoBackup> info = Backup.lerInfo(zip);
        if (info.isEmpty()) {
            mostrarErros(mensagensBackup, List.of("O arquivo " + zip.getFileName() + " não é um backup do GarageTech."));
            return;
        }
        if (!confirmarRestauracao(info.get())) {
            return;
        }
        Stage espera = JanelaProgresso.abrir(janela(), "Restaurando o backup…");
        tarefas.executar("Restaurando o backup", () -> servicoBackup.restaurar(zip),
                seguranca -> {
                    espera.close();
                    avisar(AlertType.INFORMATION, "Backup restaurado",
                            "Os dados voltaram a ser os do backup de " + ServicoBackup.descrever(info.get())
                                    + ".\n\nOs dados de antes foram guardados no backup de segurança:\n"
                                    + seguranca.arquivo());
                    aposRestaurar.run();
                },
                erro -> {
                    espera.close();
                    String mensagem = mensagemDeErro(erro);
                    if (erro instanceof BackupInvalidoException) {
                        mensagem += "\n\nOs dados atuais não foram alterados.";
                    }
                    avisar(AlertType.ERROR, "O backup não foi restaurado", mensagem);
                    mostrarErros(mensagensBackup, List.of(mensagemDeErro(erro)));
                });
    }

    private boolean confirmarRestauracao(InfoBackup info) {
        ButtonType restaurar = new ButtonType("Restaurar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = Alertas.criar(AlertType.CONFIRMATION, "Todos os dados atuais (projetos, cadastros e "
                + "configurações) serão trocados pelos do backup.\n\nAntes, o GarageTech guarda os dados atuais num "
                + "backup de segurança na pasta padrão. Ele aparece na lista de backups se você quiser voltar.\n\n"
                + "O que estiver digitado e não salvo nas telas será perdido.", restaurar, cancelar);
        alerta.initOwner(janela());
        alerta.setTitle("Restaurar backup");
        alerta.setHeaderText("Restaurar o backup de " + ServicoBackup.descrever(info) + " (GarageTech "
                + info.manifesto().versaoApp() + ")?");
        return alerta.showAndWait().filter(restaurar::equals).isPresent();
    }

    private void avisar(AlertType tipo, String titulo, String mensagem) {
        Alert alerta = Alertas.criar(tipo, mensagem, ButtonType.OK);
        alerta.initOwner(janela());
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }

    /** Erro do banco ou do backup já vem explicado; o resto ganha uma mensagem geral (o detalhe vai no log). */
    static String mensagemDeErro(Throwable erro) {
        if (erro instanceof ErroDados && erro.getMessage() != null) {
            return erro.getMessage();
        }
        return "Erro inesperado (" + erro.getClass().getSimpleName() + "). O detalhe foi gravado no log.";
    }

    private Window janela() {
        return abasConfiguracoes.getScene() == null ? null : abasConfiguracoes.getScene().getWindow();
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
