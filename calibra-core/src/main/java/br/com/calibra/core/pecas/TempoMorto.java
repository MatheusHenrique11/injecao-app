package br.com.calibra.core.pecas;

import java.util.List;
import java.util.OptionalDouble;

import br.com.calibra.core.util.Interpolacao;

/**
 * Tempo morto do bico (SPEC 7.1: t_morto(V_bat, dP)). Vem da tabela do fabricante: pontos de
 * tensão, pressão diferencial e tempo. Com um ponto só, vale para qualquer condição.
 */
public record TempoMorto(List<Ponto> pontos) {

    /**
     * Um ponto da tabela.
     *
     * @param tensaoV tensão da bateria
     * @param pressaoKpa pressão diferencial no bico
     * @param tempoMs tempo morto em ms
     */
    public record Ponto(double tensaoV, double pressaoKpa, double tempoMs) {
    }

    public TempoMorto {
        pontos = List.copyOf(pontos);
        if (pontos.isEmpty()) {
            throw new IllegalArgumentException("O tempo morto precisa de pelo menos um ponto.");
        }
    }

    /** Tempo morto de valor único, sem tabela (ex.: 1,0 ms). */
    public static TempoMorto fixo(double tempoMs) {
        return new TempoMorto(List.of(new Ponto(Double.NaN, Double.NaN, tempoMs)));
    }

    public boolean eFixo() {
        return pontos.size() == 1;
    }

    /**
     * Tempo morto na tensão e na pressão pedidas. Interpola na tensão dentro de cada pressão e depois
     * entre as pressões; tabela de uma pressão só vale para qualquer pressão.
     *
     * @param tensaoV pode ser null quando o tempo morto é fixo
     * @return vazio se a tabela não cobrir a condição pedida
     */
    public OptionalDouble valorMs(Double tensaoV, double pressaoKpa) {
        if (eFixo()) {
            return OptionalDouble.of(pontos.getFirst().tempoMs());
        }
        if (tensaoV == null) {
            return OptionalDouble.empty();
        }
        List<Interpolacao.PontoDeCurva> curvas = pontos.stream()
                .map(p -> new Interpolacao.PontoDeCurva(p.pressaoKpa(), p.tensaoV(), p.tempoMs()))
                .toList();
        return Interpolacao.porCurvas(curvas, pressaoKpa, tensaoV, true);
    }
}
