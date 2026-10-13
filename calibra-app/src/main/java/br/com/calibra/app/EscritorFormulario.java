package br.com.calibra.app;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.com.calibra.app.casos.CampoPadrao;
import br.com.calibra.app.casos.FormularioPadroes;
import br.com.calibra.core.calculo.CaminhoAr;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.PontoMedicao;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.core.util.NumeroBr;

/**
 * O caminho de volta do {@link LeitorFormulario}: um motor ou uma entrada salva viram o texto dos campos da
 * tela, nas unidades da tela (cv, bar, psi, %). Ler de volta o que foi escrito dá os mesmos números, com até
 * 4 casas. Sem JavaFX.
 */
public final class EscritorFormulario {

    private EscritorFormulario() {
    }

    /** Os campos do motor. */
    public static Map<Campo, String> motor(ConfiguracaoMotor m) {
        Map<Campo, String> v = new EnumMap<>(Campo.class);
        v.put(Campo.CILINDROS, String.valueOf(m.numeroCilindros()));
        v.put(Campo.ARRANJO, nome(m.arranjo()));
        v.put(Campo.ANGULO_BANCADAS, numero(m.anguloEntreBancadas()));
        v.put(Campo.DIAMETRO, numero(m.diametroMm()));
        v.put(Campo.CURSO, numero(m.cursoMm()));
        v.put(Campo.CILINDRADA_INFORMADA, numero(m.cilindradaInformadaCc()));
        v.put(Campo.TAXA, numero(m.taxaCompressao()));
        v.put(Campo.VALVULAS, String.valueOf(m.valvulasPorCilindro()));
        v.put(Campo.COMANDO, nome(m.comando()));
        v.put(Campo.DURACAO_COMANDO, numero(m.duracaoComandoGraus()));
        v.put(Campo.CRUZAMENTO, numero(m.cruzamentoGraus()));
        v.put(Campo.ASPIRACAO, nome(m.aspiracao()));
        v.put(Campo.PISTOES, nome(m.pistoes()));
        v.put(Campo.BIELAS, nome(m.bielas()));
        v.put(Campo.ROTACAO_MAXIMA, NumeroBr.formatar(m.rotacaoMaxima(), 0));
        v.put(Campo.MARCHA_LENTA_RPM, NumeroBr.formatar(m.rotacaoMarchaLenta(), 0));
        v.put(Campo.ORDEM_IGNICAO, m.ordemIgnicao().stream().map(String::valueOf).collect(Collectors.joining("-")));
        v.put(Campo.ANGULOS_PMS, m.angulosPms().stream().map(EscritorFormulario::numero)
                .collect(Collectors.joining("; ")));
        v.put(Campo.BICOS_POR_CILINDRO, String.valueOf(m.bicosPorCilindro()));
        v.put(Campo.BICOS_EXTRAS, String.valueOf(m.bicosExtrasCorpoBorboleta()));
        v.put(Campo.MODO_INJECAO, nome(m.modoInjecao()));
        v.put(Campo.MODO_IGNICAO, nome(m.modoIgnicao()));
        if (m.rodaFonica() != null) {
            v.put(Campo.DENTES_TOTAIS, String.valueOf(m.rodaFonica().dentesTotais()));
            v.put(Campo.DENTES_FALTANTES, String.valueOf(m.rodaFonica().dentesFaltantes()));
            v.put(Campo.SENSOR_FASE, String.valueOf(m.rodaFonica().sensorFase()));
        }
        v.put(Campo.COMBUSTIVEL, m.combustivel() == null ? "" : m.combustivel().codigo());
        v.put(Campo.SENSOR_ETANOL, String.valueOf(m.sensorEtanol()));
        v.put(Campo.ALTITUDE, numero(m.altitudeLocalM()));
        return v;
    }

