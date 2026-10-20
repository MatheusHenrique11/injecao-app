package br.com.calibra.core.orcamento;

import java.util.List;
import java.util.Objects;

/**
 * A lista de partida dos serviços de auto elétrica e de injeção programável (SPEC 6.6.1), que o dono escolhe em
 * "Serviços sugeridos…". Vem sem horas e sem preço: quem preenche é o dono, porque o catálogo exige um dos dois e o
 * app não inventa tempo de serviço (decisão 216). Cada um já vem com o modelo de checklist da categoria dele (bloco
 * 3B.3), pela chave da semente do modelo.
 */
public final class ServicosSugeridos {

    /** As chaves das sementes dos 7 modelos de checklist da SPEC 6.6.2 (migração V9). */
    public static final String CHECKLIST_SOM = "checklist:som";
    public static final String CHECKLIST_VIDRO_E_TRAVA = "checklist:vidro-e-trava";
    public static final String CHECKLIST_PARTIDA_E_ALTERNADOR = "checklist:partida-e-alternador";
    public static final String CHECKLIST_ILUMINACAO = "checklist:iluminacao";
    public static final String CHECKLIST_CHICOTE = "checklist:chicote-injecao-programavel";
    public static final String CHECKLIST_INSTALACAO_INJECAO = "checklist:instalacao-injecao-programavel";
    public static final String CHECKLIST_DIAGNOSTICO = "checklist:diagnostico-eletrico";

    /**
     * Um serviço sugerido.
     *
     * @param exigeEntradaDoVeiculo false no serviço de bancada (a confecção de chicote, SPEC 6.6.5)
     * @param checklist a chave da semente do modelo de checklist do serviço; null = sem (o dono escolhe)
     */
    public record Sugestao(CategoriaServico categoria, String nome, boolean exigeEntradaDoVeiculo, String checklist) {

        public Sugestao {
            Objects.requireNonNull(categoria, "categoria");
            Objects.requireNonNull(nome, "nome");
        }
    }

    private static final List<Sugestao> TODOS = List.of(
            noVeiculo(CategoriaServico.SOM, "Instalação de som completa", CHECKLIST_SOM),
            noVeiculo(CategoriaServico.SOM, "Troca de alto-falantes", CHECKLIST_SOM),
            noVeiculo(CategoriaServico.SOM, "Instalação de módulo de potência", CHECKLIST_SOM),
            noVeiculo(CategoriaServico.SOM, "Instalação de subwoofer e caixa", CHECKLIST_SOM),
            noVeiculo(CategoriaServico.SOM, "Instalação de multimídia ou central", CHECKLIST_SOM),
            noVeiculo(CategoriaServico.VIDROS_TRAVAS, "Troca de motor de vidro elétrico", CHECKLIST_VIDRO_E_TRAVA),
            noVeiculo(CategoriaServico.VIDROS_TRAVAS, "Troca de regulador (máquina de vidro)", CHECKLIST_VIDRO_E_TRAVA),
            noVeiculo(CategoriaServico.VIDROS_TRAVAS, "Instalação de kit de vidro elétrico", CHECKLIST_VIDRO_E_TRAVA),
            noVeiculo(CategoriaServico.VIDROS_TRAVAS, "Instalação de kit de trava elétrica", CHECKLIST_VIDRO_E_TRAVA),
            noVeiculo(CategoriaServico.VIDROS_TRAVAS, "Reparo de chave ou botoeira", CHECKLIST_VIDRO_E_TRAVA),
            noVeiculo(CategoriaServico.PARTIDA_CARGA, "Reparo ou troca de motor de partida",
                    CHECKLIST_PARTIDA_E_ALTERNADOR),
            noVeiculo(CategoriaServico.PARTIDA_CARGA, "Reparo ou troca de alternador", CHECKLIST_PARTIDA_E_ALTERNADOR),
            noVeiculo(CategoriaServico.PARTIDA_CARGA, "Teste de bateria e do sistema de carga",
                    CHECKLIST_PARTIDA_E_ALTERNADOR),
            noVeiculo(CategoriaServico.ILUMINACAO, "Instalação de LED ou xenon em farol", CHECKLIST_ILUMINACAO),
            noVeiculo(CategoriaServico.ILUMINACAO, "Instalação de farol auxiliar (milha)", CHECKLIST_ILUMINACAO),
            noVeiculo(CategoriaServico.ILUMINACAO, "Instalação de DRL ou setas sequenciais", CHECKLIST_ILUMINACAO),
            noVeiculo(CategoriaServico.ILUMINACAO, "Troca ou adaptação de farol e projetor", CHECKLIST_ILUMINACAO),
            noVeiculo(CategoriaServico.DIAGNOSTICO_ELETRICO, "Diagnóstico de falha elétrica", CHECKLIST_DIAGNOSTICO),
            noVeiculo(CategoriaServico.DIAGNOSTICO_ELETRICO, "Diagnóstico de injeção eletrônica com scanner",
                    CHECKLIST_DIAGNOSTICO),
            noVeiculo(CategoriaServico.DIAGNOSTICO_ELETRICO, "Medição de consumo em repouso (fuga de corrente)",
                    CHECKLIST_DIAGNOSTICO),
            new Sugestao(CategoriaServico.INJECAO_PROGRAMAVEL, "Confecção de chicote para injeção programável", false,
                    CHECKLIST_CHICOTE),
            noVeiculo(CategoriaServico.INJECAO_PROGRAMAVEL, "Instalação de injeção programável",
                    CHECKLIST_INSTALACAO_INJECAO),
            noVeiculo(CategoriaServico.INJECAO_PROGRAMAVEL, "Revisão ou correção de chicote de injeção programável",
                    null),
            noVeiculo(CategoriaServico.INJECAO_PROGRAMAVEL,
                    "Diagnóstico de injeção programável (falhas, leitura de sensores e log)", null));

    private ServicosSugeridos() {
    }

    /** Todos, na ordem da SPEC (agrupados por categoria). */
    public static List<Sugestao> todos() {
        return TODOS;
    }

    private static Sugestao noVeiculo(CategoriaServico categoria, String nome, String checklist) {
        return new Sugestao(categoria, nome, true, checklist);
    }
}
