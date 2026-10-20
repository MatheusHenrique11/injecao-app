package br.com.calibra.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import br.com.calibra.app.catalogo.FormaChecklist;

/**
 * Os itens do checklist no formulário do catálogo (Fase 3B, SPEC 6.6.2): uma linha por item, com o texto, as marcas
 * "obrigatório" e "a verificar" e os botões para subir, descer e remover. O item novo entra obrigatório e sem o "a
 * verificar" (quem escreve é o dono). Os campos ficam sempre editáveis, como no {@link EditorTabela}.
 */
final class EditorItensChecklist extends VBox implements EditorDeLinhas {

    /** Uma linha da tela. */
    private record LinhaTela(TextField texto, CheckBox obrigatorio, CheckBox aVerificar, HBox caixa) {
    }

    private final VBox caixas = new VBox(4);
    private final List<LinhaTela> linhas = new ArrayList<>();

    EditorItensChecklist() {
        super(6);
        Button item = new Button("+ Item");
        item.setId("checklistAdicionarItem");
        item.setOnAction(evento -> adicionar().requestFocus());
        getChildren().addAll(caixas, item);
        preencher(List.of());
    }

    /** Os itens como estão na tela (inclusive os sem texto, que a leitura deixa de lado). */
    @Override
    public List<List<String>> linhas() {
        return linhas.stream().map(linha -> FormaChecklist.linha(linha.texto().getText(),
                linha.obrigatorio().isSelected(), linha.aVerificar().isSelected())).toList();
    }

    /** Troca os itens; sem nenhum, deixa um vazio para digitar. */
    @Override
    public void preencher(List<List<String>> valores) {
        linhas.clear();
        caixas.getChildren().clear();
        for (List<String> valor : valores) {
            acrescentar(FormaChecklist.texto(valor), FormaChecklist.obrigatorio(valor),
                    FormaChecklist.aVerificar(valor));
        }
        if (valores.isEmpty()) {
            acrescentar("", true, false);
        }
    }

    /** Acrescenta um item vazio no fim, obrigatório (o botão "+ Item" e os testes); devolve o campo do texto. */
    TextField adicionar() {
        return acrescentar("", true, false).texto();
    }

    /** Quantos itens há na tela, para os testes. */
    int quantos() {
        return linhas.size();
    }

    /** O campo do texto do item (a partir de 0), para os testes. */
    TextField texto(int indice) {
        return linhas.get(indice).texto();
    }

    /** A marca "obrigatório" do item (a partir de 0), para os testes. */
    CheckBox obrigatorio(int indice) {
        return linhas.get(indice).obrigatorio();
    }

    /** Troca o item de lugar com o de cima (-1) ou o de baixo (+1); na ponta, não faz nada. */
    void mover(int indice, int passo) {
        int destino = indice + passo;
        if (indice < 0 || indice >= linhas.size() || destino < 0 || destino >= linhas.size()) {
            return;
        }
        Collections.swap(linhas, indice, destino);
        caixas.getChildren().setAll(linhas.stream().map(LinhaTela::caixa).toList());
    }

    private LinhaTela acrescentar(String texto, boolean obrigatorio, boolean aVerificar) {
        TextField campo = new TextField(texto);
        campo.setPrefColumnCount(28);
        campo.setAccessibleText("Item do checklist");
        HBox.setHgrow(campo, Priority.ALWAYS);
        CheckBox marcaObrigatorio = new CheckBox("Obrigatório");
        marcaObrigatorio.setSelected(obrigatorio);
        marcaObrigatorio.setTooltip(new Tooltip("Pendente, trava o \"pronto\" da OS até uma justificativa"));
        marcaObrigatorio.setMinWidth(Region.USE_PREF_SIZE);
        CheckBox marcaAVerificar = new CheckBox("A verificar");
        marcaAVerificar.setSelected(aVerificar);
        marcaAVerificar.setTooltip(new Tooltip("O texto ainda não foi conferido"));
        marcaAVerificar.setMinWidth(Region.USE_PREF_SIZE);
        Button subir = botao("↑", "Subir este item");
        Button descer = botao("↓", "Descer este item");
        Button remover = botao("✕", "Remover este item");
        HBox caixa = new HBox(6, campo, marcaObrigatorio, marcaAVerificar, subir, descer, remover);
        caixa.setAlignment(Pos.CENTER_LEFT);
        LinhaTela linha = new LinhaTela(campo, marcaObrigatorio, marcaAVerificar, caixa);
        subir.setOnAction(evento -> mover(linhas.indexOf(linha), -1));
        descer.setOnAction(evento -> mover(linhas.indexOf(linha), 1));
        remover.setOnAction(evento -> {
            linhas.remove(linha);
            caixas.getChildren().remove(caixa);
            if (linhas.isEmpty()) {
                acrescentar("", true, false);
            }
        });
        linhas.add(linha);
        caixas.getChildren().add(caixa);
        return linha;
    }

    private static Button botao(String texto, String dica) {
        Button botao = new Button(texto);
        botao.setTooltip(new Tooltip(dica));
        botao.setAccessibleText(dica);
        botao.setMinWidth(Region.USE_PREF_SIZE);
        return botao;
    }
}
