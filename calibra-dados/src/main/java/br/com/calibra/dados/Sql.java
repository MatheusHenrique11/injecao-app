package br.com.calibra.dados;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** Ajudas dos repositórios: datas em texto ISO, campos que podem ficar vazios e o id do último INSERT. */
public final class Sql {

    /** Sempre com os segundos, para a ordem do texto ser a ordem do tempo. */
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Sql() {
    }

    /** Agora, sem os milissegundos. */
    public static LocalDateTime agora(Clock relogio) {
        return LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS);
    }

    public static String texto(LocalDateTime dataHora) {
        return dataHora == null ? null : DATA_HORA.format(dataHora);
    }

    public static String texto(LocalDate data) {
        return data == null ? null : data.toString();
    }

    public static LocalDateTime dataHora(ResultSet linha, String coluna) throws SQLException {
        String texto = linha.getString(coluna);
        return texto == null ? null : LocalDateTime.parse(texto);
    }

    public static LocalDate data(ResultSet linha, String coluna) throws SQLException {
        String texto = linha.getString(coluna);
        return texto == null ? null : LocalDate.parse(texto);
    }

    public static Integer inteiro(ResultSet linha, String coluna) throws SQLException {
        int valor = linha.getInt(coluna);
        return linha.wasNull() ? null : valor;
    }

    public static Double numero(ResultSet linha, String coluna) throws SQLException {
        double valor = linha.getDouble(coluna);
        return linha.wasNull() ? null : valor;
    }

    public static void inteiro(PreparedStatement comando, int posicao, Integer valor) throws SQLException {
        if (valor == null) {
            comando.setNull(posicao, Types.INTEGER);
        } else {
            comando.setInt(posicao, valor);
        }
    }

    /** Número que pode faltar; NaN (o "não se aplica" do núcleo) também vira vazio. */
    public static void numero(PreparedStatement comando, int posicao, Double valor) throws SQLException {
        if (valor == null || !Double.isFinite(valor)) {
            comando.setNull(posicao, Types.REAL);
        } else {
            comando.setDouble(posicao, valor);
        }
    }

    /** Nome da constante do enum, ou vazio. */
    public static void nome(PreparedStatement comando, int posicao, Enum<?> valor) throws SQLException {
        comando.setString(posicao, valor == null ? null : valor.name());
    }

    /** Lê o nome de uma constante do enum gravada no banco; vazio vira null. */
    public static <E extends Enum<E>> E constante(ResultSet linha, String coluna, Class<E> tipo) throws SQLException {
        String texto = linha.getString(coluna);
        return texto == null ? null : Enum.valueOf(tipo, texto);
    }

    /** Id do último INSERT feito nesta conexão. */
    public static long ultimoId(Connection conexao) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT last_insert_rowid()");
                ResultSet linha = consulta.executeQuery()) {
            linha.next();
            return linha.getLong(1);
        }
    }
}
