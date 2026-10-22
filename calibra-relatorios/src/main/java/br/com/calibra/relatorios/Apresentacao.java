package br.com.calibra.relatorios;

import static br.com.calibra.core.util.NumeroBr.formatar;
import static br.com.calibra.core.util.NumeroBr.percentual;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import br.com.calibra.core.memoria.Valor;
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.core.unidades.UnidadesPreferidas;

/**
 * Texto dos resultados em várias unidades, para a tela e para o PDF (SPEC 2.6: conversão só na
 * interface). Ex.: 241,0 kPa · 2,41 bar · 34,96 psia.
 *
 * <p>Fase 4 (decisão 289): com uma unidade preferida (Configurações › Unidades), ela vem primeiro, e as outras
 * continuam depois. A preferência vale para o app inteiro, como o idioma: o app a passa ao abrir e ao salvar
 * ({@link #usarUnidades}), e a tela e os PDFs saem iguais.
 */
public final class Apresentacao {

    private static final String SEPARADOR = " · ";

    private static volatile UnidadesPreferidas unidades = UnidadesPreferidas.padrao();

    private Apresentacao() {
    }

    /** As unidades preferidas para a tela e os PDFs daqui em diante. */
    public static void usarUnidades(UnidadesPreferidas preferidas) {
        unidades = Objects.requireNonNull(preferidas, "preferidas");
    }

    public static UnidadesPreferidas unidades() {
        return unidades;
    }

    public static String potencia(double kw) {
        int primeira = switch (unidades.potencia()) {
            case CV -> 0;
            case HP -> 1;
            case KW -> 2;
            case VARIAS -> -1;
        };
        return juntar(List.of(formatar(Conversoes.kwParaCv(kw), 1) + " cv", formatar(Conversoes.kwParaHp(kw), 1)
                + " hp", formatar(kw, 1) + " kW"), primeira);
    }

    /** Pressão absoluta. */
    public static String pressaoAbsoluta(double kpa) {
        int primeira = switch (unidades.pressao()) {
            case KPA -> 0;
            case BAR -> 1;
            case PSI -> 2;
            case VARIAS -> -1;
        };
        return juntar(List.of(formatar(kpa, 1) + " kPa", formatar(Conversoes.kpaParaBar(kpa), 2) + " bar",
                formatar(Conversoes.kpaParaPsi(kpa), 2) + " psia"), primeira);
    }

    /** Pressão manométrica (acima da atmosfera), como a pressão do turbo ou da bomba. */
    public static String pressaoManometrica(double kpa) {
        int primeira = switch (unidades.pressao()) {
            case BAR -> 0;
            case PSI -> 1;
            case KPA -> 2;
            case VARIAS -> -1;
        };
        return juntar(List.of(formatar(Conversoes.kpaParaBar(kpa), 2) + " bar",
                formatar(Conversoes.kpaParaPsi(kpa), 1) + " psi", formatar(kpa, 0) + " kPa"), primeira);
    }

    public static String temperatura(double celsius) {
        int primeira = switch (unidades.temperatura()) {
            case CELSIUS -> 0;
            case FAHRENHEIT -> 1;
            case VARIAS -> -1;
        };
        return juntar(List.of(formatar(celsius, 1) + " °C", formatar(Conversoes.cParaF(celsius), 0) + " °F"),
                primeira);
    }

    /** As partes na ordem de sempre, com a da unidade preferida na frente ({@code primeira} = -1: sem mudança). */
    private static String juntar(List<String> partes, int primeira) {
        List<String> ordem = new ArrayList<>(partes);
        if (primeira > 0) {
            ordem.addFirst(ordem.remove(primeira));
        }
        return String.join(SEPARADOR, ordem);
    }

    public static String vazaoAr(double kgPorS) {
        return formatar(Conversoes.kgSParaLbMin(kgPorS), 2) + " lb/min" + SEPARADOR + formatar(kgPorS, 3) + " kg/s";
    }

    public static String combustivel(double kgPorH) {
        return formatar(kgPorH, 1) + " kg/h" + SEPARADOR + formatar(Conversoes.kgHParaLbH(kgPorH), 1) + " lb/h";
    }

    /**
     * Vazão de bico em cm³/min e em lb/h, no fluido em que foi medida. Com g/min preferido, ela vem na frente
     * (cm³/min × a densidade do fluido em g/cm³).
     */
    public static String vazaoBico(double cm3PorMin, double densidadeKgL) {
        String cm3 = formatar(cm3PorMin, 0) + " cm³/min";
        String lbH = formatar(Conversoes.cm3MinParaLbH(cm3PorMin, densidadeKgL), 1) + " lb/h";
        return switch (unidades.vazao()) {
            case VARIAS, CM3_MIN -> juntar(List.of(cm3, lbH), 0);
            case LB_H -> juntar(List.of(cm3, lbH), 1);
            case G_MIN -> juntar(List.of(formatar(cm3PorMin * densidadeKgL, 0) + " g/min", cm3, lbH), 0);
        };
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

    /** Um ponto no mapa do compressor: vazão corrigida, PR, eficiência, rotação e as folgas do surge e do choke. */
    public static String pontoNoMapa(MapaCompressor.PontoNoMapa ponto) {
        String inicio = "vazão corrigida " + vazaoAr(ponto.vazaoCorrigidaKgS()) + SEPARADOR + "PR "
                + formatar(ponto.razaoPressao(), 2);
        if (!ponto.dentro()) {
            return inicio + SEPARADOR + "fora do mapa (" + ponto.foraDoMapa() + ")";
        }
        return inicio + SEPARADOR + (ponto.eficiencia() == null ? "fora das ilhas desenhadas"
                : "eficiência " + percentual(ponto.eficiencia(), 0))
                + (ponto.rotacaoRpm() != null ? SEPARADOR + "turbo a " + formatar(ponto.rotacaoRpm(), 0) + " rpm"
                        : ponto.acimaDasLinhas() ? SEPARADOR + "acima da maior linha de rotação" : "")
                + SEPARADOR + "folga do surge " + percentual(ponto.margemSurge(), 0) + " e do choke "
                + percentual(ponto.margemChoke(), 0);
    }
}
