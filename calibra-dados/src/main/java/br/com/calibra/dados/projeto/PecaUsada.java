package br.com.calibra.dados.projeto;

/**
 * Uma peça do catálogo usada num cálculo salvo, na versão daquele momento (regra 2.5). Se o item ganhar versão
 * nova, o cálculo aparece como desatualizado.
 */
public record PecaUsada(PapelPeca papel, long itemId, int versao) {
}
