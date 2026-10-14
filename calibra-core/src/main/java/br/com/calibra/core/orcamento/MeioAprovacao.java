package br.com.calibra.core.orcamento;

/** Como o cliente aprovou (ou recusou) o orçamento: fica registrado com a data e a hora. */
public enum MeioAprovacao {
    PESSOALMENTE("Pessoalmente"),
    WHATSAPP("WhatsApp"),
    TELEFONE("Telefone");

    private final String rotulo;

    MeioAprovacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
