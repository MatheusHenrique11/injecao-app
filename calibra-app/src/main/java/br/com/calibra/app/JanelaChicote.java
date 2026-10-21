package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosChicote;
import br.com.calibra.app.casos.CasosChicote.OpcaoInjecao;
import br.com.calibra.app.casos.CasosChicote.OpcaoMotor;
import br.com.calibra.app.casos.CasosChicote.Opcoes;
import br.com.calibra.app.casos.FormularioChicote;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.oficina.FichaChicote;
import br.com.calibra.core.oficina.SituacaoChicote;
import br.com.calibra.core.oficina.SituacaoItem;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.oficina.FichaChicoteSalva;
import br.com.calibra.relatorios.DadosFichaChicote.ItemDeBancada;

/**
 * A janela da ficha do chicote (Fase 3B, SPEC 6.6.5): a injeção (do catálogo ou escrita), o motor do projeto, a versão,
 * a fonte do pinout, os pontos, os materiais, os testes de bancada (do checklist da OS, só para ver), as observações e
 * a situação. "Gravar" confere e grava; com erro, a janela fica aberta e mostra o que corrigir. A versão que já tem uma
 * nova e a ficha da OS entregue ou cancelada abrem só para ver.
 */
final class JanelaChicote {

    private static final List<EditorLinhas.Coluna> PONTOS = List.of(
            EditorLinhas.Coluna.texto("Função", 200, "ex.: bico do cilindro 1"),
            EditorLinhas.Coluna.texto("Conector", 90, "ex.: A"),
            EditorLinhas.Coluna.texto("Pino da injeção", 110, ""),
            EditorLinhas.Coluna.texto("Observação", 250, "opcional"),
            EditorLinhas.Coluna.marcar("A verificar"));

    private final CasosChicote casos;
    private final long osId;
    private final FichaChicoteSalva salva;
    private final Long anteriorId;
    private final ComboBox<OpcaoInjecao> injecao = new ComboBox<>();
    private final TextField injecaoTexto = new TextField();
    private final ComboBox<OpcaoMotor> motor = new ComboBox<>();
    private final TextField versao = new TextField();
    private final TextField fontePinout = new TextField();
    private final EditorLinhas pontos = new EditorLinhas("+ Ponto", "Nenhum ponto anotado.", PONTOS);
    private final TextArea materiais = area("Materiais");
    private final Label bancada = new Label();
    private final TextArea observacoes = area("Observações");
    private final ComboBox<SituacaoChicote> situacao = new ComboBox<>();
    private final VBox erros = new VBox(4);
    private final Dialog<ButtonType> dialogo = new Dialog<>();
    private final ButtonType gravar = new ButtonType("Gravar", ButtonBar.ButtonData.OK_DONE);
    private boolean gravou;
    private String mensagem = "";

    /** A ficha nova da OS, já preenchida (pelo projeto de preparação, quando há). */
    static JanelaChicote nova(Window dono, CasosChicote casos, long osId, String numeroOs) {
        return new JanelaChicote(dono, casos, osId, numeroOs, null, null, casos.nova(osId), false);
    }

    /**
     * A ficha gravada.
     *
     * @param leitura a OS foi entregue ou cancelada: só para ver (a versão que já tem uma nova também abre assim)
     */
    static JanelaChicote abrir(Window dono, CasosChicote casos, FichaChicoteSalva salva, String numeroOs,
            boolean leitura) {
        return new JanelaChicote(dono, casos, salva.osId(), numeroOs, salva, null, salva.ficha(),
                leitura || salva.temVersaoNova());
    }

    /** A versão nova, começando desta (que fica só para ver depois de gravar a nova). */
    static JanelaChicote versaoNova(Window dono, CasosChicote casos, FichaChicoteSalva anterior, String numeroOs) {
        return new JanelaChicote(dono, casos, anterior.osId(), numeroOs, null, anterior.id(),
                casos.versaoNova(anterior.id()), false);
    }

