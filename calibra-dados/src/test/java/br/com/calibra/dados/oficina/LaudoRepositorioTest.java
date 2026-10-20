package br.com.calibra.dados.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.oficina.CodigoFalha;
import br.com.calibra.core.oficina.CondicaoMedicao;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.LaudoEletrico;
import br.com.calibra.core.oficina.Medicao;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.ResultadoLaudo;
import br.com.calibra.core.oficina.SituacaoCodigo;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Cadastros;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.json.Json;

/**
 * Os laudos de diagnóstico elétrico no banco (V10, bloco 3B.4): ida e volta, os laudos da OS e os do veículo, o que o
 * banco recusa depois da entrega ou do cancelamento, a V10 sobre um banco na V9 e o formato do JSON.
 */
class LaudoRepositorioTest {

    /** Um laudo como o 0.3.1 grava (o mesmo de {@code laudo-0.3.1.json}). */
    static final LaudoEletrico EXEMPLO = new LaudoEletrico(LocalDateTime.of(2026, 10, 20, 10, 30),
            "Bateria descarrega de um dia para o outro", List.of(
                    new Medicao("Tensão da bateria", "12,1", "V", CondicaoMedicao.REPOUSO, "12,4 a 12,7 V", true),
                    new Medicao("Consumo em repouso", "0,45", "A", CondicaoMedicao.OUTRA, "", false)),
            List.of(new CodigoFalha("P0562", "Tensão do sistema baixa", SituacaoCodigo.HISTORICO)),
            "Teste de fuga com o amperímetro, tirando um fusível de cada vez.",
            "Módulo do som ligado direto na bateria.", "Ligação do módulo pelo pós-chave, com relé.",
            "Conferir a bateria em 30 dias.", ResultadoLaudo.RESOLVIDO);

