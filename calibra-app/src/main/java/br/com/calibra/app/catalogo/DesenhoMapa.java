package br.com.calibra.app.catalogo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import br.com.calibra.core.pecas.CalibracaoImagem;
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.pecas.UnidadeVazao;

/**
 * O mapa sendo desenhado por cima da imagem do fabricante, em pixels: a linha de surge, a de choke, as ilhas de
 * eficiência e as linhas de rotação, na ordem em que os pontos foram clicados (para desfazer). Com a calibração dos
 * eixos, vira o {@link MapaCompressor} do núcleo. Sem JavaFX.
 */
public final class DesenhoMapa {

    /** O que está sendo marcado. */
    public enum TipoElemento {
        SURGE("Linha de surge"),
        CHOKE("Linha de choke"),
        ILHA("Ilha de eficiência"),
        ROTACAO("Linha de rotação");

        private final String rotulo;

        TipoElemento(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    /** Um ponto clicado na imagem. */
    public record PontoPixel(double x, double y) {
    }

    /**
     * Um elemento do mapa.
     *
     * @param valor a eficiência da ilha (fração) ou o rpm da linha; NaN no surge e no choke
     */
    public record Elemento(TipoElemento tipo, double valor, List<PontoPixel> pontos) {
    }

    private final List<ElementoAberto> elementos = new ArrayList<>();
    private final Deque<ElementoAberto> ordem = new ArrayDeque<>();
    private ElementoAberto atual;

    private static final class ElementoAberto {
        private final TipoElemento tipo;
        private final double valor;
        private final List<PontoPixel> pontos = new ArrayList<>();

        ElementoAberto(TipoElemento tipo, double valor) {
            this.tipo = tipo;
            this.valor = valor;
        }

        Elemento fechado() {
            return new Elemento(tipo, valor, List.copyOf(pontos));
        }
    }

    /**
     * Começa a marcar um elemento. O surge e o choke são uma linha só cada: voltar a eles continua a linha que já
     * existe. Cada ilha e cada linha de rotação é um elemento novo.
     *
     * @param valor eficiência da ilha (0,76 = 76%) ou rpm da linha; ignorado no surge e no choke
     */
    public void comecar(TipoElemento tipo, double valor) {
        if (tipo == TipoElemento.SURGE || tipo == TipoElemento.CHOKE) {
            atual = elementos.stream().filter(e -> e.tipo == tipo).findFirst().orElse(null);
            if (atual == null) {
                atual = new ElementoAberto(tipo, Double.NaN);
                elementos.add(atual);
            }
            return;
        }
        atual = new ElementoAberto(tipo, valor);
        elementos.add(atual);
    }

    /** O que está sendo marcado agora; null antes de escolher. */
    public TipoElemento marcando() {
        return atual == null ? null : atual.tipo;
    }

    /** Acrescenta um ponto ao elemento que está sendo marcado. */
    public void adicionar(double x, double y) {
        if (atual == null) {
            throw new IllegalStateException("Escolha o que marcar (surge, choke, ilha ou linha de rotação).");
        }
        if (!elementos.contains(atual)) {
            elementos.add(atual);
        }
        atual.pontos.add(new PontoPixel(x, y));
        ordem.push(atual);
    }

    /** Tira o último ponto clicado; um elemento que fica sem pontos sai do desenho. */
    public boolean desfazer() {
        ElementoAberto ultimo = ordem.poll();
        if (ultimo == null) {
            return false;
        }
        ultimo.pontos.removeLast();
        if (ultimo.pontos.isEmpty()) {
            elementos.remove(ultimo);
        }
        return true;
    }

    /** Os elementos com pelo menos um ponto, na ordem em que foram começados. */
    public List<Elemento> elementos() {
        return elementos.stream().filter(e -> !e.pontos.isEmpty()).map(ElementoAberto::fechado).toList();
    }

    /**
     * O mapa do núcleo: cada pixel vira vazão (no eixo original, depois kg/s) e razão de pressão pela calibração.
     *
     * @param rotacaoMaximaRpm opcional
     */
    public MapaCompressor paraMapa(CalibracaoImagem calibracao, UnidadeVazao eixo, double temperaturaReferenciaK,
            double pressaoReferenciaKpa, Double rotacaoMaximaRpm) {
        List<MapaCompressor.PontoMapa> surge = new ArrayList<>();
        List<MapaCompressor.PontoMapa> choke = new ArrayList<>();
        List<MapaCompressor.Ilha> ilhas = new ArrayList<>();
        List<MapaCompressor.LinhaRotacao> rotacoes = new ArrayList<>();
        for (Elemento elemento : elementos()) {
            List<MapaCompressor.PontoMapa> pontos = elemento.pontos().stream()
                    .map(p -> new MapaCompressor.PontoMapa(eixo.paraKgS(calibracao.vazao(p.x())),
                            calibracao.razaoPressao(p.y())))
                    .toList();
            switch (elemento.tipo()) {
                case SURGE -> surge.addAll(pontos);
                case CHOKE -> choke.addAll(pontos);
                case ILHA -> ilhas.add(new MapaCompressor.Ilha(elemento.valor(), pontos));
                case ROTACAO -> rotacoes.add(new MapaCompressor.LinhaRotacao(elemento.valor(), pontos));
            }
        }
        return new MapaCompressor(eixo, temperaturaReferenciaK, pressaoReferenciaKpa, surge, choke, ilhas, rotacoes,
                rotacaoMaximaRpm);
    }

    /** Um desenho a partir de um mapa gravado, para continuar por cima da mesma imagem (os pontos voltam a pixels). */
    public static DesenhoMapa de(MapaCompressor mapa, CalibracaoImagem calibracao) {
        DesenhoMapa desenho = new DesenhoMapa();
        desenho.abrir(TipoElemento.SURGE, Double.NaN, mapa.surge(), mapa.eixoOriginal(), calibracao);
        desenho.abrir(TipoElemento.CHOKE, Double.NaN, mapa.choke(), mapa.eixoOriginal(), calibracao);
        mapa.ilhas().forEach(ilha -> desenho.abrir(TipoElemento.ILHA, ilha.eficiencia(), ilha.contorno(),
                mapa.eixoOriginal(), calibracao));
        mapa.rotacoes().forEach(linha -> desenho.abrir(TipoElemento.ROTACAO, linha.rpm(), linha.pontos(),
                mapa.eixoOriginal(), calibracao));
        desenho.atual = null;
        return desenho;
    }

    private void abrir(TipoElemento tipo, double valor, List<MapaCompressor.PontoMapa> pontos, UnidadeVazao eixo,
            CalibracaoImagem calibracao) {
        if (pontos.isEmpty()) {
            return;
        }
        comecar(tipo, valor);
        for (MapaCompressor.PontoMapa ponto : pontos) {
            adicionar(calibracao.xPixel(eixo.deKgS(ponto.vazaoKgS())), calibracao.yPixel(ponto.razaoPressao()));
        }
    }
}
