package br.com.calibra.app;

import java.util.Objects;
import java.util.Optional;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.util.StringConverter;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * A escolha de um item de outro catálogo num campo do formulário (Fase 3B: o checklist do serviço): "(nenhum)" e os
 * itens ativos do tipo, em ordem de nome. O item já escolhido aparece mesmo se estiver arquivado, marcado como tal. O
 * valor é o id do item, como texto (vazio = nenhum).
 */
final class EditorReferencia extends ComboBox<EditorReferencia.Opcao> {

    /** Uma opção da lista: o id (null = nenhum) e o nome que aparece. */
    record Opcao(Long id, String nome) {
    }

    private static final Opcao NENHUM = new Opcao(null, "(nenhum)");

    private final CasosCatalogo casos;
    private final TipoCatalogo<?> tipo;

    EditorReferencia(CasosCatalogo casos, TipoCatalogo<?> tipo, String rotulo) {
        this.casos = Objects.requireNonNull(casos, "a referência ao catálogo precisa do catálogo");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        getItems().add(NENHUM);
        casos.ativos(tipo).forEach(item -> getItems().add(new Opcao(item.id(), item.info().nome())));
        StringConverter<Opcao> conversor = new StringConverter<>() {
            @Override
            public String toString(Opcao opcao) {
                return opcao == null ? NENHUM.nome() : opcao.nome();
            }

            @Override
            public Opcao fromString(String texto) {
                return getItems().stream().filter(o -> o.nome().equals(texto)).findFirst().orElse(NENHUM);
            }
        };
        setConverter(conversor);
        setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Opcao opcao, boolean vazio) {
                super.updateItem(opcao, vazio);
                setText(vazio ? null : conversor.toString(opcao));
            }
        });
        setButtonCell(getCellFactory().call(null));
        setValue(NENHUM);
        setAccessibleText(rotulo);
    }

    /** O id escolhido, como texto; vazio = nenhum. */
    String valor() {
        Opcao escolhida = getValue();
        return escolhida == null || escolhida.id() == null ? "" : escolhida.id().toString();
    }

    /** Escolhe pelo id (texto); vazio ou inválido = nenhum. O que não está na lista entra marcado. */
    void escolher(String texto) {
        Long id = id(texto);
        if (id == null) {
            setValue(NENHUM);
            return;
        }
        Optional<Opcao> naLista = getItems().stream().filter(o -> id.equals(o.id())).findFirst();
        if (naLista.isPresent()) {
            setValue(naLista.get());
            return;
        }
        String nome = casos.buscar(tipo, id)
                .map(item -> item.info().nome() + (item.arquivado() ? " (arquivado)" : ""))
                .orElse(tipo.singular() + " #" + id + " (não está no catálogo)");
        Opcao fora = new Opcao(id, nome);
        getItems().add(fora);
        setValue(fora);
    }

    private static Long id(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(texto.strip());
        } catch (NumberFormatException erro) {
            return null;
        }
    }
}
