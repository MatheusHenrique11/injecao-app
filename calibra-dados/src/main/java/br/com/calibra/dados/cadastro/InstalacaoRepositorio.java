package br.com.calibra.dados.cadastro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.core.cadastro.Instalacao;
import br.com.calibra.core.cadastro.TipoInstalacao;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;

/**
 * As instalações do veículo (tabela {@code instalacao}, V11, SPEC 6.2). Cada gravação vai para o log de auditoria,
 * campo a campo, como a do veículo; a OS de origem aparece no histórico pelo número. O banco recusa a OS de origem de
 * outro veículo. A anonimização do cliente não mexe aqui: a instalação não é dado pessoal.
 */
public final class InstalacaoRepositorio {

    static final String ENTIDADE = "instalacao";

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String CONSULTA = "SELECT i.id, i.veiculo_id, i.os_id, o.ano, o.sequencial, i.tipo, "
            + "i.descricao, i.marca, i.modelo, i.potencia_ou_especificacao, i.data_instalacao, i.observacoes, "
            + "i.retirada_em, i.criado_em, i.atualizado_em FROM instalacao i "
            + "LEFT JOIN ordem_servico o ON o.id = i.os_id ";

    /**
     * As que estão no veículo antes das retiradas; em cada grupo, da data mais nova para a mais antiga (sem data, no
     * fim) e, na mesma data, a cadastrada por último antes.
     */
    private static final String ORDEM = " ORDER BY i.retirada_em IS NOT NULL, i.data_instalacao IS NULL, "
            + "i.data_instalacao DESC, i.id DESC";

    private final Clock relogio;

    public InstalacaoRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /** @param osId a OS de origem, do mesmo veículo; null = sem OS */
    public long inserir(Connection conexao, long veiculoId, Long osId, Instalacao instalacao) throws SQLException {
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement insercao = conexao.prepareStatement("INSERT INTO instalacao (tipo, descricao, marca, "
                + "modelo, potencia_ou_especificacao, data_instalacao, observacoes, os_id, veiculo_id, criado_em, "
                + "atualizado_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            preencher(insercao, instalacao, osId);
            insercao.setLong(9, veiculoId);
            insercao.setString(10, Sql.texto(agora));
            insercao.setString(11, Sql.texto(agora));
            insercao.executeUpdate();
        }
        long id = Sql.ultimoId(conexao);
        Auditoria.registrar(conexao, ENTIDADE, id, "Cadastro", null, "criada", agora);
        return id;
    }

