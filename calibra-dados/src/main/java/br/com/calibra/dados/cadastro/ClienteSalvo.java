package br.com.calibra.dados.cadastro;

import java.time.LocalDateTime;

import br.com.calibra.core.cadastro.Cliente;

/** Um cliente gravado no banco. */
public record ClienteSalvo(long id, Cliente cliente, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
}
