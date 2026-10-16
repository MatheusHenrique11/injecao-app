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

import br.com.calibra.core.oficina.FormaPagamento;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;

/**
 * Recebimentos da OS na tabela {@code os_pagamento}. O banco recusa apagar e mudar recebimento; só deixa cancelar,
 * uma vez, com o motivo. Recebimento cancelado deixa de contar.
 */
public final class PagamentoRepositorio {

    private static final String COLUNAS = "id, os_id, recebido_em, forma, valor_centavos, observacao, cancelado_em, "
            + "motivo_cancelamento, criado_em";

    private final Clock relogio;

    public PagamentoRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    public long registrar(Connection conexao, long osId, Pagamento pagamento) throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO os_pagamento (os_id, recebido_em, forma, valor_centavos, observacao, criado_em) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            insercao.setLong(1, osId);
            insercao.setString(2, Sql.texto(pagamento.recebidoEm().withNano(0)));
            insercao.setString(3, pagamento.forma().name());
            insercao.setLong(4, pagamento.valor().centavos());
            insercao.setString(5, pagamento.observacao());
            insercao.setString(6, Sql.texto(Sql.agora(relogio)));
            insercao.executeUpdate();
        }
        return Sql.ultimoId(conexao);
    }

    /** Cancela um recebimento lançado errado; ele fica no histórico, com a data e o motivo. */
    public void cancelar(Connection conexao, long id, String motivo) throws SQLException {
        PagamentoSalvo atual = buscar(conexao, id).orElseThrow(
                () -> new OperacaoRecusadaException("Recebimento #" + id + " não encontrado."));
        if (atual.cancelado()) {
            throw new OperacaoRecusadaException("Este recebimento já foi cancelado.");
        }
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE os_pagamento SET cancelado_em = ?, motivo_cancelamento = ? WHERE id = ?")) {
            atualizacao.setString(1, Sql.texto(Sql.agora(relogio)));
            atualizacao.setString(2, motivo == null ? "" : motivo.strip());
            atualizacao.setLong(3, id);
            atualizacao.executeUpdate();
        }
    }

    public Optional<PagamentoSalvo> buscar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM os_pagamento WHERE id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha)) : Optional.empty();
            }
        }
    }

    /** Os recebimentos da OS, do mais antigo para o mais novo, inclusive os cancelados. */
    public List<PagamentoSalvo> daOs(Connection conexao, long osId) throws SQLException {
        List<PagamentoSalvo> lista = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM os_pagamento WHERE os_id = ? ORDER BY recebido_em, id")) {
            consulta.setLong(1, osId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    lista.add(ler(linha));
                }
            }
        }
        return lista;
    }

    /** A soma do que foi recebido e não foi cancelado. */
    public Dinheiro recebido(Connection conexao, long osId) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT COALESCE(SUM(valor_centavos), 0) FROM os_pagamento WHERE os_id = ? AND cancelado_em IS NULL")) {
            consulta.setLong(1, osId);
            try (ResultSet linha = consulta.executeQuery()) {
                linha.next();
                return new Dinheiro(linha.getLong(1));
            }
        }
    }

    private static PagamentoSalvo ler(ResultSet linha) throws SQLException {
        Pagamento pagamento = new Pagamento(Sql.dataHora(linha, "recebido_em"),
                Sql.constante(linha, "forma", FormaPagamento.class), new Dinheiro(linha.getLong("valor_centavos")),
                linha.getString("observacao"));
        return new PagamentoSalvo(linha.getLong("id"), linha.getLong("os_id"), pagamento,
                Sql.dataHora(linha, "cancelado_em"), linha.getString("motivo_cancelamento"),
                Sql.dataHora(linha, "criado_em"));
    }
}
