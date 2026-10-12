package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Labeled;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

/** Ajuda dos testes de interface (tag "ui"): roda na thread do JavaFX e salva imagens da cena em target/. */
final class TelaDeTeste {

    /** Um passo do teste que roda na thread do JavaFX. */
    @FunctionalInterface
    interface Passo {
        void executar() throws Exception;
    }

    private TelaDeTeste() {
    }

    /** Liga o JavaFX (uma vez por execução) e roda o passo na thread dele, esperando terminar. */
    static void naTelaDoJavaFx(Passo passo) throws Throwable {
        CountDownLatch iniciado = new CountDownLatch(1);
        try {
            Platform.startup(iniciado::countDown);
        } catch (IllegalStateException jaIniciado) {
            iniciado.countDown();
        }
        assertTrue(iniciado.await(20, TimeUnit.SECONDS), "JavaFX não iniciou");

        AtomicReference<Throwable> falha = new AtomicReference<>();
        CountDownLatch terminou = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                passo.executar();
            } catch (Throwable erro) {
                falha.set(erro);
            } finally {
                terminou.countDown();
            }
        });
        assertTrue(terminou.await(60, TimeUnit.SECONDS), "a tela não terminou a tempo");
        if (falha.get() != null) {
            throw falha.get();
        }
    }

    /** Espera (até 30 s) uma condição conferida na thread do JavaFX, para trabalhos em segundo plano. */
    static void esperar(BooleanSupplier condicao, String oQue) throws Throwable {
        AtomicBoolean pronto = new AtomicBoolean();
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < limite) {
            naTelaDoJavaFx(() -> pronto.set(condicao.getAsBoolean()));
            if (pronto.get()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("esperei 30 s e não aconteceu: " + oQue);
    }

    /** Procura um nó pelo fx:id depois de aplicar o CSS (que cria o conteúdo das abas). */
    @SuppressWarnings("unchecked")
    static <T extends Node> T achar(Scene cena, String id) {
        cena.getRoot().applyCss();
        cena.getRoot().layout();
        Node no = cena.lookup("#" + id);
        assertNotNull(no, "não achei #" + id);
        return (T) no;
    }

    static boolean contemTexto(Node no, String trecho) {
        if (no instanceof Labeled rotulo && rotulo.getText() != null && rotulo.getText().contains(trecho)) {
            return true;
        }
        if (no instanceof Parent pai) {
            for (Node filho : pai.getChildrenUnmodifiable()) {
                if (contemTexto(filho, trecho)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Salva a imagem da cena (só o app, não a tela do Windows) em target/. */
    static void salvar(Scene cena, String nome) throws IOException {
        cena.getRoot().applyCss();
        cena.getRoot().layout();
        WritableImage imagem = cena.snapshot(null);
        int largura = (int) imagem.getWidth();
        int altura = (int) imagem.getHeight();
        BufferedImage saida = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixels = imagem.getPixelReader();
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                saida.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        Path pasta = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target");
        Files.createDirectories(pasta);
        ImageIO.write(saida, "png", pasta.resolve(nome).toFile());
    }
}
