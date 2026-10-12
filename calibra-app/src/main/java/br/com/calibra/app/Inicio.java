package br.com.calibra.app;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;

import br.com.calibra.dados.PastasDados;

/**
 * Partida do app (o "main"). Liga o log e o tratamento de erros e só depois abre o JavaFX. Fica fora do
 * {@link CalibraApp} para o app rodar pelo classpath, como no instalador.
 */
public final class Inicio {

    /** Hora em que o app começou a abrir, para medir o tempo de abertura (SPEC 10: menos de 5 s). */
    static final long INICIO_NANOS = System.nanoTime();

    private Inicio() {
    }

    public static void main(String[] args) {
        PastasDados pastas = Partida.pastas();
        try {
            pastas.criar();
            Partida.prepararCaches(pastas);
            Logs.configurar(pastas.logs());
        } catch (IOException erro) {
            // Sem a pasta de dados o app não grava nada; a tela abre e mostra o erro ao abrir o banco.
            System.err.println("Não foi possível preparar a pasta " + pastas.raiz() + ": " + erro.getMessage());
        }
        ErroInesperado.instalar(pastas.logs());
        Logger.getLogger(Inicio.class.getName()).log(Level.INFO, "Calibra {0} abrindo; Java {1}.",
                new Object[] {Versao.atual(), Runtime.version()});
        Application.launch(CalibraApp.class, args);
    }

    /** Milissegundos desde o começo da abertura. */
    static long msDesdeOInicio() {
        return (System.nanoTime() - INICIO_NANOS) / 1_000_000;
    }
}
