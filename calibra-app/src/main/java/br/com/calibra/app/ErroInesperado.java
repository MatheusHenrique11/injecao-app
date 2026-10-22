package br.com.calibra.app;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import br.com.calibra.dados.ErroDados;

/**
 * Erro que ninguém tratou (SPEC 10): grava o detalhe no log e mostra uma mensagem em português, sem a
 * pilha de chamadas do Java.
 */
final class ErroInesperado {

    private static final Logger LOG = Logger.getLogger(ErroInesperado.class.getName());

    /** Evita uma janela de erro em cima da outra se a própria janela falhar. */
    private static final AtomicBoolean MOSTRANDO = new AtomicBoolean();

    private ErroInesperado() {
    }

    /** Passa a tratar todo erro não tratado, em qualquer thread. */
    static void instalar(Path pastaLogs) {
        Thread.setDefaultUncaughtExceptionHandler((thread, erro) -> tratar(erro, pastaLogs));
    }

    static void tratar(Throwable erro, Path pastaLogs) {
        LOG.log(Level.SEVERE, "Erro inesperado", erro);
        mostrar("Erro", mensagem(erro, pastaLogs));
    }

    /**
     * O texto para a pessoa. Erro do banco ou do backup já vem com mensagem em português; o resto ganha uma
     * mensagem geral que diz onde está o detalhe.
     */
    static String mensagem(Throwable erro, Path pastaLogs) {
        for (Throwable causa = erro; causa != null; causa = causa.getCause()) {
            if (causa instanceof ErroDados && causa.getMessage() != null) {
                return causa.getMessage();
            }
        }
        return "Aconteceu um erro inesperado. O detalhe foi gravado no log, na pasta " + pastaLogs
                + ". Se o problema continuar, guarde esse arquivo para mostrar a quem dá suporte ao app.";
    }

    /**
     * Janela de erro; antes de a tela abrir, só fica o log e a saída de erro. A tela de abertura (o logo grande) fecha
     * antes, para não ficar na frente da mensagem.
     */
    static void mostrar(String titulo, String mensagem) {
        TelaDeAbertura.fechar();
        Runnable janela = () -> {
            if (!MOSTRANDO.compareAndSet(false, true)) {
                return;
            }
            try {
                Alert alerta = Alertas.criar(AlertType.ERROR, mensagem);
                alerta.setTitle("Calibra — " + titulo);
                alerta.setHeaderText(null);
                alerta.showAndWait();
            } finally {
                MOSTRANDO.set(false);
            }
        };
        try {
            if (Platform.isFxApplicationThread()) {
                janela.run();
            } else {
                Platform.runLater(janela);
            }
        } catch (IllegalStateException telaNaoAberta) {
            System.err.println(mensagem);
        }
    }
}
