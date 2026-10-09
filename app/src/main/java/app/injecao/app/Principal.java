package app.injecao.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Principal extends Application {

    @Override
    public void start(Stage palco) {
        palco.setTitle("Injeção - fase 0");
        palco.setScene(new Scene(new StackPane(new Label("Projeto vazio - fase 0")), 480, 240));
        palco.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
