package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import javax.imageio.ImageIO;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.JanelaDigitalizador.Modo;
import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.ImagensTurbo;
import br.com.calibra.app.catalogo.FormasCatalogo;
import br.com.calibra.app.catalogo.UnidadesMapa;
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.pecas.Turbo;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Teste de interface do digitalizador (bloco 2.4, opcional, fora do verify padrão). A imagem do mapa é desenhada
 * aqui mesmo e é FICTÍCIA: não é de nenhum fabricante (nenhum mapa de fabricante vai para o repositório). Calibra os
 * eixos, marca surge, choke, ilhas e linhas de rotação, desfaz, grava a versão nova do turbo e reabre.
 */
@Tag("ui")
class DigitalizadorUiTest {

    // Escala da imagem fictícia: x = 60 + 10 · vazão (lb/min); y = 500 − 160 · (PR − 1).
    private static final int LARGURA = 740;
    private static final int ALTURA = 560;

    private static final double[][] SURGE = {{10, 1.3}, {14, 1.8}, {19, 2.4}, {25, 3.0}, {30, 3.4}};
    private static final double[][] CHOKE = {{40, 1.3}, {46, 1.8}, {50, 2.4}, {53, 3.0}, {54, 3.4}};
    private static final double[][] ILHA_60 = {{14, 1.5}, {30, 1.4}, {45, 1.6}, {50, 2.3}, {46, 3.0}, {30, 3.2},
            {20, 2.6}};
    private static final double[][] ILHA_70 = {{18, 1.7}, {32, 1.6}, {42, 1.9}, {44, 2.5}, {36, 2.9}, {24, 2.6}};
    private static final double[][] ILHA_76 = {{24, 2.0}, {34, 1.9}, {38, 2.3}, {32, 2.6}, {26, 2.4}};
    private static final double[][] LINHA_100K = {{13, 1.75}, {28, 1.65}, {43, 1.5}};
    private static final double[][] LINHA_130K = {{19, 2.4}, {35, 2.3}, {49, 2.1}};
    private static final double[][] LINHA_160K = {{26, 3.05}, {42, 2.95}, {53, 2.7}};

    @TempDir
    Path pasta;

    private ContextoApp contexto;
    private PrincipalController principal;
    private Scene cena;
    private CasosCatalogo casos;
    private ImagensTurbo imagens;
    private Path imagemFicticia;

    @Test
    void digitalizarOMapaDeUmTurbo() throws Throwable {
        try {
            imagemFicticia = desenharMapaFicticio(pasta.resolve("mapa-ficticio.png"));
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(this::digitalizar);
            TelaDeTeste.naTelaDoJavaFx(this::reabrir);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    private void abrir() throws Exception {
        contexto = Partida.abrir(new PastasDados(pasta.resolve("dados")), "0.2.0-teste", Clock.systemDefaultZone());
        casos = new CasosCatalogo(contexto.banco(), contexto.relogio());
        imagens = new ImagensTurbo(contexto.pastas().anexos(), contexto.relogio());
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("principal.fxml"));
        Parent raiz = carregador.load();
        principal = carregador.getController();
        principal.iniciar(contexto, null, new Stage());
        cena = new Scene(raiz, 1380, 860);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());

        casos.criar(TipoCatalogo.TURBO, new Turbo("Turbo fictício T60", null, 0.64, null, null,
                "Mapa fictício desenhado pelo teste", LocalDate.of(2026, 10, 13)),
                new InfoCatalogo("Turbo fictício T60", "Mapa fictício desenhado pelo teste", LocalDate.of(2026, 10, 13),
                        true, null, "Dados fictícios: não é um turbo real."));
        TelaDeTeste.<ToggleButton>achar(cena, "navCatalogos").fire();
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaTURBO"));
        turbos().atualizar();
        Button mapa = achar(cena, "mapaDoCompressor");
        assertTrue(mapa.isDisabled(), "sem turbo escolhido, o botão fica desligado");
        turbos().tabela().getSelectionModel().selectFirst();
        assertFalse(mapa.isDisabled());
        assertEquals("sem mapa", resumo(turbos().tabela().getItems().getFirst().dados()).get(1));
    }

