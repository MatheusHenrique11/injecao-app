package br.com.calibra.app;

/** Os campos da calculadora, com o nome que aparece nas mensagens de erro. */
public enum Campo {
    // Motor
    CILINDROS("o número de cilindros"),
    ARRANJO("o arranjo"),
    ANGULO_BANCADAS("o ângulo entre bancadas"),
    DIAMETRO("o diâmetro"),
    CURSO("o curso"),
    CILINDRADA_INFORMADA("a cilindrada informada"),
    TAXA("a taxa de compressão"),
    VALVULAS("as válvulas por cilindro"),
    COMANDO("o comando"),
    DURACAO_COMANDO("a duração do comando"),
    CRUZAMENTO("o cruzamento"),
    ASPIRACAO("a aspiração"),
    PISTOES("os pistões"),
    BIELAS("as bielas"),
    ROTACAO_MAXIMA("a rotação máxima"),
    MARCHA_LENTA_RPM("a rotação de marcha lenta"),
    ORDEM_IGNICAO("a ordem de ignição"),
    ANGULOS_PMS("os ângulos dos PMS"),
    BICOS_POR_CILINDRO("os bicos por cilindro"),
    BICOS_EXTRAS("os bicos extras"),
    MODO_INJECAO("o modo de injeção"),
    MODO_IGNICAO("o modo de ignição"),
    DENTES_TOTAIS("os dentes da roda fônica"),
    DENTES_FALTANTES("os dentes faltantes"),
    SENSOR_FASE("o sensor de fase"),
    COMBUSTIVEL("o combustível"),
    SENSOR_ETANOL("o sensor de etanol"),
    ALTITUDE("a altitude da oficina"),
    // Meta
    POTENCIA_CV("a potência meta"),
    PONTO_MEDICAO("onde a potência é medida"),
    PERDA_TRANSMISSAO("a perda de transmissão"),
    RPM_POTENCIA("a rotação de potência máxima"),
    RPM_TORQUE("a rotação de torque máximo"),
    USO("o uso"),
    // Ar e combustível
    CAMINHO_AR("o caminho da vazão de ar"),
    K_AR("o k_ar"),
    BSFC("o BSFC"),
    LAMBDA("a lambda alvo"),
    VE("a VE"),
    // Ambiente e parâmetros
    TEMPERATURA_AMBIENTE("a temperatura ambiente"),
    PERDA_DESCARGA_PSI("a perda depois do compressor"),
    PERDA_ADMISSAO_PSI("a perda antes do compressor"),
    ETA_C("a eficiência do compressor"),
    EPS("a eficiência do intercooler"),
    ACRESCIMO_TM("o acréscimo de temperatura do aspirado"),
    DC_MAX_PCT("o DC máximo"),
    DENSIDADE_TESTE_PADRAO("a densidade padrão do fluido de teste"),
    TI_MINIMO_MARCHA_LENTA("o TI mínimo na marcha lenta"),
    PRESSAO_BASE_BAR("a pressão de base do combustível"),
    // Bico
    BICO_VAZAO("a vazão nominal do bico"),
    BICO_PRESSAO_TESTE_BAR("a pressão de teste do bico"),
    BICO_DENSIDADE_TESTE("a densidade do fluido de teste do bico"),
    BICO_TEMPO_MORTO("o tempo morto do bico"),
    BICO2_VAZAO("a vazão nominal do bico secundário"),
    BICO2_PRESSAO_TESTE_BAR("a pressão de teste do bico secundário"),
    BICO2_DENSIDADE_TESTE("a densidade do fluido de teste do bico secundário"),
    BICO2_TEMPO_MORTO("o tempo morto do bico secundário"),
    // Opcionais
    TENSAO_V("a tensão de trabalho"),
    MARCHA_LENTA_MAP("o MAP de marcha lenta"),
    MARCHA_LENTA_VE("a VE de marcha lenta"),
    BOMBA_VAZAO("a vazão da bomba"),
    SENSOR_MAP_KPA("a faixa do sensor MAP"),
    INJECAO_NOME("o nome da injeção"),
    INJECAO_SAIDAS_BICO("as saídas de bico da injeção"),
    INJECAO_SAIDAS_IGNICAO("as saídas de ignição da injeção");

    private final String nome;

    Campo(String nome) {
        this.nome = nome;
    }

    /** Nome com artigo, para mensagens como "Informe a VE". */
    public String nome() {
        return nome;
    }
}
