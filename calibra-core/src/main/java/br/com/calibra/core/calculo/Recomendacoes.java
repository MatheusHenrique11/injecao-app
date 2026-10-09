package br.com.calibra.core.calculo;

import static br.com.calibra.core.util.NumeroBr.formatar;

import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.LimiaresSemaforo;

/** Recomendações de peças que não saem de uma fórmula (SPEC 7.3). */
public final class Recomendacoes {

    private Recomendacoes() {
    }

    /** Intercooler: ar-ar por padrão; ar-água sugerido para arrancada. Aspirado: null. */
    public static String intercooler(Aspiracao aspiracao, Uso uso) {
        if (!aspiracao.sobrealimentado()) {
            return null;
        }
        if (uso == Uso.ARRANCADA) {
            return "Intercooler: ar-água (watercooler) sugerido para arrancada.";
        }
        return "Intercooler: ar-ar (padrão).";
    }

    /**
     * Radiador de óleo (regra definida pelo dono do produto em 08/10/2026): circuito/endurance e náutico
     * sempre; off-road só quando a BMEP da meta passa da referência do semáforo (13 bar aspirado,
     * 22 bar sobrealimentado). Nos outros casos: null.
     */
    public static String radiadorDeOleo(Uso uso, double bmepBar, boolean sobrealimentado, LimiaresSemaforo limiares) {
        double referencia = limiares.bmepReferenciaBar(sobrealimentado);
        return switch (uso) {
            case CIRCUITO_ENDURANCE -> "Radiador de óleo recomendado (circuito / endurance).";
            case NAUTICO -> "Radiador de óleo recomendado (náutico).";
            case OFF_ROAD -> bmepBar > referencia
                    ? "Radiador de óleo recomendado (off-road com BMEP de " + formatar(bmepBar, 1)
                            + " bar, acima da referência de " + formatar(referencia, 0) + " bar)."
                    : null;
            case RUA, ARRANCADA -> null;
        };
    }
}
