package br.com.calibra.core.oficina;

import java.util.ArrayList;
import java.util.List;

/** A vistoria da entrada: os itens conferidos e os objetos que ficaram no veículo. */
public record Vistoria(List<ItemVistoria> itens, String objetos) {

    public static final Vistoria VAZIA = new Vistoria(List.of(), "");

    public Vistoria {
        itens = itens == null ? List.of() : List.copyOf(itens);
        objetos = objetos == null ? "" : objetos.strip();
    }

    /** Tudo o que está errado, em português; vazio = pode gravar. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i).item().isEmpty()) {
                erros.add("Item " + (i + 1) + " da vistoria sem nome.");
            }
        }
        return erros;
    }
}
