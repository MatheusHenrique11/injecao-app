package br.com.calibra.core.calculo;

/** Como a vazão de ar é estimada: pelo fator k_ar (padrão da SPEC 7.1) ou pelo BSFC (exemplo da Garrett). */
public enum CaminhoAr {
    K_AR("k_ar (hp por lb/min)"),
    BSFC("BSFC (lb/hp·h)");

    private final String rotulo;

    CaminhoAr(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
