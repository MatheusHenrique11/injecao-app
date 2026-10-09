package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Regra inegociável 2.1 da SPEC: zero rede. Varre o código de produção (src/main) de todos os
 * módulos e falha se encontrar qualquer API de rede.
 */
class SemRedeTest {

    private static final List<String> PROIBIDOS =
            List.of("java.net.http", "HttpURLConnection", "java.net.Socket", "openConnection");

    private static final Set<String> EXTENSOES_DE_TEXTO = Set.of(".java", ".fxml", ".css", ".properties");

    @Test
    void nenhumCodigoDeProducaoAcessaARede() throws IOException {
        Path raiz = Path.of(System.getProperty("basedir", "")).toAbsolutePath().getParent();
        List<Path> modulos;
        try (Stream<Path> itens = Files.list(raiz)) {
            modulos = itens
                    .filter(p -> p.getFileName().toString().startsWith("calibra-"))
                    .filter(p -> Files.isDirectory(p.resolve("src").resolve("main")))
                    .toList();
        }
        assertTrue(modulos.size() >= 5, "Esperava os 5 módulos calibra-* em " + raiz + ", achei " + modulos);

        List<String> achados = new ArrayList<>();
        for (Path modulo : modulos) {
            try (Stream<Path> arquivos = Files.walk(modulo.resolve("src").resolve("main"))) {
                for (Path arquivo : arquivos.filter(Files::isRegularFile).filter(SemRedeTest::eTexto).toList()) {
                    String conteudo = Files.readString(arquivo);
                    for (String proibido : PROIBIDOS) {
                        if (conteudo.contains(proibido)) {
                            achados.add(raiz.relativize(arquivo) + " usa " + proibido);
                        }
                    }
                }
            }
        }
        assertTrue(achados.isEmpty(), "Código de produção com acesso à rede (regra 2.1): " + achados);
    }

    private static boolean eTexto(Path arquivo) {
        String nome = arquivo.getFileName().toString();
        return EXTENSOES_DE_TEXTO.stream().anyMatch(nome::endsWith);
    }
}
