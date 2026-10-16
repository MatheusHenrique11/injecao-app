package br.com.calibra.dados.oficina;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.dados.Sql;

/**
 * Mecânicos da oficina (só o nome; sem login, SPEC 1). Não se apagam: quem sai fica inativo, e as OS antigas
 * continuam com ele.
 */
public final class MecanicoRepositorio {

    private final Clock relogio;

    public MecanicoRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    public long inserir(Connection conexao, String nome) throws SQLException {
        String agora = Sql.texto(Sql.agora(relogio));
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO mecanico (nome, ativo, criado_em, atualizado_em) VALUES (?, 1, ?, ?)")) {
            insercao.setString(1, nome.strip());
            insercao.setString(2, agora);
            insercao.setString(3, agora);
            insercao.executeUpdate();
        }
        return Sql.ultimoId(conexao);
    }

    public void renomear(Connection conexao, long id, String nome) throws SQLException {
        atualizar(conexao, "UPDATE mecanico SET nome = ?, atualizado_em = ? WHERE id = ?", nome.strip(), id);
    }

    public void ativar(Connection conexao, long id, boolean ativo) throws SQLException {
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE mecanico SET ativo = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setInt(1, ativo ? 1 : 0);
            atualizacao.setString(2, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(3, id);
            atualizacao.executeUpdate();
        }
    }

    public Optional<MecanicoSalvo> buscar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT id, nome, ativo FROM mecanico WHERE id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha)) : Optional.empty();
            }
        }
    }

    /** Em ordem de nome; os inativos só se pedidos. */
    public List<MecanicoSalvo> listar(Connection conexao, boolean incluirInativos) throws SQLException {
        List<MecanicoSalvo> lista = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT id, nome, ativo FROM mecanico"
                + (incluirInativos ? "" : " WHERE ativo = 1") + " ORDER BY nome COLLATE NOCASE, id")) {
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    lista.add(ler(linha));
                }
            }
        }
        return lista;
    }

    private void atualizar(Connection conexao, String sql, String texto, long id) throws SQLException {
        try (PreparedStatement atualizacao = conexao.prepareStatement(sql)) {
            atualizacao.setString(1, texto);
            atualizacao.setString(2, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(3, id);
            atualizacao.executeUpdate();
        }
    }

    private static MecanicoSalvo ler(ResultSet linha) throws SQLException {
        return new MecanicoSalvo(linha.getLong("id"), linha.getString("nome"), linha.getInt("ativo") == 1);
    }
}
