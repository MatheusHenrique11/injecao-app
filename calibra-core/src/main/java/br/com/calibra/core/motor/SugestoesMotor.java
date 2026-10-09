package br.com.calibra.core.motor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sugestões que o app oferece para cada número de cilindros (SPEC 6.3). São sempre marcadas
 * "confirmar no manual do motor" e sempre editáveis.
 */
public final class SugestoesMotor {

    public static final String CONFIRMAR = "Sugestão: confirmar no manual do motor.";

    /** Ordens de ignição comuns. Para 7, 9 e 11 cilindros a ordem é genérica (alterna ímpares e pares). */
    private static final Map<Integer, List<Integer>> ORDENS = Map.of(
            3, List.of(1, 2, 3),
            4, List.of(1, 3, 4, 2),
            5, List.of(1, 2, 4, 5, 3),
            6, List.of(1, 5, 3, 6, 2, 4),
            7, List.of(1, 3, 5, 7, 2, 4, 6),
            8, List.of(1, 8, 4, 3, 6, 5, 7, 2),
            9, List.of(1, 3, 5, 7, 9, 2, 4, 6, 8),
            10, List.of(1, 10, 9, 4, 3, 6, 5, 8, 7, 2),
            11, List.of(1, 3, 5, 7, 9, 11, 2, 4, 6, 8, 10),
            12, List.of(1, 7, 5, 11, 3, 9, 6, 12, 2, 8, 4, 10));

    private static final Set<Integer> ORDENS_GENERICAS = Set.of(7, 9, 11);

    private SugestoesMotor() {
    }

    public static List<Integer> ordemIgnicao(int cilindros) {
        exigirFaixa(cilindros);
        return ORDENS.get(cilindros);
    }

    /** Para 7, 9 e 11 cilindros não há uma ordem comum: a sugestão é genérica. */
    public static boolean ordemGenerica(int cilindros) {
        return ORDENS_GENERICAS.contains(cilindros);
    }

    /** Ângulos dos PMS com espaçamento igual de 720/n graus, na ordem de ignição (SPEC 6.3). */
    public static List<Double> angulosPms(int cilindros) {
        exigirFaixa(cilindros);
        List<Double> angulos = new ArrayList<>();
        for (int i = 0; i < cilindros; i++) {
            angulos.add(720.0 * i / cilindros);
        }
        return List.copyOf(angulos);
    }

    /** Até 6 cilindros, em linha; acima disso, V. */
    public static Arranjo arranjo(int cilindros) {
        exigirFaixa(cilindros);
        return cilindros <= 6 ? Arranjo.EM_LINHA : Arranjo.V;
    }

    /** Ângulo entre bancadas sugerido para o arranjo em V: 60° no V12, 90° nos outros. Em linha: null. */
    public static Double anguloEntreBancadas(int cilindros) {
        if (arranjo(cilindros) != Arranjo.V) {
            return null;
        }
        return cilindros == 12 ? 60.0 : 90.0;
    }

    /** Preenche no construtor tudo que depende do número de cilindros, com modos sequenciais. */
    public static ConfiguracaoMotor.Construtor aplicar(ConfiguracaoMotor.Construtor construtor, int cilindros) {
        return construtor
                .cilindros(cilindros)
                .arranjo(arranjo(cilindros))
                .anguloEntreBancadas(anguloEntreBancadas(cilindros))
                .ordemIgnicao(ordemIgnicao(cilindros))
                .angulosPms(angulosPms(cilindros))
                .modoInjecao(ModoInjecao.SEQUENCIAL)
                .modoIgnicao(ModoIgnicao.SEQUENCIAL);
    }

    private static void exigirFaixa(int cilindros) {
        if (cilindros < ValidadorMotor.MIN_CILINDROS || cilindros > ValidadorMotor.MAX_CILINDROS) {
            throw new IllegalArgumentException(ValidadorMotor.MENSAGEM_CILINDROS);
        }
    }
}
