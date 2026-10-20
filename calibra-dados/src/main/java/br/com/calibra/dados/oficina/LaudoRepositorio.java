package br.com.calibra.dados.oficina;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.core.oficina.LaudoEletrico;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.json.Json;

/**
 * Os laudos de diagnóstico elétrico da OS (tabela {@code laudo}, V10, Fase 3B). O conteúdo fica em JSON; o número da
 * OS e o nome do mecânico vêm junto na leitura. Com a OS entregue ou cancelada, o banco recusa criar, mudar e apagar.
 */
public final class LaudoRepositorio {

    private static final String CONSULTA = "SELECT l.id, l.os_id, l.mecanico_id, m.nome AS mecanico, "
            + "l.conteudo_json, l.criado_em, l.atualizado_em, o.ano, o.sequencial FROM laudo l "
            + "JOIN ordem_servico o ON o.id = l.os_id LEFT JOIN mecanico m ON m.id = l.mecanico_id ";

    /** Pela data e hora do laudo (sem data, antes), e na mesma data, pela ordem em que foram criados. */
    private static final Comparator<LaudoSalvo> PELA_DATA = Comparator
            .comparing((LaudoSalvo l) -> l.laudo().dataHora(), Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(LaudoSalvo::id);

    private final Clock relogio;

    public LaudoRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /** @param mecanicoId null = sem mecânico */
    public long inserir(Connection conexao, long osId, Long mecanicoId, LaudoEletrico laudo) throws SQLException {
        String agora = Sql.texto(Sql.agora(relogio));
        try (PreparedStatement insercao = conexao.prepareStatement("INSERT INTO laudo (os_id, mecanico_id, "
                + "conteudo_json, criado_em, atualizado_em) VALUES (?, ?, ?, ?, ?)")) {
            insercao.setLong(1, osId);
            Sql.longo(insercao, 2, mecanicoId);
            insercao.setString(3, Json.escrever(laudo));
            insercao.setString(4, agora);
            insercao.setString(5, agora);
            insercao.executeUpdate();
        }
        return Sql.ultimoId(conexao);
    }

    /** Grava o laudo como está agora (quem fez e o conteúdo). */
    public void salvar(Connection conexao, long id, Long mecanicoId, LaudoEletrico laudo) throws SQLException {
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE laudo SET mecanico_id = ?, conteudo_json = ?, atualizado_em = ? WHERE id = ?")) {
            Sql.longo(atualizacao, 1, mecanicoId);
            atualizacao.setString(2, Json.escrever(laudo));
            atualizacao.setString(3, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(4, id);
            if (atualizacao.executeUpdate() == 0) {
                throw new OperacaoRecusadaException("Laudo #" + id + " não encontrado.");
            }
        }
    }

    public void apagar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement remocao = conexao.prepareStatement("DELETE FROM laudo WHERE id = ?")) {
            remocao.setLong(1, id);
            if (remocao.executeUpdate() == 0) {
                throw new OperacaoRecusadaException("Laudo #" + id + " não encontrado.");
            }
        }
    }

    public Optional<LaudoSalvo> buscar(Connection conexao, long id) throws SQLException {
        return lista(conexao, CONSULTA + "WHERE l.id = ?", id).stream().findFirst();
    }

    /** Os laudos da OS, do mais antigo para o mais novo (pela data e hora de cada um). */
    public List<LaudoSalvo> daOs(Connection conexao, long osId) throws SQLException {
        List<LaudoSalvo> laudos = lista(conexao, CONSULTA + "WHERE l.os_id = ?", osId);
        laudos.sort(PELA_DATA);
        return laudos;
    }

    /** Os laudos de todas as OS do veículo, do mais novo para o mais antigo (para a ficha do veículo). */
    public List<LaudoSalvo> doVeiculo(Connection conexao, long veiculoId) throws SQLException {
        List<LaudoSalvo> laudos = lista(conexao, CONSULTA + "WHERE o.veiculo_id = ?", veiculoId);
        laudos.sort(PELA_DATA.reversed());
        return laudos;
    }

    private static List<LaudoSalvo> lista(Connection conexao, String sql, long parametro) throws SQLException {
        List<LaudoSalvo> laudos = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setLong(1, parametro);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    laudos.add(new LaudoSalvo(linha.getLong("id"), linha.getLong("os_id"),
                            new NumeroOs(linha.getInt("ano"), linha.getInt("sequencial")),
                            Sql.longo(linha, "mecanico_id"), linha.getString("mecanico"),
                            Json.ler(linha.getString("conteudo_json"), LaudoEletrico.class),
                            Sql.dataHora(linha, "criado_em"), Sql.dataHora(linha, "atualizado_em")));
                }
            }
        }
        return laudos;
    }
}
