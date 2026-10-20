package br.com.calibra.relatorios;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.FormaPagamento;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.GarantiaDoServico;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.PadroesOs;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.oficina.ResumoChecklist;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.TipoItem;

/**
 * Uma OS fictícia para os documentos (bloco 3.3): a Ana trouxe o Uno com barulho na suspensão, o orçamento de
 * R$ 1.000,00 foi aprovado e ela pagou em duas partes (um recebimento lançado errado foi cancelado).
 */
final class ExemplosOs {

    static final LocalDateTime ABERTA = LocalDateTime.of(2026, 10, 13, 8, 40);
    static final LocalDateTime CHEGOU = LocalDateTime.of(2026, 10, 13, 9, 2);
    static final LocalDateTime ENTREGUE = LocalDateTime.of(2026, 10, 14, 17, 30);

    private ExemplosOs() {
    }

    static DadosOficina oficina() {
        return new DadosOficina("Oficina Teste (fictícia)", "11.222.333/0001-81", "Rua das Bielas, 100 · São Paulo",
                "(11) 99999-0000", "contato@oficina-teste.com.br", null);
    }

    static EntradaVeiculo entrada() {
        return new EntradaVeiculo(CHEGOU, 120_500.0, NivelCombustivel.MEIO, new Vistoria(List.of(
                new ItemVistoria("Lataria e pintura", "Risco na porta direita"),
                new ItemVistoria("Vidros, faróis e retrovisores", ""),
                new ItemVistoria("Pneus, rodas e estepe", "Pneu traseiro esquerdo careca")), "Estepe e macaco"));
    }

    static DadosOs.OrcamentoAprovado aprovado() {
        return new DadosOs.OrcamentoAprovado("ORC-2026-0005 rev. 2", new Orcamento(List.of(
                ItemOrcamento.digitado(TipoItem.SERVICO, "Troca dos amortecedores dianteiros", BigDecimal.valueOf(2),
                        "h", Dinheiro.ler("180,00")),
                ItemOrcamento.digitado(TipoItem.PECA, "Amortecedor dianteiro (par)", BigDecimal.ONE, "par",
                        Dinheiro.ler("640,00"))),
                Desconto.NENHUM, 2, "", 15, "", ""));
    }

    /** O sinal no PIX, os R$ 50,00 lançados errado e cancelados e o resto em dinheiro na entrega. */
    static List<DadosOs.Recebimento> recebimentos() {
        return List.of(
                new DadosOs.Recebimento(new Pagamento(CHEGOU.plusHours(1), FormaPagamento.PIX, Dinheiro.ler("500,00"),
                        "Sinal"), false),
                new DadosOs.Recebimento(new Pagamento(ENTREGUE.minusMinutes(10), FormaPagamento.CARTAO_DEBITO,
                        Dinheiro.ler("50,00"), ""), true),
                new DadosOs.Recebimento(new Pagamento(ENTREGUE.minusMinutes(5), FormaPagamento.DINHEIRO,
                        Dinheiro.ler("500,00"), "Resto na entrega"), false));
    }

    /** Entregue, com garantia de 90 dias, e paga. */
    static DadosOs entregue(Path logo) {
        return new DadosOs(oficina(), logo, "OS-2026-0007", StatusOs.ENTREGUE, ABERTA, "Ana Costa (fictícia)",
                "(11) 91234-5678", "Fiat Uno Mille 2011 · EFG4H56", false, "Barulho na suspensão dianteira em lombada",
                LocalDate.of(2026, 10, 15), "Carlos (fictício)", entrada(),
                new Entrega(ENTREGUE, 120_530.0, new Garantia(90)), aprovado(),
                List.of(new GarantiaDoServico("Troca dos amortecedores dianteiros", null)), recebimentos(),
                PadroesOs.TEXTO_VIA_PADRAO, "0.3.0-teste", ENTREGUE.plusMinutes(5), List.of());
    }

