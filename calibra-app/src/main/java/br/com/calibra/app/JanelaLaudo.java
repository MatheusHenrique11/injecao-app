package br.com.calibra.app;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
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

import br.com.calibra.app.casos.CasosLaudo;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.app.casos.FormularioLaudo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.oficina.CondicaoMedicao;
import br.com.calibra.core.oficina.LaudoEletrico;
import br.com.calibra.core.oficina.ResultadoLaudo;
import br.com.calibra.core.oficina.SituacaoCodigo;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.oficina.LaudoSalvo;
import br.com.calibra.dados.oficina.MecanicoSalvo;

/**
 * A janela do laudo de diagnóstico elétrico (Fase 3B, SPEC 6.6.3): a data e a hora, o mecânico, a queixa (com a dica
 * de LGPD), as medições, os códigos de falha, o diagnóstico e o resultado. "Gravar" confere e grava; com erro, a
 * janela fica aberta e mostra o que corrigir. Com a OS entregue ou cancelada, abre só para ver.
 */
final class JanelaLaudo {

    static final String DICA_QUEIXA = "Nas palavras do cliente, sem dados pessoais (nome, telefone, documento, "
            + "e-mail): o laudo é dado técnico e fica mesmo se o cliente for anonimizado.";

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static final List<EditorLinhas.Coluna> MEDICOES = List.of(
            EditorLinhas.Coluna.texto("Medição", 180, "ex.: tensão da bateria"),
            EditorLinhas.Coluna.texto("Valor", 64, ""),
            EditorLinhas.Coluna.texto("Unidade", 56, "V, A, Ω"),
            EditorLinhas.Coluna.escolha("Condição", 136, CondicaoMedicao.values(),
                    condicao -> maiuscula(condicao.rotulo())),
            EditorLinhas.Coluna.texto("Referência", 136, "opcional"),
            EditorLinhas.Coluna.marcar("A verificar"));

    private static final List<EditorLinhas.Coluna> CODIGOS = List.of(
            EditorLinhas.Coluna.texto("Código", 100, "ex.: P0300"),
            EditorLinhas.Coluna.texto("Descrição", 360, "opcional"),
            EditorLinhas.Coluna.escolha("Situação", 140, SituacaoCodigo.values(),
                    situacao -> maiuscula(situacao.rotulo())));

    private final CasosLaudo casos;
    private final long osId;
    private final LaudoSalvo laudo;
    private final DatePicker data = new DatePicker();
    private final TextField hora = new TextField();
    private final ComboBox<MecanicoSalvo> mecanico = new ComboBox<>();
    private final CaixaMecanico caixaMecanico;
    private final TextArea queixa = area(3, "Queixa do cliente");
    private final EditorLinhas medicoes = new EditorLinhas("+ Medição", "Nenhuma medição anotada.", MEDICOES);
    private final EditorLinhas codigos = new EditorLinhas("+ Código de falha", "Nenhum código de falha anotado.",
            CODIGOS);
    private final TextArea testes = area(2, "Testes realizados");
    private final TextArea causa = area(2, "Causa encontrada");
    private final TextArea solucao = area(2, "Solução");
    private final TextArea recomendacoes = area(2, "Recomendações");
    private final ComboBox<ResultadoLaudo> resultado = new ComboBox<>();
    private final VBox erros = new VBox(4);
    private final Dialog<ButtonType> dialogo = new Dialog<>();
    private final ButtonType gravar = new ButtonType("Gravar", ButtonBar.ButtonData.OK_DONE);
    private boolean gravou;
    private String mensagem = "";
    private List<String> avisos = List.of();

    /**
     * @param laudo null para o laudo novo, que já vem preenchido (agora, a queixa e o mecânico da OS)
     * @param leitura a OS foi entregue ou cancelada: a janela só mostra
     */
    JanelaLaudo(Window dono, CasosLaudo casos, CasosOficina oficina, long osId, String numeroOs, LaudoSalvo laudo,
            boolean leitura) {
        this.casos = casos;
        this.osId = osId;
        this.laudo = laudo;
        this.caixaMecanico = new CaixaMecanico(mecanico, oficina);
        Datas.emPortugues(data);
        hora.setPrefColumnCount(5);
        hora.setAccessibleText("Hora");
        CamposTela.configurarOpcional(resultado, List.of(ResultadoLaudo.values()), r -> maiuscula(r.rotulo()),
                "(a definir)");
        if (laudo == null) {
            CasosLaudo.Rascunho rascunho = casos.rascunho(osId);
            preencher(rascunho.laudo(), rascunho.mecanicoId());
        } else {
            preencher(laudo.laudo(), laudo.mecanicoId());
        }
        ScrollPane rolagem = new ScrollPane(conteudo());
        rolagem.setFitToWidth(true);
        rolagem.setPrefViewportWidth(990);
        rolagem.setPrefViewportHeight(560);
        if (dono != null) {
            dialogo.initOwner(dono);
        }
        dialogo.setTitle(laudo == null ? "Novo laudo" : "Laudo");
        dialogo.setHeaderText("Laudo de diagnóstico elétrico · " + numeroOs + (leitura
                ? " · a OS foi entregue ou cancelada: o laudo fica só para ver." : ""));
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
        Platform.runLater(queixa::requestFocus);
        dialogo.showAndWait();
        return gravou;
    }

