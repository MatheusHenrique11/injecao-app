package br.com.calibra.dados.projeto;

import java.time.LocalDateTime;
import java.util.List;

import br.com.calibra.core.mapa.EntradaMapaBase;
import br.com.calibra.core.mapa.JustificativaMapa;
import br.com.calibra.core.mapa.MapaBase;
import br.com.calibra.core.memoria.Resultado;

/**
 * Um mapa base salvo no projeto (fase 4; decisão 296): a entrada, o resultado com as tabelas, as travas, os alertas e a
 * memória, as justificativas dos vermelhos, as peças do catálogo (com a versão), o cálculo e a versão do motor que
 * usou. Nunca muda.
 *
 * @param numero a versão do mapa no projeto (1, 2...)
 */
public record MapaBaseSalvo(
        long id,
        long projetoId,
        int numero,
        long calculoId,
        long versaoMotorId,
        int numeroVersaoMotor,
        EntradaMapaBase entrada,
        Resultado<MapaBase> resultado,
        List<JustificativaMapa> justificativas,
        List<PecaUsada> pecas,
        String versaoNucleo,
        String versaoApp,
        LocalDateTime criadoEm) {

    public MapaBaseSalvo {
        justificativas = List.copyOf(justificativas);
        pecas = List.copyOf(pecas);
    }
}
