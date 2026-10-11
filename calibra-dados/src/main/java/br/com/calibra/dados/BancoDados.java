package br.com.calibra.dados;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.DataSource;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

/**
 * O banco SQLite do Calibra (SPEC 5): um arquivo com journal_mode=WAL e foreign_keys=ON.
 *
 * <p>Cada operação abre uma conexão e fecha no fim. Com WAL, leitura e gravação não se bloqueiam, e
 * quando a última conexão fecha o SQLite passa tudo para o arquivo principal. Toda gravação deve usar
 * {@link #transacao}: assim ela é tudo ou nada, e o app sabe que houve mudança (para o backup ao fechar).
 */
public final class BancoDados {

    /** Uma operação que usa a conexão. */
    @FunctionalInterface
    public interface OperacaoSql<T> {
        T executar(Connection conexao) throws SQLException;
    }

    private final Path arquivo;
    private final SQLiteDataSource fonte;
    private final AtomicLong gravacoes = new AtomicLong();

    public BancoDados(Path arquivo) {
        this.arquivo = Objects.requireNonNull(arquivo, "arquivo").toAbsolutePath();
        SQLiteConfig config = new SQLiteConfig();
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.enforceForeignKeys(true);
        config.setBusyTimeout(5_000);
        this.fonte = new SQLiteDataSource(config);
        this.fonte.setUrl("jdbc:sqlite:" + this.arquivo);
    }

    public Path arquivo() {
        return arquivo;
    }

    /** Para o Flyway. */
    public DataSource fonte() {
        return fonte;
    }

    /** Conexão nova; quem pede fecha (try-with-resources). */
    public Connection conexao() throws SQLException {
        return fonte.getConnection();
    }

    /** Leitura: abre, executa e fecha. */
    public <T> T ler(OperacaoSql<T> operacao) {
        try (Connection conexao = conexao()) {
            return operacao.executar(conexao);
        } catch (SQLException erro) {
            throw new ErroDados("Erro ao ler o banco de dados: " + erro.getMessage(), erro);
        }
    }

    /** Gravação tudo ou nada: se algo falhar, nada fica gravado. */
    public <T> T transacao(OperacaoSql<T> operacao) {
        try (Connection conexao = conexao()) {
            conexao.setAutoCommit(false);
            try {
                T resultado = operacao.executar(conexao);
                conexao.commit();
                gravacoes.incrementAndGet();
                return resultado;
            } catch (SQLException | RuntimeException erro) {
                conexao.rollback();
                throw erro;
            }
        } catch (SQLException erro) {
            throw new ErroDados("Erro ao gravar no banco de dados: " + erro.getMessage(), erro);
        }
    }

    /** Quantas gravações deram certo desde que o app abriu. */
    public long gravacoes() {
        return gravacoes.get();
    }

    /** Versão do banco (a última migração aplicada), ou null se o banco ainda não foi migrado. */
    public String versaoDoEsquema() {
        return ler(BancoDados::versaoDoEsquema);
    }

    /** Versão do banco pela tabela de histórico do Flyway, ou null se ela não existir. */
    public static String versaoDoEsquema(Connection conexao) throws SQLException {
        try (PreparedStatement existe = conexao.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'flyway_schema_history'");
                ResultSet tabela = existe.executeQuery()) {
            if (!tabela.next()) {
                return null;
            }
        }
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT version FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL "
                        + "ORDER BY installed_rank DESC LIMIT 1");
                ResultSet linha = consulta.executeQuery()) {
            return linha.next() ? linha.getString(1) : null;
        }
    }
}
