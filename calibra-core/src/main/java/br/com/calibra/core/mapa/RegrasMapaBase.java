package br.com.calibra.core.mapa;

import java.util.ArrayList;
import java.util.List;

/**
 * As regras do mapa base (SPEC 7.5), editáveis em Configurações (decisão 294). Os padrões são os da SPEC; as reduções
 * de ignição não têm padrão (a SPEC não dá número), e o gerador avisa enquanto não forem configuradas.
 *
 * @param lambdaCargaParcial o lambda abaixo de {@code cargaParcialAteKpa} (SPEC: 1,00 em RUA)
 * @param cargaParcialAteKpa até onde vale o lambda de carga parcial (70 kPa)
 * @param plenaCargaKpa onde a interpolação chega ao alvo de plena carga do aspirado (100 kPa); acima dela, o alvo sob
 *     pressão do combustível
 * @param riquezaExtraAcimaKpa a partir de que carga o mapa base fica mais rico que o alvo (90 kPa)
 * @param riquezaExtra quanto mais rico nessas células (0,03 de lambda)
 * @param riquezaExtraUsoContinuo a mais em NÁUTICO e CIRCUITO/ENDURANCE (0,02)
 * @param reducaoIgnicaoPorPressao graus tirados a cada 10 kPa acima de 100 kPa; null = não configurada
 * @param reducaoIgnicaoPorTemperatura graus tirados a cada 10 °C de ar acima da temperatura da fonte; null = não
 *     configurada
 * @param reducaoIgnicaoTrocaCombustivel graus tirados quando a fonte é de outro combustível; null = não configurada
 * @param margemLimitadorRpm o limitador fica tantos rpm abaixo da rotação máxima (500)
 * @param fracaoPressaoPrevista a pressão alvo do turbo fica nesta fração da prevista, ou na mola da wastegate, o que
 *     for menor (0,50)
 */
public record RegrasMapaBase(
        double lambdaCargaParcial,
        double cargaParcialAteKpa,
        double plenaCargaKpa,
        double riquezaExtraAcimaKpa,
        double riquezaExtra,
        double riquezaExtraUsoContinuo,
        Double reducaoIgnicaoPorPressao,
        Double reducaoIgnicaoPorTemperatura,
        Double reducaoIgnicaoTrocaCombustivel,
        double margemLimitadorRpm,
        double fracaoPressaoPrevista) {

    /** Os valores da SPEC 7.5; as reduções de ignição vazias. */
    public static RegrasMapaBase padrao() {
        return new RegrasMapaBase(1.00, 70, 100, 90, 0.03, 0.02, null, null, null, 500, 0.50);
    }

    /** As mesmas regras sem a riqueza extra (para conferir o mapa contra o dimensionamento, nos testes). */
    public RegrasMapaBase semRiquezaExtra() {
        return new RegrasMapaBase(lambdaCargaParcial, cargaParcialAteKpa, plenaCargaKpa, riquezaExtraAcimaKpa, 0, 0,
                reducaoIgnicaoPorPressao, reducaoIgnicaoPorTemperatura, reducaoIgnicaoTrocaCombustivel,
                margemLimitadorRpm, fracaoPressaoPrevista);
    }

    /** As mesmas regras com as três reduções de ignição. */
    public RegrasMapaBase comReducoes(Double porPressao, Double porTemperatura, Double trocaCombustivel) {
        return new RegrasMapaBase(lambdaCargaParcial, cargaParcialAteKpa, plenaCargaKpa, riquezaExtraAcimaKpa,
                riquezaExtra, riquezaExtraUsoContinuo, porPressao, porTemperatura, trocaCombustivel,
                margemLimitadorRpm, fracaoPressaoPrevista);
    }

    /** O que está errado nas regras, em português; vazio se estiverem certas. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (!(lambdaCargaParcial >= 0.6 && lambdaCargaParcial <= 1.2)) {
            erros.add("Lambda de carga parcial: de 0,60 a 1,20.");
        }
        if (!(cargaParcialAteKpa > 0 && cargaParcialAteKpa < plenaCargaKpa)) {
            erros.add("A carga parcial precisa terminar antes da plena carga.");
        }
        if (!(plenaCargaKpa >= 50 && plenaCargaKpa <= 150)) {
            erros.add("Plena carga do aspirado: de 50 a 150 kPa.");
        }
        if (!(riquezaExtra >= 0 && riquezaExtra <= 0.2) || !(riquezaExtraUsoContinuo >= 0
                && riquezaExtraUsoContinuo <= 0.2)) {
            erros.add("A riqueza extra fica entre 0 e 0,20 de lambda.");
        }
        for (Double reducao : new Double[] {reducaoIgnicaoPorPressao, reducaoIgnicaoPorTemperatura,
                reducaoIgnicaoTrocaCombustivel}) {
            if (reducao != null && !(reducao >= 0 && reducao <= 20)) {
                erros.add("Cada redução de ignição fica entre 0° e 20° (ou em branco).");
                break;
            }
        }
        if (!(margemLimitadorRpm >= 0 && margemLimitadorRpm <= 3000)) {
            erros.add("A margem do limitador fica entre 0 e 3.000 rpm.");
        }
        if (!(fracaoPressaoPrevista > 0 && fracaoPressaoPrevista <= 1)) {
            erros.add("A fração da pressão prevista fica entre 1% e 100%.");
        }
        return erros;
    }

    /** As reduções de ignição que faltam configurar, pelo nome. */
    public List<String> reducoesNaoConfiguradas() {
        List<String> faltam = new ArrayList<>();
        if (reducaoIgnicaoPorPressao == null) {
            faltam.add("por pressão");
        }
        if (reducaoIgnicaoPorTemperatura == null) {
            faltam.add("por temperatura do ar");
        }
        if (reducaoIgnicaoTrocaCombustivel == null) {
            faltam.add("por troca de combustível");
        }
        return faltam;
    }
}
