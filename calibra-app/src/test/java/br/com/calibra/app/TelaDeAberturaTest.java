package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

/**
 * A tela de abertura (decisão 283): sem a opção -splash (como nos testes e no app aberto pelo Maven sem ela), não há o
 * que fechar nem onde escrever, e nada disso dá erro. A abertura de verdade é conferida no app instalado.
 */
class TelaDeAberturaTest {

    /** A faixa de baixo da imagem, livre: o "Gestão e Tecnologia Automotiva" termina antes desta linha (de 320). */
    private static final int FIM_DO_LOGO = 288;

    @Test
    void fecharSemTelaDeAberturaNaoFazNada() {
        assertDoesNotThrow(TelaDeAbertura::fechar);
        assertDoesNotThrow(TelaDeAbertura::fechar, "pode ser chamado de novo");
    }

    @Test
    void escreverSemTelaDeAberturaNaoFazNada() {
        assertDoesNotThrow(() -> TelaDeAbertura.escrever("Atualizando os dados…"));
    }

    /**
     * Decisão 291: o aviso sai centralizado na faixa livre embaixo do logo, sem encostar nele. O teste desenha numa
     * cópia da imagem de verdade e salva o resultado em target/ para conferir de olho.
     */
    @Test
    void oAvisoFicaNaFaixaDeBaixoSemEncostarNoLogo() throws IOException {
        BufferedImage original = ImageIO.read(Path.of("src/main/instalador/abertura.jpg").toFile());
        BufferedImage copia = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D desenho = copia.createGraphics();
        try {
            desenho.drawImage(original, 0, 0, null);
            TelaDeAbertura.desenhar(desenho, new Dimension(original.getWidth(), original.getHeight()),
                    "Atualizando os dados…");
        } finally {
            desenho.dispose();
        }
        Files.createDirectories(Path.of("target"));
        ImageIO.write(copia, "png", Path.of("target/captura-abertura-atualizando.png").toFile());

        int cima = Integer.MAX_VALUE;
        int esquerda = Integer.MAX_VALUE;
        int direita = -1;
        for (int y = 0; y < copia.getHeight(); y++) {
            for (int x = 0; x < copia.getWidth(); x++) {
                if (copia.getRGB(x, y) != original.getRGB(x, y)) {
                    cima = Math.min(cima, y);
                    esquerda = Math.min(esquerda, x);
                    direita = Math.max(direita, x);
                }
            }
        }
        assertTrue(direita >= 0, "o aviso foi desenhado");
        assertTrue(cima > FIM_DO_LOGO, "o aviso começa na linha " + cima + ", embaixo do logo");
        int sobraEsquerda = esquerda;
        int sobraDireita = copia.getWidth() - 1 - direita;
        assertTrue(Math.abs(sobraEsquerda - sobraDireita) <= 2,
                "centralizado: sobra " + sobraEsquerda + " px à esquerda e " + sobraDireita + " px à direita");
    }
}
