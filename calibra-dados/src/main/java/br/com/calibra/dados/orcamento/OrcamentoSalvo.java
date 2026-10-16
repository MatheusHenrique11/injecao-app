package br.com.calibra.dados.orcamento;

import java.time.LocalDateTime;

import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.NumeroOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.StatusOrcamento;

/**
 * Uma revisão de orçamento gravada. O orçamento pertence a uma OS (SPEC 6.5).
 *
 * @param calculo o cálculo de preparação que o orçamento usou; null na OS comum
 * @param enviadoEm quando foi marcado como enviado; null no rascunho (a validade conta a partir daí)
 * @param respondidoEm data e hora da aprovação ou da recusa, como o usuário informou; null antes da resposta
 * @param meioResposta como o cliente respondeu (pessoalmente, WhatsApp ou telefone); null antes da resposta
 */
public record OrcamentoSalvo(
        long id,
        long osId,
        CalculoDoOrcamento calculo,
        NumeroOrcamento numero,
        StatusOrcamento status,
        Orcamento conteudo,
        LocalDateTime enviadoEm,
        LocalDateTime respondidoEm,
        MeioAprovacao meioResposta,
        String observacaoResposta,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    /** Orçamento de preparação (aponta para o cálculo de um projeto). */
    public boolean dePreparacao() {
        return calculo != null;
    }
}
