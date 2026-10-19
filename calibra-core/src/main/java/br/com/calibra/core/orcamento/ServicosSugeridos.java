package br.com.calibra.core.orcamento;

import java.util.List;
import java.util.Objects;

/**
 * A lista de partida dos serviços de auto elétrica e de injeção programável (SPEC 6.6.1), que o dono escolhe em
 * "Serviços sugeridos…". Vem sem horas e sem preço: quem preenche é o dono, porque o catálogo exige um dos dois e o
 * app não inventa tempo de serviço (decisão 216).
 */
public final class ServicosSugeridos {

    /**
     * Um serviço sugerido.
     *
     * @param exigeEntradaDoVeiculo false no serviço de bancada (a confecção de chicote, SPEC 6.6.5)
     */
    public record Sugestao(CategoriaServico categoria, String nome, boolean exigeEntradaDoVeiculo) {

        public Sugestao {
            Objects.requireNonNull(categoria, "categoria");
            Objects.requireNonNull(nome, "nome");
        }
    }

    private static final List<Sugestao> TODOS = List.of(
            noVeiculo(CategoriaServico.SOM, "Instalação de som completa"),
            noVeiculo(CategoriaServico.SOM, "Troca de alto-falantes"),
            noVeiculo(CategoriaServico.SOM, "Instalação de módulo de potência"),
            noVeiculo(CategoriaServico.SOM, "Instalação de subwoofer e caixa"),
            noVeiculo(CategoriaServico.SOM, "Instalação de multimídia ou central"),
            noVeiculo(CategoriaServico.VIDROS_TRAVAS, "Troca de motor de vidro elétrico"),
            noVeiculo(CategoriaServico.VIDROS_TRAVAS, "Troca de regulador (máquina de vidro)"),
            noVeiculo(CategoriaServico.VIDROS_TRAVAS, "Instalação de kit de vidro elétrico"),
            noVeiculo(CategoriaServico.VIDROS_TRAVAS, "Instalação de kit de trava elétrica"),
            noVeiculo(CategoriaServico.VIDROS_TRAVAS, "Reparo de chave ou botoeira"),
            noVeiculo(CategoriaServico.PARTIDA_CARGA, "Reparo ou troca de motor de partida"),
            noVeiculo(CategoriaServico.PARTIDA_CARGA, "Reparo ou troca de alternador"),
            noVeiculo(CategoriaServico.PARTIDA_CARGA, "Teste de bateria e do sistema de carga"),
            noVeiculo(CategoriaServico.ILUMINACAO, "Instalação de LED ou xenon em farol"),
            noVeiculo(CategoriaServico.ILUMINACAO, "Instalação de farol auxiliar (milha)"),
            noVeiculo(CategoriaServico.ILUMINACAO, "Instalação de DRL ou setas sequenciais"),
            noVeiculo(CategoriaServico.ILUMINACAO, "Troca ou adaptação de farol e projetor"),
            noVeiculo(CategoriaServico.DIAGNOSTICO_ELETRICO, "Diagnóstico de falha elétrica"),
            noVeiculo(CategoriaServico.DIAGNOSTICO_ELETRICO, "Diagnóstico de injeção eletrônica com scanner"),
            noVeiculo(CategoriaServico.DIAGNOSTICO_ELETRICO, "Medição de consumo em repouso (fuga de corrente)"),
            new Sugestao(CategoriaServico.INJECAO_PROGRAMAVEL, "Confecção de chicote para injeção programável", false),
            noVeiculo(CategoriaServico.INJECAO_PROGRAMAVEL, "Instalação de injeção programável"),
            noVeiculo(CategoriaServico.INJECAO_PROGRAMAVEL, "Revisão ou correção de chicote de injeção programável"),
            noVeiculo(CategoriaServico.INJECAO_PROGRAMAVEL,
                    "Diagnóstico de injeção programável (falhas, leitura de sensores e log)"));

    private ServicosSugeridos() {
    }

    /** Todos, na ordem da SPEC (agrupados por categoria). */
    public static List<Sugestao> todos() {
        return TODOS;
    }

    private static Sugestao noVeiculo(CategoriaServico categoria, String nome) {
        return new Sugestao(categoria, nome, true);
    }
}
