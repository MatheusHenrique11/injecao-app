package br.com.calibra.core.oficina;

/** Em que condição a medição do laudo elétrico foi feita (SPEC 6.6.3, Fase 3B). */
public enum CondicaoMedicao {
    REPOUSO("repouso"),
    MOTOR_LIGADO("motor ligado"),
    CARGA_ACIONADA("carga acionada"),
    OUTRA("outra");

    private final String rotulo;

    CondicaoMedicao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
