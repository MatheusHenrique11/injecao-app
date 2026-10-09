package br.com.calibra.relatorios;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.semaforo.Cor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Escreve texto corrido num PDF A4: quebra linhas, abre página nova quando acaba o espaço e põe o
 * rodapé com "página X de Y" no fim.
 */
final class EscritorPdf {

    private static final PDRectangle PAGINA = PDRectangle.A4;
    private static final float MARGEM = 50f;
    private static final float LARGURA = PAGINA.getWidth() - 2 * MARGEM;
    private static final float TOPO = PAGINA.getHeight() - MARGEM;
    /** Abaixo disto fica o rodapé. */
    private static final float BASE = MARGEM + 18f;
    private static final float ENTRELINHA = 1.35f;
    static final float CORPO = 9.5f;

    private final PDDocument documento;
    private final PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont negrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDFont italico = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
    private final TextoPdf textoNormal = new TextoPdf(normal);
    private final TextoPdf textoNegrito = new TextoPdf(negrito);
    private final TextoPdf textoItalico = new TextoPdf(italico);
    private PDPageContentStream conteudo;
    private float y;

    EscritorPdf(PDDocument documento) throws IOException {
        this.documento = documento;
        novaPagina();
    }

    // ---------- Blocos de texto ----------

    void titulo(String texto) throws IOException {
        escrever(texto, negrito, 16f, 0f);
        espaco(2f);
    }

    void subtitulo(String texto) throws IOException {
        escrever(texto, italico, 9f, 0f);
    }

    void secao(String texto) throws IOException {
        espaco(10f);
        garantir(40f);
        escrever(texto, negrito, 12f, 0f);
        espaco(3f);
    }

    void paragrafo(String texto) throws IOException {
        escrever(texto, normal, CORPO, 0f);
    }

    void paragrafo(String texto, float recuo) throws IOException {
        escrever(texto, normal, CORPO, recuo);
    }

    void negrito(String texto, float recuo) throws IOException {
        escrever(texto, negrito, CORPO, recuo);
    }

    void italico(String texto, float recuo) throws IOException {
        escrever(texto, italico, CORPO, recuo);
    }

    /** Linha "Rótulo: texto", com o rótulo em negrito. */
    void item(String rotulo, String texto) throws IOException {
        String inicio = limpar(rotulo + ": ", negrito);
        float larguraRotulo = largura(inicio, negrito, CORPO);
        List<String> linhas = quebrar(limpar(texto, normal), normal, CORPO, LARGURA - larguraRotulo);
        float altura = CORPO * ENTRELINHA;
        garantir(altura);
        mostrar(inicio, negrito, CORPO, MARGEM, y - CORPO);
        for (int i = 0; i < linhas.size(); i++) {
            if (i > 0) {
                garantir(altura);
            }
            mostrar(linhas.get(i), normal, CORPO, MARGEM + larguraRotulo, y - CORPO);
            y -= altura;
        }
    }

    /** Texto dentro de uma caixa com borda, para avisos importantes. */
    void caixa(String texto) throws IOException {
        float recuo = 6f;
        List<String> linhas = quebrar(limpar(texto, italico), italico, CORPO, LARGURA - 2 * recuo);
        float altura = linhas.size() * CORPO * ENTRELINHA + 2 * recuo;
        garantir(altura);
        conteudo.setStrokingColor(0.55f, 0.55f, 0.55f);
        conteudo.addRect(MARGEM, y - altura, LARGURA, altura);
        conteudo.stroke();
        float linhaY = y - recuo;
        for (String linha : linhas) {
            mostrar(linha, italico, CORPO, MARGEM + recuo, linhaY - CORPO);
            linhaY -= CORPO * ENTRELINHA;
        }
        y -= altura + 4f;
    }

