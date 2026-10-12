package br.com.calibra.dados.projeto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.core.util.TextoBusca;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.json.Json;

/**
 * Versões do motor do projeto (SPEC 6.3) na tabela {@code motor_versao}. Cada gravação é uma versão nova; o
 * banco recusa alterar ou apagar uma versão.
 */
public final class MotorRepositorio {

    private static final String COLUNAS = "id, projeto_id, numero, descricao, configuracao_json, criada_em";

    private final Clock relogio;

    public MotorRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /** Grava a próxima versão do motor do projeto (v1, v2...). */
    public VersaoMotor salvarVersao(Connection conexao, long projetoId, String descricao,
            ConfiguracaoMotor configuracao) throws SQLException {
        LocalDateTime agora = Sql.agora(relogio);
        String nome = descricao == null ? "" : descricao.strip();
        int numero = proximoNumero(conexao, projetoId);
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO motor_versao (projeto_id, numero, descricao, configuracao_json, cilindros, "
                        + "cilindrada_cc, aspiracao, combustivel, busca, criada_em) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insercao.setLong(1, projetoId);
            insercao.setInt(2, numero);
            insercao.setString(3, nome);
            insercao.setString(4, Json.escrever(configuracao));
            insercao.setInt(5, configuracao.numeroCilindros());
            Sql.numero(insercao, 6, configuracao.cilindradaCc());
            Sql.nome(insercao, 7, configuracao.aspiracao());
            insercao.setString(8, configuracao.combustivel() == null ? null : configuracao.combustivel().codigo());
            insercao.setString(9, busca(nome, configuracao));
            insercao.setString(10, Sql.texto(agora));
            insercao.executeUpdate();
        }
        return new VersaoMotor(Sql.ultimoId(conexao), projetoId, numero, nome, configuracao, agora);
    }

    /** A versão mais nova do motor do projeto. */
    public Optional<VersaoMotor> atual(Connection conexao, long projetoId) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM motor_versao WHERE projeto_id = ? ORDER BY numero DESC LIMIT 1")) {
            consulta.setLong(1, projetoId);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha)) : Optional.empty();
            }
        }
    }

    /** Todas as versões do motor do projeto, da v1 em diante. */
    public List<VersaoMotor> versoes(Connection conexao, long projetoId) throws SQLException {
        List<VersaoMotor> versoes = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM motor_versao WHERE projeto_id = ? ORDER BY numero")) {
            consulta.setLong(1, projetoId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    versoes.add(ler(linha));
                }
            }
        }
        return versoes;
    }

    public Optional<VersaoMotor> buscar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM motor_versao WHERE id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha)) : Optional.empty();
            }
        }
    }

    private static int proximoNumero(Connection conexao, long projetoId) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT COALESCE(MAX(numero), 0) + 1 FROM motor_versao WHERE projeto_id = ?")) {
            consulta.setLong(1, projetoId);
            try (ResultSet linha = consulta.executeQuery()) {
                linha.next();
                return linha.getInt(1);
            }
        }
    }

    /** "AP 2.0 8V", "4 cilindros", "2,0" litros, aspiração e combustível, sem acento. */
    private static String busca(String descricao, ConfiguracaoMotor m) {
        String litros = Double.isFinite(m.cilindradaCc()) ? NumeroBr.formatar(m.cilindradaCc() / 1000.0, 1) : "";
        return TextoBusca.juntar(descricao, m.numeroCilindros() + " cilindros", litros,
                m.aspiracao() == null ? "" : m.aspiracao().rotulo(),
                m.combustivel() == null ? "" : m.combustivel().nome() + " " + m.combustivel().codigo());
    }

    private static VersaoMotor ler(ResultSet linha) throws SQLException {
        return new VersaoMotor(linha.getLong("id"), linha.getLong("projeto_id"), linha.getInt("numero"),
                linha.getString("descricao"), Json.ler(linha.getString("configuracao_json"), ConfiguracaoMotor.class),
                Sql.dataHora(linha, "criada_em"));
    }
}
