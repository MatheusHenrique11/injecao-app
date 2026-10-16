package br.com.calibra.core.oficina;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.orcamento.Dinheiro;

/**
 * Um recebimento da OS (ex.: o sinal na aprovação e o resto na entrega). Recebimento errado não se apaga: é
 * cancelado, com o motivo, e deixa de contar.
 */
public record Pagamento(LocalDateTime recebidoEm, FormaPagamento forma, Dinheiro valor, String observacao) {

    public Pagamento {
        observacao = observacao == null ? "" : observacao.strip();
    }

    /** Tudo o que está errado, em português; vazio = pode gravar. */
    public List<String> validar(LocalDateTime agora) {
        List<String> erros = new ArrayList<>();
        if (recebidoEm == null) {
            erros.add("Informe a data e a hora do recebimento.");
        } else if (recebidoEm.isAfter(agora.plusMinutes(1))) {
            erros.add("A data e a hora do recebimento não podem estar no futuro.");
        }
        if (forma == null) {
            erros.add("Escolha a forma de pagamento.");
        }
        if (valor == null || valor.compareTo(Dinheiro.ZERO) <= 0) {
            erros.add("O valor recebido deve ser maior que zero.");
        }
        return erros;
    }
}