    /** Grava o conteúdo e a OS de origem, com uma linha no histórico para cada campo que mudou. */
    public void salvar(Connection conexao, long id, Long osId, Instalacao nova) throws SQLException {
        InstalacaoSalva atual = exigir(conexao, id);
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement atualizacao = conexao.prepareStatement("UPDATE instalacao SET tipo = ?, descricao = ?, "
                + "marca = ?, modelo = ?, potencia_ou_especificacao = ?, data_instalacao = ?, observacoes = ?, "
                + "os_id = ?, atualizado_em = ? WHERE id = ?")) {
            preencher(atualizacao, nova, osId);
            atualizacao.setString(9, Sql.texto(agora));
            atualizacao.setLong(10, id);
            atualizacao.executeUpdate();
        }
        Auditoria.registrarMudancas(conexao, ENTIDADE, id, campos(atual.instalacao(), numero(atual.numeroOs())),
                campos(nova, numero(numeroDaOs(conexao, osId))), agora);
    }

    /**
     * O item saiu do veículo nesta data (ele continua na lista), ou volta a estar nele.
     *
     * @param data null = desfaz a retirada
     */
    public void retirar(Connection conexao, long id, LocalDate data) throws SQLException {
        InstalacaoSalva atual = exigir(conexao, id);
        if (Objects.equals(atual.retiradaEm(), data)) {
            return;
        }
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE instalacao SET retirada_em = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, Sql.texto(data));
            atualizacao.setString(2, Sql.texto(agora));
            atualizacao.setLong(3, id);
            atualizacao.executeUpdate();
        }
        Auditoria.registrar(conexao, ENTIDADE, id, "Retirada do veículo", dia(atual.retiradaEm()), dia(data), agora);
    }

    /** Apaga a instalação cadastrada por engano, com o histórico dela (como o veículo apagado). */
    public void apagar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement remocao = conexao.prepareStatement("DELETE FROM instalacao WHERE id = ?")) {
            remocao.setLong(1, id);
            if (remocao.executeUpdate() == 0) {
                throw new OperacaoRecusadaException("Instalação #" + id + " não encontrada.");
            }
        }
        Auditoria.apagarHistorico(conexao, ENTIDADE, id);
    }

    public Optional<InstalacaoSalva> buscar(Connection conexao, long id) throws SQLException {
        return lista(conexao, CONSULTA + "WHERE i.id = ?", id).stream().findFirst();
    }

    /** As instalações do veículo (também as retiradas), na ordem da lista da ficha. */
    public List<InstalacaoSalva> doVeiculo(Connection conexao, long veiculoId) throws SQLException {
        return lista(conexao, CONSULTA + "WHERE i.veiculo_id = ?" + ORDEM, veiculoId);
    }

    /** As instalações ligadas a esta OS, na mesma ordem. */
    public List<InstalacaoSalva> daOs(Connection conexao, long osId) throws SQLException {
        return lista(conexao, CONSULTA + "WHERE i.os_id = ?" + ORDEM, osId);
    }

    /** O histórico da instalação, do salvamento mais novo para o mais antigo. */
    public List<EventoAuditoria> historico(Connection conexao, long id) throws SQLException {
        return Auditoria.historico(conexao, ENTIDADE, id);
    }

    /** Os campos como aparecem no histórico (rótulo → valor); a OS de origem pelo número. */
    static Map<String, String> campos(Instalacao instalacao, String numeroOs) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("Tipo", instalacao.tipo() == null ? "" : instalacao.tipo().rotulo());
        campos.put("Descrição", instalacao.descricao());
        campos.put("Marca", instalacao.marca());
        campos.put("Modelo", instalacao.modelo());
        campos.put("Potência ou especificação", instalacao.potenciaOuEspecificacao());
        campos.put("Data da instalação", dia(instalacao.dataInstalacao()));
        campos.put("OS de origem", numeroOs);
        campos.put("Observações", instalacao.observacoes());
        return campos;
    }

    private InstalacaoSalva exigir(Connection conexao, long id) throws SQLException {
        return buscar(conexao, id).orElseThrow(
                () -> new OperacaoRecusadaException("Instalação #" + id + " não encontrada."));
    }

    private static NumeroOs numeroDaOs(Connection conexao, Long osId) throws SQLException {
        if (osId == null) {
            return null;
        }
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT ano, sequencial FROM ordem_servico WHERE id = ?")) {
            consulta.setLong(1, osId);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? new NumeroOs(linha.getInt("ano"), linha.getInt("sequencial")) : null;
            }
        }
    }

    private static String numero(NumeroOs numero) {
        return numero == null ? "" : numero.texto();
    }

    private static String dia(LocalDate data) {
        return data == null ? "" : data.format(DIA);
    }

    private static void preencher(PreparedStatement comando, Instalacao instalacao, Long osId) throws SQLException {
        Sql.nome(comando, 1, instalacao.tipo());
        comando.setString(2, instalacao.descricao());
        comando.setString(3, instalacao.marca());
        comando.setString(4, instalacao.modelo());
        comando.setString(5, instalacao.potenciaOuEspecificacao());
        comando.setString(6, Sql.texto(instalacao.dataInstalacao()));
        comando.setString(7, instalacao.observacoes());
        Sql.longo(comando, 8, osId);
    }

    private static List<InstalacaoSalva> lista(Connection conexao, String sql, long parametro) throws SQLException {
        List<InstalacaoSalva> instalacoes = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setLong(1, parametro);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    Long osId = Sql.longo(linha, "os_id");
                    Instalacao instalacao = new Instalacao(Sql.constante(linha, "tipo", TipoInstalacao.class),
                            linha.getString("descricao"), linha.getString("marca"), linha.getString("modelo"),
                            linha.getString("potencia_ou_especificacao"), Sql.data(linha, "data_instalacao"),
                            linha.getString("observacoes"));
                    instalacoes.add(new InstalacaoSalva(linha.getLong("id"), linha.getLong("veiculo_id"), osId,
                            osId == null ? null : new NumeroOs(linha.getInt("ano"), linha.getInt("sequencial")),
                            instalacao, Sql.data(linha, "retirada_em"), Sql.dataHora(linha, "criado_em"),
                            Sql.dataHora(linha, "atualizado_em")));
                }
            }
        }
        return instalacoes;
    }
}
