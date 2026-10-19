package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import br.com.calibra.app.casos.FormularioOrcamento.Linha;
import br.com.calibra.app.casos.FormularioOrcamento.Textos;
import br.com.calibra.app.casos.FormularioOrcamento.TextosPadroes;
import br.com.calibra.app.casos.FormularioOrcamento.TipoDesconto;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.orcamento.TipoItem;

/** O orçamento como texto da tela e de volta (bloco 2.5), sem JavaFX. */
class FormularioOrcamentoTest {

    private static Textos textos(List<Linha> linhas, TipoDesconto tipo, String desconto) {
        return new Textos(linhas, tipo, desconto, "10", "a partir da aprovação", "15", "50% na aprovação", "");
    }

    private static Linha bicos(String quantidade, String valor) {
        return new Linha(TipoItem.PECA, "Bico teste 1050", quantidade, "un", valor, 7L, 2);
    }

    @Test
    void leOQueFoiDigitado() {
        FormularioOrcamento.Leitura leitura = FormularioOrcamento.ler(textos(List.of(bicos("4", "450,00"),
                Linha.digitada(TipoItem.SERVICO, "Mão de obra", "2,5", "h", "180")), TipoDesconto.PERCENTUAL, "10"));
        assertTrue(leitura.erros().isEmpty(), leitura.erros()::toString);
        Orcamento o = leitura.orcamento();
        assertEquals(Dinheiro.ler("2.025,00"), o.total(), "(1.800 + 450) − 10%");
        assertEquals(7L, o.itens().getFirst().catalogoItemId(), "a origem no catálogo vai junto");
        assertEquals(2, o.itens().getFirst().catalogoVersao());
        assertEquals(new BigDecimal("2.5"), o.itens().getLast().quantidade());
        assertEquals(Desconto.emPercentual(BigDecimal.TEN), o.desconto());
        assertEquals(10, o.prazoDiasUteis());
        assertEquals("50% na aprovação", o.condicoes());

        Orcamento emReais = FormularioOrcamento.ler(textos(List.of(bicos("4", "450,00")), TipoDesconto.VALOR,
                "100,00")).orcamento();
        assertEquals(Dinheiro.ler("1.700,00"), emReais.total());
        Orcamento semDesconto = FormularioOrcamento.ler(textos(List.of(bicos("4", "450,00")), TipoDesconto.NENHUM,
                "o texto é ignorado")).orcamento();
        assertTrue(semDesconto.desconto().nenhum());
    }

    @Test
    void idaEVolta() {
        Orcamento original = new Orcamento(List.of(
                new ItemOrcamento(TipoItem.PECA, "Bico teste 1050", BigDecimal.valueOf(4), "un", Dinheiro.ler("450,00"),
                        7L, 2, false),
                ItemOrcamento.digitado(TipoItem.SERVICO, "Mão de obra", new BigDecimal("2.5"), "h",
                        Dinheiro.ler("1.180,50"))),
                Desconto.emPercentual(new BigDecimal("7.5")), 12, "a partir da chegada das peças", 10, "à vista",
                "Garantia de 90 dias.");
        Textos textos = FormularioOrcamento.escrever(original);
        assertEquals("1.180,50", textos.linhas().getLast().valor());
        assertEquals("2,5", textos.linhas().getLast().quantidade());
        assertEquals(TipoDesconto.PERCENTUAL, textos.tipoDesconto());
        assertEquals("7,5", textos.desconto());
        assertEquals(original, FormularioOrcamento.ler(textos).orcamento());
    }

    @Test
    void errosDeDigitacaoDeTodasAsLinhas() {
        FormularioOrcamento.Leitura leitura = FormularioOrcamento.ler(new Textos(List.of(bicos("4", ""),
                Linha.digitada(TipoItem.SERVICO, "", "2.5", "h", "180,00")), TipoDesconto.VALOR, "", "", "", "1,5",
                "", ""));
        assertNull(leitura.orcamento());
        assertEquals(List.of(
                "Item 1 (Bico teste 1050): informe o valor.",
                "Item 2: escreva a descrição.",
                "Item 2: quantidade: \"2.5\" não é um número válido: use vírgula para decimais (ex.: 3,5).",
                "Informe o desconto em reais.",
                "Informe o prazo em dias úteis (0 = a combinar).",
                "A validade em dias: use um número inteiro."), leitura.erros());
    }

    @Test
    void regrasDoNucleoDepoisDaDigitacao() {
        FormularioOrcamento.Leitura leitura = FormularioOrcamento.ler(textos(List.of(bicos("1", "450,00")),
                TipoDesconto.VALOR, "500,00"));
        assertEquals(List.of("O desconto (R$ 500,00) é maior que o total de peças e serviços (R$ 450,00)."),
                leitura.erros());
        assertEquals(List.of("O orçamento precisa de pelo menos um item (peça ou serviço)."),
                FormularioOrcamento.ler(textos(List.of(), TipoDesconto.NENHUM, "")).erros());
    }

    @Test
    void totalDaLinhaEnquantoSeDigita() {
        assertEquals(Optional.of(Dinheiro.ler("450,00")), FormularioOrcamento.totalDaLinha(
                Linha.digitada(TipoItem.SERVICO, "", "2,5", "h", "180")), "sem descrição ainda, o total já aparece");
        assertEquals(Optional.empty(), FormularioOrcamento.totalDaLinha(bicos("4", "")));
        assertEquals(Optional.empty(), FormularioOrcamento.totalDaLinha(bicos("4,", "1,00")));
    }

    @Test
    void padroesDeConfiguracoes() {
        TextosPadroes padrao = FormularioOrcamento.escrever(PadroesOrcamento.padrao());
        assertEquals(TipoDesconto.NENHUM, padrao.tipoDesconto());
        assertEquals("10", padrao.prazoDias());
        assertEquals("15", padrao.validadeDias());
        assertEquals("", padrao.valorHora());
        assertEquals(PadroesOrcamento.padrao(), FormularioOrcamento.ler(padrao).padroes());

        FormularioOrcamento.LeituraPadroes editados = FormularioOrcamento.ler(new TextosPadroes(TipoDesconto.VALOR,
                "50,00", "7", "úteis", "30", "Pix", "180,00"));
        assertTrue(editados.erros().isEmpty(), editados.erros()::toString);
        assertEquals(Dinheiro.ler("180,00"), editados.padroes().valorHora());
        assertEquals(Desconto.emValor(Dinheiro.ler("50,00")), editados.padroes().desconto());

        assertEquals(List.of("Valor da hora: \"cento e oitenta\" não é um valor em reais: use vírgula para os centavos "
                + "(ex.: 1.234,56)."), FormularioOrcamento.ler(new TextosPadroes(TipoDesconto.NENHUM, "", "7", "",
                        "30", "", "cento e oitenta")).erros());
        assertEquals(List.of("A validade padrão é de pelo menos 1 dia."), FormularioOrcamento.ler(new TextosPadroes(
                TipoDesconto.NENHUM, "", "7", "", "0", "", "")).erros());
    }
}
