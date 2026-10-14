package br.com.calibra.dados.orcamento;

import java.time.LocalDateTime;

import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.NumeroOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.StatusOrcamento;

/**
 * Uma revisão de orçamento gravada.
 *
 * @param enviadoEm quando foi marcado como enviado; null no rascunho (a validade conta a partir daí)
 * @param respondidoEm data e hora da aprovação ou da recusa, como o usuário informou; null antes da resposta
 * @param meioResposta como o cliente respondeu (pessoalmente, WhatsApp ou telefone); null antes da resposta
 */
public record OrcamentoSalvo(
        long id,
        long projetoId,
        long calculoId,
        long motorVersaoId,
        NumeroOrcamento numero,
        StatusOrcamento status,
        Orcamento conteudo,
        LocalDateTime enviadoEm,
        LocalDateTime respondidoEm,
        MeioAprovacao meioResposta,
        String observacaoResposta,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