    private static final String JSON_031 = "/br/com/calibra/dados/json/laudo-0.3.1.json";

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private OrdemServicoRepositorio ordens;
    private LaudoRepositorio laudos;
    private long veiculo;
    private long os;
    private long carlos;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta.resolve("dados"));
        ordens = new OrdemServicoRepositorio(base.relogio);
        laudos = new LaudoRepositorio(base.relogio);
        Cadastros.Repositorios cadastros = Cadastros.Repositorios.de(base);
        veiculo = base.banco.transacao(c -> cadastros.veiculos().inserir(c,
                cadastros.clientes().inserir(c, Cadastros.cliente("Ana Costa")), Cadastros.carro("ABC1D23"), null));
        os = abrirOs(base, veiculo, 1);
        carlos = base.banco.transacao(c -> new MecanicoRepositorio(base.relogio).inserir(c, "Carlos"));
    }

    private static long abrirOs(BaseDeTeste b, long veiculoId, int sequencial) {
        return b.banco.transacao(c -> {
            long cliente = Cadastros.Repositorios.de(b).veiculos().buscar(c, veiculoId).orElseThrow().clienteId();
            return new OrdemServicoRepositorio(b.relogio).inserir(c, new NumeroOs(2026, sequencial),
                    new DadosAbertura(cliente, veiculoId, null, "Bateria descarregando", null, null, ""));
        });
    }

    private long inserir(long osId, Long mecanico, LaudoEletrico laudo) {
        return base.banco.transacao(c -> laudos.inserir(c, osId, mecanico, laudo));
    }

    private static LaudoEletrico em(LocalDateTime quando, String queixa) {
        return new LaudoEletrico(quando, queixa, List.of(), List.of(), "", "", "", "", null);
    }

    private void executar(String sql) {
        base.banco.transacao(c -> {
            try (Statement comando = c.createStatement()) {
                return comando.executeUpdate(sql);
            }
        });
    }

    @Test
    void laudoVaiEVolta() {
        long id = inserir(os, carlos, EXEMPLO);
        LaudoSalvo lido = base.banco.ler(c -> laudos.buscar(c, id)).orElseThrow();
        assertEquals(EXEMPLO, lido.laudo());
        assertEquals(os, lido.osId());
        assertEquals("OS-2026-0001", lido.numeroOs().texto());
        assertEquals(carlos, lido.mecanicoId());
        assertEquals("Carlos", lido.mecanico());
        assertNotNull(lido.criadoEm());

        LaudoEletrico corrigido = new LaudoEletrico(EXEMPLO.dataHora(), EXEMPLO.queixa(), EXEMPLO.medicoes(),
                EXEMPLO.codigos(), EXEMPLO.testesRealizados(), EXEMPLO.causa(), EXEMPLO.solucao(), "",
                ResultadoLaudo.PARCIAL);
        base.banco.transacao(c -> {
            laudos.salvar(c, id, null, corrigido);
            return null;
        });
        LaudoSalvo salvo = base.banco.ler(c -> laudos.buscar(c, id)).orElseThrow();
        assertEquals(corrigido, salvo.laudo());
        assertNull(salvo.mecanicoId(), "sem mecânico");
        assertNull(salvo.mecanico());
        assertTrue(salvo.atualizadoEm().isAfter(salvo.criadoEm()), "a hora da mudança fica gravada");

        base.banco.transacao(c -> {
            laudos.apagar(c, id);
            return null;
        });
        assertEquals(List.of(), base.banco.ler(c -> laudos.daOs(c, os)), "antes da entrega, dá para apagar");
        assertThrows(OperacaoRecusadaException.class, () -> base.banco.transacao(c -> {
            laudos.salvar(c, id, null, EXEMPLO);
            return null;
        }));
    }

    /** Na OS, do mais antigo para o mais novo; no veículo, os de todas as OS dele, do mais novo para o mais antigo. */
    @Test
    void laudosDaOsEDoVeiculo() {
        LocalDateTime dia = LocalDateTime.of(2026, 10, 20, 9, 0);
        long tarde = inserir(os, carlos, em(dia.plusHours(6), "Volta a descarregar à tarde"));
        long manha = inserir(os, null, em(dia, "Bateria descarrega"));
        long segunda = abrirOs(base, veiculo, 2);
        long outra = inserir(segunda, null, em(dia.plusDays(10), "Luz do alternador acesa"));
        long outroVeiculo = base.banco.transacao(c -> Cadastros.Repositorios.de(base).veiculos().inserir(c,
                Cadastros.Repositorios.de(base).clientes().inserir(c, Cadastros.cliente("Bruno Dias")),
                Cadastros.carro("BCD2E34"), null));
        inserir(abrirOs(base, outroVeiculo, 3), null, em(dia, "Não é deste veículo"));

        assertEquals(List.of(manha, tarde), base.banco.ler(c -> laudos.daOs(c, os)).stream().map(LaudoSalvo::id)
                .toList(), "pela data e hora do laudo");
        List<LaudoSalvo> doVeiculo = base.banco.ler(c -> laudos.doVeiculo(c, veiculo));
        assertEquals(List.of(outra, tarde, manha), doVeiculo.stream().map(LaudoSalvo::id).toList());
        assertEquals("OS-2026-0002", doVeiculo.getFirst().numeroOs().texto(), "com o número da OS de cada um");
    }

    /** Teste nº 6 da Fase 3B (a parte do laudo), no banco: a OS entregue ou cancelada não ganha nem muda laudo. */
    @Test
    void depoisDaEntregaOuDoCancelamentoNaoMuda() {
        long id = inserir(os, carlos, EXEMPLO);
        base.banco.transacao(c -> {
            for (StatusOs passo : List.of(StatusOs.AGUARDANDO_APROVACAO, StatusOs.APROVADA, StatusOs.EM_EXECUCAO,
                    StatusOs.PRONTA)) {
                ordens.mudarStatus(c, os, passo, "");
            }
            ordens.entregar(c, os, new Entrega(LocalDateTime.of(2026, 10, 21, 17, 0), 120_000.0, new Garantia(90)));
            return null;
        });
        ErroDados mudar = assertThrows(ErroDados.class, () -> base.banco.transacao(c -> {
            laudos.salvar(c, id, carlos, em(EXEMPLO.dataHora(), "Outra queixa"));
            return null;
        }));
        assertTrue(mudar.getMessage().contains("OS entregue ou cancelada: o laudo não muda."), mudar.getMessage());
        ErroDados apagar = assertThrows(ErroDados.class, () -> base.banco.transacao(c -> {
            laudos.apagar(c, id);
            return null;
        }));
        assertTrue(apagar.getMessage().contains("OS entregue ou cancelada: o laudo não é apagado."),
                apagar.getMessage());
        ErroDados novo = assertThrows(ErroDados.class, () -> inserir(os, null, EXEMPLO));
        assertTrue(novo.getMessage().contains("OS entregue ou cancelada não ganha laudo."), novo.getMessage());
        assertEquals(EXEMPLO, base.banco.ler(c -> laudos.buscar(c, id)).orElseThrow().laudo(), "dá para ler");

        long cancelada = abrirOs(base, veiculo, 2);
        long daCancelada = inserir(cancelada, null, EXEMPLO);
        base.banco.transacao(c -> {
            ordens.cancelar(c, cancelada, "O cliente desistiu.");
            return null;
        });
        assertThrows(ErroDados.class, () -> base.banco.transacao(c -> {
            laudos.apagar(c, daCancelada);
            return null;
        }));
    }

    @Test
    void oLaudoNaoMudaDeOsEPrecisaDeUmaOs() {
        long id = inserir(os, null, EXEMPLO);
        long outra = abrirOs(base, veiculo, 2);
        assertThrows(ErroDados.class, () -> executar("UPDATE laudo SET os_id = " + outra + " WHERE id = " + id));
        assertThrows(ErroDados.class, () -> inserir(999, null, EXEMPLO), "OS que não existe");
        assertThrows(ErroDados.class, () -> inserir(os, 999L, EXEMPLO), "mecânico que não existe");
    }

    /** O JSON do laudo como o 0.3.1 grava: continua sendo lido igual, e o formato não muda sem querer. */
    @Test
    void formatoDoJson() throws IOException {
        String json;
        try (InputStream dados = LaudoRepositorioTest.class.getResourceAsStream(JSON_031)) {
            assertNotNull(dados, "falta o arquivo " + JSON_031);
            json = new String(dados.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(EXEMPLO, Json.ler(json, LaudoEletrico.class));
        assertEquals(Json.arvore(json), Json.arvore(Json.escrever(EXEMPLO)), "os mesmos campos e valores");
    }

    /** A V10 sobre um banco que já está na V9 (com uma OS): só cria a tabela, e as chaves valem. */
    @Test
    void v10SobreUmBancoNaV9() throws Exception {
        BaseDeTeste antiga = BaseDeTeste.semMigrar(pasta.resolve("v9"));
        Flyway.configure().dataSource(antiga.banco.fonte()).locations(Migracoes.LOCAL_PADRAO).target("9").load()
                .migrate();
        long veiculoAntigo = antiga.banco.transacao(c -> Cadastros.Repositorios.de(antiga).veiculos().inserir(c,
                Cadastros.Repositorios.de(antiga).clientes().inserir(c, Cadastros.cliente("Ana Costa")),
                Cadastros.carro("CDE3F45"), null));
        long osAntiga = abrirOs(antiga, veiculoAntigo, 1);
        Migracoes.Resultado resultado = Migracoes.migrar(antiga.banco, antiga.backup);
        assertEquals("9", resultado.versaoAntes());
        assertEquals(BaseDeTeste.VERSAO_BANCO, resultado.versaoDepois());
        LaudoRepositorio repositorio = new LaudoRepositorio(antiga.relogio);
        assertEquals(List.of(), antiga.banco.ler(c -> repositorio.daOs(c, osAntiga)), "a OS que já existia fica sem");
        assertEquals(1, (long) antiga.banco.transacao(c -> repositorio.inserir(c, osAntiga, null, EXEMPLO)));
    }
}
