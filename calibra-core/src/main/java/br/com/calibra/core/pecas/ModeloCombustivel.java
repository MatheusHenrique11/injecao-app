package br.com.calibra.core.pecas;

/**
 * Como a injeção calcula o combustível (SPEC 9). Em ms, o mapa base leva o tempo de injeção de cada célula; por VE e
 * no modelo físico, leva a VE, o lambda alvo e os dados do bico (SPEC 7.5).
 */
public enum ModeloCombustivel {
    MS("Tempo de injeção (ms)"),
    VE("Eficiência volumétrica (VE)"),
    MODELO_FISICO("Modelo físico");

    private final String rotulo;

    ModeloCombustivel(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** A tabela de combustível é de VE (o modelo da injeção faz a conta com o bico e o lambda). */
    public boolean porVe() {
        return this != MS;
    }
}
