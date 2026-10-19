package br.com.calibra.app;

import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosCatalogo.Gravacao;
import br.com.calibra.app.catalogo.FormularioCatalogo;
import br.com.calibra.app.catalogo.ValoresForm;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.catalogo.ItemCatalogo;

/**
 * Janela de cadastro ou edição de um item de catálogo. "Salvar" confere os campos e grava: item novo na v1, item
 * existente numa versão nova (se algo mudou). Com erro, a janela fica aberta e mostra o que corrigir.
 */
final class JanelaItemCatalogo<T> {

    private final CasosCatalogo casos;
    private final FormularioCatalogo<T> formulario;
    private final ItemCatalogo<T> item;
    private final FormularioTela tela;
    private final VBox erros = new VBox(4);
    private final Dialog<ButtonType> dialogo = new Dialog<>();
    private final ButtonType salvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
    private ItemCatalogo<T> gravado;
    private String mensagem = "";

    /**
     * @param item null para um item novo (ou uma cópia)
     * @param valores o que aparece nos campos ao abrir
     */
    JanelaItemCatalogo(Window dono, CasosCatalogo casos, FormularioCatalogo<T> formulario, ItemCatalogo<T> item,
            ValoresForm valores) {
        this.casos = casos;
        this.formulario = formulario;
        this.item = item;
        this.tela = new FormularioTela(formulario.campos(), casos);
        tela.preencher(valores);
        if (item != null) {
            tela.travarCamposFixos();
        }
        ScrollPane rolagem = new ScrollPane(tela.raiz());
        rolagem.setFitToWidth(true);
        rolagem.setPrefViewportWidth(760);
        rolagem.setPrefViewportHeight(520);
        if (dono != null) {
            dialogo.initOwner(dono);
        }
        String singular = formulario.tipo().singular();
        dialogo.setTitle(item == null ? "Cadastrar " + singular : "Editar " + singular);
        dialogo.setHeaderText(item == null ? "Cadastrar " + singular
                : item.info().nome() + " · versão atual v" + item.versao() + ". Salvar com mudanças grava a v"
                        + (item.versao() + 1) + "; a anterior fica guardada.");
        dialogo.getDialogPane().setContent(new VBox(10, rolagem, erros));
        dialogo.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        dialogo.getDialogPane().getButtonTypes().setAll(salvar,
                new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().lookupButton(salvar).addEventFilter(ActionEvent.ACTION, evento -> {
            if (!gravar()) {
                evento.consume();
            }
        });
        dialogo.setResizable(true);
    }

    /** Mostra a janela e espera; devolve o item gravado (vazio se cancelou). */
    Optional<ItemCatalogo<T>> mostrar() {
        Platform.runLater(tela::focarPrimeiro);
        dialogo.showAndWait();
        return Optional.ofNullable(gravado);
    }

    /**
     * Confere e grava o que está nos campos (o que o botão Salvar faz).
     *
     * @return false se havia erro: as mensagens ficam na janela e nada é gravado
     */
    boolean gravar() {
        FormularioCatalogo.Leitura<T> lido = formulario.ler(tela.valores());
        if (!lido.ok()) {
            mostrarErros(lido.erros());
            return false;
        }
        try {
            if (item == null) {
                gravado = casos.criar(formulario.tipo(), lido.dados(), lido.info());
                mensagem = "Cadastrado: " + gravado.info().nome() + " (v1).";
            } else {
                Gravacao<T> gravacao = casos.salvar(formulario.tipo(), item.id(), lido.dados(), lido.info());
                gravado = gravacao.item();
                mensagem = gravacao.versaoNova() ? "Gravado: " + gravado.info().nome() + " (v" + gravado.versao() + ")."
                        : "Nada mudou em " + gravado.info().nome() + ": nenhuma versão nova.";
            }
            return true;
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
        return false;
    }

    /** O que aconteceu na última gravação, para a barra da lista. */
    String mensagem() {
        return mensagem;
    }

    FormularioTela tela() {
        return tela;
    }

    Dialog<ButtonType> dialogo() {
        return dialogo;
    }

    /** As mensagens de erro mostradas na janela. */
    List<String> erros() {
        return erros.getChildren().stream().map(no -> ((Label) no).getText().substring(2)).toList();
    }

    private void mostrarErros(List<String> mensagens) {
        erros.getChildren().clear();
        for (String erro : mensagens) {
            Label rotulo = new Label("• " + erro);
            rotulo.getStyleClass().add("erro");
            rotulo.setWrapText(true);
            erros.getChildren().add(rotulo);
        }
    }
}
