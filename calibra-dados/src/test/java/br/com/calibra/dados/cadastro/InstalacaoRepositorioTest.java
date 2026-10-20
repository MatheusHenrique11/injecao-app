package br.com.calibra.dados.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.cadastro.Instalacao;
import br.com.calibra.core.cadastro.TipoInstalacao;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Cadastros;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.oficina.DadosAbertura;
import br.com.calibra.dados.oficina.OrdemServicoRepositorio;

/**
 * As instalações do veículo no banco (V11, bloco 3B.5): ida e volta, o histórico de cada mudança, a retirada, apagar,
 * a ordem da lista, a OS de origem do mesmo veículo, o veículo arquivado e o cliente anonimizado, e a V11 sobre um
 * banco na V10.
 */
class InstalacaoRepositorioTest {

    static final Instalacao SOM = new Instalacao(TipoInstalacao.SOM, "Módulo no porta-malas, com fusível de 80 A",
            "Taramps", "TS 800x4", "800 W RMS", LocalDate.of(2026, 10, 18), "Cabo de 21 mm²");

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private Cadastros.Repositorios cadastros;
    private InstalacaoRepositorio instalacoes;
    private long cliente;
    private long veiculo;
    private long os;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta.resolve("dados"));
        cadastros = Cadastros.Repositorios.de(base);
        instalacoes = new InstalacaoRepositorio(base.relogio);
        cliente = base.banco.transacao(c -> cadastros.clientes().inserir(c, Cadastros.cliente("Ana Costa")));
        veiculo = base.banco.transacao(c -> cadastros.veiculos().inserir(c, cliente, Cadastros.carro("ABC1D23"), null));
        os = abrirOs(base, veiculo, 1);
    }

    private static long abrirOs(BaseDeTeste b, long veiculoId, int sequencial) {
        return b.banco.transacao(c -> {
            long dono = Cadastros.Repositorios.de(b).veiculos().buscar(c, veiculoId).orElseThrow().clienteId();
            return new OrdemServicoRepositorio(b.relogio).inserir(c, new NumeroOs(2026, sequencial),
                    new DadosAbertura(dono, veiculoId, null, "Instalar o som", null, null, ""));
        });
    }

    private long inserir(Long osId, Instalacao instalacao) {
        return base.banco.transacao(c -> instalacoes.inserir(c, veiculo, osId, instalacao));
    }

    private InstalacaoSalva buscar(long id) {
        return base.banco.ler(c -> instalacoes.buscar(c, id)).orElseThrow();
    }

    private List<EventoAuditoria> historico(long id) {
        return base.banco.ler(c -> instalacoes.historico(c, id));
    }

    private static Instalacao com(TipoInstalacao tipo, String descricao, LocalDate data) {
        return new Instalacao(tipo, descricao, "", "", "", data, "");
    }

    @Test
    void vaiEVolta() {
        long id = inserir(os, SOM);
        InstalacaoSalva lida = buscar(id);
        assertEquals(SOM, lida.instalacao());
        assertEquals(veiculo, lida.veiculoId());
        assertEquals(os, lida.osId());
        assertEquals("OS-2026-0001", lida.numeroOs().texto());
        assertNull(lida.retiradaEm());
        assertNotNull(lida.criadoEm());

        Instalacao alarme = new Instalacao(TipoInstalacao.ALARME_RASTREADOR, "Alarme informado pelo cliente", "", "",
                "", null, "");
        InstalacaoSalva semOs = buscar(inserir(null, alarme));
        assertEquals(alarme, semOs.instalacao());
        assertNull(semOs.osId(), "sem OS: informado pelo cliente");
        assertNull(semOs.numeroOs());
    }

    /** Cada gravação vai para o histórico, campo a campo; a OS de origem aparece pelo número. */
    @Test
    void historicoDeCadaMudanca() {
        long id = inserir(null, SOM);
        Instalacao mudada = new Instalacao(TipoInstalacao.SOM, SOM.descricao(), "Taramps", "TS 800x4",
                "800 W RMS, 4 canais", LocalDate.of(2026, 10, 19), SOM.observacoes());
        base.banco.transacao(c -> {
            instalacoes.salvar(c, id, os, mudada);
            return null;
        });
        assertEquals(mudada, buscar(id).instalacao());
        assertEquals(os, buscar(id).osId());

        List<EventoAuditoria> eventos = historico(id);
        assertEquals(List.of("Potência ou especificação", "Data da instalação", "OS de origem", "Cadastro"),
                eventos.stream().map(EventoAuditoria::campo).toList(), "do mais novo para o mais antigo, na ordem "
                        + "da tela");
        assertEquals("800 W RMS", eventos.get(0).valorAntigo());
        assertEquals("800 W RMS, 4 canais", eventos.get(0).valorNovo());
        assertEquals("18/10/2026", eventos.get(1).valorAntigo());
        assertEquals("19/10/2026", eventos.get(1).valorNovo());
        assertEquals("", eventos.get(2).valorAntigo());
        assertEquals("OS-2026-0001", eventos.get(2).valorNovo());
        assertNull(eventos.get(3).valorAntigo());
        assertEquals("criada", eventos.get(3).valorNovo());
        assertTrue(eventos.getFirst().dataHora().isAfter(eventos.getLast().dataHora()), "data e hora gravadas");

        base.banco.transacao(c -> {
            instalacoes.salvar(c, id, os, mudada);
            return null;
        });
        assertEquals(4, historico(id).size(), "salvar sem mudar nada não grava histórico");
    }

    /** O item que saiu do carro continua na lista, com a data; desfazer volta, e os dois ficam no histórico. */
    @Test
    void retirarEDesfazer() {
        long id = inserir(os, SOM);
        LocalDate dia = LocalDate.of(2026, 10, 20);
        base.banco.transacao(c -> {
            instalacoes.retirar(c, id, dia);
            return null;
        });
        assertEquals(dia, buscar(id).retiradaEm());
        assertTrue(buscar(id).retirada());
        assertEquals(List.of(id), base.banco.ler(c -> instalacoes.doVeiculo(c, veiculo)).stream()
                .map(InstalacaoSalva::id).toList(), "continua na lista");
        EventoAuditoria retirada = historico(id).getFirst();
        assertEquals("Retirada do veículo", retirada.campo());
        assertEquals("", retirada.valorAntigo());
        assertEquals("20/10/2026", retirada.valorNovo());

        base.banco.transacao(c -> {
            instalacoes.retirar(c, id, dia);
            return null;
        });
        assertEquals(2, historico(id).size(), "retirar de novo na mesma data não muda nada");

        base.banco.transacao(c -> {
            instalacoes.retirar(c, id, null);
            return null;
        });
        assertNull(buscar(id).retiradaEm());
        EventoAuditoria desfeita = historico(id).getFirst();
        assertEquals("20/10/2026", desfeita.valorAntigo());
        assertEquals("", desfeita.valorNovo());
    }

    @Test
    void apagarLevaOHistorico() {
        long id = inserir(os, SOM);
        long outra = inserir(null, com(TipoInstalacao.OUTRA, "Tomada USB no console", null));
        base.banco.transacao(c -> {
            instalacoes.apagar(c, id);
            return null;
        });
        assertTrue(base.banco.ler(c -> instalacoes.buscar(c, id)).isEmpty());
        assertEquals(List.of(), historico(id));
        assertEquals(1, historico(outra).size(), "o histórico das outras fica");
        assertThrows(OperacaoRecusadaException.class, () -> base.banco.transacao(c -> {
            instalacoes.apagar(c, id);
            return null;
        }));
        assertThrows(OperacaoRecusadaException.class, () -> base.banco.transacao(c -> {
            instalacoes.salvar(c, id, null, SOM);
            return null;
        }));
    }

    /**
     * Na ficha: as que estão no veículo primeiro, da data mais nova para a mais antiga (sem data, no fim; na mesma
     * data, a cadastrada por último antes); as retiradas no fim. Na OS, só as dela.
     */
    @Test
    void ordemDaListaEAsDaOs() {
        long dia10 = inserir(null, com(TipoInstalacao.SOM, "Alto-falantes", LocalDate.of(2026, 10, 10)));
        long dia15 = inserir(os, com(TipoInstalacao.ILUMINACAO, "Farol de milha", LocalDate.of(2026, 10, 15)));
        long semData = inserir(null, com(TipoInstalacao.ALARME_RASTREADOR, "Alarme", null));
        long retirada = inserir(os, com(TipoInstalacao.SOM, "Som antigo", LocalDate.of(2026, 10, 18)));
        long semDataDepois = inserir(null, com(TipoInstalacao.OUTRA, "Tomada USB", null));
        base.banco.transacao(c -> {
            instalacoes.retirar(c, retirada, LocalDate.of(2026, 10, 19));
            return null;
        });
        long outroVeiculo = base.banco.transacao(c -> cadastros.veiculos().inserir(c, cliente,
                Cadastros.carro("BCD2E34"), null));
        base.banco.transacao(c -> instalacoes.inserir(c, outroVeiculo, null, com(TipoInstalacao.SOM, "Outro carro",
                null)));

        assertEquals(List.of(dia15, dia10, semDataDepois, semData, retirada), base.banco.ler(c -> instalacoes
                .doVeiculo(c, veiculo)).stream().map(InstalacaoSalva::id).toList());
        assertEquals(List.of(dia15, retirada), base.banco.ler(c -> instalacoes.daOs(c, os)).stream()
                .map(InstalacaoSalva::id).toList());
    }

    /** O banco recusa a OS de origem de outro veículo e a troca de veículo. */
    @Test
    void osDeOrigemDoMesmoVeiculo() {
        long outroVeiculo = base.banco.transacao(c -> cadastros.veiculos().inserir(c, cliente,
                Cadastros.carro("BCD2E34"), null));
        long osDoOutro = abrirOs(base, outroVeiculo, 2);
        ErroDados nova = assertThrows(ErroDados.class, () -> inserir(osDoOutro, SOM));
        assertTrue(nova.getMessage().contains("A OS de origem precisa ser do mesmo veículo."), nova.getMessage());

        long id = inserir(os, SOM);
        ErroDados mudar = assertThrows(ErroDados.class, () -> base.banco.transacao(c -> {
            instalacoes.salvar(c, id, osDoOutro, SOM);
            return null;
        }));
        assertTrue(mudar.getMessage().contains("A OS de origem precisa ser do mesmo veículo."), mudar.getMessage());
        assertThrows(ErroDados.class, () -> inserir(999L, SOM), "OS que não existe");
        ErroDados veiculoNovo = assertThrows(ErroDados.class, () -> base.banco.transacao(c -> {
            try (Statement comando = c.createStatement()) {
                return comando.executeUpdate("UPDATE instalacao SET veiculo_id = " + outroVeiculo + ", os_id = NULL "
                        + "WHERE id = " + id);
            }
        }));
        assertTrue(veiculoNovo.getMessage().contains("A instalação não muda de veículo."), veiculoNovo.getMessage());
        assertThrows(ErroDados.class, () -> base.banco.transacao(c -> instalacoes.inserir(c, 999, null, SOM)),
                "veículo que não existe");
        assertEquals(os, buscar(id).osId(), "nada mudou");
    }

    /** SPEC 6.2 e teste nº 7 da Fase 3B: o veículo arquivado e o cliente anonimizado mantêm as instalações. */
    @Test
    void veiculoArquivadoEClienteAnonimizadoMantem() {
        long id = inserir(os, SOM);
        base.banco.transacao(c -> {
            instalacoes.salvar(c, id, os, new Instalacao(SOM.tipo(), SOM.descricao(), SOM.marca(), SOM.modelo(),
                    "800 W RMS, 4 canais", SOM.dataInstalacao(), SOM.observacoes()));
            return null;
        });
        InstalacaoSalva antes = buscar(id);
        List<EventoAuditoria> historicoAntes = historico(id);

        base.banco.transacao(c -> {
            cadastros.veiculos().arquivar(c, veiculo, true);
            cadastros.clientes().anonimizar(c, cliente);
            return null;
        });
        assertEquals(List.of(antes), base.banco.ler(c -> instalacoes.doVeiculo(c, veiculo)));
        assertEquals(historicoAntes, historico(id), "o histórico da instalação fica igual");
    }

    /** A V11 sobre um banco que já está na V10 (com um veículo e uma OS): só cria a tabela, e as chaves valem. */
    @Test
    void v11SobreUmBancoNaV10() throws Exception {
        BaseDeTeste antiga = BaseDeTeste.semMigrar(pasta.resolve("v10"));
        Flyway.configure().dataSource(antiga.banco.fonte()).locations(Migracoes.LOCAL_PADRAO).target("10").load()
                .migrate();
        Cadastros.Repositorios deAntes = Cadastros.Repositorios.de(antiga);
        long veiculoAntigo = antiga.banco.transacao(c -> deAntes.veiculos().inserir(c,
                deAntes.clientes().inserir(c, Cadastros.cliente("Ana Costa")), Cadastros.carro("CDE3F45"), null));
        long osAntiga = abrirOs(antiga, veiculoAntigo, 1);
        Migracoes.Resultado resultado = Migracoes.migrar(antiga.banco, antiga.backup);
        assertEquals("10", resultado.versaoAntes());
        assertEquals(BaseDeTeste.VERSAO_BANCO, resultado.versaoDepois());
        InstalacaoRepositorio repositorio = new InstalacaoRepositorio(antiga.relogio);
        assertEquals(List.of(), antiga.banco.ler(c -> repositorio.doVeiculo(c, veiculoAntigo)), "o veículo fica sem");
        long id = antiga.banco.transacao(c -> repositorio.inserir(c, veiculoAntigo, osAntiga, SOM));
        assertEquals(SOM, antiga.banco.ler(c -> repositorio.buscar(c, id)).orElseThrow().instalacao());
    }
}
