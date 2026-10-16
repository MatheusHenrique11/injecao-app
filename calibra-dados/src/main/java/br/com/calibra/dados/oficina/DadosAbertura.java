package br.com.calibra.dados.oficina;

import java.time.LocalDate;

/**
 * O que se informa ao abrir uma OS (SPEC 6.5). A entrada do veículo vem à parte, porque pode chegar depois do
 * orçamento.
 *
 * @param projetoId o projeto de preparação ligado; null na OS comum
 * @param mecanicoId null = nenhum mecânico escolhido
 * @param previsaoEntrega null = sem previsão
 */
public record DadosAbertura(long clienteId, long veiculoId, Long projetoId, String servicoPedido, Long mecanicoId,
        LocalDate previsaoEntrega, String observacoes) {

    public DadosAbertura {
        servicoPedido = servicoPedido == null ? "" : servicoPedido.strip();
        observacoes = observacoes == null ? "" : observacoes.strip();
    }
}