    /** Os campos de meta, ar, parâmetros e peças (tudo menos o motor). */
    public static Map<Campo, String> semMotor(EntradaDimensionamento e) {
        Map<Campo, String> v = new EnumMap<>(Campo.class);
        v.putAll(meta(e.meta()));
        Parametros p = e.parametros();
        v.put(Campo.CAMINHO_AR, nome(p.caminhoAr()));
        v.put(Campo.K_AR, p.caminhoAr() == CaminhoAr.K_AR ? numero(p.kAr()) : "");
        v.put(Campo.BSFC, p.caminhoAr() == CaminhoAr.BSFC ? numero(p.bsfc()) : "");
        v.put(Campo.LAMBDA, numero(e.lambdaAlvo()));
        v.put(Campo.VE, numero(e.ve()));
        v.put(Campo.TEMPERATURA_AMBIENTE, numero(e.temperaturaAmbienteC()));
        Map<CampoPadrao, String> padroes = FormularioPadroes.textos(p);
        v.put(Campo.PERDA_DESCARGA_PSI, padroes.get(CampoPadrao.PERDA_DESCARGA_PSI));
        v.put(Campo.PERDA_ADMISSAO_PSI, padroes.get(CampoPadrao.PERDA_ADMISSAO_PSI));
        v.put(Campo.ETA_C, padroes.get(CampoPadrao.ETA_C));
        v.put(Campo.EPS, padroes.get(CampoPadrao.EPS));
        v.put(Campo.ACRESCIMO_TM, padroes.get(CampoPadrao.ACRESCIMO_TM));
        v.put(Campo.DC_MAX_PCT, padroes.get(CampoPadrao.DC_MAX_PCT));
        v.put(Campo.DENSIDADE_TESTE_PADRAO, padroes.get(CampoPadrao.DENSIDADE_TESTE));
        v.put(Campo.TI_MINIMO_MARCHA_LENTA, padroes.get(CampoPadrao.TI_MINIMO));
        v.put(Campo.PRESSAO_BASE_BAR, numero(Conversoes.kpaParaBar(e.pressaoBaseKpa())));
        escreverBico(v, e.bico(), Campo.BICO_VAZAO, Campo.BICO_PRESSAO_TESTE_BAR, Campo.BICO_DENSIDADE_TESTE,
                Campo.BICO_TEMPO_MORTO, p);
        escreverBico(v, e.bicoSecundario(), Campo.BICO2_VAZAO, Campo.BICO2_PRESSAO_TESTE_BAR,
                Campo.BICO2_DENSIDADE_TESTE, Campo.BICO2_TEMPO_MORTO, p);
        v.put(Campo.TENSAO_V, numero(e.tensaoV()));
        v.put(Campo.MARCHA_LENTA_MAP, e.marchaLenta() == null ? "" : numero(e.marchaLenta().mapKpa()));
        v.put(Campo.MARCHA_LENTA_VE, e.marchaLenta() == null ? "" : numero(e.marchaLenta().ve()));
        v.put(Campo.BOMBA_VAZAO, numero(e.vazaoBombaDisponivelLh()));
        v.put(Campo.SENSOR_MAP_KPA, numero(e.faixaSensorMapKpa()));
        v.put(Campo.INJECAO_NOME, e.injecao() == null ? "" : e.injecao().nome());
        v.put(Campo.INJECAO_SAIDAS_BICO, e.injecao() == null ? "" : String.valueOf(e.injecao().saidasBico()));
        v.put(Campo.INJECAO_SAIDAS_IGNICAO, e.injecao() == null ? "" : String.valueOf(e.injecao().saidasIgnicao()));
        return v;
    }

    /** Os campos da meta: potência em cv, onde é medida, perda, rotações e uso. */
    public static Map<Campo, String> meta(Meta meta) {
        Map<Campo, String> v = new EnumMap<>(Campo.class);
        v.put(Campo.POTENCIA_CV, numero(Conversoes.kwParaCv(meta.potenciaKw())));
        v.put(Campo.PONTO_MEDICAO, nome(meta.pontoMedicao()));
        v.put(Campo.PERDA_TRANSMISSAO,
                meta.pontoMedicao() == PontoMedicao.RODA ? numero(meta.perdaTransmissaoPct()) : "");
        v.put(Campo.RPM_POTENCIA, NumeroBr.formatar(meta.rotacaoPotenciaMaxima(), 0));
        v.put(Campo.RPM_TORQUE,
                meta.rotacaoTorqueMaximo() == null ? "" : NumeroBr.formatar(meta.rotacaoTorqueMaximo(), 0));
        v.put(Campo.USO, nome(meta.uso()));
        return v;
    }

    /** Bico digitado: vazão, pressão de teste, fluido e tempo morto. Sem bico, o fluido fica no padrão. */
    private static void escreverBico(Map<Campo, String> v, Bico bico, Campo vazao, Campo pressao, Campo densidade,
            Campo tempoMorto, Parametros p) {
        if (bico == null) {
            v.put(vazao, "");
            v.put(pressao, "");
            v.put(densidade, FormularioPadroes.textos(p).get(CampoPadrao.DENSIDADE_TESTE));
            v.put(tempoMorto, "");
            return;
        }
        v.put(vazao, numero(bico.vazaoNominalCm3Min()));
        v.put(pressao, numero(Conversoes.kpaParaBar(bico.pressaoTesteKpa())));
        v.put(densidade, numero(bico.densidadeFluidoTesteKgL()));
        List<TempoMorto.Ponto> pontos = bico.tempoMorto().pontos();
        v.put(tempoMorto, bico.tempoMorto().eFixo() ? numero(pontos.getFirst().tempoMs()) : "");
    }

    private static String nome(Enum<?> valor) {
        return valor == null ? "" : valor.name();
    }

    /** Número com até 4 casas, sem zeros sobrando; vazio se não houver. */
    private static String numero(Double valor) {
        return valor == null || !Double.isFinite(valor) ? "" : NumeroBr.formatar(valor, 0, 4);
    }
}
