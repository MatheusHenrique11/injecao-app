package br.com.calibra.app;

import java.awt.SplashScreen;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A tela de abertura do GarageTech (decisão 283): o logo grande que o lançador do Java mostra no instante do clique,
 * antes de o Java carregar (a opção {@code -splash}, posta pelo instalador e pelo {@code javafx:run}). Ela fecha
 * quando a janela principal aparece ou antes de qualquer janela de erro da abertura, para não esconder a mensagem.
 * Sem a opção (os testes), não há o que fechar.
 */
final class TelaDeAbertura {

    private static final Logger LOG = Logger.getLogger(TelaDeAbertura.class.getName());

    private TelaDeAbertura() {
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
