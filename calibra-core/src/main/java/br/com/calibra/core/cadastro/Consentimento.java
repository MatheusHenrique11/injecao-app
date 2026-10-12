package br.com.calibra.core.cadastro;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Consentimento do cliente para o uso dos dados pessoais (SPEC 6.1 e regra 2.8): quando foi dado e para
 * quê.
 */
public record Consentimento(LocalDate data, String finalidade) {

    /** Finalidade sugerida na tela; o usuário pode trocar. */
    public static final String FINALIDADE_PADRAO =
            "Orçamento, execução do serviço de preparação e contato sobre o veículo.";

    public Consentimento {
        finalidade = Contato.limpo(finalidade);
    }

    /** Erros de cadastro, em português. {@code hoje} vem de fora: o núcleo não lê o relógio. */
    public List<String> validar(LocalDate hoje) {
        List<String> erros = new ArrayList<>();
        if (data == null) {
            erros.add("Informe a data do consentimento do cliente.");
        } else if (hoje != null && data.isAfter(hoje)) {
            erros.add("A data do consentimento não pode ser depois de hoje.");
        }
        if (finalidade.isEmpty()) {
            erros.add("Informe a finalidade do uso dos dados do cliente.");
        }
        return erros;
    }
}