    /**
     * Confere e grava o que está na janela (o que o botão Gravar faz).
     *
     * @return false se havia erro: as mensagens ficam na janela e nada é gravado
     */
    boolean gravar() {
        List<String> lidos = new ArrayList<>();
        LocalDateTime quando = JanelasOs.quando(data, hora, lidos);
        if (!lidos.isEmpty()) {
            mostrarErros(lidos);
            return false;
        }
        LaudoEletrico lido = FormularioLaudo.ler(quando, queixa.getText(), medicoes.linhas(), codigos.linhas(),
                testes.getText(), causa.getText(), solucao.getText(), recomendacoes.getText(), resultado.getValue());
        try {
            if (laudo == null) {
                avisos = casos.criar(osId, lido, caixaMecanico.escolhido()).avisos();
                mensagem = "Laudo gravado.";
            } else {
                avisos = casos.salvar(laudo.id(), lido, caixaMecanico.escolhido());
                mensagem = "Laudo salvo.";
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

    /** Os avisos que não impediram (o de LGPD). */
    List<String> avisos() {
        return avisos;
    }

    Dialog<ButtonType> dialogo() {
        return dialogo;
    }

    /** As mensagens de erro mostradas na janela. */
    List<String> erros() {
        return erros.getChildren().stream().map(no -> ((Label) no).getText().substring(2)).toList();
    }

    // ===================== Campos, para os testes =====================

    TextArea queixa() {
        return queixa;
    }

    TextField hora() {
        return hora;
    }

    ComboBox<MecanicoSalvo> mecanico() {
        return mecanico;
    }

    EditorLinhas medicoes() {
        return medicoes;
    }

    EditorLinhas codigos() {
        return codigos;
    }

    TextArea causa() {
        return causa;
    }

    TextArea solucao() {
        return solucao;
    }

    ComboBox<ResultadoLaudo> resultado() {
        return resultado;
    }

    // ===================== Montagem =====================

    private void preencher(LaudoEletrico conteudo, Long mecanicoId) {
        if (conteudo.dataHora() != null) {
            data.setValue(conteudo.dataHora().toLocalDate());
            hora.setText(HORA.format(conteudo.dataHora()));
        }
        caixaMecanico.carregar(mecanicoId);
        queixa.setText(conteudo.queixa());
        medicoes.preencher(FormularioLaudo.linhasDasMedicoes(conteudo.medicoes()));
        codigos.preencher(FormularioLaudo.linhasDosCodigos(conteudo.codigos()));
        testes.setText(conteudo.testesRealizados());
        causa.setText(conteudo.causa());
        solucao.setText(conteudo.solucao());
        recomendacoes.setText(conteudo.recomendacoes());
        resultado.setValue(conteudo.resultado());
    }

    private Node conteudo() {
        GridPane grade = new GridPane();
        grade.getStyleClass().add("grade");
        ColumnConstraints rotulos = new ColumnConstraints();
        rotulos.setMinWidth(135);
        ColumnConstraints campos = new ColumnConstraints();
        campos.setHgrow(Priority.ALWAYS);
        campos.setFillWidth(true);
        grade.getColumnConstraints().setAll(rotulos, campos);
        int linha = 0;
        linha = linha(grade, linha, "Data e hora", new HBox(8, data, hora), "");
        linha = linha(grade, linha, "Mecânico", mecanico, "");
        linha = linha(grade, linha, "Queixa do cliente", queixa, DICA_QUEIXA);
        linha = linha(grade, linha, "Medições", medicoes, "O valor e a referência como você escreve (ex.: \"12,1\" "
                + "V; referência \"12,4 a 12,7 V\"). O app não julga o valor; a referência ainda não conferida fica "
                + "marcada \"a verificar\".");
        linha = linha(grade, linha, "Códigos de falha", codigos, "");
        linha = linha(grade, linha, "Testes realizados", testes, "");
        linha = linha(grade, linha, "Causa encontrada", causa, "");
        linha = linha(grade, linha, "Solução", solucao, "");
        linha = linha(grade, linha, "Recomendações", recomendacoes, "Opcional.");
        linha(grade, linha, "Resultado", resultado, "Fica \"a definir\" enquanto o diagnóstico não termina.");
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

    private void somenteLeitura() {
        for (Node campo : List.of(data, hora, mecanico, resultado)) {
            campo.setDisable(true);
        }
        for (TextArea area : List.of(queixa, testes, causa, solucao, recomendacoes)) {
            area.setEditable(false);
        }
        medicoes.somenteLeitura(true);
        codigos.somenteLeitura(true);
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

    private static TextArea area(int linhas, String rotulo) {
        TextArea area = new TextArea();
        area.setPrefRowCount(linhas);
        area.setWrapText(true);
        area.setAccessibleText(rotulo);
        return area;
    }

    static String maiuscula(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
