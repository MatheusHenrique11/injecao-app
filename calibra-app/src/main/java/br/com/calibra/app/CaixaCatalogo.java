package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.util.StringConverter;

import br.com.calibra.dados.catalogo.ItemCatalogo;

/**
 * Caixa de escolha de um item do catálogo, com uma opção vazia (ex.: "(digitar os dados)"). Mostra o nome, a
 * versão e se o item está "a verificar" ou arquivado.
 */
final class CaixaCatalogo<T> {

    private final ComboBox<ItemCatalogo<T>> caixa;

    CaixaCatalogo(ComboBox<ItemCatalogo<T>> caixa, String textoVazio) {
        this.caixa = caixa;
        StringConverter<ItemCatalogo<T>> conversor = new StringConverter<>() {
            @Override
            public String toString(ItemCatalogo<T> item) {
                return item == null ? textoVazio : rotulo(item);
            }

            @Override
            public ItemCatalogo<T> fromString(String texto) {
                return null;
            }
        };
        caixa.setConverter(conversor);
        caixa.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(ItemCatalogo<T> item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio ? null : conversor.toString(item));
            }
        });
        caixa.setPromptText(textoVazio);
        itens(List.of());
    }

    /** "Bosch 440 · v2 · a verificar". */
    static String rotulo(ItemCatalogo<?> item) {
        return item.info().nome() + " · v" + item.versao() + (item.info().aVerificar() ? " · a verificar" : "")
                + (item.arquivado() ? " · arquivado" : "");
    }

    /** Troca as opções; o item escolhido continua escolhido (na versão nova, se mudou), se ainda estiver na lista. */
    void itens(List<ItemCatalogo<T>> itens) {
        Long escolhido = escolhido() == null ? null : escolhido().id();
        List<ItemCatalogo<T>> opcoes = new ArrayList<>();
        opcoes.add(null);
        opcoes.addAll(itens);
        caixa.getItems().setAll(opcoes);
        caixa.setValue(opcoes.stream().filter(i -> i != null && Objects.equals(i.id(), escolhido)).findFirst()
                .orElse(null));
    }

    ItemCatalogo<T> escolhido() {
        return caixa.getValue();
    }

    /** Escolhe o item; se ele não está nas opções (ex.: arquivado, usado num cálculo salvo), entra nelas. */
    void escolher(ItemCatalogo<T> item) {
        if (item == null) {
            caixa.setValue(null);
            return;
        }
        ItemCatalogo<T> naLista = caixa.getItems().stream().filter(i -> i != null && i.id() == item.id()).findFirst()
                .orElse(null);
        if (naLista == null) {
            caixa.getItems().add(item);
            naLista = item;
        }
        caixa.setValue(naLista);
    }

    /** Volta para a opção vazia. */
    void limpar() {
        caixa.setValue(null);
    }

    void aoMudar(Runnable acao) {
        caixa.valueProperty().addListener((obs, antes, depois) -> acao.run());
    }

    ComboBox<ItemCatalogo<T>> caixa() {
        return caixa;
    }
}
