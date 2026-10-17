package br.com.calibra.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.core.util.NumeroBr;

/**
 * A entrada do veículo (SPEC 6.5): o dia e a hora da chegada, o km (ou as horas de motor, no náutico), o tanque, a
 * vistoria com a observação de cada item e os objetos deixados. Usada na Nova OS e na aba Entrada da OS.
 */
public class EntradaFormController {

    /** A hora aparece como 09:30 e é lida também como 9:30. */
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter HORA_DIGITADA = DateTimeFormatter.ofPattern("H:mm");

    @FXML private DatePicker entradaData;
    @FXML private TextField entradaHora;
    @FXML private Label rotuloUso;
    @FXML private TextField usoEntrada;
    @FXML private Label dicaUso;
    @FXML private ComboBox<NivelCombustivel> combustivel;
    @FXML private GridPane gradeVistoria;

    /** Os itens que a vistoria mostra e a observação de cada um, na mesma ordem. */
    private final List<String> itens = new ArrayList<>();
    private final List<TextField> observacoes = new ArrayList<>();
    private final TextField objetos = new TextField();

    @FXML
    private void initialize() {
        Datas.emPortugues(entradaData);
        CamposTela.configurarOpcional(combustivel, Arrays.asList(NivelCombustivel.values()), NivelCombustivel::rotulo);
        objetos.setId("objetos");
        objetos.setPromptText("ex.: estepe, macaco, óculos no porta-luvas");
        montarVistoria(Vistoria.ITENS_PADRAO);
    }

    /** Entrada nova: a chegada agora, a vistoria padrão em branco. */
    void limpar(LocalDateTime agora) {
        entradaData.setValue(agora.toLocalDate());
        entradaHora.setText(HORA.format(agora));
        usoEntrada.clear();
        combustivel.setValue(null);
        montarVistoria(Vistoria.ITENS_PADRAO);
        objetos.clear();
    }

    /** A entrada gravada, para corrigir; a vistoria mostra os itens dela (sem nenhum, os padrão). */
    void preencher(EntradaVeiculo entrada) {
        entradaData.setValue(entrada.quando().toLocalDate());
        entradaHora.setText(HORA.format(entrada.quando()));
        usoEntrada.setText(entrada.uso() == null ? "" : NumeroBr.formatar(entrada.uso(), 0, 1));
        combustivel.setValue(entrada.combustivel());
        List<ItemVistoria> gravados = entrada.vistoria().itens();
        montarVistoria(gravados.isEmpty() ? Vistoria.ITENS_PADRAO : gravados.stream().map(ItemVistoria::item).toList());
        for (int i = 0; i < gravados.size(); i++) {
            observacoes.get(i).setText(gravados.get(i).observacao());
        }
        objetos.setText(entrada.vistoria().objetos());
    }

    /** O nome do campo de uso (km ou horas de motor) e o valor que está no cadastro do veículo. */
    void usarVeiculo(Veiculo veiculo) {
        boolean nautico = veiculo != null && veiculo.tipo() != null && veiculo.tipo().nautico();
        rotuloUso.setText(nautico ? "Horas de motor" : "Km no painel");
        Double noCadastro = veiculo == null ? null : veiculo.usoAcumulado();
        dicaUso.setText(noCadastro == null ? "" : "no cadastro: " + NumeroBr.formatar(noCadastro, 0)
                + (nautico ? " h" : " km"));
    }

    /** A entrada como está na tela; null se o dia ou a hora não deram para ler (o erro vai para a lista). */
    EntradaVeiculo ler(List<String> erros) {
        LocalDate dia = entradaData.getValue();
        if (dia == null) {
            erros.add("Informe o dia da chegada do veículo.");
        }
        LocalTime hora = null;
        try {
            hora = LocalTime.parse(entradaHora.getText().strip(), HORA_DIGITADA);
        } catch (DateTimeParseException erro) {
            erros.add("Hora da chegada: use horas e minutos, como 09:30.");
        }
        Double uso = null;
        String digitado = usoEntrada.getText();
        if (digitado != null && !digitado.isBlank()) {
            try {
                uso = NumeroBr.ler(digitado);
            } catch (NumberFormatException erro) {
                erros.add(rotuloUso.getText() + ": " + erro.getMessage());
            }
        }
        List<ItemVistoria> vistoria = new ArrayList<>();
        for (int i = 0; i < itens.size(); i++) {
            vistoria.add(new ItemVistoria(itens.get(i), observacoes.get(i).getText()));
        }
        if (dia == null || hora == null) {
            return null;
        }
        return new EntradaVeiculo(LocalDateTime.of(dia, hora), uso, combustivel.getValue(),
                new Vistoria(vistoria, objetos.getText()));
    }

    /** Só se edita com a OS em andamento. */
    void habilitar(boolean editavel) {
        entradaData.setDisable(!editavel);
        entradaHora.setDisable(!editavel);
        usoEntrada.setDisable(!editavel);
        combustivel.setDisable(!editavel);
        observacoes.forEach(campo -> campo.setDisable(!editavel));
        objetos.setDisable(!editavel);
    }

    /** Um campo de observação para cada item (ids vistoria0, vistoria1…) e, no fim, os objetos deixados. */
    private void montarVistoria(List<String> nomes) {
        gradeVistoria.getChildren().clear();
        itens.clear();
        observacoes.clear();
        for (String nome : nomes) {
            TextField observacao = new TextField();
            observacao.setId("vistoria" + observacoes.size());
            observacao.setPromptText("Nada a anotar");
            gradeVistoria.addRow(observacoes.size(), new Label(nome), observacao);
            itens.add(nome);
            observacoes.add(observacao);
        }
        gradeVistoria.addRow(observacoes.size(), new Label("Objetos deixados no veículo"), objetos);
    }
}
