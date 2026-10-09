package br.com.calibra.core.motor;

/** Saídas que o motor exige da injeção (SPEC 6.3). */
public final class SaidasExigidas {

    /**
     * Saídas exigidas.
     *
     * @param bicos saídas para os bicos dos cilindros
     * @param ignicao saídas de ignição
     * @param bicosExtras bicos no corpo de borboleta, contados à parte
     */
    public record Saidas(int bicos, int ignicao, int bicosExtras) {

        /** Bicos dos cilindros mais os extras. */
        public int bicosTotal() {
            return bicos + bicosExtras;
        }
    }

    private SaidasExigidas() {
    }

    /**
     * Bicos: sequencial = n × bicos por cilindro; semissequencial = (n/2) × bicos por cilindro;
     * simultâneo = grupos × bicos por cilindro. Ignição: sequencial = n; centelha perdida = n/2;
     * distribuidor = 1.
     *
     * @param gruposSimultaneo grupos do modo simultâneo (1 ou 2, conforme o perfil da injeção)
     */
    public static Saidas calcular(ConfiguracaoMotor motor, int gruposSimultaneo) {
        int n = motor.numeroCilindros();
        int porCilindro = motor.bicosPorCilindro();
        int bicos = switch (motor.modoInjecao()) {
            case SEQUENCIAL -> n * porCilindro;
            case SEMISSEQUENCIAL -> (n / 2) * porCilindro;
            case SIMULTANEO -> gruposSimultaneo * porCilindro;
        };
        int ignicao = switch (motor.modoIgnicao()) {
            case SEQUENCIAL -> n;
            case CENTELHA_PERDIDA -> n / 2;
            case DISTRIBUIDOR -> 1;
        };
        return new Saidas(bicos, ignicao, motor.bicosExtrasCorpoBorboleta());
    }
}
