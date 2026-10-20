package br.com.calibra.dados.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.oficina.ChecklistModelo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.ItemDoChecklist;
import br.com.calibra.core.oficina.Marcacao;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.SituacaoItem;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.dados.BaseDeTeste;
import br.com.calibra.dados.Cadastros;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.catalogo.CatalogoRepositorio;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.json.Json;

/**
 * Os checklists da OS no banco (V8, bloco 3B.3): a cópia do modelo com os itens pendentes, a marcação com quem e
 * quando, e o que o banco recusa (mudar o texto, apagar, "não se aplica" sem justificativa, mexer depois da entrega).
 */
class ChecklistRepositorioTest {

    private static final String FONTE = "Tabela da oficina";
    private static final ChecklistModelo SOM = new ChecklistModelo("Som", List.of(
            new ChecklistModelo.Item("Medir a tensão da bateria antes de começar", true, true),
            new ChecklistModelo.Item("Teste de ruído com o motor ligado", true, true),
            new ChecklistModelo.Item("Fotos de antes e depois", false, false)), FONTE, null);

    @TempDir
    Path pasta;

    private BaseDeTeste base;
    private OrdemServicoRepositorio ordens;
    private ChecklistRepositorio checklists;
    private long os;
    private long outraOs;
    private long servico;
    private long modelo;
    private long carlos;

    @BeforeEach
    void abrir() throws Exception {
        base = BaseDeTeste.criada(pasta.resolve("dados"));
        ordens = new OrdemServicoRepositorio(base.relogio);
        checklists = new ChecklistRepositorio(base.relogio);
        os = abrirOs(base, "ABC1D23", 1);
        outraOs = abrirOs(base, "BCD2E34", 2);
        CatalogoRepositorio catalogo = new CatalogoRepositorio(base.relogio);
        servico = base.banco.transacao(c -> catalogo.inserir(c, TipoCatalogo.SERVICO,
                new ServicoPadrao("Instalação de som completa", 4.0, FONTE, null, CategoriaServico.SOM, "", true, null,
                        null),
                new InfoCatalogo("Instalação de som completa", FONTE, null, false, null, ""), null).id());
        modelo = base.banco.transacao(ChecklistRepositorioTest::modeloNoCatalogo);
        carlos = base.banco.transacao(c -> new MecanicoRepositorio(base.relogio).inserir(c, "Carlos"));
    }

    private static long abrirOs(BaseDeTeste b, String placa, int sequencial) {
        Cadastros.Repositorios cadastros = Cadastros.Repositorios.de(b);
        return b.banco.transacao(c -> {
            long cliente = cadastros.clientes().inserir(c, Cadastros.cliente("João da Silva"));
            long veiculo = cadastros.veiculos().inserir(c, cliente, Cadastros.carro(placa), null);
            return new OrdemServicoRepositorio(b.relogio).inserir(c, new NumeroOs(2026, sequencial),
                    new DadosAbertura(cliente, veiculo, null, "Instalação de som", null, null, ""));
        });
    }

    /** O modelo no catálogo: para a cópia, basta o item existir (ela leva o texto, não lê o catálogo de novo). */
    private static long modeloNoCatalogo(Connection c) throws SQLException {
        try (PreparedStatement insercao = c.prepareStatement("INSERT INTO catalogo_item (tipo, versao_atual, nome, "
                + "fonte, a_verificar, observacoes, dados_json, busca, criado_em, atualizado_em) "
                + "VALUES ('CHECKLIST', 1, 'Som', ?, 1, '', ?, 'som', '2026-10-19T08:00:00', '2026-10-19T08:00:00')")) {
            insercao.setString(1, FONTE);
            insercao.setString(2, Json.escrever(SOM));
            insercao.executeUpdate();
        }
        return Sql.ultimoId(c);
    }

    private long copiar(long osId) {
        return base.banco.transacao(c -> checklists.copiar(c, osId, servico, "Instalação de som completa", modelo, 1,
                SOM));
    }

    private List<ItemChecklistSalvo> itens(long osId) {
        return base.banco.ler(c -> checklists.daOs(c, osId)).getFirst().itens();
    }

