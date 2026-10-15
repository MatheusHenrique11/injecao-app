package br.com.calibra.app;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import br.com.calibra.dados.ErroDados;

/** A aplicação JavaFX: abre o banco (com migração) e depois a tela. A partida fica em {@link Inicio}. */
public class CalibraApp extends Application {

    private static final Logger LOG = Logger.getLogger(CalibraApp.class.getName());

    /** Altura da barra de título do Windows, que fica fora da cena. */
    private static final double BORDA_DA_JANELA = 40;

    private PrincipalController principal;

    @Override
    public void start(Stage palco) throws IOException {
        ContextoApp contexto;
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
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("principal.fxml"));
        Parent raiz = carregador.load();
        principal = carregador.getController();
        principal.iniciar(contexto, getHostServices(), palco);
        palco.setOnCloseRequest(evento -> {
            evento.consume();
            principal.fechar();
        });
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        double largura = raiz.prefWidth(-1);
        double altura = raiz.prefHeight(-1);
        boolean cabe = largura <= area.getWidth() && altura + BORDA_DA_JANELA <= area.getHeight();
        Scene cena = new Scene(raiz, Math.min(largura, area.getWidth()),
                Math.min(altura, area.getHeight() - BORDA_DA_JANELA));
        cena.getStylesheets().add(folhaDeEstilo());
        palco.setScene(cena);
        palco.getIcons().setAll(icones());
        palco.setMinWidth(900);
        palco.setMinHeight(600);
        // Notebook com tela pequena (1366 × 768): a janela abre maximizada e as telas rolam.
        palco.setMaximized(!cabe);
        palco.show();
        LOG.info(() -> "Tela aberta em " + Inicio.msDesdeOInicio() + " ms.");
        principal.ligarBackupAutomatico();
    }

    @Override
    public void stop() {
        if (principal != null) {
            principal.encerrar();
        }
    }

    /** Tamanhos do ícone gerados por scripts\gerar-icone.ps1. */
    static final List<Integer> TAMANHOS_DO_ICONE = List.of(16, 32, 48, 64, 128, 256);

    /** O ícone da janela em vários tamanhos: o Windows escolhe o melhor para o título e a barra de tarefas. */
    static List<Image> icones() {
        return TAMANHOS_DO_ICONE.stream()
                .map(lado -> new Image(Objects.requireNonNull(CalibraApp.class.getResource(
                        "icones/calibra-" + lado + ".png"), "ícone de " + lado + " px").toExternalForm()))
                .toList();
    }

    /** Endereço do calibra.css dentro do próprio app. */
    static String folhaDeEstilo() {
        return Objects.requireNonNull(CalibraApp.class.getResource("calibra.css"), "calibra.css").toExternalForm();
    }
}
