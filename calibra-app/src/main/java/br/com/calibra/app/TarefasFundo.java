package br.com.calibra.app;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.dados.ErroDados;

/**
 * Trabalho demorado fora da thread da tela (SPEC 10: backup e restauração em {@link Task}, com indicador
 * de progresso; desde a fase 3B, também a cópia de fotos e documentos). Tudo roda numa única linha de execução, um
 * de cada vez: um backup automático nunca roda junto com uma restauração, e a cópia de uma foto nunca roda junto com
 * a restauração, que troca a pasta de anexos inteira.
 */
final class TarefasFundo {

    private static final Logger LOG = Logger.getLogger(TarefasFundo.class.getName());

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(trabalho -> {
        Thread linha = new Thread(trabalho, "calibra-fundo");
        linha.setDaemon(true);
        return linha;
    });
    private final ReadOnlyIntegerWrapper emAndamento = new ReadOnlyIntegerWrapper();
    private final ReadOnlyStringWrapper descricao = new ReadOnlyStringWrapper("");

    /**
     * Põe um trabalho na fila. Chamar na thread da tela; o resultado e o erro voltam nela.
     *
     * @param texto o que aparece na barra de status enquanto roda (ex.: "Fazendo backup")
     */
    <T> void executar(String texto, Callable<T> trabalho, Consumer<T> aoTerminar, Consumer<Throwable> aoFalhar) {
        Task<T> tarefa = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabalho.call();
            }
        };
        tarefa.setOnSucceeded(evento -> {
            terminou();
            aoTerminar.accept(tarefa.getValue());
        });
        tarefa.setOnFailed(evento -> {
            terminou();
            Throwable erro = tarefa.getException();
            LOG.log(Level.WARNING, texto + ": falhou", erro);
            aoFalhar.accept(erro);
        });
        descricao.set(texto + "…");
        emAndamento.set(emAndamento.get() + 1);
        executor.execute(tarefa);
    }

    /** Chama a verificação na thread da tela a cada hora, a partir de uma hora daqui. */
    void deHoraEmHora(Runnable verificacao) {
        executor.scheduleWithFixedDelay(() -> Platform.runLater(verificacao), 1, 1, TimeUnit.HOURS);
    }

    /** Quantos trabalhos estão na fila ou rodando. */
    ReadOnlyIntegerProperty emAndamento() {
        return emAndamento.getReadOnlyProperty();
    }

    /** O texto do último trabalho posto na fila. */
    ReadOnlyStringProperty descricao() {
        return descricao.getReadOnlyProperty();
    }

    /** Para a fila ao sair do app. */
    void encerrar() {
        executor.shutdownNow();
    }

    /**
     * O que mostrar na tela quando um trabalho falha: os erros de entrada ou a mensagem do erro de dados (as duas já
     * em português). Um erro inesperado vai para o tratamento de sempre (log e janela de erro), e a lista fica vazia.
     */
    static List<String> mensagens(Throwable erro) {
        if (erro instanceof EntradaInvalidaException invalida) {
            return invalida.erros();
        }
        if (erro instanceof ErroDados dados) {
            return List.of(dados.getMessage());
        }
        Thread.UncaughtExceptionHandler tratamento = Thread.getDefaultUncaughtExceptionHandler();
        if (tratamento != null) {
            tratamento.uncaughtException(Thread.currentThread(), erro);
            return List.of();
        }
        return List.of("Erro inesperado: " + erro);
    }

    private void terminou() {
        emAndamento.set(Math.max(0, emAndamento.get() - 1));
    }
}
