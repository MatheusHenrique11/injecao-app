package br.com.calibra.dados.migracoes;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import br.com.calibra.dados.Sql;

/**
 * V6 (fase 3, bloco 3.1): ordens de serviço (SPEC 6.5), mecânicos e recebimentos, e o orçamento passando a pertencer
 * à OS.
 *
 * <p>A tabela {@code orcamento} da V5 exige projeto, cálculo e versão do motor; a OS comum não tem nada disso. Como o
 * SQLite não muda coluna, a tabela é refeita pelo procedimento da documentação dele: tabela nova, cópia, troca de
 * nome e índices e gatilhos de novo. Isso exige as chaves estrangeiras desligadas, o que só vale fora de transação:
 * por isso a migração roda fora da transação do Flyway e abre a sua, conferindo as chaves antes de gravar.
 *
 * <p>Cada projeto que já tem orçamento ganha uma OS, com o status da última revisão. Os ids, os números e o
 * histórico dos orçamentos não mudam.
 */
public class V6__Ordens_de_servico extends BaseJavaMigration {

    /** Tabelas e gatilhos novos, na ordem. */
    static final List<String> TABELAS = List.of(
            """
            CREATE TABLE mecanico (
                id            INTEGER PRIMARY KEY,
                nome          TEXT    NOT NULL,
                ativo         INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1)),
                criado_em     TEXT    NOT NULL,
                atualizado_em TEXT    NOT NULL
            ) STRICT""",
            """
            CREATE TABLE ordem_servico (
                id                  INTEGER PRIMARY KEY,
                ano                 INTEGER NOT NULL,
                sequencial          INTEGER NOT NULL,
                cliente_id          INTEGER NOT NULL REFERENCES cliente (id),
                veiculo_id          INTEGER NOT NULL REFERENCES veiculo (id),
                projeto_id          INTEGER REFERENCES projeto (id),
                status              TEXT    NOT NULL,
                servico_pedido      TEXT    NOT NULL,
                mecanico_id         INTEGER REFERENCES mecanico (id),
                previsao_entrega    TEXT,
                entrada_em          TEXT,
                uso_entrada         REAL,
                combustivel_entrada TEXT,
                vistoria_json       TEXT,
                pronta_em           TEXT,
                entregue_em         TEXT,
                uso_saida           REAL,
                garantia_dias       INTEGER,
                garantia_ate        TEXT,
                motivo_cancelamento TEXT    NOT NULL DEFAULT '',
                observacoes         TEXT    NOT NULL DEFAULT '',
                criado_em           TEXT    NOT NULL,
                atualizado_em       TEXT    NOT NULL,
                UNIQUE (ano, sequencial)
            ) STRICT""",
            "CREATE INDEX ordem_servico_por_status ON ordem_servico (status, id)",
            "CREATE INDEX ordem_servico_por_veiculo ON ordem_servico (veiculo_id, id)",
            "CREATE INDEX ordem_servico_por_projeto ON ordem_servico (projeto_id, id)",
            """
            CREATE TRIGGER ordem_servico_finalizada_nao_muda BEFORE UPDATE ON ordem_servico
                WHEN OLD.status IN ('ENTREGUE', 'CANCELADA')
            BEGIN
                SELECT RAISE(ABORT, 'OS entregue ou cancelada não muda.');
            END""",
            """
            CREATE TRIGGER ordem_servico_nao_apaga BEFORE DELETE ON ordem_servico
            BEGIN
                SELECT RAISE(ABORT, 'OS não é apagada: cancele, com o motivo.');
            END""",
            """
            CREATE TABLE os_status (
                id         INTEGER PRIMARY KEY,
                os_id      INTEGER NOT NULL REFERENCES ordem_servico (id),
                status     TEXT    NOT NULL,
                data_hora  TEXT    NOT NULL,
                observacao TEXT    NOT NULL DEFAULT ''
            ) STRICT""",
            "CREATE INDEX os_status_por_os ON os_status (os_id, id)",
            """
            CREATE TABLE os_pagamento (
                id                  INTEGER PRIMARY KEY,
                os_id               INTEGER NOT NULL REFERENCES ordem_servico (id),
                recebido_em         TEXT    NOT NULL,
                forma               TEXT    NOT NULL,
                valor_centavos      INTEGER NOT NULL CHECK (valor_centavos > 0),
                observacao          TEXT    NOT NULL DEFAULT '',
                cancelado_em        TEXT,
                motivo_cancelamento TEXT    NOT NULL DEFAULT '',
                criado_em           TEXT    NOT NULL
            ) STRICT""",
            "CREATE INDEX os_pagamento_por_os ON os_pagamento (os_id, id)",
            """
            CREATE TRIGGER os_pagamento_so_cancela BEFORE UPDATE ON os_pagamento
                WHEN OLD.cancelado_em IS NOT NULL OR NEW.cancelado_em IS NULL
                    OR NEW.os_id IS NOT OLD.os_id OR NEW.recebido_em IS NOT OLD.recebido_em
                    OR NEW.forma IS NOT OLD.forma OR NEW.valor_centavos IS NOT OLD.valor_centavos
                    OR NEW.observacao IS NOT OLD.observacao OR NEW.criado_em IS NOT OLD.criado_em
            BEGIN
                SELECT RAISE(ABORT, 'Recebimento não muda: só pode ser cancelado, uma vez, com o motivo.');
            END""",
            """
            CREATE TRIGGER os_pagamento_nao_apaga BEFORE DELETE ON os_pagamento
            BEGIN
                SELECT RAISE(ABORT, 'Recebimento não é apagado: cancele, com o motivo.');
            END""");

