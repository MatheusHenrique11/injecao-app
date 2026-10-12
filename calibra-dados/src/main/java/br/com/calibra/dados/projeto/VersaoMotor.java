package br.com.calibra.dados.projeto;

import java.time.LocalDateTime;

import br.com.calibra.core.motor.ConfiguracaoMotor;

/**
 * Uma versão imutável do motor do projeto (SPEC 6.3): v1, v2...
 *
 * @param descricao nome do motor para a pessoa reconhecer (ex.: "AP 2.0 8V"); pode ser vazio
 */
public record VersaoMotor(long id, long projetoId, int numero, String descricao, ConfiguracaoMotor configuracao,
        LocalDateTime criadaEm) {
}
