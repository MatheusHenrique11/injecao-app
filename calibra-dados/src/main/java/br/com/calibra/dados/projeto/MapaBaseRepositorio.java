package br.com.calibra.dados.projeto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;

import br.com.calibra.core.mapa.AlertaMapa;
import br.com.calibra.core.mapa.EntradaMapaBase;
import br.com.calibra.core.mapa.JustificativaMapa;
import br.com.calibra.core.mapa.MapaBase;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.dados.Sql;
import br.com.calibra.dados.json.Json;

/**
 * Os mapas base salvos nos projetos (fase 4; decisão 296), as peças que usaram e as justificativas dos vermelhos. O
 * banco recusa alterar ou apagar: gerar de novo cria a versão seguinte, e as antigas ficam no histórico.
 */
public final class MapaBaseRepositorio {

    private static final TypeReference<Resultado<MapaBase>> TIPO_RESULTADO = new TypeReference<>() {
    };

    private final Clock relogio;

    public MapaBaseRepositorio(Clock relogio) {
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    /**
     * Grava a versão seguinte do mapa base do projeto.
     *
     * @param pecas as peças do catálogo usadas, cada uma na versão daquele momento (regra 2.5)
     * @return o id do mapa salvo
     */
    public long salvar(Connection conexao, long projetoId, long calculoId, long versaoMotorId, EntradaMapaBase entrada,
            Resultado<MapaBase> resultado, List<JustificativaMapa> justificativas, List<PecaUsada> pecas,
            String versaoApp) throws SQLException {
        int numero;
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT COALESCE(MAX(numero), 0) + 1 FROM mapa_base WHERE projeto_id = ?")) {
            consulta.setLong(1, projetoId);
            try (ResultSet linha = consulta.executeQuery()) {
                linha.next();
                numero = linha.getInt(1);
            }
        }
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO mapa_base (projeto_id, numero, calculo_id, motor_versao_id, entrada_json, resultado_json, "
                        + "versao_nucleo, versao_app, vermelhos, amarelos, criado_em) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insercao.setLong(1, projetoId);
            insercao.setInt(2, numero);
            insercao.setLong(3, calculoId);
            insercao.setLong(4, versaoMotorId);
            insercao.setString(5, Json.escrever(entrada));
            insercao.setString(6, Json.escrever(resultado));
            insercao.setString(7, resultado.versaoNucleo());
            insercao.setString(8, versaoApp);
            insercao.setInt(9, contar(resultado, Cor.VERMELHO));
            insercao.setInt(10, contar(resultado, Cor.AMARELO));
            insercao.setString(11, Sql.texto(Sql.agora(relogio)));
            insercao.executeUpdate();
        }
        long id = Sql.ultimoId(conexao);
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO mapa_base_justificativa (mapa_base_id, alerta, texto, data_hora) VALUES (?, ?, ?, ?)")) {
            for (JustificativaMapa justificativa : justificativas) {
                insercao.setLong(1, id);
                insercao.setString(2, justificativa.alerta().name());
                insercao.setString(3, justificativa.texto());
                insercao.setString(4, Sql.texto(justificativa.dataHora()));
                insercao.executeUpdate();
            }
        }
        try (PreparedStatement insercao = conexao.prepareStatement(
                "INSERT INTO mapa_base_peca (mapa_base_id, papel, item_id, versao) VALUES (?, ?, ?, ?)")) {
            for (PecaUsada peca : pecas) {
                insercao.setLong(1, id);
                insercao.setString(2, peca.papel().name());
                insercao.setLong(3, peca.itemId());
                insercao.setInt(4, peca.versao());
                insercao.executeUpdate();
            }
        }
        return id;
    }

    /** Os mapas do projeto, do mais novo para o mais antigo, sem as tabelas. */
    public List<ResumoMapaBase> doProjeto(Connection conexao, long projetoId) throws SQLException {
        Map<Long, List<PecaUsada>> pecas = new HashMap<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT p.mapa_base_id, p.papel, p.item_id, p.versao FROM mapa_base_peca p "
                        + "JOIN mapa_base m ON m.id = p.mapa_base_id WHERE m.projeto_id = ? "
                        + "ORDER BY p.mapa_base_id, p.papel")) {
            consulta.setLong(1, projetoId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    pecas.computeIfAbsent(linha.getLong("mapa_base_id"), id -> new ArrayList<>()).add(lerPeca(linha));
                }
            }
        }
        List<ResumoMapaBase> mapas = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT b.id, b.numero, b.calculo_id, b.motor_versao_id, m.numero AS numero_motor, b.versao_nucleo, "
                        + "b.vermelhos, b.amarelos, b.criado_em FROM mapa_base b "
                        + "JOIN motor_versao m ON m.id = b.motor_versao_id WHERE b.projeto_id = ? ORDER BY b.id DESC")) {
            consulta.setLong(1, projetoId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    long id = linha.getLong("id");
                    mapas.add(new ResumoMapaBase(id, linha.getInt("numero"), linha.getLong("calculo_id"),
                            linha.getLong("motor_versao_id"), linha.getInt("numero_motor"),
                            linha.getString("versao_nucleo"), linha.getInt("vermelhos"), linha.getInt("amarelos"),
                            pecas.getOrDefault(id, List.of()), Sql.dataHora(linha, "criado_em")));
                }
            }
        }
        return mapas;
    }

    /** O mapa inteiro: entrada, resultado com as tabelas e a memória, justificativas e peças. */
    public Optional<MapaBaseSalvo> buscar(Connection conexao, long id) throws SQLException {
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT b.id, b.projeto_id, b.numero, b.calculo_id, b.motor_versao_id, m.numero AS numero_motor, "
                        + "b.entrada_json, b.resultado_json, b.versao_nucleo, b.versao_app, b.criado_em "
                        + "FROM mapa_base b JOIN motor_versao m ON m.id = b.motor_versao_id WHERE b.id = ?")) {
            consulta.setLong(1, id);
            try (ResultSet linha = consulta.executeQuery()) {
                if (!linha.next()) {
                    return Optional.empty();
                }
                return Optional.of(new MapaBaseSalvo(linha.getLong("id"), linha.getLong("projeto_id"),
                        linha.getInt("numero"), linha.getLong("calculo_id"), linha.getLong("motor_versao_id"),
                        linha.getInt("numero_motor"), Json.ler(linha.getString("entrada_json"), EntradaMapaBase.class),
                        Json.ler(linha.getString("resultado_json"), TIPO_RESULTADO), justificativas(conexao, id),
                        pecas(conexao, id), linha.getString("versao_nucleo"), linha.getString("versao_app"),
                        Sql.dataHora(linha, "criado_em")));
            }
        }
    }

    private static List<PecaUsada> pecas(Connection conexao, long mapaId) throws SQLException {
        List<PecaUsada> pecas = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT papel, item_id, versao FROM mapa_base_peca WHERE mapa_base_id = ? ORDER BY papel")) {
            consulta.setLong(1, mapaId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    pecas.add(lerPeca(linha));
                }
            }
        }
        return pecas;
    }

    private static List<JustificativaMapa> justificativas(Connection conexao, long mapaId) throws SQLException {
        List<JustificativaMapa> justificativas = new ArrayList<>();
        try (PreparedStatement consulta = conexao.prepareStatement(
                "SELECT alerta, texto, data_hora FROM mapa_base_justificativa WHERE mapa_base_id = ? ORDER BY id")) {
            consulta.setLong(1, mapaId);
            try (ResultSet linha = consulta.executeQuery()) {
                while (linha.next()) {
                    justificativas.add(new JustificativaMapa(Sql.constante(linha, "alerta", AlertaMapa.Codigo.class),
                            linha.getString("texto"), Sql.dataHora(linha, "data_hora")));
                }
            }
        }
        return justificativas;
    }

    private static PecaUsada lerPeca(ResultSet linha) throws SQLException {
        return new PecaUsada(Sql.constante(linha, "papel", PapelPeca.class), linha.getLong("item_id"),
                linha.getInt("versao"));
    }

    private static int contar(Resultado<MapaBase> resultado, Cor cor) {
        return (int) resultado.valor().alertas().stream().map(AlertaMapa::cor).filter(cor::equals).count();
    }
}
