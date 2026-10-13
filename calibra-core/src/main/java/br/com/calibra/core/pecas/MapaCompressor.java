package br.com.calibra.core.pecas;

import static br.com.calibra.core.util.NumeroBr.formatar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

import br.com.calibra.core.util.Interpolacao;

/**
 * Mapa do compressor digitalizado da imagem do fabricante (SPEC 7.3): linhas de surge e de choke, ilhas de eficiência
 * e linhas de rotação, no plano vazão corrigida × razão de pressão. Os pontos ficam em kg/s (SI); o eixo original só
 * serve para mostrar o mapa como o fabricante desenhou. A temperatura e a pressão de referência são as do próprio
 * mapa: cada fabricante corrige a vazão com as suas.
 *
 * @param eixoOriginal unidade do eixo de vazão na imagem do fabricante
 * @param temperaturaReferenciaK ex.: Garrett, 545 °R = 302,8 K
 * @param pressaoReferenciaKpa ex.: Garrett, 13,95 psia = 96,2 kPa
 * @param rotacaoMaximaRpm rotação máxima do turbo; opcional
 */
public record MapaCompressor(
        UnidadeVazao eixoOriginal,
        double temperaturaReferenciaK,
        double pressaoReferenciaKpa,
        List<PontoMapa> surge,
        List<PontoMapa> choke,
        List<Ilha> ilhas,
        List<LinhaRotacao> rotacoes,
        Double rotacaoMaximaRpm) {

    /** Diferença de PR abaixo da qual o ponto é considerado na fronteira (dígitos da digitalização). */
    private static final double TOLERANCIA_PR = 1e-9;

    /** Um ponto do mapa: vazão corrigida em kg/s e razão de pressão. */
    public record PontoMapa(double vazaoKgS, double razaoPressao) {
    }

    /** Uma ilha de eficiência: contorno fechado e a eficiência dele (0,76 = 76%). */
    public record Ilha(double eficiencia, List<PontoMapa> contorno) {

        public Ilha {
            contorno = List.copyOf(contorno);
        }
    }

    /** Uma linha de rotação do turbo, com os pontos da esquerda para a direita. */
    public record LinhaRotacao(double rpm, List<PontoMapa> pontos) {

        public LinhaRotacao {
            pontos = List.copyOf(pontos);
        }
    }

    /**
     * Onde um ponto cai no mapa.
     *
     * @param foraDoMapa por que o ponto está fora, em português; null se está dentro
     * @param margemSurge (vazão − vazão do surge) ÷ vazão do surge, na mesma PR; NaN fora do mapa
     * @param margemChoke (vazão do choke − vazão) ÷ vazão do choke, na mesma PR; NaN fora do mapa
     * @param rotacaoRpm rotação interpolada entre as duas linhas vizinhas; null sem linhas que cubram a vazão, abaixo
     *     da menor linha ou acima da maior
     * @param acimaDasLinhas o ponto passa da maior linha de rotação desenhada
     * @param eficiencia a da ilha mais interna que contém o ponto; null fora das ilhas
     */
    public record PontoNoMapa(double vazaoCorrigidaKgS, double razaoPressao, String foraDoMapa, double margemSurge,
            double margemChoke, Double rotacaoRpm, boolean acimaDasLinhas, Double eficiencia) {

        public boolean dentro() {
            return foraDoMapa == null;
        }
    }

    public MapaCompressor {
        Objects.requireNonNull(eixoOriginal, "eixoOriginal");
        surge = List.copyOf(surge);
        choke = List.copyOf(choke);
        ilhas = List.copyOf(ilhas);
        rotacoes = List.copyOf(rotacoes);
    }

    /** O que falta ou não fecha no mapa, em português (vazio = mapa usável). */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (!(temperaturaReferenciaK > 0) || !(pressaoReferenciaKpa > 0)) {
            erros.add("Informe a temperatura e a pressão de referência do mapa (estão na legenda do fabricante).");
        }
        if (surge.size() < 2) {
            erros.add("Marque pelo menos 2 pontos na linha de surge.");
        }
        if (choke.size() < 2) {
            erros.add("Marque pelo menos 2 pontos na linha de choke.");
        }
        for (Ilha ilha : ilhas) {
            if (ilha.contorno().size() < 3) {
                erros.add("A ilha de " + formatar(ilha.eficiencia() * 100, 0, 1) + "% precisa de pelo menos 3 pontos.");
            }
            if (!(ilha.eficiencia() > 0 && ilha.eficiencia() <= 1)) {
                erros.add("A eficiência de cada ilha fica entre 0 e 1 (0,76 = 76%).");
            }
        }
        for (LinhaRotacao linha : rotacoes) {
            if (linha.pontos().size() < 2) {
                erros.add("A linha de " + formatar(linha.rpm(), 0) + " rpm precisa de pelo menos 2 pontos.");
            }
            if (!(linha.rpm() > 0)) {
                erros.add("A rotação de cada linha deve ser maior que zero.");
            }
        }
        if (rotacaoMaximaRpm != null && !(rotacaoMaximaRpm > 0)) {
            erros.add("A rotação máxima do turbo deve ser maior que zero.");
        }
        boolean pontosValidos = todos().stream().allMatch(p -> p.vazaoKgS() > 0 && p.razaoPressao() > 0);
        if (!pontosValidos) {
            erros.add("Todos os pontos do mapa precisam de vazão e razão de pressão maiores que zero.");
        }
        return erros.stream().distinct().toList();
    }

    /** Eficiência da ilha mais interna (o pico do mapa); null se nenhuma ilha foi marcada. */
    public Double eficienciaPico() {
        return ilhas.stream().map(Ilha::eficiencia).max(Double::compare).orElse(null);
    }

    /**
     * Onde o ponto cai no mapa. Fora do mapa: acima do topo das linhas de surge e de choke, à esquerda do surge, à
     * direita do choke ou acima da rotação máxima. Abaixo do primeiro ponto marcado, o surge e o choke valem o
     * primeiro ponto (as linhas descem quase retas até PR 1).
     */
    public PontoNoMapa avaliar(double vazaoCorrigidaKgS, double razaoPressao) {
        double topo = Math.min(maiorPr(surge), maiorPr(choke));
        if (razaoPressao > topo + TOLERANCIA_PR) {
            return fora(vazaoCorrigidaKgS, razaoPressao, "PR " + formatar(razaoPressao, 2)
                    + " acima do topo do mapa (" + formatar(topo, 2) + ")");
        }
        double vazaoSurge = vazaoNaLinha(surge, razaoPressao);
        double vazaoChoke = vazaoNaLinha(choke, razaoPressao);
        if (vazaoCorrigidaKgS < vazaoSurge) {
            return fora(vazaoCorrigidaKgS, razaoPressao, "à esquerda da linha de surge");
        }
        if (vazaoCorrigidaKgS > vazaoChoke) {
            return fora(vazaoCorrigidaKgS, razaoPressao, "à direita da linha de choke");
        }
        Rotacao rotacao = rotacao(vazaoCorrigidaKgS, razaoPressao);
        if (rotacaoMaximaRpm != null && rotacao.rpm() != null && rotacao.rpm() > rotacaoMaximaRpm) {
            return fora(vazaoCorrigidaKgS, razaoPressao, "acima da rotação máxima do turbo ("
                    + formatar(rotacaoMaximaRpm, 0) + " rpm)");
        }
        if (rotacaoMaximaRpm != null && rotacao.acima() && rotacao.maiorLinhaRpm() >= rotacaoMaximaRpm) {
            return fora(vazaoCorrigidaKgS, razaoPressao, "acima da linha de rotação máxima ("
                    + formatar(rotacaoMaximaRpm, 0) + " rpm)");
        }
        Double eficiencia = ilhas.stream()
                .filter(ilha -> dentroDoContorno(ilha.contorno(), vazaoCorrigidaKgS, razaoPressao))
                .map(Ilha::eficiencia).max(Double::compare).orElse(null);
        return new PontoNoMapa(vazaoCorrigidaKgS, razaoPressao, null, (vazaoCorrigidaKgS - vazaoSurge) / vazaoSurge,
                (vazaoChoke - vazaoCorrigidaKgS) / vazaoChoke, rotacao.rpm(), rotacao.acima(), eficiencia);
    }

    // ===================== Contas do mapa =====================

    private static PontoNoMapa fora(double vazao, double pr, String motivo) {
        return new PontoNoMapa(vazao, pr, motivo, Double.NaN, Double.NaN, null, false, null);
    }

    private static double maiorPr(List<PontoMapa> linha) {
        return linha.stream().mapToDouble(PontoMapa::razaoPressao).max().orElse(0.0);
    }

    /** Vazão de uma linha (surge ou choke) na PR pedida; abaixo do primeiro ponto, a do primeiro ponto. */
    private static double vazaoNaLinha(List<PontoMapa> linha, double pr) {
        List<PontoMapa> ordenada = new ArrayList<>(linha);
        ordenada.sort(Comparator.comparingDouble(PontoMapa::razaoPressao));
        if (pr <= ordenada.getFirst().razaoPressao()) {
            return ordenada.getFirst().vazaoKgS();
        }
        if (pr >= ordenada.getLast().razaoPressao()) {
            return ordenada.getLast().vazaoKgS();
        }
        OptionalDouble vazao = Interpolacao.linear(ordenada.stream()
                .map(p -> new Interpolacao.Ponto(p.razaoPressao(), p.vazaoKgS())).toList(), pr);
        return vazao.orElseThrow();
    }

    /** A rotação no ponto, entre as linhas que cobrem a vazão; o maior rpm dessas linhas, para a rotação máxima. */
    private record Rotacao(Double rpm, boolean acima, double maiorLinhaRpm) {
    }

    /**
     * Interpola a rotação na PR do ponto, entre as linhas vizinhas: cada linha dá a sua PR na vazão do ponto, e as
     * linhas de rotação maior ficam mais altas no mapa.
     */
    private Rotacao rotacao(double vazao, double pr) {
        List<double[]> naVazao = new ArrayList<>();
        for (LinhaRotacao linha : rotacoes) {
            OptionalDouble prDaLinha = Interpolacao.linear(linha.pontos().stream()
                    .map(p -> new Interpolacao.Ponto(p.vazaoKgS(), p.razaoPressao())).toList(), vazao);
            prDaLinha.ifPresent(valor -> naVazao.add(new double[] {linha.rpm(), valor}));
        }
        if (naVazao.isEmpty()) {
            return new Rotacao(null, false, 0.0);
        }
        naVazao.sort(Comparator.comparingDouble(linha -> linha[0]));
        double[] menor = naVazao.getFirst();
        double[] maior = naVazao.getLast();
        if (pr > maior[1]) {
            return new Rotacao(null, true, maior[0]);
        }
        if (pr < menor[1]) {
            return new Rotacao(null, false, maior[0]);
        }
        for (int i = 0; i + 1 < naVazao.size(); i++) {
            double[] baixo = naVazao.get(i);
            double[] alto = naVazao.get(i + 1);
            if (pr >= baixo[1] && pr <= alto[1]) {
                double fracao = alto[1] == baixo[1] ? 0.0 : (pr - baixo[1]) / (alto[1] - baixo[1]);
                return new Rotacao(baixo[0] + fracao * (alto[0] - baixo[0]), false, maior[0]);
            }
        }
        // Linhas cruzadas (erro de digitalização): sem rotação confiável.
        return new Rotacao(null, false, maior[0]);
    }

    /** Ponto dentro do contorno fechado (raio para a direita, contando cruzamentos). */
    static boolean dentroDoContorno(List<PontoMapa> contorno, double vazao, double pr) {
        boolean dentro = false;
        for (int i = 0, j = contorno.size() - 1; i < contorno.size(); j = i++) {
            PontoMapa a = contorno.get(i);
            PontoMapa b = contorno.get(j);
            boolean cruza = (a.razaoPressao() > pr) != (b.razaoPressao() > pr)
                    && vazao < (b.vazaoKgS() - a.vazaoKgS()) * (pr - a.razaoPressao())
                            / (b.razaoPressao() - a.razaoPressao()) + a.vazaoKgS();
            if (cruza) {
                dentro = !dentro;
            }
        }
        return dentro;
    }

    private List<PontoMapa> todos() {
        List<PontoMapa> todos = new ArrayList<>(surge);
        todos.addAll(choke);
        ilhas.forEach(ilha -> todos.addAll(ilha.contorno()));
        rotacoes.forEach(linha -> todos.addAll(linha.pontos()));
        return todos;
    }
}
