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

import br.com.calibra.core.oficina.FichaChicote;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.SituacaoChicote;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.json.Json;

/**
 * As fichas de chicote da OS (tabela {@code ficha_chicote}, V13, Fase 3B). O conteúdo fica em JSON; o número da OS e se
 * já há versão nova vêm junto na leitura. O banco recusa mudar e apagar a versão que já tem uma nova, e criar, mudar e
 * apagar ficha com a OS entregue ou cancelada.
 */
public final class FichaChicoteRepositorio {

    private static final String CONSULTA = "SELECT f.id, f.os_id, f.versao_anterior_id, f.conteudo_json, "
            + "f.criado_em, f.atualizado_em, o.ano, o.sequencial, "
            + "EXISTS (SELECT 1 FROM ficha_chicote n WHERE n.versao_anterior_id = f.id) AS tem_versao_nova "
            + "FROM ficha_chicote f JOIN ordem_servico o ON o.id = f.os_id ";

    private final Clock relogio;

    public FichaChicoteRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /** @param versaoAnteriorId a ficha de onde a versão nova começa; null na primeira versão */
    public long inserir(Connection conexao, long osId, Long versaoAnteriorId, FichaChicote ficha)
            throws SQLException {
        String agora = Sql.texto(Sql.agora(relogio));
        try (PreparedStatement insercao = conexao.prepareStatement("INSERT INTO ficha_chicote (os_id, "
                + "versao_anterior_id, conteudo_json, criado_em, atualizado_em) VALUES (?, ?, ?, ?, ?)")) {
            insercao.setLong(1, osId);
            Sql.longo(insercao, 2, versaoAnteriorId);
            insercao.setString(3, Json.escrever(ficha));
            insercao.setString(4, agora);
            insercao.setString(5, agora);
            insercao.executeUpdate();
        }
        return Sql.ultimoId(conexao);
    }

    /** Grava a ficha como está agora. */
    public void salvar(Connection conexao, long id, FichaChicote ficha) throws SQLException {
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE ficha_chicote SET conteudo_json = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, Json.escrever(ficha));
            atualizacao.setString(2, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(3, id);
            if (atualizacao.executeUpdate() == 0) {
                throw new OperacaoRecusadaException("Ficha do chicote #" + id + " não encontrada.");
            }
        }
    }

    public void apagar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement remocao = conexao.prepareStatement("DELETE FROM ficha_chicote WHERE id = ?")) {
            remocao.setLong(1, id);
            if (remocao.executeUpdate() == 0) {
                throw new OperacaoRecusadaException("Ficha do chicote #" + id + " não encontrada.");
            }
        }
    }

    public Optional<FichaChicoteSalva> buscar(Connection conexao, long id) throws SQLException {
        return lista(conexao, CONSULTA + "WHERE f.id = ?", id).stream().findFirst();
    }

    /** As fichas da OS, na ordem em que foram feitas (a versão nova depois da anterior). */
    public List<FichaChicoteSalva> daOs(Connection conexao, long osId) throws SQLException {
        return lista(conexao, CONSULTA + "WHERE f.os_id = ? ORDER BY f.id", osId);
    }

    /** As fichas de todas as OS do veículo, da mais nova para a mais antiga (para a ficha do veículo). */
    public List<FichaChicoteSalva> doVeiculo(Connection conexao, long veiculoId) throws SQLException {
        return lista(conexao, CONSULTA + "WHERE o.veiculo_id = ? ORDER BY f.id DESC", veiculoId);
    }

    /**
     * Na entrega da OS (antes de ela ficar entregue): a última versão de cada chicote fica "entregue". A versão que já
     * tem uma nova fica como estava.
     *
     * @return quantas fichas mudaram
     */
    public int marcarEntregues(Connection conexao, long osId) throws SQLException {
        int mudaram = 0;
        for (FichaChicoteSalva salva : daOs(conexao, osId)) {
            FichaChicote ficha = salva.ficha();
            if (salva.temVersaoNova() || ficha.situacao() == SituacaoChicote.ENTREGUE) {
                continue;
            }
            salvar(conexao, salva.id(), new FichaChicote(ficha.injecao(), ficha.injecaoItemId(),
                    ficha.motorVersaoId(), ficha.versaoChicote(), ficha.fontePinout(), ficha.pontos(),
                    ficha.materiais(), ficha.observacoes(), SituacaoChicote.ENTREGUE));
            mudaram++;
        }
        return mudaram;
    }

    private static List<FichaChicoteSalva> lista(Connection conexao, String sql, long parametro) throws SQLException {
        List<FichaChicoteSalva> fichas = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setLong(1, parametro);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    fichas.add(new FichaChicoteSalva(linha.getLong("id"), linha.getLong("os_id"),
                            new NumeroOs(linha.getInt("ano"), linha.getInt("sequencial")),
                            Sql.longo(linha, "versao_anterior_id"), linha.getInt("tem_versao_nova") == 1,
                            Json.ler(linha.getString("conteudo_json"), FichaChicote.class),
                            Sql.dataHora(linha, "criado_em"), Sql.dataHora(linha, "atualizado_em")));
                }
            }
        }
        return fichas;
    }
}
