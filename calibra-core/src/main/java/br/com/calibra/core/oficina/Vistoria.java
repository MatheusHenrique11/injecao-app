package br.com.calibra.core.oficina;

import java.util.ArrayList;
import java.util.List;

/** A vistoria da entrada: os itens conferidos e os objetos que ficaram no veículo. */
public record Vistoria(List<ItemVistoria> itens, String objetos) {

    public static final Vistoria VAZIA = new Vistoria(List.of(), "");

    /**
     * Os itens conferidos na entrada, escolhidos pelo dono da oficina em 15/10/2026 (o tanque e os objetos deixados
     * têm campo próprio). No bloco 3.3, a lista passa a ser editável em Configurações.
     */
    public static final List<String> ITENS_PADRAO = List.of("Lataria e pintura", "Vidros, faróis e retrovisores",
            "Pneus, rodas e estepe", "Interior e painel");

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
