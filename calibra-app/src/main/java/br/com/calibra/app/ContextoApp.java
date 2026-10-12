package br.com.calibra.app;

import java.time.Clock;

import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.config.ConfiguracaoRepositorio;

/** O que as telas usam do app aberto: pastas, banco, backup e configurações. */
record ContextoApp(PastasDados pastas, BancoDados banco, Backup backup, ConfiguracaoRepositorio configuracao,
        String versaoApp, Clock relogio, Migracoes.Resultado migracao) {
}
