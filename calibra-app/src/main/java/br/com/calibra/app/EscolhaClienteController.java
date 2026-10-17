package br.com.calibra.app;

import java.time.LocalDate;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.CasosProjeto.ClienteExistente;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.EscolhaCliente;
import br.com.calibra.dados.cadastro.ClienteSalvo;

/**
 * A etapa Cliente da Nova OS e do assistente de projeto: o cliente já cadastrado (a busca acha pelo nome, telefone,
 * documento, placa ou nome da embarcação) ou um cliente novo.
 */
public class EscolhaClienteController {

    @FXML private RadioButton clienteExistente;
    @FXML private VBox caixaClienteExistente;
    @FXML private TextField buscaCliente;
    @FXML private ListView<ClienteSalvo> listaClientes;
    @FXML private RadioButton clienteNovo;
    @FXML private VBox caixaClienteNovo;
    @FXML private ClienteFormController formClienteController;

    private CasosProjeto casos;

    @FXML
    private void initialize() {
        listaClientes.setCellFactory(lista -> CamposTela.celula(c -> c.cliente().nome() + " · "
                + c.cliente().telefone()));
        buscaCliente.textProperty().addListener((obs, antes, depois) -> pesquisar());
        caixaClienteExistente.disableProperty().bind(clienteExistente.selectedProperty().not());
        caixaClienteNovo.disableProperty().bind(clienteNovo.selectedProperty().not());
        listaClientes.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) -> {
            if (depois != null) {
                clienteExistente.setSelected(true);
            }
        });
    }

    /** Começa a etapa: a lista de clientes e o formulário em branco; sem nenhum cliente, já vem "Cliente novo". */
    void iniciar(CasosProjeto casos, LocalDate hoje) {
        this.casos = casos;
        formClienteController.limpar(hoje);
        pesquisar();
        boolean temClientes = !listaClientes.getItems().isEmpty();
        clienteExistente.setDisable(!temClientes);
        (temClientes ? clienteExistente : clienteNovo).setSelected(true);
    }

    /** Os erros da etapa, pelas mesmas regras que o caso de uso confere no fim. */
    List<String> conferir(LocalDate hoje) {
        if (clienteExistente.isSelected() && listaClientes.getSelectionModel().getSelectedItem() == null) {
            return List.of("Escolha o cliente na lista ou marque \"Cliente novo\".");
        }
        return clienteNovo.isSelected() ? formClienteController.ler().validar(hoje) : List.of();
    }

    /** O cliente escolhido na lista; null com "Cliente novo". */
    ClienteSalvo existente() {
        return clienteExistente.isSelected() ? listaClientes.getSelectionModel().getSelectedItem() : null;
    }

    /** O que está escrito na busca (a Nova OS usa a placa buscada para já escolher o veículo). */
    String busca() {
        return buscaCliente.getText();
    }

    /** A escolha para o caso de uso, depois de {@link #conferir} sem erros. */
    EscolhaCliente escolha() {
        if (clienteExistente.isSelected()) {
            return new ClienteExistente(listaClientes.getSelectionModel().getSelectedItem().id());
        }
        return new ClienteNovo(formClienteController.ler());
    }

    private void pesquisar() {
        if (casos != null) {
            listaClientes.getItems().setAll(casos.pesquisarClientes(buscaCliente.getText()));
        }
    }
}
