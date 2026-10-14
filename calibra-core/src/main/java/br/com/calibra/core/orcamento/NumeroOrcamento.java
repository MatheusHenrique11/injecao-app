package br.com.calibra.core.orcamento;

/**
 * Número do orçamento: ORC-AAAA-NNNN, com a sequência recomeçando a cada ano. As revisões guardam o mesmo número
 * ("ORC-2026-0007 rev. 2"), para o cliente reconhecer o orçamento.
 *
 * @param revisao 1 na primeira
 */
public record NumeroOrcamento(int ano, int sequencial, int revisao) {

    public NumeroOrcamento {
        if (ano < 2000 || sequencial < 1 || revisao < 1) {
            throw new IllegalArgumentException("Número de orçamento inválido: " + ano + "/" + sequencial + "/" + revisao);
        }
    }

    /** "ORC-2026-0007". */
    public String texto() {
        return "ORC-%d-%04d".formatted(ano, sequencial);
    }

    /** "ORC-2026-0007" na primeira; "ORC-2026-0007 rev. 2" nas seguintes. */
    public String comRevisao() {
        return revisao == 1 ? texto() : texto() + " rev. " + revisao;
    }

    /** O mesmo número, na revisão seguinte. */
    public NumeroOrcamento proximaRevisao() {
        return new NumeroOrcamento(ano, sequencial, revisao + 1);
    }
}
