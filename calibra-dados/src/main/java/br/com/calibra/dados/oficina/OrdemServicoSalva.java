package br.com.calibra.dados.oficina;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.StatusOs;

/**
 * Uma ordem de serviço gravada (SPEC 6.5).
 *
 * @param projetoId o projeto de preparação ligado; null na OS comum
 * @param entrada null = o veículo ainda não chegou (orçamento antes do veículo)
 * @param entrega null = não foi entregue
 * @param garantiaAte o último dia da garantia; null antes da entrega
 */
public record OrdemServicoSalva(
        long id,
        NumeroOs numero,
        long clienteId,
        long veiculoId,
        Long projetoId,
        StatusOs status,
        String servicoPedido,
        Long mecanicoId,
        LocalDate previsaoEntrega,
        EntradaVeiculo entrada,
        LocalDateTime prontaEm,
        Entrega entrega,
        LocalDate garantiaAte,
        String motivoCancelamento,
        String observacoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {

    /** O veículo já chegou (a entrada foi registrada). */
    public boolean veiculoChegou() {
        return entrada != null;
    }
}
