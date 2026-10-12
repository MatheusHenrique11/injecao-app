package br.com.calibra.app;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.logging.Logger;

import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.config.ConfiguracaoRepositorio;

/**
 * O que o app faz ao abrir, antes da tela: pastas, caches e banco migrado. Sem JavaFX, para dar para testar.
 */
final class Partida {

    /** Troca a pasta de dados (testes de interface e ensaios); sem ela, vale %LOCALAPPDATA%\Calibra. */
    static final String PROPRIEDADE_PASTA = "calibra.pasta";

    private static final Logger LOG = Logger.getLogger(Partida.class.getName());

    private Partida() {
    }

    /** %LOCALAPPDATA%\Calibra (SPEC 5), nunca a pasta de instalação. */
    static PastasDados pastas() {
        String escolhida = System.getProperty(PROPRIEDADE_PASTA);
        if (escolhida != null && !escolhida.isBlank()) {
            return new PastasDados(Path.of(escolhida));
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        Path raiz = localAppData != null ? Path.of(localAppData) : Path.of(System.getProperty("user.home"));
        return new PastasDados(raiz.resolve("Calibra"));
    }

    /**
     * Manda os caches das bibliotecas para dentro de cache\: as fontes do PDFBox, a DLL do sqlite-jdbc e os
     * nativos do JavaFX (que iriam para a pasta do usuário). Precisa rodar antes de elas serem usadas; não
     * troca o que já estiver configurado (os testes usam target/).
     */
    static void prepararCaches(PastasDados pastas) {
        String cache = pastas.cache().toString();
        for (String propriedade : new String[] {"pdfbox.fontcache", "org.sqlite.tmpdir", "javafx.cachedir"}) {
            if (System.getProperty(propriedade) == null) {
                System.setProperty(propriedade, cache);
            }
        }
    }

    /**
     * Cria as pastas e abre o banco, migrando se precisar (com backup antes; SPEC 10).
     *
     * @throws IOException se não der para criar as pastas
     * @throws br.com.calibra.dados.ErroDados se o banco não abrir; a mensagem já explica o que houve
     */
    static ContextoApp abrir(PastasDados pastas, String versaoApp, Clock relogio) throws IOException {
        pastas.criar();
        prepararCaches(pastas);
        BancoDados banco = new BancoDados(pastas.banco());
        Backup backup = new Backup(pastas, banco, versaoApp, relogio);
        Migracoes.Resultado migracao = Migracoes.migrar(banco, backup);
        LOG.info(() -> "Banco aberto em " + pastas.banco() + " (versão " + migracao.versaoDepois() + ").");
        return new ContextoApp(pastas, banco, backup, new ConfiguracaoRepositorio(banco, relogio), versaoApp,
                relogio, migracao);
    }
}
