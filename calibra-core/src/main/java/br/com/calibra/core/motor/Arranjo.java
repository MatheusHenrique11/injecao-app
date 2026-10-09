package br.com.calibra.core.motor;

/** Arranjo dos cilindros (SPEC 6.3). */
public enum Arranjo {
    EM_LINHA("Em linha"),
    V("V"),
    BOXER("Boxer"),
    VR_W("VR / W");

    private final String rotulo;

    Arranjo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** V e VR/W exigem o ângulo entre bancadas. */
    public boolean exigeAnguloEntreBancadas() {
        return this == V || this == VR_W;
    }
}
