package br.com.calibra.core.semaforo;

/**
 * Limiares do semáforo. Os padrões são os da SPEC 7.4 e podem ser editados (tela de Configurações,
 * fase 2). Frações em vez de percentuais: 0,80 = 80%.
 *
 * <p>Os limiares do compressor e do câmbio entraram no núcleo 1.1.0 (plano da fase 2, "a validar"). Um JSON gravado
 * antes deles chega com zero nesses campos (o Jackson preenche assim) e recebe o padrão.
 *
 * @param dcVerdeAte DC do bico: verde até este valor (0,80)
 * @param dcAmareloAte DC do bico: amarelo até este valor; acima, vermelho (0,85)
 * @param folgaBombaVerde folga da bomba: verde a partir deste valor (0,30)
 * @param folgaBombaAmarelo folga da bomba: amarelo a partir deste valor; abaixo, vermelho (0,15)
 * @param bmepReferenciaAspiradoBar BMEP de referência do aspirado (13 bar)
 * @param bmepReferenciaSobrealimentadoBar BMEP de referência do sobrealimentado (22 bar)
 * @param bmepFatorAmarelo BMEP amarela até este múltiplo da referência (1,15 = 15% acima)
 * @param pistaoVerdeAte velocidade média do pistão: verde até (20 m/s)
 * @param pistaoAmareloAte velocidade média do pistão: amarelo até; acima, vermelho (25 m/s)
 * @param sensorMapVerde faixa do sensor MAP: verde a partir deste múltiplo do MAP máximo (1,2)
 * @param sensorMapAmarelo faixa do sensor MAP: amarelo a partir deste múltiplo; abaixo, vermelho (1,0)
 * @param aspiradoVerdeAte aspirado, MAP necessário ÷ P_atm: verde até (0,90)
 * @param aspiradoAmareloAte aspirado, MAP necessário ÷ P_atm: amarelo até; acima, vermelho (0,98)
 * @param compressorMargemSurge mapa: amarelo com menos que esta folga de vazão sobre o surge, na mesma PR (0,10)
 * @param compressorMargemChoke mapa: amarelo com menos que esta folga de vazão até o choke, na mesma PR (0,05)
 * @param compressorRotacaoFator mapa: amarelo acima desta fração da rotação máxima do turbo (0,95)
 * @param compressorEficienciaPontos mapa: verde até esta queda de eficiência abaixo do pico do mapa (0,05 = 5 pontos)
 * @param cambioVerdeAte torque ÷ torque máximo do câmbio: verde até (0,90)
 * @param cambioAmareloAte torque ÷ torque máximo do câmbio: amarelo até; acima, vermelho (1,00)
 */
