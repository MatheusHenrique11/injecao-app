package br.com.calibra.core.oficina;

import java.util.Objects;

/**
 * A garantia de um serviço do orçamento aprovado (SPEC 6.6.1): a própria, do catálogo, ou, sem ela, a da OS. A
 * própria vem da versão do serviço que está no orçamento; a linha digitada usa a da OS.
 *
 * @param servico o texto da linha do orçamento
 * @param diasProprios a garantia própria do serviço (0 = sem garantia); null = a da OS
 */
public record GarantiaDoServico(String servico, Integer diasProprios) {

    public GarantiaDoServico {
        Objects.requireNonNull(servico, "servico");
    }

    /** O serviço tem garantia própria (também quando ela é 0, sem garantia). */
    public boolean propria() {
        return diasProprios != null;
    }

    /** A garantia que vale para o serviço: a própria ou, sem ela, a da OS. */
    public Garantia garantia(Garantia daOs) {
        return diasProprios == null ? Objects.requireNonNull(daOs, "daOs") : new Garantia(diasProprios);
    }
}
