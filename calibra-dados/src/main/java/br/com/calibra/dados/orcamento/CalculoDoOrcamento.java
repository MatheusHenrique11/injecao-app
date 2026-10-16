package br.com.calibra.dados.orcamento;

/**
 * O cálculo que o orçamento de preparação usou: o projeto, o cálculo salvo e a versão do motor. O orçamento de uma
 * OS comum não tem cálculo (SPEC 6.5).
 */
public record CalculoDoOrcamento(long projetoId, long calculoId, long motorVersaoId) {
}
