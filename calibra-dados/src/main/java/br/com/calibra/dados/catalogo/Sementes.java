package br.com.calibra.dados.catalogo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.calibra.dados.json.Json;

/**
 * Itens que vêm com o app (SPEC 8.5 e pedido C): os combustíveis da SPEC 7.2 e os motores e montagens da
 * pesquisa, todos com fonte. Ficam no JSON {@code sementes/catalogo.json}, legível, e entram no banco pela
 * migração V4.
 */
public final class Sementes {

    static final String RECURSO = "/br/com/calibra/dados/sementes/catalogo.json";

    /** Um item semente: a chave que o identifica, o tipo, os dados técnicos e a informação comum. */
    public record Semente(String chave, TipoCatalogo<?> tipo, Object dados, InfoCatalogo info) {
    }

    private Sementes() {
    }

    /** Todas as sementes do arquivo, na ordem dele. */
    public static List<Semente> ler() {
        JsonNode raiz = Json.arvore(new String(bytes(), StandardCharsets.UTF_8));
        List<Semente> sementes = new ArrayList<>();
        for (JsonNode item : raiz.get("itens")) {
            TipoCatalogo<?> tipo = TipoCatalogo.porCodigo(item.get("tipo").asText());
            sementes.add(new Semente(item.get("chave").asText(), tipo,
                    Json.converter(item.get("dados"), tipo.classe()),
                    Json.converter(item.get("info"), InfoCatalogo.class)));
        }
        return sementes;
    }

    /** Grava todas as sementes (na migração, num banco que ainda não tem catálogo). */
    public static int aplicar(Connection conexao, Clock relogio) throws SQLException {
        CatalogoRepositorio repositorio = new CatalogoRepositorio(relogio);
        List<Semente> sementes = ler();
        for (Semente semente : sementes) {
            inserir(repositorio, conexao, semente.tipo(), semente);
        }
        return sementes.size();
    }

    /** Impressão digital do arquivo: se ele mudar depois de aplicado, o Flyway acusa (migração não se edita). */
    public static int checksum() {
        return checksum(bytes());
    }

    /**
     * CRC32 do arquivo sem os CR ({@code \r}). Com o Git em autocrlf, uma cópia nova do repositório grava o arquivo
     * com CRLF; o número não pode mudar por isso, senão o Flyway recusa os bancos que já têm a V4. Sem CR, o
     * resultado é o mesmo do arquivo com LF, que é o que os bancos guardam desde a 0.2.0.
     */
    static int checksum(byte[] conteudo) {
        CRC32 crc = new CRC32();
        for (byte umByte : conteudo) {
            if (umByte != '\r') {
                crc.update(umByte);
            }
        }
        return (int) crc.getValue();
    }

    private static <T> void inserir(CatalogoRepositorio repositorio, Connection conexao, TipoCatalogo<T> tipo,
            Semente semente) throws SQLException {
        repositorio.inserir(conexao, tipo, tipo.converter(semente.dados()), semente.info(), semente.chave());
    }

    private static byte[] bytes() {
        try (InputStream arquivo = Sementes.class.getResourceAsStream(RECURSO)) {
            if (arquivo == null) {
                throw new IllegalStateException("Arquivo de sementes não está no app: " + RECURSO);
            }
            return arquivo.readAllBytes();
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }
}
