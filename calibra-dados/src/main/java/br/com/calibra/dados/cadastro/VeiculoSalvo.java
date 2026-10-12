package br.com.calibra.dados.cadastro;

import java.time.LocalDateTime;

import br.com.calibra.core.cadastro.Veiculo;

/** Um veículo gravado no banco, com o dono. Arquivado sai das listas, mas os projetos continuam. */
public record VeiculoSalvo(long id, long clienteId, Veiculo veiculo, boolean arquivado, LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
