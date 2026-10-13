package br.com.calibra.dados.catalogo;

/** Nome e versão atual de um item, para conferir se um cálculo salvo ficou desatualizado. */
public record ResumoItem(long id, String tipo, String nome, int versaoAtual, boolean arquivado) {
}