    private void digitalizar() throws Exception {
        ItemCatalogo<Turbo> turbo = turbos().tabela().getItems().getFirst();
        JanelaDigitalizador janela = new JanelaDigitalizador(null, casos, imagens, turbo);
        // Aberta antes dos cliques, como no app (medida fora da janela, a fonte da tela ainda não vale e corta textos).
        janela.dialogo().show();
        try {
            marcarESalvar(janela, turbo);
        } finally {
            janela.dialogo().close();
        }
    }

    private void marcarESalvar(JanelaDigitalizador janela, ItemCatalogo<Turbo> turbo) throws Exception {
        // Sem imagem e sem calibração não grava, e diz o que falta.
        assertFalse(janela.gravar());
        assertTrue(janela.erros().contains("Marque o ponto X1 na imagem."), janela.erros()::toString);
        assertTrue(janela.erros().contains("Carregue a imagem do mapa do fabricante."), janela.erros()::toString);

        janela.carregarImagem(imagemFicticia);
        janela.clicar(100, 100);
        assertEquals(List.of("Escolha antes o que marcar: um ponto de eixo ou um elemento do desenho."), janela.erros());

        // Eixos: X1 no 10 e X2 no 60 da escala de vazão; Y1 no 1,5 e Y2 no 3,5 da razão de pressão.
        marcarEixo(janela, Modo.X1, x(10), 0, "10");
        marcarEixo(janela, Modo.X2, x(60), 0, "60");
        marcarEixo(janela, Modo.Y1, 0, y(1.5), "1,5");
        marcarEixo(janela, Modo.Y2, 0, y(3.5), "3,5");
        janela.temperaturaReferencia().setText("545");
        janela.unidadeTemperatura().setValue(UnidadesMapa.Temperatura.RANKINE);
        janela.pressaoReferencia().setText("13,95");
        janela.unidadePressao().setValue(UnidadesMapa.Pressao.PSIA);
        janela.rotacaoMaxima().setText("160.000");

        Scene dialogo = janela.dialogo().getDialogPane().getScene();
        marcar(janela, dialogo, "marcarSurge", SURGE);
        marcar(janela, dialogo, "marcarChoke", CHOKE);
        ilha(janela, dialogo, "60", ILHA_60);
        ilha(janela, dialogo, "70", ILHA_70);
        ilha(janela, dialogo, "76", ILHA_76);
        linha(janela, dialogo, "100.000", LINHA_100K);
        linha(janela, dialogo, "130.000", LINHA_130K);

        // Um clique errado e o desfazer.
        janela.clicar(700, 40);
        assertEquals(4, janela.desenho().elementos().getLast().pontos().size());
        janela.desfazer();
        assertEquals(3, janela.desenho().elementos().getLast().pontos().size());
        linha(janela, dialogo, "160.000", LINHA_160K);
        assertEquals(8, janela.desenho().elementos().size(), "surge, choke, 3 ilhas e 3 linhas");
        salvar(dialogo, "captura-digitalizador.png");

        assertTrue(janela.gravar(), janela.erros()::toString);
        ItemCatalogo<Turbo> gravado = casos.buscar(TipoCatalogo.TURBO, turbo.id()).orElseThrow();
        assertEquals(2, gravado.versao(), "o mapa entra como versão nova do turbo");
        MapaCompressor mapa = gravado.dados().mapa();
        assertNotNull(mapa);
        assertEquals(Conversoes.lbMinParaKgS(10), mapa.surge().getFirst().vazaoKgS(), 1e-9);
        assertEquals(1.3, mapa.surge().getFirst().razaoPressao(), 1e-9);
        assertEquals(Conversoes.lbMinParaKgS(54), mapa.choke().getLast().vazaoKgS(), 1e-9);
        assertEquals(302.78, mapa.temperaturaReferenciaK(), 0.01, "545 °R");
        assertEquals(96.18, mapa.pressaoReferenciaKpa(), 0.01, "13,95 psia");
        assertEquals(160_000.0, mapa.rotacaoMaximaRpm(), 0.0);
        assertEquals(0.76, mapa.eficienciaPico(), 1e-12);
        // O mapa digitalizado já serve ao cálculo: 30 lb/min a PR 2,2 cai na ilha de 76%, entre 100 e 130 mil rpm.
        MapaCompressor.PontoNoMapa ponto = mapa.avaliar(Conversoes.lbMinParaKgS(30), 2.2);
        assertTrue(ponto.dentro(), ponto::foraDoMapa);
        assertEquals(0.76, ponto.eficiencia(), 1e-12);
        assertTrue(ponto.rotacaoRpm() > 100_000 && ponto.rotacaoRpm() < 130_000, ponto::toString);
        assertTrue(Files.exists(imagens.caminho(gravado.dados().imagemMapa())), "a imagem ficou nos anexos");

        turbos().atualizar();
        turbos().tabela().getSelectionModel().selectFirst();
        assertEquals(List.of("0,64", "digitalizado", "76%", "160.000 rpm"), resumo(gravado.dados()));
        salvar(cena, "captura-catalogo-turbos.png");
    }

