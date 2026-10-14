package br.com.calibra.app;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import br.com.calibra.core.orcamento.MeioAprovacao;

/** Pergunta quando e como o cliente respondeu ao orçamento (aprovou ou recusou). */
final class JanelaResposta {

    /** O que a pessoa informou. */
    record Resposta(LocalDateTime quando, MeioAprovacao meio, String observacao) {
    }

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private JanelaResposta() {
    }

    /**
     * @param titulo ex.: "Aprovado pelo cliente"
     * @param agora a data e a hora que aparecem já preenchidas
     * @return vazio se a pessoa cancelou
     */
    static Optional<Resposta> mostrar(Window dono, String titulo, LocalDateTime agora) {
        Dialog<Resposta> dialogo = new Dialog<>();
        if (dono != null) {
            dialogo.initOwner(dono);
        }
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo + ": quando e como o cliente respondeu?");
        DatePicker data = new DatePicker(agora.toLocalDate());
        Datas.emPortugues(data);
        TextField hora = new TextField(agora.format(HORA));
        hora.setPrefColumnCount(6);
        ComboBox<MeioAprovacao> meio = new ComboBox<>();
        CamposTela.configurar(meio, Arrays.asList(MeioAprovacao.values()), MeioAprovacao::rotulo);
        TextField observacao = new TextField();
        observacao.setPromptText("opcional (ex.: pediu para começar na segunda)");
        Label erro = new Label();
        erro.getStyleClass().add("erro");
        erro.setWrapText(true);

        GridPane grade = new GridPane();
        grade.getStyleClass().add("grade");
        grade.addRow(0, new Label("Data"), data);
        grade.addRow(1, new Label("Hora (hh:mm)"), hora);
        grade.addRow(2, new Label("Como respondeu"), meio);
        grade.addRow(3, new Label("Observação"), observacao);
        grade.add(erro, 0, 4, 2, 1);
        dialogo.getDialogPane().setContent(grade);
        dialogo.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        ButtonType gravar = new ButtonType("Gravar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().setAll(gravar, new ButtonType("Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().lookupButton(gravar).addEventFilter(ActionEvent.ACTION, evento -> {
            if (data.getValue() == null || meio.getValue() == null || ler(hora.getText()) == null) {
                erro.setText("Preencha a data, a hora (ex.: 14:30) e como o cliente respondeu.");
                evento.consume();
            }
        });
        dialogo.setResultConverter(botao -> botao == gravar
                ? new Resposta(data.getValue().atTime(ler(hora.getText())), meio.getValue(), observacao.getText())
                : null);
        return dialogo.showAndWait();
    }

    private static LocalTime ler(String texto) {
        try {
            return LocalTime.parse(texto == null ? "" : texto.strip(), DateTimeFormatter.ofPattern("H:mm"));
        } catch (DateTimeParseException erro) {
            return null;
        }
    }
}
