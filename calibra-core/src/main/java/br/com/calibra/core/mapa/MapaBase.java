package br.com.calibra.core.mapa;

import java.util.List;
import java.util.Objects;

import br.com.calibra.core.pecas.ModeloCombustivel;
import br.com.calibra.core.semaforo.Cor;

/**
 * O mapa base gerado (SPEC 7.5; decisão 294). As tabelas ficam nos eixos do perfil da injeção (ou nos padrão da
 * SPEC): rotação em rpm e carga em kPa absolutos.
 *
 * @param modeloCombustivel ms, VE ou modelo físico: diz o que vai na tabela de combustível
 * @param tempoMortoNaTabela a tabela em ms já inclui o tempo morto do bico
 * @param temperaturaReferenciaC a temperatura do ar em que a tabela de combustível vale
 * @param ve a VE (fração) de cada célula, nos eixos do combustível
 * @param lambda o lambda alvo, nos eixos da tabela de lambda (ou do combustível)
 * @param combustivel em ms (tempo de injeção) ou em % (VE), nos eixos do combustível
 * @param dutyCycle o ciclo do bico (fração), já com o tempo morto, nos eixos do combustível
 * @param ignicaoFonte o avanço da fonte nos eixos da ignição, antes das reduções; vazia sem fonte
 * @param ignicao o avanço com as reduções conservadoras; vazia sem fonte
 */
public record MapaBase(
        ModeloCombustivel modeloCombustivel,
        boolean tempoMortoNaTabela,
        double temperaturaReferenciaC,
        Tabela ve,
        Tabela lambda,
        Tabela combustivel,
        Tabela dutyCycle,
        Tabela ignicaoFonte,
        Tabela ignicao,
        Travas travas,
        List<AlertaMapa> alertas) {

    public MapaBase {
        Objects.requireNonNull(modeloCombustivel, "modeloCombustivel");
        alertas = List.copyOf(alertas);
    }

    /**
     * As travas de segurança (SPEC 7.5).
     *
     * @param pressaoPrevistaKpa a pressão do turbo (relativa) prevista no dimensionamento; NaN no aspirado
     * @param molaWastegateKpa a pressão da mola informada; null se não foi informada
     * @param pressaoAlvoKpa o menor entre a mola e a fração da prevista; NaN no aspirado
     * @param limitadorRpm a rotação máxima menos a margem
     */
    public record Travas(double pressaoPrevistaKpa, Double molaWastegateKpa, double pressaoAlvoKpa, int limitadorRpm) {
    }

    /** Os alertas vermelhos, que só passam com justificativa. */
    public List<AlertaMapa> vermelhos() {
        return alertas.stream().filter(a -> a.cor() == Cor.VERMELHO).toList();
    }
}
