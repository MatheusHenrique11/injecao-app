package br.com.calibra.dados.projeto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Um mapa base salvo, para a lista do projeto: sem as tabelas.
 *
 * @param pecas as peças do catálogo que ele usou, com a versão
 */
public record ResumoMapaBase(
        long id,
        int numero,
        long calculoId,
        long versaoMotorId,
        int numeroVersaoMotor,
        String versaoNucleo,
        int vermelhos,
        int amarelos,
        List<PecaUsada> pecas,
        LocalDateTime criadoEm) {

    public ResumoMapaBase {
        pecas = List.copyOf(pecas);
    }
}
