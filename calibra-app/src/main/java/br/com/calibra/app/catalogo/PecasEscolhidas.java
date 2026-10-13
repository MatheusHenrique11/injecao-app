package br.com.calibra.app.catalogo;

import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Intercooler;
import br.com.calibra.core.pecas.Sensor;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.projeto.PapelPeca;
import br.com.calibra.dados.projeto.PecaUsada;

/**
 * Peças do catálogo escolhidas para um cálculo, no lugar do que seria digitado. Cada uma pode ser null: aí vale o
 * campo digitado (ou nada).
 *
 * @param sensorMap um sensor do tipo MAP: a faixa máxima dele é a faixa do semáforo
 * @param intercooler com eficiência informada, ela vira o eps do cálculo
 */
public record PecasEscolhidas(ItemCatalogo<Bico> bico, ItemCatalogo<Bico> bicoSecundario, ItemCatalogo<Bomba> bomba,
        ItemCatalogo<Intercooler> intercooler, ItemCatalogo<Sensor> sensorMap, ItemCatalogo<Injecao> injecao) {

    /** Nenhuma peça do catálogo: tudo digitado. */
    public static final PecasEscolhidas NENHUMA = new PecasEscolhidas(null, null, null, null, null, null);

    /** As mesmas peças, sem os bicos (para sugerir um). */
    public PecasEscolhidas semBicos() {
        return new PecasEscolhidas(null, null, bomba, intercooler, sensorMap, injecao);
    }

    /** As mesmas peças, sem a bomba (para sugerir uma). */
    public PecasEscolhidas semBomba() {
        return new PecasEscolhidas(bico, bicoSecundario, null, intercooler, sensorMap, injecao);
    }

    /** As peças para gravar com o cálculo: papel, item e versão (o combustível entra pelo motor). */
    public List<PecaUsada> usadas() {
        List<PecaUsada> usadas = new ArrayList<>();
        adicionar(usadas, PapelPeca.BICO, bico);
        adicionar(usadas, PapelPeca.BICO_SECUNDARIO, bicoSecundario);
        adicionar(usadas, PapelPeca.BOMBA, bomba);
        adicionar(usadas, PapelPeca.INTERCOOLER, intercooler);
        adicionar(usadas, PapelPeca.SENSOR_MAP, sensorMap);
        adicionar(usadas, PapelPeca.INJECAO, injecao);
        return usadas;
    }

    private static void adicionar(List<PecaUsada> usadas, PapelPeca papel, ItemCatalogo<?> item) {
        if (item != null) {
            usadas.add(new PecaUsada(papel, item.id(), item.versao()));
        }
    }
}
