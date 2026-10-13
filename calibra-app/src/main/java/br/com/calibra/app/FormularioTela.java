package br.com.calibra.app;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import br.com.calibra.app.catalogo.CampoForm;
import br.com.calibra.app.catalogo.ValoresForm;

/**
 * Formulário montado a partir da lista de campos de um tipo de catálogo: rótulo à esquerda, campo à direita e a
 * dica embaixo. Lê e escreve tudo como texto ({@link ValoresForm}); quem confere é o formulário do catálogo.
 */
final class FormularioTela {

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Como ler e escrever um campo da tela, e o que esconder quando ele não se aplica. */
    private record CampoTela(CampoForm campo, Supplier<String> ler, Consumer<String> escrever, EditorTabela tabela,
            List<Node> nos, Node controle) {
    }

    private final GridPane grade = new GridPane();
    private final Map<String, CampoTela> campos = new LinkedHashMap<>();

    FormularioTela(List<CampoForm> lista) {
        grade.getStyleClass().add("grade");
        ColumnConstraints rotulos = new ColumnConstraints();
        rotulos.setMinWidth(Region.USE_PREF_SIZE);
        rotulos.setHalignment(HPos.LEFT);
        ColumnConstraints controles = new ColumnConstraints();
        controles.setHgrow(Priority.ALWAYS);
        controles.setFillWidth(true);
        grade.getColumnConstraints().setAll(rotulos, controles);
        int linha = 0;
        for (CampoForm campo : lista) {
            CampoTela tela = montar(campo);
            campos.put(campo.chave(), tela);
            if (campo.tipo() == CampoForm.Tipo.OCULTO) {
                continue;
            }
            Label rotulo = new Label(campo.tipo() == CampoForm.Tipo.MARCAR ? "" : campo.rotulo());
            rotulo.setLabelFor(tela.controle());
            GridPane.setValignment(rotulo, campo.tipo() == CampoForm.Tipo.TABELA || campo.tipo() == CampoForm.Tipo.AREA
                    ? VPos.TOP : VPos.CENTER);
            VBox celula = new VBox(2, tela.controle());
            if (!campo.dica().isEmpty()) {
                Label dica = new Label(campo.dica());
                dica.getStyleClass().add("dica");
                dica.setWrapText(true);
                celula.getChildren().add(dica);
            }
            grade.add(rotulo, 0, linha);
            grade.add(celula, 1, linha);
            tela.nos().addAll(List.of(rotulo, celula));
            linha++;
        }
        for (CampoTela tela : campos.values()) {
            CampoForm.Condicao condicao = tela.campo().mostrarSe();
            if (condicao != null) {
                CampoTela escolha = campos.get(condicao.chave());
                if (escolha != null && escolha.controle() instanceof ComboBox<?> caixa) {
                    caixa.valueProperty().addListener((obs, antes, depois) -> mostrarConforme(tela));
                }
                mostrarConforme(tela);
            }
        }
    }

    Node raiz() {
        return grade;
    }

    /** Escreve os valores (texto) nos campos. */
    void preencher(ValoresForm valores) {
        for (CampoTela tela : campos.values()) {
            if (tela.tabela() != null) {
                tela.tabela().preencher(valores.tabela(tela.campo().chave()));
            } else {
                tela.escrever().accept(valores.texto(tela.campo().chave()));
            }
        }
        campos.values().stream().filter(t -> t.campo().mostrarSe() != null).forEach(this::mostrarConforme);
    }

    /** O que está digitado, como texto. Campo escondido (que não se aplica) vai vazio. */
    ValoresForm valores() {
        ValoresForm valores = new ValoresForm();
        for (CampoTela tela : campos.values()) {
            if (tela.tabela() != null) {
                valores.tabela(tela.campo().chave(), tela.tabela().linhas());
            } else {
                valores.texto(tela.campo().chave(), tela.ler().get());
            }
        }
        for (CampoTela tela : campos.values()) {
            if (tela.campo().mostrarSe() != null && !tela.campo().mostrarSe().atende(valores)) {
                valores.texto(tela.campo().chave(), "");
            }
        }
        return valores;
    }

    /** Item já gravado: os campos que só valem no item novo (ex.: código do combustível) ficam travados. */
    void travarCamposFixos() {
        campos.values().stream().filter(t -> t.campo().fixoNaEdicao()).forEach(t -> t.controle().setDisable(true));
    }

    /** O controle de um campo, para os testes e para dar foco. */
    Node controle(String chave) {
        return campos.get(chave).controle();
    }

    EditorTabela tabela(String chave) {
        return campos.get(chave).tabela();
    }

    void focarPrimeiro() {
        campos.values().stream().map(CampoTela::controle).filter(n -> !n.isDisabled()).findFirst()
                .ifPresent(Node::requestFocus);
    }

