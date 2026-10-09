package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Teste de interface (opcional, fora do verify padrão): abre a calculadora sem mostrar janela,
 * preenche o caso golden B, calcula e salva imagens da própria cena em target/. Precisa de uma área de
 * trabalho do Windows. Rodar com: mvn -q -pl calibra-app verify -Dgroups=ui -Dtestes.excluidos=nenhum
 */
@Tag("ui")
class CalculadoraUiTest {

    @Test
    void preencheOCasoBCalculaEMostraOSemaforo() throws Throwable {
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
                executar();
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

    private static void executar() throws IOException {
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("calculadora.fxml"));
        Parent raiz = carregador.load();
        CalculadoraController controlador = carregador.getController();
        Scene cena = new Scene(raiz, 1280, 900);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());

        controlador.preencher(ValoresDeTeste.casoB());
        TabPane abas = (TabPane) cena.lookup("#abas");
        abas.getSelectionModel().select(0);
        salvar(cena, "captura-calculadora-motor.png");
        abas.getSelectionModel().select(1);
        salvar(cena, "captura-calculadora-meta.png");

        ((Button) cena.lookup("#botaoCalcular")).fire();
        raiz.applyCss();
        raiz.layout();

        Node painel = cena.lookup("#painelResultado");
        assertNotNull(painel);
        assertTrue(contemTexto(painel, "VERMELHO"), "o DC de 86% deveria aparecer vermelho");
        assertTrue(contemTexto(painel, "DC do bico na potência máxima"));
        assertNotNull(cena.lookup("#justificativa-DC_BICO"), "campo de justificativa do vermelho");
        assertFalse(((Button) cena.lookup("#botaoPdf")).isDisabled(), "PDF liberado depois de calcular");
        salvar(cena, "captura-calculadora-resultado.png");

        controlador.preencher(Map.of(Campo.VE, "0,90"));
        assertTrue(((Button) cena.lookup("#botaoPdf")).isDisabled(), "mudou um campo: PDF bloqueado");
        assertTrue(contemTexto(painel, "desatualizado"), "faixa de resultado desatualizado");
    }

    private static boolean contemTexto(Node no, String trecho) {
        if (no instanceof Label rotulo && rotulo.getText() != null && rotulo.getText().contains(trecho)) {
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
    private static void salvar(Scene cena, String nome) throws IOException {
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
