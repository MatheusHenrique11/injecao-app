package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.catalogo.FormularioSugeridos;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.ServicosSugeridos;
import br.com.calibra.core.orcamento.ServicosSugeridos.Sugestao;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * "Serviços sugeridos…" na aba Serviços dos Catálogos (SPEC 6.6.1): a lista de partida da auto elétrica e da injeção
 * programável, agrupada por categoria. O dono marca os que a oficina faz e preenche as horas ou o preço de cada um (o
 * app não inventa tempo de serviço, decisão 216). Os que já estão no catálogo aparecem como "já cadastrado".
 */
final class JanelaServicosSugeridos {

    /** Uma linha da janela: a caixa de marcar (com o nome), as horas e o preço. */
    record LinhaTela(Sugestao sugestao, CheckBox marcar, TextField horas, TextField preco, boolean cadastrado) {

        FormularioSugeridos.Linha lida() {
            return new FormularioSugeridos.Linha(sugestao, marcar.isSelected(), horas.getText(), preco.getText());
        }
    }

    private final CasosCatalogo casos;
    private final Dialog<ButtonType> dialogo = new Dialog<>();
    private final ButtonType adicionar = new ButtonType("Adicionar ao catálogo", ButtonBar.ButtonData.OK_DONE);
    private final List<LinhaTela> linhas = new ArrayList<>();
    private final Label erro = new Label();
    private int gravados;

    JanelaServicosSugeridos(Window dono, CasosCatalogo casos) {
        this.casos = casos;
        Set<String> cadastrados = casos.listar(TipoCatalogo.SERVICO, "", true).stream()
                .map(item -> FormularioSugeridos.chave(item.info().nome())).collect(Collectors.toSet());
        GridPane grade = new GridPane();
        grade.getStyleClass().add("grade");
        grade.addRow(0, negrito("Serviço"), negrito("Horas"), negrito("Preço (R$)"));
        int linha = 1;
        CategoriaServico categoria = null;
        List<Sugestao> todos = ServicosSugeridos.todos();
        for (int i = 0; i < todos.size(); i++) {
            Sugestao sugestao = todos.get(i);
            if (sugestao.categoria() != categoria) {
                categoria = sugestao.categoria();
                Label titulo = new Label(categoria.rotulo());
                titulo.getStyleClass().add("titulo-secao");
                grade.add(titulo, 0, linha++, 4, 1);
            }
            LinhaTela nova = montar(i, sugestao, cadastrados.contains(FormularioSugeridos.chave(sugestao.nome())));
            Label nota = new Label(nova.cadastrado() ? "já cadastrado"
                    : sugestao.exigeEntradaDoVeiculo() ? "" : "de bancada, sem o veículo");
            nota.getStyleClass().add("dica");
            grade.addRow(linha++, nova.marcar(), nova.horas(), nova.preco(), nota);
            linhas.add(nova);
        }
        ScrollPane rolagem = new ScrollPane(grade);
        rolagem.setFitToWidth(true);
        rolagem.setPrefViewportWidth(780);
        rolagem.setPrefViewportHeight(440);
        Label dica = new Label("A lista vem sem horas e sem preço: o app não inventa tempo de serviço. Marque os que a "
                + "oficina faz e preencha as horas (o tempo padrão) ou o preço de referência de cada um; digitar já "
                + "marca a linha. Depois, cada serviço se edita na lista, como os outros (descrição no orçamento, "
                + "garantia própria).");
        dica.getStyleClass().add("dica");
        dica.setWrapText(true);
        erro.getStyleClass().add("erro");
        erro.setWrapText(true);
        erro.setId("erroSugeridos");

        if (dono != null) {
            dialogo.initOwner(dono);
        }
        dialogo.setTitle("Serviços sugeridos");
        dialogo.setHeaderText("Serviços sugeridos: marque os que a oficina faz e preencha as horas ou o preço");
        VBox conteudo = new VBox(8, dica, rolagem, erro);
        // Sem a largura fixa, a dica (uma linha comprida) abriria a janela do tamanho dela, em vez de quebrar.
        conteudo.setPrefWidth(820);
        dialogo.getDialogPane().setContent(conteudo);
        dialogo.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        dialogo.getDialogPane().getButtonTypes().setAll(adicionar, new ButtonType("Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().lookupButton(adicionar).addEventFilter(ActionEvent.ACTION, evento -> {
            if (!gravar()) {
                evento.consume();
            }
        });
        dialogo.setResizable(true);
    }

