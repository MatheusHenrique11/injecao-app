package br.com.calibra.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Ponto de entrada do app. Na fase 0 só abre uma janela vazia. */
public class CalibraApp extends Application {

    public static final String VERSAO = "0.0.1";

    @Override
    public void start(Stage palco) {
        palco.setTitle("Calibra " + VERSAO);
        palco.setScene(new Scene(new StackPane(new Label("Calibra " + VERSAO)), 640, 400));
        palco.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
