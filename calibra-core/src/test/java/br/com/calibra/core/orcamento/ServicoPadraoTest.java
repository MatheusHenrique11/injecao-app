package br.com.calibra.core.orcamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.orcamento.ServicosSugeridos.Sugestao;

/** O serviço do catálogo (SPEC 6.6.1): horas, categoria, descrição no orçamento, garantia e os sugeridos. */
class ServicoPadraoTest {

    private static final String FONTE = "Tabela da oficina";

    private static ServicoPadrao servico(Double horas, Integer garantiaDias) {
        return new ServicoPadrao("Revisão", horas, FONTE, null, CategoriaServico.OUTRO, "", true, garantiaDias, null);
    }

    @Test
    void horasMaioresQueZero() {
        assertEquals(List.of(), servico(null, null).validar());
        assertEquals(List.of(), servico(0.5, null).validar());
        for (double horas : new double[] {0, -1, Double.NaN}) {
            assertEquals(List.of("As horas do serviço devem ser maiores que zero."), servico(horas, null).validar(),
                    String.valueOf(horas));
        }
    }

    @Test
    void garantiaPropriaDeZeroADezAnos() {
        for (int dias : new int[] {0, 90, 3650}) {
            assertEquals(List.of(), servico(1.0, dias).validar(), String.valueOf(dias));
        }
        for (int dias : new int[] {-1, 3651}) {
            assertEquals(List.of("A garantia do serviço vai de 0 (sem garantia) a 3650 dias; em branco, vale a "
                    + "garantia da OS."), servico(1.0, dias).validar(), String.valueOf(dias));
        }
    }

    /** O que o JSON do 0.3.0 não tem chega vazio: Outro, exige o veículo, sem descrição e a garantia da OS. */
    @Test
    void camposVaziosViramOPadrao() {
        ServicoPadrao antigo = new ServicoPadrao("Limpeza de bicos", 1.5, FONTE, null, null, null, null, null, null);
        assertEquals(CategoriaServico.OUTRO, antigo.categoria());
        assertEquals("", antigo.descricaoPadrao());
        assertTrue(antigo.exigeEntradaDoVeiculo());
        assertNull(antigo.garantiaDias());
        assertEquals(new ServicoPadrao("Limpeza de bicos", 1.5, FONTE, null, CategoriaServico.OUTRO, "", true, null,
                null),
                antigo, "igual ao serviço salvo com os valores padrão: salvar sem mudar não cria versão nova");
    }

    @Test
    void descricaoEmBrancoEntraONomeNoOrcamento() {
        assertEquals("Revisão", servico(1.0, null).textoNoOrcamento());
        ServicoPadrao comDescricao = new ServicoPadrao("Troca de alto-falantes", 1.0, FONTE, null, CategoriaServico.SOM,
                "  Troca do par de alto-falantes dianteiros, com teste de som  ", true, null, null);
        assertEquals("Troca do par de alto-falantes dianteiros, com teste de som", comDescricao.textoNoOrcamento());
        assertEquals("Revisão", new ServicoPadrao("Revisão", 1.0, FONTE, null, null, "   ", null, null, null)
                .textoNoOrcamento(), "só espaços conta como em branco");
    }

