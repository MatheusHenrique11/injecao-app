package br.com.calibra.dados.oficina;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.dados.Sql;

/**
 * Anexos da OS e do veículo (tabela {@code anexo}, V7). Só o registro: copiar e apagar o arquivo na pasta de anexos é
 * com o app.
 */
public final class AnexoRepositorio {

    private static final String COLUNAS = "id, veiculo_id, os_id, tipo, descricao, arquivo, nome_original, "
            + "tamanho_bytes, sha256, validade, pessoal, criado_em";

    private final Clock relogio;

    public AnexoRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /**
     * @param osId null = anexo do veículo, sem OS
     * @param arquivo o caminho relativo à pasta de anexos, onde a cópia já foi gravada
     */
    public long inserir(Connection conexao, long veiculoId, Long osId, TipoAnexo tipo, String descricao,
            String arquivo, String nomeOriginal, long tamanhoBytes, String sha256, LocalDate validade, boolean pessoal)
            throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement("INSERT INTO anexo (veiculo_id, os_id, tipo, "
                + "descricao, arquivo, nome_original, tamanho_bytes, sha256, validade, pessoal, criado_em) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insercao.setLong(1, veiculoId);
            Sql.longo(insercao, 2, osId);
            insercao.setString(3, tipo.name());
            insercao.setString(4, descricao == null ? "" : descricao.strip());
            insercao.setString(5, arquivo);
            insercao.setString(6, nomeOriginal);
            insercao.setLong(7, tamanhoBytes);
            insercao.setString(8, sha256);
            insercao.setString(9, Sql.texto(validade));
            insercao.setInt(10, pessoal ? 1 : 0);
            insercao.setString(11, Sql.texto(Sql.agora(relogio)));
            insercao.executeUpdate();
        }
        return Sql.ultimoId(conexao);
    }

    /** Muda o que se diz do anexo (o arquivo não muda). */
    public void atualizar(Connection conexao, long id, TipoAnexo tipo, String descricao, LocalDate validade,
            boolean pessoal) throws SQLException {
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE anexo SET tipo = ?, descricao = ?, validade = ?, pessoal = ? WHERE id = ?")) {
            atualizacao.setString(1, tipo.name());
            atualizacao.setString(2, descricao == null ? "" : descricao.strip());
            atualizacao.setString(3, Sql.texto(validade));
            atualizacao.setInt(4, pessoal ? 1 : 0);
            atualizacao.setLong(5, id);
            atualizacao.executeUpdate();
        }
    }

    public void apagar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement remocao = conexao.prepareStatement("DELETE FROM anexo WHERE id = ?")) {
            remocao.setLong(1, id);
            remocao.executeUpdate();
        }
    }

    public Optional<AnexoSalvo> buscar(Connection conexao, long id) throws SQLException {
        List<AnexoSalvo> lista = lista(conexao, "SELECT " + COLUNAS + " FROM anexo WHERE id = ?", id);
        return lista.stream().findFirst();
    }

    /** Os anexos da OS, do mais antigo para o mais novo. */
    public List<AnexoSalvo> daOs(Connection conexao, long osId) throws SQLException {
        return lista(conexao, "SELECT " + COLUNAS + " FROM anexo WHERE os_id = ? ORDER BY id", osId);
    }

    /** Todos os anexos do veículo (das OS dele e os sem OS), do mais antigo para o mais novo. */
    public List<AnexoSalvo> doVeiculo(Connection conexao, long veiculoId) throws SQLException {
        return lista(conexao, "SELECT " + COLUNAS + " FROM anexo WHERE veiculo_id = ? ORDER BY id", veiculoId);
    }

    /**
     * Os anexos marcados como documento pessoal do cliente (LGPD: saem na anonimização): os das OS dele e os dos
     * veículos dele que não são de OS.
     */
    public List<AnexoSalvo> pessoaisDoCliente(Connection conexao, long clienteId) throws SQLException {
        return lista(conexao, "SELECT " + COLUNAS + " FROM anexo WHERE pessoal = 1 AND (os_id IN (SELECT id FROM "
                + "ordem_servico WHERE cliente_id = ?) OR (os_id IS NULL AND veiculo_id IN (SELECT id FROM veiculo "
                + "WHERE cliente_id = ?))) ORDER BY id", clienteId, clienteId);
    }

    /** Quantos anexos apontam para o arquivo (o mesmo arquivo anexado duas vezes é uma cópia só). */
    public int quantosUsam(Connection conexao, String arquivo) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT COUNT(*) FROM anexo WHERE arquivo = ?")) {
            consulta.setString(1, arquivo);
            try (ResultSet linha = consulta.executeQuery()) {
                linha.next();
                return linha.getInt(1);
            }
        }
    }

    private static List<AnexoSalvo> lista(Connection conexao, String sql, long... parametros) throws SQLException {
        List<AnexoSalvo> lista = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                consulta.setLong(i + 1, parametros[i]);
            }
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    lista.add(new AnexoSalvo(linha.getLong("id"), linha.getLong("veiculo_id"),
                            Sql.longo(linha, "os_id"), Sql.constante(linha, "tipo", TipoAnexo.class),
                            linha.getString("descricao"), linha.getString("arquivo"),
                            linha.getString("nome_original"), linha.getLong("tamanho_bytes"),
                            linha.getString("sha256"), Sql.data(linha, "validade"), linha.getInt("pessoal") == 1,
                            Sql.dataHora(linha, "criado_em")));
                }
            }
        }
        return lista;
    }
}
