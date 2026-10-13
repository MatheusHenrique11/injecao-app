package br.com.calibra.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.ImagensTurbo;
import br.com.calibra.app.catalogo.DesenhoMapa;
import br.com.calibra.app.catalogo.DesenhoMapa.TipoElemento;
import br.com.calibra.app.catalogo.UnidadesMapa;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.pecas.CalibracaoImagem;
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.pecas.Turbo;
import br.com.calibra.core.pecas.UnidadeVazao;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Digitalizador do mapa do compressor (plano 2.4): carrega a imagem do fabricante (copiada para os anexos), calibra
 * cada eixo com dois pontos e marca o surge, o choke, as ilhas (com a eficiência) e as linhas de rotação (com o rpm),
 * com desfazer. O desenho fica por cima da imagem. Salvar grava uma versão nova do turbo, com o mapa.
 */
final class JanelaDigitalizador {

    /** O que o próximo clique na imagem faz. */
    enum Modo {
        X1("1º ponto do eixo de vazão"),
        X2("2º ponto do eixo de vazão"),
        Y1("1º ponto do eixo de razão de pressão"),
        Y2("2º ponto do eixo de razão de pressão"),
        DESENHO("ponto do desenho");

        private final String rotulo;

        Modo(String rotulo) {
            this.rotulo = rotulo;
        }
    }

    private static final Color COR_SURGE = Color.web("#c62828");
    private static final Color COR_CHOKE = Color.web("#1565c0");
    private static final Color COR_ILHA = Color.web("#2e7d32");
    private static final Color COR_ROTACAO = Color.web("#6d4c41");
    private static final Color COR_EIXO = Color.web("#ef6c00");

    private final CasosCatalogo casos;
    private final ImagensTurbo imagens;
    private final ItemCatalogo<Turbo> item;
    private final Dialog<ButtonType> dialogo = new Dialog<>();
    private final ButtonType salvar = new ButtonType("Salvar mapa", ButtonBar.ButtonData.OK_DONE);
    private final ImageView imagem = new ImageView();
    private final Canvas tela = new Canvas(640, 420);
    private final Label nomeImagem = new Label("Nenhuma imagem carregada.");
    private final Double[] pixels = new Double[4];
    private final TextField[] valores = {campo("ex.: 10"), campo("ex.: 60"), campo("ex.: 1,5"), campo("ex.: 3,5")};
    private final ComboBox<UnidadeVazao> eixo = new ComboBox<>();
    private final TextField temperaturaReferencia = campo("ex.: 29,6");
    private final ComboBox<UnidadesMapa.Temperatura> unidadeTemperatura = new ComboBox<>();
    private final TextField pressaoReferencia = campo("ex.: 96,2");
    private final ComboBox<UnidadesMapa.Pressao> unidadePressao = new ComboBox<>();
    private final TextField rotacaoMaxima = campo("opcional");
    private final TextField eficienciaIlha = campo("ex.: 76");
    private final TextField rpmLinha = campo("ex.: 120.000");
    private final Label estado = new Label();
    private final VBox erros = new VBox(4);
    private DesenhoMapa desenho = new DesenhoMapa();
    private String arquivoImagem;
    private Modo modo;
    private ItemCatalogo<Turbo> gravado;