public record LimiaresSemaforo(
        double dcVerdeAte,
        double dcAmareloAte,
        double folgaBombaVerde,
        double folgaBombaAmarelo,
        double bmepReferenciaAspiradoBar,
        double bmepReferenciaSobrealimentadoBar,
        double bmepFatorAmarelo,
        double pistaoVerdeAte,
        double pistaoAmareloAte,
        double sensorMapVerde,
        double sensorMapAmarelo,
        double aspiradoVerdeAte,
        double aspiradoAmareloAte,
        double compressorMargemSurge,
        double compressorMargemChoke,
        double compressorRotacaoFator,
        double compressorEficienciaPontos,
        double cambioVerdeAte,
        double cambioAmareloAte) {

    public static final double PADRAO_COMPRESSOR_MARGEM_SURGE = 0.10;
    public static final double PADRAO_COMPRESSOR_MARGEM_CHOKE = 0.05;
    public static final double PADRAO_COMPRESSOR_ROTACAO_FATOR = 0.95;
    public static final double PADRAO_COMPRESSOR_EFICIENCIA_PONTOS = 0.05;
    public static final double PADRAO_CAMBIO_VERDE_ATE = 0.90;
    public static final double PADRAO_CAMBIO_AMARELO_ATE = 1.00;

    public LimiaresSemaforo {
        compressorMargemSurge = positivoOuPadrao(compressorMargemSurge, PADRAO_COMPRESSOR_MARGEM_SURGE);
        compressorMargemChoke = positivoOuPadrao(compressorMargemChoke, PADRAO_COMPRESSOR_MARGEM_CHOKE);
        compressorRotacaoFator = positivoOuPadrao(compressorRotacaoFator, PADRAO_COMPRESSOR_ROTACAO_FATOR);
        compressorEficienciaPontos = positivoOuPadrao(compressorEficienciaPontos, PADRAO_COMPRESSOR_EFICIENCIA_PONTOS);
        cambioVerdeAte = positivoOuPadrao(cambioVerdeAte, PADRAO_CAMBIO_VERDE_ATE);
        cambioAmareloAte = positivoOuPadrao(cambioAmareloAte, PADRAO_CAMBIO_AMARELO_ATE);
        exigirOrdem(dcVerdeAte, dcAmareloAte, "DC do bico");
        exigirOrdem(folgaBombaAmarelo, folgaBombaVerde, "folga da bomba");
        exigirOrdem(1.0, bmepFatorAmarelo, "fator amarelo da BMEP");
        exigirOrdem(pistaoVerdeAte, pistaoAmareloAte, "velocidade do pistão");
        exigirOrdem(sensorMapAmarelo, sensorMapVerde, "faixa do sensor MAP");
        exigirOrdem(aspiradoVerdeAte, aspiradoAmareloAte, "aspirado MAP ÷ P_atm");
        exigirOrdem(cambioVerdeAte, cambioAmareloAte, "torque × câmbio");
        exigirOrdem(compressorRotacaoFator, 1.0, "rotação do turbo");
        if (!(bmepReferenciaAspiradoBar > 0) || !(bmepReferenciaSobrealimentadoBar > 0)) {
            throw new IllegalArgumentException("As BMEP de referência precisam ser maiores que zero.");
        }
    }

    /** Limiares sem os do compressor e do câmbio (como era até o núcleo 1.0.0): esses ficam no padrão. */
    public LimiaresSemaforo(double dcVerdeAte, double dcAmareloAte, double folgaBombaVerde, double folgaBombaAmarelo,
            double bmepReferenciaAspiradoBar, double bmepReferenciaSobrealimentadoBar, double bmepFatorAmarelo,
            double pistaoVerdeAte, double pistaoAmareloAte, double sensorMapVerde, double sensorMapAmarelo,
            double aspiradoVerdeAte, double aspiradoAmareloAte) {
        this(dcVerdeAte, dcAmareloAte, folgaBombaVerde, folgaBombaAmarelo, bmepReferenciaAspiradoBar,
                bmepReferenciaSobrealimentadoBar, bmepFatorAmarelo, pistaoVerdeAte, pistaoAmareloAte, sensorMapVerde,
                sensorMapAmarelo, aspiradoVerdeAte, aspiradoAmareloAte, PADRAO_COMPRESSOR_MARGEM_SURGE,
                PADRAO_COMPRESSOR_MARGEM_CHOKE, PADRAO_COMPRESSOR_ROTACAO_FATOR, PADRAO_COMPRESSOR_EFICIENCIA_PONTOS,
                PADRAO_CAMBIO_VERDE_ATE, PADRAO_CAMBIO_AMARELO_ATE);
    }

    /** Valores da SPEC 7.4 e, para o compressor e o câmbio, os do plano da fase 2 ("a validar"). */
    public static LimiaresSemaforo padrao() {
        return new LimiaresSemaforo(0.80, 0.85, 0.30, 0.15, 13.0, 22.0, 1.15, 20.0, 25.0, 1.2, 1.0, 0.90, 0.98);
    }

    public double bmepReferenciaBar(boolean sobrealimentado) {
        return sobrealimentado ? bmepReferenciaSobrealimentadoBar : bmepReferenciaAspiradoBar;
    }

    private static double positivoOuPadrao(double valor, double padrao) {
        return valor > 0 ? valor : padrao;
    }

    private static void exigirOrdem(double menor, double maior, String nome) {
        if (!(menor <= maior)) {
            throw new IllegalArgumentException("Limiares de " + nome + " fora de ordem: " + menor + " > " + maior);
        }
    }
}
