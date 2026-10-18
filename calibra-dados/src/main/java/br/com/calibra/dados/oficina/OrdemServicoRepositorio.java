package br.com.calibra.dados.oficina;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.NumeroOs;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.core.util.TextoBusca;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.json.Json;

/**
 * Ordens de serviço (SPEC 6.5) na tabela {@code ordem_servico}, com o histórico de status em {@code os_status}. O
 * banco recusa apagar OS e mudar OS entregue ou cancelada; aqui, os passos seguem {@link StatusOs#podeIrPara}.
 */
public final class OrdemServicoRepositorio {

    private static final String COLUNAS = "id, ano, sequencial, cliente_id, veiculo_id, projeto_id, status, "
            + "servico_pedido, mecanico_id, previsao_entrega, entrada_em, uso_entrada, combustivel_entrada, "
            + "vistoria_json, pronta_em, entregue_em, uso_saida, garantia_dias, garantia_ate, motivo_cancelamento, "
            + "observacoes, criado_em, atualizado_em";
    /** As linhas das listas de OS (a tela Oficina, o Painel e a ficha do veículo), com o cliente e o veículo. */
    private static final String SELECT_RESUMO = "SELECT o.id, o.ano, o.sequencial, o.status, o.servico_pedido, "
            + "o.projeto_id, o.entrada_em, o.previsao_entrega, o.atualizado_em, c.nome, v.tipo, v.marca, v.modelo, "
            + "v.versao, v.placa, v.nome_embarcacao FROM ordem_servico o "
            + "JOIN cliente c ON c.id = o.cliente_id "
            + "JOIN veiculo v ON v.id = o.veiculo_id";

    private final Clock relogio;