    /** Recém-aberta: o veículo ainda não chegou e não há orçamento, recebimento nem texto da via. */
    static DadosOs semOVeiculo() {
        return new DadosOs(DadosOficina.vazia(), null, "OS-2026-0008", StatusOs.ABERTA, ABERTA, "Bruno Dias (fictício)",
                "", "VW Gol GTi 1994 · BCD2E34", false, "Embreagem patinando", null, "", null, null, null, List.of(),
                List.of(), "", "0.3.0-teste", ABERTA.plusMinutes(1), List.of());
    }

    /** Pronta para retirar, com o sinal recebido. */
    static DadosOs pronta() {
        DadosOs d = entregue(null);
        return new DadosOs(d.oficina(), null, d.numero(), StatusOs.PRONTA, d.abertaEm(), d.cliente(),
                d.telefoneCliente(), d.veiculo(), false, d.servicoPedido(), d.previsao(), d.mecanico(), d.entrada(),
                null, d.aprovado(), d.garantias(), d.recebimentos().subList(0, 1), d.textoVia(), d.versaoApp(),
                d.geradoEm(), d.checklists());
    }

    /** Entregue, com R$ 75,00 recebidos a mais (um PIX lançado sem cancelar o errado). */
    static DadosOs pagaAMais() {
        DadosOs d = entregue(null);
        List<DadosOs.Recebimento> recebimentos = new ArrayList<>(d.recebimentos());
        recebimentos.add(new DadosOs.Recebimento(new Pagamento(ENTREGUE.minusMinutes(4), FormaPagamento.PIX,
                Dinheiro.ler("75,00"), "Lançado a mais"), false));
        return new DadosOs(d.oficina(), null, d.numero(), d.status(), d.abertaEm(), d.cliente(), d.telefoneCliente(),
                d.veiculo(), false, d.servicoPedido(), d.previsao(), d.mecanico(), d.entrada(), d.entrega(),
                d.aprovado(), d.garantias(), recebimentos, d.textoVia(), d.versaoApp(), d.geradoEm(), d.checklists());
    }

    /** Uma lancha: o uso em horas de motor. */
    static DadosOs embarcacao() {
        DadosOs d = entregue(null);
        EntradaVeiculo e = d.entrada();
        return new DadosOs(d.oficina(), null, d.numero(), d.status(), d.abertaEm(), d.cliente(), d.telefoneCliente(),
                "Lancha Focker 215 · Estrela do Mar", true, "Motor falhando em alta", null, "",
                new EntradaVeiculo(e.quando(), 1_250.0, e.combustivel(), e.vistoria()),
                new Entrega(ENTREGUE, 1_252.0, Garantia.SEM_GARANTIA), d.aprovado(), d.garantias(), d.recebimentos(),
                "", d.versaoApp(), d.geradoEm(), d.checklists());
    }

    /**
     * Entregue com três serviços (Fase 3B): o digitado fica com a garantia da OS (90 dias), o alternador tem a própria
     * de 365 dias e o diagnóstico, 0 (sem garantia), como viriam do catálogo. O total é o mesmo R$ 1.000,00, pago.
     */
    static DadosOs comGarantiaPorServico() {
        DadosOs d = entregue(null);
        Orcamento orcamento = new Orcamento(List.of(
                ItemOrcamento.digitado(TipoItem.SERVICO, "Troca dos amortecedores dianteiros", BigDecimal.valueOf(2),
                        "h", Dinheiro.ler("180,00")),
                ItemOrcamento.doServico("Reparo ou troca de alternador", 2.0, null, Dinheiro.ler("180,00"), 41, 1),
                ItemOrcamento.doServico("Diagnóstico de falha elétrica", null, Dinheiro.ler("280,00"), null, 42, 1)),
                Desconto.NENHUM, 2, "", 15, "", "");
        return new DadosOs(d.oficina(), null, d.numero(), d.status(), d.abertaEm(), d.cliente(), d.telefoneCliente(),
                d.veiculo(), false, d.servicoPedido(), d.previsao(), d.mecanico(), d.entrada(), d.entrega(),
                new DadosOs.OrcamentoAprovado("ORC-2026-0006 rev. 1", orcamento),
                List.of(new GarantiaDoServico("Troca dos amortecedores dianteiros", null),
                        new GarantiaDoServico("Reparo ou troca de alternador", 365),
                        new GarantiaDoServico("Diagnóstico de falha elétrica", 0)),
                d.recebimentos(), d.textoVia(), d.versaoApp(), d.geradoEm(), d.checklists());
    }

