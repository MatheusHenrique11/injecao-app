package br.com.calibra.dados.backup;

/** Por que um backup foi feito; fica no manifesto e aparece na lista de backups. */
public enum MotivoBackup {
    AUTOMATICO("automático (diário)"),
    AO_FECHAR("ao fechar o app"),
    MANUAL("feito por você"),
    ANTES_DE_MIGRAR("antes de atualizar o banco"),
    ANTES_DE_RESTAURAR("antes de restaurar outro backup");

    private final String rotulo;

    MotivoBackup(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
