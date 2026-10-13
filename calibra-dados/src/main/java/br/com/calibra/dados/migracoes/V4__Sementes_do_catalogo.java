package br.com.calibra.dados.migracoes;

import java.time.Clock;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import br.com.calibra.dados.catalogo.Sementes;

/**
 * V4 (fase 2, bloco 2.3): grava os itens que vêm com o app (combustíveis da SPEC 7.2, motores e montagens da
 * pesquisa). É uma migração em Java porque lê o JSON das sementes; o nome segue a regra do Flyway (V4__...).
 */
public class V4__Sementes_do_catalogo extends BaseJavaMigration {

    @Override
    public Integer getChecksum() {
        return Sementes.checksum();
    }

    @Override
    public void migrate(Context contexto) throws Exception {
        Sementes.aplicar(contexto.getConnection(), Clock.systemDefaultZone());
    }
}
