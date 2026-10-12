package br.com.calibra.dados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.backup.InfoBackup;
import br.com.calibra.dados.backup.MotivoBackup;
import br.com.calibra.dados.config.Chave;

class MigracoesTest {

    private static final DadosOficina OFICINA = new DadosOficina("Oficina do Teste", "", "", "", "", null);

    @TempDir
    Path pasta;

    @Test
    void bancoNovoRecebeTodasAsMigracoesSemBackup() throws Exception {
        BaseDeTeste base = BaseDeTeste.semMigrar(pasta);
        Migracoes.Resultado resultado = Migracoes.migrar(base.banco, base.backup);
        assertNull(resultado.versaoAntes());
        assertEquals(BaseDeTeste.VERSAO_BANCO, resultado.versaoDepois());
        assertTrue(resultado.migrou());
        assertNull(resultado.backupAntes(), "banco novo não precisa de backup");
        assertTrue(tabelaExiste(base.banco, "configuracao"));
        assertTrue(tabelaExiste(base.banco, "auditoria"));
        assertTrue(Backup.listar(base.pastas.backupsPadrao()).isEmpty());
    }

    @Test
    void abrirDeNovoNaoMexeEmNada() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        Migracoes.Resultado resultado = Migracoes.migrar(base.banco, base.backup);
        assertFalse(resultado.migrou());
        assertEquals(BaseDeTeste.VERSAO_BANCO, resultado.versaoAntes());
        assertNull(resultado.backupAntes());
    }

    @Test
    void bancoComDadosGanhaBackupAntesDeMigrar() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        base.configuracao.gravar(Chave.OFICINA, OFICINA);

        Migracoes.Resultado resultado =
                Migracoes.migrar(base.banco, base.backup, Migracoes.LOCAL_PADRAO, BaseDeTeste.FUTURA_OK);

        assertEquals(BaseDeTeste.VERSAO_BANCO, resultado.versaoAntes());
        assertEquals(BaseDeTeste.VERSAO_FUTURA, resultado.versaoDepois());
        assertNotNull(resultado.backupAntes());
        assertEquals(MotivoBackup.ANTES_DE_MIGRAR, resultado.backupAntes().manifesto().motivo());
        assertEquals(BaseDeTeste.VERSAO_BANCO, resultado.backupAntes().manifesto().versaoBanco(),
                "o backup guarda o banco antes da migração futura");
        assertTrue(tabelaExiste(base.banco, "tabela_de_teste"));
        assertEquals(OFICINA, base.configuracao.ler(Chave.OFICINA));
    }

    /** SPEC 10: se a migração falhar, restaurar o backup e mostrar a mensagem. */
    @Test
    void migracaoQueFalhaVoltaOBackup() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        base.configuracao.gravar(Chave.OFICINA, OFICINA);

        // V900 dá certo e V901 falha no meio: sem a restauração, o banco ficaria na V900.
        MigracaoFalhouException erro = assertThrows(MigracaoFalhouException.class, () -> Migracoes.migrar(
                base.banco, base.backup, Migracoes.LOCAL_PADRAO, BaseDeTeste.FUTURA_OK, BaseDeTeste.FUTURA_FALHA));

        assertTrue(erro.getMessage().contains("voltaram a ser como estavam antes"), erro.getMessage());
        assertEquals(BaseDeTeste.VERSAO_BANCO, base.banco.versaoDoEsquema());
        assertFalse(tabelaExiste(base.banco, "tabela_de_teste"), "a V900 foi desfeita junto");
        assertEquals(OFICINA, base.configuracao.ler(Chave.OFICINA), "os dados continuam lá");
        List<InfoBackup> backups = Backup.listar(base.pastas.backupsPadrao());
        assertEquals(1, backups.size());
        assertEquals(MotivoBackup.ANTES_DE_MIGRAR, backups.getFirst().manifesto().motivo());
    }

    @Test
    void bancoDeVersaoMaisNovaERecusado() throws Exception {
        BaseDeTeste base = BaseDeTeste.criada(pasta);
        Migracoes.migrar(base.banco, base.backup, Migracoes.LOCAL_PADRAO, BaseDeTeste.FUTURA_OK);

        // O app de hoje não conhece a V900.
        BancoMaisNovoException erro =
                assertThrows(BancoMaisNovoException.class, () -> Migracoes.migrar(base.banco, base.backup));
        assertTrue(erro.getMessage().contains("versão mais nova do Calibra"), erro.getMessage());
        assertEquals(BaseDeTeste.VERSAO_FUTURA, base.banco.versaoDoEsquema(), "nada foi alterado");
    }

    static boolean tabelaExiste(BancoDados banco, String nome) {
        return banco.ler(conexao -> {
            try (PreparedStatement consulta = conexao.prepareStatement(
                    "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
                consulta.setString(1, nome);
                try (ResultSet linha = consulta.executeQuery()) {
                    return linha.next();
                }
            }
        });
    }
}
