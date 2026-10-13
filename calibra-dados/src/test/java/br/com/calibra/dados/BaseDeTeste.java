package br.com.calibra.dados;

import java.nio.file.Path;

import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.backup.Restauracao;
import br.com.calibra.dados.config.ConfiguracaoRepositorio;

/** Pastas, banco, backup e configurações numa pasta temporária, como o app monta em %LOCALAPPDATA%. */
public final class BaseDeTeste {

    public static final String VERSAO_APP = "0.2.0-teste";

    /** Última migração do app (V1, V2...). Sobe junto com cada migração nova. */
    public static final String VERSAO_BANCO = "4";

    /** Migrações só de teste, com números bem acima dos do app: simulam uma versão futura do banco. */
    public static final String FUTURA_OK = "classpath:br/com/calibra/dados/migracoes-teste/futura-ok";
    public static final String FUTURA_FALHA = "classpath:br/com/calibra/dados/migracoes-teste/futura-falha";
    public static final String VERSAO_FUTURA = "900";

    public final PastasDados pastas;
    public final RelogioDeTeste relogio;
    public final BancoDados banco;
    public final Backup backup;
    public final ConfiguracaoRepositorio configuracao;

    private BaseDeTeste(Path raiz) {
        this.pastas = new PastasDados(raiz);
        this.relogio = new RelogioDeTeste();
        this.banco = new BancoDados(pastas.banco());
        this.backup = new Backup(pastas, banco, VERSAO_APP, relogio);
        this.configuracao = new ConfiguracaoRepositorio(banco, relogio);
    }

    /** Cria as pastas e o banco, com as migrações do app aplicadas. */
    public static BaseDeTeste criada(Path raiz) throws Exception {
        BaseDeTeste base = semMigrar(raiz);
        Migracoes.migrar(base.banco, base.backup);
        return base;
    }

    /** Cria as pastas, sem banco. */
    public static BaseDeTeste semMigrar(Path raiz) throws Exception {
        BaseDeTeste base = new BaseDeTeste(raiz);
        base.pastas.criar();
        return base;
    }

    public Restauracao restauracao() {
        return new Restauracao(backup);
    }
}