    /** A lista da SPEC 6.6.1: 24 serviços, sem horas e sem preço, e só a confecção de chicote é de bancada. */
    @Test
    void servicosSugeridosDaSpec() {
        List<Sugestao> todos = ServicosSugeridos.todos();
        Map<CategoriaServico, Integer> porCategoria = new LinkedHashMap<>();
        todos.forEach(s -> porCategoria.merge(s.categoria(), 1, Integer::sum));
        assertEquals(Map.of(CategoriaServico.SOM, 5, CategoriaServico.VIDROS_TRAVAS, 5, CategoriaServico.PARTIDA_CARGA,
                3, CategoriaServico.ILUMINACAO, 4, CategoriaServico.DIAGNOSTICO_ELETRICO, 3,
                CategoriaServico.INJECAO_PROGRAMAVEL, 4), porCategoria);
        assertEquals(24, todos.size());
        assertEquals(List.of(CategoriaServico.SOM, CategoriaServico.VIDROS_TRAVAS, CategoriaServico.PARTIDA_CARGA,
                CategoriaServico.ILUMINACAO, CategoriaServico.DIAGNOSTICO_ELETRICO,
                CategoriaServico.INJECAO_PROGRAMAVEL), List.copyOf(porCategoria.keySet()), "agrupados, na ordem da SPEC");

        Set<String> nomes = new HashSet<>();
        for (Sugestao s : todos) {
            assertTrue(nomes.add(s.nome().toLowerCase(Locale.ROOT)), "nome repetido: " + s.nome());
            assertTrue(Character.isUpperCase(s.nome().charAt(0)), s.nome());
        }
        assertEquals(List.of("Confecção de chicote para injeção programável"), todos.stream()
                .filter(s -> !s.exigeEntradaDoVeiculo()).map(Sugestao::nome).toList(), "só o chicote é de bancada");
        assertFalse(porCategoria.containsKey(CategoriaServico.PREPARACAO));
        assertFalse(porCategoria.containsKey(CategoriaServico.OUTRO));
    }

    /**
     * Bloco 3B.3: cada sugerido já vem com o modelo de checklist da categoria dele; na injeção programável, só a
     * confecção de chicote e a instalação, cada uma com o seu.
     */
    @Test
    void sugeridosComOChecklistDaCategoria() {
        Map<CategoriaServico, String> daCategoria = Map.of(CategoriaServico.SOM, ServicosSugeridos.CHECKLIST_SOM,
                CategoriaServico.VIDROS_TRAVAS, ServicosSugeridos.CHECKLIST_VIDRO_E_TRAVA,
                CategoriaServico.PARTIDA_CARGA, ServicosSugeridos.CHECKLIST_PARTIDA_E_ALTERNADOR,
                CategoriaServico.ILUMINACAO, ServicosSugeridos.CHECKLIST_ILUMINACAO,
                CategoriaServico.DIAGNOSTICO_ELETRICO, ServicosSugeridos.CHECKLIST_DIAGNOSTICO);
        for (Sugestao s : ServicosSugeridos.todos()) {
            if (s.categoria() != CategoriaServico.INJECAO_PROGRAMAVEL) {
                assertEquals(daCategoria.get(s.categoria()), s.checklist(), s.nome());
            }
        }
        Map<String, String> injecao = new LinkedHashMap<>();
        ServicosSugeridos.todos().stream().filter(s -> s.categoria() == CategoriaServico.INJECAO_PROGRAMAVEL)
                .forEach(s -> injecao.put(s.nome(), s.checklist() == null ? "sem" : s.checklist()));
        assertEquals(Map.of("Confecção de chicote para injeção programável", ServicosSugeridos.CHECKLIST_CHICOTE,
                "Instalação de injeção programável", ServicosSugeridos.CHECKLIST_INSTALACAO_INJECAO,
                "Revisão ou correção de chicote de injeção programável", "sem",
                "Diagnóstico de injeção programável (falhas, leitura de sensores e log)", "sem"), injecao,
                "a revisão do chicote e o diagnóstico ficam sem: o dono escolhe");
    }

    /** O serviço gravado antes do bloco 3B.3 (sem o campo) fica sem checklist. */
    @Test
    void servicoSemChecklist() {
        assertNull(servico(1.0, null).checklistModeloId());
        assertEquals(31L, new ServicoPadrao("Som", 4.0, FONTE, null, CategoriaServico.SOM, "", true, null, 31L)
                .checklistModeloId());
    }

    @Test
    void rotuloDasCategorias() {
        assertEquals(List.of("Preparação", "Som", "Vidros e travas", "Partida e carga", "Iluminação",
                "Diagnóstico elétrico", "Injeção programável", "Outro"),
                List.of(CategoriaServico.values()).stream().map(CategoriaServico::rotulo).toList());
    }
}
