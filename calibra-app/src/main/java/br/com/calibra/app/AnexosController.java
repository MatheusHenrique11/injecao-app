package br.com.calibra.app;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import br.com.calibra.app.casos.AnexosDaOficina;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.oficina.AnexoSalvo;

/**
 * A lista de anexos com miniatura (bloco 3.2; componente desde o bloco 3.3): adicionar (pelo botão ou arrastando do
 * Windows Explorer), abrir, editar e remover. De onde vêm os anexos e para onde vão os novos, diz a {@link Fonte}: a
 * aba Anexos da OS e a ficha do veículo usam a mesma lista.
 */
public class AnexosController {

    /** De onde vêm os anexos (a OS ou o veículo) e o que muda entre uma e outro. */
    interface Fonte {

        /** Os anexos da lista, do mais antigo para o mais novo. */
        List<AnexoSalvo> anexos();

        /**
         * Prepara a anexação dos arquivos, na thread da tela (ex.: o tipo de cada um, pela etapa da OS), e devolve o
         * trabalho de copiar e gravar, que roda em segundo plano.
         *
         * @return o trabalho; ele devolve a mensagem de sucesso e lança {@link EntradaInvalidaException} ou
         *     {@link ErroDados} se não deu (nada é gravado)
         */
        Callable<String> anexar(List<Path> arquivos);

        /** Se o anexo se remove aqui (ex.: o da OS entregue não; o da OS, só na própria OS). */
        boolean podeRemover(AnexoSalvo anexo);

        /** O título da janela de escolher os arquivos (ex.: "Anexar à OS-2026-0001"). */
        String tituloEscolha();

        /** De onde é o anexo, na linha da lista (ex.: "OS-2026-0001"); vazio = não mostra. */
        default String origem(AnexoSalvo anexo) {
            return "";
        }
    }

    private static final double LARGURA_MINIATURA = 120;
    private static final double ALTURA_MINIATURA = 90;

    @FXML private VBox caixaAnexos;
    @FXML private ListView<AnexoSalvo> listaAnexos;
    @FXML private Label listaVazia;
    @FXML private Button botaoAdicionarAnexos;
    @FXML private Button botaoAbrirAnexo;
    @FXML private Button botaoEditarAnexo;
    @FXML private Button botaoRemoverAnexo;

    /** As miniaturas já carregadas, pelo id do anexo (a lista reaproveita as linhas ao rolar). */
    private final Map<Long, Image> miniaturas = new HashMap<>();
    private CasosOficina casos;
    private HostServices servicos;
    private TarefasFundo tarefas;
    private Fonte fonte;
    private Consumer<String> ok = texto -> { };
    private Consumer<List<String>> erros = lista -> { };
    /** Uma cópia está em andamento: outra (pelo botão ou arrastando) espera ela terminar. */
    private boolean copiando;

