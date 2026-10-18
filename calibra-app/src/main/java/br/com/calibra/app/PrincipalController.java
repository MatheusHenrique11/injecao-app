package br.com.calibra.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.app.casos.CasosOrcamento;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.ImagensTurbo;
import br.com.calibra.app.casos.ServicoBackup;
import br.com.calibra.core.memoria.VersaoNucleo;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.dados.backup.InfoBackup;
import br.com.calibra.dados.config.Chave;

/**
 * Janela principal: navegação entre as telas, barra de status com o backup e a versão (SPEC 10). O app abre no
 * Painel. Cada tela é carregada na primeira vez que é aberta e continua com o que foi digitado quando se troca de
 * tela.
 */
public class PrincipalController implements Navegacao {

    private static final Logger LOG = Logger.getLogger(PrincipalController.class.getName());

    @FXML private ToggleGroup navegacao;
    @FXML private ToggleButton navPainel;
    @FXML private ToggleButton navOficina;
    @FXML private ToggleButton navPreparacao;
    @FXML private ToggleButton navCalculadora;
    @FXML private ToggleButton navCatalogos;
    @FXML private ToggleButton navConfiguracoes;
    @FXML private StackPane conteudo;
    @FXML private ProgressIndicator progresso;
    @FXML private Label rotuloStatus;
    @FXML private Label rotuloVersao;

    private ContextoApp contexto;
    private HostServices servicos;
    private Stage palco;
    private ServicoBackup servicoBackup;
    private final TarefasFundo tarefas = new TarefasFundo();
    /** Último erro de backup em segundo plano; fica na barra até o próximo backup dar certo. */
    private String erroDeBackup;
    private boolean fechando;

    private CasosProjeto casosProjeto;
    private CasosCatalogo casosCatalogo;
    private CasosOrcamento casosOrcamento;
    private CasosOficina casosOficina;
    private Parent telaPainel;
    private PainelController painel;
    private Parent telaListaOficina;
    private OficinaController oficina;
    /** O que está aberto na área da Oficina: a lista, a Nova OS ou uma OS. */
    private Parent telaOficina;
    private String tituloOficina;
    private NovaOsController novaOsAberta;
    private OsController osAberta;
    private FichaVeiculoController fichaAberta;
    private Parent telaInicio;
    private InicioController inicio;
    /** O que está aberto na área de projetos: a lista, o assistente ou um projeto. */
    private Parent telaProjetos;
    private String tituloProjetos;
    private ProjetoController projetoAberto;
    private Parent telaCalculadora;
    private CalculadoraController calculadora;
    private Parent telaCatalogos;
    private CatalogosController catalogos;
    private Parent telaConfiguracoes;
    private ConfiguracoesController configuracoes;

    @FXML
    private void initialize() {
        rotuloVersao.setText("Calibra " + Versao.atual() + " · núcleo " + VersaoNucleo.ATUAL + " · offline");
        // Clicar de novo no botão da tela aberta não pode deixar a navegação sem nada escolhido.
        navegacao.selectedToggleProperty().addListener((obs, antes, depois) -> {
            if (depois == null && antes != null) {
                antes.setSelected(true);
            }
        });
        tarefas.emAndamento().addListener((obs, antes, depois) -> {
            atualizarStatus();
            if (depois.intValue() == 0 && configuracoes != null) {
                configuracoes.atualizarBackup();
            }
            if (depois.intValue() == 0 && painel != null) {
                painel.atualizarBackup();
            }
        });
    }

