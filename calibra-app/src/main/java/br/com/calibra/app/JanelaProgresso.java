package br.com.calibra.app;

import java.util.concurrent.atomic.AtomicReference;

import javafx.animation.PauseTransition;
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
import javafx.util.Duration;

/**
 * Janela de espera que bloqueia a tela enquanto um trabalho que não pode ser interrompido roda (backup ao
 * fechar, restauração, cópia de fotos). Não tem botão de fechar; quem abriu fecha quando o trabalho termina.
 */
final class JanelaProgresso {

    /** Quanto um trabalho rápido pode levar sem abrir a janela (ela piscaria na tela). */
    static final Duration ATRASO = Duration.millis(500);

    private JanelaProgresso() {
    }

    /**
     * Abre a janela só se o trabalho passar de {@link #ATRASO}: um trabalho rápido não pisca a tela. Chamar na thread
     * da tela.
     *
     * @return o que fecha a janela (ou desiste de abrir, se ela ainda não abriu); chamar na thread da tela
     */
    static Runnable abrirSeDemorar(Window dono, String texto) {
        AtomicReference<Stage> aberta = new AtomicReference<>();
        PauseTransition espera = new PauseTransition(ATRASO);
        espera.setOnFinished(evento -> aberta.set(abrir(dono, texto)));
        espera.play();
        return () -> {
            espera.stop();
            Stage janela = aberta.getAndSet(null);
            if (janela != null) {
                janela.close();
            }
        };
    }

    static Stage abrir(Window dono, String texto) {
        Stage janela = new Stage(StageStyle.UTILITY);
        if (dono != null) {
            janela.initOwner(dono);
            janela.initModality(Modality.WINDOW_MODAL);
        } else {
            janela.initModality(Modality.APPLICATION_MODAL);
        }
        janela.setTitle("GarageTech");
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
