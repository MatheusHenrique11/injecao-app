package br.com.calibra.dados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BancoDadosTest {

    @TempDir
    Path pasta;

    @Test
    void ligaWalEChavesEstrangeiras() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        assertEquals("wal", pragma(base.banco, "journal_mode"));
        assertEquals("1", pragma(base.banco, "foreign_keys"));
    }

    @Test
    void bancoNovoNaoTemVersao() throws Exception {
        BaseDeTeste base = BaseDeTeste.semMigrar(pasta);
        assertNull(base.banco.versaoDoEsquema());
    }

    @Test
    void transacaoComErroNaoGravaNada() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        assertThrows(ErroDados.class, () -> base.banco.transacao(conexao -> {
            inserirAuditoria(conexao, "primeira");
            inserirAuditoria(conexao, null); // campo obrigatório vazio: o banco recusa
            return null;
        }));
        assertEquals(0, contarAuditoria(base.banco));
        assertEquals(0, base.banco.gravacoes());

        base.banco.transacao(conexao -> inserirAuditoria(conexao, "ok"));
        assertEquals(1, contarAuditoria(base.banco));
        assertEquals(1, base.banco.gravacoes(), "conta só as gravações que deram certo");
    }

    private static int inserirAuditoria(Connection conexao, String campo) throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement("INSERT INTO auditoria "
                + "(entidade, entidade_id, campo, valor_antigo, valor_novo, data_hora) VALUES ('TESTE', 1, ?, 'a', 'b', "
                + "'2026-10-11T18:30:00')")) {
            insercao.setString(1, campo);
            return insercao.executeUpdate();
        }
    }

    private static int contarAuditoria(BancoDados banco) {
        return banco.ler(conexao -> {
            try (Statement consulta = conexao.createStatement();
                    ResultSet linha = consulta.executeQuery("SELECT COUNT(*) FROM auditoria")) {
                linha.next();
                return linha.getInt(1);
            }
        });
    }

    private static String pragma(BancoDados banco, String nome) {
        return banco.ler(conexao -> {
            try (Statement consulta = conexao.createStatement();
                    ResultSet linha = consulta.executeQuery("PRAGMA " + nome)) {
                linha.next();
                return linha.getString(1);
            }
        });
    }
}
