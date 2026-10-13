package br.com.calibra.app.catalogo;

import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.Cambio;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Intercooler;
import br.com.calibra.core.pecas.Sensor;
import br.com.calibra.core.pecas.Turbo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.projeto.PapelPeca;
import br.com.calibra.dados.projeto.PecaUsada;

/**
 * Peças do catálogo escolhidas para um cálculo, no lugar do que seria digitado. Cada uma pode ser null: aí vale o
 * campo digitado (ou nada).
 *
 * @param sensorMap um sensor do tipo MAP: a faixa máxima dele é a faixa do semáforo
 * @param intercooler com eficiência informada, ela vira o eps do cálculo
 * @param turbo com o mapa do compressor, os pontos de potência e de torque são conferidos nele
 * @param cambio com o torque máximo, entra o indicador torque × câmbio (no projeto, é o câmbio do veículo)
 */
public record PecasEscolhidas(ItemCatalogo<Bico> bico, ItemCatalogo<Bico> bicoSecundario, ItemCatalogo<Bomba> bomba,
        ItemCatalogo<Intercooler> intercooler, ItemCatalogo<Sensor> sensorMap, ItemCatalogo<Injecao> injecao,
        ItemCatalogo<Turbo> turbo, ItemCatalogo<Cambio> cambio) {

    /** Nenhuma peça do catálogo: tudo digitado. */
    public static final PecasEscolhidas NENHUMA = new PecasEscolhidas(null, null, null, null, null, null);

    /** Sem turbo e sem câmbio (como era antes do bloco 2.4). */
    public PecasEscolhidas(ItemCatalogo<Bico> bico, ItemCatalogo<Bico> bicoSecundario, ItemCatalogo<Bomba> bomba,
            ItemCatalogo<Intercooler> intercooler, ItemCatalogo<Sensor> sensorMap, ItemCatalogo<Injecao> injecao) {
        this(bico, bicoSecundario, bomba, intercooler, sensorMap, injecao, null, null);
    }

    /** As mesmas peças, sem os bicos (para sugerir um). */
    public PecasEscolhidas semBicos() {
        return new PecasEscolhidas(null, null, bomba, intercooler, sensorMap, injecao, turbo, cambio);
    }

    /** As mesmas peças, sem a bomba (para sugerir uma). */
    public PecasEscolhidas semBomba() {
        return new PecasEscolhidas(bico, bicoSecundario, null, intercooler, sensorMap, injecao, turbo, cambio);
    }

    /** As mesmas peças, sem o turbo (para sugerir um). */
    public PecasEscolhidas semTurbo() {
        return new PecasEscolhidas(bico, bicoSecundario, bomba, intercooler, sensorMap, injecao, null, cambio);
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
        adicionar(usadas, PapelPeca.TURBO, turbo);
        adicionar(usadas, PapelPeca.CAMBIO, cambio);
        return usadas;
    }

    private static void adicionar(List<PecaUsada> usadas, PapelPeca papel, ItemCatalogo<?> item) {
        if (item != null) {
            usadas.add(new PecaUsada(papel, item.id(), item.versao()));
        }
    }
}