    public OrdemServicoRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /** O próximo número da sequência do ano (a sequência recomeça a cada ano). */
    public int proximoSequencial(Connection conexao, int ano) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT COALESCE(MAX(sequencial), 0) + 1 FROM ordem_servico WHERE ano = ?")) {
            consulta.setInt(1, ano);
            try (ResultSet linha = consulta.executeQuery()) {
                linha.next();
                return linha.getInt(1);
            }
        }
    }

    /** OS nova, ABERTA e sem a entrada do veículo. */
    public long inserir(Connection conexao, NumeroOs numero, DadosAbertura dados) throws SQLException {
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO ordem_servico (ano, sequencial, cliente_id, veiculo_id, projeto_id, status, servico_pedido, "
                        + "mecanico_id, previsao_entrega, observacoes, criado_em, atualizado_em) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insercao.setInt(1, numero.ano());
            insercao.setInt(2, numero.sequencial());
            insercao.setLong(3, dados.clienteId());
            insercao.setLong(4, dados.veiculoId());
            Sql.longo(insercao, 5, dados.projetoId());
            insercao.setString(6, StatusOs.ABERTA.name());
            insercao.setString(7, dados.servicoPedido());
            Sql.longo(insercao, 8, dados.mecanicoId());
            insercao.setString(9, Sql.texto(dados.previsaoEntrega()));
            insercao.setString(10, dados.observacoes());
            insercao.setString(11, Sql.texto(agora));
            insercao.setString(12, Sql.texto(agora));
            insercao.executeUpdate();
        }
        long id = Sql.ultimoId(conexao);
        registrarStatus(conexao, id, StatusOs.ABERTA, agora, "");
        return id;
    }

    /** Muda o que foi informado na abertura (o serviço pedido, o mecânico, a previsão e as observações). */
    public void atualizarAbertura(Connection conexao, long id, String servicoPedido, Long mecanicoId,
            LocalDate previsaoEntrega, String observacoes) throws SQLException {
        exigirEmAndamento(conexao, id);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE ordem_servico SET servico_pedido = ?, mecanico_id = ?, previsao_entrega = ?, observacoes = ?, "
                        + "atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, servicoPedido == null ? "" : servicoPedido.strip());
            Sql.longo(atualizacao, 2, mecanicoId);
            atualizacao.setString(3, Sql.texto(previsaoEntrega));
            atualizacao.setString(4, observacoes == null ? "" : observacoes.strip());
            atualizacao.setString(5, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(6, id);
            atualizacao.executeUpdate();
        }
    }

    /** Grava (ou corrige) a entrada do veículo. */
    public void registrarEntrada(Connection conexao, long id, EntradaVeiculo entrada) throws SQLException {
        exigirEmAndamento(conexao, id);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE ordem_servico SET entrada_em = ?, uso_entrada = ?, combustivel_entrada = ?, vistoria_json = ?, "
                        + "atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, Sql.texto(entrada.quando().withNano(0)));
            Sql.numero(atualizacao, 2, entrada.uso());
            Sql.nome(atualizacao, 3, entrada.combustivel());
            atualizacao.setString(4, Json.escrever(entrada.vistoria()));
            atualizacao.setString(5, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(6, id);
            atualizacao.executeUpdate();
        }
    }

    /**
     * Um passo da OS, permitido por {@link StatusOs#podeIrPara}. Entregar e cancelar têm métodos próprios, porque
     * gravam mais coisas (a entrega e a garantia; o motivo).
     */
    public void mudarStatus(Connection conexao, long id, StatusOs novo, String observacao) throws SQLException {
        if (novo == StatusOs.ENTREGUE || novo == StatusOs.CANCELADA) {
            throw new IllegalArgumentException("Use entregar() ou cancelar() para " + novo);
        }
        OrdemServicoSalva atual = exigirPasso(conexao, id, novo);
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE ordem_servico SET status = ?, pronta_em = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, novo.name());
            atualizacao.setString(2, Sql.texto(novo == StatusOs.PRONTA ? agora : atual.prontaEm()));
            atualizacao.setString(3, Sql.texto(agora));
            atualizacao.setLong(4, id);
            atualizacao.executeUpdate();
        }
        registrarStatus(conexao, id, novo, agora, observacao);
    }

    /** Entrega a OS pronta: grava a entrega, a garantia e o último dia dela. Depois disso a OS não muda mais. */
    public void entregar(Connection conexao, long id, Entrega entrega) throws SQLException {
        exigirPasso(conexao, id, StatusOs.ENTREGUE);
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE ordem_servico SET status = ?, entregue_em = ?, uso_saida = ?, garantia_dias = ?, "
                        + "garantia_ate = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, StatusOs.ENTREGUE.name());
            atualizacao.setString(2, Sql.texto(entrega.quando().withNano(0)));
            Sql.numero(atualizacao, 3, entrega.usoSaida());
            atualizacao.setInt(4, entrega.garantia().dias());
            atualizacao.setString(5, Sql.texto(entrega.garantiaVence()));
            atualizacao.setString(6, Sql.texto(agora));
            atualizacao.setLong(7, id);
            atualizacao.executeUpdate();
        }
        registrarStatus(conexao, id, StatusOs.ENTREGUE, agora, "");
    }

    /** Cancela a OS (a qualquer momento antes da entrega), com o motivo. Depois disso a OS não muda mais. */
    public void cancelar(Connection conexao, long id, String motivo) throws SQLException {
        exigirPasso(conexao, id, StatusOs.CANCELADA);
        String texto = motivo == null ? "" : motivo.strip();
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE ordem_servico SET status = ?, motivo_cancelamento = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setString(1, StatusOs.CANCELADA.name());
            atualizacao.setString(2, texto);
            atualizacao.setString(3, Sql.texto(agora));
            atualizacao.setLong(4, id);
            atualizacao.executeUpdate();
        }
        registrarStatus(conexao, id, StatusOs.CANCELADA, agora, texto);
    }

    /** Liga a OS a um projeto de preparação (ex.: criado a partir dela). Só OS em andamento e ainda sem projeto. */
    public void ligarProjeto(Connection conexao, long id, long projetoId) throws SQLException {
        OrdemServicoSalva atual = exigirEmAndamento(conexao, id);
        if (atual.projetoId() != null) {
            throw new OperacaoRecusadaException("A OS " + atual.numero().texto() + " já está ligada ao projeto #"
                    + atual.projetoId() + ".");
        }
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE ordem_servico SET projeto_id = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setLong(1, projetoId);
            atualizacao.setString(2, Sql.texto(Sql.agora(relogio)));
            atualizacao.setLong(3, id);
            atualizacao.executeUpdate();
        }
    }

    public Optional<OrdemServicoSalva> buscar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM ordem_servico WHERE id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha)) : Optional.empty();
            }
        }
    }

    /** As OS do projeto, da mais nova para a mais antiga. */
    public List<OrdemServicoSalva> doProjeto(Connection conexao, long projetoId) throws SQLException {
        return lista(conexao, "SELECT " + COLUNAS + " FROM ordem_servico WHERE projeto_id = ? ORDER BY id DESC",
                projetoId);
    }

    /** A OS atual do projeto: a mais nova que não foi entregue nem cancelada. */
    public Optional<OrdemServicoSalva> atualDoProjeto(Connection conexao, long projetoId) throws SQLException {
        return lista(conexao, "SELECT " + COLUNAS + " FROM ordem_servico WHERE projeto_id = ? "
                + "AND status NOT IN ('ENTREGUE', 'CANCELADA') ORDER BY id DESC LIMIT 1", projetoId).stream()
                .findFirst();
    }

    /** As OS do veículo, da mais nova para a mais antiga (a ficha do veículo). */
    public List<OrdemServicoSalva> doVeiculo(Connection conexao, long veiculoId) throws SQLException {
        return lista(conexao, "SELECT " + COLUNAS + " FROM ordem_servico WHERE veiculo_id = ? ORDER BY id DESC",
                veiculoId);
    }

    /** As OS do veículo, da mais nova para a mais antiga, todas (a ficha do veículo, bloco 3.3). */
    public List<ResumoOs> resumosDoVeiculo(Connection conexao, long veiculoId) throws SQLException {
        List<ResumoOs> resultado = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(SELECT_RESUMO
                + " WHERE o.veiculo_id = ? ORDER BY o.id DESC")) {
            consulta.setLong(1, veiculoId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    resultado.add(resumo(linha));
                }
            }
        }
        return resultado;
    }

    /** A lista da tela Oficina: as que mudaram por último primeiro. */
    public List<ResumoOs> pesquisar(Connection conexao, FiltroOs filtro, int limite) throws SQLException {
        List<String> palavras = TextoBusca.palavras(filtro.termo());
        StringBuilder sql = new StringBuilder(SELECT_RESUMO + " WHERE 1 = 1");
        sql.append(" AND (c.busca || ' ' || v.busca || ' ' || printf('os%d%04d', o.ano, o.sequencial)) LIKE ?"
                .repeat(palavras.size()));
        List<StatusOs> status = filtro.status().stream().sorted().toList();
        if (!status.isEmpty()) {
            sql.append(" AND o.status IN (").append(String.join(", ", status.stream().map(s -> "?").toList()))
                    .append(")");
        } else if (!filtro.incluirFinalizadas()) {
            sql.append(" AND o.status NOT IN ('ENTREGUE', 'CANCELADA')");
        }
        sql.append(" ORDER BY o.atualizado_em DESC, o.id DESC LIMIT ?");
        List<ResumoOs> resultado = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(sql.toString())) {
            int posicao = 1;
            for (String palavra : palavras) {
                consulta.setString(posicao++, "%" + palavra + "%");
            }
            for (StatusOs umStatus : status) {
                consulta.setString(posicao++, umStatus.name());
            }
            consulta.setInt(posicao, limite);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    resultado.add(resumo(linha));
                }
            }
        }
        return resultado;
    }

    /** Quantas OS há em cada status (os cartões do Painel); um status sem nenhuma OS fica fora do mapa. */
    public Map<StatusOs, Integer> contarPorStatus(Connection conexao) throws SQLException {
        Map<StatusOs, Integer> contagem = new EnumMap<>(StatusOs.class);
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT status, COUNT(*) AS quantas FROM ordem_servico GROUP BY status");
                ResultSet linha = consulta.executeQuery()) {
            while (linha.next()) {
                contagem.put(Sql.constante(linha, "status", StatusOs.class), linha.getInt("quantas"));
            }
        }
        return contagem;
    }

    /** O histórico de status, do mais antigo para o mais novo. */
    public List<MudancaStatusOs> historico(Connection conexao, long id) throws SQLException {
        List<MudancaStatusOs> historico = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT status, data_hora, observacao FROM os_status WHERE os_id = ? ORDER BY id")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    historico.add(new MudancaStatusOs(Sql.constante(linha, "status", StatusOs.class),
                            Sql.dataHora(linha, "data_hora"), linha.getString("observacao")));
                }
            }
        }
        return historico;
    }

    private OrdemServicoSalva exigir(Connection conexao, long id) throws SQLException {
        return buscar(conexao, id).orElseThrow(() -> new OperacaoRecusadaException("OS #" + id + " não encontrada."));
    }

    private OrdemServicoSalva exigirEmAndamento(Connection conexao, long id) throws SQLException {
        OrdemServicoSalva atual = exigir(conexao, id);
        if (atual.status().finalizada()) {
            throw new OperacaoRecusadaException("A OS " + atual.numero().texto() + " está \"" + atual.status().rotulo()
                    + "\" e não muda mais.");
        }
        return atual;
    }

    private OrdemServicoSalva exigirPasso(Connection conexao, long id, StatusOs novo) throws SQLException {
        OrdemServicoSalva atual = exigir(conexao, id);
        if (!atual.status().podeIrPara(novo)) {
            throw new OperacaoRecusadaException("A OS " + atual.numero().texto() + " está \"" + atual.status().rotulo()
                    + "\" e não pode passar para \"" + novo.rotulo() + "\".");
        }
        return atual;
    }

    private void registrarStatus(Connection conexao, long id, StatusOs status, LocalDateTime quando, String observacao)
            throws SQLException {
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO os_status (os_id, status, data_hora, observacao) VALUES (?, ?, ?, ?)")) {
            insercao.setLong(1, id);
            insercao.setString(2, status.name());
            insercao.setString(3, Sql.texto(quando));
            insercao.setString(4, observacao == null ? "" : observacao.strip());
            insercao.executeUpdate();
        }
    }

    private static List<OrdemServicoSalva> lista(Connection conexao, String sql, long parametro) throws SQLException {
        List<OrdemServicoSalva> lista = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(sql)) {
            consulta.setLong(1, parametro);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    lista.add(ler(linha));
                }
            }
        }
        return lista;
    }

    private static OrdemServicoSalva ler(ResultSet linha) throws SQLException {
        LocalDateTime entradaEm = Sql.dataHora(linha, "entrada_em");
        EntradaVeiculo entrada = entradaEm == null ? null : new EntradaVeiculo(entradaEm,
                Sql.numero(linha, "uso_entrada"), Sql.constante(linha, "combustivel_entrada", NivelCombustivel.class),
                Json.ler(linha.getString("vistoria_json"), Vistoria.class));
        LocalDateTime entregueEm = Sql.dataHora(linha, "entregue_em");
        Entrega entrega = entregueEm == null ? null : new Entrega(entregueEm, Sql.numero(linha, "uso_saida"),
                new Garantia(linha.getInt("garantia_dias")));
        return new OrdemServicoSalva(linha.getLong("id"), new NumeroOs(linha.getInt("ano"), linha.getInt("sequencial")),
                linha.getLong("cliente_id"), linha.getLong("veiculo_id"), Sql.longo(linha, "projeto_id"),
                Sql.constante(linha, "status", StatusOs.class), linha.getString("servico_pedido"),
                Sql.longo(linha, "mecanico_id"), Sql.data(linha, "previsao_entrega"), entrada,
                Sql.dataHora(linha, "pronta_em"), entrega, Sql.data(linha, "garantia_ate"),
                linha.getString("motivo_cancelamento"), linha.getString("observacoes"), Sql.dataHora(linha, "criado_em"),
                Sql.dataHora(linha, "atualizado_em"));
    }

    private static ResumoOs resumo(ResultSet linha) throws SQLException {
        TipoVeiculo tipo = Sql.constante(linha, "tipo", TipoVeiculo.class);
        String veiculo = String.join(" ", List.of(linha.getString("marca"), linha.getString("modelo"),
                linha.getString("versao")).stream().filter(texto -> !texto.isBlank()).toList());
        String identificacao = tipo != null && tipo.nautico() ? linha.getString("nome_embarcacao")
                : linha.getString("placa");
        return new ResumoOs(linha.getLong("id"), new NumeroOs(linha.getInt("ano"), linha.getInt("sequencial")),
                Sql.constante(linha, "status", StatusOs.class), linha.getString("nome"), veiculo, identificacao,
                linha.getString("servico_pedido"), Sql.longo(linha, "projeto_id"), Sql.dataHora(linha, "entrada_em"),
                Sql.data(linha, "previsao_entrega"), Sql.dataHora(linha, "atualizado_em"));
    }
}
