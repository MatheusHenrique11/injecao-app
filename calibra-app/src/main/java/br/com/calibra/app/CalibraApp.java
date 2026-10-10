package br.com.calibra.app;

import java.io.IOException;
import java.util.Objects;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Ponto de entrada do app. Na fase 1 abre a calculadora. */
public class CalibraApp extends Application {

    public static final String VERSAO = "0.1.1";

    @Override
    public void start(Stage palco) throws IOException {
        PastasCalibra.prepararCacheDeFontes();
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("calculadora.fxml"));
        Parent raiz = carregador.load();
        CalculadoraController controlador = carregador.getController();
        controlador.usarServicos(getHostServices());
        Scene cena = new Scene(raiz);
        cena.getStylesheets().add(folhaDeEstilo());
        palco.setTitle("Calibra " + VERSAO + " — Calculadora");
        palco.setScene(cena);
        palco.show();
    }

    /** Endereço do calibra.css dentro do próprio app. */
    static String folhaDeEstilo() {
        return Objects.requireNonNull(CalibraApp.class.getResource("calibra.css"), "calibra.css").toExternalForm();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
