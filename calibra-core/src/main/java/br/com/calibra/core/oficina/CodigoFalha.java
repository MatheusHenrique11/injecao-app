package br.com.calibra.core.oficina;

/** Um código de falha lido no diagnóstico (SPEC 6.6.3): o código, como saiu no scanner, a descrição e a situação. */
public record CodigoFalha(String codigo, String descricao, SituacaoCodigo situacao) {

    public CodigoFalha {
        codigo = codigo == null ? "" : codigo.strip();
        descricao = descricao == null ? "" : descricao.strip();
    }
}