    @FXML
    private void initialize() {
        listaAnexos.setCellFactory(lista -> new CelulaAnexo());
        listaAnexos.setOnMouseClicked(evento -> {
            if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2
                    && listaAnexos.getSelectionModel().getSelectedItem() != null) {
                abrirAnexo();
            }
        });
        botaoAbrirAnexo.disableProperty().bind(listaAnexos.getSelectionModel().selectedItemProperty().isNull());
        botaoEditarAnexo.disableProperty().bind(listaAnexos.getSelectionModel().selectedItemProperty().isNull());
        listaAnexos.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) -> mostrarBotoes());
        caixaAnexos.setOnDragOver(evento -> {
            if (evento.getDragboard().hasFiles()) {
                evento.acceptTransferModes(TransferMode.COPY);
            }
            evento.consume();
        });
        caixaAnexos.setOnDragDropped(evento -> {
            boolean temArquivos = evento.getDragboard().hasFiles();
            if (temArquivos) {
                adicionarAnexos(evento.getDragboard().getFiles().stream().map(File::toPath).toList());
            }
            evento.setDropCompleted(temArquivos);
            evento.consume();
        });
    }

    /**
     * @param tarefas a fila de segundo plano do app, onde a cópia dos arquivos roda
     * @param ok mostra a mensagem de sucesso na tela que usa a lista
     * @param erros mostra os erros na tela que usa a lista
     */
    void iniciar(CasosOficina casos, HostServices servicos, TarefasFundo tarefas, Fonte fonte, Consumer<String> ok,
            Consumer<List<String>> erros) {
        this.casos = casos;
        this.servicos = servicos;
        this.tarefas = tarefas;
        this.fonte = fonte;
        this.ok = ok;
        this.erros = erros;
        atualizar();
    }

    /** O texto do botão de adicionar e o aviso da lista vazia (ex.: na ficha, "Adicionar documento do veículo…"). */
    void textos(String botaoAdicionar, String vazia) {
        botaoAdicionarAnexos.setText(botaoAdicionar);
        listaVazia.setText(vazia);
    }

    /** Lê a lista de novo (depois de mudar algo na OS ou no veículo). */
    void atualizar() {
        AnexoSalvo escolhido = listaAnexos.getSelectionModel().getSelectedItem();
        listaAnexos.getItems().setAll(fonte.anexos());
        if (escolhido != null) {
            listaAnexos.getItems().stream().filter(anexo -> anexo.id() == escolhido.id()).findFirst()
                    .ifPresent(listaAnexos.getSelectionModel()::select);
        }
        mostrarBotoes();
    }

    private void mostrarBotoes() {
        AnexoSalvo escolhido = listaAnexos.getSelectionModel().getSelectedItem();
        botaoRemoverAnexo.setDisable(escolhido == null || !fonte.podeRemover(escolhido));
    }

    @FXML
    private void adicionarAnexos() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle(fonte.tituloEscolha());
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fotos (JPG ou PNG) e PDF", "*.jpg",
                "*.jpeg", "*.png", "*.pdf"));
        List<File> arquivos = escolha.showOpenMultipleDialog(janela());
        if (arquivos != null) {
            adicionarAnexos(arquivos.stream().map(File::toPath).toList());
        }
    }

    /**
     * Anexa os arquivos (do botão ou arrastados do Windows Explorer). A cópia roda na fila de segundo plano: uma foto
     * grande não trava a tela, e a janela de espera só aparece se passar de meio segundo.
     */
    void adicionarAnexos(List<Path> arquivos) {
        if (arquivos.isEmpty() || copiando) {
            return;
        }
        Callable<String> trabalho;
        try {
            trabalho = fonte.anexar(arquivos);
        } catch (EntradaInvalidaException | ErroDados erro) {
            erros.accept(TarefasFundo.mensagens(erro));
            return;
        }
        String texto = arquivos.size() == 1 ? "Copiando 1 arquivo" : "Copiando " + arquivos.size() + " arquivos";
        copiando = true;
        botaoAdicionarAnexos.setDisable(true);
        Runnable fecharEspera = JanelaProgresso.abrirSeDemorar(janela(), texto + "…");
        tarefas.executar(texto, trabalho, mensagem -> {
            fecharEspera.run();
            terminouCopia();
            atualizar();
            ok.accept(mensagem);
        }, erro -> {
            fecharEspera.run();
            terminouCopia();
            erros.accept(TarefasFundo.mensagens(erro));
        });
    }

    private void terminouCopia() {
        copiando = false;
        botaoAdicionarAnexos.setDisable(false);
    }

    /** Uma cópia está em andamento (para os testes). */
    boolean copiando() {
        return copiando;
    }

    @FXML
    private void abrirAnexo() {
        AnexoSalvo anexo = listaAnexos.getSelectionModel().getSelectedItem();
        if (anexo == null) {
            return;
        }
        Optional<Path> arquivo = casos.arquivoDoAnexo(anexo);
        if (arquivo.isEmpty()) {
            erros.accept(List.of("O arquivo de " + anexo.nomeOriginal() + " não está na pasta de anexos (foi apagado "
                    + "fora do GarageTech?). Restaure um backup ou anexe de novo."));
        } else if (servicos != null) {
            servicos.showDocument(arquivo.get().toUri().toString());
        }
    }

    @FXML
    private void editarAnexo() {
        AnexoSalvo anexo = listaAnexos.getSelectionModel().getSelectedItem();
        if (anexo != null) {
            JanelasOs.anexo(janela(), anexo).ifPresent(dados -> executar(() -> {
                casos.editarAnexo(anexo.id(), dados.tipo(), dados.descricao(), dados.validade(), dados.pessoal());
                return "Anexo alterado.";
            }));
        }
    }

    @FXML
    private void removerAnexo() {
        AnexoSalvo anexo = listaAnexos.getSelectionModel().getSelectedItem();
        if (anexo == null) {
            return;
        }
        Alert alerta = Alertas.criar(Alert.AlertType.CONFIRMATION, anexo.tipo().rotulo() + ": "
                + anexo.nomeOriginal() + ". O arquivo sai da pasta de anexos se nenhum outro anexo usar o mesmo.",
                ButtonType.YES, ButtonType.NO);
        alerta.initOwner(janela());
        alerta.setTitle("Remover anexo");
        alerta.setHeaderText("Remover o anexo?");
        if (alerta.showAndWait().filter(ButtonType.YES::equals).isPresent()) {
            removerAnexo(anexo.id());
        }
    }

    void removerAnexo(long anexoId) {
        executar(() -> {
            casos.removerAnexo(anexoId);
            miniaturas.remove(anexoId);
            return "Anexo removido.";
        });
    }

    /** Faz a ação, lê a lista de novo e mostra a mensagem; com erro, os erros. */
    private void executar(java.util.function.Supplier<String> acao) {
        String mensagem;
        try {
            mensagem = acao.get();
        } catch (EntradaInvalidaException erro) {
            erros.accept(erro.erros());
            return;
        } catch (ErroDados erro) {
            erros.accept(List.of(erro.getMessage()));
            return;
        }
        atualizar();
        ok.accept(mensagem);
    }

    /** A lista (para os testes). */
    ListView<AnexoSalvo> lista() {
        return listaAnexos;
    }

    private Window janela() {
        return listaAnexos.getScene() == null ? null : listaAnexos.getScene().getWindow();
    }

    /** Uma linha da lista: a miniatura (ou "PDF") e o tipo, a descrição, a origem, o arquivo e a validade. */
    private final class CelulaAnexo extends ListCell<AnexoSalvo> {

        private final ImageView imagem = new ImageView();
        private final Label documento = new Label();
        private final StackPane quadro = new StackPane();
        private final Label nome = new Label();
        private final Label detalhes = new Label();
        private final HBox linha;

        CelulaAnexo() {
            quadro.setMinSize(LARGURA_MINIATURA, ALTURA_MINIATURA);
            quadro.setPrefSize(LARGURA_MINIATURA, ALTURA_MINIATURA);
            quadro.getStyleClass().add("miniatura");
            documento.getStyleClass().add("negrito");
            nome.getStyleClass().add("negrito");
            detalhes.getStyleClass().add("dica");
            linha = new HBox(12.0, quadro, new VBox(4.0, nome, detalhes));
            linha.setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(AnexoSalvo anexo, boolean vazio) {
            super.updateItem(anexo, vazio);
            setText(null);
            if (vazio || anexo == null) {
                setGraphic(null);
                return;
            }
            Optional<Path> arquivo = casos.arquivoDoAnexo(anexo);
            if (arquivo.isPresent() && AnexosDaOficina.imagem(anexo.arquivo())) {
                // A imagem carrega em segundo plano, já no tamanho da miniatura: a lista não trava com fotos grandes.
                imagem.setImage(miniaturas.computeIfAbsent(anexo.id(), id -> new Image(arquivo.get().toUri()
                        .toString(), LARGURA_MINIATURA, ALTURA_MINIATURA, true, true, true)));
                quadro.getChildren().setAll(imagem);
            } else {
                documento.setText(arquivo.isPresent() ? "PDF" : "arquivo\nnão achado");
                quadro.getChildren().setAll(documento);
            }
            String origem = fonte.origem(anexo);
            nome.setText(anexo.tipo().rotulo() + (anexo.descricao().isBlank() ? "" : " · " + anexo.descricao())
                    + (origem.isEmpty() ? "" : " · " + origem));
            detalhes.setText(anexo.nomeOriginal() + " · " + NumeroBr.tamanhoArquivo(anexo.tamanhoBytes()) + " · "
                    + Datas.diaHora(anexo.criadoEm()) + (anexo.validade() == null ? ""
                            : " · válido até " + Datas.dia(anexo.validade()))
                    + (anexo.pessoal() ? " · documento pessoal" : ""));
            setGraphic(linha);
        }
    }
}
