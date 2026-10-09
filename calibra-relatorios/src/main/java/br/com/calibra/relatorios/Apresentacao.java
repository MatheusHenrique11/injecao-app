package br.com.calibra.relatorios;

import static br.com.calibra.core.util.NumeroBr.formatar;
import static br.com.calibra.core.util.NumeroBr.percentual;

import br.com.calibra.core.memoria.Valor;
import br.com.calibra.core.unidades.Conversoes;

/**
 * Texto dos resultados em várias unidades, para a tela e para o PDF (SPEC 2.6: conversão só na
 * interface). Ex.: 241,0 kPa · 2,41 bar · 34,96 psia.
 */
public final class Apresentacao {

    private static final String SEPARADOR = " · ";

    private Apresentacao() {
    }

    public static String potencia(double kw) {
        return formatar(Conversoes.kwParaCv(kw), 1) + " cv" + SEPARADOR + formatar(Conversoes.kwParaHp(kw), 1) + " hp"
                + SEPARADOR + formatar(kw, 1) + " kW";
    }

    /** Pressão absoluta. */
    public static String pressaoAbsoluta(double kpa) {
        return formatar(kpa, 1) + " kPa" + SEPARADOR + formatar(Conversoes.kpaParaBar(kpa), 2) + " bar"
                + SEPARADOR + formatar(Conversoes.kpaParaPsi(kpa), 2) + " psia";
    }

    /** Pressão manométrica (acima da atmosfera), como a pressão do turbo ou da bomba. */
    public static String pressaoManometrica(double kpa) {
        return formatar(Conversoes.kpaParaBar(kpa), 2) + " bar" + SEPARADOR + formatar(Conversoes.kpaParaPsi(kpa), 1)
                + " psi" + SEPARADOR + formatar(kpa, 0) + " kPa";
    }

    public static String temperatura(double celsius) {
        return formatar(celsius, 1) + " °C" + SEPARADOR + formatar(Conversoes.cParaF(celsius), 0) + " °F";
    }

    public static String vazaoAr(double kgPorS) {
        return formatar(Conversoes.kgSParaLbMin(kgPorS), 2) + " lb/min" + SEPARADOR + formatar(kgPorS, 3) + " kg/s";
    }

    public static String combustivel(double kgPorH) {
        return formatar(kgPorH, 1) + " kg/h" + SEPARADOR + formatar(Conversoes.kgHParaLbH(kgPorH), 1) + " lb/h";
    }

    /** Vazão de bico em cm³/min e em lb/h, no fluido em que foi medida. */
    public static String vazaoBico(double cm3PorMin, double densidadeKgL) {
        return formatar(cm3PorMin, 0) + " cm³/min" + SEPARADOR
                + formatar(Conversoes.cm3MinParaLbH(cm3PorMin, densidadeKgL), 1) + " lb/h";
    }

    public static String percentualDe(double fracao) {
        return percentual(fracao, 1);
    }

    /** Número com unidade, para a memória de cálculo. "fração" vira percentual. */
    public static String valor(Valor valor) {
        return valor(valor.numero(), valor.unidade());
    }

    public static String valor(double numero, String unidade) {
        if ("fração".equals(unidade)) {
            return percentual(numero, 1);
        }
        boolean inteiro = numero == Math.rint(numero) && Math.abs(numero) < 1e9;
        String texto = inteiro ? formatar(numero, 0) : formatar(numero);
        return unidade == null || unidade.isBlank() ? texto : texto + " " + unidade;
    }
}
