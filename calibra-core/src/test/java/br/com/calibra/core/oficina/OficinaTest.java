package br.com.calibra.core.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.orcamento.Dinheiro;

/** As peças da OS no núcleo (SPEC 6.5): número, entrada com vistoria, entrega com garantia e pagamento em partes. */
class OficinaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 16, 9, 0);

    @Test
    void numeroComQuatroDigitosPorAno() {
        assertEquals("OS-2026-0007", new NumeroOs(2026, 7).texto());
        assertEquals("OS-2027-1234", new NumeroOs(2027, 1234).texto());
        assertThrows(IllegalArgumentException.class, () -> new NumeroOs(2026, 0));
        assertThrows(IllegalArgumentException.class, () -> new NumeroOs(1999, 1));
    }

    @Test
    void entradaComVistoria() {
        Vistoria vistoria = new Vistoria(List.of(new ItemVistoria(" Lataria ", "risco na porta direita"),
                new ItemVistoria("Estepe", "ok")), " óculos no porta-luvas ");
        EntradaVeiculo entrada = new EntradaVeiculo(AGORA.minusMinutes(5), 251_500.0, NivelCombustivel.UM_QUARTO,
                vistoria);
        assertEquals(List.of(), entrada.validar(AGORA));
        assertEquals("Lataria", entrada.vistoria().itens().getFirst().item(), "texto sem espaços nas pontas");
        assertEquals("óculos no porta-luvas", entrada.vistoria().objetos());
        assertEquals(Vistoria.VAZIA, new EntradaVeiculo(AGORA, null, null, null).vistoria(), "vistoria opcional");
    }

    @Test
    void entradaRecusaFuturoKmNegativoEItemSemNome() {
        EntradaVeiculo errada = new EntradaVeiculo(AGORA.plusHours(1), -1.0, null,
                new Vistoria(List.of(new ItemVistoria(" ", "amassado")), ""));
        assertEquals(List.of("A data e a hora da entrada não podem estar no futuro.",
                "O km (ou as horas de motor) da entrada não pode ser negativo.",
                "Item 1 da vistoria sem nome."), errada.validar(AGORA));
        assertEquals(List.of("Informe a data e a hora da entrada."),
                new EntradaVeiculo(null, null, null, null).validar(AGORA));
        assertEquals(List.of(), new EntradaVeiculo(AGORA.plusSeconds(30), null, null, null).validar(AGORA),
                "um relógio adiantado em segundos não atrapalha");
    }

    @Test
    void garantiaContaDoDiaDaEntrega() {
        assertEquals(LocalDate.of(2027, 1, 14), new Garantia(90).vence(LocalDate.of(2026, 10, 16)));
        assertEquals(LocalDate.of(2026, 10, 16), Garantia.SEM_GARANTIA.vence(LocalDate.of(2026, 10, 16)));
        assertEquals(List.of("A garantia não pode ser negativa (0 = sem garantia)."), new Garantia(-1).validar());
        assertEquals(List.of(), new Garantia(3650).validar(), "10 anos ainda vale");
        assertEquals(List.of("A garantia vai até 3650 dias (10 anos): confira o número digitado."),
                new Garantia(9000).validar(), "revisão final: 9000 no lugar de 90 ficaria gravado para sempre");
    }

    @Test
    void entregaComGarantiaEAvisoDeKmMenor() {
        Entrega entrega = new Entrega(AGORA, 251_480.0, new Garantia(90));
        assertEquals(List.of(), entrega.validar(AGORA));
        assertEquals(LocalDate.of(2027, 1, 14), entrega.garantiaVence());
        assertEquals(List.of("O km de saída (251.480) é menor que o da entrada (251.500). Confira se não foi erro "
                + "de digitação."), entrega.avisos(251_500.0));
        assertEquals(List.of(), new Entrega(AGORA, 251_530.0, new Garantia(90)).avisos(251_500.0));
        assertEquals(List.of(), new Entrega(AGORA, null, new Garantia(90)).avisos(251_500.0), "km não anotado");
        assertEquals(List.of("Informe a data e a hora da entrega.", "Informe a garantia em dias (0 = sem garantia)."),
                new Entrega(null, null, null).validar(AGORA));
        assertEquals("", entrega.quemRetirou(), "o veículo que passou pela oficina sai sem \"quem retirou\"");
    }

    /** Fase 3B (SPEC 6.5): a OS em que o veículo não passou pela oficina é entregue com quem retirou, sem km. */
    @Test
    void entregaSemVeiculoPedeQuemRetirou() {
        Entrega chicote = new Entrega(AGORA, null, new Garantia(90), "  Oficina do João  ");
        assertEquals("Oficina do João", chicote.quemRetirou());
        assertEquals(List.of(), chicote.validar(AGORA));
        assertEquals(List.of(), chicote.validarSemVeiculo());
        assertEquals(List.of("Escreva quem retirou: o veículo não passou pela oficina.",
                "O veículo não passou pela oficina: a entrega não tem km (nem horas de motor) de saída."),
                new Entrega(AGORA, 120_000.0, new Garantia(90), null).validarSemVeiculo());
    }

    @Test
    void pagamentoPrecisaDeFormaEValor() {
        assertEquals(List.of(), new Pagamento(AGORA, FormaPagamento.PIX, Dinheiro.ler("1.000,00"), " sinal ")
                .validar(AGORA));
        assertEquals("sinal", new Pagamento(AGORA, FormaPagamento.PIX, Dinheiro.ler("1,00"), " sinal ").observacao());
        assertEquals(List.of("A data e a hora do recebimento não podem estar no futuro.",
                "Escolha a forma de pagamento.", "O valor recebido deve ser maior que zero."),
                new Pagamento(AGORA.plusDays(1), null, Dinheiro.ZERO, "").validar(AGORA));
        assertTrue(new Pagamento(null, FormaPagamento.DINHEIRO, null, null).validar(AGORA)
                .contains("Informe a data e a hora do recebimento."));
    }

    /** OS de R$ 2.025,00 com sinal de 50%: pendente, parcial, paga, paga a mais; e a OS sem valor. */
    @Test
    void situacaoDoPagamento() {
        Dinheiro valor = Dinheiro.ler("2.025,00");
        assertEquals(SituacaoPagamento.PENDENTE, SituacaoPagamento.de(valor, Dinheiro.ZERO));
        assertEquals(SituacaoPagamento.PARCIAL, SituacaoPagamento.de(valor, Dinheiro.ler("1.012,50")));
        assertEquals(Dinheiro.ler("1.012,50"), SituacaoPagamento.falta(valor, Dinheiro.ler("1.012,50")));
        assertEquals(SituacaoPagamento.PAGO, SituacaoPagamento.de(valor, Dinheiro.ler("2.025,00")));
        assertEquals(SituacaoPagamento.PAGO, SituacaoPagamento.de(valor, Dinheiro.ler("2.100,00")));
        assertEquals(Dinheiro.ZERO, SituacaoPagamento.falta(valor, Dinheiro.ler("2.100,00")), "falta nunca é negativa");
        assertEquals(Dinheiro.ler("75,00"), SituacaoPagamento.aMais(valor, Dinheiro.ler("2.100,00")),
                "o recebido a mais aparece (revisão final)");
        assertEquals(Dinheiro.ZERO, SituacaoPagamento.aMais(valor, Dinheiro.ler("1.012,50")));
        assertEquals(SituacaoPagamento.PAGO, SituacaoPagamento.de(Dinheiro.ZERO, Dinheiro.ZERO), "serviço sem valor");
    }

    @Test
    void rotulosEmPortugues() {
        assertEquals("1/4", NivelCombustivel.UM_QUARTO.rotulo());
        assertEquals("Cartão de crédito", FormaPagamento.CARTAO_CREDITO.rotulo());
        assertEquals("Parcial", SituacaoPagamento.PARCIAL.rotulo());
    }
}
