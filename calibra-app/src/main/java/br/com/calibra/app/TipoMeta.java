package br.com.calibra.app;

import br.com.calibra.core.unidades.Conversoes;

/** Como a meta é dada (bloco 2.4): pela potência, ou pela pressão do turbo (só sobrealimentado). */
public enum TipoMeta {
    POTENCIA("Potência"),
    PRESSAO("Pressão do turbo (só sobrealimentado)");

    private final String rotulo;

    TipoMeta(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** A pressão relativa do turbo, na unidade da tela. */
    public enum Unidade {
        BAR("bar"),
        PSI("psi"),
        KPA("kPa");

        private final String simbolo;

        Unidade(String simbolo) {
            this.simbolo = simbolo;
        }

        public String simbolo() {
            return simbolo;
        }

        public double paraKpa(double valor) {
            return switch (this) {
                case BAR -> Conversoes.barParaKpa(valor);
                case PSI -> Conversoes.psiParaKpa(valor);
                case KPA -> valor;
            };
        }

        public double deKpa(double kpa) {
            return switch (this) {
                case BAR -> Conversoes.kpaParaBar(kpa);
                case PSI -> Conversoes.kpaParaPsi(kpa);
                case KPA -> kpa;
            };
        }
    }
}
