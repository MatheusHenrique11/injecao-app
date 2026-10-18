package br.com.calibra.relatorios;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * As páginas de um PDF desenhadas como imagens, para imprimir (bloco 3.3). Na primeira vez a biblioteca procura as
 * fontes do Windows, o que leva alguns segundos; depois elas ficam no cache dela (a pasta cache dos dados).
 */
public final class PaginasPdf {

    private PaginasPdf() {
    }

    /**
     * @param dpi pontos por polegada (200 dá uma página A4 com 1654 × 2339 pontos, boa para texto)
     */
    public static List<BufferedImage> desenhar(Path pdf, float dpi) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf.toFile())) {
            PDFRenderer desenhista = new PDFRenderer(documento);
            List<BufferedImage> paginas = new ArrayList<>();
            for (int i = 0; i < documento.getNumberOfPages(); i++) {
                paginas.add(desenhista.renderImageWithDPI(i, dpi, ImageType.RGB));
            }
            return paginas;
        }
    }
}
