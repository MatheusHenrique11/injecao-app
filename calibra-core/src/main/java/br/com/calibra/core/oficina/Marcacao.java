package br.com.calibra.core.oficina;

import java.util.ArrayList;
import java.util.List;

/**
 * A marcação de um item do checklist da OS (SPEC 6.5, Fase 3B): a situação, a observação (o "campo para anotar" do
 * item) e, no "não se aplica", a justificativa, que é obrigatória.
 *
 * @param justificativa só vale no "não se aplica"; nas outras situações, fica vazia
 */
public record Marcacao(SituacaoItem situacao, String observacao, String justificativa) {

    public Marcacao {
        observacao = observacao == null ? "" : observacao.strip();
        justificativa = (justificativa == null || situacao != SituacaoItem.NAO_SE_APLICA) ? "" : justificativa.strip();
    }

    /**
     * O que está errado, em português; vazio = pode gravar.
     *
     * @param item o texto do item, para a mensagem
     */
    public List<String> validar(String item) {
        List<String> erros = new ArrayList<>();
        if (situacao == null) {
            erros.add(item + ": escolha a situação (pendente, feito ou não se aplica).");
        } else if (situacao == SituacaoItem.NAO_SE_APLICA && justificativa.isEmpty()) {
            erros.add(item + ": escreva por que o item não se aplica.");
        }
        return erros;
    }
}
