package br.com.calibra.core.orcamento;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Uma linha do orçamento: peça ou serviço, com quantidade, unidade e valor unitário em centavos.
 *
 * @param quantidade exata (ex.: 2,5 horas); maior que zero
 * @param unidade texto livre (ex.: "un", "h", "jogo"); pode ficar vazio
 * @param catalogoItemId de onde a peça veio no catálogo (o preço é o de referência dele); null se digitada
 * @param catalogoVersao a versão do item do catálogo; null se digitada
 */
public record ItemOrcamento(TipoItem tipo, String descricao, BigDecimal quantidade, String unidade,
        Dinheiro valorUnitario, Long catalogoItemId, Integer catalogoVersao) {

    public ItemOrcamento {
        Objects.requireNonNull(tipo, "tipo");
        descricao = descricao == null ? "" : descricao.strip();
        unidade = unidade == null ? "" : unidade.strip();
        quantidade = quantidade == null ? null : quantidade.stripTrailingZeros();
        if (quantidade != null && quantidade.scale() < 0) {
            quantidade = quantidade.setScale(0);
        }
    }

    /** Peça ou serviço digitado (sem origem no catálogo). */
    public static ItemOrcamento digitado(TipoItem tipo, String descricao, BigDecimal quantidade, String unidade,
            Dinheiro valorUnitario) {
        return new ItemOrcamento(tipo, descricao, quantidade, unidade, valorUnitario, null, null);
    }

    /** Quantidade × valor unitário, arredondado no centavo. */
    public Dinheiro total() {
        return valorUnitario.vezes(quantidade);
    }

    /**
     * O que falta ou está errado nesta linha.
     *
     * @param linha o número da linha na tela (1, 2...), para a mensagem
     */
    public List<String> validar(int linha) {
        List<String> erros = new ArrayList<>();
        String onde = "Item " + linha + (descricao.isEmpty() ? "" : " (" + descricao + ")") + ": ";
        if (descricao.isEmpty()) {
            erros.add(onde + "escreva a descrição.");
        }
        if (quantidade == null || quantidade.signum() <= 0) {
            erros.add(onde + "a quantidade deve ser maior que zero.");
        }
        if (valorUnitario == null) {
            erros.add(onde + "informe o valor.");
        } else if (valorUnitario.negativo()) {
            erros.add(onde + "o valor não pode ser negativo (use o desconto).");
        }
        return erros;
    }
}
