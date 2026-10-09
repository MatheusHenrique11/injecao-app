package br.com.calibra.core.pecas;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

import br.com.calibra.core.util.Interpolacao;

/**
 * Bomba de combustível com a curva do fabricante: pontos de pressão, tensão e vazão (SPEC 7.3).
 * Os dados vêm do fabricante ou do usuário, nunca do app.
 */
public record Bomba(String nome, List<Ponto> curva, String fonte, LocalDate data) {

    /**
     * Um ponto da curva.
     *
     * @param pressaoKpa pressão de saída (manométrica)
     * @param tensaoV tensão na bomba
     * @param vazaoLh vazão nessa pressão e tensão
     */
    public record Ponto(double pressaoKpa, double tensaoV, double vazaoLh) {
    }

    public Bomba {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(fonte, "fonte");
        curva = List.copyOf(curva);
        if (curva.isEmpty()) {
            throw new IllegalArgumentException("A curva da bomba precisa de pelo menos um ponto.");
        }
    }

    /**
     * Vazão na pressão e na tensão pedidas: interpola na pressão dentro de cada tensão e depois entre as
     * tensões. Não extrapola; uma curva de uma tensão só vale apenas nessa tensão.
     *
     * @return vazio se a curva não cobrir a condição pedida
     */
    public OptionalDouble vazaoLh(double pressaoKpa, double tensaoV) {
        List<Interpolacao.PontoDeCurva> pontos = curva.stream()
                .map(p -> new Interpolacao.PontoDeCurva(p.tensaoV(), p.pressaoKpa(), p.vazaoLh()))
                .toList();
        return Interpolacao.porCurvas(pontos, tensaoV, pressaoKpa, false);
    }
}
