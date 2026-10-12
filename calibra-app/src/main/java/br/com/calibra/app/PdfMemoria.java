package br.com.calibra.app;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

import javafx.application.HostServices;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import br.com.calibra.relatorios.DadosRelatorio;
import br.com.calibra.relatorios.RelatorioMemoria;

/**
 * PDF da memória de cálculo: pergunta onde salvar, gera fora da thread da tela (SPEC 10) e oferece abrir o
 * arquivo no fim. Usado pela calculadora e pelo histórico de cálculos do projeto.
 */
final class PdfMemoria {

    private static final DateTimeFormatter NOME_ARQUIVO = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm");

    private PdfMemoria() {
    }

    /**
     * @param estado recebe as mensagens para a barra de estado ("Gerando o PDF…", "PDF salvo em…")
     * @param aoTerminar chamado no fim, deu certo ou não (para liberar botões)
     * @return false se a pessoa desistiu na escolha do arquivo
     */
    static boolean gerar(Window janela, HostServices servicos, DadosRelatorio dados, String prefixoNome,
            Consumer<String> estado, Runnable aoTerminar) {
        Optional<File> arquivo = escolherArquivo(janela, prefixoNome + "-" + dados.geradoEm().format(NOME_ARQUIVO)
                + ".pdf");
        if (arquivo.isEmpty()) {
            return false;
        }
        Path destino = arquivo.get().toPath();
        Task<Void> tarefa = new Task<>() {
            @Override
            protected Void call() throws Exception {
                RelatorioMemoria.gerar(dados, destino);
                return null;
            }
        };
        tarefa.setOnSucceeded(evento -> {
            aoTerminar.run();
            estado.accept("PDF salvo em " + destino);
            oferecerAbrir(servicos, destino);
        });
        tarefa.setOnFailed(evento -> {
            aoTerminar.run();
            estado.accept("O PDF não foi gerado.");
            Throwable erro = tarefa.getException();
            Alert alerta = new Alert(Alert.AlertType.ERROR, (erro == null ? "Erro desconhecido." : erro.getMessage())
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

    private static Optional<File> escolherArquivo(Window janela, String nomeSugerido) {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Salvar memória de cálculo");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        escolha.setInitialFileName(nomeSugerido);
        Path documentos = Path.of(System.getProperty("user.home"), "Documents");
        if (Files.isDirectory(documentos)) {
            escolha.setInitialDirectory(documentos.toFile());
        }
        return Optional.ofNullable(escolha.showSaveDialog(janela));
    }

    private static void oferecerAbrir(HostServices servicos, Path arquivo) {
        ButtonType abrir = new ButtonType("Abrir", ButtonBar.ButtonData.OK_DONE);
        ButtonType fechar = new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, "PDF salvo em:\n" + arquivo, abrir, fechar);
        alerta.setTitle("Memória de cálculo");
        alerta.setHeaderText("PDF gerado.");
        Optional<ButtonType> resposta = alerta.showAndWait();
        if (resposta.isPresent() && resposta.get() == abrir && servicos != null) {
            servicos.showDocument(arquivo.toUri().toString());
        }
    }
}
