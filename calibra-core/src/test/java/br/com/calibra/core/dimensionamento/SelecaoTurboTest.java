package br.com.calibra.core.dimensionamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.Fixtures;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.memoria.Aviso;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.pecas.Turbo;

/** Seleção de turbo (SPEC 7.3): só os que contêm os dois pontos, do mais eficiente no ponto de potência. */
class SelecaoTurboTest {

    /** O mapa fictício com todas as vazões multiplicadas (um turbo maior ou menor). */
    private static MapaCompressor escalado(MapaCompressor mapa, double fator) {
        return new MapaCompressor(mapa.eixoOriginal(), mapa.temperaturaReferenciaK(), mapa.pressaoReferenciaKpa(),
                escalar(mapa.surge(), fator), escalar(mapa.choke(), fator),
                mapa.ilhas().stream().map(i -> new MapaCompressor.Ilha(i.eficiencia(), escalar(i.contorno(), fator)))
                        .toList(),
                mapa.rotacoes().stream().map(r -> new MapaCompressor.LinhaRotacao(r.rpm(), escalar(r.pontos(), fator)))
                        .toList(),
                mapa.rotacaoMaximaRpm());
    }

    private static List<MapaCompressor.PontoMapa> escalar(List<MapaCompressor.PontoMapa> pontos, double fator) {
        List<MapaCompressor.PontoMapa> novos = new ArrayList<>();
        pontos.forEach(p -> novos.add(new MapaCompressor.PontoMapa(p.vazaoKgS() * fator, p.razaoPressao())));
        return novos;
    }

    @Test
    void ordemPelaEficienciaNoPontoDePotencia() {
        Dimensionamento d = CalculoDimensionamento.calcular(Fixtures.casoB()).valor();
        List<Turbo> catalogo = List.of(
                Fixtures.turbo("B menos eficiente", Fixtures.mapaFicticio(0.66, 0.70, 0.73, 160_000.0)),
                Fixtures.turbo("Pequeno", escalado(Fixtures.mapaFicticio(), 0.5)),
                Fixtures.turbo("Sem mapa", null),
                Fixtures.turbo("A mais eficiente", Fixtures.mapaFicticio()));

        Resultado<List<SelecaoTurbo.Escolha>> resultado = SelecaoTurbo.escolher(catalogo, d.compressor());
        List<SelecaoTurbo.Escolha> escolhas = resultado.valor();
        assertEquals(List.of("A mais eficiente", "B menos eficiente"),
                escolhas.stream().map(e -> e.turbo().nome()).toList());
        assertEquals("77%", SelecaoTurbo.eficiencia(escolhas.getFirst().potencia()));
        assertEquals("73%", SelecaoTurbo.eficiencia(escolhas.get(1).potencia()));
        List<String> avisos = resultado.avisos().stream().map(Aviso::mensagem).toList();
        assertTrue(avisos.contains("Turbo Sem mapa ignorado: sem mapa digitalizado."), avisos::toString);
        assertTrue(avisos.stream().anyMatch(a -> a.startsWith("Turbo Pequeno não atende: o ponto de potência está fora "
                + "do mapa (à direita da linha de choke)")), avisos::toString);
    }

    @Test
    void pontoDeTorqueForaTiraOTurbo() {
        Dimensionamento d = CalculoDimensionamento.calcular(Fixtures.casoB()
                .comMeta(Fixtures.casoB().meta().comRotacaoTorque(4500))).valor();
        Resultado<List<SelecaoTurbo.Escolha>> resultado = SelecaoTurbo.escolher(
                List.of(Fixtures.turbo("Grande", Fixtures.mapaFicticio())), d.compressor());
        assertTrue(resultado.valor().isEmpty());
        assertTrue(resultado.avisos().stream().map(Aviso::mensagem)
                .anyMatch(a -> a.contains("o ponto de torque está fora do mapa (à esquerda da linha de surge)")));
        assertTrue(resultado.avisos().stream().map(Aviso::mensagem)
                .anyMatch(a -> a.startsWith("Nenhum turbo do catálogo tem os pontos de potência e de torque")));
    }
}
