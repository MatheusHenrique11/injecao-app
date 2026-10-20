package br.com.calibra.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosInstalacao;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.cadastro.ClienteSalvo;
import br.com.calibra.dados.cadastro.EventoAuditoria;
import br.com.calibra.dados.cadastro.InstalacaoSalva;
import br.com.calibra.dados.cadastro.VeiculoSalvo;

/**
 * As janelas de cadastro do projeto e da ficha do veículo (bloco 3.3): editar o cliente e o veículo, ver o histórico de
 * cada um e confirmar a anonimização do cliente (LGPD). Na Fase 3B, também o histórico e a retirada das instalações do
 * veículo.
 */
final class JanelasCadastro {

    private JanelasCadastro() {
    }

    /** Edita o cliente; true se gravou. Com erro, a mensagem aparece na própria janela, que fica aberta. */
    static boolean editarCliente(Window dono, CasosProjeto casos, ClienteSalvo cliente) {
        FXMLLoader carregador = new FXMLLoader(JanelasCadastro.class.getResource("cliente.fxml"));
        Parent formulario = carregar(carregador);
        ClienteFormController form = carregador.getController();
        form.preencher(cliente.cliente());
        return editar(dono, "Editar cliente", formulario, () -> casos.salvarCliente(cliente.id(), form.ler()));
    }

    /** Edita o veículo, com o câmbio do catálogo; true se gravou. Cada campo mudado vai para o histórico do veículo. */
    static boolean editarVeiculo(Window dono, CasosProjeto casos, CasosCatalogo catalogo, VeiculoSalvo veiculo) {
        FXMLLoader carregador = new FXMLLoader(JanelasCadastro.class.getResource("veiculo.fxml"));
        Parent formulario = carregar(carregador);
        VeiculoFormController form = carregador.getController();
        form.usarCatalogo(catalogo);
        form.preencher(veiculo.veiculo(), veiculo.cambioItemId());
        ScrollPane rolagem = new ScrollPane(formulario);
        rolagem.setFitToWidth(true);
        rolagem.setPrefViewportHeight(480);
        rolagem.setPrefViewportWidth(760);
        return editar(dono, "Editar veículo", rolagem, () -> {
            VeiculoFormController.Leitura leitura = form.ler();
            if (!leitura.erros().isEmpty()) {
                throw new EntradaInvalidaException(leitura.erros());
            }
            casos.salvarVeiculo(veiculo.id(), leitura.veiculo(), form.cambioItemId());
        });
    }

    static void historicoCliente(Window dono, CasosProjeto casos, long clienteId) {
        historico(dono, "Histórico do cliente", casos.historicoCliente(clienteId));
    }

    static void historicoVeiculo(Window dono, CasosProjeto casos, long veiculoId) {
        historico(dono, "Histórico do veículo", casos.historicoVeiculo(veiculoId));
    }

    /** O histórico de uma instalação do veículo (Fase 3B): a criação, cada campo mudado e a retirada. */
    static void historicoInstalacao(Window dono, CasosInstalacao casos, InstalacaoSalva instalacao) {
        dialogoDoHistoricoDaInstalacao(dono, casos, instalacao).showAndWait();
    }

    /** A janela do histórico da instalação, sem mostrar (para os testes). */
    static Dialog<ButtonType> dialogoDoHistoricoDaInstalacao(Window dono, CasosInstalacao casos,
            InstalacaoSalva instalacao) {
        Dialog<ButtonType> dialogo = dialogoDoHistorico(dono, "Histórico da instalação",
                casos.historico(instalacao.id()));
        dialogo.setHeaderText("Histórico da instalação · " + instalacao.instalacao().tipo().rotulo() + " · "
                + LaudosController.resumo(instalacao.instalacao().descricao()));
        return dialogo;
    }

