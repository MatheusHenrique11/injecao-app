package br.com.calibra.app;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

/**
 * Copiar texto para a área de transferência do Windows (os textos para o WhatsApp), avisando quando não deu: às
 * vezes outro programa (o histórico da área de transferência, por exemplo) a está usando naquele instante.
 */
final class AreaDeTransferencia {

    static final String COPIADO = "Texto copiado. Cole no WhatsApp do cliente (Ctrl+V) e envie você mesmo.";
    static final String NAO_COPIOU = "Não consegui copiar: outro programa estava usando a área de transferência. Tente "
            + "de novo.";

    private AreaDeTransferencia() {
    }

    /** @return false se o Windows não deixou copiar (a pessoa tenta de novo) */
    static boolean copiar(String texto) {
        ClipboardContent conteudo = new ClipboardContent();
        conteudo.putString(texto);
        return Clipboard.getSystemClipboard().setContent(conteudo);
    }
}