    /** Os projetos com orçamento, na ordem do primeiro orçamento de cada um. */
    static final String PROJETOS_COM_ORCAMENTO = """
            SELECT o.projeto_id, MIN(o.id) AS primeiro, p.veiculo_id, v.cliente_id
            FROM orcamento o
            JOIN projeto p ON p.id = o.projeto_id
            JOIN veiculo v ON v.id = p.veiculo_id
            GROUP BY o.projeto_id
            ORDER BY primeiro""";

    static final String INSERIR_OS = """
            INSERT INTO ordem_servico (ano, sequencial, cliente_id, veiculo_id, projeto_id, status, servico_pedido,
                observacoes, criado_em, atualizado_em)
            VALUES (?, ?, ?, ?, ?, ?, 'Preparação', ?, ?, ?)""";

    static final String INSERIR_STATUS = """
            INSERT INTO os_status (os_id, status, data_hora, observacao)
            VALUES (?, ?, ?, 'Criada na atualização para a 0.3.0.')""";

    /** A tabela {@code orcamento} refeita: a OS manda; projeto, cálculo e versão do motor juntos ou nenhum. */
    static final List<String> REFAZER_ORCAMENTO = List.of(
            """
            CREATE TABLE orcamento_v6 (
                id                  INTEGER PRIMARY KEY,
                os_id               INTEGER NOT NULL REFERENCES ordem_servico (id),
                projeto_id          INTEGER REFERENCES projeto (id),
                calculo_id          INTEGER REFERENCES calculo (id),
                motor_versao_id     INTEGER REFERENCES motor_versao (id),
                ano                 INTEGER NOT NULL,
                sequencial          INTEGER NOT NULL,
                revisao             INTEGER NOT NULL,
                status              TEXT    NOT NULL,
                conteudo_json       TEXT    NOT NULL,
                total_centavos      INTEGER NOT NULL,
                enviado_em          TEXT,
                respondido_em       TEXT,
                meio_resposta       TEXT,
                observacao_resposta TEXT    NOT NULL DEFAULT '',
                criado_em           TEXT    NOT NULL,
                atualizado_em       TEXT    NOT NULL,
                UNIQUE (ano, sequencial, revisao),
                CHECK ((projeto_id IS NULL AND calculo_id IS NULL AND motor_versao_id IS NULL)
                    OR (projeto_id IS NOT NULL AND calculo_id IS NOT NULL AND motor_versao_id IS NOT NULL))
            ) STRICT""",
            """
            INSERT INTO orcamento_v6 (id, os_id, projeto_id, calculo_id, motor_versao_id, ano, sequencial, revisao,
                status, conteudo_json, total_centavos, enviado_em, respondido_em, meio_resposta, observacao_resposta,
                criado_em, atualizado_em)
            SELECT o.id, (SELECT os.id FROM ordem_servico os WHERE os.projeto_id = o.projeto_id), o.projeto_id,
                o.calculo_id, o.motor_versao_id, o.ano, o.sequencial, o.revisao, o.status, o.conteudo_json,
                o.total_centavos, o.enviado_em, o.respondido_em, o.meio_resposta, o.observacao_resposta, o.criado_em,
                o.atualizado_em
            FROM orcamento o""",
            "DROP TABLE orcamento",
            "ALTER TABLE orcamento_v6 RENAME TO orcamento",
            "CREATE INDEX orcamento_por_os ON orcamento (os_id, id)",
            "CREATE INDEX orcamento_por_projeto ON orcamento (projeto_id, id)",
            """
            CREATE TRIGGER orcamento_so_rascunho_muda BEFORE UPDATE OF conteudo_json, total_centavos, os_id,
                projeto_id, calculo_id, motor_versao_id ON orcamento WHEN OLD.status <> 'RASCUNHO'
            BEGIN
                SELECT RAISE(ABORT, 'Orçamento enviado não muda: abra uma revisão nova.');
            END""",
            """
            CREATE TRIGGER orcamento_nao_apaga BEFORE DELETE ON orcamento
            BEGIN
                SELECT RAISE(ABORT, 'Orçamento não é apagado.');
            END""");

