package br.com.calibra.dados.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.FichaChicote;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.PontoChicote;
import br.com.calibra.core.oficina.SituacaoChicote;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Cadastros;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.json.Json;

/**
 * As fichas de chicote no banco (V13, bloco 3B.6): ida e volta, a versão nova que não apaga a anterior (teste nº 13
 * da Fase 3B), as fichas da OS e do veículo, o que o banco recusa depois da entrega, a situação "entregue" na entrega,
 * o formato do JSON e a V13 sobre um banco na V12.
 */
class FichaChicoteRepositorioTest {

    /** Uma ficha como o 0.3.1 grava (a mesma de {@code ficha-chicote-0.3.1.json}). */
    static final FichaChicote EXEMPLO = new FichaChicote("FuelTech FT450", 31L, 7L, "v1",
            "Manual da FT450, pinagem do conector A", List.of(
                    new PontoChicote("Bico do cilindro 1", "A", "12", "", false),
                    new PontoChicote("TPS", "B", "4", "sinal de 0 a 5 V", true)),
            "Cabo 1 mm², conectores Deutsch, conduíte de 10 mm", "Rota pela caixa de ar.", SituacaoChicote.TESTADO);

    private static final String JSON_031 = "/br/com/calibra/dados/json/ficha-chicote-0.3.1.json";

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private OrdemServicoRepositorio ordens;
    private FichaChicoteRepositorio fichas;
    private long veiculo;
    private long os;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta.resolve("dados"));
        ordens = new OrdemServicoRepositorio(base.relogio);
        fichas = new FichaChicoteRepositorio(base.relogio);
        Cadastros.Repositorios cadastros = Cadastros.Repositorios.de(base);
        veiculo = base.banco.transacao(c -> cadastros.veiculos().inserir(c,
                cadastros.clientes().inserir(c, Cadastros.cliente("Ana Costa")), Cadastros.carro("ABC1D23"), null));
        os = abrirOs(base, veiculo, 1);
    }

    private static long abrirOs(BaseDeTeste b, long veiculoId, int sequencial) {
        return b.banco.transacao(c -> {
            long cliente = Cadastros.Repositorios.de(b).veiculos().buscar(c, veiculoId).orElseThrow().clienteId();
            return new OrdemServicoRepositorio(b.relogio).inserir(c, new NumeroOs(2026, sequencial),
                    new DadosAbertura(cliente, veiculoId, null, "Chicote para injeção programável", null, null, ""));
        });
    }

    private long inserir(long osId, Long anterior, FichaChicote ficha) {
        return base.banco.transacao(c -> fichas.inserir(c, osId, anterior, ficha));
    }

    private FichaChicoteSalva buscar(long id) {
        return base.banco.ler(c -> fichas.buscar(c, id)).orElseThrow();
    }

    private void salvar(long id, FichaChicote ficha) {
        base.banco.transacao(c -> {
            fichas.salvar(c, id, ficha);
            return null;
        });
    }

    private void apagar(long id) {
        base.banco.transacao(c -> {
            fichas.apagar(c, id);
            return null;
        });
    }

    private static FichaChicote com(String versao, SituacaoChicote situacao) {
        return new FichaChicote(EXEMPLO.injecao(), EXEMPLO.injecaoItemId(), EXEMPLO.motorVersaoId(), versao,
                EXEMPLO.fontePinout(), EXEMPLO.pontos(), EXEMPLO.materiais(), EXEMPLO.observacoes(), situacao);
    }

    @Test
    void fichaVaiEVolta() {
        long id = inserir(os, null, EXEMPLO);
        FichaChicoteSalva lida = buscar(id);
        assertEquals(EXEMPLO, lida.ficha());
        assertEquals(os, lida.osId());
        assertEquals("OS-2026-0001", lida.numeroOs().texto());
        assertNull(lida.versaoAnteriorId());
        assertFalse(lida.temVersaoNova());
        assertNotNull(lida.criadoEm());

        FichaChicote testada = com("v1", SituacaoChicote.TESTADO);
        salvar(id, testada);
        assertEquals(testada, buscar(id).ficha());
        assertTrue(buscar(id).atualizadoEm().isAfter(lida.criadoEm()), "a hora da mudança fica gravada");

        apagar(id);
        assertEquals(List.of(), base.banco.ler(c -> fichas.daOs(c, os)), "a ficha feita por engano sai");
        assertThrows(OperacaoRecusadaException.class, () -> salvar(id, EXEMPLO));
        assertThrows(OperacaoRecusadaException.class, () -> apagar(id));
    }

    /** Teste nº 13 da Fase 3B: a versão nova do chicote não apaga a anterior, que fica só para ver. */
    @Test
    void versaoNovaNaoApagaAAnterior() {
        long v1 = inserir(os, null, EXEMPLO);
        long v2 = inserir(os, v1, EXEMPLO.versaoNova());
        assertEquals(List.of(v1, v2), base.banco.ler(c -> fichas.daOs(c, os)).stream().map(FichaChicoteSalva::id)
                .toList(), "as duas versões ficam");
        assertTrue(buscar(v1).temVersaoNova());
        assertEquals(v1, buscar(v2).versaoAnteriorId());
        assertEquals("v2", buscar(v2).ficha().versaoChicote());
        assertEquals(EXEMPLO, buscar(v1).ficha(), "a anterior fica como era");

        ErroDados mudar = assertThrows(ErroDados.class, () -> salvar(v1, com("v1", SituacaoChicote.ENTREGUE)));
        assertTrue(mudar.getMessage().contains("já tem uma versão nova: ela fica só para ver"), mudar.getMessage());
        ErroDados tirar = assertThrows(ErroDados.class, () -> apagar(v1));
        assertTrue(tirar.getMessage().contains("já tem uma versão nova: ela não é apagada"), tirar.getMessage());
        assertThrows(ErroDados.class, () -> inserir(os, v1, EXEMPLO.versaoNova()), "uma versão nova só de cada");
        long outraOs = abrirOs(base, veiculo, 2);
        ErroDados deOutra = assertThrows(ErroDados.class, () -> inserir(outraOs, v2, EXEMPLO));
        assertTrue(deOutra.getMessage().contains("é da mesma OS que a anterior"), deOutra.getMessage());

        apagar(v2);
        assertFalse(buscar(v1).temVersaoNova(), "apagada a nova (feita por engano), a anterior volta a mudar");
        salvar(v1, com("v1", SituacaoChicote.TESTADO));
    }

    /** Na OS, na ordem em que foram feitas; no veículo, as de todas as OS dele, da mais nova para a mais antiga. */
    @Test
    void fichasDaOsEDoVeiculo() {
        long primeira = inserir(os, null, EXEMPLO);
        long segundaOs = abrirOs(base, veiculo, 2);
        long daSegunda = inserir(segundaOs, null, com("v1", SituacaoChicote.EM_CONFECCAO));
        long outroVeiculo = base.banco.transacao(c -> Cadastros.Repositorios.de(base).veiculos().inserir(c,
                Cadastros.Repositorios.de(base).clientes().inserir(c, Cadastros.cliente("Bruno Dias")),
                Cadastros.carro("BCD2E34"), null));
        inserir(abrirOs(base, outroVeiculo, 3), null, EXEMPLO);

        assertEquals(List.of(primeira), base.banco.ler(c -> fichas.daOs(c, os)).stream().map(FichaChicoteSalva::id)
                .toList());
        List<FichaChicoteSalva> doVeiculo = base.banco.ler(c -> fichas.doVeiculo(c, veiculo));
        assertEquals(List.of(daSegunda, primeira), doVeiculo.stream().map(FichaChicoteSalva::id).toList());
        assertEquals("OS-2026-0002", doVeiculo.getFirst().numeroOs().texto(), "com o número da OS de cada uma");
    }

    /**
     * Na entrega, a última versão de cada chicote fica "entregue" (a anterior, como era); depois, o banco recusa criar,
     * mudar e apagar ficha da OS (o mesmo do teste nº 6, para a ficha).
     */
    @Test
    void naEntregaFicaEntregueEDepoisNaoMuda() {
        long v1 = inserir(os, null, EXEMPLO);
        long v2 = inserir(os, v1, EXEMPLO.versaoNova());
        base.banco.transacao(c -> {
            for (StatusOs passo : List.of(StatusOs.AGUARDANDO_APROVACAO, StatusOs.APROVADA, StatusOs.EM_EXECUCAO,
                    StatusOs.PRONTA)) {
                ordens.mudarStatus(c, os, passo, "");
            }
            assertEquals(1, fichas.marcarEntregues(c, os), "só a última versão");
            ordens.entregar(c, os, new Entrega(LocalDateTime.of(2026, 10, 21, 17, 0), null, new Garantia(90),
                    "O próprio cliente"));
            return null;
        });
        assertEquals(SituacaoChicote.ENTREGUE, buscar(v2).ficha().situacao());
        assertEquals(SituacaoChicote.TESTADO, buscar(v1).ficha().situacao(), "a versão anterior fica como era");

        ErroDados mudar = assertThrows(ErroDados.class, () -> salvar(v2, com("v2", SituacaoChicote.TESTADO)));
        assertTrue(mudar.getMessage().contains("OS entregue ou cancelada: a ficha do chicote não muda."),
                mudar.getMessage());
        ErroDados tirar = assertThrows(ErroDados.class, () -> apagar(v2));
        assertTrue(tirar.getMessage().contains("OS entregue ou cancelada: a ficha do chicote não é apagada."),
                tirar.getMessage());
        ErroDados nova = assertThrows(ErroDados.class, () -> inserir(os, null, EXEMPLO));
        assertTrue(nova.getMessage().contains("OS entregue ou cancelada não ganha ficha do chicote."),
                nova.getMessage());
        assertEquals(2, base.banco.ler(c -> fichas.daOs(c, os)).size(), "dá para ler");
    }

    /** O JSON da ficha como o 0.3.1 grava: continua sendo lido igual, e o formato não muda sem querer. */
    @Test
    void formatoDoJson() throws IOException {
        String json;
        try (InputStream dados = FichaChicoteRepositorioTest.class.getResourceAsStream(JSON_031)) {
            assertNotNull(dados, "falta o arquivo " + JSON_031);
            json = new String(dados.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(EXEMPLO, Json.ler(json, FichaChicote.class));
        assertEquals(Json.arvore(json), Json.arvore(Json.escrever(EXEMPLO)), "os mesmos campos e valores");
    }

    /** A V13 sobre um banco que já está na V12 (com uma OS): só cria a tabela, e as chaves valem. */
    @Test
    void v13SobreUmBancoNaV12() throws Exception {
        BaseDeTeste antiga = BaseDeTeste.semMigrar(pasta.resolve("v12"));
        Flyway.configure().dataSource(antiga.banco.fonte()).locations(Migracoes.LOCAL_PADRAO).target("12").load()
                .migrate();
        long veiculoAntigo = antiga.banco.transacao(c -> Cadastros.Repositorios.de(antiga).veiculos().inserir(c,
                Cadastros.Repositorios.de(antiga).clientes().inserir(c, Cadastros.cliente("Ana Costa")),
                Cadastros.carro("CDE3F45"), null));
        long osAntiga = abrirOs(antiga, veiculoAntigo, 1);
        Migracoes.Resultado resultado = Migracoes.migrar(antiga.banco, antiga.backup);
        assertEquals("12", resultado.versaoAntes());
        assertEquals(BaseDeTeste.VERSAO_BANCO, resultado.versaoDepois());
        FichaChicoteRepositorio repositorio = new FichaChicoteRepositorio(antiga.relogio);
        assertEquals(List.of(), antiga.banco.ler(c -> repositorio.daOs(c, osAntiga)), "a OS que já existia fica sem");
        long id = antiga.banco.transacao(c -> repositorio.inserir(c, osAntiga, null, EXEMPLO));
        assertEquals(EXEMPLO, antiga.banco.ler(c -> repositorio.buscar(c, id)).orElseThrow().ficha());
    }
}
