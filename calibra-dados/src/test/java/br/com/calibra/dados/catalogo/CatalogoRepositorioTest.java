package br.com.calibra.dados.catalogo;

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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.combustivel.GasolinaComEtanol;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.core.util.TextoBusca;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.json.Json;

/** Catálogo no banco (SPEC 8.5): versões, histórico, arquivar, busca e a trava contra alterar versão. */
class CatalogoRepositorioTest {

    /** Dois serviços como o 0.3.0 grava (gerados pelo código daquela versão antes da Fase 3B). */
    private static final String SERVICOS_030 = "/br/com/calibra/dados/json/servico-0.3.0.json";

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

    /**
     * Teste nº 1 da Fase 3B: o serviço gravado pelo 0.3.0 (o JSON de {@code servico-0.3.0.json}, sem os campos da auto
     * elétrica) continua sendo lido: Outro, exige o veículo, sem descrição e com a garantia da OS. Nada é regravado.
     */
    @Test
    void servicoGravadoPeloApp030ContinuaLegivel() throws IOException {
        List<String> gravados = new ArrayList<>();
        try (InputStream dados = CatalogoRepositorioTest.class.getResourceAsStream(SERVICOS_030)) {
            assertNotNull(dados, "falta o arquivo " + SERVICOS_030);
            Json.arvore(new String(dados.readAllBytes(), StandardCharsets.UTF_8)).forEach(no -> gravados.add(
                    Json.escrever(no)));
        }
        assertFalse(gravados.getFirst().contains("categoria"), "o arquivo é do 0.3.0: sem os campos novos");
        long limpeza = inserirComoNo030(gravados.get(0), "Limpeza de bicos", "2026-10-17");
        long diagnostico = inserirComoNo030(gravados.get(1), "Diagnóstico com scanner", null);

        List<ItemCatalogo<ServicoPadrao>> lista = base.banco.ler(c -> catalogo.listar(c, TipoCatalogo.SERVICO, "",
                false));
        assertEquals(List.of(diagnostico, limpeza), lista.stream().map(ItemCatalogo::id).toList(), "ordem de nome");
        ServicoPadrao horas = base.banco.ler(c -> catalogo.buscar(c, TipoCatalogo.SERVICO, limpeza)).orElseThrow()
                .dados();
        assertEquals(new ServicoPadrao("Limpeza de bicos", 1.5, "Tabela da oficina", LocalDate.of(2026, 10, 17),
                CategoriaServico.OUTRO, "", true, null, null), horas);
        ServicoPadrao semHoras = base.banco.ler(c -> catalogo.versao(c, TipoCatalogo.SERVICO, diagnostico, 1))
                .orElseThrow().dados();
        assertEquals(CategoriaServico.OUTRO, semHoras.categoria());
        assertTrue(semHoras.exigeEntradaDoVeiculo());
        assertNull(semHoras.garantiaDias(), "a garantia da OS");
        assertEquals("Diagnóstico com scanner", semHoras.textoNoOrcamento(), "sem descrição, entra o nome");
        assertEquals(gravados.get(1), base.banco.ler(c -> {
            try (PreparedStatement consulta = c.prepareStatement(
                    "SELECT dados_json FROM catalogo_versao WHERE item_id = ?")) {
                consulta.setLong(1, diagnostico);
                try (ResultSet linha = consulta.executeQuery()) {
                    return linha.next() ? linha.getString(1) : null;
                }
            }
        }), "ler não regrava nada");
    }

    /**
     * Um serviço como o 0.3.0 gravava: o JSON sem os campos novos, direto nas duas tabelas.
     *
     * @param data a data da informação, como no JSON; null sem data
     */
    private long inserirComoNo030(String json, String nome, String data) {
        return base.banco.transacao(c -> {
            try (PreparedStatement item = c.prepareStatement("INSERT INTO catalogo_item (tipo, versao_atual, nome, "
                    + "fonte, data, a_verificar, observacoes, dados_json, busca, criado_em, atualizado_em) VALUES "
                    + "('SERVICO', 1, ?, 'Tabela da oficina', ?, 0, '', ?, ?, '2026-10-18T20:52', "
                    + "'2026-10-18T20:52')")) {
                item.setString(1, nome);
                item.setString(2, data);
                item.setString(3, json);
                item.setString(4, TextoBusca.juntar(nome, "Tabela da oficina"));
                item.executeUpdate();
            }
            long id = Sql.ultimoId(c);
            try (PreparedStatement versao = c.prepareStatement("INSERT INTO catalogo_versao (item_id, versao, nome, "
                    + "fonte, data, a_verificar, observacoes, dados_json, criada_em) VALUES "
                    + "(?, 1, ?, 'Tabela da oficina', ?, 0, '', ?, '2026-10-18T20:52')")) {
                versao.setLong(1, id);
                versao.setString(2, nome);
                versao.setString(3, data);
                versao.setString(4, json);
                versao.executeUpdate();
            }
            return id;
        });
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
