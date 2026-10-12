package br.com.calibra.dados.projeto;

import java.time.LocalDateTime;

import br.com.calibra.core.projeto.StatusProjeto;

/** Uma linha do histórico de status do projeto. */
public record MudancaStatus(StatusProjeto status, LocalDateTime dataHora) {
}
