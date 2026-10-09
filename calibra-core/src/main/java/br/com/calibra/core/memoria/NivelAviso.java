package br.com.calibra.core.memoria;

/** Gravidade de um aviso. Erros que bloqueiam o cálculo não são avisos: viram exceção. */
public enum NivelAviso {
    INFORMACAO("Informação"),
    AMARELO("Amarelo"),
    VERMELHO("Vermelho");

    private final String rotulo;

    NivelAviso(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
