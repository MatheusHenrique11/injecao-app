package br.com.calibra.core.orcamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * O conteúdo de um orçamento (SPEC 1 e 12, fase 2): peças e serviços, desconto, prazo, validade, condições e
 * observações. Os totais saem dos itens, em centavos. O número, o status e a aprovação ficam no orçamento gravado.
 *
 * @param prazoDiasUteis prazo de entrega do serviço, em dias úteis (o texto da observação diz a partir de quando)
 * @param validadeDias por quantos dias o orçamento vale, a partir da emissão
 */
public record Orcamento(List<ItemOrcamento> itens, Desconto desconto, int prazoDiasUteis, String prazoObservacao,
        int validadeDias, String condicoes, String observacoes) {

    public Orcamento {
        itens = itens == null ? List.of() : List.copyOf(itens);
        desconto = desconto == null ? Desconto.NENHUM : desconto;
        prazoObservacao = prazoObservacao == null ? "" : prazoObservacao.strip();
        condicoes = condicoes == null ? "" : condicoes.strip();
        observacoes = observacoes == null ? "" : observacoes.strip();
    }

    public List<ItemOrcamento> pecas() {
        return itens.stream().filter(item -> item.tipo() == TipoItem.PECA).toList();
    }

    public List<ItemOrcamento> servicos() {
        return itens.stream().filter(item -> item.tipo() == TipoItem.SERVICO).toList();
    }

    public Dinheiro totalPecas() {
        return somar(pecas());
    }

    public Dinheiro totalServicos() {
        return somar(servicos());
    }

    /** Peças + serviços, antes do desconto. */
    public Dinheiro subtotal() {
        return totalPecas().mais(totalServicos());
    }

    public Dinheiro valorDesconto() {
        return desconto.sobre(subtotal());
    }

    public Dinheiro total() {
        return subtotal().menos(valorDesconto());
    }

    /** Até quando o orçamento vale (dias corridos a partir da emissão). */
    public LocalDate validoAte(LocalDate emissao) {
        return emissao.plusDays(validadeDias);
    }

    private static Dinheiro somar(List<ItemOrcamento> itens) {
        Dinheiro soma = Dinheiro.ZERO;
        for (ItemOrcamento item : itens) {
            soma = soma.mais(item.total());
        }
        return soma;
    }

    /** Tudo o que falta ou está errado, em português; vazio = pode gravar. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (itens.isEmpty()) {
            erros.add("O orçamento precisa de pelo menos um item (peça ou serviço).");
        }
        for (int i = 0; i < itens.size(); i++) {
            erros.addAll(itens.get(i).validar(i + 1));
        }
        if (!erros.isEmpty()) {
            return erros;
        }
        if (desconto.valor() != null && desconto.valor().negativo()) {
            erros.add("O desconto não pode ser negativo.");
        }
        if (desconto.percentual() != null && (desconto.percentual().signum() < 0
                || desconto.percentual().compareTo(BigDecimal.valueOf(100)) > 0)) {
            erros.add("O desconto em percentual fica entre 0 e 100%.");
        }
        if (erros.isEmpty() && valorDesconto().compareTo(subtotal()) > 0) {
            erros.add("O desconto (" + valorDesconto().formatar() + ") é maior que o total de peças e serviços ("
                    + subtotal().formatar() + ").");
        }
        if (prazoDiasUteis < 0) {
            erros.add("O prazo não pode ser negativo.");
        }
        if (validadeDias < 1) {
            erros.add("A validade do orçamento é de pelo menos 1 dia.");
        }
        return erros;
    }
}