    /** O status da OS criada na atualização, pelo status da última revisão do orçamento. */
    static String statusDaOs(String statusDoOrcamento) {
        return switch (statusDoOrcamento) {
            case "ENVIADO" -> "AGUARDANDO_APROVACAO";
            case "APROVADO" -> "APROVADA";
            default -> "ABERTA";
        };
    }

    /** Impressão digital do SQL desta migração: se ele for editado depois de aplicado, o Flyway acusa. */
    @Override
    public Integer getChecksum() {
        List<String> comandos = new ArrayList<>(TABELAS);
        comandos.addAll(List.of(PROJETOS_COM_ORCAMENTO, INSERIR_OS, INSERIR_STATUS));
        comandos.addAll(REFAZER_ORCAMENTO);
        String sql = String.join("\n", comandos);
        CRC32 crc = new CRC32();
        for (byte umByte : sql.getBytes(StandardCharsets.UTF_8)) {
            if (umByte != '\r') {
                crc.update(umByte);
            }
        }
        return (int) crc.getValue();
    }

    /** As chaves estrangeiras só desligam fora de transação: a migração abre a própria. */
    @Override
    public boolean canExecuteInTransaction() {
        return false;
    }

    @Override
    public void migrate(Context contexto) throws Exception {
        Connection conexao = contexto.getConnection();
        if (!conexao.getAutoCommit()) {
            conexao.commit();
            conexao.setAutoCommit(true);
        }
        executar(conexao, "PRAGMA foreign_keys = OFF");
        try {
            if (pragma(conexao, "PRAGMA foreign_keys") != 0) {
                throw new IllegalStateException("Não foi possível desligar as chaves estrangeiras para a migração V6.");
            }
            conexao.setAutoCommit(false);
            try {
                for (String comando : TABELAS) {
                    executar(conexao, comando);
                }
                criarOsDosProjetos(conexao, Clock.systemDefaultZone());
                for (String comando : REFAZER_ORCAMENTO) {
                    executar(conexao, comando);
                }
                conferirChaves(conexao);
                conexao.commit();
            } catch (SQLException | RuntimeException erro) {
                conexao.rollback();
                throw erro;
            } finally {
                conexao.setAutoCommit(true);
            }
        } finally {
            executar(conexao, "PRAGMA foreign_keys = ON");
        }
    }

