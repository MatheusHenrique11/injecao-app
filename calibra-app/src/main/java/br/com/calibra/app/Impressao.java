package br.com.calibra.app;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.concurrent.Task;
import javafx.print.PageLayout;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.stage.Window;

import br.com.calibra.relatorios.PaginasPdf;

/**
 * Imprimir um documento (bloco 3.3): a pessoa escolhe a impressora na janela de impressão do Windows; o PDF é gerado
 * e as páginas são desenhadas em segundo plano, a 200 DPI; e cada página vai para a impressora do tamanho da folha.
 * Sem impressora instalada, avisa.
 */
final class Impressao {

    private static final Logger LOG = Logger.getLogger(Impressao.class.getName());
    /** Boa para texto sem pesar demais: uma página A4 vira uma imagem de cerca de 1654 × 2339 pontos. */
    static final float DPI = 200f;

    private Impressao() {
    }

    /**
     * @param titulo o que é o documento (ex.: "Via do cliente OS-2026-0001"), para a fila da impressora
     * @param pastaTemporaria onde o PDF fica enquanto as páginas são desenhadas (é apagado no fim)
     * @param estado recebe as mensagens para a tela ("Preparando a impressão…", "Enviado para a impressora…")
     */
    static void imprimir(Window janela, String titulo, Path pastaTemporaria, ArquivoPdf.Escrita escrita,
            Consumer<String> estado) {
        PrinterJob trabalho = PrinterJob.createPrinterJob();
        if (trabalho == null) {
            avisar(janela, "Nenhuma impressora instalada", "Não achei impressora instalada no Windows. Instale a "
                    + "impressora (ou use a \"Microsoft Print to PDF\") ou gere o PDF e imprima por ele.");
            return;
        }
        trabalho.getJobSettings().setJobName("GarageTech · " + titulo);
        if (!trabalho.showPrintDialog(janela)) {
            trabalho.cancelJob();
            return;
        }
        Task<List<Image>> tarefa = new Task<>() {
            @Override
            protected List<Image> call() throws Exception {
                Files.createDirectories(pastaTemporaria);
                Path pdf = Files.createTempFile(pastaTemporaria, "impressao-", ".pdf");
                try {
                    escrita.escrever(pdf);
                    return PaginasPdf.desenhar(pdf, DPI).stream().map(Impressao::imagem).toList();
                } finally {
                    Files.deleteIfExists(pdf);
                }
            }
        };
        tarefa.setOnSucceeded(evento -> {
            List<Image> paginas = tarefa.getValue();
            if (imprimirPaginas(trabalho, paginas)) {
                estado.accept("Enviado para a impressora (" + paginas.size()
                        + (paginas.size() == 1 ? " página)." : " páginas)."));
            } else {
                estado.accept("A impressão foi cancelada.");
            }
        });
        tarefa.setOnFailed(evento -> {
            trabalho.cancelJob();
            LOG.log(Level.SEVERE, "Não foi possível imprimir " + titulo, tarefa.getException());
            estado.accept("A impressão não foi feita.");
            avisar(janela, "Não foi possível imprimir", "Não consegui preparar as páginas. O detalhe foi gravado no "
                    + "log. Gere o PDF e imprima por ele.");
        });
        estado.accept("Preparando a impressão… (na primeira vez demora alguns segundos)");
        Thread linha = new Thread(tarefa, "imprimir");
        linha.setDaemon(true);
        linha.start();
    }

    /** Cada página do tamanho da área de impressão da folha escolhida, sem distorcer. */
    private static boolean imprimirPaginas(PrinterJob trabalho, List<Image> paginas) {
        PageLayout layout = trabalho.getJobSettings().getPageLayout();
        for (Image pagina : paginas) {
            ImageView vista = new ImageView(pagina);
            vista.setPreserveRatio(true);
            vista.setSmooth(true);
            vista.setFitWidth(layout.getPrintableWidth());
            vista.setFitHeight(layout.getPrintableHeight());
            if (!trabalho.printPage(layout, vista)) {
                trabalho.cancelJob();
                return false;
            }
        }
        return trabalho.endJob();
    }

    /** A página desenhada como imagem do JavaFX: os pontos são copiados direto, sem módulo a mais. */
    static Image imagem(BufferedImage pagina) {
        int largura = pagina.getWidth();
        int altura = pagina.getHeight();
        int[] pontos = pagina.getRGB(0, 0, largura, altura, null, 0, largura);
        WritableImage imagem = new WritableImage(largura, altura);
        imagem.getPixelWriter().setPixels(0, 0, largura, altura, PixelFormat.getIntArgbInstance(), pontos, 0, largura);
        return imagem;
    }

    private static void avisar(Window janela, String titulo, String texto) {
        Alert alerta = Alertas.criar(Alert.AlertType.WARNING, texto, ButtonType.OK);
        if (janela != null) {
            alerta.initOwner(janela);
        }
        alerta.setTitle(titulo);
        alerta.setHeaderText(titulo);
        alerta.showAndWait();
    }
}
