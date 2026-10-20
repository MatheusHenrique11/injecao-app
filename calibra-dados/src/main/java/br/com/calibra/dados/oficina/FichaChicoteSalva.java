package br.com.calibra.dados.oficina;

import java.time.LocalDateTime;
import java.util.Objects;

import br.com.calibra.core.oficina.FichaChicote;
import br.com.calibra.core.oficina.NumeroOs;

/**
 * Uma ficha do chicote gravada (V13, Fase 3B): de qual OS, de qual versão ela veio e o conteúdo.
 *
 * @param versaoAnteriorId a ficha de onde esta versão começou; null na primeira
 * @param temVersaoNova já há uma versão feita a partir desta: ela fica só para ver
 */
public record FichaChicoteSalva(long id, long osId, NumeroOs numeroOs, Long versaoAnteriorId, boolean temVersaoNova,
        FichaChicote ficha, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {

    public FichaChicoteSalva {
        Objects.requireNonNull(numeroOs, "numeroOs");
        Objects.requireNonNull(ficha, "ficha");
    }
}
