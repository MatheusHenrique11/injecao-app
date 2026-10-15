package br.com.calibra.app;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Region;

/**
 * Janelas de aviso (Alert) do app com o texto inteiro. Sem isto, o JavaFX corta a mensagem com "..." quando ela
 * passa de umas quatro linhas (o ensaio do bloco 2.6 achou isso na restauração do backup, que escondia o caminho
 * do backup de segurança).
 */
final class Alertas {

    /** Largura da janela: cabe numa linha um caminho de arquivo comprido, como o da pasta de backup. */
    static final double LARGURA = 600;

    private Alertas() {
    }

    /** Igual a {@code new Alert(tipo, mensagem, botoes)}; sem botões, usa os botões padrão do tipo. */
    static Alert criar(AlertType tipo, String mensagem, ButtonType... botoes) {
        Alert alerta = new Alert(tipo, mensagem, botoes);
        DialogPane painel = alerta.getDialogPane();
        painel.setPrefWidth(LARGURA);
        painel.setMinHeight(Region.USE_PREF_SIZE);
        return alerta;
    }
}
