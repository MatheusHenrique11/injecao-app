package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.catalogo.FormaKit;
import br.com.calibra.core.orcamento.Kit.TipoLinha;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * As linhas do kit no formulário do catálogo (Fase 3B, SPEC 6.6.4): cada linha é um item ou um serviço escolhido no
 * catálogo, com a quantidade. Ao abrir, os nomes vêm do catálogo como estão agora (o kit guarda o nome só para mostrar),
 * e o arquivado aparece marcado. Os campos ficam sempre editáveis, como no {@link EditorTabela}.
 */
final class EditorLinhasKit extends VBox implements EditorDeLinhas {

    private static final double LARGURA_TIPO = 70;
    private static final double LARGURA_NOME = 340;

    /** Uma linha da tela; o nome é o do catálogo, sem a marca de arquivado. */
    private record LinhaTela(TipoLinha tipo, long id, String nome, TextField quantidade, HBox caixa) {
    }

    private final CasosCatalogo casos;
    private final VBox caixas = new VBox(4);
    private final List<LinhaTela> linhas = new ArrayList<>();
    private final Label vazio = new Label("Nenhuma linha ainda. Use \"+ Item do catálogo…\" ou \"+ Serviço do "
            + "catálogo…\".");

    EditorLinhasKit(CasosCatalogo casos) {
        super(6);
        this.casos = casos;
        HBox cabecalho = new HBox(6, titulo("Tipo", LARGURA_TIPO), titulo("Item ou serviço", LARGURA_NOME),
                titulo("Quantidade", 90));
        Button item = new Button("+ Item do catálogo…");
        item.setId("kitAdicionarItem");
        item.setOnAction(evento -> escolher(TipoCatalogo.ITEM, TipoLinha.ITEM, "Item do catálogo"));
        Button servico = new Button("+ Serviço do catálogo…");
        servico.setId("kitAdicionarServico");
        servico.setOnAction(evento -> escolher(TipoCatalogo.SERVICO, TipoLinha.SERVICO, "Serviço do catálogo"));
        vazio.getStyleClass().add("dica");
        getChildren().addAll(cabecalho, caixas, new HBox(8, item, servico));
        mostrarVazio();
    }

    @Override
    public List<List<String>> linhas() {
        return linhas.stream()
                .map(linha -> FormaKit.linha(linha.tipo(), linha.id(), linha.nome(), linha.quantidade().getText()))
                .toList();
    }

    @Override
    public void preencher(List<List<String>> valores) {
        linhas.clear();
        caixas.getChildren().clear();
        for (List<String> valor : valores) {
            TipoLinha tipo = FormaKit.tipo(valor);
            long id = FormaKit.id(valor);
            Optional<ItemCatalogo<?>> noCatalogo = buscar(tipo, id);
            acrescentar(tipo, id, noCatalogo.map(item -> item.info().nome()).orElse(FormaKit.nome(valor)),
                    noCatalogo.map(ItemCatalogo::arquivado).orElse(false), noCatalogo.isEmpty(),
                    FormaKit.quantidade(valor));
        }
        mostrarVazio();
    }

    /** Acrescenta o item ou o serviço escolhido, com quantidade 1 (a escolha da janela, e os testes). */
    void adicionar(TipoLinha tipo, ItemCatalogo<?> item) {
        acrescentar(tipo, item.id(), item.info().nome(), item.arquivado(), false, "1");
        mostrarVazio();
    }

    /** O campo da quantidade da linha (a partir de 0), para os testes. */
    TextField quantidade(int linha) {
        return linhas.get(linha).quantidade();
    }

    private <T> void escolher(TipoCatalogo<T> tipoCatalogo, TipoLinha tipo, String titulo) {
        new JanelaEscolhaCatalogo<>(janela(), casos, tipoCatalogo, titulo, "A linha entra com quantidade 1; mude a "
                + "quantidade no kit. No orçamento, ela usa o preço de agora do catálogo.", item -> true, null)
                .escolher().ifPresent(item -> adicionar(tipo, item));
    }

    /** O item ou o serviço como o catálogo tem agora; vazio se não achar. */
    private Optional<ItemCatalogo<?>> buscar(TipoLinha tipo, long id) {
        if (tipo == TipoLinha.ITEM) {
            return casos.buscar(TipoCatalogo.ITEM, id).map(item -> item);
        }
        return casos.buscar(TipoCatalogo.SERVICO, id).map(item -> item);
    }

    private void acrescentar(TipoLinha tipo, long id, String nome, boolean arquivado, boolean sumiu,
            String quantidade) {
        Label rotuloTipo = new Label(tipo == TipoLinha.ITEM ? "Item" : "Serviço");
        rotuloTipo.setPrefWidth(LARGURA_TIPO);
        Label rotuloNome = new Label(nome + (sumiu ? " (não está no catálogo)" : arquivado ? " (arquivado)" : ""));
        rotuloNome.setPrefWidth(LARGURA_NOME);
        if (arquivado || sumiu) {
            rotuloNome.getStyleClass().add("aviso-amarelo");
        }
        TextField campo = new TextField(quantidade);
        campo.setPrefColumnCount(6);
        campo.setAccessibleText("Quantidade de " + nome);
        Button remover = new Button("✕");
        remover.setTooltip(new Tooltip("Remover esta linha"));
        remover.setAccessibleText("Remover " + nome);
        HBox caixa = new HBox(6, rotuloTipo, rotuloNome, campo, remover);
        caixa.setAlignment(Pos.CENTER_LEFT);
        LinhaTela linha = new LinhaTela(tipo, id, nome, campo, caixa);
        remover.setOnAction(evento -> {
            linhas.remove(linha);
            caixas.getChildren().remove(caixa);
            mostrarVazio();
        });
        linhas.add(linha);
        caixas.getChildren().add(caixa);
    }

    private void mostrarVazio() {
        if (linhas.isEmpty()) {
            caixas.getChildren().setAll(vazio);
        } else {
            caixas.getChildren().remove(vazio);
        }
    }

    private Window janela() {
        return getScene() == null ? null : getScene().getWindow();
    }

    private static Label titulo(String texto, double largura) {
        Label titulo = new Label(texto);
        titulo.getStyleClass().add("negrito");
        titulo.setPrefWidth(largura);
        return titulo;
    }
}
