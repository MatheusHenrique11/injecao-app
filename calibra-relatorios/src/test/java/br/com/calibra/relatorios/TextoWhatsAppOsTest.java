package br.com.calibra.relatorios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Os textos da OS para o WhatsApp (bloco 3.3): OS aberta e pronto para retirar. Dados fictícios. */
class TextoWhatsAppOsTest {

    @Test
    void osAberta() {
        assertEquals("""
                *OS-2026-0007* — Oficina Teste (fictícia)
                Veículo: Fiat Uno Mille 2011 · EFG4H56
                Chegou em 13/10/2026 às 09:02.
                Serviço pedido: Barulho na suspensão dianteira em lombada
                Previsão de entrega: 15/10/2026

                Oficina Teste (fictícia) · (11) 99999-0000""", TextoWhatsAppOs.osAberta(ExemplosOs.entregue(null)));
    }

    /** Sem o veículo, sem previsão e sem os dados da oficina: o orçamento a fazer e nada de contato. */
    @Test
    void osAbertaSemOVeiculo() {
        assertEquals("""
                *OS-2026-0008*
                Veículo: VW Gol GTi 1994 · BCD2E34
                Orçamento a fazer: o veículo ainda não chegou.
                Serviço pedido: Embreagem patinando""", TextoWhatsAppOs.osAberta(ExemplosOs.semOVeiculo()));
    }

    @Test
    void prontaParaRetirarComOQueFalta() {
        assertEquals("""
                *OS-2026-0007: pronto para retirar* — Oficina Teste (fictícia)
                Veículo: Fiat Uno Mille 2011 · EFG4H56
                Valor: R$ 1.000,00
                Recebido: R$ 500,00
                *Falta: R$ 500,00*

                Oficina Teste (fictícia) · (11) 99999-0000""", TextoWhatsAppOs.prontaParaRetirar(ExemplosOs.pronta()));
    }

    /** O recebimento cancelado não conta: com o sinal e o resto, está tudo pago. */
    @Test
    void prontaParaRetirarJaPaga() {
        assertEquals("""
                *OS-2026-0007: pronto para retirar* — Oficina Teste (fictícia)
                Veículo: Fiat Uno Mille 2011 · EFG4H56
                Valor: R$ 1.000,00
                Recebido: R$ 1.000,00
                Tudo pago.

                Oficina Teste (fictícia) · (11) 99999-0000""",
                TextoWhatsAppOs.prontaParaRetirar(ExemplosOs.entregue(null)));
    }
}