    private record ProjetoComOrcamento(long projetoId, long primeiroOrcamento, long veiculoId, long clienteId) {
    }

    /** A primeira revisão do orçamento do projeto: a OS herda o ano e a data de criação dela. */
    private record PrimeiroOrcamento(int ano, String numero, String criadoEm) {
    }

    private static void criarOsDosProjetos(Connection conexao, Clock relogio) throws SQLException {
        List<ProjetoComOrcamento> projetos = new ArrayList<>();
        try (Statement consulta = conexao.createStatement();
                ResultSet linha = consulta.executeQuery(PROJETOS_COM_ORCAMENTO)) {
            while (linha.next()) {
                projetos.add(new ProjetoComOrcamento(linha.getLong("projeto_id"), linha.getLong("primeiro"),
                        linha.getLong("veiculo_id"), linha.getLong("cliente_id")));
            }
        }
        String agora = Sql.texto(Sql.agora(relogio));
        Map<Integer, Integer> sequencias = new HashMap<>();
        for (ProjetoComOrcamento projeto : projetos) {
            PrimeiroOrcamento primeiro = primeiroOrcamento(conexao, projeto.primeiroOrcamento());
            int ano = primeiro.ano();
            int sequencial = sequencias.merge(ano, 1, Integer::sum);
            String status = statusDaOs(ultimoStatus(conexao, projeto.projetoId()));
            long os;
            try (PreparedStatement insercao = conexao.prepareStatement(INSERIR_OS)) {
                insercao.setInt(1, ano);
                insercao.setInt(2, sequencial);
                insercao.setLong(3, projeto.clienteId());
                insercao.setLong(4, projeto.veiculoId());
                insercao.setLong(5, projeto.projetoId());
                insercao.setString(6, status);
                insercao.setString(7, "Criada na atualização para a 0.3.0, a partir do orçamento " + primeiro.numero()
                        + ".");
                insercao.setString(8, primeiro.criadoEm());
                insercao.setString(9, agora);
                insercao.executeUpdate();
                os = Sql.ultimoId(conexao);
            }
            try (PreparedStatement insercao = conexao.prepareStatement(INSERIR_STATUS)) {
                insercao.setLong(1, os);
                insercao.setString(2, status);
                insercao.setString(3, agora);
                insercao.executeUpdate();
            }
        }
    }

    private static PrimeiroOrcamento primeiroOrcamento(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT ano, sequencial, criado_em FROM orcamento WHERE id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                linha.next();
                int ano = linha.getInt("ano");
                return new PrimeiroOrcamento(ano, "ORC-%d-%04d".formatted(ano, linha.getInt("sequencial")),
                        linha.getString("criado_em"));
            }
        }
    }

    private static String ultimoStatus(Connection conexao, long projetoId) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT status FROM orcamento WHERE projeto_id = ? ORDER BY id DESC LIMIT 1")) {
            consulta.setLong(1, projetoId);
            try (ResultSet linha = consulta.executeQuery()) {
                linha.next();
                return linha.getString(1);
            }
        }
    }

    /** Nenhuma chave estrangeira quebrada depois da troca; senão, nada é gravado. */
    private static void conferirChaves(Connection conexao) throws SQLException {
        try (Statement consulta = conexao.createStatement();
                ResultSet linha = consulta.executeQuery("PRAGMA foreign_key_check")) {
            if (linha.next()) {
                throw new SQLException("A migração V6 quebraria uma chave estrangeira na tabela "
                        + linha.getString("table") + " (linha " + linha.getLong("rowid") + "); nada foi gravado.");
            }
        }
    }

    private static int pragma(Connection conexao, String pragma) throws SQLException {
        try (Statement consulta = conexao.createStatement(); ResultSet linha = consulta.executeQuery(pragma)) {
            return linha.next() ? linha.getInt(1) : -1;
        }
    }

    private static void executar(Connection conexao, String comando) throws SQLException {
        try (Statement execucao = conexao.createStatement()) {
            execucao.execute(comando);
        }
    }
}