    /**
     * Pergunta a data em que o item saiu do veículo (vem a de hoje) e retira. Com erro (ex.: antes da instalação), a
     * mensagem aparece na janela, que fica aberta. true se retirou.
     */
    static boolean retirarInstalacao(Window dono, CasosInstalacao casos, InstalacaoSalva instalacao, LocalDate hoje) {
        DatePicker data = new DatePicker(hoje);
        Datas.emPortugues(data);
        Label explicacao = new Label("O item continua na lista, em cinza, e a retirada vai para o histórico. Se foi "
                + "engano, \"Desfazer retirada\" volta.");
        explicacao.getStyleClass().add("dica");
        explicacao.setWrapText(true);
        VBox erros = new VBox(4);
        HBox linha = new HBox(10, new Label("Saiu do veículo em"), data);
        linha.setAlignment(Pos.CENTER_LEFT);
        VBox conteudo = new VBox(10, linha, explicacao, erros);
        conteudo.setPrefWidth(520);
        Dialog<ButtonType> dialogo = dialogo(dono, "Retirar instalação", conteudo);
        dialogo.setHeaderText("Retirar · " + instalacao.instalacao().tipo().rotulo() + " · "
                + LaudosController.resumo(instalacao.instalacao().descricao()));
        ButtonType retirar = new ButtonType("Retirar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().setAll(retirar, new ButtonType("Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().lookupButton(retirar).addEventFilter(ActionEvent.ACTION, evento -> {
            String texto = data.getEditor().getText();
            LocalDate lida = data.getConverter().fromString(texto);
            try {
                if (lida == null) {
                    throw new EntradaInvalidaException(texto == null || texto.isBlank()
                            ? "Informe a data em que o item saiu do veículo."
                            : "Data: use dia/mês/ano, como 20/10/2026.");
                }
                casos.retirar(instalacao.id(), lida);
            } catch (EntradaInvalidaException erro) {
                mostrarErros(erros, erro.erros());
                evento.consume();
            } catch (ErroDados erro) {
                mostrarErros(erros, List.of(erro.getMessage()));
                evento.consume();
            }
        });
        return dialogo.showAndWait().filter(retirar::equals).isPresent();
    }

    /** Pergunta antes de anonimizar, dizendo o que some e o que fica; não tem volta. */
    static boolean confirmarAnonimizacao(Window dono, long clienteId) {
        ButtonType anonimizar = new ButtonType("Anonimizar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alerta = Alertas.criar(Alert.AlertType.CONFIRMATION, "O nome vira \"Cliente anonimizado #" + clienteId
                + "\" e telefone, e-mail, documento e observações são apagados, também no histórico. Os veículos dele "
                + "perdem placa, chassi e identificação da embarcação, e os anexos marcados como documento pessoal "
                + "(como CNH e CRLV) são apagados. Fotos, laudos, notas fiscais, OS, projetos, motores e cálculos "
                + "ficam.\n\nIsto não pode ser desfeito.", anonimizar, cancelar);
        if (dono != null) {
            alerta.initOwner(dono);
        }
        alerta.setTitle("Anonimizar o cliente");
        alerta.setHeaderText("Anonimizar o cliente?");
        return alerta.showAndWait().filter(anonimizar::equals).isPresent();
    }

    /**
     * O complemento da mensagem quando arquivos dos documentos apagados ficaram na pasta (abertos em outro programa,
     * como um PDF no leitor); vazio se nenhum ficou.
     */
    static String avisoArquivosPresos(int presos) {
        if (presos == 0) {
            return "";
        }
        return (presos == 1 ? " 1 arquivo estava aberto em outro programa e ainda não saiu da pasta"
                : " " + presos + " arquivos estavam abertos em outro programa e ainda não saíram da pasta")
                + ": feche o programa; o Calibra apaga na próxima vez que abrir.";
    }

    /** A janela de edição: Salvar grava e fecha; com erro, mostra a mensagem e fica aberta. */
    private static boolean editar(Window dono, String titulo, Node formulario, Runnable gravar) {
        VBox erros = new VBox(4);
        Dialog<ButtonType> dialogo = dialogo(dono, titulo, new VBox(10, formulario, erros));
        ButtonType salvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().setAll(salvar, new ButtonType("Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().lookupButton(salvar).addEventFilter(ActionEvent.ACTION, evento -> {
            try {
                gravar.run();
            } catch (EntradaInvalidaException erro) {
                mostrarErros(erros, erro.erros());
                evento.consume();
            } catch (ErroDados erro) {
                mostrarErros(erros, List.of(erro.getMessage()));
                evento.consume();
            }
        });
        return dialogo.showAndWait().filter(salvar::equals).isPresent();
    }

    private static void historico(Window dono, String titulo, List<EventoAuditoria> eventos) {
        dialogoDoHistorico(dono, titulo, eventos).showAndWait();
    }

    /** A janela do histórico (data e hora, campo, antes e depois), com o botão Fechar, sem mostrar. */
    private static Dialog<ButtonType> dialogoDoHistorico(Window dono, String titulo, List<EventoAuditoria> eventos) {
        TableView<EventoAuditoria> tabela = new TableView<>();
        tabela.getColumns().setAll(List.of(
                coluna("Data e hora", 140, e -> Datas.diaHora(e.dataHora())),
                coluna("Campo", 210, EventoAuditoria::campo),
                coluna("Antes", 240, e -> valor(e.valorAntigo())),
                coluna("Depois", 240, e -> valor(e.valorNovo()))));
        tabela.getItems().setAll(eventos);
        tabela.setPrefSize(860, 360);
        Label vazio = new Label("Nenhuma mudança registrada.");
        vazio.getStyleClass().add("dica");
        tabela.setPlaceholder(vazio);
        Dialog<ButtonType> dialogo = dialogo(dono, titulo, tabela);
        dialogo.getDialogPane().getButtonTypes().setAll(new ButtonType("Fechar", ButtonBar.ButtonData.CANCEL_CLOSE));
        return dialogo;
    }

    private static Dialog<ButtonType> dialogo(Window dono, String titulo, Node conteudo) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        if (dono != null) {
            dialogo.initOwner(dono);
        }
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.getDialogPane().setContent(conteudo);
        dialogo.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        dialogo.setResizable(true);
        return dialogo;
    }

    private static Parent carregar(FXMLLoader carregador) {
        try {
            return carregador.load();
        } catch (IOException erro) {
            throw new UncheckedIOException("Tela do app não abriu: " + carregador.getLocation(), erro);
        }
    }

    private static void mostrarErros(VBox caixa, List<String> erros) {
        caixa.getChildren().clear();
        for (String erro : erros) {
            Label rotulo = new Label("• " + erro);
            rotulo.getStyleClass().add("erro");
            rotulo.setWrapText(true);
            caixa.getChildren().add(rotulo);
        }
    }

    private static <T> TableColumn<T, String> coluna(String titulo, double largura, Function<T, String> valor) {
        TableColumn<T, String> coluna = new TableColumn<>(titulo);
        coluna.setPrefWidth(largura);
        coluna.setCellValueFactory(celula -> new ReadOnlyStringWrapper(valor.apply(celula.getValue())));
        return coluna;
    }

    private static String valor(String texto) {
        return texto == null || texto.isEmpty() ? "—" : texto;
    }
}
