package br.com.calibra.dados.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.combustivel.GasolinaComEtanol;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.ErroDados;

/** Catálogo no banco (SPEC 8.5): versões, histórico, arquivar, busca e a trava contra alterar versão. */
class CatalogoRepositorioTest {

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private CatalogoRepositorio catalogo;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta);
        catalogo = new CatalogoRepositorio(base.relogio);
    }

    /** Bico fictício com tabela de tempo morto (tensão × pressão), como viria do fabricante. */
    private static Bico bico(String nome, double vazao) {
        TempoMorto tabela = new TempoMorto(List.of(new TempoMorto.Ponto(12.0, 300, 1.10),
                new TempoMorto.Ponto(14.0, 300, 0.90)));
        return new Bico(nome, vazao, 300, 0.70, tabela, 12.0, "folha de dados de teste", LocalDate.of(2026, 10, 1));
    }

    private static InfoCatalogo info(String nome) {
        return new InfoCatalogo(nome, "folha de dados de teste", LocalDate.of(2026, 10, 1), false,
                new Dinheiro(45_000), "");
    }

    private ItemCatalogo<Bico> inserirBico(String nome, double vazao) {
        return base.banco.transacao(c -> catalogo.inserir(c, TipoCatalogo.BICO, bico(nome, vazao), info(nome), null));
    }

    @Test
    void gravaELeOItemComATabelaDeTempoMorto() {
        ItemCatalogo<Bico> novo = inserirBico("Bico teste 1050", 1050);
        ItemCatalogo<Bico> lido = base.banco.ler(c -> catalogo.buscar(c, TipoCatalogo.BICO, novo.id())).orElseThrow();
        assertEquals(1, lido.versao());
        assertEquals(bico("Bico teste 1050", 1050), lido.dados());
        assertEquals(info("Bico teste 1050"), lido.info());
        assertEquals(new Dinheiro(45_000), lido.info().precoReferencia());
        assertFalse(lido.arquivado());
    }

    @Test
    void editarCriaVersaoNovaEAAnteriorFicaComoEra() {
        ItemCatalogo<Bico> v1 = inserirBico("Bico teste 1050", 1050);
        ItemCatalogo<Bico> v2 = base.banco.transacao(c -> catalogo.novaVersao(c, TipoCatalogo.BICO, v1.id(),
                bico("Bico teste 1050", 1080), info("Bico teste 1050 (medido)")));
        assertEquals(2, v2.versao());

        List<VersaoCatalogo<Bico>> versoes = base.banco.ler(c -> catalogo.versoes(c, TipoCatalogo.BICO, v1.id()));
        assertEquals(List.of(2, 1), versoes.stream().map(VersaoCatalogo::versao).toList());
        assertEquals(1050, versoes.get(1).dados().vazaoNominalCm3Min(), 1e-9, "a v1 continua com 1050");
        assertEquals(1080, base.banco.ler(c -> catalogo.buscar(c, TipoCatalogo.BICO, v1.id())).orElseThrow()
                .dados().vazaoNominalCm3Min(), 1e-9);
        assertEquals("Bico teste 1050", base.banco.ler(c -> catalogo.versao(c, TipoCatalogo.BICO, v1.id(), 1))
                .orElseThrow().info().nome());
    }

    @Test
    void bancoRecusaAlterarOuApagarUmaVersao() {
        inserirBico("Bico teste 1050", 1050);
        ErroDados alterar = assertThrows(ErroDados.class, () -> executar("UPDATE catalogo_versao SET nome = 'x'"));
        assertTrue(alterar.getMessage().contains("Versão de catálogo não muda"), alterar.getMessage());
        assertThrows(ErroDados.class, () -> executar("DELETE FROM catalogo_versao"));
    }

    @Test
    void arquivadoSaiDaListaEVoltaAoReativar() {
        ItemCatalogo<Bico> pequeno = inserirBico("Bico teste 550", 550);
        inserirBico("Bico teste 1050", 1050);
        base.banco.transacao(c -> {
            catalogo.arquivar(c, pequeno.id(), true);
            return null;
        });
        assertEquals(List.of("Bico teste 1050"), nomes("", false));
        assertEquals(List.of("Bico teste 1050", "Bico teste 550"), nomes("", true));
        assertTrue(base.banco.ler(c -> catalogo.buscar(c, TipoCatalogo.BICO, pequeno.id())).orElseThrow().arquivado(),
                "arquivado continua no banco (cálculos antigos)");
        base.banco.transacao(c -> {
            catalogo.arquivar(c, pequeno.id(), false);
            return null;
        });
        assertEquals(2, nomes("", false).size());
    }

    @Test
    void buscaSemAcentoNoNomeFonteEObservacoes() {
        inserirBico("Bico teste 1050", 1050);
        base.banco.transacao(c -> catalogo.inserir(c, TipoCatalogo.BICO, bico("Injetor ação", 800),
                new InfoCatalogo("Injetor ação", "medição na bancada", null, true, null, "Usado no Opala"), null));
        assertEquals(List.of("Injetor ação"), nomes("ACAO", false));
        assertEquals(List.of("Injetor ação"), nomes("bancada", false));
        assertEquals(List.of("Injetor ação"), nomes("opala", false));
        assertEquals(List.of(), nomes("1050 opala", false));
    }

    /** SPEC 7.2: editar um combustível cria versão nova, e o record leva o número da versão (o cálculo guarda). */
    @Test
    void combustivelLevaONumeroDaVersao() {
        ItemCatalogo<Combustivel> e32 = base.banco.ler(c -> catalogo.porChaveSemente(c, TipoCatalogo.COMBUSTIVEL,
                "combustivel:E32")).orElseThrow();
        assertEquals(1, e32.dados().versao());
        Combustivel e35 = new Combustivel("E32", "Gasolina C comum (E35 em teste)", new GasolinaComEtanol(0.35),
                0.756, 0.80, 0.87, null, LocalDate.of(2027, 1, 1), "portaria de teste", 1);
        ItemCatalogo<Combustivel> v2 = base.banco.transacao(c -> catalogo.novaVersao(c, TipoCatalogo.COMBUSTIVEL,
                e32.id(), e35, e32.info()));
        assertEquals(2, v2.dados().versao());
        assertEquals(2, base.banco.ler(c -> catalogo.buscar(c, TipoCatalogo.COMBUSTIVEL, e32.id())).orElseThrow()
                .dados().versao());
        assertEquals(CombustiveisPadrao.E32, base.banco.ler(c -> catalogo.versao(c, TipoCatalogo.COMBUSTIVEL,
                e32.id(), 1)).orElseThrow().dados(), "a v1 continua igual à SPEC");
    }

    @Test
    void resumoComNomeEVersaoAtual() {
        ItemCatalogo<Bico> v1 = inserirBico("Bico teste 1050", 1050);
        base.banco.transacao(c -> catalogo.novaVersao(c, TipoCatalogo.BICO, v1.id(), bico("Bico teste 1050", 1050),
                info("Bico teste 1050")));
        Map<Long, ResumoItem> resumos = base.banco.ler(c -> catalogo.resumos(c, List.of(v1.id(), 99_999L)));
        assertEquals(1, resumos.size(), "id que não existe fica de fora");
        assertEquals(new ResumoItem(v1.id(), "BICO", "Bico teste 1050", 2, false), resumos.get(v1.id()));
    }

    private List<String> nomes(String termo, boolean incluirArquivados) {
        return base.banco.ler(c -> catalogo.listar(c, TipoCatalogo.BICO, termo, incluirArquivados)).stream()
                .map(item -> item.info().nome()).toList();
    }

    private void executar(String sql) {
        base.banco.transacao(c -> {
            try (PreparedStatement comando = c.prepareStatement(sql)) {
                return comando.executeUpdate();
            }
        });
    }
}
