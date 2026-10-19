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
 * @param catalogoItemId de onde a peça ou o serviço veio no catálogo (o preço é o de referência dele); null se
 *        digitado
 * @param catalogoVersao a versão do item do catálogo; null se digitada
 * @param fornecidoPeloCliente a peça que o cliente traz (SPEC 6.6.4, Fase 3B; ex.: a ECU que ele já comprou): entra
 *        com valor zero e não soma no total. Só peça; os orçamentos gravados antes da Fase 3B são lidos como false
 */
public record ItemOrcamento(TipoItem tipo, String descricao, BigDecimal quantidade, String unidade,
        Dinheiro valorUnitario, Long catalogoItemId, Integer catalogoVersao, boolean fornecidoPeloCliente) {

    public ItemOrcamento {
        Objects.requireNonNull(tipo, "tipo");
        descricao = descricao == null ? "" : descricao.strip();
        unidade = unidade == null ? "" : unidade.strip();
        quantidade = quantidade == null ? null : quantidade.stripTrailingZeros();
        if (quantidade != null && quantidade.scale() < 0) {
            quantidade = quantidade.setScale(0);
        }
        if (fornecidoPeloCliente) {
            valorUnitario = Dinheiro.ZERO;
        }
    }

    /** Peça ou serviço digitado (sem origem no catálogo). */
    public static ItemOrcamento digitado(TipoItem tipo, String descricao, BigDecimal quantidade, String unidade,
            Dinheiro valorUnitario) {
        return new ItemOrcamento(tipo, descricao, quantidade, unidade, valorUnitario, null, null, false);
    }

    /**
     * A linha de um serviço do catálogo de serviços (bloco 3.3, regra escolhida pelo dono): com preço próprio, entra
     * "1 serviço" pelo preço; sem preço, entram as horas × o valor da hora de Configurações. Sem o valor da hora, o
     * valor fica em branco, para a pessoa preencher (como a peça sem preço de referência).
     *
     * @param descricao o texto da linha: a descrição padrão do serviço ou, sem ela, o nome
     *        ({@link ServicoPadrao#textoNoOrcamento()})
     * @param horas o tempo padrão do serviço; null = não informado
     * @param preco o preço de referência do item do catálogo; null = sem preço
     * @param valorHora o valor da hora de mão de obra de Configurações; null = não configurado
     */
    public static ItemOrcamento doServico(String descricao, Double horas, Dinheiro preco, Dinheiro valorHora,
            long itemId, int versao) {
        if (preco != null) {
            return new ItemOrcamento(TipoItem.SERVICO, descricao, BigDecimal.ONE, "serviço", preco, itemId, versao,
                    false);
        }
        if (horas == null) {
            // O catálogo não grava serviço sem preço e sem horas; se vier assim, a pessoa preenche o valor.
            return new ItemOrcamento(TipoItem.SERVICO, descricao, BigDecimal.ONE, "serviço", null, itemId, versao,
                    false);
        }
        return new ItemOrcamento(TipoItem.SERVICO, descricao, BigDecimal.valueOf(horas), "h", valorHora, itemId,
                versao, false);
    }

    /**
     * A linha de um item de material do catálogo (SPEC 6.6.4, Fase 3B): 1 × a unidade dele, pelo preço de referência.
     * Sem preço, o valor fica em branco, para a pessoa preencher.
     *
     * @param preco o preço de referência do item do catálogo; null = sem preço
     */
    public static ItemOrcamento doItem(String descricao, String unidade, Dinheiro preco, long itemId, int versao) {
        return new ItemOrcamento(TipoItem.PECA, descricao, BigDecimal.ONE, unidade, preco, itemId, versao, false);
    }

    /**
     * A mesma linha com a quantidade multiplicada: a quantidade da linha do kit (2 × um serviço de 0,5 h = 1 h; 2 × um
     * serviço com preço = 2 serviços).
     */
    public ItemOrcamento multiplicarQuantidade(BigDecimal fator) {
        Objects.requireNonNull(fator, "fator");
        return new ItemOrcamento(tipo, descricao, quantidade == null ? null : quantidade.multiply(fator), unidade,
                valorUnitario, catalogoItemId, catalogoVersao, fornecidoPeloCliente);
    }

    /** Quantidade × valor unitário, arredondado no centavo; zero na peça fornecida pelo cliente. */
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
        if (fornecidoPeloCliente && tipo != TipoItem.PECA) {
            erros.add(onde + "só peça pode ser fornecida pelo cliente.");
        }
        return erros;
    }
}
