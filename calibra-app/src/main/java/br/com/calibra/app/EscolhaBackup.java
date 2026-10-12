package br.com.calibra.app;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import br.com.calibra.app.casos.ServicoBackup;
import br.com.calibra.dados.backup.InfoBackup;

/** Janela para escolher o backup a restaurar: a lista das pastas de backup ou outro arquivo zip. */
final class EscolhaBackup {

    private EscolhaBackup() {
    }

    static Optional<Path> mostrar(Window dono, List<InfoBackup> backups, Path pastaInicial) {
        ListView<InfoBackup> lista = new ListView<>(FXCollections.observableArrayList(backups));
        lista.setCellFactory(visao -> new ListCell<>() {
            @Override
            protected void updateItem(InfoBackup info, boolean vazio) {
                super.updateItem(info, vazio);
                setText(vazio || info == null ? null : ServicoBackup.descrever(info) + " · Calibra "
                        + info.manifesto().versaoApp() + "\n" + info.arquivo().getParent());
            }
        });
        lista.setPlaceholder(new Label("Nenhum backup nas pastas de backup. Use \"Outro arquivo…\"."));
        lista.setPrefSize(720, 360);

        ButtonType restaurar = new ButtonType("Restaurar o escolhido…", ButtonData.OK_DONE);
        ButtonType outro = new ButtonType("Outro arquivo…", ButtonData.OTHER);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
        Dialog<Path> dialogo = new Dialog<>();
        if (dono != null) {
            dialogo.initOwner(dono);
        }
        dialogo.setTitle("Restaurar um backup");
        dialogo.setHeaderText("Escolha o backup que vai substituir os dados atuais (o mais novo está em cima).");
        dialogo.getDialogPane().setContent(lista);
        dialogo.getDialogPane().getButtonTypes().addAll(restaurar, outro, cancelar);
        dialogo.getDialogPane().lookupButton(restaurar).disableProperty()
                .bind(lista.getSelectionModel().selectedItemProperty().isNull());
        dialogo.setResultConverter(botao -> {
            if (botao == restaurar) {
                return lista.getSelectionModel().getSelectedItem().arquivo();
            }
            if (botao == outro) {
                return escolherArquivo(dono, pastaInicial);
            }
            return null;
        });
        return dialogo.showAndWait();
    }

    private static Path escolherArquivo(Window dono, Path pastaInicial) {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Arquivo de backup do Calibra");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup do Calibra (zip)", "*.zip"));
        if (pastaInicial != null && Files.isDirectory(pastaInicial)) {
            escolha.setInitialDirectory(pastaInicial.toFile());
        }
        File arquivo = escolha.showOpenDialog(dono);
        return arquivo == null ? null : arquivo.toPath();
    }
}
