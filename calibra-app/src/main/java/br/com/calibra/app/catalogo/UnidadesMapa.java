package br.com.calibra.app.catalogo;

import br.com.calibra.core.unidades.Conversoes;

/** Unidades da referência do mapa do compressor, como aparecem na legenda do fabricante. O núcleo usa K e kPa. */
public final class UnidadesMapa {

    private UnidadesMapa() {
    }

    /** Temperatura de referência. */
    public enum Temperatura {
        CELSIUS("°C"),
        KELVIN("K"),
        FAHRENHEIT("°F"),
        RANKINE("°R");

        private final String simbolo;

        Temperatura(String simbolo) {
            this.simbolo = simbolo;
        }

        public String simbolo() {
            return simbolo;
        }

        public double paraK(double valor) {
            return switch (this) {
                case CELSIUS -> Conversoes.cParaK(valor);
                case KELVIN -> valor;
                case FAHRENHEIT -> Conversoes.cParaK(Conversoes.fParaC(valor));
                case RANKINE -> valor / 1.8;
            };
        }

        public double deK(double kelvin) {
            return switch (this) {
                case CELSIUS -> Conversoes.kParaC(kelvin);
                case KELVIN -> kelvin;
                case FAHRENHEIT -> Conversoes.cParaF(Conversoes.kParaC(kelvin));
                case RANKINE -> kelvin * 1.8;
            };
        }
    }

    /** Pressão de referência (absoluta). */
    public enum Pressao {
        KPA("kPa"),
        PSIA("psia"),
        BAR("bar");

        private final String simbolo;

        Pressao(String simbolo) {
            this.simbolo = simbolo;
        }

        public String simbolo() {
            return simbolo;
        }

        public double paraKpa(double valor) {
            return switch (this) {
                case KPA -> valor;
                case PSIA -> Conversoes.psiParaKpa(valor);
                case BAR -> Conversoes.barParaKpa(valor);
            };
        }

        public double deKpa(double kpa) {
            return switch (this) {
                case KPA -> kpa;
                case PSIA -> Conversoes.kpaParaPsi(kpa);
                case BAR -> Conversoes.kpaParaBar(kpa);
            };
        }
    }
}