    private void reabrir() throws Exception {
        ItemCatalogo<Turbo> gravado = turbos().tabela().getItems().getFirst();
        JanelaDigitalizador janela = new JanelaDigitalizador(null, casos, imagens, gravado);
        assertEquals(8, janela.desenho().elementos().size(), "o desenho volta por cima da mesma imagem");
        assertEquals("10", janela.valorEixo(Modo.X1).getText());
        assertEquals("3,5", janela.valorEixo(Modo.Y2).getText());
        assertEquals("29,63", janela.temperaturaReferencia().getText(), "reabre em °C");
        assertEquals("96,18", janela.pressaoReferencia().getText(), "reabre em kPa");
        assertTrue(janela.erros().isEmpty(), janela.erros()::toString);
        fotografar(janela.dialogo(), "captura-digitalizador-reaberto.png");
    }

    // ===================== Ajudas =====================

    private PainelCatalogo<Turbo> turbos() {
        return achar(cena, "painelTURBO");
    }

    private static void marcarEixo(JanelaDigitalizador janela, Modo modo, double px, double py, String valor) {
        janela.escolherModo(modo);
        janela.clicar(px, py);
        janela.valorEixo(modo).setText(valor);
    }

    private static void marcar(JanelaDigitalizador janela, Scene dialogo, String botao, double[][] pontos) {
        TelaDeTeste.<Button>achar(dialogo, botao).fire();
        for (double[] p : pontos) {
            janela.clicar(x(p[0]), y(p[1]));
        }
    }

    private static void ilha(JanelaDigitalizador janela, Scene dialogo, String pct, double[][] pontos) {
        janela.eficienciaIlha().setText(pct);
        marcar(janela, dialogo, "novaIlha", pontos);
    }

    private static void linha(JanelaDigitalizador janela, Scene dialogo, String rpm, double[][] pontos) {
        janela.rpmLinha().setText(rpm);
        marcar(janela, dialogo, "novaLinha", pontos);
    }

    private static double x(double vazaoLbMin) {
        return 60 + 10 * vazaoLbMin;
    }

    private static double y(double razaoPressao) {
        return 500 - 160 * (razaoPressao - 1);
    }

    /** Abre a janela (sem esperar), salva a imagem já no tamanho dela e fecha. */
    private static void fotografar(Dialog<?> dialogo, String arquivo) throws Exception {
        dialogo.show();
        try {
            salvar(dialogo.getDialogPane().getScene(), arquivo);
        } finally {
            dialogo.close();
        }
    }

    /** As colunas do turbo na lista do catálogo. */
    private static List<String> resumo(Turbo turbo) {
        return FormasCatalogo.de(TipoCatalogo.TURBO).resumo(turbo);
    }

