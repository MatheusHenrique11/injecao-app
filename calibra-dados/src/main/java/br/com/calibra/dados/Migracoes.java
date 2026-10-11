package br.com.calibra.dados;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.output.MigrateResult;

import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.backup.InfoBackup;
import br.com.calibra.dados.backup.MotivoBackup;
import br.com.calibra.dados.backup.Restauracao;

/**
 * Migrações do banco (SPEC 10): ao abrir o app, o Flyway aplica os arquivos V1, V2... que faltam. Antes de
 * mexer num banco que já tem dados, faz um backup; se a migração falhar, volta esse backup e avisa.
 *
 * <p>Regra para quem programa: migração aplicada nunca é editada. Mudou o banco, crie um arquivo novo.
 */
public final class Migracoes {

    /** Pasta das migrações dentro do calibra-dados. */
    public static final String LOCAL_PADRAO = "classpath:br/com/calibra/dados/migracoes";

    private static final Logger LOG = Logger.getLogger(Migracoes.class.getName());

    /**
     * O que aconteceu ao abrir o banco.
     *
     * @param versaoAntes null num banco novo
     * @param backupAntes backup feito antes de migrar, ou null (banco novo ou nada a migrar)
     */
    public record Resultado(String versaoAntes, String versaoDepois, int aplicadas, InfoBackup backupAntes) {

        public boolean migrou() {
            return aplicadas > 0;
        }
    }

    private Migracoes() {
    }

    public static Resultado migrar(BancoDados banco, Backup backup) {
        return migrar(banco, backup, LOCAL_PADRAO);
    }

    /**
     * @throws BancoMaisNovoException se o banco veio de uma versão mais nova do app (nada é alterado)
     * @throws MigracaoFalhouException se a migração falhar (os dados voltam ao backup feito antes)
     */
    public static Resultado migrar(BancoDados banco, Backup backup, String... locais) {
        Flyway flyway = Flyway.configure()
                .dataSource(banco.fonte())
                .locations(locais)
                .load();
        MigrationInfoService info = flyway.info();
        recusarBancoMaisNovo(info);
        MigrationInfo atual = info.current();
        String antes = atual == null || atual.getVersion() == null ? null : atual.getVersion().getVersion();
        if (info.pending().length == 0) {
            return new Resultado(antes, antes, 0, null);
        }
        InfoBackup copia = antes == null ? null
                : backup.fazer(backup.pastas().backupsPadrao(), MotivoBackup.ANTES_DE_MIGRAR);
        try {
            MigrateResult resultado = flyway.migrate();
            LOG.info(() -> "Banco migrado de v" + antes + " para v" + resultado.targetSchemaVersion + " ("
                    + resultado.migrationsExecuted + " migração(ões)).");
            return new Resultado(antes, resultado.targetSchemaVersion, resultado.migrationsExecuted, copia);
        } catch (FlywayException erro) {
            LOG.log(Level.SEVERE, "A migração do banco falhou", erro);
            throw falhou(erro, copia, backup);
        }
    }

    private static MigracaoFalhouException falhou(FlywayException erro, InfoBackup copia, Backup backup) {
        if (copia == null) {
            return new MigracaoFalhouException("Não foi possível criar o banco de dados do Calibra. Detalhe: "
                    + erro.getMessage(), erro);
        }
        try {
            new Restauracao(backup).voltar(copia.arquivo());
        } catch (RuntimeException falhaAoVoltar) {
            erro.addSuppressed(falhaAoVoltar);
            return new MigracaoFalhouException("A atualização do banco de dados falhou e não foi possível voltar "
                    + "o backup sozinho. Seus dados estão no backup " + copia.arquivo() + ".", erro);
        }
        return new MigracaoFalhouException("A atualização do banco de dados falhou. Os dados voltaram a ser como "
                + "estavam antes (backup " + copia.arquivo().getFileName() + "). Detalhe: " + erro.getMessage(), erro);
    }

    /** Banco com migração que este app não conhece = banco de uma versão mais nova do Calibra. */
    private static void recusarBancoMaisNovo(MigrationInfoService info) {
        for (MigrationInfo migracao : info.all()) {
            if (migracao.getState().name().startsWith("FUTURE")) {
                throw new BancoMaisNovoException("Estes dados foram gravados por uma versão mais nova do Calibra "
                        + "(banco v" + migracao.getVersion() + "). Instale a versão mais nova para abri-los.");
            }
        }
    }
}
