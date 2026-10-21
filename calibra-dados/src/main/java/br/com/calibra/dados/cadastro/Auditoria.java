package br.com.calibra.dados.cadastro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import br.com.calibra.dados.Sql;

/**
 * Log de auditoria dos cadastros (tabela {@code auditoria}, SPEC 6.2): todo salvamento grava o campo, o valor
 * antigo, o novo, a data e a hora. A OS usa o mesmo log para as correções da abertura e da entrada (bloco 3B.7): por
 * isso a classe e os dois métodos que ela usa são públicos.
 */
public final class Auditoria {

    /**
     * O que fica no lugar de um valor pessoal apagado na anonimização. É o mesmo texto que o gatilho da V14 aceita no
     * "quem retirou" da OS entregue.
     */
    public static final String APAGADO = "(apagado na anonimização)";

    private Auditoria() {
    }

    static void registrar(Connection conexao, String entidade, long id, String campo, String antigo, String novo,
            LocalDateTime quando) throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO auditoria (entidade, entidade_id, campo, valor_antigo, valor_novo, data_hora) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            insercao.setString(1, entidade);
            insercao.setLong(2, id);
            insercao.setString(3, campo);
            insercao.setString(4, antigo);
            insercao.setString(5, novo);
            insercao.setString(6, Sql.texto(quando));
            insercao.executeUpdate();
        }
    }

    /** Grava uma linha para cada campo que mudou; os mapas têm o rótulo do campo e o valor como texto. */
    public static void registrarMudancas(Connection conexao, String entidade, long id, Map<String, String> antes,
            Map<String, String> depois, LocalDateTime quando) throws SQLException {
        for (Map.Entry<String, String> campo : depois.entrySet()) {
            String antigo = antes.get(campo.getKey());
            if (!Objects.equals(antigo, campo.getValue())) {
                registrar(conexao, entidade, id, campo.getKey(), antigo, campo.getValue(), quando);
            }
        }
    }

    /** Histórico do cadastro, do salvamento mais novo para o mais antigo; dentro dele, na ordem da tela. */
    public static List<EventoAuditoria> historico(Connection conexao, String entidade, long id) throws SQLException {
        List<EventoAuditoria> eventos = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT campo, valor_antigo, valor_novo, data_hora FROM auditoria "
                        + "WHERE entidade = ? AND entidade_id = ? ORDER BY data_hora DESC, id")) {
            consulta.setString(1, entidade);
            consulta.setLong(2, id);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    eventos.add(new EventoAuditoria(linha.getString("campo"), linha.getString("valor_antigo"),
                            linha.getString("valor_novo"), Sql.dataHora(linha, "data_hora")));
                }
            }
        }
        return eventos;
    }

    /** Anonimização: troca os valores antigos e novos destes campos pelo aviso de apagado. */
    static void apagarValores(Connection conexao, String entidade, long id, Collection<String> campos)
            throws SQLException {
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE auditoria SET "
                        + "valor_antigo = CASE WHEN valor_antigo IS NULL OR valor_antigo = '' THEN valor_antigo ELSE ? END, "
                        + "valor_novo = CASE WHEN valor_novo IS NULL OR valor_novo = '' THEN valor_novo ELSE ? END "
                        + "WHERE entidade = ? AND entidade_id = ? AND campo = ?")) {
            for (String campo : campos) {
                atualizacao.setString(1, APAGADO);
                atualizacao.setString(2, APAGADO);
                atualizacao.setString(3, entidade);
                atualizacao.setLong(4, id);
                atualizacao.setString(5, campo);
                atualizacao.executeUpdate();
            }
        }
    }

    /** Apaga o histórico inteiro de um cadastro que foi apagado. */
    static void apagarHistorico(Connection conexao, String entidade, long id) throws SQLException {
        try (PreparedStatement remocao = conexao.prepareStatement(
                "DELETE FROM auditoria WHERE entidade = ? AND entidade_id = ?")) {
            remocao.setString(1, entidade);
            remocao.setLong(2, id);
            remocao.executeUpdate();
        }
    }
}
