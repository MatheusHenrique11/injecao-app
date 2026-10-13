package br.com.calibra.core.semaforo;

/** Os indicadores do semáforo da SPEC 7.4. */
public enum Indicador {
    DC_BICO("DC do bico na potência máxima"),
    FOLGA_BOMBA("Folga da bomba"),
    MAPA_COMPRESSOR("Ponto no mapa do compressor"),
    BMEP("BMEP"),
    VELOCIDADE_PISTAO("Velocidade média do pistão"),
    SAIDAS_INJECAO("Saídas da injeção"),
    FAIXA_SENSOR_MAP("Faixa do sensor MAP"),
    ASPIRADO_MAP_PATM("Aspirado: MAP necessário ÷ P_atm local"),
    TORQUE_CAMBIO("Torque × câmbio");

    private final String rotulo;

    Indicador(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
