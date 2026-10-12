package br.com.calibra.app;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 * Os campos de um formulário, lidos e escritos como texto, no formato do {@link LeitorFormulario}: número
 * como digitado, escolha pelo nome da constante, caixa de marcar como "true"/"false".
 */
final class CamposTela {

    private record CampoTela(Supplier<String> ler, Consumer<String> escrever) {
    }

    private final Map<Campo, CampoTela> campos = new EnumMap<>(Campo.class);
    private final List<Runnable> aoMudar = new ArrayList<>();

    void registrar(Campo campo, TextField texto) {
        campos.put(campo, new CampoTela(texto::getText, texto::setText));
        texto.textProperty().addListener((obs, antes, depois) -> mudou());
    }

    void registrar(Campo campo, CheckBox caixa) {
        campos.put(campo, new CampoTela(() -> String.valueOf(caixa.isSelected()),
                valor -> caixa.setSelected(Boolean.parseBoolean(valor))));
        caixa.selectedProperty().addListener((obs, antes, depois) -> mudou());
    }

    <T> void registrar(Campo campo, ComboBox<T> combo, Function<T, String> chave) {
        campos.put(campo, new CampoTela(
                () -> combo.getValue() == null ? "" : chave.apply(combo.getValue()),
                valor -> combo.getItems().stream()
                        .filter(item -> chave.apply(item).equals(valor))
                        .findFirst()
                        .ifPresentOrElse(combo::setValue, () -> combo.setValue(null))));
        combo.valueProperty().addListener((obs, antes, depois) -> mudou());
    }

    /** Avisa quando qualquer campo muda. */
    void aoMudar(Runnable acao) {
        aoMudar.add(acao);
    }

    /** Texto de todos os campos. */
    Map<Campo, String> valores() {
        Map<Campo, String> valores = new EnumMap<>(Campo.class);
        campos.forEach((campo, tela) -> valores.put(campo, tela.ler().get()));
        return valores;
    }

    String ler(Campo campo) {
        return campos.get(campo).ler().get();
    }

    void escrever(Campo campo, String valor) {
        campos.get(campo).escrever().accept(valor);
    }

    /** Escreve os campos deste formulário que vierem no mapa, na ordem do {@link Campo}; os outros ficam. */
    void preencher(Map<Campo, String> valores) {
        new EnumMap<>(valores).forEach((campo, valor) -> {
            CampoTela tela = campos.get(campo);
            if (tela != null) {
                tela.escrever().accept(valor);
            }
        });
    }

    private void mudou() {
        aoMudar.forEach(Runnable::run);
    }

    /** Escolhas de uma caixa, mostradas pelo rótulo em português. */
    static <T> void configurar(ComboBox<T> combo, List<T> itens, Function<T, String> rotulo) {
        combo.getItems().setAll(itens);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(T item) {
                return item == null ? "" : rotulo.apply(item);
            }

            @Override
            public T fromString(String texto) {
                return null;
            }
        });
    }
}
