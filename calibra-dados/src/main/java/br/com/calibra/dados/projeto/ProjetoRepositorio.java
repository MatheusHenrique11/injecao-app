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

import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.util.TextoBusca;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.json.Json;

/**
 * Projetos (SPEC 6.4) na tabela {@code projeto}, com o histórico de status. A meta fica em JSON; o status, em
 * coluna para filtrar a lista.
 */
public final class ProjetoRepositorio {

    private static final String COLUNAS = "id, veiculo_id, status, status_antes_de_arquivar, meta_json, observacoes, "
            + "criado_em, atualizado_em";

    private final Clock relogio;

    public ProjetoRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /** Projeto novo, em RASCUNHO. */
    public long inserir(Connection conexao, long veiculoId, Meta meta, String observacoes) throws SQLException {
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO projeto (veiculo_id, status, meta_json, observacoes, criado_em, atualizado_em) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
            insercao.setLong(1, veiculoId);
            insercao.setString(2, StatusProjeto.RASCUNHO.name());
            insercao.setString(3, Json.escrever(meta));
            insercao.setString(4, observacoes == null ? "" : observacoes.strip());
            insercao.setString(5, Sql.texto(agora));
            insercao.setString(6, Sql.texto(agora));
            insercao.executeUpdate();
        }
        long id = Sql.ultimoId(conexao);
        registrarStatus(conexao, id, StatusProjeto.RASCUNHO, agora);
        return id;
    }

    public Optional<ProjetoSalvo> buscar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM projeto WHERE id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha)) : Optional.empty();
            }
        }
    }

    /** Projetos de um veículo, do mais novo para o mais antigo. */
    public List<ProjetoSalvo> doVeiculo(Connection conexao, long veiculoId) throws SQLException {
        List<ProjetoSalvo> projetos = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM projeto WHERE veiculo_id = ? ORDER BY id DESC")) {
            consulta.setLong(1, veiculoId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    projetos.add(ler(linha));
                }
            }
        }
        return projetos;
    }

    public void atualizarMeta(Connection conexao, long id, Meta meta) throws SQLException {
        exigir(conexao, id);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE projeto SET meta_json = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, Json.escrever(meta));
            atualizacao.setString(2, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(3, id);
            atualizacao.executeUpdate();
        }
    }

    public void atualizarObservacoes(Connection conexao, long id, String observacoes) throws SQLException {
        exigir(conexao, id);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE projeto SET observacoes = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, observacoes == null ? "" : observacoes.strip());
            atualizacao.setString(2, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(3, id);
            atualizacao.executeUpdate();
        }
    }

    /** Marca o projeto como mexido agora (versão do motor ou cálculo novos): ele sobe na lista. */
    public void tocar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE projeto SET atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(2, id);
            atualizacao.executeUpdate();
        }
    }

    /**
     * Muda o status (SPEC 6.4): para a frente, um passo de cada vez; arquivar, a qualquer momento; um projeto
     * arquivado só volta para o status que tinha antes de arquivar.
     *
     * @throws OperacaoRecusadaException se a mudança não for permitida
     */
    public void mudarStatus(Connection conexao, long id, StatusProjeto novo) throws SQLException {
        ProjetoSalvo atual = exigir(conexao, id);
        if (!atual.status().podeIrPara(novo)) {
            throw new OperacaoRecusadaException("O projeto está \"" + atual.status().rotulo()
                    + "\" e não pode passar para \"" + (novo == null ? "?" : novo.rotulo()) + "\".");
        }
        StatusProjeto antesDeArquivar = null;
        if (novo == StatusProjeto.ARQUIVADO) {
            antesDeArquivar = atual.status();
        } else if (atual.status() == StatusProjeto.ARQUIVADO && novo != voltaDoArquivo(atual)) {
            throw new OperacaoRecusadaException("Ao reativar, o projeto volta para \""
                    + voltaDoArquivo(atual).rotulo() + "\", o status de antes de arquivar.");
        }
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE projeto SET status = ?, status_antes_de_arquivar = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, novo.name());
            Sql.nome(atualizacao, 2, antesDeArquivar);
            atualizacao.setString(3, Sql.texto(agora));
            atualizacao.setLong(4, id);
            atualizacao.executeUpdate();
        }
        registrarStatus(conexao, id, novo, agora);
    }

    public void arquivar(Connection conexao, long id) throws SQLException {
        mudarStatus(conexao, id, StatusProjeto.ARQUIVADO);
    }

    /** Tira do arquivo, de volta ao status de antes. */
    public void reativar(Connection conexao, long id) throws SQLException {
        mudarStatus(conexao, id, voltaDoArquivo(exigir(conexao, id)));
    }

    /** Histórico de status, do mais antigo para o mais novo. */
    public List<MudancaStatus> historicoStatus(Connection conexao, long id) throws SQLException {
        List<MudancaStatus> historico = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT status, data_hora FROM projeto_status WHERE projeto_id = ? ORDER BY id")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    historico.add(new MudancaStatus(Sql.constante(linha, "status", StatusProjeto.class),
                            Sql.dataHora(linha, "data_hora")));
                }
            }
        }
        return historico;
    }

    /**
     * Lista de projetos (tela Início), do mexido mais recentemente para o mais antigo. Cada palavra do termo
     * precisa aparecer no cliente, no veículo (placa, embarcação, marca, modelo) ou no motor, sem diferença de
     * acento.
     */
    public List<ResumoProjeto> pesquisar(Connection conexao, FiltroProjetos filtro, int limite) throws SQLException {
        List<String> palavras = TextoBusca.palavras(filtro.termo());
        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.status, p.atualizado_em, c.nome, v.tipo, v.marca, v.modelo, v.versao, v.placa, "
                        + "v.nome_embarcacao, m.numero, m.descricao, m.cilindros, m.cilindrada_cc, m.aspiracao "
                        + "FROM projeto p "
                        + "JOIN veiculo v ON v.id = p.veiculo_id "
                        + "JOIN cliente c ON c.id = v.cliente_id "
                        + "JOIN motor_versao m ON m.projeto_id = p.id "
                        + "WHERE m.numero = (SELECT MAX(numero) FROM motor_versao WHERE projeto_id = p.id)");
        sql.append(" AND (c.busca || ' ' || v.busca || ' ' || m.busca) LIKE ?".repeat(palavras.size()));
        if (filtro.status() != null) {
            sql.append(" AND p.status = ?");
        }
        if (!filtro.incluirArquivados() && filtro.status() != StatusProjeto.ARQUIVADO) {
            sql.append(" AND p.status <> 'ARQUIVADO'");
        }
        sql.append(" ORDER BY p.atualizado_em DESC, p.id DESC LIMIT ?");
        List<ResumoProjeto> projetos = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(sql.toString())) {
            int posicao = 1;
            for (String palavra : palavras) {
                consulta.setString(posicao++, "%" + palavra + "%");
            }
            if (filtro.status() != null) {
                consulta.setString(posicao++, filtro.status().name());
            }
            consulta.setInt(posicao, limite);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    projetos.add(resumo(linha));
                }
            }
        }
        return projetos;
    }

    private static ResumoProjeto resumo(ResultSet linha) throws SQLException {
        TipoVeiculo tipo = Sql.constante(linha, "tipo", TipoVeiculo.class);
        String veiculo = String.join(" ", List.of(linha.getString("marca"), linha.getString("modelo"),
                linha.getString("versao")).stream().filter(texto -> !texto.isBlank()).toList());
        String identificacao = tipo != null && tipo.nautico() ? linha.getString("nome_embarcacao")
                : linha.getString("placa");
        return new ResumoProjeto(linha.getLong("id"), Sql.constante(linha, "status", StatusProjeto.class),
                linha.getString("nome"), tipo, veiculo, identificacao, linha.getString("descricao"),
                linha.getInt("cilindros"), Sql.numero(linha, "cilindrada_cc"),
                Sql.constante(linha, "aspiracao", Aspiracao.class), linha.getInt("numero"),
                Sql.dataHora(linha, "atualizado_em"));
    }

    private static StatusProjeto voltaDoArquivo(ProjetoSalvo projeto) {
        return projeto.statusAntesDeArquivar() == null ? StatusProjeto.RASCUNHO : projeto.statusAntesDeArquivar();
    }

    private ProjetoSalvo exigir(Connection conexao, long id) throws SQLException {
        return buscar(conexao, id).orElseThrow(
                () -> new OperacaoRecusadaException("Projeto #" + id + " não encontrado."));
    }

    private static void registrarStatus(Connection conexao, long id, StatusProjeto status, LocalDateTime quando)
            throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO projeto_status (projeto_id, status, data_hora) VALUES (?, ?, ?)")) {
            insercao.setLong(1, id);
            insercao.setString(2, status.name());
            insercao.setString(3, Sql.texto(quando));
            insercao.executeUpdate();
        }
    }

    private static ProjetoSalvo ler(ResultSet linha) throws SQLException {
        return new ProjetoSalvo(linha.getLong("id"), linha.getLong("veiculo_id"),
                Sql.constante(linha, "status", StatusProjeto.class),
                Sql.constante(linha, "status_antes_de_arquivar", StatusProjeto.class),
                Json.ler(linha.getString("meta_json"), Meta.class), linha.getString("observacoes"),
                Sql.dataHora(linha, "criado_em"), Sql.dataHora(linha, "atualizado_em"));
    }
}
