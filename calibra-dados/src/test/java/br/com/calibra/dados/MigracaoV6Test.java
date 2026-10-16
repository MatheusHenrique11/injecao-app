package br.com.calibra.dados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.backup.InfoBackup;
import br.com.calibra.dados.backup.MotivoBackup;
import br.com.calibra.dados.json.Json;
import br.com.calibra.dados.projeto.CalculoRepositorio;
import br.com.calibra.dados.projeto.VersaoMotor;

/**
 * Migração V6 (fase 3, bloco 3.1) num banco como a 0.2.0 deixava: cada projeto com orçamento ganha uma OS com o status
 * da última revisão; os orçamentos e o histórico deles não mudam; gatilhos, chaves e CHECK continuam valendo. Grava o
 * antes e o depois em target/evidencia-migracao-v6.md.
 */
class MigracaoV6Test {

    private static final String COLUNAS_V5 = "id, projeto_id, calculo_id, motor_versao_id, ano, sequencial, revisao, "
            + "status, conteudo_json, total_centavos, enviado_em, respondido_em, meio_resposta, observacao_resposta, "
            + "criado_em, atualizado_em";

    @TempDir
    Path pasta;

    /** Um projeto do banco de exemplo: o id e os da versão do motor e do cálculo. */
    private record Projeto(long id, long motorVersao, long calculo) {
    }

    @Test
    void cadaProjetoComOrcamentoGanhaUmaOsENadaSePerde() throws Exception {
        BaseDeTeste base = bancoDa020(pasta.resolve("dados"));
        List<List<String>> orcamentosAntes = linhas(base, "SELECT " + COLUNAS_V5 + " FROM orcamento ORDER BY id");
        List<List<String>> historicoAntes = linhas(base, "SELECT * FROM orcamento_status ORDER BY id");

        Migracoes.Resultado resultado = Migracoes.migrar(base.banco, base.backup);

        assertEquals("5", resultado.versaoAntes());
        assertEquals("6", resultado.versaoDepois());
        assertTrue(resultado.backupAntes() != null, "backup antes de migrar");
        assertEquals(orcamentosAntes, linhas(base, "SELECT " + COLUNAS_V5 + " FROM orcamento ORDER BY id"),
                "ids, números, conteúdo, status e datas dos orçamentos iguais");
        assertEquals(historicoAntes, linhas(base, "SELECT * FROM orcamento_status ORDER BY id"));

        List<List<String>> os = linhas(base, "SELECT ano, sequencial, projeto_id, status, servico_pedido, entrada_em, "
                + "observacoes, criado_em FROM ordem_servico ORDER BY id");
        assertEquals(List.of(
                List.of("2026", "1", "1", "APROVADA", "Preparação", "null",
                        "Criada na atualização para a 0.3.0, a partir do orçamento ORC-2026-0001.", "2026-10-11T09:00:00"),
                List.of("2026", "2", "2", "AGUARDANDO_APROVACAO", "Preparação", "null",
                        "Criada na atualização para a 0.3.0, a partir do orçamento ORC-2026-0002.", "2026-10-12T09:00:00"),
                List.of("2026", "3", "3", "ABERTA", "Preparação", "null",
                        "Criada na atualização para a 0.3.0, a partir do orçamento ORC-2026-0003.", "2026-10-13T09:00:00"),
                List.of("2027", "1", "4", "ABERTA", "Preparação", "null",
                        "Criada na atualização para a 0.3.0, a partir do orçamento ORC-2027-0001.", "2027-01-05T09:00:00")),
                os, "aprovado, enviado, recusado e rascunho; o projeto sem orçamento fica sem OS; número por ano");
        assertEquals(List.of(), linhas(base, "SELECT o.id FROM orcamento o JOIN ordem_servico s ON s.id = o.os_id "
                + "WHERE s.projeto_id IS NOT o.projeto_id"), "cada orçamento na OS do seu projeto");
        assertEquals(List.of(List.of("4")), linhas(base, "SELECT COUNT(*) FROM os_status"), "um status por OS");

        conferirRegras(base);
        gravarEvidencia(orcamentosAntes, os);
    }

