package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosOficina.ChecklistDaOs;
import br.com.calibra.app.casos.CasosOficina.MarcacaoDoItem;
import br.com.calibra.app.casos.CasosOficina.NecessidadeDoVeiculo;
import br.com.calibra.app.casos.CasosOficina.NovaOs;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.Marcacao;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.SituacaoItem;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.orcamento.ServicosSugeridos;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.oficina.ItemChecklistSalvo;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;

/**
 * A OS em que o veículo não passa pela oficina (Fase 3B, bloco 3B.6; SPEC 6.5): o serviço de bancada começa sem a
 * entrada, o checklist do serviço que precisa do veículo fica bloqueado até a entrada, o "pronto" exige a entrada só se
 * algum serviço precisar do veículo, e a entrega sem o veículo registra quem retirou. Testes obrigatórios nº 9 e nº 10
 * da fase. Dados fictícios; os checklists são os que vêm com o app.
 */
class OsSemVeiculoTest {

    private static final String CHICOTE = "Confecção de chicote teste";
    private static final String INSTALACAO = "Instalação de injeção programável teste";
    private static final Dinheiro HORA = Dinheiro.ler("180,00");

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 20, 9, 0));
    private CasosOrcamento orcamentos;
    private CasosOficina oficina;
    private CasosCatalogo catalogo;

    @BeforeEach
    void abrirBanco() throws IOException {
        PastasDados pastas = new PastasDados(raiz.resolve("dados"));
        pastas.criar();
        BancoDados banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.3.0-teste", relogio));
        CasosProjeto casos = new CasosProjeto(banco, relogio, "0.3.0-teste");
        orcamentos = new CasosOrcamento(banco, relogio, casos);
        oficina = new CasosOficina(banco, relogio, casos, pastas.anexos());
        catalogo = new CasosCatalogo(banco, relogio);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    /** Um serviço de injeção programável do catálogo, com o checklist que vem com o app. */
    private ItemCatalogo<ServicoPadrao> servico(String nome, boolean exigeVeiculo, String chaveDoModelo) {
        ServicoPadrao servico = new ServicoPadrao(nome, 4.0, "Tabela da oficina", null,
                CategoriaServico.INJECAO_PROGRAMAVEL, "", exigeVeiculo, null,
                catalogo.checklistsDasSementes().get(chaveDoModelo));
        return catalogo.criar(TipoCatalogo.SERVICO, servico, new InfoCatalogo(nome, "Tabela da oficina", null, false,
                null, "Dados fictícios."));
    }

    private ItemCatalogo<ServicoPadrao> chicote() {
        return servico(CHICOTE, false, ServicosSugeridos.CHECKLIST_CHICOTE);
    }

    private ItemCatalogo<ServicoPadrao> instalacao() {
        return servico(INSTALACAO, true, ServicosSugeridos.CHECKLIST_INSTALACAO_INJECAO);
    }

    /** A OS aberta sem a entrada do veículo, com o orçamento destes serviços aprovado. */
    @SafeVarargs
    private long aprovadaSemEntrada(ItemCatalogo<ServicoPadrao>... servicos) {
        long os = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Ana Costa")),
                new VeiculoNovo(CasosProjetoTest.carro("ABC1D23")), "Chicote para injeção programável", null, null,
                "", null)).osId();
        relogio.andar(Duration.ofMinutes(30));
        List<ItemOrcamento> linhas = new ArrayList<>();
        for (ItemCatalogo<ServicoPadrao> servico : servicos) {
            linhas.add(ItemOrcamento.doServico(servico.dados().textoNoOrcamento(), servico.dados().horas(), null, HORA,
                    servico.id(), servico.versao()));
        }
        OrcamentoSalvo orcamento = orcamentos.criarNaOs(os, new Orcamento(linhas, Desconto.NENHUM, 1, "", 15, "", ""));
        orcamentos.marcarEnviado(orcamento.id());
        orcamentos.aprovar(orcamento.id(), agora().minusMinutes(5), MeioAprovacao.WHATSAPP, "Pode fazer.");
        relogio.andar(Duration.ofMinutes(10));
        return os;
    }

    private ChecklistDaOs checklist(long os, String servico) {
        return oficina.checklists(os).stream().filter(c -> c.checklist().servico().equals(servico)).findFirst()
                .orElseThrow();
    }

    /** Marca como feitos todos os itens do checklist do serviço. */
    private void marcarFeitos(long os, String servico) {
        List<MarcacaoDoItem> marcacoes = new ArrayList<>();
        for (ItemChecklistSalvo item : checklist(os, servico).checklist().itens()) {
            marcacoes.add(new MarcacaoDoItem(item.id(), new Marcacao(SituacaoItem.FEITO, "", "")));
        }
        oficina.marcar(os, marcacoes, null);
    }

    /**
     * Teste nº 9 da Fase 3B: a OS só com a confecção de chicote (serviço que não exige o veículo) vai a "em execução"
     * sem a entrada e é entregue com quem retirou, sem km de saída.
     */
    @Test
    void soChicoteComecaSemEntradaEEEntregueComQuemRetirou() {
        long os = aprovadaSemEntrada(chicote());
        NecessidadeDoVeiculo necessidade = oficina.buscar(os).necessidadeDoVeiculo();
        assertTrue(necessidade.temBancada());
        assertFalse(necessidade.exigeVeiculo());

        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");
        assertEquals(StatusOs.EM_EXECUCAO, oficina.buscar(os).os().status(), "começou sem a entrada");
        assertFalse(checklist(os, CHICOTE).bloqueado(), "o checklist da bancada se marca sem o veículo");
        marcarFeitos(os, CHICOTE);
        oficina.mudarStatus(os, StatusOs.PRONTA, "");

        assertEquals(List.of("Escreva quem retirou: o veículo não passou pela oficina."), assertThrows(
                EntradaInvalidaException.class, () -> oficina.entregar(os, new Entrega(agora(), null,
                        new Garantia(90)))).erros());
        assertEquals(List.of("O veículo não passou pela oficina: a entrega não tem km (nem horas de motor) de "
                + "saída."), assertThrows(EntradaInvalidaException.class, () -> oficina.entregar(os,
                        new Entrega(agora(), 120_000.0, new Garantia(90), "Oficina do João"))).erros());
        assertEquals(StatusOs.PRONTA, oficina.buscar(os).os().status(), "nada foi gravado");

        oficina.entregar(os, new Entrega(agora(), null, new Garantia(90), "Oficina do João"));
        Entrega entrega = oficina.buscar(os).os().entrega();
        assertEquals("Oficina do João", entrega.quemRetirou());
        assertNull(entrega.usoSaida(), "sem km de saída");
        assertEquals(StatusOs.ENTREGUE, oficina.buscar(os).os().status());

        oficina.anonimizarCliente(oficina.buscar(os).os().clienteId());
        assertEquals("(apagado na anonimização)", oficina.buscar(os).os().entrega().quemRetirou(),
                "versão 0.3.2 (decisão 279): a anonimização apaga quem retirou, também na OS entregue");
        assertEquals(StatusOs.ENTREGUE, oficina.buscar(os).os().status());
    }

    /**
     * Teste nº 10 da Fase 3B: com chicote e instalação, a bancada começa sem a entrada, o checklist da instalação fica
     * bloqueado até a entrada, e o "pronto" exige a entrada registrada.
     */
    @Test
    void chicoteEInstalacaoBloqueiamAteAEntrada() {
        long os = aprovadaSemEntrada(chicote(), instalacao());
        NecessidadeDoVeiculo necessidade = oficina.buscar(os).necessidadeDoVeiculo();
        assertTrue(necessidade.temBancada());
        assertEquals(List.of(INSTALACAO), necessidade.comVeiculo());

        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");
        assertFalse(checklist(os, CHICOTE).bloqueado(), "a bancada começa sem o veículo");
        assertTrue(checklist(os, INSTALACAO).bloqueado(), "a instalação espera a entrada");
        long itemDaInstalacao = checklist(os, INSTALACAO).checklist().itens().getFirst().id();
        EntradaInvalidaException bloqueado = assertThrows(EntradaInvalidaException.class, () -> oficina.marcar(os,
                List.of(new MarcacaoDoItem(itemDaInstalacao, new Marcacao(SituacaoItem.FEITO, "", ""))), null));
        assertEquals(List.of("O checklist de " + INSTALACAO + " fica bloqueado até a entrada do veículo (aba "
                + "Entrada)."), bloqueado.erros());
        marcarFeitos(os, CHICOTE);

        OperacaoRecusadaException pronto = assertThrows(OperacaoRecusadaException.class,
                () -> oficina.mudarStatus(os, StatusOs.PRONTA, ""));
        assertEquals("Registre a entrada do veículo (aba Entrada) antes do pronto: " + INSTALACAO + " precisa do "
                + "veículo na oficina.", pronto.getMessage());

        oficina.registrarEntrada(os, new EntradaVeiculo(agora(), 120_000.0, NivelCombustivel.MEIO,
                new Vistoria(List.of(new ItemVistoria("Lataria", "ok")), "")));
        assertFalse(checklist(os, INSTALACAO).bloqueado(), "com a entrada, o checklist da instalação libera");
        marcarFeitos(os, INSTALACAO);
        oficina.mudarStatus(os, StatusOs.PRONTA, "");
        relogio.andar(Duration.ofHours(1));
        oficina.entregar(os, new Entrega(agora(), 120_010.0, new Garantia(90)));
        assertEquals("", oficina.buscar(os).os().entrega().quemRetirou(), "com o veículo, a entrega de sempre");
    }

    /**
     * Versão 0.3.2 (decisão 278): a OS só de bancada, pronta sem o veículo, ganha numa revisão a instalação, que
     * precisa dele. A entrega fica travada: ela pede a entrada e o checklist da instalação. Com a entrada e os itens
     * marcados, a entrega volta a passar, com o km.
     */
    @Test
    void servicoQuePrecisaDoVeiculoAprovadoDepoisDoProntoTravaAEntrega() {
        ItemCatalogo<ServicoPadrao> instalacao = instalacao();
        long os = aprovadaSemEntrada(chicote());
        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");
        marcarFeitos(os, CHICOTE);
        oficina.mudarStatus(os, StatusOs.PRONTA, "");
        relogio.andar(Duration.ofMinutes(30));

        OrcamentoSalvo primeira = orcamentos.orcamentosDaOs(os).getFirst().orcamento();
        OrcamentoSalvo segunda = orcamentos.novaRevisao(primeira.id());
        List<ItemOrcamento> linhas = new ArrayList<>(primeira.conteudo().itens());
        linhas.add(ItemOrcamento.doServico(instalacao.dados().textoNoOrcamento(), instalacao.dados().horas(), null,
                HORA, instalacao.id(), instalacao.versao()));
        orcamentos.salvar(segunda.id(), new Orcamento(linhas, Desconto.NENHUM, 1, "", 15, "", ""));
        orcamentos.marcarEnviado(segunda.id());
        orcamentos.aprovar(segunda.id(), agora(), MeioAprovacao.WHATSAPP, "Pode instalar também.");
        relogio.andar(Duration.ofMinutes(10));

        List<String> travas = oficina.travasDaEntrega(os);
        assertEquals("Registre a entrada do veículo (aba Entrada) e retome o serviço: " + INSTALACAO + " precisa do "
                + "veículo na oficina.", travas.getFirst());
        assertTrue(travas.size() > 2, "e os itens do checklist da instalação: " + travas);
        assertEquals(travas, assertThrows(EntradaInvalidaException.class, () -> oficina.entregar(os,
                new Entrega(agora(), null, new Garantia(90), "Oficina do João"))).erros());

        oficina.registrarEntrada(os, new EntradaVeiculo(agora(), 120_000.0, NivelCombustivel.MEIO,
                new Vistoria(List.of(new ItemVistoria("Lataria", "ok")), "")));
        marcarFeitos(os, INSTALACAO);
        assertEquals(List.of(), oficina.travasDaEntrega(os));
        relogio.andar(Duration.ofHours(1));
        oficina.entregar(os, new Entrega(agora(), 120_010.0, new Garantia(90)));
        assertEquals(StatusOs.ENTREGUE, oficina.buscar(os).os().status());
    }

    /** Sem serviço de bancada (só o que exige o veículo, ou só linha digitada), começar pede a entrada, como antes. */
    @Test
    void semBancadaComecarPedeAEntrada() {
        long os = aprovadaSemEntrada(instalacao());
        assertFalse(oficina.buscar(os).necessidadeDoVeiculo().temBancada());
        OperacaoRecusadaException semEntrada = assertThrows(OperacaoRecusadaException.class,
                () -> oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, ""));
        assertEquals("Registre a entrada do veículo (km, tanque e vistoria) antes de começar o serviço.",
                semEntrada.getMessage());
        assertEquals(StatusOs.APROVADA, oficina.buscar(os).os().status());
    }
}
