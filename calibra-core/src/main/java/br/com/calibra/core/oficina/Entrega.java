package br.com.calibra.core.oficina;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.util.NumeroBr;

/**
 * A entrega do veículo ao cliente (SPEC 6.5): quando, o km (ou as horas de motor) de saída e a garantia, que conta
 * a partir do dia da entrega.
 *
 * @param usoSaida null = não anotado
 */
public record Entrega(LocalDateTime quando, Double usoSaida, Garantia garantia) {

    /** Tudo o que está errado, em português; vazio = pode gravar. */
    public List<String> validar(LocalDateTime agora) {
        List<String> erros = new ArrayList<>();
        if (quando == null) {
            erros.add("Informe a data e a hora da entrega.");
        } else if (quando.isAfter(agora.plusMinutes(1))) {
            erros.add("A data e a hora da entrega não podem estar no futuro.");
        }
        if (usoSaida != null && !(usoSaida >= 0)) {
            erros.add("O km (ou as horas de motor) da saída não pode ser negativo.");
        }
        if (garantia == null) {
            erros.add("Informe a garantia em dias (0 = sem garantia).");
        } else {
            erros.addAll(garantia.validar());
        }
        return erros;
    }

    /**
     * Avisos que não impedem a entrega: saída com menos km que a entrada é quase sempre erro de digitação, mas
     * acontece quando o painel é trocado no serviço.
     */
    public List<String> avisos(Double usoEntrada) {
        List<String> avisos = new ArrayList<>();
        if (usoSaida != null && usoEntrada != null && usoSaida < usoEntrada) {
            avisos.add("O km de saída (" + NumeroBr.formatar(usoSaida, 0) + ") é menor que o da entrada ("
                    + NumeroBr.formatar(usoEntrada, 0) + "). Confira se não foi erro de digitação.");
        }
        return avisos;
    }

    /** O último dia da garantia. */
    public LocalDate garantiaVence() {
        return garantia.vence(quando.toLocalDate());
    }
}
