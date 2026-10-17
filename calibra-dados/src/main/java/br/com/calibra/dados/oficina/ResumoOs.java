package br.com.calibra.dados.oficina;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.StatusOs;

/**
 * Uma linha da lista de OS: o que aparece na tela Oficina e no Painel.
 *
 * @param veiculo marca, modelo e versão ("VW Gol GTi")
 * @param identificacao a placa nos terrestres, o nome nos náuticos
 * @param entradaEm null = o veículo ainda não chegou
 */
public record ResumoOs(
        long osId,
        NumeroOs numero,
        StatusOs status,
        String cliente,
        String veiculo,
        String identificacao,
        String servicoPedido,
        Long projetoId,
        LocalDateTime entradaEm,
        LocalDate previsaoEntrega,
        LocalDateTime atualizadoEm) {

    /**
     * A previsão de entrega já passou e o serviço não ficou pronto. A OS pronta não conta: ela espera o cliente
     * buscar, não a oficina.
     */
    public boolean atrasada(LocalDate hoje) {
        return previsaoEntrega != null && previsaoEntrega.isBefore(hoje) && status != StatusOs.PRONTA
                && !status.finalizada();
    }
}
