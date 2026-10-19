package br.com.calibra.app;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.app.casos.CasosOficina.Anonimizacao;
import br.com.calibra.app.casos.CasosOficina.FichaVeiculo;
import br.com.calibra.app.casos.CasosOficina.NovoAnexo;
import br.com.calibra.app.casos.CasosOficina.ProjetoDoVeiculo;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.oficina.AnexoSalvo;
import br.com.calibra.dados.oficina.ResumoOs;

/**
 * A ficha do veículo (bloco 3.3, área Oficina): o cliente e o veículo, com editar, histórico e anonimizar (LGPD); as OS
 * e os projetos do veículo, que abrem com duplo clique; e os anexos de todas as OS dele, com os documentos do próprio
 * veículo (ex.: o CRLV, com a validade). Abre pela OS e pelo cabeçalho do projeto.
 */
public class FichaVeiculoController {

    /**
     * O que a tela usa do resto do app.
     *
     * @param tarefas a fila de segundo plano do app (a cópia dos documentos)
     */
    record Recursos(CasosOficina oficina, CasosProjeto projetos, CasosCatalogo catalogo, HostServices servicos,
            Navegacao navegacao, Clock relogio, TarefasFundo tarefas) {
    }

    @FXML private Button botaoVoltar;
    @FXML private Label titulo;
    @FXML private Label subtitulo;
    @FXML private Label clienteNome;
    @FXML private Label clienteContato;
    @FXML private Label clienteConsentimento;
    @FXML private Button botaoEditarCliente;
    @FXML private Button botaoAnonimizar;
    @FXML private Label veiculoResumo;
    @FXML private Label veiculoDetalhes;
    @FXML private TableView<ResumoOs> ordens;
    @FXML private TableView<ProjetoDoVeiculo> projetos;
    @FXML private TableColumn<ProjetoDoVeiculo, String> colunaProjeto;
    @FXML private TableColumn<ProjetoDoVeiculo, String> colunaMotor;
    @FXML private TableColumn<ProjetoDoVeiculo, String> colunaSituacaoProjeto;
    @FXML private AnexosController anexosController;
    @FXML private VBox mensagens;

    private Recursos recursos;
    private long veiculoId;
    private Runnable voltar;
    private FichaVeiculo ficha;
    /** O número de cada OS do veículo, pelo id (para a origem dos anexos). */
    private Map<Long, String> numerosDasOs = Map.of();

