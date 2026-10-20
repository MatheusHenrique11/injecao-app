package br.com.calibra.core.oficina;

/** Como o diagnóstico terminou (SPEC 6.6.3, Fase 3B). Sem resultado, o laudo está "a definir". */
public enum ResultadoLaudo {
    RESOLVIDO("resolvido"),
    PARCIAL("resolvido em parte"),
    NAO_RESOLVIDO("não resolvido");

    private final String rotulo;

    ResultadoLaudo(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
