package br.com.calibra.app;

import java.io.IOException;
import java.time.Clock;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import br.com.calibra.dados.ErroDados;

/** A aplicação JavaFX: abre o banco (com migração) e depois a tela. A partida fica em {@link Inicio}. */
public class CalibraApp extends Application {

    private static final Logger LOG = Logger.getLogger(CalibraApp.class.getName());

    private ContextoApp contexto;

    @Override
    public void start(Stage palco) throws IOException {
        try {
            contexto = Partida.abrir(Partida.pastas(), Versao.atual(), Clock.systemDefaultZone());
        } catch (IOException | ErroDados erro) {
            LOG.log(Level.SEVERE, "O app não conseguiu abrir os dados", erro);
            String mensagem = erro instanceof ErroDados ? erro.getMessage()
                    : "Não foi possível criar a pasta de dados do Calibra: " + erro.getMessage();
            ErroInesperado.mostrar("Não foi possível abrir", mensagem);
            Platform.exit();
            return;
        }
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("calculadora.fxml"));
        Parent raiz = carregador.load();
        CalculadoraController controlador = carregador.getController();
        controlador.usarServicos(getHostServices());
        Scene cena = new Scene(raiz);
        cena.getStylesheets().add(folhaDeEstilo());
        palco.setTitle("Calibra " + Versao.atual() + " — Calculadora");
        palco.setScene(cena);
        palco.show();
        LOG.info(() -> "Tela aberta em " + Inicio.msDesdeOInicio() + " ms.");
    }

    /** Endereço do calibra.css dentro do próprio app. */
    static String folhaDeEstilo() {
        return Objects.requireNonNull(CalibraApp.class.getResource("calibra.css"), "calibra.css").toExternalForm();
    }
}
