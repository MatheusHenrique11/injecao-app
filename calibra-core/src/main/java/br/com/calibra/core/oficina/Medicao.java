package br.com.calibra.core.oficina;

/**
 * Uma medição do laudo elétrico (SPEC 6.6.3): o que se mediu, o valor e a unidade como foram escritos (texto, como no
 * multímetro: "12,1", "sem sinal"), a condição e, se o dono quiser, a referência. O app não traz faixa "boa" ou
 * "ruim" pronta e não julga o valor (decisão 219).
 *
 * @param referencia a faixa de referência escrita pelo dono (ex.: "12,4 a 12,7 V"); vazia = sem referência
 * @param referenciaAVerificar a referência ainda não foi conferida (regra 2.7)
 */
public record Medicao(String nome, String valor, String unidade, CondicaoMedicao condicao, String referencia,
        boolean referenciaAVerificar) {

    public Medicao {
        nome = limpo(nome);
        valor = limpo(valor);
        unidade = limpo(unidade);
        referencia = limpo(referencia);
    }

    /** "12,1 V", ou só o valor sem unidade. */
    public String valorComUnidade() {
        return unidade.isEmpty() ? valor : valor + " " + unidade;
    }

    private static String limpo(String texto) {
        return texto == null ? "" : texto.strip();
    }
}
