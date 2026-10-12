package br.com.calibra.app;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.dados.projeto.FiltroProjetos;
import br.com.calibra.dados.projeto.ResumoProjeto;

/** Início (SPEC 8.1): lista de projetos, busca, filtros e "Novo projeto". */
public class InicioController {

    @FXML private TextField busca;
    @FXML private ComboBox<StatusProjeto> filtroStatus;
    @FXML private CheckBox mostrarArquivados;
    @FXML private TableView<ResumoProjeto> tabela;
    @FXML private Label vazio;
    @FXML private TableColumn<ResumoProjeto, String> colunaCliente;
    @FXML private TableColumn<ResumoProjeto, String> colunaVeiculo;
    @FXML private TableColumn<ResumoProjeto, String> colunaIdentificacao;
    @FXML private TableColumn<ResumoProjeto, String> colunaMotor;
    @FXML private TableColumn<ResumoProjeto, String> colunaStatus;
    @FXML private TableColumn<ResumoProjeto, String> colunaAtualizado;

    private CasosProjeto casos;
    private Navegacao navegacao;

    @FXML
    private void initialize() {
        CamposTela.configurarOpcional(filtroStatus, Arrays.asList(StatusProjeto.values()), StatusProjeto::rotulo,
                "Todos");
        coluna(colunaCliente, ResumoProjeto::cliente);
        coluna(colunaVeiculo, ResumoProjeto::veiculo);
        coluna(colunaIdentificacao, ResumoProjeto::identificacao);
        coluna(colunaMotor, ResumoProjeto::motor);
        coluna(colunaStatus, r -> r.status().rotulo());
        coluna(colunaAtualizado, r -> Datas.diaHora(r.atualizadoEm()));
        tabela.setRowFactory(lista -> {
            TableRow<ResumoProjeto> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2 && !linha.isEmpty()) {
                    navegacao.abrirProjeto(linha.getItem().projetoId());
                }
            });
            return linha;
        });
        tabela.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER && tabela.getSelectionModel().getSelectedItem() != null) {
                navegacao.abrirProjeto(tabela.getSelectionModel().getSelectedItem().projetoId());
            }
        });
        busca.textProperty().addListener((obs, antes, depois) -> atualizar());
        filtroStatus.valueProperty().addListener((obs, antes, depois) -> atualizar());
        mostrarArquivados.selectedProperty().addListener((obs, antes, depois) -> atualizar());
    }

    void iniciar(CasosProjeto casos, Navegacao navegacao) {
        this.casos = casos;
        this.navegacao = navegacao;
        atualizar();
    }

    /** Lê a lista de novo, com a busca e os filtros da tela. */
    void atualizar() {
        if (casos == null) {
            return;
        }
        FiltroProjetos filtro = new FiltroProjetos(busca.getText(), filtroStatus.getValue(),
                mostrarArquivados.isSelected());
        List<ResumoProjeto> projetos = casos.pesquisar(filtro);
        tabela.getItems().setAll(projetos);
        boolean filtrando = !busca.getText().isBlank() || filtroStatus.getValue() != null;
        vazio.setText(filtrando ? "Nenhum projeto com essa busca."
                : "Nenhum projeto ainda. Clique em \"Novo projeto\" para cadastrar o cliente, o veículo e o motor.");
    }

    @FXML
    private void novoProjeto() {
        navegacao.novoProjeto();
    }

    void focarBusca() {
        busca.requestFocus();
    }

    private static void coluna(TableColumn<ResumoProjeto, String> coluna, Function<ResumoProjeto, String> valor) {
        coluna.setCellValueFactory(celula -> new ReadOnlyStringWrapper(valor.apply(celula.getValue())));
    }
}
