package br.com.calibra.app;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import br.com.calibra.app.ListaOs.Coluna;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.dados.oficina.FiltroOs;
import br.com.calibra.dados.oficina.ResumoOs;

/** Oficina (SPEC 8.9): a lista de OS, com busca por placa, cliente, embarcação ou número e o filtro por status. */
public class OficinaController {

    /**
     * Uma escolha do filtro de status: as OS em andamento (o padrão), as de um status só ou todas.
     *
     * @param status null = as em andamento, ou todas
     * @param todas com as entregues e as canceladas
     */
    record FiltroStatus(String rotulo, StatusOs status, boolean todas) {

        static final FiltroStatus EM_ANDAMENTO = new FiltroStatus("Em andamento", null, false);
        static final FiltroStatus TODAS = new FiltroStatus("Todas, com as entregues e as canceladas", null, true);

        FiltroOs filtro(String termo) {
            return new FiltroOs(termo, status == null ? Set.of() : Set.of(status), todas);
        }
    }

    @FXML private TextField busca;
    @FXML private ComboBox<FiltroStatus> filtroStatus;
    @FXML private TableView<ResumoOs> listaOs;
    @FXML private Label vazio;

    private final List<FiltroStatus> escolhas = new ArrayList<>();
    private CasosOficina casos;
    private Navegacao navegacao;

    @FXML
    private void initialize() {
        escolhas.add(FiltroStatus.EM_ANDAMENTO);
        for (StatusOs status : StatusOs.values()) {
            escolhas.add(new FiltroStatus(status.rotulo(), status, false));
        }
        escolhas.add(FiltroStatus.TODAS);
        CamposTela.configurar(filtroStatus, escolhas, FiltroStatus::rotulo);
        filtroStatus.setValue(FiltroStatus.EM_ANDAMENTO);
        busca.textProperty().addListener((obs, antes, depois) -> atualizar());
        filtroStatus.valueProperty().addListener((obs, antes, depois) -> atualizar());
    }

    void iniciar(CasosOficina casos, Navegacao navegacao, Clock relogio) {
        this.casos = casos;
        this.navegacao = navegacao;
        ListaOs.montar(listaOs, relogio, Coluna.NUMERO, Coluna.CLIENTE, Coluna.VEICULO, Coluna.SERVICO,
                Coluna.STATUS, Coluna.CHEGADA, Coluna.PREVISAO);
        atualizar();
    }

    /** Lê a lista de novo, com a busca e o filtro da tela. */
    void atualizar() {
        if (casos == null) {
            return;
        }
        FiltroStatus escolha = filtroStatus.getValue() == null ? FiltroStatus.EM_ANDAMENTO : filtroStatus.getValue();
        listaOs.getItems().setAll(casos.listar(escolha.filtro(busca.getText())));
        boolean filtrando = !busca.getText().isBlank() || escolha != FiltroStatus.EM_ANDAMENTO;
        vazio.setText(filtrando ? "Nenhuma OS com essa busca." : "Nenhuma OS em andamento. Clique em \"Nova OS\" "
                + "quando um veículo chegar ou um cliente pedir orçamento.");
    }

    @FXML
    private void novaOs() {
        navegacao.novaOs();
    }

    /** Mostra só as OS do status, sem a busca, lidas de novo (o cartão do Painel); null = as em andamento. */
    void filtrar(StatusOs status) {
        busca.clear();
        filtroStatus.setValue(escolhas.stream().filter(escolha -> escolha.status() == status && !escolha.todas())
                .findFirst().orElse(FiltroStatus.EM_ANDAMENTO));
        atualizar();
    }

    void focarBusca() {
        busca.requestFocus();
    }
}
