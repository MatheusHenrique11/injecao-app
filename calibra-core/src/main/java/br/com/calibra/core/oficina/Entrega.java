package br.com.calibra.core.oficina;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.util.NumeroBr;

/**
 * A entrega do veículo ao cliente (SPEC 6.5): quando, o km (ou as horas de motor) de saída e a garantia, que conta
 * a partir do dia da entrega. Na OS em que o veículo não passou pela oficina (ex.: só a confecção de chicote, Fase 3B),
 * a entrega registra quem retirou, no lugar do km de saída.
 *
 * @param usoSaida null = não anotado
 * @param quemRetirou o nome de quem retirou, na OS sem a entrada do veículo; vazio nas outras
 */
public record Entrega(LocalDateTime quando, Double usoSaida, Garantia garantia, String quemRetirou) {

    public Entrega {
        quemRetirou = quemRetirou == null ? "" : quemRetirou.strip();
    }

    /** A entrega do veículo que passou pela oficina (sem "quem retirou"). */
    public Entrega(LocalDateTime quando, Double usoSaida, Garantia garantia) {
        this(quando, usoSaida, garantia, "");
    }

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
     * O que falta na entrega da OS em que o veículo não passou pela oficina (sem a entrada): quem retirou, e nada de km
     * de saída. Vazio = pode gravar.
     */
    public List<String> validarSemVeiculo() {
        List<String> erros = new ArrayList<>();
        if (quemRetirou.isEmpty()) {
            erros.add("Escreva quem retirou: o veículo não passou pela oficina.");
        }
        if (usoSaida != null) {
            erros.add("O veículo não passou pela oficina: a entrega não tem km (nem horas de motor) de saída.");
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