    private void marcar(long item, Marcacao marcacao, Long mecanico) {
        base.banco.transacao(c -> {
            checklists.marcar(c, item, marcacao, mecanico);
            return null;
        });
    }

    private void passos(long osId, StatusOs... passos) {
        base.banco.transacao(c -> {
            for (StatusOs passo : passos) {
                ordens.mudarStatus(c, osId, passo, "");
            }
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
    void copiaComOsItensDoModeloNaOrdemTodosPendentes() {
        long id = copiar(os);
        List<ChecklistSalvo> lidos = base.banco.ler(c -> checklists.daOs(c, os));
        assertEquals(1, lidos.size());
        ChecklistSalvo copia = lidos.getFirst();
        assertEquals(id, copia.id());
        assertEquals(os, copia.osId());
        assertEquals(servico, copia.servicoItemId());
        assertEquals("Instalação de som completa", copia.servico());
        assertEquals(modelo, copia.modeloItemId());
        assertEquals(1, copia.modeloVersao());
        assertEquals("Som", copia.modeloNome());
        assertNotNull(copia.criadoEm());
        assertEquals(SOM.itens().stream().map(ItemDoChecklist::copia).toList(),
                copia.itens().stream().map(ItemChecklistSalvo::item).toList(),
                "o texto, o obrigatório e o a verificar do modelo, todos pendentes");
        assertEquals(List.of(1, 2, 3), copia.itens().stream().map(ItemChecklistSalvo::ordem).toList());
        for (ItemChecklistSalvo item : copia.itens()) {
            assertNull(item.mecanicoId());
            assertNull(item.marcadoPor());
            assertNull(item.marcadoEm());
        }
        assertThrows(ErroDados.class, () -> copiar(os), "uma cópia só por serviço na OS");
        assertEquals(List.of(), base.banco.ler(c -> checklists.daOs(c, outraOs)), "a cópia é só da OS dela");
    }

    @Test
    void marcacaoComQuemEQuando() {
        copiar(os);
        List<ItemChecklistSalvo> itens = itens(os);
        marcar(itens.get(0).id(), new Marcacao(SituacaoItem.FEITO, " 12,6 V ", ""), carlos);
        marcar(itens.get(2).id(), new Marcacao(SituacaoItem.NAO_SE_APLICA, "", "Cliente não quis fotos."), null);

        List<ItemChecklistSalvo> marcados = itens(os);
        ItemChecklistSalvo tensao = marcados.get(0);
        assertEquals(new Marcacao(SituacaoItem.FEITO, "12,6 V", ""), tensao.item().marcacao());
        assertEquals(carlos, tensao.mecanicoId());
        assertEquals("Carlos", tensao.marcadoPor());
        LocalDateTime copiadoEm = base.banco.ler(c -> checklists.daOs(c, os)).getFirst().criadoEm();
        assertTrue(tensao.marcadoEm().isAfter(copiadoEm), "marcado depois da cópia");
        assertFalse(tensao.item().pendente());
        ItemChecklistSalvo fotos = marcados.get(2);
        assertEquals("Cliente não quis fotos.", fotos.item().marcacao().justificativa());
        assertNull(fotos.mecanicoId(), "sem mecânico, a marcação grava só quando");
        assertNull(fotos.marcadoPor());
        assertNotNull(fotos.marcadoEm());
        assertTrue(marcados.get(1).item().pendente(), "o item que ninguém marcou continua pendente");
        assertNull(marcados.get(1).marcadoEm());

        marcar(itens.get(0).id(), new Marcacao(SituacaoItem.PENDENTE, "", ""), carlos);
        assertTrue(itens(os).get(0).item().pendente(), "antes da entrega, dá para desfazer");
        assertThrows(OperacaoRecusadaException.class,
                () -> marcar(999, new Marcacao(SituacaoItem.FEITO, "", ""), carlos));
    }

    /** Teste nº 4 da Fase 3B, no banco: o "não se aplica" sem justificativa não grava. */
    @Test
    void naoSeAplicaSemJustificativaOBancoRecusa() {
        copiar(os);
        long item = itens(os).getFirst().id();
        assertThrows(ErroDados.class, () -> marcar(item, new Marcacao(SituacaoItem.NAO_SE_APLICA, "", "  "), carlos));
        assertThrows(ErroDados.class, () -> executar("UPDATE checklist_os_item SET situacao = 'NAO_SE_APLICA'"));
        assertThrows(ErroDados.class, () -> executar("UPDATE checklist_os_item SET situacao = 'OK'"),
                "só as três situações");
        assertTrue(itens(os).getFirst().item().pendente(), "nada foi gravado");
    }

    /** Teste nº 2 da Fase 3B, no banco: a cópia não muda de texto nem se apaga; só a marcação muda. */
    @Test
    void aCopiaNaoMudaNemSeApaga() {
        copiar(os);
        for (String sql : List.of("UPDATE checklist_os_item SET texto = 'Outro texto'",
                "UPDATE checklist_os_item SET obrigatorio = 0", "UPDATE checklist_os_item SET a_verificar = 0",
                "UPDATE checklist_os_item SET ordem = ordem + 10", "DELETE FROM checklist_os_item",
                "UPDATE checklist_os SET servico = 'Outro serviço'", "UPDATE checklist_os SET modelo_versao = 2",
                "DELETE FROM checklist_os")) {
            assertThrows(ErroDados.class, () -> executar(sql), sql);
        }
        assertEquals(SOM.itens().stream().map(ItemDoChecklist::copia).toList(),
                itens(os).stream().map(ItemChecklistSalvo::item).toList());
    }

    /** Teste nº 6 da Fase 3B, no banco: depois da entrega ou do cancelamento, o checklist da OS não muda. */
    @Test
    void depoisDaEntregaOuDoCancelamentoNaoMuda() {
        passos(os, StatusOs.AGUARDANDO_APROVACAO, StatusOs.APROVADA, StatusOs.EM_EXECUCAO);
        copiar(os);
        long item = itens(os).getFirst().id();
        marcar(item, new Marcacao(SituacaoItem.FEITO, "", ""), carlos);
        passos(os, StatusOs.PRONTA);
        base.banco.transacao(c -> {
            ordens.entregar(c, os, new Entrega(LocalDateTime.of(2026, 10, 20, 17, 0), 120_000.0, new Garantia(90)));
            return null;
        });

        ErroDados erro = assertThrows(ErroDados.class,
                () -> marcar(item, new Marcacao(SituacaoItem.PENDENTE, "", ""), carlos));
        assertTrue(erro.getMessage().contains("OS entregue ou cancelada: o checklist não muda."), erro.getMessage());
        assertEquals(SituacaoItem.FEITO, itens(os).getFirst().item().marcacao().situacao(), "dá para ler, como era");

        base.banco.transacao(c -> {
            ordens.cancelar(c, outraOs, "O cliente desistiu.");
            return null;
        });
        erro = assertThrows(ErroDados.class, () -> copiar(outraOs));
        assertTrue(erro.getMessage().contains("OS entregue ou cancelada não ganha checklist."), erro.getMessage());
    }

    /** A V8 sobre um banco que já está na V7 (com uma OS): só cria as tabelas, e as chaves valem. */
    @Test
    void v8SobreUmBancoNaV7() throws Exception {
        BaseDeTeste antiga = BaseDeTeste.semMigrar(pasta.resolve("v7"));
        Flyway.configure().dataSource(antiga.banco.fonte()).locations(Migracoes.LOCAL_PADRAO).target("7").load()
                .migrate();
        long osAntiga = abrirOs(antiga, "CDE3F45", 1);
        Migracoes.Resultado resultado = Migracoes.migrar(antiga.banco, antiga.backup);
        assertEquals("7", resultado.versaoAntes());
        assertEquals(BaseDeTeste.VERSAO_BANCO, resultado.versaoDepois());
        ChecklistRepositorio repositorio = new ChecklistRepositorio(antiga.relogio);
        assertEquals(List.of(), antiga.banco.ler(c -> repositorio.daOs(c, osAntiga)),
                "a OS que já existia fica sem checklist");
        assertThrows(ErroDados.class, () -> antiga.banco.transacao(c -> repositorio.copiar(c, 999, 1, "Serviço", 1, 1,
                SOM)), "OS que não existe");
    }
}