    private void mostrarConforme(CampoTela tela) {
        CampoForm.Condicao condicao = tela.campo().mostrarSe();
        CampoTela escolha = campos.get(condicao.chave());
        boolean visivel = escolha != null && condicao.valores().contains(escolha.ler().get());
        for (Node no : tela.nos()) {
            no.setVisible(visivel);
            no.setManaged(visivel);
        }
    }

    private static CampoTela montar(CampoForm campo) {
        List<Node> nos = new ArrayList<>();
        switch (campo.tipo()) {
            case TEXTO, NUMERO, INTEIRO -> {
                TextField texto = new TextField();
                if (campo.tipo() != CampoForm.Tipo.TEXTO) {
                    texto.setPrefColumnCount(10);
                    texto.setMaxWidth(Region.USE_PREF_SIZE);
                } else {
                    texto.setPrefColumnCount(32);
                }
                texto.setAccessibleText(campo.rotulo());
                return new CampoTela(campo, texto::getText, texto::setText, null, nos, texto);
            }
            case AREA -> {
                TextArea area = new TextArea();
                area.setPrefRowCount(2);
                area.setWrapText(true);
                area.setAccessibleText(campo.rotulo());
                return new CampoTela(campo, area::getText, area::setText, null, nos, area);
            }
            case MARCAR -> {
                CheckBox marcar = new CheckBox(campo.rotulo());
                return new CampoTela(campo, () -> String.valueOf(marcar.isSelected()),
                        texto -> marcar.setSelected(Boolean.parseBoolean(texto)), null, nos, marcar);
            }
            case DATA -> {
                DatePicker data = new DatePicker();
                Datas.emPortugues(data);
                data.setAccessibleText(campo.rotulo());
                // Lê o texto digitado (e não o valor do seletor): data inválida vira mensagem em vez de sumir.
                return new CampoTela(campo, () -> data.getEditor().getText(), texto -> {
                    data.setValue(lerData(texto));
                    data.getEditor().setText(texto == null ? "" : texto);
                }, null, nos, data);
            }
            case ESCOLHA -> {
                ComboBox<CampoForm.Opcao> escolha = caixaDeEscolha(campo);
                return new CampoTela(campo, () -> escolha.getValue() == null ? "" : escolha.getValue().valor(),
                        texto -> escolha.setValue(campo.opcoes().stream().filter(o -> o.valor().equals(texto))
                                .findFirst().orElse(null)),
                        null, nos, escolha);
            }
            case TABELA -> {
                EditorTabela tabela = new EditorTabela(campo.rotulo(), campo.colunas());
                return new CampoTela(campo, () -> "", texto -> { }, tabela, nos, tabela);
            }
            case OCULTO -> {
                // Só guarda o valor: não entra na grade (o controle fica fora da tela e desligado).
                String[] valor = {""};
                Region nada = new Region();
                nada.setDisable(true);
                return new CampoTela(campo, () -> valor[0], texto -> valor[0] = texto == null ? "" : texto, null, nos,
                        nada);
            }
            default -> throw new IllegalStateException("Tipo de campo sem tela: " + campo.tipo());
        }
    }

    /** Escolha opcional ganha a linha "(não informado)"; a obrigatória começa vazia. */
    private static ComboBox<CampoForm.Opcao> caixaDeEscolha(CampoForm campo) {
        ComboBox<CampoForm.Opcao> caixa = new ComboBox<>();
        if (!campo.obrigatorio()) {
            caixa.getItems().add(null);
        }
        caixa.getItems().addAll(campo.opcoes());
        StringConverter<CampoForm.Opcao> conversor = new StringConverter<>() {
            @Override
            public String toString(CampoForm.Opcao opcao) {
                return opcao == null ? (campo.obrigatorio() ? "" : "(não informado)") : opcao.rotulo();
            }

            @Override
            public CampoForm.Opcao fromString(String texto) {
                return campo.opcoes().stream().filter(o -> o.rotulo().equals(texto)).findFirst().orElse(null);
            }
        };
        caixa.setConverter(conversor);
        caixa.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(CampoForm.Opcao opcao, boolean vazio) {
                super.updateItem(opcao, vazio);
                setText(vazio ? null : conversor.toString(opcao));
            }
        });
        caixa.setButtonCell(caixa.getCellFactory().call(null));
        caixa.setPromptText(campo.obrigatorio() ? "Escolha…" : "(não informado)");
        caixa.setAccessibleText(campo.rotulo());
        return caixa;
    }

    private static LocalDate lerData(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(texto.strip(), DIA);
        } catch (DateTimeParseException erro) {
            return null;
        }
    }
}
