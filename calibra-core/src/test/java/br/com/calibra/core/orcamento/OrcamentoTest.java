package br.com.calibra.core.orcamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.util.NumeroBr;

/** Orçamento em centavos: itens, totais, desconto, validade, status e número. Valores fictícios. */
class OrcamentoTest {

    private static ItemOrcamento peca(String descricao, String quantidade, String valor) {
        return ItemOrcamento.digitado(TipoItem.PECA, descricao, NumeroBr.lerDecimal(quantidade), "un",
                Dinheiro.ler(valor));
    }

    private static ItemOrcamento servico(String descricao, String quantidade, String valor) {
        return ItemOrcamento.digitado(TipoItem.SERVICO, descricao, NumeroBr.lerDecimal(quantidade), "h",
                Dinheiro.ler(valor));
    }

    private static Orcamento orcamento(Desconto desconto, ItemOrcamento... itens) {
        return new Orcamento(List.of(itens), desconto, 10, "a partir da aprovação", 15, "50% na aprovação", "");
    }

    /** 4 bicos × 450,00 + bomba 890,00 + 2,5 h × 180,00 de mão de obra, com 10% de desconto. */
    @Test
    void totaisEmCentavos() {
        Orcamento o = orcamento(Desconto.emPercentual(BigDecimal.TEN), peca("Bico teste", "4", "450,00"),
                peca("Bomba teste", "1", "890,00"), servico("Mão de obra", "2,5", "180,00"));
        assertEquals(Dinheiro.ler("2.690,00"), o.totalPecas());
        assertEquals(Dinheiro.ler("450,00"), o.totalServicos());
        assertEquals(Dinheiro.ler("3.140,00"), o.subtotal());
        assertEquals(Dinheiro.ler("314,00"), o.valorDesconto());
        assertEquals(Dinheiro.ler("2.826,00"), o.total());
        assertTrue(o.validar().isEmpty(), o.validar()::toString);

        Orcamento emValor = orcamento(Desconto.emValor(Dinheiro.ler("140,00")), peca("Bico teste", "4", "450,00"),
                peca("Bomba teste", "1", "890,00"), servico("Mão de obra", "2,5", "180,00"));
        assertEquals(Dinheiro.ler("3.000,00"), emValor.total());
    }

    /** Meio centavo sobe: 1/3 de hora a 10,00 dá 3,33; 7,5% de 99,99 dá 7,50. */
    @Test
    void arredondaNoCentavo() {
        assertEquals(new Dinheiro(333), Dinheiro.ler("10,00").vezes(new BigDecimal("0.333")));
        assertEquals(new Dinheiro(750), Dinheiro.ler("99,99").percentual(new BigDecimal("7.5")));
        assertEquals(new Dinheiro(5), new Dinheiro(1).vezes(new BigDecimal("4.5")), "4,5 centavos viram 5");
        assertEquals(new BigDecimal("2.5"), NumeroBr.lerDecimal("2,5"));
        assertEquals(new BigDecimal("1050"), NumeroBr.lerDecimal("1.050"));
        assertThrows(NumberFormatException.class, () -> NumeroBr.lerDecimal("2.5"));
        assertEquals(new BigDecimal("2"), peca("x", "2,00", "1,00").quantidade(), "a quantidade guarda o número limpo");
    }

    @Test
    void validadeViraUmaData() {
        Orcamento o = orcamento(Desconto.NENHUM, peca("Bico teste", "4", "450,00"));
        assertEquals(LocalDate.of(2026, 10, 28), o.validoAte(LocalDate.of(2026, 10, 13)));
    }

