package br.com.calibra.dados.oficina;

import java.time.LocalDateTime;

import br.com.calibra.core.oficina.Pagamento;

/**
 * Um recebimento gravado. Cancelado não se apaga: fica com a data e o motivo, e deixa de contar.
 *
 * @param canceladoEm null = vale
 */
public record PagamentoSalvo(long id, long osId, Pagamento pagamento, LocalDateTime canceladoEm,
        String motivoCancelamento, LocalDateTime criadoEm) {

    public boolean cancelado() {
        return canceladoEm != null;
    }
}