    /** Um item do semáforo: etiqueta colorida com a cor escrita, o indicador e a mensagem. */
    void semaforo(Cor cor, String indicador, String mensagem) throws IOException {
        float larguraEtiqueta = 58f;
        float recuo = larguraEtiqueta + 8f;
        float alturaLinha = CORPO * ENTRELINHA;
        List<String> linhas = quebrar(limpar(mensagem, normal), normal, CORPO, LARGURA - recuo);
        garantir(alturaLinha * (1 + Math.min(linhas.size(), 2)));
        float[] rgb = switch (cor) {
            case VERDE -> new float[] {0.18f, 0.49f, 0.20f};
            case AMARELO -> new float[] {0.98f, 0.75f, 0.18f};
            case VERMELHO -> new float[] {0.78f, 0.16f, 0.16f};
        };
        conteudo.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        conteudo.addRect(MARGEM, y - CORPO - 3f, larguraEtiqueta, CORPO + 4f);
        conteudo.fill();
        if (cor == Cor.AMARELO) {
            conteudo.setNonStrokingColor(0f, 0f, 0f);
        } else {
            conteudo.setNonStrokingColor(1f, 1f, 1f);
        }
        mostrar(limpar(cor.name(), negrito), negrito, 8f, MARGEM + 4f, y - CORPO);
        conteudo.setNonStrokingColor(0f, 0f, 0f);
        mostrar(limpar(indicador, negrito), negrito, CORPO, MARGEM + recuo, y - CORPO);
        y -= alturaLinha;
        for (String linha : linhas) {
            garantir(alturaLinha);
            mostrar(linha, normal, CORPO, MARGEM + recuo, y - CORPO);
            y -= alturaLinha;
        }
        espaco(2f);
    }

    void espaco(float pontos) {
        y -= pontos;
    }

    /** Fecha a última página e escreve o rodapé com "página X de Y" em todas. */
    void terminar(String rodape) throws IOException {
        conteudo.close();
        int total = documento.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage pagina = documento.getPage(i);
            try (PDPageContentStream rodapePagina = new PDPageContentStream(documento, pagina,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                String texto = limpar(rodape + " · página " + (i + 1) + " de " + total, italico);
                rodapePagina.beginText();
                rodapePagina.setFont(italico, 8f);
                rodapePagina.newLineAtOffset(MARGEM, MARGEM - 10f);
                rodapePagina.showText(texto);
                rodapePagina.endText();
            }
        }
    }

    // ---------- Mecânica ----------

    private void novaPagina() throws IOException {
        if (conteudo != null) {
            conteudo.close();
        }
        PDPage pagina = new PDPage(PAGINA);
        documento.addPage(pagina);
        conteudo = new PDPageContentStream(documento, pagina);
        y = TOPO;
    }

    private void garantir(float altura) throws IOException {
        if (y - altura < BASE) {
            novaPagina();
        }
    }

    private void escrever(String texto, PDFont fonte, float tamanho, float recuo) throws IOException {
        float altura = tamanho * ENTRELINHA;
        for (String linha : quebrar(limpar(texto, fonte), fonte, tamanho, LARGURA - recuo)) {
            garantir(altura);
            mostrar(linha, fonte, tamanho, MARGEM + recuo, y - tamanho);
            y -= altura;
        }
    }

    private void mostrar(String texto, PDFont fonte, float tamanho, float x, float linhaDeBase) throws IOException {
        conteudo.beginText();
        conteudo.setFont(fonte, tamanho);
        conteudo.newLineAtOffset(x, linhaDeBase);
        conteudo.showText(texto);
        conteudo.endText();
    }

    private String limpar(String texto, PDFont fonte) {
        TextoPdf limpador = fonte == negrito ? textoNegrito : fonte == italico ? textoItalico : textoNormal;
        return limpador.limpar(texto);
    }

    private static float largura(String texto, PDFont fonte, float tamanho) throws IOException {
        return fonte.getStringWidth(texto) / 1000f * tamanho;
    }

    /** Quebra o texto em linhas que cabem na largura. Quebras de linha do texto são respeitadas. */
    private static List<String> quebrar(String texto, PDFont fonte, float tamanho, float larguraMaxima)
            throws IOException {
        List<String> linhas = new ArrayList<>();
        for (String paragrafo : texto.split("\n", -1)) {
            StringBuilder linha = new StringBuilder();
            for (String palavra : paragrafo.split(" ")) {
                String tentativa = linha.isEmpty() ? palavra : linha + " " + palavra;
                if (linha.isEmpty() || largura(tentativa, fonte, tamanho) <= larguraMaxima) {
                    linha = new StringBuilder(tentativa);
                } else {
                    linhas.add(linha.toString());
                    linha = new StringBuilder(palavra);
                }
            }
            linhas.add(linha.toString());
        }
        return linhas;
    }
}
