package br.com.calibra.core.semaforo;

/** Cor do semáforo (SPEC 7.4). Vermelho só avança com justificativa escrita (regra 2.4). */
public enum Cor {
    VERDE("Verde"),
    AMARELO("Amarelo"),
    VERMELHO("Vermelho");

    private final String rotulo;

    Cor(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
