package br.com.calibra.core.dimensionamento;

import static br.com.calibra.core.util.NumeroBr.formatar;
import static br.com.calibra.core.util.NumeroBr.percentual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.memoria.MemoriaCalculo;
import br.com.calibra.core.memoria.NivelAviso;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.pecas.Turbo;

/**
 * Escolha do turbo no catálogo (SPEC 7.3): os turbos cujo mapa contém os pontos de potência e de torque, em ordem de
 * eficiência no ponto de potência. A vazão corrigida de cada turbo usa a referência do próprio mapa.
 */
public final class SelecaoTurbo {

    /**
     * Um turbo que atende, com os dois pontos no mapa dele.
     *
     * @param torque null sem a rotação de torque máximo
     */
    public record Escolha(Turbo turbo, MapaCompressor.PontoNoMapa potencia, MapaCompressor.PontoNoMapa torque) {
    }

    private SelecaoTurbo() {
    }

    /**
     * Os turbos que atendem, do mais eficiente no ponto de potência para o menos (ponto fora das ilhas por último).
     * Turbo sem mapa fica de fora, com um aviso.
     */
    public static Resultado<List<Escolha>> escolher(List<Turbo> catalogo, Dimensionamento.PontosCompressor pontos) {
        MemoriaCalculo memoria = new MemoriaCalculo();
        List<Escolha> atendem = new ArrayList<>();
        for (Turbo turbo : catalogo) {
            MapaCompressor mapa = turbo.mapa();
            if (mapa == null) {
                memoria.aviso(NivelAviso.INFORMACAO, "Turbo " + turbo.nome() + " ignorado: sem mapa digitalizado.");
                continue;
            }
            Dimensionamento.NoMapa noMapa = pontos.noMapa(turbo.nome(), mapa);
            String fora = foraDoMapa(noMapa);
            if (fora != null) {
                memoria.aviso(NivelAviso.INFORMACAO, "Turbo " + turbo.nome() + " não atende: " + fora + ".");
                continue;
            }
            atendem.add(new Escolha(turbo, noMapa.potencia(), noMapa.torque()));
        }
        atendem.sort(Comparator.comparingDouble((Escolha e) -> e.potencia().eficiencia() == null ? -1.0
                : e.potencia().eficiencia()).reversed().thenComparing(e -> e.turbo().nome()));
        for (Escolha escolha : atendem) {
            Double eficiencia = escolha.potencia().eficiencia();
            memoria.passo("Turbo " + escolha.turbo().nome(), "eficiência no ponto de potência (ilha mais interna)",
                    eficiencia == null ? Double.NaN : eficiencia, "fração",
                    MemoriaCalculo.e("vazão corrigida", escolha.potencia().vazaoCorrigidaKgS(), "kg/s"),
                    MemoriaCalculo.e("PR", escolha.potencia().razaoPressao(), ""));
        }
        if (atendem.isEmpty() && !catalogo.isEmpty()) {
            memoria.aviso(NivelAviso.AMARELO, "Nenhum turbo do catálogo tem os pontos de potência e de torque dentro do "
                    + "mapa (PR " + formatar(pontos.razaoPressao(), 2) + ").");
        }
        return memoria.resultado(atendem);
    }

    private static String foraDoMapa(Dimensionamento.NoMapa noMapa) {
        if (!noMapa.potencia().dentro()) {
            return "o ponto de potência está fora do mapa (" + noMapa.potencia().foraDoMapa() + ")";
        }
        if (noMapa.torque() != null && !noMapa.torque().dentro()) {
            return "o ponto de torque está fora do mapa (" + noMapa.torque().foraDoMapa() + ")";
        }
        return null;
    }

    /** "76%" ou "fora das ilhas", para as listas. */
    public static String eficiencia(MapaCompressor.PontoNoMapa ponto) {
        return ponto.eficiencia() == null ? "fora das ilhas" : percentual(ponto.eficiencia(), 0);
    }
}
