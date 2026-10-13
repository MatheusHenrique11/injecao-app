package br.com.calibra.dados.catalogo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.util.TextoBusca;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.json.Json;

/**
 * Catálogos (SPEC 8.5) nas tabelas {@code catalogo_item} (versão atual, com nome e busca em colunas) e
 * {@code catalogo_versao} (todas as versões; o banco recusa alterar ou apagar). Editar um item grava uma versão
 * nova; nada é apagado, só arquivado.
 */
public final class CatalogoRepositorio {

    private static final String COLUNAS = "id, tipo, chave_semente, versao_atual, nome, fonte, data, a_verificar, "
            + "preco_centavos, observacoes, dados_json, arquivado, atualizado_em";

    private final Clock relogio;

    public CatalogoRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /** Item novo, na versão 1. */
    public <T> ItemCatalogo<T> inserir(Connection conexao, TipoCatalogo<T> tipo, T dados, InfoCatalogo info,
            String chaveSemente) throws SQLException {
        LocalDateTime agora = Sql.agora(relogio);
        T naVersao = tipo.comVersao(dados, 1);
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO catalogo_item (nome, fonte, data, a_verificar, preco_centavos, observacoes, dados_json, "
                        + "busca, tipo, chave_semente, versao_atual, criado_em, atualizado_em) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)")) {
            preencher(insercao, naVersao, info);
            insercao.setString(9, tipo.codigo());
            insercao.setString(10, chaveSemente);
            insercao.setString(11, Sql.texto(agora));
            insercao.setString(12, Sql.texto(agora));
            insercao.executeUpdate();
        }
        long id = Sql.ultimoId(conexao);
        inserirVersao(conexao, id, 1, naVersao, info, agora);
        return new ItemCatalogo<>(id, tipo, 1, naVersao, info, false, chaveSemente, agora);
    }

    /** Grava uma versão nova do item (a anterior fica guardada como estava). */
    public <T> ItemCatalogo<T> novaVersao(Connection conexao, TipoCatalogo<T> tipo, long id, T dados,
            InfoCatalogo info) throws SQLException {
        ItemCatalogo<T> atual = exigir(conexao, tipo, id);
        LocalDateTime agora = Sql.agora(relogio);
        int versao = atual.versao() + 1;
        T naVersao = tipo.comVersao(dados, versao);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE catalogo_item SET nome = ?, fonte = ?, data = ?, a_verificar = ?, preco_centavos = ?, "
                        + "observacoes = ?, dados_json = ?, busca = ?, versao_atual = ?, atualizado_em = ? WHERE id = ?")) {
            preencher(atualizacao, naVersao, info);
            atualizacao.setInt(9, versao);
            atualizacao.setString(10, Sql.texto(agora));
            atualizacao.setLong(11, id);
            atualizacao.executeUpdate();
        }
        inserirVersao(conexao, id, versao, naVersao, info, agora);
        return new ItemCatalogo<>(id, tipo, versao, naVersao, info, atual.arquivado(), atual.chaveSemente(), agora);
    }

    public <T> Optional<ItemCatalogo<T>> buscar(Connection conexao, TipoCatalogo<T> tipo, long id)
            throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM catalogo_item WHERE id = ? AND tipo = ?")) {
            consulta.setLong(1, id);
            consulta.setString(2, tipo.codigo());
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha, tipo)) : Optional.empty();
            }
        }
    }

    /** Item que veio com o app, pela chave da semente (ex.: "motor:vw-ap-2.0"). */
    public <T> Optional<ItemCatalogo<T>> porChaveSemente(Connection conexao, TipoCatalogo<T> tipo, String chave)
            throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM catalogo_item WHERE chave_semente = ? AND tipo = ?")) {
            consulta.setString(1, chave);
            consulta.setString(2, tipo.codigo());
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha, tipo)) : Optional.empty();
            }
        }
    }

    /**
     * Itens de um tipo em ordem de nome. Cada palavra do termo precisa aparecer no nome, na fonte ou nas
     * observações, sem diferença de acento.
     */
    public <T> List<ItemCatalogo<T>> listar(Connection conexao, TipoCatalogo<T> tipo, String termo,
            boolean incluirArquivados) throws SQLException {
        List<String> palavras = TextoBusca.palavras(termo);
        String sql = "SELECT " + COLUNAS + " FROM catalogo_item WHERE tipo = ?"
                + (incluirArquivados ? "" : " AND arquivado = 0")
                + " AND busca LIKE ?".repeat(palavras.size())
                + " ORDER BY nome COLLATE NOCASE, id";
        List<ItemCatalogo<T>> itens = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setString(1, tipo.codigo());
            for (int i = 0; i < palavras.size(); i++) {
                consulta.setString(i + 2, "%" + palavras.get(i) + "%");
            }
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    itens.add(ler(linha, tipo));
                }
            }
        }
        return itens;
    }

    /** Todas as versões do item, da mais nova para a mais antiga. */
    public <T> List<VersaoCatalogo<T>> versoes(Connection conexao, TipoCatalogo<T> tipo, long id)
            throws SQLException {
        return lerVersoes(conexao, tipo, id, null);
    }

    public <T> Optional<VersaoCatalogo<T>> versao(Connection conexao, TipoCatalogo<T> tipo, long id, int versao)
            throws SQLException {
        return lerVersoes(conexao, tipo, id, versao).stream().findFirst();
    }

    /** Arquiva (sai das escolhas, mas continua nos cálculos antigos) ou reativa. */
    public void arquivar(Connection conexao, long id, boolean arquivado) throws SQLException {
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE catalogo_item SET arquivado = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setInt(1, arquivado ? 1 : 0);
            atualizacao.setString(2, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(3, id);
            if (atualizacao.executeUpdate() == 0) {
                throw new OperacaoRecusadaException("Item de catálogo #" + id + " não encontrado.");
            }
        }
    }

    /** Nome e versão atual de cada item pedido (os que não existirem ficam de fora). */
    public Map<Long, ResumoItem> resumos(Connection conexao, Collection<Long> ids) throws SQLException {
        Map<Long, ResumoItem> resumos = new HashMap<>();
        if (ids.isEmpty()) {
            return resumos;
        }
        String marcadores = String.join(", ", Collections.nCopies(ids.size(), "?"));
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT id, tipo, nome, versao_atual, arquivado FROM catalogo_item WHERE id IN (" + marcadores + ")")) {
            int posicao = 1;
            for (long id : ids) {
                consulta.setLong(posicao++, id);
            }
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    resumos.put(linha.getLong("id"), new ResumoItem(linha.getLong("id"), linha.getString("tipo"),
                            linha.getString("nome"), linha.getInt("versao_atual"), linha.getInt("arquivado") == 1));
                }
            }
        }
        return resumos;
    }

    private <T> ItemCatalogo<T> exigir(Connection conexao, TipoCatalogo<T> tipo, long id) throws SQLException {
        return buscar(conexao, tipo, id).orElseThrow(() -> new OperacaoRecusadaException(
                "Item de catálogo #" + id + " (" + tipo.singular() + ") não encontrado."));
    }

    private static void inserirVersao(Connection conexao, long id, int versao, Object dados, InfoCatalogo info,
            LocalDateTime quando) throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO catalogo_versao (nome, fonte, data, a_verificar, preco_centavos, observacoes, dados_json, "
                        + "item_id, versao, criada_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            preencherInfo(insercao, info);
            insercao.setString(7, Json.escrever(dados));
            insercao.setLong(8, id);
            insercao.setInt(9, versao);
            insercao.setString(10, Sql.texto(quando));
            insercao.executeUpdate();
        }
    }

    private static <T> List<VersaoCatalogo<T>> lerVersoes(Connection conexao, TipoCatalogo<T> tipo, long id,
            Integer versao) throws SQLException {
        List<VersaoCatalogo<T>> versoes = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT v.item_id, v.versao, v.nome, v.fonte, v.data, v.a_verificar, v.preco_centavos, v.observacoes, "
                        + "v.dados_json, v.criada_em FROM catalogo_versao v JOIN catalogo_item i ON i.id = v.item_id "
                        + "WHERE v.item_id = ? AND i.tipo = ?" + (versao == null ? "" : " AND v.versao = ?")
                        + " ORDER BY v.versao DESC")) {
            consulta.setLong(1, id);
            consulta.setString(2, tipo.codigo());
            if (versao != null) {
                consulta.setInt(3, versao);
            }
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    versoes.add(new VersaoCatalogo<>(linha.getLong("item_id"), linha.getInt("versao"),
                            Json.ler(linha.getString("dados_json"), tipo.classe()), lerInfo(linha),
                            Sql.dataHora(linha, "criada_em")));
                }
            }
        }
        return versoes;
    }

    /** Colunas 1 a 8: nome, fonte, data, a verificar, preço, observações, dados e busca. */
    private static void preencher(PreparedStatement comando, Object dados, InfoCatalogo info) throws SQLException {
        preencherInfo(comando, info);
        comando.setString(7, Json.escrever(dados));
        comando.setString(8, TextoBusca.juntar(info.nome(), info.fonte(), info.observacoes()));
    }

    /** Colunas 1 a 6: nome, fonte, data, a verificar, preço e observações. */
    private static void preencherInfo(PreparedStatement comando, InfoCatalogo info) throws SQLException {
        comando.setString(1, info.nome());
        comando.setString(2, info.fonte());
        comando.setString(3, Sql.texto(info.data()));
        comando.setInt(4, info.aVerificar() ? 1 : 0);
        if (info.precoReferencia() == null) {
            comando.setNull(5, Types.INTEGER);
        } else {
            comando.setLong(5, info.precoReferencia().centavos());
        }
        comando.setString(6, info.observacoes());
    }

    private static InfoCatalogo lerInfo(ResultSet linha) throws SQLException {
        long centavos = linha.getLong("preco_centavos");
        Dinheiro preco = linha.wasNull() ? null : new Dinheiro(centavos);
        return new InfoCatalogo(linha.getString("nome"), linha.getString("fonte"), Sql.data(linha, "data"),
                linha.getInt("a_verificar") == 1, preco, linha.getString("observacoes"));
    }

    private static <T> ItemCatalogo<T> ler(ResultSet linha, TipoCatalogo<T> tipo) throws SQLException {
        return new ItemCatalogo<>(linha.getLong("id"), tipo, linha.getInt("versao_atual"),
                Json.ler(linha.getString("dados_json"), tipo.classe()), lerInfo(linha),
                linha.getInt("arquivado") == 1, linha.getString("chave_semente"), Sql.dataHora(linha, "atualizado_em"));
    }
}
