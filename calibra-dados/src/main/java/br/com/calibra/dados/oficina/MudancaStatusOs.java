package br.com.calibra.dados.oficina;

import java.time.LocalDateTime;

import br.com.calibra.core.oficina.StatusOs;

/** Uma linha do histórico de status da OS; a observação guarda, por exemplo, o motivo do cancelamento. */
public record MudancaStatusOs(StatusOs status, LocalDateTime dataHora, String observacao) {
}
