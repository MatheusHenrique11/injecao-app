package br.com.calibra.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import br.com.calibra.app.casos.AnexosDaOficina;
import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.app.casos.CasosOficina.Gravado;
import br.com.calibra.app.casos.CasosOficina.NovaOs;
import br.com.calibra.app.casos.CasosOficina.NovoAnexo;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.PadroesOs;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.cadastro.ClienteSalvo;
import br.com.calibra.dados.oficina.MecanicoSalvo;

/**
 * Nova OS (SPEC 8.9): Cliente → Veículo → Serviço e entrada. Se o veículo já está na oficina, a entrada vai junto
 * (dia e hora, km, tanque, vistoria, objetos deixados e fotos); se não, fica para quando ele chegar. Cada etapa é
 * conferida antes de seguir; no fim, o caso de uso confere tudo de novo e grava tudo ou nada, com as fotos.
 */
public class NovaOsController {

    private static final int CLIENTE = 0;
    private static final int VEICULO = 1;
    private static final int SERVICO = 2;

    @FXML private Label etapa1;
    @FXML private Label etapa2;
    @FXML private Label etapa3;
    @FXML private ScrollPane rolagem;
    @FXML private VBox painelCliente;
    @FXML private EscolhaClienteController escolhaClienteController;
    @FXML private VBox painelVeiculo;
    @FXML private EscolhaVeiculoController escolhaVeiculoController;
    @FXML private VBox painelServico;
    @FXML private Label resumoEscolha;
    @FXML private TextArea servicoPedido;
    @FXML private ComboBox<MecanicoSalvo> mecanico;
    @FXML private DatePicker previsao;
    @FXML private TextArea observacoes;
    @FXML private CheckBox veiculoChegou;
    @FXML private Label dicaSemEntrada;
    @FXML private VBox caixaEntrada;
    @FXML private EntradaFormController formEntradaController;
    @FXML private Button botaoTirarFoto;
    @FXML private ListView<Path> listaFotos;
    @FXML private VBox mensagens;
    @FXML private Button botaoVoltar;
    @FXML private Button botaoProximo;
    @FXML private Button botaoAbrir;

    private CasosOficina casos;
    private CaixaMecanico caixaMecanico;
    private Navegacao navegacao;
    private Clock relogio;
    private int etapa;

    @FXML
    private void initialize() {
        Datas.emPortugues(previsao);
        caixaEntrada.visibleProperty().bind(veiculoChegou.selectedProperty());
        caixaEntrada.managedProperty().bind(veiculoChegou.selectedProperty());
        dicaSemEntrada.visibleProperty().bind(veiculoChegou.selectedProperty().not());
        dicaSemEntrada.managedProperty().bind(veiculoChegou.selectedProperty().not());
        listaFotos.setCellFactory(lista -> CamposTela.celula(NovaOsController::descreverFoto));
        botaoTirarFoto.disableProperty().bind(listaFotos.getSelectionModel().selectedItemProperty().isNull());
        caixaEntrada.setOnDragOver(evento -> {
            if (evento.getDragboard().hasFiles()) {
                evento.acceptTransferModes(TransferMode.COPY);
            }
            evento.consume();
        });
        caixaEntrada.setOnDragDropped(evento -> {
            boolean temArquivos = evento.getDragboard().hasFiles();
            if (temArquivos) {
                adicionarFotos(evento.getDragboard().getFiles().stream().map(File::toPath).toList());
            }
            evento.setDropCompleted(temArquivos);
            evento.consume();
        });
    }

