package br.com.calibra.dados;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regra 2.1 (zero rede) também nas bibliotecas novas da fase 2: o Flyway (que tem telemetria na edição
 * paga da Redgate), o sqlite-jdbc e os dois Jacksons não podem ter cliente HTTP nem socket. O teste abre
 * o jar de cada uma e procura as classes de rede dentro do código compilado.
 */
class DependenciasSemRedeTest {

    private static final List<String> PROIBIDOS =
            List.of("java/net/http/HttpClient", "java/net/HttpURLConnection", "java/net/Socket");

    @ParameterizedTest
    @ValueSource(strings = {
        "org.flywaydb.core.Flyway",
        "org.sqlite.JDBC",
        "com.fasterxml.jackson.databind.ObjectMapper",
        "com.fasterxml.jackson.core.JsonFactory",
        "tools.jackson.databind.ObjectMapper",
        "tools.jackson.core.json.JsonFactory"
    })
    void bibliotecaNaoUsaRede(String classe) throws Exception {
        Path jar = jarDe(Class.forName(classe));
        List<String> achados = new ArrayList<>();
        try (ZipFile arquivo = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entradas = arquivo.entries();
            while (entradas.hasMoreElements()) {
                ZipEntry entrada = entradas.nextElement();
                if (!entrada.getName().endsWith(".class")) {
                    continue;
                }
                String codigo = lerComoTexto(arquivo, entrada);
                for (String proibido : PROIBIDOS) {
                    if (codigo.contains(proibido)) {
                        achados.add(entrada.getName() + " usa " + proibido);
                    }
                }
            }
        }
        assertTrue(achados.isEmpty(), jar.getFileName() + " tem código de rede: " + achados);
    }

    private static Path jarDe(Class<?> classe) throws URISyntaxException {
        Path local = Path.of(classe.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(local.getFileName().toString().endsWith(".jar"), "esperava um jar: " + local);
        return local;
    }

    private static String lerComoTexto(ZipFile arquivo, ZipEntry entrada) throws IOException {
        try (InputStream dados = arquivo.getInputStream(entrada)) {
            return new String(dados.readAllBytes(), StandardCharsets.ISO_8859_1);
        }
    }
}
