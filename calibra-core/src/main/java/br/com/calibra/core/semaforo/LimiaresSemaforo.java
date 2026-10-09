package br.com.calibra.core.semaforo;

/**
 * Limiares do semáforo. Os padrões são os da SPEC 7.4 e podem ser editados (tela de Configurações,
 * fase 2). Frações em vez de percentuais: 0,80 = 80%.
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
        double aspiradoAmareloAte) {

    public LimiaresSemaforo {
        exigirOrdem(dcVerdeAte, dcAmareloAte, "DC do bico");
        exigirOrdem(folgaBombaAmarelo, folgaBombaVerde, "folga da bomba");
        exigirOrdem(1.0, bmepFatorAmarelo, "fator amarelo da BMEP");
        exigirOrdem(pistaoVerdeAte, pistaoAmareloAte, "velocidade do pistão");
        exigirOrdem(sensorMapAmarelo, sensorMapVerde, "faixa do sensor MAP");
        exigirOrdem(aspiradoVerdeAte, aspiradoAmareloAte, "aspirado MAP ÷ P_atm");
        if (!(bmepReferenciaAspiradoBar > 0) || !(bmepReferenciaSobrealimentadoBar > 0)) {
            throw new IllegalArgumentException("As BMEP de referência precisam ser maiores que zero.");
        }
    }

    /** Valores da SPEC 7.4. */
    public static LimiaresSemaforo padrao() {
        return new LimiaresSemaforo(0.80, 0.85, 0.30, 0.15, 13.0, 22.0, 1.15, 20.0, 25.0, 1.2, 1.0, 0.90, 0.98);
    }

    public double bmepReferenciaBar(boolean sobrealimentado) {
        return sobrealimentado ? bmepReferenciaSobrealimentadoBar : bmepReferenciaAspiradoBar;
    }

    private static void exigirOrdem(double menor, double maior, String nome) {
        if (!(menor <= maior)) {
            throw new IllegalArgumentException("Limiares de " + nome + " fora de ordem: " + menor + " > " + maior);
        }
    }
}
