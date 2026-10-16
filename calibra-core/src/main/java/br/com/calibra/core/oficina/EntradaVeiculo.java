package br.com.calibra.core.oficina;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A chegada do veículo na oficina (SPEC 6.5). Pode ser registrada depois da abertura da OS, quando o orçamento vem
 * antes do veículo; a execução exige a entrada.
 *
 * @param uso km nos terrestres, horas de motor nos náuticos; null = não anotado
 * @param combustivel null = não anotado (ex.: embarcação)
 */
public record EntradaVeiculo(LocalDateTime quando, Double uso, NivelCombustivel combustivel, Vistoria vistoria) {

    public EntradaVeiculo {
        vistoria = vistoria == null ? Vistoria.VAZIA : vistoria;
    }

    /**
     * Tudo o que está errado, em português; vazio = pode gravar. {@code agora} vem de fora: o núcleo não lê o
     * relógio.
     */
    public List<String> validar(LocalDateTime agora) {
        List<String> erros = new ArrayList<>();
        if (quando == null) {
            erros.add("Informe a data e a hora da entrada.");
        } else if (quando.isAfter(agora.plusMinutes(1))) {
            erros.add("A data e a hora da entrada não podem estar no futuro.");
        }
        if (uso != null && !(uso >= 0)) {
            erros.add("O km (ou as horas de motor) da entrada não pode ser negativo.");
        }
        erros.addAll(vistoria.validar());
        return erros;
    }
}
