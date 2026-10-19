package br.com.calibra.dados.cadastro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.cadastro.Tracao;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.core.util.TextoBusca;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.Sql;

/**
 * Veículos (SPEC 6.2) na tabela {@code veiculo}. Todo salvamento grava no histórico campo, valor antigo, valor
 * novo, data e hora. Veículo com projeto não é apagado, só arquivado.
 */
public final class VeiculoRepositorio {

    static final String ENTIDADE = "veiculo";

    /** Campos que identificam o dono: apagados ao anonimizar o cliente, também no histórico. */
    static final List<String> CAMPOS_PESSOAIS =
            List.of("Placa", "Chassi", "Nome da embarcação", "Número de inscrição", "Número do casco");

    private static final String COLUNAS = "id, cliente_id, tipo, marca, modelo, versao, ano_fabricacao, ano_modelo, "
            + "placa, chassi, nome_embarcacao, numero_inscricao, numero_casco, uso_acumulado, cambio, "
            + "cambio_controlado, tracao, peso_kg, cor, observacoes, arquivado, cambio_item_id, criado_em, atualizado_em";

    private final Clock relogio;

    public VeiculoRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /** @param cambioItemId câmbio do catálogo (SPEC 6.2, bloco 2.3); null se não escolhido */
    public long inserir(Connection conexao, long clienteId, Veiculo veiculo, Long cambioItemId) throws SQLException {
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO veiculo (tipo, marca, modelo, versao, ano_fabricacao, ano_modelo, placa, chassi, "
                        + "nome_embarcacao, numero_inscricao, numero_casco, uso_acumulado, cambio, cambio_controlado, "
                        + "tracao, peso_kg, cor, observacoes, busca, cliente_id, cambio_item_id, criado_em, atualizado_em) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            preencher(insercao, veiculo);
            insercao.setLong(20, clienteId);
            Sql.longo(insercao, 21, cambioItemId);
            insercao.setString(22, Sql.texto(agora));
            insercao.setString(23, Sql.texto(agora));
            insercao.executeUpdate();
        }
        long id = Sql.ultimoId(conexao);
        Auditoria.registrar(conexao, ENTIDADE, id, "Cadastro", null, "criado", agora);
        return id;
    }

