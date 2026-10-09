package br.com.calibra.core.combustivel;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Os combustíveis da tabela da SPEC 7.2. Na fase 2 eles passam para o banco, editáveis e versionados.
 */
public final class CombustiveisPadrao {

    public static final String FONTE = "SPEC 7.2 (versão 1.0, 08/10/2026)";

    public static final Combustivel E32 = new Combustivel("E32", "Gasolina C comum E32",
            new GasolinaComEtanol(0.32), 0.756, 0.80, 0.87, null, LocalDate.of(2026, 8, 1), FONTE, 1);

    public static final Combustivel E25 = new Combustivel("E25", "Gasolina premium E25",
            new GasolinaComEtanol(0.25), 0.752, 0.80, 0.87, null, null, FONTE, 1);

    public static final Combustivel EHC = new Combustivel("EHC", "Etanol hidratado (93% em massa)",
            new EtanolHidratado(0.93), 0.809, 0.80, 0.87, null, null, FONTE, 1);

    public static final Combustivel EAC = new Combustivel("EAC", "Etanol anidro",
            new AfrFixo(9.0), 0.789, 0.80, 0.87, null, null, FONTE, 1);

    public static final Combustivel E85 = new Combustivel("E85", "E85",
            new AfrFixo(9.75), 0.78, 0.80, 0.87, null, null, FONTE, 1);

    public static final Combustivel METANOL = new Combustivel("METANOL", "Metanol",
            new AfrFixo(6.42), 0.792, 0.65, 0.70,
            "Metanol: lambda padrão 0,65 sob pressão e 0,70 aspirado, bem mais rica que os outros combustíveis. "
                    + "Confirmar no acerto.",
            null, FONTE, 1);

    private static final List<Combustivel> TODOS = List.of(E32, E25, EHC, EAC, E85, METANOL);

    private CombustiveisPadrao() {
    }

    public static List<Combustivel> todos() {
        return TODOS;
    }

    public static Optional<Combustivel> porCodigo(String codigo) {
        return TODOS.stream().filter(c -> c.codigo().equals(codigo)).findFirst();
    }
}