    private JanelaChicote(Window dono, CasosChicote casos, long osId, String numeroOs, FichaChicoteSalva salva,
            Long anteriorId, FichaChicote ficha, boolean leitura) {
        this.casos = casos;
        this.osId = osId;
        this.salva = salva;
        this.anteriorId = anteriorId;
        Opcoes opcoes = casos.opcoes(osId);
        List<OpcaoInjecao> injecoes = new ArrayList<>(opcoes.injecoes());
        if (ficha.injecaoItemId() != null && injecoes.stream().noneMatch(o -> o.id() == ficha.injecaoItemId())) {
            injecoes.add(new OpcaoInjecao(ficha.injecaoItemId(), ficha.injecao()));
        }
        CamposTela.configurarOpcional(injecao, injecoes, OpcaoInjecao::nome, "(texto livre)");
        injecao.setAccessibleText("Injeção do catálogo");
        injecaoTexto.setPrefColumnCount(24);
        injecaoTexto.setPromptText("a injeção, se não está no catálogo");
        injecaoTexto.setAccessibleText("Injeção");
        injecao.valueProperty().addListener((obs, antes, depois) -> mostrar(injecaoTexto, depois == null));
        CamposTela.configurarOpcional(motor, opcoes.motores(), OpcaoMotor::rotulo, "(nenhuma)");
        motor.setAccessibleText("Motor");
        versao.setPrefColumnCount(8);
        versao.setAccessibleText("Versão do chicote");
        fontePinout.setPrefColumnCount(40);
        fontePinout.setPromptText("ex.: manual da injeção, página da pinagem");
        fontePinout.setAccessibleText("Fonte do pinout");
        List<SituacaoChicote> situacoes = new ArrayList<>(List.of(SituacaoChicote.EM_CONFECCAO,
                SituacaoChicote.TESTADO));
        if (ficha.situacao() == SituacaoChicote.ENTREGUE) {
            situacoes.add(SituacaoChicote.ENTREGUE);
        }
        CamposTela.configurar(situacao, situacoes, s -> JanelaLaudo.maiuscula(s.rotulo()));
        situacao.setAccessibleText("Situação");
        bancada.getStyleClass().add("dica");
        bancada.setWrapText(true);
        bancada.setText(textoDaBancada(casos.bancada(osId)));
        preencher(ficha);
        ScrollPane rolagem = new ScrollPane(conteudo(!opcoes.motores().isEmpty()));
        rolagem.setFitToWidth(true);
        rolagem.setPrefViewportWidth(980);
        rolagem.setPrefViewportHeight(560);
        if (dono != null) {
            dialogo.initOwner(dono);
        }
        dialogo.setTitle(salva == null ? "Nova ficha do chicote" : "Ficha do chicote");
        dialogo.setHeaderText("Ficha do chicote · " + numeroOs + (anteriorId == null ? "" : " · versão nova") + (leitura
                ? " · só para ver (" + (salva != null && salva.temVersaoNova() ? "já tem uma versão nova"
                        : "a OS foi entregue ou cancelada") + ")" : ""));
        dialogo.getDialogPane().setContent(new VBox(10, rolagem, erros));
        dialogo.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        if (leitura) {
            somenteLeitura();
            dialogo.getDialogPane().getButtonTypes().setAll(new ButtonType("Fechar",
                    ButtonBar.ButtonData.CANCEL_CLOSE));
        } else {
            dialogo.getDialogPane().getButtonTypes().setAll(gravar,
                    new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
            dialogo.getDialogPane().lookupButton(gravar).addEventFilter(ActionEvent.ACTION, evento -> {
                if (!gravar()) {
                    evento.consume();
                }
            });
        }
        dialogo.setResizable(true);
    }

    /** Mostra a janela e espera; devolve se gravou. */
    boolean mostrar() {
        Platform.runLater(fontePinout::requestFocus);
        dialogo.showAndWait();
        return gravou;
    }

    /**
     * Confere e grava o que está na janela (o que o botão Gravar faz).
     *
     * @return false se havia erro: as mensagens ficam na janela e nada é gravado
     */
    boolean gravar() {
        FichaChicote lida = FormularioChicote.ler(injecao.getValue(), injecaoTexto.getText(),
                motor.getValue() == null ? null : motor.getValue().id(), versao.getText(), fontePinout.getText(),
                pontos.linhas(), materiais.getText(), observacoes.getText(), situacao.getValue());
        try {
            if (anteriorId != null) {
                casos.criarVersaoNova(anteriorId, lida);
                mensagem = "Versão nova do chicote gravada. A anterior fica guardada, só para ver.";
            } else if (salva == null) {
                casos.criar(osId, lida);
                mensagem = "Ficha do chicote gravada.";
            } else {
                casos.salvar(salva.id(), lida);
                mensagem = "Ficha do chicote salva.";
            }
            gravou = true;
            return true;
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
        return false;
    }

    /** O que aconteceu na gravação, para a tela da OS. */
    String mensagem() {
        return mensagem;
    }

    Dialog<ButtonType> dialogo() {
        return dialogo;
    }

    /** As mensagens de erro mostradas na janela. */
    List<String> erros() {
        return erros.getChildren().stream().map(no -> ((Label) no).getText().substring(2)).toList();
    }

    // ===================== Campos, para os testes =====================

    ComboBox<OpcaoInjecao> injecao() {
        return injecao;
    }

    TextField injecaoTexto() {
        return injecaoTexto;
    }

    ComboBox<OpcaoMotor> motor() {
        return motor;
    }

    TextField versao() {
        return versao;
    }

    TextField fontePinout() {
        return fontePinout;
    }

    EditorLinhas pontos() {
        return pontos;
    }

    ComboBox<SituacaoChicote> situacao() {
        return situacao;
    }

    Label bancada() {
        return bancada;
    }

    // ===================== Montagem =====================

    private void preencher(FichaChicote ficha) {
        injecao.setValue(ficha.injecaoItemId() == null ? null : injecao.getItems().stream()
                .filter(o -> o != null && o.id() == ficha.injecaoItemId()).findFirst().orElse(null));
        injecaoTexto.setText(ficha.injecaoItemId() == null ? ficha.injecao() : "");
        mostrar(injecaoTexto, injecao.getValue() == null);
        motor.setValue(ficha.motorVersaoId() == null ? null : motor.getItems().stream()
                .filter(o -> o != null && o.id() == ficha.motorVersaoId()).findFirst().orElse(null));
        versao.setText(ficha.versaoChicote());
        fontePinout.setText(ficha.fontePinout());
        pontos.preencher(FormularioChicote.linhasDosPontos(ficha.pontos()));
        materiais.setText(ficha.materiais());
        observacoes.setText(ficha.observacoes());
        situacao.setValue(ficha.situacao());
    }

    private Node conteudo(boolean temProjeto) {
        GridPane grade = new GridPane();
        grade.getStyleClass().add("grade");
        ColumnConstraints rotulos = new ColumnConstraints();
        rotulos.setMinWidth(135);
        ColumnConstraints campos = new ColumnConstraints();
        campos.setHgrow(Priority.ALWAYS);
        campos.setFillWidth(true);
        grade.getColumnConstraints().setAll(rotulos, campos);
        int linha = 0;
        linha = linha(grade, linha, "Injeção", new HBox(8, injecao, injecaoTexto), "Do catálogo de injeções ou, "
                + "com \"(texto livre)\", escrita. Com o projeto de preparação ligado à OS, já vem a injeção dele.");
        if (temProjeto) {
            linha = linha(grade, linha, "Motor", motor, "A versão do motor do projeto de preparação da OS.");
        }
        linha = linha(grade, linha, "Versão do chicote", versao, "A versão nova não apaga a anterior.");
        linha = linha(grade, linha, "Fonte do pinout", fontePinout, "Obrigatória: de onde veio a pinagem (o manual "
                + "da injeção, o modelo do fabricante).");
        linha = linha(grade, linha, "Pontos", pontos, "Cada fio: a função, o conector e o pino da injeção. O app não "
                + "confere pinagem nem bitola; o ponto ainda não conferido no manual fica marcado \"a verificar\".");
        linha = linha(grade, linha, "Materiais", materiais, "Cabo, bitola, conectores, proteção (texto livre).");
        linha = linha(grade, linha, "Testes de bancada", bancada, "");
        linha = linha(grade, linha, "Observações", observacoes, "Opcional. As fotos ficam nos anexos da OS.");
        linha(grade, linha, "Situação", situacao, "\"Entregue\" vem sozinha quando a OS é entregue.");
        return grade;
    }

    private static int linha(GridPane grade, int linha, String rotulo, Node campo, String dica) {
        Label titulo = new Label(rotulo);
        GridPane.setValignment(titulo, VPos.TOP);
        VBox celula = new VBox(2, campo);
        if (!dica.isEmpty()) {
            Label texto = new Label(dica);
            texto.getStyleClass().add("dica");
            texto.setWrapText(true);
            celula.getChildren().add(texto);
        }
        grade.add(titulo, 0, linha);
        grade.add(celula, 1, linha);
        return linha + 1;
    }

    /** "Do checklist da aba Execução: 3 de 12 itens feitos. O PDF traz cada item, com quem marcou e quando." */
    private static String textoDaBancada(List<ItemDeBancada> itens) {
        if (itens.isEmpty()) {
            return "Esta OS não tem checklist de bancada (o da confecção do chicote, na aba Execução).";
        }
        long feitos = itens.stream().filter(i -> i.situacao() == SituacaoItem.FEITO).count();
        return "Do checklist da aba Execução: " + feitos + " de " + itens.size() + (itens.size() == 1
                ? " item feito." : " itens feitos.") + " O PDF traz cada item, com quem marcou e quando.";
    }

    private void somenteLeitura() {
        for (Node campo : List.of(injecao, injecaoTexto, motor, versao, fontePinout, situacao)) {
            campo.setDisable(true);
        }
        for (TextArea texto : List.of(materiais, observacoes)) {
            texto.setEditable(false);
        }
        pontos.somenteLeitura(true);
    }

    private void mostrarErros(List<String> mensagens) {
        erros.getChildren().clear();
        for (String erro : mensagens) {
            Label rotulo = new Label("• " + erro);
            rotulo.getStyleClass().add("erro");
            rotulo.setWrapText(true);
            erros.getChildren().add(rotulo);
        }
    }

    private static void mostrar(Node no, boolean visivel) {
        no.setVisible(visivel);
        no.setManaged(visivel);
    }

    private static TextArea area(String rotulo) {
        TextArea area = new TextArea();
        area.setPrefRowCount(2);
        area.setWrapText(true);
        area.setAccessibleText(rotulo);
        return area;
    }
}