    /** Mostra e espera; devolve a mensagem para a aba Serviços (vazio se cancelou). */
    Optional<String> mostrar() {
        return dialogo.showAndWait().filter(adicionar::equals).map(botao -> mensagem());
    }

    /**
     * Lê as linhas marcadas e grava todas de uma vez, cada serviço com o checklist da categoria dele. Com erro, nada é
     * gravado, a mensagem fica na janela e ela não fecha.
     *
     * @return true se gravou
     */
    boolean gravar() {
        FormularioSugeridos.Leitura leitura = FormularioSugeridos.ler(linhas.stream().map(LinhaTela::lida).toList(),
                casos.checklistsDasSementes());
        if (!leitura.ok()) {
            erro.setText(String.join("\n", leitura.erros()));
            return false;
        }
        try {
            gravados = casos.adicionarServicosSugeridos(leitura.servicos()).size();
            erro.setText("");
            return true;
        } catch (EntradaInvalidaException recusado) {
            erro.setText(String.join("\n", recusado.erros()));
            return false;
        }
    }

    /** "3 serviços adicionados ao catálogo. …" */
    String mensagem() {
        return (gravados == 1 ? "1 serviço adicionado" : gravados + " serviços adicionados") + " ao catálogo. Edite "
                + "cada um para pôr a descrição no orçamento ou a garantia própria.";
    }

    /** A linha de um serviço sugerido, pelo nome (para os testes). */
    LinhaTela linha(String nome) {
        return linhas.stream().filter(l -> l.sugestao().nome().equals(nome)).findFirst().orElseThrow();
    }

    Label erro() {
        return erro;
    }

    Dialog<ButtonType> dialogo() {
        return dialogo;
    }

    /** @param indice a posição na lista sugerida, para os ids ("sugeridoHoras3") */
    private static LinhaTela montar(int indice, Sugestao sugestao, boolean cadastrado) {
        CheckBox marcar = new CheckBox(sugestao.nome());
        marcar.setId("sugeridoMarcar" + indice);
        TextField horas = new TextField();
        horas.setId("sugeridoHoras" + indice);
        horas.setPromptText("ex.: 1,5");
        horas.setPrefColumnCount(6);
        horas.setAccessibleText("Horas de " + sugestao.nome());
        TextField preco = new TextField();
        preco.setId("sugeridoPreco" + indice);
        preco.setPromptText("ex.: 150,00");
        preco.setPrefColumnCount(8);
        preco.setAccessibleText("Preço de " + sugestao.nome());
        if (cadastrado) {
            marcar.setDisable(true);
            horas.setDisable(true);
            preco.setDisable(true);
        } else {
            // Digitar as horas ou o preço já marca a linha (desmarcar continua valendo: a linha fica de fora).
            horas.textProperty().addListener((obs, antes, depois) -> marcarSePreencheu(marcar, antes, depois));
            preco.textProperty().addListener((obs, antes, depois) -> marcarSePreencheu(marcar, antes, depois));
        }
        return new LinhaTela(sugestao, marcar, horas, preco, cadastrado);
    }

    /** Marca quando o campo deixa de estar em branco; não desmarca o que a pessoa marcou. */
    private static void marcarSePreencheu(CheckBox marcar, String antes, String depois) {
        boolean estavaVazio = antes == null || antes.isBlank();
        if (estavaVazio && depois != null && !depois.isBlank()) {
            marcar.setSelected(true);
        }
    }

    private static Label negrito(String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("negrito");
        return rotulo;
    }
}