    /**
     * A de três serviços com garantia por serviço e, nos dois do catálogo, o checklist (Fase 3B): o do alternador com
     * tudo feito; o do diagnóstico com um item que não se aplica e um que ficou pendente (o "pronto" teve a
     * justificativa).
     */
    static DadosOs comChecklist() {
        DadosOs d = comGarantiaPorServico();
        return new DadosOs(d.oficina(), null, d.numero(), d.status(), d.abertaEm(), d.cliente(), d.telefoneCliente(),
                d.veiculo(), false, d.servicoPedido(), d.previsao(), d.mecanico(), d.entrada(), d.entrega(),
                d.aprovado(), d.garantias(), d.recebimentos(), d.textoVia(), d.versaoApp(), d.geradoEm(), List.of(
                        new DadosOs.ChecklistDoServico("Reparo ou troca de alternador", new ResumoChecklist(6, 6, 0, 0)),
                        new DadosOs.ChecklistDoServico("Diagnóstico de falha elétrica", new ResumoChecklist(5, 3, 1,
                                1))));
    }

    /**
     * Fase 3B: só a confecção de chicote, feita na bancada; o veículo não passou pela oficina (sem a entrada), e a
     * entrega registrou quem retirou, sem km de saída.
     */
    static DadosOs chicoteSemOVeiculo() {
        DadosOs d = entregue(null);
        Orcamento chicote = new Orcamento(List.of(ItemOrcamento.doServico(
                "Confecção de chicote para injeção programável", 8.0, null, Dinheiro.ler("125,00"), 51, 1)),
                Desconto.NENHUM, 1, "", 15, "", "");
        return new DadosOs(d.oficina(), null, "OS-2026-0009", d.status(), d.abertaEm(), d.cliente(),
                d.telefoneCliente(), d.veiculo(), false, "Chicote para FuelTech, entregar na oficina do João",
                d.previsao(), d.mecanico(), null, new Entrega(ENTREGUE, null, new Garantia(90), "Oficina do João "
                        + "(fictícia)"), new DadosOs.OrcamentoAprovado("ORC-2026-0009 rev. 1", chicote),
                List.of(new GarantiaDoServico("Confecção de chicote para injeção programável", null)),
                d.recebimentos(), d.textoVia(), d.versaoApp(), d.geradoEm(), List.of(new DadosOs.ChecklistDoServico(
                        "Confecção de chicote para injeção programável", new ResumoChecklist(12, 12, 0, 0))));
    }

    /** Entregue, com um orçamento aprovado só de peças: nenhum serviço, nenhuma garantia por serviço. */
    static DadosOs soComPecas() {
        DadosOs d = entregue(null);
        Orcamento pecas = new Orcamento(List.of(ItemOrcamento.digitado(TipoItem.PECA, "Amortecedor dianteiro (par)",
                BigDecimal.ONE, "par", Dinheiro.ler("640,00"))), Desconto.NENHUM, 2, "", 15, "", "");
        return new DadosOs(d.oficina(), null, d.numero(), d.status(), d.abertaEm(), d.cliente(), d.telefoneCliente(),
                d.veiculo(), false, d.servicoPedido(), d.previsao(), d.mecanico(), d.entrada(), d.entrega(),
                new DadosOs.OrcamentoAprovado("ORC-2026-0007 rev. 1", pecas), List.of(), d.recebimentos(),
                d.textoVia(), d.versaoApp(), d.geradoEm(), d.checklists());
    }
}
