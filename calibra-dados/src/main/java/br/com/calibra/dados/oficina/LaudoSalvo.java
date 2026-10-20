package br.com.calibra.dados.oficina;

import java.time.LocalDateTime;
import java.util.Objects;

import br.com.calibra.core.oficina.LaudoEletrico;
import br.com.calibra.core.oficina.NumeroOs;

/**
 * Um laudo de diagnóstico elétrico gravado (V10, Fase 3B): de qual OS, quem fez e o conteúdo.
 *
 * @param mecanicoId null = sem mecânico
 * @param mecanico o nome do mecânico como está no cadastro; null sem mecânico
 */
public record LaudoSalvo(long id, long osId, NumeroOs numeroOs, Long mecanicoId, String mecanico,
        LaudoEletrico laudo, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {

    public LaudoSalvo {
        Objects.requireNonNull(numeroOs, "numeroOs");
        Objects.requireNonNull(laudo, "laudo");
    }
}
