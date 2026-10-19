package br.com.calibra.core.orcamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

/** O serviço do catálogo no orçamento (bloco 3.3): pelo preço próprio ou pelas horas. Valores fictícios. */
class ItemOrcamentoTest {

    private static final Dinheiro HORA = Dinheiro.ler("180,00");

    @Test
    void servicoComPrecoEntraUmaVezPeloPreco() {
        ItemOrcamento item = ItemOrcamento.doServico("Limpeza de bicos", 1.5, Dinheiro.ler("350,00"), HORA, 7, 2);
        assertEquals(TipoItem.SERVICO, item.tipo());
        assertEquals("Limpeza de bicos", item.descricao());
        assertEquals(BigDecimal.ONE, item.quantidade());
        assertEquals("serviço", item.unidade());
        assertEquals(Dinheiro.ler("350,00"), item.total(), "o preço vale mais que as horas");
        assertEquals(7L, item.catalogoItemId());
        assertEquals(2, item.catalogoVersao());
    }

    @Test
    void servicoSemPrecoEntraPelasHorasVezesOValorDaHora() {
        ItemOrcamento item = ItemOrcamento.doServico("Revisão da injeção", 2.5, null, HORA, 7, 1);
        assertEquals(new BigDecimal("2.5"), item.quantidade());
        assertEquals("h", item.unidade());
        assertEquals(HORA, item.valorUnitario());
        assertEquals(Dinheiro.ler("450,00"), item.total());
        assertEquals(List.of(), item.validar(1));
    }

    @Test
    void semValorDaHoraOValorFicaEmBrancoParaPreencher() {
        ItemOrcamento item = ItemOrcamento.doServico("Revisão da injeção", 2.0, null, null, 7, 1);
        assertEquals(new BigDecimal("2"), item.quantidade());
        assertNull(item.valorUnitario());
        assertEquals(List.of("Item 1 (Revisão da injeção): informe o valor."), item.validar(1));
    }

    @Test
    void semPrecoESemHorasEntraUmServicoParaPreencher() {
        ItemOrcamento item = ItemOrcamento.doServico("Serviço antigo", null, null, HORA, 7, 1);
        assertEquals(BigDecimal.ONE, item.quantidade());
        assertEquals("serviço", item.unidade());
        assertNull(item.valorUnitario());
    }
}
