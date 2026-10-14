package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.pecas.UnidadeVazao;
import br.com.calibra.core.util.NumeroBr;

/**
 * O mapa do compressor desenhado a partir dos dados gravados (sem a imagem do fabricante): surge, choke, ilhas e
 * linhas de rotação, no eixo de vazão em que o mapa foi digitalizado, com os pontos de potência e de torque do
 * cálculo (vazão corrigida pela referência do mapa).
 */
final class GraficoMapa {

    private static final double ESQUERDA = 58;
    private static final double DIREITA = 24;
    private static final double TOPO = 28;
    private static final double BAIXO = 46;

    private static final Color COR_SURGE = Color.web("#c62828");
    private static final Color COR_CHOKE = Color.web("#1565c0");
    private static final Color COR_ILHA = Color.web("#2e7d32");
    private static final Color COR_ROTACAO = Color.web("#6d4c41");
    private static final Color COR_POTENCIA = Color.web("#212121");
    private static final Color COR_TORQUE = Color.web("#ef6c00");
    private static final Color COR_GRADE = Color.web("#e0e0e0");

    /** Uma escala com marcas redondas (1, 2 ou 5 × 10ⁿ). */
    private record Escala(double minimo, double maximo, double passo) {

        static Escala de(double minimo, double maximo) {
            double bruto = (maximo - minimo) / 6.0;
            double potencia = Math.pow(10, Math.floor(Math.log10(bruto)));
            double fator = bruto / potencia;
            double passo = (fator < 1.5 ? 1 : fator < 3 ? 2 : fator < 7 ? 5 : 10) * potencia;
            return new Escala(Math.floor(minimo / passo) * passo, Math.ceil(maximo / passo) * passo, passo);
        }
    }

    private final MapaCompressor mapa;
    private final UnidadeVazao eixo;
    private final double largura;
    private final double altura;
    private Escala x;
    private Escala y;

    private GraficoMapa(MapaCompressor mapa, double largura, double altura) {
        this.mapa = mapa;
        this.eixo = mapa.eixoOriginal();
        this.largura = largura;
        this.altura = altura;
    }

    /**
     * @param torque null sem a rotação de torque máximo
     */
    static Canvas desenhar(MapaCompressor mapa, MapaCompressor.PontoNoMapa potencia, MapaCompressor.PontoNoMapa torque,
            double largura, double altura) {
        GraficoMapa grafico = new GraficoMapa(mapa, largura, altura);
        Canvas tela = new Canvas(largura, altura);
        tela.setId("graficoMapa");
        grafico.escalas(potencia, torque);
        GraphicsContext g = tela.getGraphicsContext2D();
        grafico.grade(g);
        grafico.elementos(g);
        grafico.ponto(g, potencia, "potência", COR_POTENCIA, false);
        if (torque != null) {
            grafico.ponto(g, torque, "torque", COR_TORQUE, true);
        }
        grafico.legenda(g, torque != null);
        return tela;
    }

    private void escalas(MapaCompressor.PontoNoMapa potencia, MapaCompressor.PontoNoMapa torque) {
        List<MapaCompressor.PontoMapa> todos = new ArrayList<>(mapa.surge());
        todos.addAll(mapa.choke());
        mapa.ilhas().forEach(ilha -> todos.addAll(ilha.contorno()));
        mapa.rotacoes().forEach(linha -> todos.addAll(linha.pontos()));
        todos.add(new MapaCompressor.PontoMapa(potencia.vazaoCorrigidaKgS(), potencia.razaoPressao()));
        if (torque != null) {
            todos.add(new MapaCompressor.PontoMapa(torque.vazaoCorrigidaKgS(), torque.razaoPressao()));
        }
        double vazaoMaxima = todos.stream().mapToDouble(p -> eixo.deKgS(p.vazaoKgS())).max().orElse(1);
        double prMinima = todos.stream().mapToDouble(MapaCompressor.PontoMapa::razaoPressao).min().orElse(1);
        double prMaxima = todos.stream().mapToDouble(MapaCompressor.PontoMapa::razaoPressao).max().orElse(2);
        x = Escala.de(0, vazaoMaxima * 1.08);
        y = Escala.de(Math.min(1.0, prMinima), prMaxima * 1.05);
    }

    private double px(double vazaoKgS) {
        double v = eixo.deKgS(vazaoKgS);
        return ESQUERDA + (v - x.minimo()) / (x.maximo() - x.minimo()) * (largura - ESQUERDA - DIREITA);
    }

    private double py(double pr) {
        return altura - BAIXO - (pr - y.minimo()) / (y.maximo() - y.minimo()) * (altura - TOPO - BAIXO);
    }

