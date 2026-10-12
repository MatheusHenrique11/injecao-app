package br.com.calibra.dados.cadastro;

import java.time.LocalDateTime;

/**
 * Uma linha do histórico de um cadastro (SPEC 6.2): o campo, o valor antigo, o novo e quando mudou.
 *
 * @param valorAntigo null na criação do cadastro
 */
public record EventoAuditoria(String campo, String valorAntigo, String valorNovo, LocalDateTime dataHora) {
}
