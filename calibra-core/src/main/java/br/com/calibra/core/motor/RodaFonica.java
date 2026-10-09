package br.com.calibra.core.motor;

/**
 * Roda fônica e sensor de fase (SPEC 6.3). Ex.: 60-2 = 60 dentes totais, 2 faltantes.
 *
 * @param sensorFase se o motor tem sensor de fase (comando). Injeção ou ignição sequencial exigem.
 */
public record RodaFonica(int dentesTotais, int dentesFaltantes, boolean sensorFase) {

    @Override
    public String toString() {
        return dentesTotais + "-" + dentesFaltantes + (sensorFase ? " com sensor de fase" : " sem sensor de fase");
    }
}