    /**
     * Grava as mudanças e uma linha no histórico para cada campo que mudou (SPEC 6.2). Com o dono anonimizado (LGPD),
     * a placa, o chassi e a identificação da embarcação não voltam (uma tela aberta antes da anonimização ainda os
     * mostraria).
     */
    public void atualizar(Connection conexao, long id, Veiculo novo, Long cambioItemId) throws SQLException {
        VeiculoSalvo atual = exigir(conexao, id);
        if (temIdentificacao(novo) && donoAnonimizado(conexao, atual.clienteId())) {
            throw new OperacaoRecusadaException("O dono deste veículo foi anonimizado (LGPD): a placa, o chassi e a "
                    + "identificação da embarcação não voltam. Deixe esses campos em branco para salvar.");
        }
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE veiculo SET tipo = ?, marca = ?, modelo = ?, versao = ?, ano_fabricacao = ?, ano_modelo = ?, "
                        + "placa = ?, chassi = ?, nome_embarcacao = ?, numero_inscricao = ?, numero_casco = ?, "
                        + "uso_acumulado = ?, cambio = ?, cambio_controlado = ?, tracao = ?, peso_kg = ?, cor = ?, "
                        + "observacoes = ?, busca = ?, cambio_item_id = ?, atualizado_em = ? WHERE id = ?")) {
            preencher(atualizacao, novo);
            Sql.longo(atualizacao, 20, cambioItemId);
            atualizacao.setString(21, Sql.texto(agora));
            atualizacao.setLong(22, id);
            atualizacao.executeUpdate();
        }
        Auditoria.registrarMudancas(conexao, ENTIDADE, id, campos(atual.veiculo()), campos(novo), agora);
        if (!Objects.equals(atual.cambioItemId(), cambioItemId)) {
            Auditoria.registrar(conexao, ENTIDADE, id, "Câmbio do catálogo", nomeDoItem(conexao, atual.cambioItemId()),
                    nomeDoItem(conexao, cambioItemId), agora);
        }
    }

    public Optional<VeiculoSalvo> buscar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT " + COLUNAS + " FROM veiculo WHERE id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? Optional.of(ler(linha)) : Optional.empty();
            }
        }
    }

    /** Veículos do cliente, do mais novo para o mais antigo. */
    public List<VeiculoSalvo> doCliente(Connection conexao, long clienteId, boolean incluirArquivados)
            throws SQLException {
        List<VeiculoSalvo> veiculos = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT " + COLUNAS + " FROM veiculo "
                + "WHERE cliente_id = ?" + (incluirArquivados ? "" : " AND arquivado = 0") + " ORDER BY id DESC")) {
            consulta.setLong(1, clienteId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    veiculos.add(ler(linha));
                }
            }
        }
        return veiculos;
    }

    public List<EventoAuditoria> historico(Connection conexao, long id) throws SQLException {
        return Auditoria.historico(conexao, ENTIDADE, id);
    }

    /** Arquiva ou reativa; o veículo arquivado sai das listas, mas os projetos dele continuam. */
    public void arquivar(Connection conexao, long id, boolean arquivado) throws SQLException {
        VeiculoSalvo atual = exigir(conexao, id);
        if (atual.arquivado() == arquivado) {
            return;
        }
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement atualizacao = conexao.prepareStatement(
                "UPDATE veiculo SET arquivado = ?, atualizado_em = ? WHERE id = ?")) {
            atualizacao.setInt(1, arquivado ? 1 : 0);
            atualizacao.setString(2, Sql.texto(agora));
            atualizacao.setLong(3, id);
            atualizacao.executeUpdate();
        }
        Auditoria.registrar(conexao, ENTIDADE, id, "Arquivado", arquivado ? "não" : "sim", arquivado ? "sim" : "não",
                agora);
    }

    /** Apaga um veículo sem projeto (cadastro feito por engano). Com projeto, só arquivar (SPEC 6.2). */
    public void apagar(Connection conexao, long id) throws SQLException {
        exigir(conexao, id);
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT 1 FROM projeto WHERE veiculo_id = ? LIMIT 1")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                if (linha.next()) {
                    throw new OperacaoRecusadaException(
                            "Veículo com projeto não pode ser apagado, só arquivado (SPEC 6.2).");
                }
            }
        }
        try (PreparedStatement remocao = conexao.prepareStatement("DELETE FROM veiculo WHERE id = ?")) {
            remocao.setLong(1, id);
            remocao.executeUpdate();
        }
        Auditoria.apagarHistorico(conexao, ENTIDADE, id);
    }

    /** Anonimização do cliente: apaga placa, chassi e identificação da embarcação dos veículos dele. */
    private static boolean temIdentificacao(Veiculo veiculo) {
        return !veiculo.placa().isEmpty() || !veiculo.chassi().isEmpty() || !veiculo.nomeEmbarcacao().isEmpty()
                || !veiculo.numeroInscricao().isEmpty() || !veiculo.numeroCasco().isEmpty();
    }

    private static boolean donoAnonimizado(Connection conexao, long clienteId) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT anonimizado FROM cliente WHERE id = ?")) {
            consulta.setLong(1, clienteId);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() && linha.getInt(1) == 1;
            }
        }
    }

    static void anonimizarDoCliente(Connection conexao, long clienteId, LocalDateTime quando) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT id FROM veiculo WHERE cliente_id = ?")) {
            consulta.setLong(1, clienteId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    ids.add(linha.getLong(1));
                }
            }
        }
        for (long id : ids) {
            try (PreparedStatement atualizacao = conexao.prepareStatement(
                    "UPDATE veiculo SET placa = '', chassi = '', nome_embarcacao = '', numero_inscricao = '', "
                            + "numero_casco = '', busca = ?, atualizado_em = ? WHERE id = ?")) {
                atualizacao.setString(1, buscaSemIdentificacao(conexao, id));
                atualizacao.setString(2, Sql.texto(quando));
                atualizacao.setLong(3, id);
                atualizacao.executeUpdate();
            }
            Auditoria.apagarValores(conexao, ENTIDADE, id, CAMPOS_PESSOAIS);
            Auditoria.registrar(conexao, ENTIDADE, id, "Anonimização", null,
                    "placa, chassi e identificação da embarcação apagados (LGPD)", quando);
        }
    }

    /** Os campos como aparecem no histórico (rótulo → valor). */
    static Map<String, String> campos(Veiculo v) {
        String unidade = v.tipo() == null ? "" : " " + v.tipo().unidadeUso();
        Map<String, String> campos = new LinkedHashMap<>();
        campos.put("Tipo", v.tipo() == null ? "" : v.tipo().rotulo());
        campos.put("Marca", v.marca());
        campos.put("Modelo", v.modelo());
        campos.put("Versão", v.versao());
        campos.put("Ano de fabricação", v.anoFabricacao() == null ? "" : v.anoFabricacao().toString());
        campos.put("Ano do modelo", v.anoModelo() == null ? "" : v.anoModelo().toString());
        campos.put("Placa", v.placa());
        campos.put("Chassi", v.chassi());
        campos.put("Nome da embarcação", v.nomeEmbarcacao());
        campos.put("Número de inscrição", v.numeroInscricao());
        campos.put("Número do casco", v.numeroCasco());
        campos.put("Uso acumulado", v.usoAcumulado() == null ? "" : NumeroBr.formatar(v.usoAcumulado(), 0, 1) + unidade);
        campos.put("Câmbio", v.cambio() == null ? "" : v.cambio().rotulo());
        campos.put("Câmbio controlado pela injeção", v.cambioControladoPelaInjecao() ? "sim" : "não");
        campos.put("Tração", v.tracao() == null ? "" : v.tracao().rotulo());
        campos.put("Peso (kg)", v.pesoKg() == null ? "" : NumeroBr.formatar(v.pesoKg(), 0, 1));
        campos.put("Cor", v.cor());
        campos.put("Observações", v.observacoes());
        return campos;
    }

    private VeiculoSalvo exigir(Connection conexao, long id) throws SQLException {
        return buscar(conexao, id).orElseThrow(
                () -> new OperacaoRecusadaException("Veículo #" + id + " não encontrado."));
    }

    private static String busca(Veiculo v) {
        return TextoBusca.juntar(v.tipo() == null ? "" : v.tipo().rotulo(), v.marca(), v.modelo(), v.versao(),
                v.placa(), v.chassi(), v.nomeEmbarcacao(), v.numeroInscricao(), v.numeroCasco(), v.cor());
    }

    private static String buscaSemIdentificacao(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT tipo, marca, modelo, versao, cor FROM veiculo WHERE id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                linha.next();
                TipoVeiculo tipo = Sql.constante(linha, "tipo", TipoVeiculo.class);
                return TextoBusca.juntar(tipo == null ? "" : tipo.rotulo(), linha.getString("marca"),
                        linha.getString("modelo"), linha.getString("versao"), linha.getString("cor"));
            }
        }
    }

    private static void preencher(PreparedStatement comando, Veiculo v) throws SQLException {
        Sql.nome(comando, 1, v.tipo());
        comando.setString(2, v.marca());
        comando.setString(3, v.modelo());
        comando.setString(4, v.versao());
        Sql.inteiro(comando, 5, v.anoFabricacao());
        Sql.inteiro(comando, 6, v.anoModelo());
        comando.setString(7, v.placa());
        comando.setString(8, v.chassi());
        comando.setString(9, v.nomeEmbarcacao());
        comando.setString(10, v.numeroInscricao());
        comando.setString(11, v.numeroCasco());
        Sql.numero(comando, 12, v.usoAcumulado());
        Sql.nome(comando, 13, v.cambio());
        comando.setInt(14, v.cambioControladoPelaInjecao() ? 1 : 0);
        Sql.nome(comando, 15, v.tracao());
        Sql.numero(comando, 16, v.pesoKg());
        comando.setString(17, v.cor());
        comando.setString(18, v.observacoes());
        comando.setString(19, busca(v));
    }

    private static VeiculoSalvo ler(ResultSet linha) throws SQLException {
        Veiculo veiculo = new Veiculo(
                Sql.constante(linha, "tipo", TipoVeiculo.class),
                linha.getString("marca"),
                linha.getString("modelo"),
                linha.getString("versao"),
                Sql.inteiro(linha, "ano_fabricacao"),
                Sql.inteiro(linha, "ano_modelo"),
                linha.getString("placa"),
                linha.getString("chassi"),
                linha.getString("nome_embarcacao"),
                linha.getString("numero_inscricao"),
                linha.getString("numero_casco"),
                Sql.numero(linha, "uso_acumulado"),
                Sql.constante(linha, "cambio", TipoCambio.class),
                linha.getInt("cambio_controlado") == 1,
                Sql.constante(linha, "tracao", Tracao.class),
                Sql.numero(linha, "peso_kg"),
                linha.getString("cor"),
                linha.getString("observacoes"));
        return new VeiculoSalvo(linha.getLong("id"), linha.getLong("cliente_id"), veiculo,
                Sql.longo(linha, "cambio_item_id"), linha.getInt("arquivado") == 1, Sql.dataHora(linha, "criado_em"),
                Sql.dataHora(linha, "atualizado_em"));
    }

    /** Nome do item de catálogo, para o histórico; vazio se não houver. */
    private static String nomeDoItem(Connection conexao, Long itemId) throws SQLException {
        if (itemId == null) {
            return "";
        }
        try (PreparedStatement consulta = conexao.prepareStatement("SELECT nome FROM catalogo_item WHERE id = ?")) {
            consulta.setLong(1, itemId);
            try (ResultSet linha = consulta.executeQuery()) {
                return linha.next() ? linha.getString(1) : "#" + itemId;
            }
        }
    }
}