    /** @param padroes os padrões de Configurações › OS (os itens da vistoria) */
    void iniciar(CasosOficina casos, CasosProjeto projetos, CasosCatalogo catalogo, Navegacao navegacao,
            Clock relogio, PadroesOs padroes) {
        this.casos = casos;
        this.navegacao = navegacao;
        this.relogio = relogio;
        LocalDateTime agora = LocalDateTime.now(relogio);
        escolhaClienteController.iniciar(projetos, agora.toLocalDate());
        escolhaVeiculoController.iniciar(projetos, catalogo);
        caixaMecanico = new CaixaMecanico(mecanico, casos);
        formEntradaController.limpar(agora, padroes.itensVistoria());
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
        if (etapa == VEICULO) {
            prepararServico();
        }
        if (etapa < SERVICO) {
            mostrarEtapa(etapa + 1);
        }
        if (etapa == SERVICO) {
            servicoPedido.requestFocus();
        }
    }

    @FXML
    private void voltar() {
        if (etapa > CLIENTE) {
            mostrarEtapa(etapa - 1);
        }
    }

    @FXML
    private void abrirOs() {
        List<String> erros = conferirEtapa();
        if (!erros.isEmpty()) {
            mostrarErros(erros);
            return;
        }
        try {
            EntradaVeiculo entrada = veiculoChegou.isSelected() ? formEntradaController.ler(new ArrayList<>()) : null;
            NovaOs nova = new NovaOs(escolhaClienteController.escolha(), escolhaVeiculoController.escolha(),
                    servicoPedido.getText(), caixaMecanico.escolhido(), previsao.getValue(), observacoes.getText(),
                    entrada);
            List<NovoAnexo> fotos = entrada == null ? List.of() : listaFotos.getItems().stream()
                    .map(foto -> new NovoAnexo(foto, TipoAnexo.FOTO_ENTRADA, "", null, false)).toList();
            Gravado gravado = casos.abrir(nova, fotos);
            mostrarAvisos(gravado.avisos());
            navegacao.abrirOs(gravado.osId());
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
    }

    @FXML
    private void cancelar() {
        Alert alerta = Alertas.criar(Alert.AlertType.CONFIRMATION, "O que foi digitado na Nova OS será perdido.",
                ButtonType.YES, ButtonType.NO);
        alerta.initOwner(janela());
        alerta.setTitle("Cancelar a Nova OS");
        alerta.setHeaderText("Cancelar a Nova OS?");
        if (alerta.showAndWait().filter(ButtonType.YES::equals).isPresent()) {
            navegacao.voltarParaOficina();
        }
    }

    @FXML
    private void novoMecanico() {
        caixaMecanico.novo(janela(), this::mostrarErros);
    }

    @FXML
    private void adicionarFotos() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Fotos da entrada");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fotos (JPG ou PNG)", "*.jpg", "*.jpeg",
                "*.png"));
        List<File> arquivos = escolha.showOpenMultipleDialog(janela());
        if (arquivos != null) {
            adicionarFotos(arquivos.stream().map(File::toPath).toList());
        }
    }

    @FXML
    private void tirarFoto() {
        listaFotos.getItems().remove(listaFotos.getSelectionModel().getSelectedItem());
    }

    /**
     * Põe as fotos na lista (do botão ou arrastadas do Windows Explorer). O que não é JPG ou PNG, ou passa de 20 MB,
     * fica fora, com o motivo; a mesma foto não entra duas vezes.
     */
    void adicionarFotos(List<Path> arquivos) {
        List<String> recusadas = new ArrayList<>();
        for (Path arquivo : arquivos) {
            String nome = arquivo.getFileName().toString();
            long bytes = tamanho(arquivo);
            if (!AnexosDaOficina.imagem(nome)) {
                recusadas.add(nome + " não é foto em JPG ou PNG. Foto do iPhone (HEIC): mande pelo WhatsApp, que ele "
                        + "converte para JPG. Documento em PDF: anexe depois, na OS.");
            } else if (bytes < 0) {
                recusadas.add("Não deu para ler o arquivo " + nome + ".");
            } else if (bytes > AnexosDaOficina.TAMANHO_MAXIMO_BYTES) {
                recusadas.add(nome + " passa de 20 MB.");
            } else if (!listaFotos.getItems().contains(arquivo)) {
                listaFotos.getItems().add(arquivo);
            }
        }
        mostrarErros(recusadas);
    }

    // ===================== Etapas =====================

    private void mostrarEtapa(int nova) {
        etapa = nova;
        List<Node> paineis = List.of(painelCliente, painelVeiculo, painelServico);
        for (int i = 0; i < paineis.size(); i++) {
            paineis.get(i).setVisible(i == etapa);
            paineis.get(i).setManaged(i == etapa);
        }
        List<Label> rotulos = List.of(etapa1, etapa2, etapa3);
        for (int i = 0; i < rotulos.size(); i++) {
            rotulos.get(i).getStyleClass().removeAll("etapa-atual", "etapa");
            rotulos.get(i).getStyleClass().add(i == etapa ? "etapa-atual" : "etapa");
        }
        botaoVoltar.setDisable(etapa == CLIENTE);
        botaoProximo.setVisible(etapa < SERVICO);
        botaoProximo.setManaged(etapa < SERVICO);
        botaoProximo.setDefaultButton(etapa < SERVICO);
        botaoAbrir.setVisible(etapa == SERVICO);
        botaoAbrir.setManaged(etapa == SERVICO);
        mensagens.getChildren().clear();
        rolagem.setVvalue(0);
    }

    /** Erros da etapa atual, pelas mesmas regras que o caso de uso confere no fim. */
    private List<String> conferirEtapa() {
        LocalDate hoje = LocalDate.now(relogio);
        return switch (etapa) {
            case CLIENTE -> escolhaClienteController.conferir(hoje);
            case VEICULO -> escolhaVeiculoController.conferir(hoje.getYear());
            case SERVICO -> {
                List<String> erros = new ArrayList<>();
                if (servicoPedido.getText() == null || servicoPedido.getText().isBlank()) {
                    erros.add("Descreva o serviço pedido pelo cliente.");
                }
                if (veiculoChegou.isSelected()) {
                    EntradaVeiculo entrada = formEntradaController.ler(erros);
                    if (entrada != null) {
                        erros.addAll(entrada.validar(LocalDateTime.now(relogio)));
                    }
                }
                yield erros;
            }
            default -> throw new IllegalStateException("etapa " + etapa);
        };
    }

    /** Ao entrar na etapa Serviço e entrada: de quem e qual veículo, e o km (ou as horas, no náutico) do cadastro. */
    private void prepararServico() {
        ClienteSalvo cliente = escolhaClienteController.existente();
        Veiculo veiculo = escolhaVeiculoController.veiculo();
        String nomeCliente = cliente == null ? "cliente novo" : cliente.cliente().nome();
        resumoEscolha.setText(veiculo == null ? nomeCliente : veiculo.descricao()
                + (veiculo.identificacao().isEmpty() ? "" : " · " + veiculo.identificacao()) + " — " + nomeCliente);
        formEntradaController.usarVeiculo(veiculo);
    }

    // ===================== Utilidades =====================

    /** Avisos que não impediram (ex.: km menor que o do cadastro): a OS já foi aberta. */
    private void mostrarAvisos(List<String> avisos) {
        if (avisos.isEmpty()) {
            return;
        }
        Alert alerta = Alertas.criar(Alert.AlertType.INFORMATION, String.join("\n\n", avisos));
        alerta.initOwner(janela());
        alerta.setTitle("OS aberta");
        alerta.setHeaderText("A OS foi aberta, com estes avisos:");
        alerta.showAndWait();
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

    private Window janela() {
        return rolagem.getScene() == null ? null : rolagem.getScene().getWindow();
    }

    /** "IMG_0001.jpg · 3,2 MB". */
    private static String descreverFoto(Path foto) {
        long bytes = tamanho(foto);
        return foto.getFileName() + " · " + (bytes < 0 ? "não encontrada" : NumeroBr.tamanhoArquivo(bytes));
    }

    /** O tamanho do arquivo em bytes; -1 se não deu para ler. */
    private static long tamanho(Path arquivo) {
        try {
            return Files.size(arquivo);
        } catch (IOException erro) {
            return -1;
        }
    }
}