    @FXML
    private void initialize() {
        coluna(colunaProjeto, p -> "#" + p.projetoId());
        coluna(colunaMotor, ProjetoDoVeiculo::motor);
        coluna(colunaSituacaoProjeto, p -> p.status().rotulo());
        projetos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        projetos.setRowFactory(tabela -> {
            TableRow<ProjetoDoVeiculo> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2 && !linha.isEmpty()) {
                    recursos.navegacao().abrirProjeto(linha.getItem().projetoId());
                }
            });
            return linha;
        });
    }

    /**
     * @param rotuloVoltar para onde o botão de voltar leva (ex.: "OS-2026-0001", "Projeto #3", "Oficina")
     * @param voltar o que o botão faz
     */
    void iniciar(Recursos recursos, long veiculoId, String rotuloVoltar, Runnable voltar) {
        this.recursos = recursos;
        this.veiculoId = veiculoId;
        this.voltar = voltar;
        botaoVoltar.setText("← " + rotuloVoltar);
        ListaOs.montar(ordens, recursos.relogio(), ListaOs.Coluna.NUMERO, ListaOs.Coluna.SERVICO, ListaOs.Coluna.STATUS,
                ListaOs.Coluna.CHEGADA, ListaOs.Coluna.PREVISAO);
        ListaOs.aoAbrir(ordens, recursos.navegacao()::abrirOs);
        anexosController.iniciar(recursos.oficina(), recursos.servicos(), recursos.tarefas(), new AnexosDoVeiculo(),
                this::mostrarOk,
                this::mostrarErros);
        anexosController.textos("Adicionar documento do veículo…", "Nenhum anexo. Adicione os documentos do veículo "
                + "(como o CRLV); as fotos e as notas do serviço entram pelas OS.");
        atualizar();
    }

    /** Lê tudo de novo (depois de editar, anonimizar ou anexar). */
    void atualizar() {
        ficha = recursos.oficina().ficha(veiculoId);
        Cliente cliente = ficha.cliente().cliente();
        Veiculo veiculo = ficha.veiculo().veiculo();
        String identificacao = veiculo.identificacao().isEmpty() ? "" : " · " + veiculo.identificacao();
        titulo.setText("Ficha do veículo · " + veiculo.descricao() + identificacao);
        subtitulo.setText(ficha.ordens().size() + (ficha.ordens().size() == 1 ? " OS" : " OS") + " · "
                + ficha.projetos().size() + (ficha.projetos().size() == 1 ? " projeto de preparação"
                        : " projetos de preparação") + " · cadastrado em " + Datas.dia(ficha.veiculo().criadoEm()
                                .toLocalDate()));

        clienteNome.setText(cliente.nome());
        List<String> contato = new ArrayList<>();
        for (String texto : List.of(cliente.telefone(), cliente.email(), cliente.documento())) {
            if (!texto.isBlank()) {
                contato.add(texto);
            }
        }
        clienteContato.setText(contato.isEmpty() ? "sem contato" : String.join(" · ", contato));
        Consentimento consentimento = cliente.consentimento();
        clienteConsentimento.setText("Consentimento (LGPD): " + (consentimento == null ? "não registrado"
                : Datas.dia(consentimento.data()) + " — " + consentimento.finalidade()));
        botaoEditarCliente.setDisable(cliente.anonimizado());
        botaoAnonimizar.setDisable(cliente.anonimizado());

        veiculoResumo.setText(veiculo.tipo().rotulo() + " · " + veiculo.descricao()
                + (veiculo.anoModelo() == null ? "" : " " + veiculo.anoModelo()) + identificacao);
        veiculoDetalhes.setText(detalhes(veiculo));

        ordens.getItems().setAll(ficha.ordens());
        numerosDasOs = ficha.ordens().stream().collect(Collectors.toMap(ResumoOs::osId, os -> os.numero().texto()));
        projetos.getItems().setAll(ficha.projetos());
        anexosController.atualizar();
    }

    /** O que mais está no cadastro: o uso, o câmbio, a tração, a cor e o chassi (ou a inscrição da embarcação). */
    private static String detalhes(Veiculo veiculo) {
        List<String> partes = new ArrayList<>();
        if (veiculo.usoAcumulado() != null) {
            partes.add(NumeroBr.formatar(veiculo.usoAcumulado(), 0) + " " + veiculo.tipo().unidadeUso());
        }
        if (veiculo.cambio() != null) {
            partes.add("câmbio " + veiculo.cambio().rotulo().toLowerCase());
        }
        if (veiculo.tracao() != null) {
            partes.add("tração " + veiculo.tracao().rotulo().toLowerCase());
        }
        if (!veiculo.cor().isEmpty()) {
            partes.add("cor " + veiculo.cor());
        }
        if (!veiculo.chassi().isEmpty()) {
            partes.add("chassi " + veiculo.chassi());
        }
        if (!veiculo.numeroInscricao().isEmpty()) {
            partes.add("inscrição " + veiculo.numeroInscricao());
        }
        return partes.isEmpty() ? "Sem mais dados no cadastro." : String.join(" · ", partes);
    }

    // ===================== Cliente e veículo =====================

    @FXML
    private void editarCliente() {
        if (JanelasCadastro.editarCliente(janela(), recursos.projetos(), ficha.cliente())) {
            atualizar();
            mostrarOk("Cliente salvo. As mudanças estão no histórico do cliente.");
        }
    }

    @FXML
    private void historicoCliente() {
        JanelasCadastro.historicoCliente(janela(), recursos.projetos(), ficha.cliente().id());
    }

    @FXML
    private void anonimizar() {
        if (JanelasCadastro.confirmarAnonimizacao(janela(), ficha.cliente().id())) {
            anonimizarCliente();
        }
    }

    /** Anonimiza o cliente e apaga os documentos pessoais (para os testes, sem a confirmação). */
    void anonimizarCliente() {
        try {
            Anonimizacao feita = recursos.oficina().anonimizarCliente(ficha.cliente().id());
            atualizar();
            int apagados = feita.documentos();
            mostrarOk("Cliente anonimizado." + (apagados == 0 ? "" : apagados == 1
                    ? " 1 documento pessoal foi apagado." : " " + apagados + " documentos pessoais foram apagados.")
                    + JanelasCadastro.avisoArquivosPresos(feita.arquivosPresos()));
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
    }

    @FXML
    private void editarVeiculo() {
        if (JanelasCadastro.editarVeiculo(janela(), recursos.projetos(), recursos.catalogo(), ficha.veiculo())) {
            atualizar();
            mostrarOk("Veículo salvo. As mudanças estão no histórico do veículo.");
        }
    }

    @FXML
    private void historicoVeiculo() {
        JanelasCadastro.historicoVeiculo(janela(), recursos.projetos(), veiculoId);
    }

    @FXML
    private void voltar() {
        voltar.run();
    }

    // ===================== Anexos =====================

    /** Os anexos de todas as OS do veículo e os do próprio veículo; os novos são documentos do veículo. */
    private final class AnexosDoVeiculo implements AnexosController.Fonte {

        @Override
        public List<AnexoSalvo> anexos() {
            return recursos.oficina().anexosDoVeiculo(veiculoId);
        }

        @Override
        public Callable<String> anexar(List<Path> arquivos) {
            List<NovoAnexo> novos = arquivos.stream().map(arquivo -> new NovoAnexo(arquivo, TipoAnexo.DOCUMENTO, "",
                    null, TipoAnexo.DOCUMENTO.pessoalPorPadrao())).toList();
            CasosOficina oficina = recursos.oficina();
            long veiculo = veiculoId;
            return () -> {
                oficina.anexarAoVeiculo(veiculo, novos);
                return (novos.size() == 1 ? "Documento anexado ao veículo." : novos.size() + " documentos anexados "
                        + "ao veículo.") + " Para pôr a validade (ex.: do CRLV) ou mudar o tipo, use Editar.";
            };
        }

        @Override
        public boolean podeRemover(AnexoSalvo anexo) {
            return anexo.osId() == null;
        }

        @Override
        public String tituloEscolha() {
            return "Documento do veículo";
        }

        @Override
        public String origem(AnexoSalvo anexo) {
            return anexo.osId() == null ? "do veículo" : numerosDasOs.getOrDefault(anexo.osId(), "OS");
        }
    }

    // ===================== Para os testes =====================

    /** A lista de anexos da ficha. */
    AnexosController anexos() {
        return anexosController;
    }

    List<String> mensagens() {
        return mensagens.getChildren().stream().map(no -> ((Label) no).getText()).toList();
    }

    // ===================== Utilidades =====================

    private void mostrarOk(String texto) {
        mensagens.getChildren().clear();
        mensagem(texto, "ok");
    }

    private void mostrarErros(List<String> erros) {
        mensagens.getChildren().clear();
        erros.forEach(erro -> mensagem("• " + erro, "erro"));
    }

    private void mensagem(String texto, String estilo) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add(estilo);
        rotulo.setWrapText(true);
        mensagens.getChildren().add(rotulo);
    }

    private static <T> void coluna(TableColumn<T, String> coluna, Function<T, String> texto) {
        coluna.setCellValueFactory(celula -> new ReadOnlyStringWrapper(texto.apply(celula.getValue())));
    }

    private Window janela() {
        return titulo.getScene() == null ? null : titulo.getScene().getWindow();
    }
}
