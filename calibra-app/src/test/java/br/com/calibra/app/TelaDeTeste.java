package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Labeled;
import javafx.scene.input.Clipboard;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;

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
        // Fechar a última janela aberta (ex.: um diálogo mostrado para a foto) não pode desligar o JavaFX no meio
        // do teste: os passos seguintes nunca rodariam.
        Platform.setImplicitExit(false);

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

    /**
     * Roda na thread do JavaFX a ação que copia um texto e devolve o que foi para a área de transferência. Chame da
     * thread do teste, nunca de dentro de um passo na tela: depois de cada cópia, o Windows pede o texto ao app, e só
     * a thread da tela livre responde; presa num passo comprido, a área de transferência fica travada até ele acabar.
     * A área é limpa antes, para não valer o que já estava nela.
     */
    static String copiadoPor(Runnable acao) throws Throwable {
        naTelaDoJavaFx(() -> Clipboard.getSystemClipboard().clear());
        naTelaDoJavaFx(acao::run);
        AtomicReference<String> texto = new AtomicReference<>();
        esperar(() -> {
            texto.set(Clipboard.getSystemClipboard().getString());
            return texto.get() != null;
        }, "o texto ir para a área de transferência");
        return texto.get();
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

    /**
     * Roda na thread do JavaFX uma ação que abre janela e espera a resposta (showAndWait), sem esperar por ela: o
     * teste segue, acha a janela e aperta o botão, como a pessoa faria. Um erro da ação vai para {@code falha}.
     */
    static void semEsperar(Runnable acao, AtomicReference<Throwable> falha) {
        Platform.runLater(() -> {
            try {
                acao.run();
            } catch (Throwable erro) {
                falha.compareAndSet(null, erro);
            }
        });
    }

    /** Espera (até 30 s) a janela com o título abrir; se não abrir, o erro da ação aberta sem esperar vai junto. */
    static Stage esperarJanela(String titulo, AtomicReference<Throwable> falha) throws Throwable {
        try {
            esperar(() -> janelaAberta(titulo) != null, "a janela \"" + titulo + "\" abrir");
        } catch (AssertionError erro) {
            if (falha.get() != null) {
                erro.addSuppressed(falha.get());
            }
            throw erro;
        }
        AtomicReference<Stage> janela = new AtomicReference<>();
        naTelaDoJavaFx(() -> janela.set(janelaAberta(titulo)));
        return janela.get();
    }

    static Stage janelaAberta(String titulo) {
        return Window.getWindows().stream().filter(Stage.class::isInstance).map(Stage.class::cast)
                .filter(janela -> janela.isShowing() && titulo.equals(janela.getTitle())).findFirst().orElse(null);
    }

    static DialogPane painel(Stage janela) {
        return (DialogPane) janela.getScene().getRoot().lookup(".dialog-pane");
    }

    /** Nenhum texto da janela cortado com "..." (o JavaFX cortava as mensagens longas; ver Alertas). */
    static void textoInteiro(DialogPane painel) {
        painel.lookupAll(".label").stream().filter(Labeled.class::isInstance).map(Labeled.class::cast)
                .forEach(texto -> assertFalse(texto.isTextTruncated(), () -> "texto cortado: " + texto.getText()));
    }

    static void apertar(DialogPane painel, String texto) {
        apertar(painel, painel.getButtonTypes().stream().filter(tipo -> tipo.getText().equals(texto)).findFirst()
                .orElseThrow(() -> new AssertionError("a janela não tem o botão " + texto)));
    }

    static void apertarOk(DialogPane painel) {
        apertar(painel, painel.getButtonTypes().stream().filter(tipo -> tipo.getButtonData() == ButtonData.OK_DONE)
                .findFirst().orElseThrow());
    }

    private static void apertar(DialogPane painel, ButtonType tipo) {
        ((Button) painel.lookupButton(tipo)).fire();
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

    /**
     * A área com rolagem guarda uma imagem do conteúdo (cache) que a foto fora de janela não renova: trocar o painel
     * visível (ex.: etapa do assistente) saía na foto com o painel antigo. Sem o cache, a foto mostra o que está lá.
     */
    private static void desligarCache(Node no) {
        no.setCache(false);
        if (no instanceof Parent pai) {
            pai.getChildrenUnmodifiable().forEach(TelaDeTeste::desligarCache);
        }
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
        desligarCache(cena.getRoot());
        gravar(cena.snapshot(null), nome);
    }

    /** Salva a imagem de uma parte da tela (ex.: o desenho do mapa, que fica abaixo da área visível) em target/. */
    static void salvar(Node no, String nome) throws IOException {
        gravar(no.snapshot(null, null), nome);
    }

    private static void gravar(WritableImage imagem, String nome) throws IOException {
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