    /** Restaurar um backup da 0.2.0 (V5) no app novo migra os dados para a V6, com as OS. */
    @Test
    void restaurarBackupDaV5MigraParaAV6() throws Exception {
        BaseDeTeste antigo = bancoDa020(pasta.resolve("antigo"));
        InfoBackup backupV5 = antigo.backup.fazer(pasta.resolve("backups"), MotivoBackup.MANUAL);
        assertEquals("5", backupV5.manifesto().versaoBanco());

        BaseDeTeste novo = BaseDeTeste.criada(pasta.resolve("novo"));
        assertEquals("6", novo.banco.versaoDoEsquema());
        novo.restauracao().restaurar(backupV5.arquivo());

        assertEquals("6", novo.banco.versaoDoEsquema());
        assertEquals(List.of(List.of("4")), linhas(novo, "SELECT COUNT(*) FROM ordem_servico"));
        assertEquals(List.of(List.of("5")), linhas(novo, "SELECT COUNT(*) FROM orcamento WHERE os_id IS NOT NULL"));
    }

    /** Gatilhos, chaves estrangeiras e CHECK depois da troca da tabela. */
    private static void conferirRegras(BaseDeTeste base) throws Exception {
        assertEquals(List.of(), linhas(base, "PRAGMA foreign_key_check"));
        assertEquals(List.of(List.of("ok")), linhas(base, "PRAGMA integrity_check"));
        assertThrows(ErroDados.class, () -> executar(base, "UPDATE orcamento SET conteudo_json = '{}' "
                + "WHERE status = 'APROVADO'"), "enviado ou aprovado não muda");
        assertThrows(ErroDados.class, () -> executar(base, "UPDATE orcamento SET os_id = 2 WHERE status = 'APROVADO'"),
                "nem muda de OS");
        assertThrows(ErroDados.class, () -> executar(base, "DELETE FROM orcamento"), "orçamento não se apaga");
        assertThrows(ErroDados.class, () -> executar(base, "UPDATE orcamento SET os_id = 999 WHERE status = 'RASCUNHO'"),
                "a chave estrangeira volta a valer");
        assertThrows(ErroDados.class, () -> executar(base, "UPDATE orcamento SET calculo_id = NULL "
                + "WHERE status = 'RASCUNHO'"), "projeto, cálculo e versão do motor juntos ou nenhum");
        assertThrows(ErroDados.class, () -> executar(base, "DELETE FROM ordem_servico"), "OS não se apaga");
    }

    /** O banco como a 0.2.0 deixava: V5, com 5 projetos do caso B e orçamentos gravados direto na tabela da V5. */
    private static BaseDeTeste bancoDa020(Path raiz) throws Exception {
        BaseDeTeste base = BaseDeTeste.semMigrar(raiz);
        Flyway.configure().dataSource(base.banco.fonte()).locations(Migracoes.LOCAL_PADRAO).target("5").load()
                .migrate();
        assertEquals("5", base.banco.versaoDoEsquema());
        Cadastros.Repositorios repositorios = Cadastros.Repositorios.de(base);
        CalculoRepositorio calculos = new CalculoRepositorio(base.relogio);
        base.banco.transacao(c -> {
            List<Projeto> projetos = new ArrayList<>();
            String[][] clientes = {{"João da Silva", "ABC1D23"}, {"Maria Souza", "BCD2E34"}, {"Pedro Lima", "CDE3F45"},
                    {"Ana Costa", "DEF4G56"}, {"Rui Alves", "EFG5H67"}};
            for (String[] cliente : clientes) {
                long projeto = repositorios.projetoCasoB(c, cliente[0], cliente[1]);
                VersaoMotor v1 = repositorios.motores().atual(c, projeto).orElseThrow();
                EntradaDimensionamento entrada = CasoBDados.entrada();
                long calculo = calculos.salvar(c, projeto, v1.id(), entrada, CalculoDimensionamento.calcular(entrada),
                        List.of(new Justificativa(Indicador.DC_BICO, "Teste.", LocalDateTime.of(2026, 10, 11, 8, 0))),
                        List.of(), BaseDeTeste.VERSAO_APP);
                projetos.add(new Projeto(projeto, v1.id(), calculo));
            }
            orcamentoV5(c, projetos.get(0), 2026, 1, 1, "SUBSTITUIDO", "2026-10-11T09:00:00");
            orcamentoV5(c, projetos.get(0), 2026, 1, 2, "APROVADO", "2026-10-11T15:00:00");
            orcamentoV5(c, projetos.get(1), 2026, 2, 1, "ENVIADO", "2026-10-12T09:00:00");
            orcamentoV5(c, projetos.get(2), 2026, 3, 1, "RECUSADO", "2026-10-13T09:00:00");
            orcamentoV5(c, projetos.get(3), 2027, 1, 1, "RASCUNHO", "2027-01-05T09:00:00");
            return null;
        });
        return base;
    }

