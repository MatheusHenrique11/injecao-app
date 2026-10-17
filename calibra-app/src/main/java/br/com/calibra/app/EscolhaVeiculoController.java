package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.CasosProjeto.EscolhaVeiculo;
import br.com.calibra.app.casos.CasosProjeto.VeiculoExistente;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.util.TextoBusca;
import br.com.calibra.dados.cadastro.ClienteSalvo;
import br.com.calibra.dados.cadastro.VeiculoSalvo;

/** A etapa Veículo da Nova OS e do assistente de projeto: um veículo do cliente escolhido ou um veículo novo. */
public class EscolhaVeiculoController {

    @FXML private RadioButton veiculoExistente;
    @FXML private ListView<VeiculoSalvo> listaVeiculos;
    @FXML private RadioButton veiculoNovo;
    @FXML private VBox caixaVeiculoNovo;
    @FXML private VeiculoFormController formVeiculoController;

    private CasosProjeto casos;

    @FXML
    private void initialize() {
        listaVeiculos.setCellFactory(lista -> CamposTela.celula(v -> v.veiculo().descricao()
                + (v.veiculo().identificacao().isEmpty() ? "" : " · " + v.veiculo().identificacao())));
        listaVeiculos.disableProperty().bind(veiculoExistente.selectedProperty().not());
        caixaVeiculoNovo.disableProperty().bind(veiculoNovo.selectedProperty().not());
    }

    void iniciar(CasosProjeto casos, CasosCatalogo catalogo) {
        this.casos = casos;
        formVeiculoController.usarCatalogo(catalogo);
    }

    /**
     * Ao sair da etapa Cliente: os veículos do cliente escolhido, ou só "Veículo novo" para cliente novo. Já vem
     * escolhido o veículo cuja placa (ou nome da embarcação) bate com a busca da etapa Cliente; sem isso, o primeiro.
     *
     * @param cliente null = cliente novo
     */
    void prepararPara(ClienteSalvo cliente, String buscaDoCliente) {
        List<VeiculoSalvo> veiculos = cliente == null ? List.of() : casos.veiculosDoCliente(cliente.id());
        listaVeiculos.getItems().setAll(veiculos);
        veiculoExistente.setDisable(veiculos.isEmpty());
        if (veiculos.isEmpty()) {
            veiculoNovo.setSelected(true);
            return;
        }
        veiculoExistente.setSelected(true);
        List<String> palavras = TextoBusca.palavras(buscaDoCliente);
        VeiculoSalvo buscado = palavras.isEmpty() ? null : veiculos.stream().filter(v -> palavras.stream()
                .allMatch(TextoBusca.normalizar(v.veiculo().identificacao())::contains)).findFirst().orElse(null);
        if (buscado == null) {
            listaVeiculos.getSelectionModel().selectFirst();
        } else {
            listaVeiculos.getSelectionModel().select(buscado);
        }
    }

    /** Os erros da etapa, pelas mesmas regras que o caso de uso confere no fim. */
    List<String> conferir(int anoAtual) {
        if (veiculoExistente.isSelected() && listaVeiculos.getSelectionModel().getSelectedItem() == null) {
            return List.of("Escolha o veículo na lista ou marque \"Veículo novo\".");
        }
        if (!veiculoNovo.isSelected()) {
            return List.of();
        }
        VeiculoFormController.Leitura leitura = formVeiculoController.ler();
        List<String> erros = new ArrayList<>(leitura.erros());
        if (leitura.veiculo() != null) {
            erros.addAll(leitura.veiculo().validar(anoAtual));
        }
        return erros;
    }

    /** Os dados do veículo escolhido (do cadastro ou do formulário); null se o formulário tem erro de digitação. */
    Veiculo veiculo() {
        if (veiculoExistente.isSelected()) {
            VeiculoSalvo salvo = listaVeiculos.getSelectionModel().getSelectedItem();
            return salvo == null ? null : salvo.veiculo();
        }
        return formVeiculoController.ler().veiculo();
    }

    /** A escolha para o caso de uso, depois de {@link #conferir} sem erros. */
    EscolhaVeiculo escolha() {
        if (veiculoExistente.isSelected()) {
            return new VeiculoExistente(listaVeiculos.getSelectionModel().getSelectedItem().id());
        }
        return new VeiculoNovo(formVeiculoController.ler().veiculo(), formVeiculoController.cambioItemId());
    }
}
