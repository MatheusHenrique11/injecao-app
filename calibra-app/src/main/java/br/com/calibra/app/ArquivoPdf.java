package br.com.calibra.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * Gerar um PDF a partir da tela: pergunta onde salvar, gera fora da thread da tela (SPEC 10) e oferece abrir o
 * arquivo no fim. Usado pela memória de cálculo e pelo orçamento.
 */
final class ArquivoPdf {

    private static final Logger LOG = Logger.getLogger(ArquivoPdf.class.getName());

    /** O que escreve o PDF no arquivo escolhido (roda fora da thread da tela). */
    @FunctionalInterface
    interface Escrita {
        void escrever(Path destino) throws IOException;
    }

    private ArquivoPdf() {
    }

    /**
     * @param titulo o que é o PDF ("Memória de cálculo", "Orçamento"), para a janela de salvar e o aviso do fim
     * @param estado recebe as mensagens para a barra de estado ("Gerando o PDF…", "PDF salvo em…")
     * @param aoTerminar chamado no fim, deu certo ou não (para liberar botões)
     * @return false se a pessoa desistiu na escolha do arquivo
     */
    static boolean gerar(Window janela, HostServices servicos, String titulo, String nomeSugerido, Escrita escrita,
            Consumer<String> estado, Runnable aoTerminar) {
        Optional<File> arquivo = escolherArquivo(janela, "Salvar " + titulo.toLowerCase(Locale.ROOT),
                nomeSugerido);
        if (arquivo.isEmpty()) {
            return false;
        }
        Path destino = arquivo.get().toPath();
        Task<Void> tarefa = new Task<>() {
            @Override
            protected Void call() throws Exception {
                escrita.escrever(destino);
                return null;
            }
        };
        tarefa.setOnSucceeded(evento -> {
            aoTerminar.run();
            estado.accept("PDF salvo em " + destino);
            oferecerAbrir(servicos, titulo, destino);
        });
        tarefa.setOnFailed(evento -> {
            aoTerminar.run();
            estado.accept("O PDF não foi gerado.");
            Throwable erro = tarefa.getException();
            LOG.log(Level.SEVERE, "Não foi possível gerar o PDF em " + destino, erro);
            String motivo = erro == null || erro.getMessage() == null
                    ? "Erro inesperado. O detalhe foi gravado no log." : erro.getMessage();
            Alert alerta = Alertas.criar(Alert.AlertType.ERROR, motivo
                    + "\nSe o arquivo estiver aberto em outro programa, feche-o e tente de novo.", ButtonType.OK);
            alerta.setTitle("Não foi possível gerar o PDF");
            alerta.setHeaderText("Não foi possível gerar o PDF");
            alerta.showAndWait();
        });
        estado.accept("Gerando o PDF…");
        Thread linha = new Thread(tarefa, "gerar-pdf");
        linha.setDaemon(true);
        linha.start();
        return true;
    }

    private static Optional<File> escolherArquivo(Window janela, String titulo, String nomeSugerido) {
        FileChooser escolha = new FileChooser();
        escolha.setTitle(titulo);
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        escolha.setInitialFileName(nomeSugerido);
        Path documentos = Path.of(System.getProperty("user.home"), "Documents");
        if (Files.isDirectory(documentos)) {
            escolha.setInitialDirectory(documentos.toFile());
        }
        return Optional.ofNullable(escolha.showSaveDialog(janela));
    }

    private static void oferecerAbrir(HostServices servicos, String titulo, Path arquivo) {
        ButtonType abrir = new ButtonType("Abrir", ButtonBar.ButtonData.OK_DONE);
        ButtonType fechar = new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = Alertas.criar(Alert.AlertType.INFORMATION, "PDF salvo em:\n" + arquivo, abrir, fechar);
        alerta.setTitle(titulo);
        alerta.setHeaderText("PDF gerado.");
        Optional<ButtonType> resposta = alerta.showAndWait();
        if (resposta.isPresent() && resposta.get() == abrir && servicos != null) {
            servicos.showDocument(arquivo.toUri().toString());
        }
    }
}
