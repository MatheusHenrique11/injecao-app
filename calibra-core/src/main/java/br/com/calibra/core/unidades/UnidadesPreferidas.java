package br.com.calibra.core.unidades;

/**
 * As unidades que a pessoa prefere ver na tela e nos PDFs (SPEC 8.7; decisão 289). O núcleo continua em SI: a
 * preferência só muda a apresentação. A escolhida vem primeiro no texto, e as outras continuam depois; com
 * {@code VARIAS}, o texto é o de sempre (ex.: "241,0 kPa · 2,41 bar · 34,95 psia").
 *
 * @param pressao a do MAP, do turbo e da bomba
 * @param vazao a da vazão do bico
 */
public record UnidadesPreferidas(Pressao pressao, Temperatura temperatura, Potencia potencia, Vazao vazao) {

    /** A pressão: absoluta (o MAP) ou acima da atmosfera (o turbo, a bomba). */
    public enum Pressao {
        VARIAS("Várias, como sempre"), KPA("kPa"), BAR("bar"), PSI("psi");

        private final String rotulo;

        Pressao(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    public enum Temperatura {
        VARIAS("Várias, como sempre"), CELSIUS("°C"), FAHRENHEIT("°F");

        private final String rotulo;

        Temperatura(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    public enum Potencia {
        VARIAS("Várias, como sempre"), CV("cv"), HP("hp"), KW("kW");

        private final String rotulo;

        Potencia(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    /** A vazão do bico; em g/min, a conta usa a densidade do fluido de teste do bico. */
    public enum Vazao {
        VARIAS("Várias, como sempre"), CM3_MIN("cm³/min (cc/min)"), LB_H("lb/h"), G_MIN("g/min");

        private final String rotulo;

        Vazao(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    /** Campo que falta (configuração antiga) fica em "várias". */
    public UnidadesPreferidas {
        pressao = pressao == null ? Pressao.VARIAS : pressao;
        temperatura = temperatura == null ? Temperatura.VARIAS : temperatura;
        potencia = potencia == null ? Potencia.VARIAS : potencia;
        vazao = vazao == null ? Vazao.VARIAS : vazao;
    }

    /** Sem preferência: o texto de sempre, com todas as unidades. */
    public static UnidadesPreferidas padrao() {
        return new UnidadesPreferidas(Pressao.VARIAS, Temperatura.VARIAS, Potencia.VARIAS, Vazao.VARIAS);
    }
}
