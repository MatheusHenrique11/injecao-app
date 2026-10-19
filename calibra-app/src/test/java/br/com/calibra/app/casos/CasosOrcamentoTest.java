package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.app.catalogo.PecasEscolhidas;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.orcamento.StatusOrcamento;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;
import br.com.calibra.relatorios.DadosOrcamento;

/**
 * Orçamento do projeto (bloco 2.5): itens do cálculo com o preço do catálogo, rascunho, envio (projeto ORCADO),
 * aprovação (projeto APROVADO), aprovado que não muda, revisão e orçamento desatualizado. Peças fictícias.
 */
class CasosOrcamentoTest {

    private static final String FONTE = "Ficha fictícia de teste";

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 13, 9, 0));
    private CasosProjeto casos;
    private CasosCatalogo catalogo;
    private CasosOrcamento orcamentos;
    private CasosOficina oficina;
    private long projeto;

    @BeforeEach
    void abrirBanco() throws IOException {
        PastasDados pastas = new PastasDados(raiz);
        pastas.criar();
        BancoDados banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.2.0-teste", relogio));
        casos = new CasosProjeto(banco, relogio, "0.2.0-teste");
        catalogo = new CasosCatalogo(banco, relogio);
        orcamentos = new CasosOrcamento(banco, relogio, casos);
        oficina = new CasosOficina(banco, relogio, casos, pastas.anexos());
        EntradaDimensionamento entrada = CasosProjetoTest.casoB();
        projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(CasosProjetoTest.cliente("João da Silva")),
                new VeiculoNovo(CasosProjetoTest.carro("ABC1D23")), "AP 2.0 turbo", entrada.motor(), entrada.meta()));
    }

    /** O bico do caso B no catálogo, com preço de referência, e o cálculo salvo com ele. */
    private ItemCatalogo<Bico> calcularComBicoDoCatalogo() {
        Bico bico = new Bico("Bico teste 1050", 1050.0, 300.0, 0.70, TempoMorto.fixo(1.0), null, FONTE, null);
        ItemCatalogo<Bico> item = catalogo.criar(TipoCatalogo.BICO, bico,
                new InfoCatalogo("Bico teste 1050", FONTE, null, false, Dinheiro.ler("450,00"), ""));
        salvarCalculo(item);
        return item;
    }

    private void salvarCalculo(ItemCatalogo<Bico> bico) {
        EntradaDimensionamento entrada = CasosProjetoTest.casoB().comMotor(casos.abrir(projeto).motor().configuracao())
                .comBico(bico.dados());
        casos.salvarCalculo(projeto, entrada, List.of(new Justificativa(Indicador.DC_BICO, "Bico provisório.",
                LocalDateTime.of(2026, 10, 13, 9, 0))), new PecasEscolhidas(bico, null, null, null, null, null));
    }

    /** A sugestão com o valor que faltava e a mão de obra. */
    private Orcamento completo() {
        Orcamento sugerido = orcamentos.sugestao(projeto, PadroesOrcamento.padrao());
        List<ItemOrcamento> itens = new ArrayList<>(sugerido.itens());
        itens.add(ItemOrcamento.digitado(TipoItem.SERVICO, "Mão de obra", new BigDecimal("2.5"), "h",
                Dinheiro.ler("180,00")));
        return new Orcamento(itens, sugerido.desconto(), sugerido.prazoDiasUteis(), sugerido.prazoObservacao(),
                sugerido.validadeDias(), "50% na aprovação", "");
    }

    private StatusProjeto statusDoProjeto() {
        return casos.abrir(projeto).projeto().status();
    }

    private StatusOs statusDaOs(OrcamentoSalvo orcamento) {
        return oficina.buscar(orcamento.osId()).os().status();
    }

    @Test
    void itensSaemDoCalculoComOPrecoDoCatalogo() {
        EntradaInvalidaException semCalculo = assertThrows(EntradaInvalidaException.class,
                () -> orcamentos.sugestao(projeto, PadroesOrcamento.padrao()));
        assertTrue(semCalculo.getMessage().startsWith("Calcule e salve o dimensionamento"), semCalculo.getMessage());

        ItemCatalogo<Bico> bico = calcularComBicoDoCatalogo();
        Orcamento sugerido = orcamentos.sugestao(projeto, PadroesOrcamento.padrao());
        ItemOrcamento item = sugerido.itens().getFirst();
        assertEquals("Bico teste 1050", item.descricao());
        assertEquals(new BigDecimal("4"), item.quantidade(), "um bico por cilindro");
        assertEquals(Dinheiro.ler("450,00"), item.valorUnitario(), "o preço de referência do catálogo");
        assertEquals(bico.id(), item.catalogoItemId());
        assertEquals(1, item.catalogoVersao());
        assertEquals(Dinheiro.ler("1.800,00"), sugerido.total());
        assertEquals(15, sugerido.validadeDias(), "padrões de Configurações");
    }

    @Test
    void doRascunhoAoAprovado() {
        calcularComBicoDoCatalogo();
        OrcamentoSalvo criado = orcamentos.criar(projeto, completo());
        assertEquals("ORC-2026-0001", criado.numero().comRevisao());
        assertEquals(StatusOrcamento.RASCUNHO, criado.status());
        assertEquals(StatusProjeto.DIMENSIONADO, statusDoProjeto());
        assertEquals(StatusOs.ABERTA, statusDaOs(criado), "o orçamento do projeto vai para uma OS aberta para ele");
        OperacaoRecusadaException segundo = assertThrows(OperacaoRecusadaException.class,
                () -> orcamentos.criar(projeto, completo()));
        assertEquals("O projeto já tem o orçamento ORC-2026-0001: edite o rascunho.", segundo.getMessage());

        Orcamento comDesconto = new Orcamento(completo().itens(), null, 10, "", 15, "à vista com 5%", "");
        orcamentos.salvar(criado.id(), comDesconto);
        assertEquals("à vista com 5%", orcamentos.buscar(criado.id()).orElseThrow().conteudo().condicoes());

        orcamentos.marcarEnviado(criado.id());
        assertEquals(StatusProjeto.ORCADO, statusDoProjeto());
        assertEquals(StatusOs.AGUARDANDO_APROVACAO, statusDaOs(criado));
        assertNotNull(orcamentos.buscar(criado.id()).orElseThrow().enviadoEm());

        EntradaInvalidaException futuro = assertThrows(EntradaInvalidaException.class, () -> orcamentos.aprovar(
                criado.id(), LocalDateTime.of(2026, 10, 20, 9, 0), MeioAprovacao.WHATSAPP, ""));
        assertEquals(List.of("A data e a hora da resposta não podem estar no futuro."), futuro.erros());
        relogio.andar(Duration.ofHours(2));
        orcamentos.aprovar(criado.id(), LocalDateTime.of(2026, 10, 13, 10, 30), MeioAprovacao.WHATSAPP,
                "Aprovou por mensagem.");
        assertEquals(StatusProjeto.APROVADO, statusDoProjeto());
        assertEquals(StatusOs.APROVADA, statusDaOs(criado));
        OrcamentoSalvo aprovado = orcamentos.buscar(criado.id()).orElseThrow();
        assertEquals(MeioAprovacao.WHATSAPP, aprovado.meioResposta());
        assertEquals(LocalDateTime.of(2026, 10, 13, 10, 30), aprovado.respondidoEm());

        OperacaoRecusadaException naoMuda = assertThrows(OperacaoRecusadaException.class,
                () -> orcamentos.salvar(criado.id(), completo()));
        assertTrue(naoMuda.getMessage().endsWith("abra uma revisão nova."), naoMuda.getMessage());

        OrcamentoSalvo revisao = orcamentos.novaRevisao(criado.id());
        assertEquals("ORC-2026-0001 rev. 2", revisao.numero().comRevisao());
        assertEquals(StatusOrcamento.RASCUNHO, revisao.status());
        assertEquals(comDesconto, revisao.conteudo(), "a revisão começa igual à anterior");
        assertEquals(StatusOrcamento.SUBSTITUIDO, orcamentos.buscar(criado.id()).orElseThrow().status());
        assertEquals(StatusProjeto.APROVADO, statusDoProjeto(), "o projeto não volta");
        assertEquals(StatusOs.APROVADA, statusDaOs(revisao), "nem a OS: depois da aprovação, a revisão não a move");
        assertEquals(List.of(revisao.id(), criado.id()),
                orcamentos.orcamentos(projeto).stream().map(s -> s.orcamento().id()).toList());
    }

    /**
     * Revisão pelo agente: depois da entrega da primeira OS do projeto, a aba do projeto fica sem orçamento, e o
     * próximo abre a segunda OS (a segunda etapa da preparação). O orçamento antigo fica na OS dele, só para ver.
     */
    @Test
    void segundaEtapaDepoisDaEntrega() {
        calcularComBicoDoCatalogo();
        OrcamentoSalvo primeiro = orcamentos.criar(projeto, completo());
        long os1 = primeiro.osId();
        orcamentos.marcarEnviado(primeiro.id());
        relogio.andar(Duration.ofHours(1));
        orcamentos.aprovar(primeiro.id(), LocalDateTime.now(relogio), MeioAprovacao.PESSOALMENTE, "");
        oficina.registrarEntrada(os1, new EntradaVeiculo(LocalDateTime.now(relogio), 250_000.0, null, null));
        oficina.mudarStatus(os1, StatusOs.EM_EXECUCAO, "");
        oficina.mudarStatus(os1, StatusOs.PRONTA, "");
        relogio.andar(Duration.ofHours(1));
        oficina.entregar(os1, new Entrega(LocalDateTime.now(relogio), 250_010.0, new Garantia(90)));

        assertEquals(List.of(), orcamentos.orcamentos(projeto), "a OS da primeira etapa foi entregue");
        assertEquals("A OS OS-2026-0001 está \"Entregue\": o orçamento dela não muda mais.",
                orcamentos.trava(os1).orElseThrow());
        assertEquals(List.of(primeiro.id()), orcamentos.orcamentosDaOs(os1).stream().map(s -> s.orcamento().id())
                .toList(), "o orçamento antigo fica na OS dele");

        OrcamentoSalvo segundo = orcamentos.criar(projeto, completo());
        assertNotEquals(os1, segundo.osId(), "a segunda etapa abre outra OS");
        assertEquals("ORC-2026-0002", segundo.numero().comRevisao());
        assertEquals(List.of(segundo.id()), orcamentos.orcamentos(projeto).stream().map(s -> s.orcamento().id())
                .toList());
        assertTrue(orcamentos.trava(segundo.osId()).isEmpty());
    }

    @Test
    void recusaNaoMudaOProjeto() {
        calcularComBicoDoCatalogo();
        OrcamentoSalvo criado = orcamentos.criar(projeto, completo());
        assertThrows(OperacaoRecusadaException.class, () -> orcamentos.recusar(criado.id(),
                LocalDateTime.of(2026, 10, 13, 8, 0), MeioAprovacao.TELEFONE, ""), "recusar só depois de enviar");
        orcamentos.marcarEnviado(criado.id());
        orcamentos.recusar(criado.id(), LocalDateTime.of(2026, 10, 13, 8, 0), MeioAprovacao.TELEFONE, "Achou caro.");
        assertEquals(StatusOrcamento.RECUSADO, orcamentos.buscar(criado.id()).orElseThrow().status());
        assertEquals(StatusProjeto.ORCADO, statusDoProjeto());
        assertEquals(StatusOs.ABERTA, statusDaOs(criado), "a recusa volta a OS para aberta");
        EntradaInvalidaException semMeio = assertThrows(EntradaInvalidaException.class,
                () -> orcamentos.aprovar(criado.id(), null, null, ""));
        assertEquals(2, semMeio.erros().size(), semMeio.erros()::toString);
    }

    @Test
    void motorNovoDeixaOOrcamentoDesatualizado() {
        ItemCatalogo<Bico> bico = calcularComBicoDoCatalogo();
        OrcamentoSalvo criado = orcamentos.criar(projeto, completo());
        casos.salvarMotor(projeto, "AP 2.0 turbo forjado", casos.abrir(projeto).motor().configuracao().paraConstrutor()
                .taxaCompressao(9.0).construir()).orElseThrow();

        CasosOrcamento.SituacaoOrcamento situacao = orcamentos.orcamentos(projeto).getFirst();
        assertTrue(situacao.desatualizado());
        assertTrue(situacao.motivos().getFirst().startsWith("o motor mudou"), situacao.motivos()::toString);
        EntradaInvalidaException enviar = assertThrows(EntradaInvalidaException.class,
                () -> orcamentos.marcarEnviado(criado.id()));
        assertTrue(enviar.getMessage().startsWith("O orçamento está desatualizado"), enviar.getMessage());
        assertThrows(EntradaInvalidaException.class, () -> orcamentos.usarCalculoMaisNovo(criado.id()),
                "sem cálculo novo salvo, não tem o que usar");

        salvarCalculo(bico);
        assertTrue(orcamentos.orcamentos(projeto).getFirst().motivos().contains(
                "há um cálculo salvo mais novo que o deste orçamento"));
        orcamentos.usarCalculoMaisNovo(criado.id());
        assertTrue(orcamentos.orcamentos(projeto).getFirst().motivos().isEmpty());
        orcamentos.marcarEnviado(criado.id());
    }

    @Test
    void documentoComClienteAnonimizadoEDataDoEnvio() {
        calcularComBicoDoCatalogo();
        OrcamentoSalvo criado = orcamentos.criar(projeto, completo());
        DadosOrcamento rascunho = orcamentos.documento(criado.id(), DadosOficina.vazia(), null, "0.2.0-teste");
        assertEquals("João da Silva", rascunho.cliente());
        assertEquals("VW Gol GTi 1994 · ABC1D23", rascunho.veiculo());
        assertEquals("AP 2.0 turbo (v1)", rascunho.preparacao().motor());
        assertNull(rascunho.logo());

        orcamentos.marcarEnviado(criado.id());
        relogio.andar(Duration.ofDays(3));
        oficina.anonimizarCliente(casos.abrir(projeto).cliente().id());
        DadosOrcamento enviado = orcamentos.documento(criado.id(), DadosOficina.vazia(), null, "0.2.0-teste");
        assertTrue(enviado.cliente().startsWith("Cliente anonimizado #"), enviado.cliente());
        assertEquals(LocalDate.of(2026, 10, 13), enviado.emissao(), "a validade conta do envio");
    }
}
