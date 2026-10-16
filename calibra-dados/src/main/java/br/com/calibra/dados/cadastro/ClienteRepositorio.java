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

import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.util.TextoBusca;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;

/**
 * Clientes (SPEC 6.1) na tabela {@code cliente}, com o histórico na auditoria. Os métodos recebem a conexão
 * para entrar na mesma transação de outras gravações (ex.: o assistente de novo projeto).
 */
public final class ClienteRepositorio {

    static final String ENTIDADE = "cliente";

    /** Campos que identificam a pessoa: apagados também no histórico ao anonimizar. */
    static final List<String> CAMPOS_PESSOAIS = List.of("Nome", "Telefone", "E-mail", "CPF ou CNPJ", "Observações");

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String COLUNAS = "id, nome, telefone, email, documento, consentimento_data, "
            + "consentimento_finalidade, observacoes, anonimizado, criado_em, atualizado_em";

    private final Clock relogio;

    public ClienteRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    public long inserir(Connection conexao, Cliente cliente) throws SQLException {
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO cliente (nome, telefone, email, documento, consentimento_data, consentimento_finalidade, "
                        + "observacoes, anonimizado, busca, criado_em, atualizado_em) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            preencher(insercao, cliente);
            insercao.setString(10, Sql.texto(agora));
            insercao.setString(11, Sql.texto(agora));
            insercao.executeUpdate();
        }
        long id = Sql.ultimoId(conexao);
        Auditoria.registrar(conexao, ENTIDADE, id, "Cadastro", null, "criado", agora);
        return id;
    }

