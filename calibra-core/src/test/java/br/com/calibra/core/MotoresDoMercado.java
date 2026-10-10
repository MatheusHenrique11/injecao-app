package br.com.calibra.core;

import java.util.List;

import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.ConfiguracaoMotor;

/**
 * Motores do mercado brasileiro de preparação, para testar a cadeia com geometrias reais (fase 1.1).
 * Dados e fontes: docs/pesquisa/mercado-preparacao-brasil.md (pesquisa de 09/10/2026). Só entram aqui a
 * geometria e os dados de fábrica que têm fonte; o resto da configuração usa os valores neutros de
 * {@link Fixtures#motor(int)}.
 */
public final class MotoresDoMercado {

    /**
     * Um motor de referência.
     *
     * @param cilindradaFichaCc cilindrada publicada na ficha técnica
     * @param aspiracao aspiração de fábrica
     * @param potenciaSerieCv potência de fábrica no virabrequim (NaN quando não usada nos testes)
     * @param rotacaoPotencia rotação da potência de fábrica (0 quando não usada nos testes)
     * @param fonte de onde vieram os dados
     */
    public record Motor(
            String nome,
            int cilindros,
            int valvulasPorCilindro,
            double diametroMm,
            double cursoMm,
            double cilindradaFichaCc,
            Aspiracao aspiracao,
            double potenciaSerieCv,
            int rotacaoPotencia,
            String fonte) {

        public boolean temDadosDeSerie() {
            return rotacaoPotencia > 0;
        }

        /** Configuração válida com a geometria deste motor e os valores neutros de teste. */
        public ConfiguracaoMotor.Construtor configuracao() {
            return Fixtures.motor(cilindros)
                    .diametroMm(diametroMm)
                    .cursoMm(cursoMm)
                    .valvulasPorCilindro(valvulasPorCilindro)
                    .aspiracao(aspiracao);
        }

        /** Nome curto nos relatórios dos testes parametrizados. */
        @Override
        public String toString() {
            return nome;
        }
    }

    private static final String AP = "Luciano Preparação, ficha técnica do motor AP";
    private static final String OPALA = "Autoentusiastas, motores de 4 e 6 cilindros do Opala (2019)";
    private static final String FAMILIA_II = "Wikipedia, GM Family II engine";
    private static final String PRATOLA = "Wikipedia, Fiat Pratola Serra modular engines";

    public static final List<Motor> TODOS = List.of(
            new Motor("VW AP 1.6", 4, 2, 81.0, 77.4, 1596, Aspiracao.ASPIRADO, Double.NaN, 0, AP),
            new Motor("VW AP 1.8", 4, 2, 81.0, 86.4, 1781, Aspiracao.ASPIRADO, Double.NaN, 0, AP),
            new Motor("VW AP 2.0", 4, 2, 82.5, 92.8, 1984, Aspiracao.ASPIRADO, Double.NaN, 0, AP),
            new Motor("GM Opala 151 (2.5)", 4, 2, 101.6, 76.2, 2471, Aspiracao.ASPIRADO, Double.NaN, 0, OPALA),
            new Motor("GM Opala 250 / 250-S (4.1)", 6, 2, 98.42, 89.66, 4093, Aspiracao.ASPIRADO, Double.NaN, 0,
                    OPALA),
            new Motor("GM Omega 4.1 (C41GE)", 6, 2, 98.4, 89.7, 4093, Aspiracao.ASPIRADO, 168, 4500,
                    "Futcarros e Garagem do Alan, Omega 4.1"),
            new Motor("GM Omega 3.0 (C30NE)", 6, 2, 95.0, 69.8, 2968, Aspiracao.ASPIRADO, 165, 5800,
                    "Heycar e Ficha Completa, Omega CD 3.0"),
            new Motor("GM Família II C20NE", 4, 2, 86.0, 86.0, 1998, Aspiracao.ASPIRADO, Double.NaN, 0, FAMILIA_II),
            new Motor("GM Família II C22NE", 4, 2, 86.0, 94.6, 2198, Aspiracao.ASPIRADO, Double.NaN, 0, FAMILIA_II),
            new Motor("GM Família II 2.4", 4, 2, 87.5, 100.0, 2405, Aspiracao.ASPIRADO, Double.NaN, 0, FAMILIA_II),
            new Motor("Fiat 2.0 20V Turbo (Marea Turbo)", 5, 4, 82.0, 75.65, 1998, Aspiracao.TURBO, 182, 6000,
                    PRATOLA + "; Auto-data, Marea 2.0 20V Turbo"),
            new Motor("Fiat 2.4 20V", 5, 4, 83.0, 90.4, 2446, Aspiracao.ASPIRADO, Double.NaN, 0, PRATOLA),
            new Motor("VW 2.5 20V (Jetta) a 5.000 rpm", 5, 4, 82.5, 92.8, 2480, Aspiracao.ASPIRADO, 170, 5000,
                    "Notícias Automotivas, Jetta Variant 2.5"),
            new Motor("VW 2.5 20V (Jetta) a 5.700 rpm", 5, 4, 82.5, 92.8, 2480, Aspiracao.ASPIRADO, 170, 5700,
                    "Auto-data, Jetta V 2.5i (as fichas divergem na rotação)"),
            new Motor("Hyundai G4GC (i30 CW)", 4, 4, 82.0, 93.5, 1975, Aspiracao.ASPIRADO, 145, 6000,
                    "Manual de oficina Hyundai G4GC; Carros na Web, i30 2.0"),
            new Motor("VW EA211 1.0 TSI", 3, 4, 74.5, 76.4, 999, Aspiracao.TURBO, Double.NaN, 0,
                    "Wikipedia, Volkswagen EA211 engine"),
            new Motor("VW EA888 2.0 TSI (3ª geração)", 4, 4, 82.5, 92.8, 1984, Aspiracao.TURBO, Double.NaN, 0,
                    "Audi SSP 606"));

    private MotoresDoMercado() {
    }

    public static List<Motor> comDadosDeSerie() {
        return TODOS.stream().filter(Motor::temDadosDeSerie).toList();
    }

    public static List<Motor> aspiradosDeSerie() {
        return TODOS.stream()
                .filter(Motor::temDadosDeSerie)
                .filter(m -> m.aspiracao() == Aspiracao.ASPIRADO)
                .toList();
    }
}