    void iniciar(ContextoApp contexto, HostServices servicos, Stage palco) {
        this.contexto = contexto;
        this.servicos = servicos;
        this.palco = palco;
        this.servicoBackup = new ServicoBackup(contexto.backup(), contexto.configuracao(), contexto.relogio());
        this.casosProjeto = new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp());
        this.casosCatalogo = new CasosCatalogo(contexto.banco(), contexto.relogio());
        this.casosOrcamento = new CasosOrcamento(contexto.banco(), contexto.relogio(), casosProjeto);
        this.casosOficina = new CasosOficina(contexto.banco(), contexto.relogio(), casosProjeto,
                contexto.pastas().anexos());
        atualizarStatus();
        mostrarPainel();
    }

    /** Backup automático (SPEC 10: diário): confere agora, ao abrir, e depois de hora em hora. */
    void ligarBackupAutomatico() {
        backupAutomatico();
        tarefas.deHoraEmHora(this::backupAutomatico);
    }

    // ===================== Navegação =====================

    /** O Painel, lido de novo a cada vez que aparece. */
    @FXML
    private void mostrarPainel() {
        if (telaPainel == null) {
            FXMLLoader carregador = carregador("painel.fxml");
            telaPainel = carregar(carregador);
            painel = carregador.getController();
            painel.iniciar(casosOficina, this, contexto.relogio(), this::textoDoBackup);
        } else {
            painel.atualizar();
        }
        mostrar(telaPainel, navPainel, "Painel");
    }

    /** Mostra o que estava aberto na área da Oficina; a lista e a OS voltam atualizadas. */
    @FXML
    private void mostrarOficina() {
        if (telaOficina == null || telaOficina == telaListaOficina) {
            voltarParaOficina();
            return;
        }
        if (osAberta != null) {
            osAberta.atualizar();
        }
        mostrar(telaOficina, navOficina, tituloOficina);
    }

    @Override
    public void voltarParaOficina() {
        if (telaListaOficina == null) {
            FXMLLoader carregador = carregador("oficina.fxml");
            telaListaOficina = carregar(carregador);
            oficina = carregador.getController();
            oficina.iniciar(casosOficina, this, contexto.relogio());
        } else {
            oficina.atualizar();
        }
        novaOsAberta = null;
        osAberta = null;
        fichaAberta = null;
        abrirNaAreaDaOficina(telaListaOficina, "Oficina");
        oficina.focarBusca();
    }

    @Override
    public void abrirOficina(StatusOs status) {
        voltarParaOficina();
        oficina.filtrar(status);
    }

    /** A Nova OS; se já há uma sendo preenchida, volta para ela, com o que foi digitado. */
    @Override
    public void novaOs() {
        if (novaOsAberta != null) {
            mostrar(telaOficina, navOficina, tituloOficina);
            return;
        }
        FXMLLoader carregador = carregador("nova-os.fxml");
        Parent tela = carregar(carregador);
        novaOsAberta = carregador.getController();
        novaOsAberta.iniciar(casosOficina, casosProjeto, casosCatalogo, this, contexto.relogio(),
                contexto.configuracao().ler(Chave.OS));
        osAberta = null;
        fichaAberta = null;
        abrirNaAreaDaOficina(tela, "Nova OS");
    }

    /** A Nova OS sendo preenchida; null se não há nenhuma (para os testes de interface). */
    NovaOsController novaOsAberta() {
        return novaOsAberta;
    }

    @Override
    public void abrirOs(long osId) {
        FXMLLoader carregador = carregador("os.fxml");
        Parent tela = carregar(carregador);
        OsController os = carregador.getController();
        os.iniciar(new OsController.Recursos(casosOficina, casosProjeto, recursosDoOrcamento(), servicos, this,
                contexto.relogio(), () -> contexto.configuracao().ler(Chave.OS), contexto.pastas().temporaria()), osId);
        novaOsAberta = null;
        osAberta = os;
        fichaAberta = null;
        abrirNaAreaDaOficina(tela, os.numero());
    }

    /** A OS aberta na área da Oficina; null na lista ou na Nova OS (para os testes de interface). */
    OsController osAberta() {
        return osAberta;
    }

    @Override
    public void abrirFichaDoVeiculo(long veiculoId, String rotuloVoltar, Runnable voltar) {
        FXMLLoader carregador = carregador("ficha-veiculo.fxml");
        Parent tela = carregar(carregador);
        FichaVeiculoController ficha = carregador.getController();
        ficha.iniciar(new FichaVeiculoController.Recursos(casosOficina, casosProjeto, casosCatalogo, servicos, this,
                contexto.relogio()), veiculoId, rotuloVoltar, voltar);
        novaOsAberta = null;
        osAberta = null;
        fichaAberta = ficha;
        abrirNaAreaDaOficina(tela, "Ficha do veículo");
    }

    /** A ficha do veículo aberta na área da Oficina; null se não há (para os testes de interface). */
    FichaVeiculoController fichaAberta() {
        return fichaAberta;
    }

    private void abrirNaAreaDaOficina(Parent tela, String titulo) {
        telaOficina = tela;
        tituloOficina = titulo;
        mostrar(tela, navOficina, titulo);
    }

    /** Mostra o que estava aberto na área de preparação (a lista, o assistente ou um projeto). */
    @FXML
    private void mostrarPreparacao() {
        if (telaProjetos == null) {
            voltarParaProjetos();
            return;
        }
        if (projetoAberto != null) {
            projetoAberto.atualizarCatalogo();
        }
        mostrar(telaProjetos, navPreparacao, tituloProjetos);
    }

    @Override
    public void voltarParaProjetos() {
        if (telaInicio == null) {
            FXMLLoader carregador = carregador("inicio.fxml");
            telaInicio = carregar(carregador);
            inicio = carregador.getController();
            inicio.iniciar(casosProjeto, this);
        } else {
            inicio.atualizar();
        }
        projetoAberto = null;
        abrirNaAreaDeProjetos(telaInicio, "Preparação");
        inicio.focarBusca();
    }

    @Override
    public void novoProjeto() {
        FXMLLoader carregador = carregador("assistente.fxml");
        Parent tela = carregar(carregador);
        AssistenteController assistente = carregador.getController();
        assistente.iniciar(casosProjeto, casosCatalogo, contexto.configuracao().ler(Chave.CALCULO), this,
                contexto.relogio());
        projetoAberto = null;
        abrirNaAreaDeProjetos(tela, "Novo projeto");
    }

    @Override
    public void novoProjetoDaOs(long osId) {
        FXMLLoader carregador = carregador("assistente.fxml");
        Parent tela = carregar(carregador);
        AssistenteController assistente = carregador.getController();
        assistente.iniciarParaOs(casosProjeto, casosCatalogo, contexto.configuracao().ler(Chave.CALCULO), this,
                contexto.relogio(), casosOficina, osId);
        projetoAberto = null;
        abrirNaAreaDeProjetos(tela, "Novo projeto");
    }

    @Override
    public void abrirProjeto(long projetoId) {
        FXMLLoader carregador = carregador("projeto.fxml");
        Parent tela = carregar(carregador);
        ProjetoController projeto = carregador.getController();
        projeto.iniciar(casosProjeto, casosCatalogo, this, servicos, contexto.configuracao().ler(Chave.CALCULO),
                projetoId);
        projeto.usarOrcamento(recursosDoOrcamento());
        projeto.usarOficina(casosOficina);
        projetoAberto = projeto;
        abrirNaAreaDeProjetos(tela, "Projeto #" + projetoId);
    }

    /** O que o editor de orçamento usa, no projeto e na OS: os padrões e a oficina são lidos na hora. */
    private OrcamentoController.Recursos recursosDoOrcamento() {
        return new OrcamentoController.Recursos(casosOrcamento, casosCatalogo,
                () -> contexto.configuracao().ler(Chave.ORCAMENTO),
                () -> contexto.configuracao().ler(Chave.OFICINA), contexto.pastas().anexos(), contexto.versaoApp());
    }

    /** O projeto aberto na área de projetos; null na lista ou no assistente (para os testes de interface). */
    ProjetoController projetoAberto() {
        return projetoAberto;
    }

    private void abrirNaAreaDeProjetos(Parent tela, String titulo) {
        telaProjetos = tela;
        tituloProjetos = titulo;
        mostrar(tela, navPreparacao, titulo);
    }

    @FXML
    private void mostrarCalculadora() {
        if (telaCalculadora == null) {
            FXMLLoader carregador = carregador("calculadora.fxml");
            telaCalculadora = carregar(carregador);
            calculadora = carregador.getController();
            calculadora.usarServicos(servicos);
            calculadora.usarPadroes(contexto.configuracao().ler(Chave.CALCULO));
            calculadora.usarCatalogo(casosCatalogo);
        } else {
            calculadora.atualizarCatalogo();
        }
        mostrar(telaCalculadora, navCalculadora, "Calculadora");
    }

    @FXML
    private void mostrarCatalogos() {
        if (telaCatalogos == null) {
            FXMLLoader carregador = carregador("catalogos.fxml");
            telaCatalogos = carregar(carregador);
            catalogos = carregador.getController();
            catalogos.iniciar(casosCatalogo, new ImagensTurbo(contexto.pastas().anexos(), contexto.relogio()));
        } else {
            catalogos.atualizar();
        }
        mostrar(telaCatalogos, navCatalogos, "Catálogos");
    }

    @FXML
    private void mostrarConfiguracoes() {
        if (telaConfiguracoes == null) {
            FXMLLoader carregador = carregador("configuracoes.fxml");
            telaConfiguracoes = carregar(carregador);
            configuracoes = carregador.getController();
            configuracoes.iniciar(contexto.configuracao(), contexto.pastas().anexos(), padroes -> {
                if (calculadora != null) {
                    calculadora.usarPadroes(padroes);
                }
                if (projetoAberto != null) {
                    projetoAberto.usarPadroes(padroes);
                }
            });
            configuracoes.usarBackup(servicoBackup, tarefas, servicos, this::aposRestaurar);
            configuracoes.usarOficina(casosOficina);
        }
        mostrar(telaConfiguracoes, navConfiguracoes, "Configurações");
    }

    private void mostrar(Parent tela, ToggleButton botao, String titulo) {
        conteudo.getChildren().setAll(tela);
        botao.setSelected(true);
        if (palco != null) {
            palco.setTitle("Calibra " + Versao.atual() + " — " + titulo);
        }
    }

    /** Os dados mudaram todos: as telas são montadas de novo a partir do banco restaurado. */
    private void aposRestaurar() {
        telaPainel = null;
        painel = null;
        telaListaOficina = null;
        oficina = null;
        telaOficina = null;
        novaOsAberta = null;
        osAberta = null;
        fichaAberta = null;
        telaInicio = null;
        inicio = null;
        telaProjetos = null;
        projetoAberto = null;
        telaCalculadora = null;
        calculadora = null;
        telaCatalogos = null;
        catalogos = null;
        telaConfiguracoes = null;
        configuracoes = null;
        erroDeBackup = null;
        mostrarConfiguracoes();
        configuracoes.mostrarAbaBackup();
        atualizarStatus();
    }

    // ===================== Backup =====================

    private void backupAutomatico() {
        tarefas.executar("Backup automático", servicoBackup::fazerAutomaticoSeVencido,
                feito -> {
                    erroDeBackup = null;
                    atualizarStatus();
                },
                erro -> {
                    erroDeBackup = "O backup automático falhou: " + ConfiguracoesController.mensagemDeErro(erro)
                            + " O Calibra tenta de novo daqui a uma hora.";
                    atualizarStatus();
                });
    }

    /** Barra de status: o trabalho em andamento ou o último backup, com os avisos. */
    private void atualizarStatus() {
        boolean ocupado = tarefas.emAndamento().get() > 0;
        progresso.setVisible(ocupado);
        progresso.setManaged(ocupado);
        rotuloStatus.getStyleClass().removeAll("erro", "aviso-amarelo");
        if (ocupado) {
            rotuloStatus.setText(tarefas.descricao().get());
            return;
        }
        if (servicoBackup == null) {
            rotuloStatus.setText("");
            return;
        }
        if (erroDeBackup != null) {
            rotuloStatus.setText(erroDeBackup);
            rotuloStatus.getStyleClass().add("erro");
            return;
        }
        String texto = textoDoBackup();
        if (servicoBackup.avisoDaPasta().isPresent()) {
            texto += " · a pasta de backup escolhida não está ligada: usando a pasta padrão";
            rotuloStatus.getStyleClass().add("aviso-amarelo");
        }
        rotuloStatus.setText(texto);
    }

    /** "Último backup: 12/10/2026 14:50 · automático (diário) · 1,2 MB" (na barra de status e no Painel). */
    private String textoDoBackup() {
        Optional<InfoBackup> ultimo = servicoBackup.ultimo();
        return "Último backup: " + ultimo.map(ServicoBackup::descrever).orElse("nenhum ainda");
    }

    // ===================== Fechar =====================

    /**
     * Fechar a janela (SPEC 10: backup ao fechar). Se algo mudou desde o último backup, ou se há backup
     * rodando, mostra "Fazendo backup…" e só fecha no fim. Se o backup falhar, pergunta se sai mesmo assim.
     */
    void fechar() {
        if (fechando) {
            return;
        }
        fechando = true;
        if (!servicoBackup.precisaBackupAoFechar() && tarefas.emAndamento().get() == 0) {
            sair();
            return;
        }
        Stage espera = JanelaProgresso.abrir(palco, "Fazendo backup antes de fechar…");
        tarefas.executar("Backup ao fechar", servicoBackup::fazerAoFecharSePreciso,
                feito -> {
                    espera.close();
                    sair();
                },
                erro -> {
                    espera.close();
                    if (sairMesmoAssim(ConfiguracoesController.mensagemDeErro(erro))) {
                        sair();
                    } else {
                        fechando = false;
                    }
                });
    }

    private boolean sairMesmoAssim(String mensagem) {
        ButtonType sair = new ButtonType("Sair sem backup", ButtonBar.ButtonData.OK_DONE);
        ButtonType ficar = new ButtonType("Continuar no Calibra", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = Alertas.criar(AlertType.WARNING, mensagem + "\n\nSe sair agora, as mudanças desde o último "
                + "backup ficam só no banco de dados deste computador.", sair, ficar);
        alerta.initOwner(palco);
        alerta.setTitle("Backup ao fechar");
        alerta.setHeaderText("O backup ao fechar falhou. Sair mesmo assim?");
        return alerta.showAndWait().filter(sair::equals).isPresent();
    }

    private void sair() {
        LOG.info("Calibra fechando.");
        encerrar();
        Platform.exit();
    }

    /** Para a fila de trabalhos (ao sair do app). */
    void encerrar() {
        tarefas.encerrar();
    }

    // ===================== Utilidades =====================

    private static FXMLLoader carregador(String fxml) {
        return new FXMLLoader(PrincipalController.class.getResource(fxml));
    }

    private static Parent carregar(FXMLLoader carregador) {
        try {
            return carregador.load();
        } catch (IOException erro) {
            throw new UncheckedIOException("Tela do app não abriu: " + carregador.getLocation(), erro);
        }
    }
}
