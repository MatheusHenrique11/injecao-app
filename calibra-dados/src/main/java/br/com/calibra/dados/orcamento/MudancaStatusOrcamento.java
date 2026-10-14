package br.com.calibra.dados.orcamento;

import java.time.LocalDateTime;

import br.com.calibra.core.orcamento.StatusOrcamento;

/** Uma linha do histórico de status de um orçamento. */
public record MudancaStatusOrcamento(StatusOrcamento status, LocalDateTime dataHora) {
}
