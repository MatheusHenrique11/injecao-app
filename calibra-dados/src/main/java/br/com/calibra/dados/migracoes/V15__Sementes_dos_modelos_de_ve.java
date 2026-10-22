package br.com.calibra.dados.migracoes;

import java.time.Clock;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import br.com.calibra.dados.catalogo.Sementes;

/**
 * V15 (fase 4, bloco 4.2): grava os 4 modelos de VE iniciais da SPEC 7.5 (2 e 4 válvulas, comando normal e
 * agressivo), todos "a verificar", do arquivo {@code sementes/modelos-ve.json} (decisão 293). Só acrescenta itens ao
 * catálogo; nada que existe muda.
 */
public class V15__Sementes_dos_modelos_de_ve extends BaseJavaMigration {

    @Override
    public Integer getChecksum() {
        return Sementes.checksum(Sementes.MODELOS_VE);
    }

    @Override
    public void migrate(Context contexto) throws Exception {
        Sementes.aplicar(contexto.getConnection(), Clock.systemDefaultZone(), Sementes.MODELOS_VE);
    }
}
