package br.com.calibra.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/** Versão do app (SPEC 10: visível no rodapé e nos PDFs). Vem do POM, pelo versao.properties. */
public final class Versao {

    private static final String ATUAL = ler();

    private Versao() {
    }

    public static String atual() {
        return ATUAL;
    }

    private static String ler() {
        try (InputStream arquivo = Versao.class.getResourceAsStream("versao.properties")) {
            if (arquivo == null) {
                throw new IllegalStateException("versao.properties não está no app");
            }
            Properties propriedades = new Properties();
            propriedades.load(arquivo);
            String versao = propriedades.getProperty("versao", "").strip();
            if (versao.isEmpty() || versao.contains("${")) {
                throw new IllegalStateException("versao.properties sem a versão do POM: " + versao);
            }
            return versao;
        } catch (IOException erro) {
            throw new UncheckedIOException(erro);
        }
    }
}
