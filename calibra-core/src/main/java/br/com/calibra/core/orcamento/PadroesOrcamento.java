package br.com.calibra.core.orcamento;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * O que um orçamento novo traz preenchido (Configurações): desconto, prazo, validade, condições de pagamento e o
 * valor da hora de mão de obra. É a política comercial da oficina; tudo se muda em cada orçamento.
 *
 * @param valorHora valor da hora de mão de obra; null enquanto a oficina não informar
 */
public record PadroesOrcamento(Desconto desconto, int prazoDiasUteis, String prazoObservacao, int validadeDias,
        String condicoes, Dinheiro valorHora) {

    public PadroesOrcamento {
        desconto = desconto == null ? Desconto.NENHUM : desconto;
        prazoObservacao = prazoObservacao == null ? "" : prazoObservacao.strip();
        condicoes = condicoes == null ? "" : condicoes.strip();
    }

    /** Sem desconto, 10 dias úteis depois da aprovação, validade de 15 dias, sem condições nem valor da hora. */
    public static PadroesOrcamento padrao() {
        return new PadroesOrcamento(Desconto.NENHUM, 10, "a partir da aprovação e da chegada das peças", 15, "", null);
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (prazoDiasUteis < 0) {
            erros.add("O prazo padrão não pode ser negativo.");
        }
        if (validadeDias < 1) {
            erros.add("A validade padrão é de pelo menos 1 dia.");
        }
        if (valorHora != null && valorHora.negativo()) {
            erros.add("O valor da hora não pode ser negativo.");
        }
        if (desconto.percentual() != null && (desconto.percentual().signum() < 0
                || desconto.percentual().compareTo(BigDecimal.valueOf(100)) > 0)) {
            erros.add("O desconto padrão em percentual fica entre 0 e 100%.");
        }
        if (desconto.valor() != null && desconto.valor().negativo()) {
            erros.add("O desconto padrão não pode ser negativo.");
        }
        return erros;
    }

    /** Um orçamento novo com estes padrões e os itens dados. */
    public Orcamento orcamento(List<ItemOrcamento> itens) {
        return new Orcamento(itens, desconto, prazoDiasUteis, prazoObservacao, validadeDias, condicoes, "");
    }
}