    @Test
    void oQueFaltaOuEstaErrado() {
        assertEquals(List.of("O orçamento precisa de pelo menos um item (peça ou serviço)."),
                orcamento(Desconto.NENHUM).validar());
        ItemOrcamento semNada = new ItemOrcamento(TipoItem.PECA, " ", BigDecimal.ZERO, "un", null, null, null);
        assertEquals(List.of("Item 1: escreva a descrição.", "Item 1: a quantidade deve ser maior que zero.",
                "Item 1: informe o valor."), orcamento(Desconto.NENHUM, semNada).validar());

        Orcamento descontoDemais = orcamento(Desconto.emValor(Dinheiro.ler("500,00")), peca("Bico teste", "1", "450,00"));
        assertEquals(List.of("O desconto (R$ 500,00) é maior que o total de peças e serviços (R$ 450,00)."),
                descontoDemais.validar());
        assertEquals(List.of("O desconto em percentual fica entre 0 e 100%."), orcamento(
                Desconto.emPercentual(new BigDecimal("120")), peca("Bico teste", "1", "450,00")).validar());
        Orcamento semValidade = new Orcamento(List.of(peca("Bico teste", "1", "450,00")), Desconto.NENHUM, -1, "", 0,
                "", "");
        assertEquals(List.of("O prazo não pode ser negativo.", "A validade do orçamento é de pelo menos 1 dia."),
                semValidade.validar());
        assertThrows(IllegalArgumentException.class, () -> new Desconto(Dinheiro.ZERO, BigDecimal.ONE));
    }

    @Test
    void statusSoAndaParaAFrenteESoORascunhoMuda() {
        assertEquals(Set.of(StatusOrcamento.ENVIADO), StatusOrcamento.RASCUNHO.proximos());
        assertTrue(StatusOrcamento.ENVIADO.podeIrPara(StatusOrcamento.APROVADO));
        assertTrue(StatusOrcamento.ENVIADO.podeIrPara(StatusOrcamento.RECUSADO));
        assertFalse(StatusOrcamento.RASCUNHO.podeIrPara(StatusOrcamento.APROVADO), "aprovar só depois de enviar");
        assertFalse(StatusOrcamento.APROVADO.podeIrPara(StatusOrcamento.RECUSADO));
        assertTrue(StatusOrcamento.APROVADO.aceitaRevisao());
        assertFalse(StatusOrcamento.RASCUNHO.aceitaRevisao(), "o rascunho se edita, não precisa de revisão");
        assertFalse(StatusOrcamento.SUBSTITUIDO.aceitaRevisao());
        assertTrue(StatusOrcamento.RASCUNHO.editavel());
        assertFalse(StatusOrcamento.APROVADO.editavel(), "aprovado não muda");
    }

    @Test
    void padroesDoOrcamentoNovo() {
        PadroesOrcamento padrao = PadroesOrcamento.padrao();
        assertTrue(padrao.validar().isEmpty());
        Orcamento novo = padrao.orcamento(List.of(peca("Bico teste", "4", "450,00")));
        assertEquals(10, novo.prazoDiasUteis());
        assertEquals(15, novo.validadeDias());
        assertTrue(novo.desconto().nenhum());
        PadroesOrcamento errados = new PadroesOrcamento(Desconto.emPercentual(new BigDecimal("150")), -1, "", 0, "",
                Dinheiro.ler("-1,00"));
        assertEquals(List.of("O prazo padrão não pode ser negativo.", "A validade padrão é de pelo menos 1 dia.",
                "O valor da hora não pode ser negativo.", "O desconto padrão em percentual fica entre 0 e 100%."),
                errados.validar());
    }

    @Test
    void numeroComAnoESequencia() {
        NumeroOrcamento primeiro = new NumeroOrcamento(2026, 7, 1);
        assertEquals("ORC-2026-0007", primeiro.texto());
        assertEquals("ORC-2026-0007", primeiro.comRevisao());
        assertEquals("ORC-2026-0007 rev. 2", primeiro.proximaRevisao().comRevisao());
        assertEquals("ORC-2027-12345", new NumeroOrcamento(2027, 12345, 1).texto(), "passa de 9.999 sem perder");
        assertThrows(IllegalArgumentException.class, () -> new NumeroOrcamento(2026, 0, 1));
    }
}
