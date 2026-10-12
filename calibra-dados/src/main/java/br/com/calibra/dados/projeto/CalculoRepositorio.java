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

import com.fasterxml.jackson.core.type.TypeReference;

import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.json.Json;

/**
 * Cálculos salvos nos projetos (regra 2.5) e as justificativas dos vermelhos (regra 2.4). O banco recusa
 * alterar ou apagar: recalcular cria outro cálculo, e o antigo fica no histórico.
 */
public final class CalculoRepositorio {

    private static final TypeReference<Resultado<Dimensionamento>> TIPO_RESULTADO = new TypeReference<>() {
    };

    private final Clock relogio;

    public CalculoRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    public long salvar(Connection conexao, long projetoId, long versaoMotorId, EntradaDimensionamento entrada,
            Resultado<Dimensionamento> resultado, List<Justificativa> justificativas, String versaoApp)
            throws SQLException {
        LocalDateTime agora = Sql.agora(relogio);
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO calculo (projeto_id, motor_versao_id, entrada_json, resultado_json, versao_nucleo, "
                        + "versao_app, vermelhos, amarelos, criado_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insercao.setLong(1, projetoId);
            insercao.setLong(2, versaoMotorId);
            insercao.setString(3, Json.escrever(entrada));
            insercao.setString(4, Json.escrever(resultado));
            insercao.setString(5, resultado.versaoNucleo());
            insercao.setString(6, versaoApp);
            insercao.setInt(7, contar(resultado, Cor.VERMELHO));
            insercao.setInt(8, contar(resultado, Cor.AMARELO));
            insercao.setString(9, Sql.texto(agora));
            insercao.executeUpdate();
        }
        long id = Sql.ultimoId(conexao);
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO calculo_justificativa (calculo_id, indicador, texto, data_hora) VALUES (?, ?, ?, ?)")) {
            for (Justificativa justificativa : justificativas) {
                insercao.setLong(1, id);
                insercao.setString(2, justificativa.indicador().name());
                insercao.setString(3, justificativa.texto());
                insercao.setString(4, Sql.texto(justificativa.dataHora()));
                insercao.executeUpdate();
            }
        }
        return id;
    }

    /** Histórico de cálculos do projeto, do mais novo para o mais antigo. */
    public List<ResumoCalculo> doProjeto(Connection conexao, long projetoId) throws SQLException {
        List<ResumoCalculo> calculos = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT c.id, c.motor_versao_id, m.numero, c.versao_nucleo, c.vermelhos, c.amarelos, c.criado_em "
                        + "FROM calculo c JOIN motor_versao m ON m.id = c.motor_versao_id "
                        + "WHERE c.projeto_id = ? ORDER BY c.id DESC")) {
            consulta.setLong(1, projetoId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    calculos.add(new ResumoCalculo(linha.getLong("id"), linha.getLong("motor_versao_id"),
                            linha.getInt("numero"), linha.getString("versao_nucleo"), linha.getInt("vermelhos"),
                            linha.getInt("amarelos"), Sql.dataHora(linha, "criado_em")));
                }
            }
        }
        return calculos;
    }

    /** O cálculo inteiro: entrada, resultado com a memória e justificativas. */
    public Optional<CalculoSalvo> buscar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT c.id, c.projeto_id, c.motor_versao_id, m.numero, c.entrada_json, c.resultado_json, "
                        + "c.versao_nucleo, c.versao_app, c.criado_em "
                        + "FROM calculo c JOIN motor_versao m ON m.id = c.motor_versao_id WHERE c.id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                if (!linha.next()) {
                    return Optional.empty();
                }
                return Optional.of(new CalculoSalvo(linha.getLong("id"), linha.getLong("projeto_id"),
                        linha.getLong("motor_versao_id"), linha.getInt("numero"),
                        Json.ler(linha.getString("entrada_json"), EntradaDimensionamento.class),
                        Json.ler(linha.getString("resultado_json"), TIPO_RESULTADO),
                        justificativas(conexao, id), linha.getString("versao_nucleo"),
                        linha.getString("versao_app"), Sql.dataHora(linha, "criado_em")));
            }
        }
    }

    /** O cálculo salvo mais recente do projeto. */
    public Optional<CalculoSalvo> ultimo(Connection conexao, long projetoId) throws SQLException {
        long id;
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT COALESCE(MAX(id), 0) FROM calculo WHERE projeto_id = ?")) {
            consulta.setLong(1, projetoId);
            try (ResultSet linha = consulta.executeQuery()) {
                linha.next();
                id = linha.getLong(1);
            }
        }
        return id == 0 ? Optional.empty() : buscar(conexao, id);
    }

    private static List<Justificativa> justificativas(Connection conexao, long calculoId) throws SQLException {
        List<Justificativa> justificativas = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT indicador, texto, data_hora FROM calculo_justificativa WHERE calculo_id = ? ORDER BY id")) {
            consulta.setLong(1, calculoId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    justificativas.add(new Justificativa(Sql.constante(linha, "indicador", Indicador.class),
                            linha.getString("texto"), Sql.dataHora(linha, "data_hora")));
                }
            }
        }
        return justificativas;
    }

    private static int contar(Resultado<Dimensionamento> resultado, Cor cor) {
        return (int) resultado.valor().semaforo().stream().filter(avaliacao -> avaliacao.cor() == cor).count();
    }
}
