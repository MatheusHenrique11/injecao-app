package br.com.calibra.app;

import java.time.LocalDate;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;

/** Formulário do cliente (SPEC 6.1), usado no assistente e em "Editar cliente". */
public class ClienteFormController {

    @FXML private TextField nome;
    @FXML private TextField telefone;
    @FXML private TextField email;
    @FXML private TextField documento;
    @FXML private TextArea observacoes;
    @FXML private CheckBox consentiu;
    @FXML private DatePicker consentimentoData;
    @FXML private TextField consentimentoFinalidade;

    @FXML
    private void initialize() {
        Datas.emPortugues(consentimentoData);
        consentimentoData.disableProperty().bind(consentiu.selectedProperty().not());
        consentimentoFinalidade.disableProperty().bind(consentiu.selectedProperty().not());
        limpar(LocalDate.now());
    }

    /** Formulário de cliente novo: consentimento com a data de hoje e a finalidade sugerida, ainda não marcado. */
    void limpar(LocalDate hoje) {
        nome.clear();
        telefone.clear();
        email.clear();
        documento.clear();
        observacoes.clear();
        consentiu.setSelected(false);
        consentimentoData.setValue(hoje);
        consentimentoFinalidade.setText(Consentimento.FINALIDADE_PADRAO);
    }

    void preencher(Cliente cliente) {
        nome.setText(cliente.nome());
        telefone.setText(cliente.telefone());
        email.setText(cliente.email());
        documento.setText(cliente.documento());
        observacoes.setText(cliente.observacoes());
        Consentimento consentimento = cliente.consentimento();
        consentiu.setSelected(consentimento != null);
        if (consentimento != null) {
            consentimentoData.setValue(consentimento.data());
            consentimentoFinalidade.setText(consentimento.finalidade());
        }
    }

    /** O cliente como está nos campos; quem confere as regras é {@link Cliente#validar}. */
    Cliente ler() {
        Consentimento consentimento = consentiu.isSelected()
                ? new Consentimento(consentimentoData.getValue(), consentimentoFinalidade.getText())
                : null;
        return Cliente.novo(nome.getText(), telefone.getText(), email.getText(), documento.getText(), consentimento,
                observacoes.getText());
    }

    /** Põe o cursor no primeiro campo. */
    void focar() {
        nome.requestFocus();
    }
}
