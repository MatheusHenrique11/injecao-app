package br.com.calibra.dados.catalogo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.orcamento.Dinheiro;

/**
 * O que todo item de catálogo tem além dos dados técnicos (SPEC 8.5 e regra 2.7): de onde veio e quando, a
 * marca "a verificar" e o preço de referência, que vai para o orçamento.
 *
 * @param fonte obrigatória (manual, site do fabricante, medição na bancada...)
 * @param data data da informação; opcional
 * @param precoReferencia opcional
 */
public record InfoCatalogo(String nome, String fonte, LocalDate data, boolean aVerificar, Dinheiro precoReferencia,
        String observacoes) {

    public InfoCatalogo {
        nome = nome == null ? "" : nome.strip();
        fonte = fonte == null ? "" : fonte.strip();
        observacoes = observacoes == null ? "" : observacoes.strip();
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (nome.isEmpty()) {
            erros.add("Informe o nome do item.");
        }
        if (fonte.isEmpty()) {
            erros.add("Informe a fonte dos dados (manual, site do fabricante, medição na bancada...).");
        }
        return erros;
    }
}
