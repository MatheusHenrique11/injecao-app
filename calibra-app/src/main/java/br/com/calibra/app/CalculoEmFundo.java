package br.com.calibra.app;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.concurrent.Task;

/**
 * Cálculo fora da thread da tela (SPEC 10; decisões 160 e 290). A tela segue respondendo enquanto a conta roda, e o
 * botão e o indicador de progresso acompanham {@link #calculando()}. A linha de execução é só dos cálculos, separada
 * da fila do backup e da cópia de fotos ({@link TarefasFundo}): um cálculo nunca espera um backup comprido.
 */
final class CalculoEmFundo {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(trabalho -> {
        Thread linha = new Thread(trabalho, "calibra-calculo");
        linha.setDaemon(true);
        return linha;
    });
    private final ReadOnlyBooleanWrapper calculando = new ReadOnlyBooleanWrapper();

    /**
     * Roda a conta. Chamar na thread da tela; o resultado e o erro voltam nela.
     *
     * @param conta só a conta: a leitura da tela fica antes, na thread da tela
     */
    <T> void executar(Callable<T> conta, Consumer<T> aoTerminar, Consumer<Throwable> aoFalhar) {
        Task<T> tarefa = new Task<>() {
            @Override
            protected T call() throws Exception {
                return conta.call();
            }
        };
        tarefa.setOnSucceeded(evento -> {
            calculando.set(false);
            aoTerminar.accept(tarefa.getValue());
        });
        tarefa.setOnFailed(evento -> {
            calculando.set(false);
            aoFalhar.accept(tarefa.getException());
        });
        calculando.set(true);
        executor.execute(tarefa);
    }

    /** Se há uma conta rodando (o botão espera, o indicador aparece). */
    ReadOnlyBooleanProperty calculando() {
        return calculando.getReadOnlyProperty();
    }
}
