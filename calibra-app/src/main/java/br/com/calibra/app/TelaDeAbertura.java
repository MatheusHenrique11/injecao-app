package br.com.calibra.app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.SplashScreen;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A tela de abertura do GarageTech (decisão 283): o logo grande que o lançador do Java mostra no instante do clique,
 * antes de o Java carregar (a opção {@code -splash}, posta pelo instalador e pelo {@code javafx:run}). Ela fecha
 * quando a janela principal aparece ou antes de qualquer janela de erro da abertura, para não esconder a mensagem.
 * Sem a opção (os testes), não há o que fechar nem onde escrever.
 */
final class TelaDeAbertura {

    private static final Logger LOG = Logger.getLogger(TelaDeAbertura.class.getName());
    /** Cinza-claro sobre o fundo escuro da imagem, na faixa livre embaixo do "Gestão e Tecnologia Automotiva". */
    private static final Color COR_DO_TEXTO = new Color(0xB8, 0xC4, 0xD6);

    private TelaDeAbertura() {
    }

    /**
     * Escreve um aviso embaixo do logo (decisão 291: "Atualizando os dados…" enquanto o backup e a migração rodam
     * numa atualização, que podem levar alguns segundos). Sem tela de abertura, não faz nada.
     */
    static void escrever(String texto) {
        try {
            SplashScreen abertura = SplashScreen.getSplashScreen();
            if (abertura == null || !abertura.isVisible()) {
                return;
            }
            Graphics2D desenho = abertura.createGraphics();
            try {
                desenhar(desenho, abertura.getSize(), texto);
            } finally {
                desenho.dispose();
            }
            abertura.update();
        } catch (RuntimeException | LinkageError erro) {
            // Sem tela gráfica ou já fechada por outra thread: o aviso é só um conforto, a abertura segue.
            LOG.log(Level.FINE, "Tela de abertura: não deu para escrever o aviso", erro);
        }
    }

    /** O texto centralizado na faixa de baixo da imagem (separado para o teste desenhar numa cópia dela). */
    static void desenhar(Graphics2D desenho, Dimension tamanho, String texto) {
        desenho.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        desenho.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(11, tamanho.height / 24)));
        desenho.setColor(COR_DO_TEXTO);
        FontMetrics medidas = desenho.getFontMetrics();
        int x = (tamanho.width - medidas.stringWidth(texto)) / 2;
        int y = tamanho.height - tamanho.height / 32 - medidas.getDescent();
        desenho.drawString(texto, x, y);
    }

    /** Fecha a tela de abertura, se ela estiver aberta. Pode ser chamado mais de uma vez, de qualquer thread. */
    static void fechar() {
        try {
            SplashScreen abertura = SplashScreen.getSplashScreen();
            if (abertura != null && abertura.isVisible()) {
                abertura.close();
            }
        } catch (RuntimeException | LinkageError erro) {
            // Sem tela gráfica, já fechada por outra thread ou sem a biblioteca da abertura: não há o que fechar.
            LOG.log(Level.FINE, "Tela de abertura: nada a fechar", erro);
        }
    }
}
