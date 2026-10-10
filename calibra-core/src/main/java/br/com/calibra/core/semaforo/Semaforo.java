package br.com.calibra.core.semaforo;

import static br.com.calibra.core.util.NumeroBr.formatar;
import static br.com.calibra.core.util.NumeroBr.percentual;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classificação de cada indicador da SPEC 7.4. Fronteiras: "até X" inclui X no verde;
 * "X ou mais" inclui X no verde.
 */
public final class Semaforo {

    /** Folga para erros de arredondamento nas fronteiras (ex.: 22 × 1,15 = 25,2999...). */
    private static final double FOLGA_NUMERICA = 1e-9;

    private Semaforo() {
    }

    /** Valor até o limite, inclusive. */
    private static boolean ate(double valor, double limite) {
        return valor <= limite + FOLGA_NUMERICA;
    }

    /** Valor a partir do limite, inclusive. */
    private static boolean aPartirDe(double valor, double limite) {
        return valor >= limite - FOLGA_NUMERICA;
    }

    /** DC do bico, com tempo morto, na rotação de potência máxima. */
    public static Avaliacao dcBico(double dc, LimiaresSemaforo l) {
        String valor = percentual(dc, 1);
        if (ate(dc, l.dcVerdeAte())) {
            return new Avaliacao(Indicador.DC_BICO, Cor.VERDE, dc, "fração",
                    "DC de " + valor + ", dentro do limite de " + percentual(l.dcVerdeAte(), 0) + ".");
        }
        if (ate(dc, l.dcAmareloAte())) {
            return new Avaliacao(Indicador.DC_BICO, Cor.AMARELO, dc, "fração",
                    "DC de " + valor + ", entre " + percentual(l.dcVerdeAte(), 0) + " e "
                            + percentual(l.dcAmareloAte(), 0) + ": bico perto do limite.");
        }
        return new Avaliacao(Indicador.DC_BICO, Cor.VERMELHO, dc, "fração",
                "DC de " + valor + ", acima de " + percentual(l.dcAmareloAte(), 0)
                        + ": escolher bico maior ou subir a pressão de base.");
    }

    /** Folga da bomba sobre a vazão necessária (0,30 = 30% a mais). */
    public static Avaliacao folgaBomba(double folga, LimiaresSemaforo l) {
        String valor = percentual(folga, 0);
        if (aPartirDe(folga, l.folgaBombaVerde())) {
            return new Avaliacao(Indicador.FOLGA_BOMBA, Cor.VERDE, folga, "fração",
                    "Folga de " + valor + " (verde a partir de " + percentual(l.folgaBombaVerde(), 0) + ").");
        }
        if (aPartirDe(folga, l.folgaBombaAmarelo())) {
            return new Avaliacao(Indicador.FOLGA_BOMBA, Cor.AMARELO, folga, "fração",
                    "Folga de " + valor + ", entre " + percentual(l.folgaBombaAmarelo(), 0) + " e "
                            + percentual(l.folgaBombaVerde(), 0) + ".");
        }
        return new Avaliacao(Indicador.FOLGA_BOMBA, Cor.VERMELHO, folga, "fração",
                "Folga de " + valor + ", abaixo de " + percentual(l.folgaBombaAmarelo(), 0)
                        + ": bomba insuficiente para a meta.");
    }

    /** Sem mapa do compressor cadastrado o ponto não pode ser conferido: amarelo (SPEC 7.4). */
    public static Avaliacao compressorSemMapa() {
        return new Avaliacao(Indicador.MAPA_COMPRESSOR, Cor.AMARELO, Double.NaN, "",
                "Turbo sem mapa cadastrado: conferir o ponto de operação (vazão e PR) no mapa do compressor do fabricante.");
    }

    /**
     * BMEP contra a referência (13 bar aspirado, 22 bar sobrealimentado). Acima de 15% da referência:
     * vermelho com pistões ou bielas originais; com os dois forjados, amarelo com aviso.
     */
    public static Avaliacao bmep(double bmepBar, boolean sobrealimentado, boolean pistoesForjados,
            boolean bielasForjadas, LimiaresSemaforo l) {
        double referencia = l.bmepReferenciaBar(sobrealimentado);
        String tipo = sobrealimentado ? "sobrealimentado" : "aspirado";
        String inicio = "BMEP de " + formatar(bmepBar, 1) + " bar";
        String ref = "referência de " + formatar(referencia, 0) + " bar (" + tipo + ")";
        String acima = percentual(l.bmepFatorAmarelo() - 1.0, 0);
        if (ate(bmepBar, referencia)) {
            return new Avaliacao(Indicador.BMEP, Cor.VERDE, bmepBar, "bar", inicio + ", até a " + ref + ".");
        }
        if (ate(bmepBar, referencia * l.bmepFatorAmarelo())) {
            return new Avaliacao(Indicador.BMEP, Cor.AMARELO, bmepBar, "bar",
                    inicio + ", até " + acima + " acima da " + ref + ": confirmar os internos.");
        }
        if (pistoesForjados && bielasForjadas) {
            return new Avaliacao(Indicador.BMEP, Cor.AMARELO, bmepBar, "bar",
                    inicio + ", mais de " + acima + " acima da " + ref
                            + ". Pistões e bielas forjados informados: confirmar o projeto dos internos.");
        }
        return new Avaliacao(Indicador.BMEP, Cor.VERMELHO, bmepBar, "bar",
                inicio + ", mais de " + acima + " acima da " + ref + " com pistões ou bielas originais.");
    }

