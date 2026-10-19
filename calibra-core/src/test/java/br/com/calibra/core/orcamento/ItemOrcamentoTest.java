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

    /** Fase 3B (SPEC 6.6.4): o item do catálogo entra como peça, 1 × a unidade dele, pelo preço de referência. */
    @Test
    void itemDoCatalogoEntraComoPecaPeloPreco() {
        ItemOrcamento rele = ItemOrcamento.doItem("Relé 12 V 40 A", "un", Dinheiro.ler("18,50"), 9, 3);
        assertEquals(TipoItem.PECA, rele.tipo());
        assertEquals(BigDecimal.ONE, rele.quantidade());
        assertEquals("un", rele.unidade());
        assertEquals(Dinheiro.ler("18,50"), rele.total());
        assertEquals(9L, rele.catalogoItemId());
        assertEquals(3, rele.catalogoVersao());
        assertNull(ItemOrcamento.doItem("Conector", "un", null, 9, 1).valorUnitario(), "sem preço: em branco");
    }

    /** A quantidade da linha do kit multiplica a linha: 2 × um serviço de 0,5 h = 1 h; 3 × 1 m de cabo = 3 m. */
    @Test
    void quantidadeDoKitMultiplicaALinha() {
        ItemOrcamento duasHoras = ItemOrcamento.doServico("Troca de alto-falante", 0.5, null, HORA, 7, 1)
                .multiplicarQuantidade(BigDecimal.valueOf(2));
        assertEquals(BigDecimal.ONE, duasHoras.quantidade());
        assertEquals(Dinheiro.ler("180,00"), duasHoras.total());
        ItemOrcamento doisServicos = ItemOrcamento.doServico("Instalação de módulo", null, Dinheiro.ler("150,00"), HORA,
                7, 1).multiplicarQuantidade(BigDecimal.valueOf(2));
        assertEquals(new BigDecimal("2"), doisServicos.quantidade());
        assertEquals("serviço", doisServicos.unidade());
        assertEquals(Dinheiro.ler("300,00"), doisServicos.total());
        assertEquals(Dinheiro.ler("54,00"), ItemOrcamento.doItem("Cabo 6 mm²", "m", Dinheiro.ler("18,00"), 9, 1)
                .multiplicarQuantidade(new BigDecimal("3")).total());
    }

    /** Teste nº 12 da Fase 3B: a peça fornecida pelo cliente entra com valor zero e não soma no total. */
    @Test
    void fornecidoPeloClienteEntraComValorZeroENaoSoma() {
        ItemOrcamento ecu = new ItemOrcamento(TipoItem.PECA, "ECU FuelTech FT450", BigDecimal.ONE, "un",
                Dinheiro.ler("4.900,00"), 11L, 1, true);
        assertEquals(Dinheiro.ZERO, ecu.valorUnitario(), "a linha do cliente fica com valor zero");
        assertEquals(Dinheiro.ZERO, ecu.total());
        assertEquals(List.of(), ecu.validar(1));
        Orcamento orcamento = new Orcamento(List.of(ecu, ItemOrcamento.digitado(TipoItem.SERVICO,
                "Instalação de injeção programável", BigDecimal.valueOf(8), "h", HORA)), Desconto.NENHUM, 1, "", 15,
                "", "");
        assertEquals(Dinheiro.ZERO, orcamento.totalPecas());
        assertEquals(Dinheiro.ler("1.440,00"), orcamento.total(), "só o serviço: 8 h × R$ 180");

        ItemOrcamento servico = new ItemOrcamento(TipoItem.SERVICO, "Mão de obra", BigDecimal.ONE, "h", HORA, null,
                null, true);
        assertEquals(List.of("Item 2 (Mão de obra): só peça pode ser fornecida pelo cliente."), servico.validar(2));
    }
}