    private static void orcamentoV5(Connection c, Projeto projeto, int ano, int sequencial, int revisao, String status,
            String criadoEm) throws SQLException {
        Orcamento conteudo = new Orcamento(List.of(ItemOrcamento.digitado(TipoItem.PECA, "Bico teste",
                BigDecimal.valueOf(4), "un", Dinheiro.ler("450,00"))), Desconto.NENHUM, 10, "a partir da aprovação", 15,
                "", "");
        boolean enviado = !status.equals("RASCUNHO");
        boolean respondido = status.equals("APROVADO") || status.equals("RECUSADO") || status.equals("SUBSTITUIDO");
        try (PreparedStatement insercao = c.prepareStatement("INSERT INTO orcamento (projeto_id, calculo_id, "
                + "motor_versao_id, ano, sequencial, revisao, status, conteudo_json, total_centavos, enviado_em, "
                + "respondido_em, meio_resposta, observacao_resposta, criado_em, atualizado_em) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insercao.setLong(1, projeto.id());
            insercao.setLong(2, projeto.calculo());
            insercao.setLong(3, projeto.motorVersao());
            insercao.setInt(4, ano);
            insercao.setInt(5, sequencial);
            insercao.setInt(6, revisao);
            insercao.setString(7, status);
            insercao.setString(8, Json.escrever(conteudo));
            insercao.setLong(9, conteudo.total().centavos());
            insercao.setString(10, enviado ? criadoEm : null);
            insercao.setString(11, respondido ? criadoEm : null);
            insercao.setString(12, respondido ? "WHATSAPP" : null);
            insercao.setString(13, respondido ? "Respondeu por mensagem." : "");
            insercao.setString(14, criadoEm);
            insercao.setString(15, criadoEm);
            insercao.executeUpdate();
        }
        long id = Sql.ultimoId(c);
        try (PreparedStatement insercao = c.prepareStatement(
                "INSERT INTO orcamento_status (orcamento_id, status, data_hora) VALUES (?, ?, ?)")) {
            insercao.setLong(1, id);
            insercao.setString(2, status);
            insercao.setString(3, criadoEm);
            insercao.executeUpdate();
        }
    }

    /** Todas as linhas de uma consulta, como texto ("null" para vazio). */
    private static List<List<String>> linhas(BaseDeTeste base, String sql) {
        return base.banco.ler(c -> {
            List<List<String>> linhas = new ArrayList<>();
            try (Statement consulta = c.createStatement(); ResultSet linha = consulta.executeQuery(sql)) {
                int colunas = linha.getMetaData().getColumnCount();
                while (linha.next()) {
                    List<String> valores = new ArrayList<>();
                    for (int i = 1; i <= colunas; i++) {
                        valores.add(String.valueOf(linha.getString(i)));
                    }
                    linhas.add(valores);
                }
            }
            return linhas;
        });
    }

    private static void executar(BaseDeTeste base, String sql) {
        base.banco.transacao(c -> {
            try (Statement comando = c.createStatement()) {
                return comando.executeUpdate(sql);
            }
        });
    }

    private static void gravarEvidencia(List<List<String>> orcamentos, List<List<String>> os) throws Exception {
        StringBuilder texto = new StringBuilder("# Migração V6 — evidência do teste\n\n")
                .append("Banco de exemplo na V5 (como a 0.2.0 deixava), migrado para a V6 pelo `MigracaoV6Test`.\n\n")
                .append("## Orçamentos (iguais antes e depois)\n\n| id | projeto | número | revisão | status | criado em |\n")
                .append("|---|---|---|---|---|---|\n");
        for (List<String> o : orcamentos) {
            texto.append("| %s | %s | ORC-%s-%04d | %s | %s | %s |\n".formatted(o.get(0), o.get(1), o.get(4),
                    Integer.parseInt(o.get(5)), o.get(6), o.get(7), o.get(14)));
        }
        texto.append("\n## OS criadas na migração\n\n| número | projeto | status | entrada | criada em |\n")
                .append("|---|---|---|---|---|\n");
        for (List<String> o : os) {
            texto.append("| OS-%s-%04d | %s | %s | %s | %s |\n".formatted(o.get(0), Integer.parseInt(o.get(1)),
                    o.get(2), o.get(3), "null".equals(o.get(5)) ? "não registrada" : o.get(5), o.get(7)));
        }
        Path destino = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target")
                .resolve("evidencia-migracao-v6.md");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, texto);
    }
}