    public static Avaliacao velocidadePistao(double metrosPorSegundo, LimiaresSemaforo l) {
        String inicio = "Velocidade média do pistão de " + formatar(metrosPorSegundo, 1) + " m/s";
        if (ate(metrosPorSegundo, l.pistaoVerdeAte())) {
            return new Avaliacao(Indicador.VELOCIDADE_PISTAO, Cor.VERDE, metrosPorSegundo, "m/s",
                    inicio + " (até " + formatar(l.pistaoVerdeAte(), 0) + " m/s).");
        }
        if (ate(metrosPorSegundo, l.pistaoAmareloAte())) {
            return new Avaliacao(Indicador.VELOCIDADE_PISTAO, Cor.AMARELO, metrosPorSegundo, "m/s",
                    inicio + ", entre " + formatar(l.pistaoVerdeAte(), 0) + " e "
                            + formatar(l.pistaoAmareloAte(), 0) + " m/s.");
        }
        return new Avaliacao(Indicador.VELOCIDADE_PISTAO, Cor.VERMELHO, metrosPorSegundo, "m/s",
                inicio + ", acima de " + formatar(l.pistaoAmareloAte(), 0) + " m/s.");
    }

    /**
     * Saídas da injeção: suficientes = verde; insuficientes = vermelho (SPEC 6.3 e 7.4).
     *
     * @param maxCilindrosSequencial limite da injeção em sequencial, ou null se não informado
     * @param sequencial o motor usa injeção ou ignição sequencial
     */
    public static Avaliacao saidasInjecao(String nomeInjecao, int bicosExigidos, int bicosDisponiveis,
            int ignicaoExigida, int ignicaoDisponivel, Integer maxCilindrosSequencial, int cilindros,
            boolean sequencial) {
        List<String> faltas = new ArrayList<>();
        if (bicosDisponiveis < bicosExigidos) {
            faltas.add("A injeção " + nomeInjecao + " tem " + bicosDisponiveis
                    + " saídas de bico; este motor pede " + bicosExigidos + ".");
        }
        if (ignicaoDisponivel < ignicaoExigida) {
            faltas.add("A injeção " + nomeInjecao + " tem " + ignicaoDisponivel
                    + " saídas de ignição; este motor pede " + ignicaoExigida + ".");
        }
        if (sequencial && maxCilindrosSequencial != null && cilindros > maxCilindrosSequencial) {
            faltas.add("A injeção " + nomeInjecao + " controla até " + maxCilindrosSequencial
                    + " cilindros em sequencial; este motor tem " + cilindros + ".");
        }
        if (faltas.isEmpty()) {
            return new Avaliacao(Indicador.SAIDAS_INJECAO, Cor.VERDE, Double.NaN, "",
                    "A injeção " + nomeInjecao + " atende: " + bicosDisponiveis + " saídas de bico (pede "
                            + bicosExigidos + ") e " + ignicaoDisponivel + " de ignição (pede " + ignicaoExigida + ").");
        }
        return new Avaliacao(Indicador.SAIDAS_INJECAO, Cor.VERMELHO, Double.NaN, "",
                String.join(" ", faltas) + " Use drivers externos ou outra injeção que atenda.");
    }

    /** Faixa do sensor MAP comparada com o MAP máximo previsto (as duas pressões absolutas). */
    public static Avaliacao faixaSensorMap(double faixaKpa, double mapMaximoKpa, LimiaresSemaforo l) {
        double razao = faixaKpa / mapMaximoKpa;
        String inicio = "Sensor de " + formatar(faixaKpa, 0) + " kPa = " + formatar(razao, 2)
                + " × o MAP máximo previsto (" + formatar(mapMaximoKpa, 0) + " kPa)";
        if (aPartirDe(razao, l.sensorMapVerde())) {
            return new Avaliacao(Indicador.FAIXA_SENSOR_MAP, Cor.VERDE, razao, "×", inicio + ".");
        }
        if (aPartirDe(razao, l.sensorMapAmarelo())) {
            return new Avaliacao(Indicador.FAIXA_SENSOR_MAP, Cor.AMARELO, razao, "×",
                    inicio + ": pouca folga (verde a partir de " + formatar(l.sensorMapVerde(), 1) + " ×).");
        }
        return new Avaliacao(Indicador.FAIXA_SENSOR_MAP, Cor.VERMELHO, razao, "×",
                inicio + ": o sensor não lê a pressão máxima prevista.");
    }

    /** Aspirado: MAP necessário dividido pela pressão atmosférica local. */
    public static Avaliacao aspiradoMapPatm(double razao, LimiaresSemaforo l) {
        String inicio = "MAP necessário = " + formatar(razao, 3) + " × a P_atm local";
        if (ate(razao, l.aspiradoVerdeAte())) {
            return new Avaliacao(Indicador.ASPIRADO_MAP_PATM, Cor.VERDE, razao, "×", inicio + ".");
        }
        if (ate(razao, l.aspiradoAmareloAte())) {
            return new Avaliacao(Indicador.ASPIRADO_MAP_PATM, Cor.AMARELO, razao, "×",
                    inicio + ": perto do limite do aspirado.");
        }
        return new Avaliacao(Indicador.ASPIRADO_MAP_PATM, Cor.VERMELHO, razao, "×",
                inicio + ": meta acima do possível para aspirado com essa VE.");
    }

    /** Indicadores vermelhos que ainda não têm justificativa escrita (regra 2.4). */
    public static List<Indicador> vermelhosSemJustificativa(List<Avaliacao> avaliacoes,
            List<Justificativa> justificativas) {
        Set<Indicador> justificados = justificativas.stream()
                .map(Justificativa::indicador)
                .collect(Collectors.toSet());
        return avaliacoes.stream()
                .filter(a -> a.cor() == Cor.VERMELHO)
                .map(Avaliacao::indicador)
                .filter(i -> !justificados.contains(i))
                .toList();
    }
}