    /** Grava as mudanças e uma linha no histórico para cada campo que mudou. */
    public void atualizar(Connection conexao, long id, Cliente novo) throws SQLException {
        ClienteSalvo atual = exigir(conexao, id);
        if (atual.cliente().anonimizado() || novo.anonimizado()) {
            throw new OperacaoRecusadaException("Cliente anonimizado não é editado: os dados pessoais foram apagados.");
        }
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE cliente SET nome = ?, telefone = ?, email = ?, documento = ?, consentimento_data = ?, "
                        + "consentimento_finalidade = ?, observacoes = ?, anonimizado = ?, busca = ?, atualizado_em = ? "
                        + "WHERE id = ?")) {
            preencher(atualizacao, novo);
            atualizacao.setString(10, Sql.texto(agora));
            atualizacao.setLong(11, id);
            atualizacao.executeUpdate();
        }
        Auditoria.registrarMudancas(conexao, ENTIDADE, id, campos(atual.cliente()), campos(novo), agora);
    }

    public Optional<ClienteSalvo> buscar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM cliente WHERE id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha)) : Optional.empty();
            }
        }
    }

    /**
     * Clientes cujo nome, telefone, e-mail ou documento, ou os veículos (placa, embarcação, marca, modelo), contêm
     * todas as palavras do termo (sem acento). Os anonimizados ficam de fora: não recebem projeto nem OS novos.
     */
    public List<ClienteSalvo> pesquisar(Connection conexao, String termo, int limite) throws SQLException {
        List<String> palavras = TextoBusca.palavras(termo);
        StringBuilder sql = new StringBuilder("SELECT " + COLUNAS + " FROM cliente WHERE anonimizado = 0");
        // Cada palavra vale no cliente ou nos veículos dele (placa, embarcação, marca, modelo): o balcão busca pela
        // placa do carro que chegou.
        sql.append((" AND (busca || ' ' || COALESCE((SELECT group_concat(v.busca, ' ') FROM veiculo v "
                + "WHERE v.cliente_id = cliente.id), '')) LIKE ?").repeat(palavras.size()));
        sql.append(" ORDER BY nome COLLATE NOCASE, id LIMIT ?");
        List<ClienteSalvo> clientes = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(sql.toString())) {
            int posicao = 1;
            for (String palavra : palavras) {
                consulta.setString(posicao++, "%" + palavra + "%");
            }
            consulta.setInt(posicao, limite);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    clientes.add(ler(linha));
                }
            }
        }
        return clientes;
    }

    public List<EventoAuditoria> historico(Connection conexao, long id) throws SQLException {
        return Auditoria.historico(conexao, ENTIDADE, id);
    }

    /**
     * Anonimiza o cliente (SPEC 6.1, LGPD): o nome vira "Cliente anonimizado #id", telefone, e-mail, documento e
     * observações são apagados, e os veículos dele perdem placa, chassi e identificação da embarcação. O
     * histórico também perde esses valores. Projetos, motores e cálculos ficam.
     */
    public void anonimizar(Connection conexao, long id) throws SQLException {
        ClienteSalvo atual = exigir(conexao, id);
        if (atual.cliente().anonimizado()) {
            return;
        }
        LocalDateTime agora = Sql.agora(relogio);
        Cliente anonimo = atual.cliente().anonimizar(id);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE cliente SET nome = ?, telefone = ?, email = ?, documento = ?, consentimento_data = ?, "
                        + "consentimento_finalidade = ?, observacoes = ?, anonimizado = ?, busca = ?, atualizado_em = ? "
                        + "WHERE id = ?")) {
            preencher(atualizacao, anonimo);
            atualizacao.setString(10, Sql.texto(agora));
            atualizacao.setLong(11, id);
            atualizacao.executeUpdate();
        }
        Auditoria.apagarValores(conexao, ENTIDADE, id, CAMPOS_PESSOAIS);
        Auditoria.registrar(conexao, ENTIDADE, id, "Anonimização", null, "dados pessoais apagados (LGPD)", agora);
        VeiculoRepositorio.anonimizarDoCliente(conexao, id, agora);
    }

    /** Apaga um cliente sem veículos (cadastro feito por engano). Com veículo, só anonimizar. */
    public void apagar(Connection conexao, long id) throws SQLException {
        exigir(conexao, id);
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT 1 FROM veiculo WHERE cliente_id = ? LIMIT 1")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                if (linha.next()) {
                    throw new OperacaoRecusadaException("O cliente tem veículos cadastrados e não pode ser apagado. "
                            + "Para apagar os dados pessoais, use \"Anonimizar cliente\".");
                }
            }
        }
        try (PreparedStatement remocao = conexao.prepareStatement("DELETE FROM cliente WHERE id = ?")) {
            remocao.setLong(1, id);
            remocao.executeUpdate();
        }
        Auditoria.apagarHistorico(conexao, ENTIDADE, id);
    }

    /** Os campos como aparecem no histórico (rótulo → valor). */
    static Map<String, String> campos(Cliente cliente) {
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("Nome", cliente.nome());
        campos.put("Telefone", cliente.telefone());
        campos.put("E-mail", cliente.email());
        campos.put("CPF ou CNPJ", cliente.documento());
        Consentimento consentimento = cliente.consentimento();
        campos.put("Data do consentimento",
                consentimento == null || consentimento.data() == null ? "" : DIA.format(consentimento.data()));
        campos.put("Finalidade do consentimento", consentimento == null ? "" : consentimento.finalidade());
        campos.put("Observações", cliente.observacoes());
        return campos;
    }

    private ClienteSalvo exigir(Connection conexao, long id) throws SQLException {
        return buscar(conexao, id).orElseThrow(
                () -> new OperacaoRecusadaException("Cliente #" + id + " não encontrado."));
    }

    private static void preencher(PreparedStatement comando, Cliente cliente) throws SQLException {
        Consentimento consentimento = cliente.consentimento();
        comando.setString(1, cliente.nome());
        comando.setString(2, cliente.telefone());
        comando.setString(3, cliente.email());
        comando.setString(4, cliente.documento());
        comando.setString(5, consentimento == null ? null : Sql.texto(consentimento.data()));
        comando.setString(6, consentimento == null ? "" : consentimento.finalidade());
        comando.setString(7, cliente.observacoes());
        comando.setInt(8, cliente.anonimizado() ? 1 : 0);
        comando.setString(9, TextoBusca.juntar(cliente.nome(), cliente.telefone(), cliente.email(),
                cliente.documento()));
    }

    private static ClienteSalvo ler(ResultSet linha) throws SQLException {
        LocalDate data = Sql.data(linha, "consentimento_data");
        String finalidade = linha.getString("consentimento_finalidade");
        Consentimento consentimento = data == null && finalidade.isEmpty() ? null : new Consentimento(data, finalidade);
        Cliente cliente = new Cliente(linha.getString("nome"), linha.getString("telefone"), linha.getString("email"),
                linha.getString("documento"), consentimento, linha.getString("observacoes"),
                linha.getInt("anonimizado") == 1);
        return new ClienteSalvo(linha.getLong("id"), cliente, Sql.dataHora(linha, "criado_em"),
                Sql.dataHora(linha, "atualizado_em"));
    }
}
