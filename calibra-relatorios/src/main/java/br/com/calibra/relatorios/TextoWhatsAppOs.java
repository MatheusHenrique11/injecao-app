package br.com.calibra.relatorios;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Os textos da OS para colar no WhatsApp (bloco 3.3), com o *negrito* do WhatsApp. O app só copia para a área de
 * transferência: quem envia é a pessoa (regra 2.1, zero rede).
 */
public final class TextoWhatsAppOs {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private TextoWhatsAppOs() {
    }

    /** OS aberta: o número, o veículo, a chegada (ou o orçamento a fazer), o serviço pedido, a previsão e o contato. */
    public static String osAberta(DadosOs dados) {
        List<String> linhas = new ArrayList<>();
        linhas.add("*" + dados.numero() + "*" + TextoOficina.noTitulo(dados.oficina()));
        linhas.add("Veículo: " + dados.veiculo());
        linhas.add(dados.entrada() == null ? "Orçamento a fazer: o veículo ainda não chegou."
                : "Chegou em " + dados.entrada().quando().format(TextoOrcamento.DIA) + " às "
                        + dados.entrada().quando().format(HORA) + ".");
        if (!dados.servicoPedido().isEmpty()) {
            linhas.add("Serviço pedido: " + dados.servicoPedido());
        }
        if (dados.previsao() != null) {
            linhas.add("Previsão de entrega: " + dados.previsao().format(TextoOrcamento.DIA));
        }
        contato(linhas, dados);
        return String.join("\n", linhas);
    }

    /** Pronto para retirar: o número, o veículo, o valor, o recebido, o que falta (ou o que passou) e o contato. */
    public static String prontaParaRetirar(DadosOs dados) {
        List<String> linhas = new ArrayList<>();
        linhas.add("*" + dados.numero() + ": pronto para retirar*" + TextoOficina.noTitulo(dados.oficina()));
        linhas.add("Veículo: " + dados.veiculo());
        if (dados.valor() != null) {
            linhas.add("Valor: " + dados.valor().formatar());
            if (dados.recebido().centavos() > 0) {
                linhas.add("Recebido: " + dados.recebido().formatar());
            }
            if (dados.falta().centavos() > 0) {
                linhas.add("*Falta: " + dados.falta().formatar() + "*");
            } else if (dados.aMais().centavos() > 0) {
                linhas.add("Tudo pago. Recebido a mais: " + dados.aMais().formatar() + ".");
            } else {
                linhas.add("Tudo pago.");
            }
        }
        contato(linhas, dados);
        return String.join("\n", linhas);
    }

    private static void contato(List<String> linhas, DadosOs dados) {
        String contato = TextoOficina.contato(dados.oficina());
        if (!contato.isEmpty()) {
            linhas.add("");
            linhas.add(contato);
        }
    }
}
