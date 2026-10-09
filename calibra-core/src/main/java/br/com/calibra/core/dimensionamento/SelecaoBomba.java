package br.com.calibra.core.dimensionamento;

import static br.com.calibra.core.memoria.MemoriaCalculo.e;
import static br.com.calibra.core.util.NumeroBr.formatar;
import static br.com.calibra.core.util.NumeroBr.percentual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import br.com.calibra.core.memoria.MemoriaCalculo;
import br.com.calibra.core.memoria.NivelAviso;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.semaforo.Avaliacao;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.core.semaforo.Semaforo;

/**
 * Escolha da bomba no catálogo (SPEC 7.3): a menor bomba cuja vazão, na pressão exigida e na tensão
 * informada, cobre a necessidade com a folga verde do semáforo.
 */
public final class SelecaoBomba {

    /** Bomba escolhida, com a vazão disponível na condição pedida e a avaliação da folga. */
    public record Escolha(Bomba bomba, double vazaoDisponivelLh, double folga, Avaliacao avaliacao) {
    }

    private SelecaoBomba() {
    }

    public static Resultado<Optional<Escolha>> escolher(List<Bomba> catalogo, double vazaoNecessariaLh,
            double pressaoKpa, double tensaoV, LimiaresSemaforo limiares) {
        MemoriaCalculo memoria = new MemoriaCalculo();
        List<Escolha> avaliadas = new ArrayList<>();
        for (Bomba bomba : catalogo) {
            OptionalDouble disponivel = bomba.vazaoLh(pressaoKpa, tensaoV);
            if (disponivel.isEmpty()) {
                memoria.aviso(NivelAviso.INFORMACAO, "Bomba " + bomba.nome() + " ignorada: a curva não cobre "
                        + formatar(pressaoKpa, 0) + " kPa a " + formatar(tensaoV, 1) + " V.");
                continue;
            }
            double folga = disponivel.getAsDouble() / vazaoNecessariaLh - 1.0;
            memoria.passo("Bomba " + bomba.nome(), "folga = vazão disponível / vazão necessária − 1", folga, "fração",
                    e("disponível", disponivel.getAsDouble(), "L/h"), e("necessária", vazaoNecessariaLh, "L/h"),
                    e("pressão", pressaoKpa, "kPa"), e("tensão", tensaoV, "V"));
            avaliadas.add(new Escolha(bomba, disponivel.getAsDouble(), folga, Semaforo.folgaBomba(folga, limiares)));
        }
        Optional<Escolha> menorVerde = avaliadas.stream()
                .filter(a -> a.avaliacao().cor() == Cor.VERDE)
                .min(Comparator.comparingDouble(Escolha::vazaoDisponivelLh));
        if (menorVerde.isEmpty()) {
            Optional<Escolha> maiorFolga = avaliadas.stream().max(Comparator.comparingDouble(Escolha::folga));
            String detalhe = maiorFolga.map(m -> " A de maior folga é " + m.bomba().nome() + " ("
                    + percentual(m.folga(), 0) + ").").orElse("");
            memoria.aviso(NivelAviso.AMARELO, "Nenhuma bomba do catálogo atende com folga de "
                    + percentual(limiares.folgaBombaVerde(), 0) + " ou mais." + detalhe);
        }
        return memoria.resultado(menorVerde);
    }
}
