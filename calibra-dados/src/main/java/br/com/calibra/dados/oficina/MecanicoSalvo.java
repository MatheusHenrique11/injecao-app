package br.com.calibra.dados.oficina;

/** Um mecânico da oficina: só o nome, para pôr na OS (sem login, SPEC 1). Inativo some das listas de escolha. */
public record MecanicoSalvo(long id, String nome, boolean ativo) {
}