    private void grade(GraphicsContext g) {
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, largura, altura);
        g.setFont(Font.font(11));
        g.setLineWidth(1);
        g.setTextBaseline(VPos.CENTER);
        for (double v = x.minimo(); v <= x.maximo() + x.passo() / 2; v += x.passo()) {
            double posicao = px(eixo.paraKgS(v));
            g.setStroke(COR_GRADE);
            g.strokeLine(posicao, TOPO, posicao, altura - BAIXO);
            g.setFill(Color.BLACK);
            g.setTextAlign(TextAlignment.CENTER);
            g.fillText(NumeroBr.formatar(v, 0, 2), posicao, altura - BAIXO + 12);
        }
        for (double pr = y.minimo(); pr <= y.maximo() + y.passo() / 2; pr += y.passo()) {
            double posicao = py(pr);
            g.setStroke(COR_GRADE);
            g.strokeLine(ESQUERDA, posicao, largura - DIREITA, posicao);
            g.setFill(Color.BLACK);
            g.setTextAlign(TextAlignment.RIGHT);
            g.fillText(NumeroBr.formatar(pr, 1, 2), ESQUERDA - 6, posicao);
        }
        g.setStroke(Color.BLACK);
        g.strokeRect(ESQUERDA, TOPO, largura - ESQUERDA - DIREITA, altura - TOPO - BAIXO);
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText("Vazão corrigida (" + eixo.simbolo() + ")", (ESQUERDA + largura - DIREITA) / 2, altura - 14);
        g.fillText("PR", 16, (TOPO + altura - BAIXO) / 2);
    }

    private void elementos(GraphicsContext g) {
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.BOTTOM);
        for (MapaCompressor.Ilha ilha : mapa.ilhas()) {
            linha(g, ilha.contorno(), COR_ILHA, 1.5, true);
            MapaCompressor.PontoMapa primeiro = ilha.contorno().getFirst();
            g.fillText(NumeroBr.formatar(ilha.eficiencia() * 100, 0, 1) + "%", px(primeiro.vazaoKgS()) + 4,
                    py(primeiro.razaoPressao()) - 2);
        }
        g.setLineDashes(6, 4);
        for (MapaCompressor.LinhaRotacao rotacao : mapa.rotacoes()) {
            linha(g, rotacao.pontos(), COR_ROTACAO, 1.2, false);
            MapaCompressor.PontoMapa ultimo = rotacao.pontos().getLast();
            g.fillText(NumeroBr.formatar(rotacao.rpm(), 0), px(ultimo.vazaoKgS()) + 4, py(ultimo.razaoPressao()) - 2);
        }
        g.setLineDashes();
        linha(g, mapa.surge(), COR_SURGE, 2.5, false);
        linha(g, mapa.choke(), COR_CHOKE, 2.5, false);
    }

    private void linha(GraphicsContext g, List<MapaCompressor.PontoMapa> pontos, Color cor, double espessura,
            boolean fechada) {
        if (pontos.isEmpty()) {
            return;
        }
        g.setStroke(cor);
        g.setFill(cor);
        g.setLineWidth(espessura);
        g.beginPath();
        g.moveTo(px(pontos.getFirst().vazaoKgS()), py(pontos.getFirst().razaoPressao()));
        for (MapaCompressor.PontoMapa ponto : pontos.subList(1, pontos.size())) {
            g.lineTo(px(ponto.vazaoKgS()), py(ponto.razaoPressao()));
        }
        if (fechada) {
            g.closePath();
        }
        g.stroke();
    }

    private void ponto(GraphicsContext g, MapaCompressor.PontoNoMapa ponto, String rotulo, Color cor, boolean quadrado) {
        double cx = px(ponto.vazaoCorrigidaKgS());
        double cy = py(ponto.razaoPressao());
        g.setFill(cor);
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.5);
        if (quadrado) {
            g.fillRect(cx - 6, cy - 6, 12, 12);
            g.strokeRect(cx - 6, cy - 6, 12, 12);
        } else {
            g.fillOval(cx - 7, cy - 7, 14, 14);
            g.strokeOval(cx - 7, cy - 7, 14, 14);
        }
        g.setFont(Font.font(null, FontWeight.BOLD, 12));
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(rotulo, cx + 10, cy);
        g.setFont(Font.font(11));
    }

    private void legenda(GraphicsContext g, boolean comTorque) {
        g.setTextBaseline(VPos.CENTER);
        g.setTextAlign(TextAlignment.LEFT);
        double posicao = ESQUERDA;
        posicao = item(g, posicao, COR_SURGE, "surge");
        posicao = item(g, posicao, COR_CHOKE, "choke");
        posicao = item(g, posicao, COR_ILHA, "ilhas (eficiência)");
        posicao = item(g, posicao, COR_ROTACAO, "rotação do turbo (rpm)");
        posicao = item(g, posicao, COR_POTENCIA, "● potência");
        if (comTorque) {
            item(g, posicao, COR_TORQUE, "■ torque");
        }
    }

    private static double item(GraphicsContext g, double posicao, Color cor, String texto) {
        g.setFill(cor);
        g.fillText(texto, posicao, TOPO / 2);
        Text medida = new Text(texto);
        medida.setFont(g.getFont());
        return posicao + medida.getLayoutBounds().getWidth() + 18;
    }
}