    private static Tab aba(TabPane abas, String id) {
        return abas.getTabs().stream().filter(a -> id.equals(a.getId())).findFirst().orElseThrow();
    }

    /**
     * Desenha um mapa de compressor FICTÍCIO (grade, escalas, surge, choke, ilhas e linhas de rotação), como os
     * gráficos dos fabricantes, e guarda uma cópia em target/ como evidência.
     */
    private static Path desenharMapaFicticio(Path arquivo) throws IOException {
        BufferedImage figura = new BufferedImage(LARGURA, ALTURA, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = figura.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, LARGURA, ALTURA);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.setStroke(new BasicStroke(1f));
            for (int vazao = 0; vazao <= 65; vazao += 5) {
                g.setColor(vazao % 10 == 0 ? new Color(200, 200, 200) : new Color(235, 235, 235));
                g.drawLine((int) x(vazao), (int) y(1.0), (int) x(vazao), (int) y(3.8));
                if (vazao % 10 == 0) {
                    g.setColor(Color.BLACK);
                    g.drawString(String.valueOf(vazao), (int) x(vazao) - 6, (int) y(1.0) + 18);
                }
            }
            for (int decimo = 10; decimo <= 38; decimo++) {
                double pr = decimo / 10.0;
                g.setColor(decimo % 5 == 0 ? new Color(200, 200, 200) : new Color(235, 235, 235));
                g.drawLine((int) x(0), (int) y(pr), (int) x(65), (int) y(pr));
                if (decimo % 5 == 0) {
                    g.setColor(Color.BLACK);
                    g.drawString(String.format("%.1f", pr).replace('.', ','), (int) x(0) - 30, (int) y(pr) + 4);
                }
            }
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.5f));
            g.drawRect((int) x(0), (int) y(3.8), (int) (x(65) - x(0)), (int) (y(1.0) - y(3.8)));
            g.drawString("Vazão corrigida (lb/min)", (int) x(25), ALTURA - 14);
            g.drawString("PR", 8, (int) y(2.4));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g.drawString("MAPA FICTÍCIO PARA TESTE — não é de nenhum fabricante", (int) x(1), 22);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.drawString("Referência: 545 °R e 13,95 psia · rotação máxima 160.000 rpm", (int) x(1), 40);

            g.setStroke(new BasicStroke(2.5f));
            g.setColor(new Color(60, 60, 60));
            polilinha(g, SURGE, false);
            polilinha(g, CHOKE, false);
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(90, 90, 150));
            polilinha(g, ILHA_60, true);
            polilinha(g, ILHA_70, true);
            polilinha(g, ILHA_76, true);
            g.drawString("60%", (int) x(46), (int) y(3.0) - 6);
            g.drawString("70%", (int) x(42), (int) y(1.9) + 14);
            g.drawString("76%", (int) x(29), (int) y(2.25));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {6f, 4f},
                    0f));
            g.setColor(new Color(150, 90, 60));
            polilinha(g, LINHA_100K, false);
            polilinha(g, LINHA_130K, false);
            polilinha(g, LINHA_160K, false);
            g.drawString("100.000", (int) x(43) + 4, (int) y(1.5));
            g.drawString("130.000", (int) x(49) + 4, (int) y(2.1));
            g.drawString("160.000", (int) x(53) + 4, (int) y(2.7));
        } finally {
            g.dispose();
        }
        ImageIO.write(figura, "png", arquivo.toFile());
        Path alvo = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target");
        Files.createDirectories(alvo);
        Files.copy(arquivo, alvo.resolve("mapa-ficticio-digitalizador.png"), StandardCopyOption.REPLACE_EXISTING);
        return arquivo;
    }

    private static void polilinha(Graphics2D g, double[][] pontos, boolean fechada) {
        int n = pontos.length;
        int[] xs = new int[n];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            xs[i] = (int) Math.round(x(pontos[i][0]));
            ys[i] = (int) Math.round(y(pontos[i][1]));
        }
        if (fechada) {
            g.drawPolygon(xs, ys, n);
        } else {
            g.drawPolyline(xs, ys, n);
        }
    }
}
