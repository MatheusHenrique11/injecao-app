package br.com.calibra.core.dimensionamento;

import static br.com.calibra.core.memoria.MemoriaCalculo.e;
import static br.com.calibra.core.util.NumeroBr.formatar;
import static br.com.calibra.core.util.NumeroBr.percentual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.memoria.MemoriaCalculo;
import br.com.calibra.core.memoria.NivelAviso;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.pecas.Bico;

/**
 * Escolha do bico no catálogo (SPEC 7.3): o menor bico que mantém o DC, já com o tempo morto, até o
 * DC máximo na rotação de potência máxima. Se o TI efetivo na marcha lenta ficar abaixo do mínimo,
 * avisa e sugere duas bancadas por cilindro.
 */
public final class SelecaoBico {

    /** Folga para erros de arredondamento ao comparar o DC com o máximo. */
    private static final double FOLGA_NUMERICA = 1e-9;

    /**
     * Bico escolhido.
     *
     * @param tiEfetivoMarchaLentaMs NaN se a marcha lenta não foi informada
     * @param sugerirDuasBancadas TI de marcha lenta abaixo do mínimo
     */
    public record Escolha(Bico bico, double vazaoRealCm3Min, double dutyCycle, double tiEfetivoMarchaLentaMs,
            boolean sugerirDuasBancadas) {
    }

    private record Avaliado(Bico bico, double vazaoRealCm3Min, double dutyCycle, double tiMarchaLentaMs) {
    }

    private SelecaoBico() {
    }

    /**
     * Avalia cada bico do catálogo com a mesma cadeia de cálculo e escolhe o menor (pela vazão real no
     * combustível e na pressão de base) que fica dentro do DC máximo. Só para uma bancada por cilindro.
     */
    public static Resultado<Optional<Escolha>> escolher(List<Bico> catalogo, EntradaDimensionamento entrada) {
        if (entrada.motor().bicosPorCilindro() != 1) {
            throw new EntradaInvalidaException(
                    "A escolha automática de bico vale para uma bancada; com duas, avalie o par na calculadora.");
        }
        MemoriaCalculo memoria = new MemoriaCalculo();
        double dcMax = entrada.parametros().dcMax();
        double tiMinimo = entrada.parametros().tiMinimoMarchaLentaMs();

        List<Avaliado> avaliados = new ArrayList<>();
        for (Bico bico : catalogo) {
            try {
                Dimensionamento d = CalculoDimensionamento.calcular(entrada.comBico(bico).comBicoSecundario(null)).valor();
                Dimensionamento.BicoAvaliado b = d.bicoAvaliado();
                avaliados.add(new Avaliado(bico, b.vazaoRealCm3Min(), b.dutyCycle(), b.tiEfetivoMarchaLentaMs()));
                memoria.passo("Bico " + bico.nome(), "DC com tempo morto na potência máxima", b.dutyCycle(), "fração",
                        e("Q_real", b.vazaoRealCm3Min(), "cm³/min"), e("t_morto", b.tempoMortoMs(), "ms"));
            } catch (EntradaInvalidaException erro) {
                memoria.aviso(NivelAviso.INFORMACAO, "Bico " + bico.nome() + " ignorado: " + erro.getMessage());
            }
        }
        avaliados.sort(Comparator.comparingDouble(Avaliado::vazaoRealCm3Min));

        Optional<Avaliado> menor = avaliados.stream()
                .filter(a -> a.dutyCycle() <= dcMax + FOLGA_NUMERICA)
                .findFirst();
        if (menor.isEmpty()) {
            String maior = avaliados.isEmpty() ? "" : " O maior bico avaliado ("
                    + avaliados.getLast().bico().nome() + ") fica em " + percentual(avaliados.getLast().dutyCycle(), 1) + ".";
            memoria.aviso(NivelAviso.AMARELO, "Nenhum bico do catálogo mantém o DC até " + percentual(dcMax, 0)
                    + "." + maior);
            return memoria.resultado(Optional.empty());
        }
        Avaliado escolhido = menor.get();
        boolean duasBancadas = escolhido.tiMarchaLentaMs() < tiMinimo;
        if (duasBancadas) {
            memoria.aviso(NivelAviso.AMARELO, "O bico " + escolhido.bico().nome() + " fica com TI efetivo de "
                    + formatar(escolhido.tiMarchaLentaMs(), 2) + " ms na marcha lenta, abaixo de "
                    + formatar(tiMinimo, 1) + " ms: avaliar duas bancadas por cilindro (primária menor).");
        }
        return memoria.resultado(Optional.of(new Escolha(escolhido.bico(), escolhido.vazaoRealCm3Min(),
                escolhido.dutyCycle(), escolhido.tiMarchaLentaMs(), duasBancadas)));
    }
}
