package br.com.calibra.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Log do app em arquivo (SPEC 10): logs\calibra-0.log a calibra-9.log, 5 MB cada. Quando o 0 enche, ele
 * vira o 1, o 1 vira o 2... e o 9 é apagado. Usa o java.util.logging do JDK, sem dependência.
 */
final class Logs {

    static final int LIMITE_BYTES = 5 * 1024 * 1024;
    static final int ARQUIVOS = 10;

    private Logs() {
    }

    /** Liga o log em arquivo para o app inteiro. */
    static void configurar(Path pastaLogs) throws IOException {
        Logger raiz = Logger.getLogger("");
        raiz.setLevel(Level.INFO);
        raiz.addHandler(criarArquivo(pastaLogs));
    }

    /** O gravador dos arquivos de log; acrescenta ao arquivo que já existe. */
    static FileHandler criarArquivo(Path pastaLogs) throws IOException {
        // No padrão do FileHandler, "/" separa pastas, %g é o número do arquivo e % precisa virar %%.
        String padrao = pastaLogs.toAbsolutePath().toString().replace("%", "%%").replace('\\', '/')
                + "/calibra-%g.log";
        FileHandler arquivo = new FileHandler(padrao, LIMITE_BYTES, ARQUIVOS, true);
        arquivo.setEncoding(StandardCharsets.UTF_8.name());
        arquivo.setFormatter(new Formato());
        arquivo.setLevel(Level.INFO);
        return arquivo;
    }

    /** Uma linha por registro: "2026-10-11 18:30:00 INFO [Classe] mensagem", e o erro completo embaixo. */
    static final class Formato extends Formatter {

        private static final DateTimeFormatter DATA =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        @Override
        public String format(LogRecord registro) {
            StringBuilder linha = new StringBuilder()
                    .append(DATA.format(Instant.ofEpochMilli(registro.getMillis()))).append(' ')
                    .append(registro.getLevel().getName()).append(" [")
                    .append(nomeCurto(registro.getLoggerName())).append("] ")
                    .append(formatMessage(registro)).append(System.lineSeparator());
            if (registro.getThrown() != null) {
                StringWriter pilha = new StringWriter();
                registro.getThrown().printStackTrace(new PrintWriter(pilha));
                linha.append(pilha);
            }
            return linha.toString();
        }

        private static String nomeCurto(String nome) {
            if (nome == null || nome.isEmpty()) {
                return "-";
            }
            return nome.substring(nome.lastIndexOf('.') + 1);
        }
    }
}
