package br.com.calibra.app;

import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Janela de espera que bloqueia a tela enquanto um trabalho que não pode ser interrompido roda (backup ao
 * fechar, restauração). Não tem botão de fechar; quem abriu fecha quando o trabalho termina.
 */
final class JanelaProgresso {

    private JanelaProgresso() {
    }

    static Stage abrir(Window dono, String texto) {
        Stage janela = new Stage(StageStyle.UTILITY);
        if (dono != null) {
            janela.initOwner(dono);
            janela.initModality(Modality.WINDOW_MODAL);
        } else {
            janela.initModality(Modality.APPLICATION_MODAL);
        }
        janela.setTitle("Calibra");
        janela.setResizable(false);
        janela.setOnCloseRequest(Event::consume);
        ProgressIndicator indicador = new ProgressIndicator();
        indicador.setPrefSize(32, 32);
        Label rotulo = new Label(texto);
        HBox caixa = new HBox(14, indicador, rotulo);
        caixa.setAlignment(Pos.CENTER_LEFT);
        caixa.getStyleClass().add("pagina");
        caixa.setMinWidth(360);
        Scene cena = new Scene(caixa);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        janela.setScene(cena);
        janela.show();
        return janela;
    }
}
