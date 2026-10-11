package br.com.calibra.dados.config;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.json.Json;

/**
 * Lê e grava as configurações (tabela {@code configuracao}, uma linha por {@link Chave}, valor em JSON).
 *
 * <p>Na leitura, o JSON salvo é mesclado com o valor padrão: um campo que não existia quando a
 * configuração foi salva (por exemplo, um limiar novo numa versão futura) vem com o valor padrão, em vez
 * de zero.
 */
public final class ConfiguracaoRepositorio {

    private final BancoDados banco;
    private final Clock relogio;

    public ConfiguracaoRepositorio(BancoDados banco, Clock relogio) {
        this.banco = Objects.requireNonNull(banco, "banco");
        this.relogio = Objects.requireNonNull(relogio, "relogio");
    }

    public <T> T ler(Chave<T> chave) {
        String salvo = banco.ler(conexao -> {
            try (PreparedStatement consulta = conexao.prepareStatement(
                    "SELECT valor_json FROM configuracao WHERE chave = ?")) {
                consulta.setString(1, chave.nome());
                try (ResultSet linha = consulta.executeQuery()) {
                    return linha.next() ? linha.getString(1) : null;
                }
            }
        });
        T padrao = chave.padrao().get();
        if (salvo == null) {
            return padrao;
        }
        JsonNode completo = mesclar(Json.arvore(padrao), Json.arvore(salvo));
        return Json.converter(completo, chave.tipo());
    }

    public <T> void gravar(Chave<T> chave, T valor) {
        Objects.requireNonNull(valor, "valor");
        String json = Json.escrever(valor);
        String agora = LocalDateTime.now(relogio).truncatedTo(ChronoUnit.SECONDS).toString();
        banco.transacao(conexao -> {
            try (PreparedStatement gravacao = conexao.prepareStatement(
                    "INSERT INTO configuracao (chave, valor_json, atualizado_em) VALUES (?, ?, ?) "
                            + "ON CONFLICT (chave) DO UPDATE SET valor_json = excluded.valor_json, "
                            + "atualizado_em = excluded.atualizado_em")) {
                gravacao.setString(1, chave.nome());
                gravacao.setString(2, json);
                gravacao.setString(3, agora);
                return gravacao.executeUpdate();
            }
        });
    }

    /**
     * Copia os campos salvos sobre os do padrão. Só entram campos que o padrão conhece; campos de objetos
     * aninhados (como os limiares dentro dos parâmetros) são mesclados um a um.
     */
    static JsonNode mesclar(JsonNode padrao, JsonNode salvo) {
        if (!(padrao instanceof ObjectNode objetoPadrao) || !(salvo instanceof ObjectNode objetoSalvo)) {
            return salvo == null || salvo.isMissingNode() ? padrao : salvo;
        }
        ObjectNode resultado = objetoPadrao.deepCopy();
        Iterator<String> campos = objetoPadrao.fieldNames();
        while (campos.hasNext()) {
            String campo = campos.next();
            if (objetoSalvo.has(campo)) {
                resultado.set(campo, mesclar(objetoPadrao.get(campo), objetoSalvo.get(campo)));
            }
        }
        return resultado;
    }
}
