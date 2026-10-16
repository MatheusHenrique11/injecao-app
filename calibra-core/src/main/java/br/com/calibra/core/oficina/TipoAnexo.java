package br.com.calibra.core.oficina;

/**
 * O que é o anexo da OS ou do veículo (SPEC 6.5). Documento (CNH, CRLV) costuma ter dados pessoais: vem marcado como
 * pessoal e sai na anonimização do cliente (LGPD). A nota fiscal fica, porque a oficina precisa guardá-la.
 */
public enum TipoAnexo {
    FOTO_ENTRADA("Foto da entrada", false),
    FOTO_SERVICO("Foto do serviço", false),
    DOCUMENTO("Documento", true),
    NOTA_FISCAL("Nota fiscal", false),
    LAUDO("Laudo", false),
    OUTRO("Outro", false);

    private final String rotulo;
    private final boolean pessoalPorPadrao;

    TipoAnexo(String rotulo, boolean pessoalPorPadrao) {
        this.rotulo = rotulo;
        this.pessoalPorPadrao = pessoalPorPadrao;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Se o anexo deste tipo começa marcado como documento pessoal (a pessoa pode mudar). */
    public boolean pessoalPorPadrao() {
        return pessoalPorPadrao;
    }
}
