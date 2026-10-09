package br.com.calibra.app;

import java.util.EnumMap;
import java.util.Map;

/**
 * Caso golden B da SPEC 7.6 digitado como na tela. Os campos que não entram na conta (taxa, válvulas,
 * roda fônica...) têm valores de teste. O diâmetro 82,83 mm fecha os 2.000 cm³ da spec.
 */
final class ValoresDeTeste {

    private ValoresDeTeste() {
    }

    static Map<Campo, String> casoB() {
        Map<Campo, String> v = new EnumMap<>(Campo.class);
        v.put(Campo.CILINDROS, "4");
        v.put(Campo.ARRANJO, "EM_LINHA");
        v.put(Campo.DIAMETRO, "82,83");
        v.put(Campo.CURSO, "92,8");
        v.put(Campo.TAXA, "9,5");
        v.put(Campo.VALVULAS, "4");
        v.put(Campo.COMANDO, "ORIGINAL");
        v.put(Campo.ASPIRACAO, "TURBO");
        v.put(Campo.PISTOES, "ORIGINAL");
        v.put(Campo.BIELAS, "ORIGINAL");
        v.put(Campo.ROTACAO_MAXIMA, "7.500");
        v.put(Campo.MARCHA_LENTA_RPM, "900");
        v.put(Campo.ORDEM_IGNICAO, "1-3-4-2");
        v.put(Campo.ANGULOS_PMS, "0; 180; 360; 540");
        v.put(Campo.BICOS_POR_CILINDRO, "1");
        v.put(Campo.BICOS_EXTRAS, "0");
        v.put(Campo.MODO_INJECAO, "SEQUENCIAL");
        v.put(Campo.MODO_IGNICAO, "SEQUENCIAL");
        v.put(Campo.DENTES_TOTAIS, "60");
        v.put(Campo.DENTES_FALTANTES, "2");
        v.put(Campo.SENSOR_FASE, "true");
        v.put(Campo.COMBUSTIVEL, "EHC");
        v.put(Campo.SENSOR_ETANOL, "false");
        v.put(Campo.ALTITUDE, "600");
        v.put(Campo.POTENCIA_CV, "400");
        v.put(Campo.PONTO_MEDICAO, "VIRABREQUIM");
        v.put(Campo.RPM_POTENCIA, "7.000");
        v.put(Campo.USO, "RUA");
        v.put(Campo.CAMINHO_AR, "K_AR");
        v.put(Campo.K_AR, "10,5");
        v.put(Campo.LAMBDA, "0,80");
        v.put(Campo.VE, "0,95");
        v.put(Campo.TEMPERATURA_AMBIENTE, "25");
        v.put(Campo.PERDA_DESCARGA_PSI, "2");
        v.put(Campo.PERDA_ADMISSAO_PSI, "1");
        v.put(Campo.ETA_C, "0,72");
        v.put(Campo.EPS, "0,80");
        v.put(Campo.ACRESCIMO_TM, "10");
        v.put(Campo.DC_MAX_PCT, "80");
        v.put(Campo.DENSIDADE_TESTE_PADRAO, "0,70");
        v.put(Campo.TI_MINIMO_MARCHA_LENTA, "1,0");
        v.put(Campo.PRESSAO_BASE_BAR, "3");
        v.put(Campo.BICO_VAZAO, "1.050");
        v.put(Campo.BICO_PRESSAO_TESTE_BAR, "3");
        v.put(Campo.BICO_DENSIDADE_TESTE, "0,70");
        v.put(Campo.BICO_TEMPO_MORTO, "1,0");
        return v;
    }
}
