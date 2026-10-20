package br.com.calibra.dados.migracoes;

import java.time.Clock;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import br.com.calibra.dados.catalogo.Sementes;

/**
 * V9 (Fase 3B, bloco 3B.3): grava os 7 modelos de checklist da SPEC 6.6.2, todos "a verificar" e com todos os itens
 * obrigatórios, do arquivo {@code sementes/checklists.json}. Só acrescenta itens ao catálogo; nada que existe muda.
 */
public class V9__Sementes_dos_checklists extends BaseJavaMigration {

    @Override
    public Integer getChecksum() {
        return Sementes.checksum(Sementes.CHECKLISTS);
    }

    @Override
    public void migrate(Context contexto) throws Exception {
        Sementes.aplicar(contexto.getConnection(), Clock.systemDefaultZone(), Sementes.CHECKLISTS);
    }
}
