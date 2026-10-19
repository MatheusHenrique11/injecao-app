package br.com.calibra.core.orcamento;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import br.com.calibra.core.oficina.Garantia;

/**
 * Um serviço do catálogo de serviços (SPEC 6.5 e 6.6.1): a mão de obra padrão da oficina, com o tempo de
 * referência. O preço, quando há, é o preço de referência do item do catálogo (a informação comum de todo item).
 *
 * <p>A categoria, a descrição, o "exige o veículo" e a garantia entraram na Fase 3B. O serviço gravado antes deles
 * (no 0.3.0) é lido como Outro, exigindo o veículo, sem descrição e com a garantia da OS (decisão 215).
 *
 * @param horas o tempo padrão; null = não informado (o serviço precisa ter preço)
 * @param categoria null = {@link CategoriaServico#OUTRO}
 * @param descricaoPadrao o texto da linha do orçamento; vazio = o nome
 * @param exigeEntradaDoVeiculo false no serviço de bancada, feito sem o veículo (ex.: confecção de chicote);
 *        null = sim
 * @param garantiaDias a garantia própria do serviço, em dias (0 = sem garantia); null = a garantia da OS
 */
public record ServicoPadrao(String nome, Double horas, String fonte, LocalDate data, CategoriaServico categoria,
        String descricaoPadrao, Boolean exigeEntradaDoVeiculo, Integer garantiaDias) {

    public ServicoPadrao {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(fonte, "fonte");
        categoria = categoria == null ? CategoriaServico.OUTRO : categoria;
        descricaoPadrao = descricaoPadrao == null ? "" : descricaoPadrao.strip();
        exigeEntradaDoVeiculo = exigeEntradaDoVeiculo == null ? Boolean.TRUE : exigeEntradaDoVeiculo;
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (horas != null && !(horas > 0)) {
            erros.add("As horas do serviço devem ser maiores que zero.");
        }
        if (garantiaDias != null && (garantiaDias < 0 || garantiaDias > Garantia.MAXIMO_DIAS)) {
            erros.add("A garantia do serviço vai de 0 (sem garantia) a " + Garantia.MAXIMO_DIAS
                    + " dias; em branco, vale a garantia da OS.");
        }
        return erros;
    }

    /** O texto da linha do orçamento: a descrição padrão ou, sem ela, o nome. */
    public String textoNoOrcamento() {
        return descricaoPadrao.isEmpty() ? nome : descricaoPadrao;
    }
}
