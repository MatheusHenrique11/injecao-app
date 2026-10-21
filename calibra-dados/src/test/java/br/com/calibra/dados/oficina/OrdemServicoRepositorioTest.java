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
import java.util.Map;
import java.util.Set;

import org.flywaydb.core.Flyway;
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
import br.com.calibra.dados.Migracoes;
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

    /** Os cartões do Painel: quantas OS em cada status, contando também as finalizadas. */
    @Test
    void contagemPorStatus() {
        assertEquals(Map.of(), base.banco.ler(ordens::contarPorStatus), "banco sem OS");
        long primeira = abrirOs(1, cliente, veiculo, null);
        abrirOs(2, cliente, veiculo, null);
        long terceira = abrirOs(3, cliente, veiculo, null);
        passo(primeira, StatusOs.AGUARDANDO_APROVACAO);
        base.banco.transacao(c -> {
            ordens.cancelar(c, terceira, "O cliente desistiu.");
            return null;
        });
        assertEquals(Map.of(StatusOs.ABERTA, 1, StatusOs.AGUARDANDO_APROVACAO, 1, StatusOs.CANCELADA, 1),
                base.banco.ler(ordens::contarPorStatus));
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

    /** Fase 3B (V12): a OS em que o veículo não passou pela oficina é entregue com quem retirou, sem km. */
    @Test
    void entregaComQuemRetirou() {
        long id = abrirOs(1, cliente, veiculo, null);
        ateFicarPronta(id);
        Entrega semVeiculo = new Entrega(LocalDateTime.of(2026, 10, 16, 17, 0), null, new Garantia(90),
                "Oficina do João");
        base.banco.transacao(c -> {
            ordens.entregar(c, id, semVeiculo);
            return null;
        });
        assertEquals(semVeiculo, buscar(id).entrega());
        assertNull(buscar(id).entrada(), "o veículo não passou pela oficina");

        long comVeiculo = abrirOs(2, cliente, veiculo, null);
        ateFicarPronta(comVeiculo);
        entregar(comVeiculo);
        assertEquals("", buscar(comVeiculo).entrega().quemRetirou(), "com o veículo, como antes");
    }

    private void corrigirAbertura(long id, String servico, Long mecanico, LocalDate previsao, String observacoes) {
        base.banco.transacao(c -> {
            ordens.atualizarAbertura(c, id, servico, mecanico, previsao, observacoes);
            return null;
        });
    }

    private void registrar(long id, EntradaVeiculo entrada) {
        base.banco.transacao(c -> {
            ordens.registrarEntrada(c, id, entrada);
            return null;
        });
    }

    /** As correções da OS como texto: "campo: antes → depois", da mais nova para a mais antiga. */
    private List<String> correcoes(long id) {
        return base.banco.ler(c -> ordens.correcoes(c, id)).stream()
                .map(e -> e.campo() + ": " + e.valorAntigo() + " → " + e.valorNovo()).toList();
    }

    /** Bloco 3B.7 (item 3 da decisão 211): a abertura corrigida guarda o antes e o depois, o mecânico pelo nome. */
    @Test
    void correcoesDaAbertura() {
        long id = abrirOs(1, cliente, veiculo, null);
        MecanicoRepositorio mecanicos = new MecanicoRepositorio(base.relogio);
        long carlos = base.banco.transacao(c -> mecanicos.inserir(c, "Carlos"));
        assertEquals(List.of(), correcoes(id), "a abertura não é correção");

        corrigirAbertura(id, "Barulho no turbo e falha na partida", carlos, LocalDate.of(2026, 10, 22), "");
        assertEquals(List.of("Serviço pedido: Barulho no turbo → Barulho no turbo e falha na partida",
                "Mecânico:  → Carlos", "Previsão de entrega: 20/10/2026 → 22/10/2026"), correcoes(id),
                "só o que mudou, na ordem da tela");

        corrigirAbertura(id, " Barulho no turbo e falha na partida ", carlos, LocalDate.of(2026, 10, 22), " ");
        assertEquals(3, correcoes(id).size(), "salvar sem mudar nada não é correção");

        corrigirAbertura(id, "Barulho no turbo e falha na partida", null, null, " Cliente espera na oficina ");
        assertEquals(List.of("Mecânico: Carlos → ", "Previsão de entrega: 22/10/2026 → ",
                "Observações:  → Cliente espera na oficina",
                "Serviço pedido: Barulho no turbo → Barulho no turbo e falha na partida", "Mecânico:  → Carlos",
                "Previsão de entrega: 20/10/2026 → 22/10/2026"), correcoes(id), "a correção mais nova primeiro");
    }

    /** Bloco 3B.7: a entrada corrigida guarda o antes e o depois de cada campo; o primeiro registro não é correção. */
    @Test
    void correcoesDaEntrada() {
        long id = abrirOs(1, cliente, veiculo, null);
        registrar(id, new EntradaVeiculo(ENTRADA, 251_500.0, NivelCombustivel.UM_QUARTO, new Vistoria(List.of(
                new ItemVistoria("Lataria", "risco na porta direita"), new ItemVistoria("Estepe", "ok")),
                "óculos no porta-luvas")));
        assertEquals(List.of(), correcoes(id), "a primeira entrada não é correção");

        registrar(id, new EntradaVeiculo(ENTRADA.withHour(8).withMinute(30), 251_550.5, NivelCombustivel.UM_QUARTO,
                new Vistoria(List.of(new ItemVistoria("Lataria", "risco nas duas portas"),
                        new ItemVistoria("Interior", ""), new ItemVistoria("Pneus", "dianteiro careca")), "")));
        assertEquals(List.of("Chegada: 11/10/2026 09:00 → 11/10/2026 08:30", "Km no painel: 251.500 → 251.550,5",
                "Vistoria: Lataria: risco na porta direita → risco nas duas portas",
                "Vistoria: Pneus:  → dianteiro careca", "Objetos deixados no veículo: óculos no porta-luvas → ",
                "Vistoria: Estepe: ok → "), correcoes(id),
                "o tanque não mudou; o item novo em branco não é correção; o item tirado fica vazio");
    }

    /** No barco, o uso aparece como "Horas de motor", como na aba Entrada; o item repetido ganha o número. */
    @Test
    void correcoesDaEntradaDeUmBarco() {
        long lancha = base.banco.transacao(c -> cadastros.veiculos().inserir(c, cliente, Cadastros.lancha("Maré Alta"),
                null));
        long id = abrirOs(1, cliente, lancha, null);
        registrar(id, new EntradaVeiculo(ENTRADA, 120.0, null, new Vistoria(List.of(new ItemVistoria("Casco", "ok"),
                new ItemVistoria("Casco", "risco na popa")), "")));
        registrar(id, new EntradaVeiculo(ENTRADA, 121.5, null, new Vistoria(List.of(new ItemVistoria("Casco", "ok"),
                new ItemVistoria("Casco", "risco fundo na popa")), "")));
        assertEquals(List.of("Horas de motor: 120 → 121,5",
                "Vistoria: Casco (2): risco na popa → risco fundo na popa"), correcoes(id));
    }

    /**
     * Versão 0.3.2 (V14, decisão 279): a anonimização troca o "quem retirou" das OS do cliente, também a entregue. O
     * banco continua recusando qualquer outra mudança na OS entregue, também junto com a troca, e outro texto no
     * "quem retirou".
     */
    @Test
    void quemRetirouSaiNaAnonimizacao() {
        long semVeiculo = abrirOs(1, cliente, veiculo, null);
        ateFicarPronta(semVeiculo);
        base.banco.transacao(c -> {
            ordens.entregar(c, semVeiculo, new Entrega(LocalDateTime.of(2026, 10, 16, 17, 0), null, new Garantia(90),
                    "João da Silva"));
            return null;
        });
        long comVeiculo = abrirOs(2, cliente, veiculo, null);
        ateFicarPronta(comVeiculo);
        entregar(comVeiculo);
        long maria = base.banco.transacao(c -> cadastros.clientes().inserir(c, Cadastros.cliente("Maria Souza")));
        long carroDaMaria = base.banco.transacao(c -> cadastros.veiculos().inserir(c, maria, Cadastros.carro("XYZ9A87"),
                null));
        long daMaria = abrirOs(3, maria, carroDaMaria, null);
        ateFicarPronta(daMaria);
        base.banco.transacao(c -> {
            ordens.entregar(c, daMaria, new Entrega(LocalDateTime.of(2026, 10, 16, 18, 0), null, new Garantia(90),
                    "Maria Souza"));
            return null;
        });
        OrdemServicoSalva antes = buscar(semVeiculo);

        assertEquals(1, (int) base.banco.transacao(c -> ordens.anonimizarQuemRetirou(c, cliente)));
        OrdemServicoSalva depois = buscar(semVeiculo);
        assertEquals("(apagado na anonimização)", depois.entrega().quemRetirou());
        assertEquals(antes.atualizadoEm(), depois.atualizadoEm(), "o resto da OS fica igual");
        assertEquals(StatusOs.ENTREGUE, depois.status());
        assertEquals("", buscar(comVeiculo).entrega().quemRetirou(), "a entregue com o veículo fica vazia");
        assertEquals("Maria Souza", buscar(daMaria).entrega().quemRetirou(), "a da outra cliente fica");
        assertEquals(0, (int) base.banco.transacao(c -> ordens.anonimizarQuemRetirou(c, cliente)), "de novo: nada");

        assertThrows(ErroDados.class, () -> executar("UPDATE ordem_servico SET retirado_por = 'Outro nome' WHERE id = "
                + daMaria), "outro texto no \"quem retirou\" é recusado");
        assertThrows(ErroDados.class, () -> executar("UPDATE ordem_servico SET retirado_por = "
                + "'(apagado na anonimização)', observacoes = 'x' WHERE id = " + daMaria), "junto com outra mudança");
        assertThrows(ErroDados.class, () -> executar("UPDATE ordem_servico SET observacoes = 'x' WHERE id = "
                + semVeiculo), "a OS entregue continua sem mudar");
    }

    /** A V14 sobre um banco que já está na V13, com uma OS entregue sem o veículo: o gatilho muda, e os dados não. */
    @Test
    void v14SobreUmBancoNaV13() throws Exception {
        BaseDeTeste antiga = BaseDeTeste.semMigrar(pasta.resolve("v13"));
        Flyway.configure().dataSource(antiga.banco.fonte()).locations(Migracoes.LOCAL_PADRAO).target("13").load()
                .migrate();
        Cadastros.Repositorios deAntes = Cadastros.Repositorios.de(antiga);
        long dono = antiga.banco.transacao(c -> deAntes.clientes().inserir(c, Cadastros.cliente("Ana Costa")));
        long carro = antiga.banco.transacao(c -> deAntes.veiculos().inserir(c, dono, Cadastros.carro("CDE3F45"), null));
        OrdemServicoRepositorio deAntesOs = new OrdemServicoRepositorio(antiga.relogio);
        long id = antiga.banco.transacao(c -> deAntesOs.inserir(c, new NumeroOs(2026, 1),
                new DadosAbertura(dono, carro, null, "Chicote", null, null, "")));
        antiga.banco.transacao(c -> {
            try (Statement comando = c.createStatement()) {
                return comando.executeUpdate("UPDATE ordem_servico SET status = 'ENTREGUE', "
                        + "entregue_em = '2026-10-16T17:00:00', garantia_dias = 90, garantia_ate = '2027-01-14', "
                        + "retirado_por = 'Ana Costa' WHERE id = " + id);
            }
        });
        Migracoes.Resultado resultado = Migracoes.migrar(antiga.banco, antiga.backup);
        assertEquals("13", resultado.versaoAntes());
        assertEquals(BaseDeTeste.VERSAO_BANCO, resultado.versaoDepois());
        assertEquals("Ana Costa", antiga.banco.ler(c -> deAntesOs.buscar(c, id)).orElseThrow().entrega()
                .quemRetirou(), "a migração não muda dado");
        assertEquals(1, (int) antiga.banco.transacao(c -> deAntesOs.anonimizarQuemRetirou(c, dono)));
    }

    /** A V12 sobre um banco que já está na V11, com uma OS entregue: ela fica com "quem retirou" vazio. */
    @Test
    void v12SobreUmBancoNaV11() throws Exception {
        BaseDeTeste antiga = BaseDeTeste.semMigrar(pasta.resolve("v11"));
        Flyway.configure().dataSource(antiga.banco.fonte()).locations(Migracoes.LOCAL_PADRAO).target("11").load()
                .migrate();
        Cadastros.Repositorios deAntes = Cadastros.Repositorios.de(antiga);
        long dono = antiga.banco.transacao(c -> deAntes.clientes().inserir(c, Cadastros.cliente("Ana Costa")));
        long carro = antiga.banco.transacao(c -> deAntes.veiculos().inserir(c, dono, Cadastros.carro("CDE3F45"), null));
        OrdemServicoRepositorio deAntesOs = new OrdemServicoRepositorio(antiga.relogio);
        long id = antiga.banco.transacao(c -> deAntesOs.inserir(c, new NumeroOs(2026, 1),
                new DadosAbertura(dono, carro, null, "Revisão", null, null, "")));
        antiga.banco.transacao(c -> {
            try (Statement comando = c.createStatement()) {
                return comando.executeUpdate("UPDATE ordem_servico SET status = 'ENTREGUE', "
                        + "entregue_em = '2026-10-16T17:00:00', uso_saida = 251530.0, garantia_dias = 90, "
                        + "garantia_ate = '2027-01-14' WHERE id = " + id);
            }
        });
        Migracoes.Resultado resultado = Migracoes.migrar(antiga.banco, antiga.backup);
        assertEquals("11", resultado.versaoAntes());
        assertEquals(BaseDeTeste.VERSAO_BANCO, resultado.versaoDepois());
        OrdemServicoSalva entregue = antiga.banco.ler(c -> deAntesOs.buscar(c, id)).orElseThrow();
        assertEquals(new Entrega(LocalDateTime.of(2026, 10, 16, 17, 0), 251_530.0, new Garantia(90)),
                entregue.entrega(), "a entrega de antes, com \"quem retirou\" vazio");
    }
}
