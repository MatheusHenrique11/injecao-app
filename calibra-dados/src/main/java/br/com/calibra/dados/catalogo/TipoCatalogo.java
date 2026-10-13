package br.com.calibra.dados.catalogo;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.motor.ModeloMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.Cambio;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Intercooler;
import br.com.calibra.core.pecas.Sensor;
import br.com.calibra.core.pecas.Turbo;

/**
 * Um tipo de catálogo (SPEC 8.5) e o record do núcleo que guarda os dados técnicos dos itens dele. Os modelos de
 * VE e de ignição entram na fase 3.
 *
 * @param <T> o record do núcleo
 */
public final class TipoCatalogo<T> {

    public static final TipoCatalogo<ModeloMotor> MOTOR =
            new TipoCatalogo<>("MOTOR", "Motores e montagens", "motor ou montagem", ModeloMotor.class, null);
    public static final TipoCatalogo<Combustivel> COMBUSTIVEL = new TipoCatalogo<>("COMBUSTIVEL", "Combustíveis",
            "combustível", Combustivel.class, TipoCatalogo::combustivelNaVersao);
    public static final TipoCatalogo<Bico> BICO = new TipoCatalogo<>("BICO", "Bicos", "bico", Bico.class, null);
    public static final TipoCatalogo<Bomba> BOMBA = new TipoCatalogo<>("BOMBA", "Bombas", "bomba", Bomba.class, null);
    public static final TipoCatalogo<Injecao> INJECAO =
            new TipoCatalogo<>("INJECAO", "Injeções", "injeção", Injecao.class, null);
    public static final TipoCatalogo<Intercooler> INTERCOOLER =
            new TipoCatalogo<>("INTERCOOLER", "Intercoolers", "intercooler", Intercooler.class, null);
    public static final TipoCatalogo<Sensor> SENSOR =
            new TipoCatalogo<>("SENSOR", "Sensores", "sensor", Sensor.class, null);
    public static final TipoCatalogo<Cambio> CAMBIO =
            new TipoCatalogo<>("CAMBIO", "Câmbios", "câmbio", Cambio.class, null);
    /** Turbos com o mapa do compressor (bloco 2.4). */
    public static final TipoCatalogo<Turbo> TURBO = new TipoCatalogo<>("TURBO", "Turbos", "turbo", Turbo.class, null);

    private static final List<TipoCatalogo<?>> TODOS =
            List.of(MOTOR, COMBUSTIVEL, BICO, BOMBA, INJECAO, INTERCOOLER, SENSOR, CAMBIO, TURBO);

    private final String codigo;
    private final String plural;
    private final String singular;
    private final Class<T> classe;
    private final BiFunction<T, Integer, T> carimbo;

    private TipoCatalogo(String codigo, String plural, String singular, Class<T> classe,
            BiFunction<T, Integer, T> carimbo) {
        this.codigo = codigo;
        this.plural = plural;
        this.singular = singular;
        this.classe = classe;
        this.carimbo = carimbo;
    }

    public static List<TipoCatalogo<?>> todos() {
        return TODOS;
    }

    public static TipoCatalogo<?> porCodigo(String codigo) {
        return TODOS.stream().filter(tipo -> tipo.codigo.equals(codigo)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de catálogo desconhecido: " + codigo));
    }

    /** Nome gravado no banco (ex.: "BICO"). */
    public String codigo() {
        return codigo;
    }

    /** "Bicos", para a aba. */
    public String plural() {
        return plural;
    }

    /** "bico", para as mensagens. */
    public String singular() {
        return singular;
    }

    public Class<T> classe() {
        return classe;
    }

    /**
     * Os dados com o número da versão do catálogo, quando o record guarda o próprio número (o combustível: o
     * motor e o cálculo guardam a versão que usaram, SPEC 7.2).
     */
    public T comVersao(T dados, int versao) {
        return carimbo == null ? dados : carimbo.apply(dados, versao);
    }

    /** Converte um objeto lido como "algum tipo" para este tipo. */
    public T converter(Object dados) {
        return classe.cast(Objects.requireNonNull(dados, "dados"));
    }

    @Override
    public String toString() {
        return codigo;
    }

    private static Combustivel combustivelNaVersao(Combustivel c, int versao) {
        return new Combustivel(c.codigo(), c.nome(), c.composicao(), c.densidadeKgL(), c.lambdaSobPressao(),
                c.lambdaAspiradoPlenaCarga(), c.avisoLambda(), c.vigenteDesde(), c.fonte(), versao);
    }
}
