package br.com.calibra.relatorios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.util.NumeroBr;

/**
 * Os textos do orçamento que o PDF e o WhatsApp dividem: quantidade, desconto, prazo, validade e a nota do mapa
 * base. Mesma frase nos dois lugares.
 */
public final class TextoOrcamento {

    static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** A marca da peça que o cliente traz (SPEC 6.6.4, Fase 3B), no PDF e no WhatsApp. */
    static final String FORNECIDO = "fornecido pelo cliente";

    /** A nota do bloco 2.5, no PDF e no WhatsApp. */
    public static final String NOTA_MAPA_BASE = "O mapa base sai conservador, para o motor pegar com segurança; o acerto "
            + "final é feito no dinamômetro (ou na rua ou na água), com wideband.";

    private TextoOrcamento() {
    }

    /** "4", "2,5" ou "0,333". */
    public static String quantidade(BigDecimal quantidade) {
        return NumeroBr.formatar(quantidade.doubleValue(), 0, 3);
    }

    /** "4 un", "2,5 h" (sem unidade, só o número). */
    public static String quantidadeComUnidade(ItemOrcamento item) {
        return quantidade(item.quantidade()) + (item.unidade().isEmpty() ? "" : " " + item.unidade());
    }

    /** "Desconto (10%)" ou "Desconto". */
    public static String rotuloDesconto(Desconto desconto) {
        return desconto.percentual() == null ? "Desconto"
                : "Desconto (" + NumeroBr.formatar(desconto.percentual().doubleValue(), 0, 2) + "%)";
    }

    /** "10 dias úteis a partir da aprovação" (ou "a combinar" com prazo zero e sem observação). */
    public static String prazo(Orcamento orcamento) {
        String dias = orcamento.prazoDiasUteis() == 1 ? "1 dia útil" : orcamento.prazoDiasUteis() + " dias úteis";
        if (orcamento.prazoDiasUteis() == 0) {
            return orcamento.prazoObservacao().isEmpty() ? "a combinar" : orcamento.prazoObservacao();
        }
        return dias + (orcamento.prazoObservacao().isEmpty() ? "" : " " + orcamento.prazoObservacao());
    }

    /** "até 28/10/2026 (15 dias)". */
    public static String validade(Orcamento orcamento, LocalDate emissao) {
        return "até " + orcamento.validoAte(emissao).format(DIA) + " (" + orcamento.validadeDias()
                + (orcamento.validadeDias() == 1 ? " dia)" : " dias)");
    }

    /**
     * As linhas da tabela de itens: descrição, quantidade, unidade, valor unitário e total. A peça do cliente sai
     * marcada na descrição e com R$ 0,00 (Fase 3B).
     */
    static List<List<String>> linhas(List<ItemOrcamento> itens) {
        List<List<String>> linhas = new ArrayList<>();
        for (ItemOrcamento item : itens) {
            linhas.add(List.of(descricao(item), quantidade(item.quantidade()), item.unidade(),
                    item.valorUnitario().formatar(), item.total().formatar()));
        }
        return linhas;
    }

    /** "ECU FuelTech FT450 (fornecido pelo cliente)" na peça do cliente; nas outras, a descrição como está. */
    static String descricao(ItemOrcamento item) {
        return item.fornecidoPeloCliente() ? item.descricao() + " (" + FORNECIDO + ")" : item.descricao();
    }
}
