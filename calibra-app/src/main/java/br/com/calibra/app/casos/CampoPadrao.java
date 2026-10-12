package br.com.calibra.app.casos;

/**
 * Campos da tela de Configurações com os padrões de cálculo (SPEC 7.1) e os limiares do semáforo (SPEC 7.4),
 * com o nome usado nas mensagens e as casas decimais mostradas.
 */
public enum CampoPadrao {
    // Padrões de cálculo
    K_AR("o k_ar", 1, 3),
    DC_MAX_PCT("o DC máximo", 0, 2),
    ETA_C("a eficiência do compressor", 2, 3),
    EPS("a eficiência do intercooler", 2, 3),
    PERDA_DESCARGA_PSI("a perda depois do compressor", 1, 2),
    PERDA_ADMISSAO_PSI("a perda antes do compressor", 1, 2),
    ACRESCIMO_TM("o acréscimo de temperatura do aspirado", 0, 1),
    DENSIDADE_TESTE("a densidade padrão do fluido de teste", 2, 3),
    TI_MINIMO("o TI mínimo na marcha lenta", 1, 2),
    // Limiares do semáforo
    DC_VERDE_PCT("o limite do verde do DC do bico", 0, 1),
    DC_AMARELO_PCT("o limite do amarelo do DC do bico", 0, 1),
    FOLGA_VERDE_PCT("o limite do verde da folga da bomba", 0, 1),
    FOLGA_AMARELO_PCT("o limite do amarelo da folga da bomba", 0, 1),
    PISTAO_VERDE("o limite do verde da velocidade do pistão", 0, 1),
    PISTAO_AMARELO("o limite do amarelo da velocidade do pistão", 0, 1),
    SENSOR_VERDE("o limite do verde da faixa do sensor MAP", 1, 2),
    SENSOR_AMARELO("o limite do amarelo da faixa do sensor MAP", 1, 2),
    ASPIRADO_VERDE("o limite do verde do aspirado", 2, 3),
    ASPIRADO_AMARELO("o limite do amarelo do aspirado", 2, 3),
    BMEP_ASPIRADO("a BMEP de referência do aspirado", 0, 1),
    BMEP_SOBREALIMENTADO("a BMEP de referência do sobrealimentado", 0, 1),
    BMEP_AMARELO_PCT("o amarelo da BMEP", 0, 1);

    private final String nome;
    private final int minimoCasas;
    private final int maximoCasas;

    CampoPadrao(String nome, int minimoCasas, int maximoCasas) {
        this.nome = nome;
        this.minimoCasas = minimoCasas;
        this.maximoCasas = maximoCasas;
    }

    /** Nome com artigo, para mensagens como "Informe o k_ar". */
    public String nome() {
        return nome;
    }

    int minimoCasas() {
        return minimoCasas;
    }

    int maximoCasas() {
        return maximoCasas;
    }
}
