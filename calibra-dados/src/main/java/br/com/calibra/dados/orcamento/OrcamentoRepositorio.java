package br.com.calibra.dados.orcamento;

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

import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.NumeroOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.StatusOrcamento;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.json.Json;

/**
 * Orçamentos (bloco 2.5) na tabela {@code orcamento}, uma linha por revisão, com o histórico de status. Desde a V6,
 * o orçamento pertence a uma OS; o de preparação aponta também para o cálculo. O banco recusa mudar o conteúdo de
 * quem não é rascunho e apagar orçamento.
 */
public final class OrcamentoRepositorio {

    private static final String COLUNAS = "id, os_id, projeto_id, calculo_id, motor_versao_id, ano, sequencial, "
            + "revisao, status, conteudo_json, enviado_em, respondido_em, meio_resposta, observacao_resposta, "
            + "criado_em, atualizado_em";

    private final Clock relogio;

    public OrcamentoRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /** O próximo número da sequência do ano (a sequência recomeça a cada ano). */
    public int proximoSequencial(Connection conexao, int ano) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT COALESCE(MAX(sequencial), 0) + 1 FROM orcamento WHERE ano = ?")) {
            consulta.setInt(1, ano);
            try (ResultSet linha = consulta.executeQuery()) {
                linha.next();
                return linha.getInt(1);
            }
        }
    }

    /**
     * Revisão nova, em RASCUNHO.
     *
     * @param calculo o cálculo de preparação; null no orçamento de uma OS comum
     */
    public long inserir(Connection conexao, long osId, CalculoDoOrcamento calculo, NumeroOrcamento numero,
            Orcamento conteudo) throws SQLException {
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO orcamento (os_id, projeto_id, calculo_id, motor_versao_id, ano, sequencial, revisao, "
                        + "status, conteudo_json, total_centavos, criado_em, atualizado_em) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insercao.setLong(1, osId);
            Sql.longo(insercao, 2, calculo == null ? null : calculo.projetoId());
            Sql.longo(insercao, 3, calculo == null ? null : calculo.calculoId());
            Sql.longo(insercao, 4, calculo == null ? null : calculo.motorVersaoId());
            insercao.setInt(5, numero.ano());
            insercao.setInt(6, numero.sequencial());
            insercao.setInt(7, numero.revisao());
            insercao.setString(8, StatusOrcamento.RASCUNHO.name());
            insercao.setString(9, Json.escrever(conteudo));
            insercao.setLong(10, conteudo.total().centavos());
            insercao.setString(11, Sql.texto(agora));
            insercao.setString(12, Sql.texto(agora));
            insercao.executeUpdate();
        }
        long id = Sql.ultimoId(conexao);
        registrarStatus(conexao, id, StatusOrcamento.RASCUNHO, agora);
        return id;
    }

    /** Grava o conteúdo do rascunho (o banco recusa se não for rascunho). */
    public void atualizarConteudo(Connection conexao, long id, Orcamento conteudo) throws SQLException {
        exigirRascunho(conexao, id);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE orcamento SET conteudo_json = ?, total_centavos = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, Json.escrever(conteudo));
            atualizacao.setLong(2, conteudo.total().centavos());
            atualizacao.setString(3, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(4, id);
            atualizacao.executeUpdate();
        }
    }

    /** Aponta o rascunho para outro cálculo (o mais novo do projeto). */
    public void atualizarCalculo(Connection conexao, long id, long calculoId, long motorVersaoId) throws SQLException {
        exigirRascunho(conexao, id);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE orcamento SET calculo_id = ?, motor_versao_id = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setLong(1, calculoId);
            atualizacao.setLong(2, motorVersaoId);
            atualizacao.setString(3, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(4, id);
            atualizacao.executeUpdate();
        }
    }

    /**
     * Muda o status, um passo permitido de cada vez ({@link StatusOrcamento#podeIrPara}).
     *
     * @param quando a data e a hora informadas da aprovação ou da recusa; nos outros, ignorada (vale o relógio)
     * @param meio como o cliente respondeu; obrigatório na aprovação e na recusa
     */
    public void mudarStatus(Connection conexao, long id, StatusOrcamento novo, LocalDateTime quando, MeioAprovacao meio,
            String observacao) throws SQLException {
        OrcamentoSalvo atual = exigir(conexao, id);
        if (!atual.status().podeIrPara(novo)) {
            throw new OperacaoRecusadaException("O orçamento " + atual.numero().comRevisao() + " está \""
                    + atual.status().rotulo() + "\" e não pode passar para \"" + (novo == null ? "?" : novo.rotulo())
                    + "\".");
        }
        boolean resposta = novo == StatusOrcamento.APROVADO || novo == StatusOrcamento.RECUSADO;
        if (resposta && (quando == null || meio == null)) {
            throw new OperacaoRecusadaException("Informe a data, a hora e como o cliente respondeu.");
        }
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE orcamento SET status = ?, enviado_em = COALESCE(?, enviado_em), "
                        + "respondido_em = COALESCE(?, respondido_em), meio_resposta = COALESCE(?, meio_resposta), "
                        + "observacao_resposta = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, novo.name());
            atualizacao.setString(2, novo == StatusOrcamento.ENVIADO ? Sql.texto(agora) : null);
            atualizacao.setString(3, resposta ? Sql.texto(quando.withNano(0)) : null);
            atualizacao.setString(4, resposta ? meio.name() : null);
            atualizacao.setString(5, resposta && observacao != null ? observacao.strip() : atual.observacaoResposta());
            atualizacao.setString(6, Sql.texto(agora));
            atualizacao.setLong(7, id);
            atualizacao.executeUpdate();
        }
        registrarStatus(conexao, id, novo, agora);
    }

    public Optional<OrcamentoSalvo> buscar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM orcamento WHERE id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha)) : Optional.empty();
            }
        }
    }

    /** As revisões dos orçamentos de preparação do projeto, da mais nova para a mais antiga. */
    public List<OrcamentoSalvo> doProjeto(Connection conexao, long projetoId) throws SQLException {
        return lista(conexao, "SELECT " + COLUNAS + " FROM orcamento WHERE projeto_id = ? ORDER BY id DESC", projetoId);
    }

    /** As revisões do orçamento da OS, da mais nova para a mais antiga. */
    public List<OrcamentoSalvo> daOs(Connection conexao, long osId) throws SQLException {
        return lista(conexao, "SELECT " + COLUNAS + " FROM orcamento WHERE os_id = ? ORDER BY id DESC", osId);
    }

    /**
     * A revisão aprovada mais nova do orçamento da OS, mesmo que depois substituída por uma revisão nova (o cliente
     * pediu mais coisa): o total dela é o valor da OS até a revisão nova ser aprovada.
     */
    public Optional<OrcamentoSalvo> ultimoAprovado(Connection conexao, long osId) throws SQLException {
        return lista(conexao, "SELECT " + COLUNAS + " FROM orcamento o WHERE os_id = ? AND EXISTS (SELECT 1 FROM "
                + "orcamento_status s WHERE s.orcamento_id = o.id AND s.status = 'APROVADO') ORDER BY id DESC LIMIT 1",
                osId).stream().findFirst();
    }

    private static List<OrcamentoSalvo> lista(Connection conexao, String sql, long parametro) throws SQLException {
        List<OrcamentoSalvo> orcamentos = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setLong(1, parametro);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    orcamentos.add(ler(linha));
                }
            }
        }
        return orcamentos;
    }

    /** O histórico de status de uma revisão, do mais antigo para o mais novo. */
    public List<MudancaStatusOrcamento> historico(Connection conexao, long id) throws SQLException {
        List<MudancaStatusOrcamento> historico = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT status, data_hora FROM orcamento_status WHERE orcamento_id = ? ORDER BY id")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    historico.add(new MudancaStatusOrcamento(Sql.constante(linha, "status", StatusOrcamento.class),
                            Sql.dataHora(linha, "data_hora")));
                }
            }
        }
        return historico;
    }

    private OrcamentoSalvo exigir(Connection conexao, long id) throws SQLException {
        return buscar(conexao, id).orElseThrow(
                () -> new OperacaoRecusadaException("Orçamento #" + id + " não encontrado."));
    }

    private void exigirRascunho(Connection conexao, long id) throws SQLException {
        OrcamentoSalvo atual = exigir(conexao, id);
        if (!atual.status().editavel()) {
            throw new OperacaoRecusadaException("O orçamento " + atual.numero().comRevisao() + " está \""
                    + atual.status().rotulo() + "\" e não muda: abra uma revisão nova.");
        }
    }

    private static void registrarStatus(Connection conexao, long id, StatusOrcamento status, LocalDateTime quando)
            throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO orcamento_status (orcamento_id, status, data_hora) VALUES (?, ?, ?)")) {
            insercao.setLong(1, id);
            insercao.setString(2, status.name());
            insercao.setString(3, Sql.texto(quando));
            insercao.executeUpdate();
        }
    }

    private static OrcamentoSalvo ler(ResultSet linha) throws SQLException {
        Long projetoId = Sql.longo(linha, "projeto_id");
        CalculoDoOrcamento calculo = projetoId == null ? null : new CalculoDoOrcamento(projetoId,
                linha.getLong("calculo_id"), linha.getLong("motor_versao_id"));
        return new OrcamentoSalvo(linha.getLong("id"), linha.getLong("os_id"), calculo,
                new NumeroOrcamento(linha.getInt("ano"), linha.getInt("sequencial"), linha.getInt("revisao")),
                Sql.constante(linha, "status", StatusOrcamento.class),
                Json.ler(linha.getString("conteudo_json"), Orcamento.class), Sql.dataHora(linha, "enviado_em"),
                Sql.dataHora(linha, "respondido_em"), Sql.constante(linha, "meio_resposta", MeioAprovacao.class),
                linha.getString("observacao_resposta"), Sql.dataHora(linha, "criado_em"),
                Sql.dataHora(linha, "atualizado_em"));
    }
}
