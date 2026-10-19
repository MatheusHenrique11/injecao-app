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
        return new ServicoPadrao("Revisão", horas, FONTE, null, CategoriaServico.OUTRO, "", true, garantiaDias);
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
        ServicoPadrao antigo = new ServicoPadrao("Limpeza de bicos", 1.5, FONTE, null, null, null, null, null);
        assertEquals(CategoriaServico.OUTRO, antigo.categoria());
        assertEquals("", antigo.descricaoPadrao());
        assertTrue(antigo.exigeEntradaDoVeiculo());
        assertNull(antigo.garantiaDias());
        assertEquals(new ServicoPadrao("Limpeza de bicos", 1.5, FONTE, null, CategoriaServico.OUTRO, "", true, null),
                antigo, "igual ao serviço salvo com os valores padrão: salvar sem mudar não cria versão nova");
    }

    @Test
    void descricaoEmBrancoEntraONomeNoOrcamento() {
        assertEquals("Revisão", servico(1.0, null).textoNoOrcamento());
        ServicoPadrao comDescricao = new ServicoPadrao("Troca de alto-falantes", 1.0, FONTE, null, CategoriaServico.SOM,
                "  Troca do par de alto-falantes dianteiros, com teste de som  ", true, null);
        assertEquals("Troca do par de alto-falantes dianteiros, com teste de som", comDescricao.textoNoOrcamento());
        assertEquals("Revisão", new ServicoPadrao("Revisão", 1.0, FONTE, null, null, "   ", null, null)
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

    @Test
    void rotuloDasCategorias() {
        assertEquals(List.of("Preparação", "Som", "Vidros e travas", "Partida e carga", "Iluminação",
                "Diagnóstico elétrico", "Injeção programável", "Outro"),
                List.of(CategoriaServico.values()).stream().map(CategoriaServico::rotulo).toList());
    }
}