    JanelaDigitalizador(Window dono, CasosCatalogo casos, ImagensTurbo imagens, ItemCatalogo<Turbo> item) {
        this.casos = casos;
        this.imagens = imagens;
        this.item = item;
        CamposTela.configurar(eixo, Arrays.asList(UnidadeVazao.values()), UnidadeVazao::simbolo);
        CamposTela.configurar(unidadeTemperatura, Arrays.asList(UnidadesMapa.Temperatura.values()),
                UnidadesMapa.Temperatura::simbolo);
        CamposTela.configurar(unidadePressao, Arrays.asList(UnidadesMapa.Pressao.values()), UnidadesMapa.Pressao::simbolo);
        eixo.setValue(UnidadeVazao.LB_MIN);
        unidadeTemperatura.setValue(UnidadesMapa.Temperatura.CELSIUS);
        unidadePressao.setValue(UnidadesMapa.Pressao.KPA);
        tela.setOnMouseClicked(evento -> clicar(evento.getX(), evento.getY()));

        StackPane quadro = new StackPane(imagem, tela);
        quadro.setAlignment(Pos.TOP_LEFT);
        ScrollPane rolagem = new ScrollPane(quadro);
        rolagem.setPrefViewportWidth(760);
        rolagem.setPrefViewportHeight(600);
        HBox.setHgrow(rolagem, Priority.ALWAYS);
        ScrollPane lateral = new ScrollPane(painelLateral());
        lateral.setFitToWidth(true);
        lateral.setPrefViewportWidth(360);
        lateral.setPrefViewportHeight(600);

        if (dono != null) {
            dialogo.initOwner(dono);
        }
        dialogo.setTitle("Mapa do compressor");
        dialogo.setHeaderText("Mapa do compressor · " + item.info().nome() + " (v" + item.versao() + ")");
        dialogo.getDialogPane().setContent(new HBox(12, lateral, rolagem));
        dialogo.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        dialogo.getDialogPane().getButtonTypes().setAll(salvar, new ButtonType("Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE));
        dialogo.getDialogPane().lookupButton(salvar).addEventFilter(ActionEvent.ACTION, evento -> {
            if (!gravar()) {
                evento.consume();
            }
        });
        dialogo.getDialogPane().getScene().getAccelerators()
                .put(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), this::desfazer);
        dialogo.setResizable(true);
        abrirMapaGravado();
    }

    // ===================== Montagem =====================

    private Node painelLateral() {
        Button carregar = new Button("Carregar imagem…");
        carregar.setOnAction(evento -> escolherImagem());
        nomeImagem.setWrapText(true);
        nomeImagem.getStyleClass().add("dica");

        GridPane eixos = new GridPane();
        eixos.getStyleClass().add("grade");
        String[] nomes = {"Marcar X1", "Marcar X2", "Marcar Y1", "Marcar Y2"};
        for (int i = 0; i < 4; i++) {
            Modo modoDoBotao = Modo.values()[i];
            Button marcar = new Button(nomes[i]);
            marcar.setId("marcar" + modoDoBotao.name());
            marcar.setOnAction(evento -> escolherModo(modoDoBotao));
            marcar.setTooltip(new Tooltip("Clique depois num número da escala: " + modoDoBotao.rotulo));
            valores[i].setId("valor" + modoDoBotao.name());
            eixos.add(marcar, 0, i);
            eixos.add(valores[i], 1, i);
        }
        eixos.add(new Label("Vazão do eixo X em"), 0, 4);
        eixos.add(eixo, 1, 4);

        GridPane referencia = new GridPane();
        referencia.getStyleClass().add("grade");
        referencia.add(new Label("Temperatura"), 0, 0);
        referencia.add(new HBox(6, temperaturaReferencia, unidadeTemperatura), 1, 0);
        referencia.add(new Label("Pressão (absoluta)"), 0, 1);
        referencia.add(new HBox(6, pressaoReferencia, unidadePressao), 1, 1);
        referencia.add(new Label("Rotação máxima (rpm)"), 0, 2);
        referencia.add(rotacaoMaxima, 1, 2);

        Button surge = botao("Surge", "marcarSurge", () -> comecar(TipoElemento.SURGE, Double.NaN));
        Button choke = botao("Choke", "marcarChoke", () -> comecar(TipoElemento.CHOKE, Double.NaN));
        Button ilha = botao("Nova ilha", "novaIlha", this::novaIlha);
        Button linha = botao("Nova linha de rotação", "novaLinha", this::novaLinha);
        Button desfazer = botao("Desfazer (Ctrl+Z)", "desfazer", this::desfazer);
        GridPane desenhoBotoes = new GridPane();
        desenhoBotoes.getStyleClass().add("grade");
        desenhoBotoes.add(new HBox(6, surge, choke), 0, 0, 2, 1);
        desenhoBotoes.add(ilha, 0, 1);
        desenhoBotoes.add(new HBox(6, eficienciaIlha, new Label("%")), 1, 1);
        desenhoBotoes.add(linha, 0, 2);
        desenhoBotoes.add(new HBox(6, rpmLinha, new Label("rpm")), 1, 2);
        desenhoBotoes.add(desfazer, 0, 3);
        estado.setWrapText(true);
        estado.getStyleClass().add("negrito");

        VBox painel = new VBox(8,
                titulo("1. Imagem do fabricante"), carregar, nomeImagem,
                titulo("2. Eixos"), dica("Clique em \"Marcar X1\" e depois num número da escala de vazão; digite o "
                        + "valor dele. Faça o mesmo com X2 e com Y1 e Y2 na escala de razão de pressão."), eixos,
                titulo("3. Referência do mapa"), dica("Está na legenda do fabricante (ex.: Garrett: 545 °R e 13,95 "
                        + "psia; outros: 298 K e 1 bar)."), referencia,
                titulo("4. Desenho"), dica("Escolha o que marcar e clique na imagem, da esquerda para a direita. A "
                        + "ilha é o contorno fechado da eficiência."), desenhoBotoes, estado, erros);
        painel.setPadding(new Insets(4));
        atualizarEstado();
        return painel;
    }

    private static Label titulo(String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("titulo-secao");
        return rotulo;
    }

    private static Label dica(String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("dica");
        rotulo.setWrapText(true);
        return rotulo;
    }

    private static TextField campo(String exemplo) {
        TextField campo = new TextField();
        campo.setPromptText(exemplo);
        campo.setPrefColumnCount(8);
        return campo;
    }

    private static Button botao(String texto, String id, Runnable acao) {
        Button botao = new Button(texto);
        botao.setId(id);
        botao.setOnAction(evento -> acao.run());
        return botao;
    }

    /** Um mapa já gravado: a mesma imagem, a calibração e o desenho, para continuar. */
    private void abrirMapaGravado() {
        Turbo turbo = item.dados();
        if (turbo.imagemMapa() != null) {
            Path arquivo = imagens.caminho(turbo.imagemMapa());
            if (Files.exists(arquivo)) {
                mostrarImagem(arquivo);
                arquivoImagem = turbo.imagemMapa();
            } else {
                mostrarErros(List.of("A imagem " + turbo.imagemMapa() + " não está mais na pasta de anexos: carregue a "
                        + "imagem de novo."));
            }
        }
        MapaCompressor mapa = turbo.mapa();
        CalibracaoImagem calibracao = turbo.calibracao();
        if (mapa != null) {
            eixo.setValue(mapa.eixoOriginal());
            temperaturaReferencia.setText(NumeroBr.formatar(unidadeTemperatura.getValue()
                    .deK(mapa.temperaturaReferenciaK()), 0, 2));
            pressaoReferencia.setText(NumeroBr.formatar(unidadePressao.getValue().deKpa(mapa.pressaoReferenciaKpa()), 0, 2));
            rotacaoMaxima.setText(mapa.rotacaoMaximaRpm() == null ? "" : NumeroBr.formatar(mapa.rotacaoMaximaRpm(), 0));
        }
        if (calibracao != null) {
            pixels[0] = calibracao.xPixel1();
            pixels[1] = calibracao.xPixel2();
            pixels[2] = calibracao.yPixel1();
            pixels[3] = calibracao.yPixel2();
            double[] numeros = {calibracao.xValor1(), calibracao.xValor2(), calibracao.yValor1(), calibracao.yValor2()};
            for (int i = 0; i < 4; i++) {
                valores[i].setText(NumeroBr.formatar(numeros[i], 0, 4));
            }
            if (mapa != null) {
                desenho = DesenhoMapa.de(mapa, calibracao);
            }
        }
        redesenhar();
    }

    // ===================== Ações =====================

    private void escolherImagem() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Imagem do mapa do compressor");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg",
                "*.gif", "*.bmp"));
        File arquivo = escolha.showOpenDialog(dialogo.getOwner());
        if (arquivo != null) {
            carregarImagem(arquivo.toPath());
        }
    }

    /** Copia a imagem para os anexos e mostra; o desenho e a calibração começam de novo. */
    void carregarImagem(Path arquivo) {
        try {
            arquivoImagem = imagens.copiar(arquivo, item.id());
            mostrarImagem(imagens.caminho(arquivoImagem));
            Arrays.fill(pixels, null);
            desenho = new DesenhoMapa();
            modo = null;
            erros.getChildren().clear();
            redesenhar();
            atualizarEstado();
        } catch (IOException erro) {
            mostrarErros(List.of("Não foi possível copiar a imagem: " + erro.getMessage()));
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
    }

    private void mostrarImagem(Path arquivo) {
        try (InputStream entrada = Files.newInputStream(arquivo)) {
            Image figura = new Image(entrada);
            imagem.setImage(figura);
            tela.setWidth(figura.getWidth());
            tela.setHeight(figura.getHeight());
            nomeImagem.setText(arquivo.getFileName().toString() + " · " + (int) figura.getWidth() + " × "
                    + (int) figura.getHeight() + " pixels");
        } catch (IOException erro) {
            mostrarErros(List.of("Não foi possível abrir a imagem: " + erro.getMessage()));
        }
    }

    void escolherModo(Modo novo) {
        modo = novo;
        atualizarEstado();
    }

    private void comecar(TipoElemento tipo, double valor) {
        desenho.comecar(tipo, valor);
        modo = Modo.DESENHO;
        atualizarEstado();
    }

    private void novaIlha() {
        try {
            double pct = NumeroBr.ler(eficienciaIlha.getText());
            if (!(pct > 0 && pct <= 100)) {
                mostrarErros(List.of("A eficiência da ilha fica entre 0 e 100% (ex.: 76)."));
                return;
            }
            comecar(TipoElemento.ILHA, pct / 100.0);
        } catch (NumberFormatException erro) {
            mostrarErros(List.of("Digite a eficiência da ilha em % (ex.: 76) antes de marcar."));
        }
    }

    private void novaLinha() {
        try {
            double rpm = NumeroBr.ler(rpmLinha.getText());
            if (!(rpm > 0)) {
                mostrarErros(List.of("A rotação da linha deve ser maior que zero."));
                return;
            }
            comecar(TipoElemento.ROTACAO, rpm);
        } catch (NumberFormatException erro) {
            mostrarErros(List.of("Digite a rotação da linha em rpm (ex.: 120.000) antes de marcar."));
        }
    }

    /** Um clique na imagem: calibra o eixo escolhido ou acrescenta um ponto ao desenho. */
    void clicar(double x, double y) {
        if (modo == null) {
            mostrarErros(List.of("Escolha antes o que marcar: um ponto de eixo ou um elemento do desenho."));
            return;
        }
        if (modo == Modo.DESENHO) {
            desenho.adicionar(x, y);
        } else {
            pixels[modo.ordinal()] = modo == Modo.X1 || modo == Modo.X2 ? x : y;
            valores[modo.ordinal()].requestFocus();
            modo = null;
        }
        erros.getChildren().clear();
        redesenhar();
        atualizarEstado();
    }

    void desfazer() {
        desenho.desfazer();
        redesenhar();
        atualizarEstado();
    }

    /** Monta o mapa, confere e grava a versão nova do turbo; com erro, mostra o que corrigir. */
    boolean gravar() {
        List<String> problemas = new ArrayList<>();
        CalibracaoImagem calibracao = calibracao(problemas);
        Double tRef = numero(temperaturaReferencia, "a temperatura de referência", problemas);
        Double pRef = numero(pressaoReferencia, "a pressão de referência", problemas);
        Double rotacao = rotacaoMaxima.getText().isBlank() ? null : numero(rotacaoMaxima, "a rotação máxima", problemas);
        if (arquivoImagem == null) {
            problemas.add("Carregue a imagem do mapa do fabricante.");
        }
        if (!problemas.isEmpty()) {
            mostrarErros(problemas);
            return false;
        }
        MapaCompressor mapa = desenho.paraMapa(calibracao, eixo.getValue(), unidadeTemperatura.getValue().paraK(tRef),
                unidadePressao.getValue().paraKpa(pRef), rotacao);
        List<String> doMapa = new ArrayList<>(calibracao.validar());
        doMapa.addAll(mapa.validar());
        if (!doMapa.isEmpty()) {
            mostrarErros(doMapa);
            return false;
        }
        try {
            gravado = casos.salvar(TipoCatalogo.TURBO, item.id(), item.dados().comMapa(mapa, arquivoImagem, calibracao),
                    item.info()).item();
            return true;
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
        }
        return false;
    }

    private CalibracaoImagem calibracao(List<String> problemas) {
        String[] nomes = {"X1", "X2", "Y1", "Y2"};
        double[] numeros = new double[4];
        for (int i = 0; i < 4; i++) {
            if (pixels[i] == null) {
                problemas.add("Marque o ponto " + nomes[i] + " na imagem.");
            }
            Double valor = numero(valores[i], "o valor de " + nomes[i], problemas);
            numeros[i] = valor == null ? Double.NaN : valor;
        }
        if (!problemas.isEmpty()) {
            return null;
        }
        return new CalibracaoImagem(pixels[0], numeros[0], pixels[1], numeros[1], pixels[2], numeros[2], pixels[3],
                numeros[3]);
    }

    private static Double numero(TextField campo, String nome, List<String> problemas) {
        try {
            return NumeroBr.ler(campo.getText());
        } catch (NumberFormatException erro) {
            problemas.add("Informe " + nome + ": " + erro.getMessage());
            return null;
        }
    }

    // ===================== Desenho =====================

    private void redesenhar() {
        GraphicsContext g = tela.getGraphicsContext2D();
        g.clearRect(0, 0, tela.getWidth(), tela.getHeight());
        g.setFont(Font.font(13));
        for (int i = 0; i < 4; i++) {
            if (pixels[i] == null) {
                continue;
            }
            g.setStroke(COR_EIXO);
            g.setFill(COR_EIXO);
            g.setLineWidth(2);
            String nome = Modo.values()[i].name();
            if (i < 2) {
                g.strokeLine(pixels[i], 0, pixels[i], tela.getHeight());
                g.fillText(nome, pixels[i] + 4, 16);
            } else {
                g.strokeLine(0, pixels[i], tela.getWidth(), pixels[i]);
                g.fillText(nome, 4, pixels[i] - 4);
            }
        }
        for (DesenhoMapa.Elemento elemento : desenho.elementos()) {
            Color cor = switch (elemento.tipo()) {
                case SURGE -> COR_SURGE;
                case CHOKE -> COR_CHOKE;
                case ILHA -> COR_ILHA;
                case ROTACAO -> COR_ROTACAO;
            };
            g.setStroke(cor);
            g.setFill(cor);
            g.setLineWidth(2.5);
            List<DesenhoMapa.PontoPixel> pontos = elemento.pontos();
            for (int i = 0; i < pontos.size(); i++) {
                DesenhoMapa.PontoPixel p = pontos.get(i);
                g.fillOval(p.x() - 3.5, p.y() - 3.5, 7, 7);
                if (i > 0) {
                    g.strokeLine(pontos.get(i - 1).x(), pontos.get(i - 1).y(), p.x(), p.y());
                }
            }
            if (elemento.tipo() == TipoElemento.ILHA && pontos.size() > 2) {
                g.strokeLine(pontos.getLast().x(), pontos.getLast().y(), pontos.getFirst().x(), pontos.getFirst().y());
            }
            DesenhoMapa.PontoPixel primeiro = pontos.getFirst();
            String rotulo = switch (elemento.tipo()) {
                case SURGE -> "surge";
                case CHOKE -> "choke";
                case ILHA -> NumeroBr.formatar(elemento.valor() * 100, 0, 1) + "%";
                case ROTACAO -> NumeroBr.formatar(elemento.valor(), 0) + " rpm";
            };
            g.fillText(rotulo, primeiro.x() + 6, primeiro.y() - 6);
        }
    }

    private void atualizarEstado() {
        if (modo == null) {
            estado.setText("Escolha o que marcar.");
        } else if (modo == Modo.DESENHO) {
            estado.setText("Marcando: " + desenho.marcando().rotulo() + ". Clique na imagem; Ctrl+Z desfaz.");
        } else {
            estado.setText("Clique no " + modo.rotulo + ".");
        }
    }

    private void mostrarErros(List<String> mensagens) {
        erros.getChildren().clear();
        for (String mensagem : mensagens) {
            Label rotulo = new Label("• " + mensagem);
            rotulo.getStyleClass().add("erro");
            rotulo.setWrapText(true);
            erros.getChildren().add(rotulo);
        }
    }

    // ===================== Para quem usa a janela =====================

    /** Mostra e espera; devolve o turbo gravado com o mapa (vazio se cancelou). */
    Optional<ItemCatalogo<Turbo>> mostrar() {
        dialogo.showAndWait();
        return Optional.ofNullable(gravado);
    }

    Dialog<ButtonType> dialogo() {
        return dialogo;
    }

    DesenhoMapa desenho() {
        return desenho;
    }

    /** Os campos da calibração, da referência e do desenho (para os testes). */
    TextField valorEixo(Modo ponto) {
        return valores[ponto.ordinal()];
    }

    TextField temperaturaReferencia() {
        return temperaturaReferencia;
    }

    ComboBox<UnidadesMapa.Temperatura> unidadeTemperatura() {
        return unidadeTemperatura;
    }

    TextField pressaoReferencia() {
        return pressaoReferencia;
    }

    ComboBox<UnidadesMapa.Pressao> unidadePressao() {
        return unidadePressao;
    }

    TextField rotacaoMaxima() {
        return rotacaoMaxima;
    }

    TextField eficienciaIlha() {
        return eficienciaIlha;
    }

    TextField rpmLinha() {
        return rpmLinha;
    }

    List<String> erros() {
        return erros.getChildren().stream().map(no -> ((Label) no).getText().substring(2)).toList();
    }
}
