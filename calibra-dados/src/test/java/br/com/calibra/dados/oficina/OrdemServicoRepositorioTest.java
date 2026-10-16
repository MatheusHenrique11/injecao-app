package br.com.calibra.dados.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Cadastros;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.OperacaoRecusadaException;

/** Ordens de serviço no banco (SPEC 6.5): número por ano, entrada, passos com histórico, entrega, cancelamento e lista. */
class OrdemServicoRepositorioTest {

    private static final LocalDateTime ENTRADA = LocalDateTime.of(2026, 10, 11, 9, 0, 30, 500);

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private Cadastros.Repositorios cadastros;
    private OrdemServicoRepositorio ordens;
    private long cliente;
    private long veiculo;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta);
        cadastros = Cadastros.Repositorios.de(base);
        ordens = new OrdemServicoRepositorio(base.relogio);
        cliente = base.banco.transacao(c -> cadastros.clientes().inserir(c, Cadastros.cliente("João da Silva")));
        veiculo = base.banco.transacao(c -> cadastros.veiculos().inserir(c, cliente, Cadastros.carro("ABC1D23"), null));
    }

    private long abrirOs(int sequencial, long clienteId, long veiculoId, Long projeto) {
        return base.banco.transacao(c -> ordens.inserir(c, new NumeroOs(2026, sequencial),
                new DadosAbertura(clienteId, veiculoId, projeto, " Barulho no turbo ", null, LocalDate.of(2026, 10, 20),
                        "")));
    }

    private OrdemServicoSalva buscar(long id) {
        return base.banco.ler(c -> ordens.buscar(c, id)).orElseThrow();
    }

    private void passo(long id, StatusOs status) {
        base.banco.transacao(c -> {
            ordens.mudarStatus(c, id, status, "");
            return null;
        });
    }

    /** Leva a OS de ABERTA até PRONTA, pelos passos permitidos. */
    private void ateFicarPronta(long id) {
        for (StatusOs status : List.of(StatusOs.AGUARDANDO_APROVACAO, StatusOs.APROVADA, StatusOs.EM_EXECUCAO,
                StatusOs.PRONTA)) {
            passo(id, status);
        }
    }

    private void entregar(long id) {
        base.banco.transacao(c -> {
            ordens.entregar(c, id, new Entrega(LocalDateTime.of(2026, 10, 16, 17, 0), 251_530.0, new Garantia(90)));
            return null;
        });
    }

    private void executar(String sql) {
        base.banco.transacao(c -> {
            try (Statement comando = c.createStatement()) {
                return comando.executeUpdate(sql);
            }
        });
    }

    @Test
    void numeroPorAnoEAberturaSemEntrada() {
        assertEquals(1, (int) base.banco.ler(c -> ordens.proximoSequencial(c, 2026)));
        long id = abrirOs(1, cliente, veiculo, null);
        assertEquals(2, (int) base.banco.ler(c -> ordens.proximoSequencial(c, 2026)));
        assertEquals(1, (int) base.banco.ler(c -> ordens.proximoSequencial(c, 2027)), "a sequência recomeça no ano");
        assertThrows(ErroDados.class, () -> abrirOs(1, cliente, veiculo, null), "número não se repete");

        OrdemServicoSalva os = buscar(id);
        assertEquals("OS-2026-0001", os.numero().texto());
        assertEquals(StatusOs.ABERTA, os.status());
        assertEquals("Barulho no turbo", os.servicoPedido());
        assertEquals(LocalDate.of(2026, 10, 20), os.previsaoEntrega());
        assertNull(os.entrada(), "o orçamento pode vir antes do veículo");
        assertFalse(os.veiculoChegou());
        assertEquals(List.of(StatusOs.ABERTA), base.banco.ler(c -> ordens.historico(c, id)).stream()
                .map(MudancaStatusOs::status).toList());
    }

    @Test
    void entradaComVistoriaVoltaInteira() {
        long id = abrirOs(1, cliente, veiculo, null);
        EntradaVeiculo entrada = new EntradaVeiculo(ENTRADA, 251_500.0, NivelCombustivel.UM_QUARTO,
                new Vistoria(List.of(new ItemVistoria("Lataria", "risco na porta direita"), new ItemVistoria("Estepe",
                        "ok")), "óculos no porta-luvas"));
        base.banco.transacao(c -> {
            ordens.registrarEntrada(c, id, entrada);
            return null;
        });
        OrdemServicoSalva os = buscar(id);
        assertTrue(os.veiculoChegou());
        assertEquals(new EntradaVeiculo(ENTRADA.withNano(0), 251_500.0, NivelCombustivel.UM_QUARTO, entrada.vistoria()),
                os.entrada());
    }

    @Test
    void passosComHistoricoEntregaComGarantia() {
        long id = abrirOs(1, cliente, veiculo, null);
        OperacaoRecusadaException pulo = assertThrows(OperacaoRecusadaException.class,
                () -> passo(id, StatusOs.EM_EXECUCAO));
        assertEquals("A OS OS-2026-0001 está \"Aberta\" e não pode passar para \"Em execução\".", pulo.getMessage());

        passo(id, StatusOs.AGUARDANDO_APROVACAO);
        passo(id, StatusOs.APROVADA);
        passo(id, StatusOs.EM_EXECUCAO);
        passo(id, StatusOs.AGUARDANDO_PECAS);
        passo(id, StatusOs.EM_EXECUCAO);
        assertNull(buscar(id).prontaEm());
        passo(id, StatusOs.PRONTA);
        assertNotNull(buscar(id).prontaEm());
        entregar(id);

        OrdemServicoSalva entregue = buscar(id);
        assertEquals(StatusOs.ENTREGUE, entregue.status());
        assertEquals(new Entrega(LocalDateTime.of(2026, 10, 16, 17, 0), 251_530.0, new Garantia(90)), entregue.entrega());
        assertEquals(LocalDate.of(2027, 1, 14), entregue.garantiaAte());
        assertEquals(List.of(StatusOs.ABERTA, StatusOs.AGUARDANDO_APROVACAO, StatusOs.APROVADA, StatusOs.EM_EXECUCAO,
                StatusOs.AGUARDANDO_PECAS, StatusOs.EM_EXECUCAO, StatusOs.PRONTA, StatusOs.ENTREGUE),
                base.banco.ler(c -> ordens.historico(c, id)).stream().map(MudancaStatusOs::status).toList());
    }

    @Test
    void entregueNaoMudaNemSeApaga() {
        long id = abrirOs(1, cliente, veiculo, null);
        ateFicarPronta(id);
        entregar(id);
        OperacaoRecusadaException erro = assertThrows(OperacaoRecusadaException.class, () -> base.banco.transacao(c -> {
            ordens.registrarEntrada(c, id, new EntradaVeiculo(ENTRADA, null, null, null));
            return null;
        }));
        assertEquals("A OS OS-2026-0001 está \"Entregue\" e não muda mais.", erro.getMessage());
        assertThrows(ErroDados.class, () -> executar("UPDATE ordem_servico SET servico_pedido = 'x'"),
                "o banco também recusa");
        assertThrows(ErroDados.class, () -> executar("DELETE FROM ordem_servico"), "OS não se apaga");
        assertThrows(IllegalArgumentException.class, () -> passo(id, StatusOs.ENTREGUE), "entrega tem método próprio");
    }

    @Test
    void cancelarGuardaOMotivo() {
        long id = abrirOs(1, cliente, veiculo, null);
        passo(id, StatusOs.AGUARDANDO_APROVACAO);
        passo(id, StatusOs.APROVADA);
        base.banco.transacao(c -> {
            ordens.cancelar(c, id, " Cliente desistiu do serviço. ");
            return null;
        });
        OrdemServicoSalva cancelada = buscar(id);
        assertEquals(StatusOs.CANCELADA, cancelada.status());
        assertEquals("Cliente desistiu do serviço.", cancelada.motivoCancelamento());
        assertEquals("Cliente desistiu do serviço.", base.banco.ler(c -> ordens.historico(c, id)).getLast().observacao());
        assertThrows(OperacaoRecusadaException.class, () -> base.banco.transacao(c -> {
            ordens.cancelar(c, id, "de novo");
            return null;
        }));
    }

    @Test
    void listaComBuscaEFiltro() {
        long joao = abrirOs(1, cliente, veiculo, null);
        long outroCliente = base.banco.transacao(c -> cadastros.clientes().inserir(c, Cadastros.cliente("Maria Souza")));
        long lancha = base.banco.transacao(c -> cadastros.veiculos().inserir(c, outroCliente,
                Cadastros.lancha("Maré Alta"), null));
        long maria = abrirOs(2, outroCliente, lancha, null);
        ateFicarPronta(maria);
        entregar(maria);

        List<ResumoOs> emAndamento = base.banco.ler(c -> ordens.pesquisar(c, FiltroOs.emAndamento(), 50));
        assertEquals(List.of(joao), emAndamento.stream().map(ResumoOs::osId).toList(), "entregue sai da lista");
        ResumoOs linha = emAndamento.getFirst();
        assertEquals("João da Silva", linha.cliente());
        assertEquals("VW Gol GTi", linha.veiculo());
        assertEquals("ABC1D23", linha.identificacao());

        assertEquals(2, base.banco.ler(c -> ordens.pesquisar(c, FiltroOs.todas(), 50)).size());
        assertEquals(List.of(joao), ids(new FiltroOs("abc-1d23", Set.of(), true)), "pela placa");
        assertEquals(List.of(maria), ids(new FiltroOs("mare alta", Set.of(), true)), "pelo nome da embarcação");
        assertEquals(List.of(maria), ids(new FiltroOs("OS-2026-0002", Set.of(), true)), "pelo número");
        assertEquals(List.of(maria), ids(new FiltroOs("", Set.of(StatusOs.ENTREGUE), false)), "pelo status");
        assertEquals(List.of(), ids(new FiltroOs("maria", Set.of(), false)), "sem as finalizadas");
    }

    private List<Long> ids(FiltroOs filtro) {
        return base.banco.ler(c -> ordens.pesquisar(c, filtro, 50)).stream().map(ResumoOs::osId).toList();
    }

    /** A OS atual do projeto é a mais nova em andamento; projeto se liga uma vez. */
    @Test
    void osDoProjetoEDoVeiculo() {
        long projeto = base.banco.transacao(c -> cadastros.projetoCasoB(c, "Pedro Lima", "CDE3F45"));
        long veiculoDoProjeto = base.banco.ler(c -> cadastros.projetos().buscar(c, projeto)).orElseThrow().veiculoId();
        long clienteDoProjeto = base.banco.ler(c -> cadastros.veiculos().buscar(c, veiculoDoProjeto)).orElseThrow()
                .clienteId();
        long primeira = abrirOs(1, clienteDoProjeto, veiculoDoProjeto, projeto);
        assertEquals(primeira, base.banco.ler(c -> ordens.atualDoProjeto(c, projeto)).orElseThrow().id());
        base.banco.transacao(c -> {
            ordens.cancelar(c, primeira, "Orçamento recusado.");
            return null;
        });
        assertTrue(base.banco.ler(c -> ordens.atualDoProjeto(c, projeto)).isEmpty(), "cancelada não é a atual");

        long segunda = abrirOs(2, clienteDoProjeto, veiculoDoProjeto, null);
        base.banco.transacao(c -> {
            ordens.ligarProjeto(c, segunda, projeto);
            return null;
        });
        assertEquals(projeto, buscar(segunda).projetoId());
        assertThrows(OperacaoRecusadaException.class, () -> base.banco.transacao(c -> {
            ordens.ligarProjeto(c, segunda, projeto);
            return null;
        }), "só se liga uma vez");
        assertEquals(segunda, base.banco.ler(c -> ordens.atualDoProjeto(c, projeto)).orElseThrow().id());
        assertEquals(List.of(segunda, primeira), base.banco.ler(c -> ordens.doProjeto(c, projeto)).stream()
                .map(OrdemServicoSalva::id).toList());
        assertEquals(List.of(segunda, primeira), base.banco.ler(c -> ordens.doVeiculo(c, veiculoDoProjeto)).stream()
                .map(OrdemServicoSalva::id).toList());
    }
}
