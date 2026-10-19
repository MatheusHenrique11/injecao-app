package br.com.calibra.core.oficina;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Os padrões das OS em Configurações (bloco 3.3): a garantia que vem preenchida na entrega, os itens da vistoria da
 * entrada e o texto acima das assinaturas na via do cliente. Os valores de fábrica foram escolhidos pelo dono da
 * oficina em 17/10/2026.
 *
 * @param garantiaDias 0 = sem garantia
 * @param itensVistoria na ordem em que aparecem na vistoria
 */
public record PadroesOs(int garantiaDias, List<String> itensVistoria, String textoVia) {

    /** O texto da via do cliente que vem de fábrica (o dono muda em Configurações). */
    public static final String TEXTO_VIA_PADRAO = "Autorizo a oficina a fazer o orçamento do serviço pedido. O estado "
            + "do veículo na chegada (km, tanque, vistoria e objetos deixados) está anotado acima. O serviço só começa "
            + "depois que eu aprovar o orçamento.";

    public static final int MAXIMO_DIAS = Garantia.MAXIMO_DIAS;
    public static final int MAXIMO_ITENS = 20;
    public static final int MAXIMO_LETRAS_ITEM = 60;
    public static final int MAXIMO_LETRAS_TEXTO = 600;

    public PadroesOs {
        itensVistoria = itensVistoria == null ? List.of()
                : itensVistoria.stream().map(item -> item == null ? "" : item.strip()).toList();
        textoVia = textoVia == null ? "" : textoVia.strip();
    }

    /** Os padrões de fábrica: 90 dias de garantia, os 4 itens da vistoria e o texto da via. */
    public static PadroesOs padrao() {
        return new PadroesOs(90, Vistoria.ITENS_PADRAO, TEXTO_VIA_PADRAO);
    }

    /** Tudo o que está errado, em português; vazio = pode gravar. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (garantiaDias < 0 || garantiaDias > MAXIMO_DIAS) {
            erros.add("A garantia padrão vai de 0 (sem garantia) a " + MAXIMO_DIAS + " dias.");
        }
        if (itensVistoria.isEmpty()) {
            erros.add("A vistoria precisa de pelo menos um item.");
        } else if (itensVistoria.size() > MAXIMO_ITENS) {
            erros.add("A vistoria tem no máximo " + MAXIMO_ITENS + " itens.");
        }
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < itensVistoria.size(); i++) {
            String item = itensVistoria.get(i);
            if (item.isEmpty()) {
                erros.add("O item " + (i + 1) + " da vistoria está sem nome.");
            } else if (item.length() > MAXIMO_LETRAS_ITEM) {
                erros.add("O item \"" + item + "\" passa de " + MAXIMO_LETRAS_ITEM + " letras.");
            } else if (!vistos.add(item.toLowerCase(Locale.ROOT))) {
                erros.add("O item \"" + item + "\" aparece duas vezes na vistoria.");
            }
        }
        if (textoVia.length() > MAXIMO_LETRAS_TEXTO) {
            erros.add("O texto da via do cliente passa de " + MAXIMO_LETRAS_TEXTO + " letras.");
        }
        return erros;
    }
}
