package br.com.calibra.app;

import java.util.List;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosInstalacao;
import br.com.calibra.app.casos.FormularioInstalacao;
import br.com.calibra.app.catalogo.CampoForm;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.cadastro.InstalacaoSalva;

/**
 * A janela da instalação do veículo (Fase 3B, SPEC 6.2): o tipo, a descrição, a marca, o modelo, a potência ou
 * especificação, a data, a OS de origem e as observações, no mesmo formulário dos catálogos. "Gravar" confere e grava;
 * com erro, a janela fica aberta e mostra o que corrigir.
 */
final class JanelaInstalacao {

    private final CasosInstalacao casos;
    private final long veiculoId;
    private final InstalacaoSalva salva;
    private final List<CampoForm> campos;
    private final FormularioTela tela;
    private final VBox erros = new VBox(4);
    private final Dialog<ButtonType> dialogo = new Dialog<>();
    private final ButtonType gravar = new ButtonType("Gravar", ButtonBar.ButtonData.OK_DONE);
    private boolean gravou;
    private String mensagem = "";

    /** A instalação nova (pela ficha, vazia; pela OS, com a OS e a data de hoje). */
    static JanelaInstalacao nova(Window dono, CasosInstalacao casos, CasosInstalacao.Rascunho rascunho) {
        return new JanelaInstalacao(dono, casos, rascunho.veiculoId(), null, rascunho);
    }

    /** A instalação gravada, para mudar; cada campo mudado vai para o histórico dela. */
    static JanelaInstalacao editar(Window dono, CasosInstalacao casos, InstalacaoSalva salva) {
        return new JanelaInstalacao(dono, casos, salva.veiculoId(), salva, null);
    }

    private JanelaInstalacao(Window dono, CasosInstalacao casos, long veiculoId, InstalacaoSalva salva,
            CasosInstalacao.Rascunho rascunho) {
        this.casos = casos;
        this.veiculoId = veiculoId;
        this.salva = salva;
        this.campos = FormularioInstalacao.campos(casos.osDoVeiculo(veiculoId));
        this.tela = new FormularioTela(campos, null);
        tela.preencher(salva == null ? FormularioInstalacao.valores(rascunho.instalacao(), rascunho.osId())
                : FormularioInstalacao.valores(salva.instalacao(), salva.osId()));
        ScrollPane rolagem = new ScrollPane(tela.raiz());
        rolagem.setFitToWidth(true);
        rolagem.setPrefViewportWidth(720);
        rolagem.setPrefViewportHeight(400);
        if (dono != null) {
            dialogo.initOwner(dono);
        }
        dialogo.setTitle(salva == null ? "Nova instalação" : "Editar instalação");
        dialogo.setHeaderText(salva == null ? "Nova instalação no veículo"
                : salva.instalacao().tipo().rotulo() + " · " + salva.instalacao().descricao()
                        + "\nCada campo mudado vai para o histórico da instalação.");
        dialogo.getDialogPane().setContent(new VBox(10, rolagem, erros));
        dialogo.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        dialogo.getDialogPane().getButtonTypes().setAll(gravar,
                new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().lookupButton(gravar).addEventFilter(ActionEvent.ACTION, evento -> {
            if (!gravar()) {
                evento.consume();
            }
        });
        dialogo.setResizable(true);
    }

    /** Mostra a janela e espera; devolve se gravou. */
    boolean mostrar() {
        Platform.runLater(tela::focarPrimeiro);
        dialogo.showAndWait();
        return gravou;
    }

    /**
     * Confere e grava o que está na janela (o que o botão Gravar faz).
     *
     * @return false se havia erro: as mensagens ficam na janela e nada é gravado
     */
    boolean gravar() {
        FormularioInstalacao.Leitura lida = FormularioInstalacao.ler(tela.valores(), campos);
        if (!lida.erros().isEmpty()) {
            mostrarErros(lida.erros());
            return false;
        }
        try {
            if (salva == null) {
                casos.criar(veiculoId, lida.osId(), lida.instalacao());
                mensagem = "Instalação gravada.";
            } else {
                casos.salvar(salva.id(), lida.osId(), lida.instalacao());
                mensagem = "Instalação salva. As mudanças estão no histórico dela.";
            }
            gravou = true;
            return true;
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
        return false;
    }

    /** O que aconteceu na gravação, para a tela que abriu a janela. */
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
