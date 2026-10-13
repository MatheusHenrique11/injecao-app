package br.com.calibra.dados.cadastro;

import java.time.LocalDateTime;

import br.com.calibra.core.cadastro.Veiculo;

/**
 * Um veículo gravado no banco, com o dono. Arquivado sai das listas, mas os projetos continuam.
 *
 * @param cambioItemId câmbio escolhido do catálogo (SPEC 6.2, bloco 2.3); null se não houver
 */
public record VeiculoSalvo(long id, long clienteId, Veiculo veiculo, Long cambioItemId, boolean arquivado,
        LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
}
