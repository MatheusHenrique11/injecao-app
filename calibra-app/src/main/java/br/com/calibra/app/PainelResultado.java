package br.com.calibra.app;

import static br.com.calibra.core.util.NumeroBr.formatar;
import static br.com.calibra.core.util.NumeroBr.percentual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.memoria.Aviso;
import br.com.calibra.core.memoria.NivelAviso;
import br.com.calibra.core.memoria.Passo;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.semaforo.Avaliacao;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.relatorios.Apresentacao;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Monta a aba de resultado: semáforo (com justificativa dos vermelhos), resultados, avisos e memória. */
final class PainelResultado {

    private final Map<Indicador, TextField> justificativas = new EnumMap<>(Indicador.class);

    /** Campos de justificativa dos indicadores vermelhos. */
    Map<Indicador, TextField> justificativas() {
        return justificativas;
    }

    List<Node> montar(Resultado<Dimensionamento> resultado) {
        return montar(resultado, null);
    }

    /**
     * @param mapa o mapa do turbo do cálculo; com ele, o mapa aparece desenhado com os pontos de potência e de torque
     */
    List<Node> montar(Resultado<Dimensionamento> resultado, MapaCompressor mapa) {
        Dimensionamento d = resultado.valor();
        justificativas.clear();
        List<Node> partes = new ArrayList<>(List.of(secao("Semáforo"), semaforo(d), secao("Resultados"), resultados(d)));
        if (mapa != null && d.noMapa() != null) {
            partes.add(secao("Mapa do compressor · " + d.noMapa().turbo()));
            partes.add(GraficoMapa.desenhar(mapa, d.noMapa().potencia(), d.noMapa().torque(), 760, 460));
        }
        partes.addAll(List.of(
                secao("Recomendações"), lista(d.recomendacoes().stream().map(r -> "• " + r).toList(), null),
                secao("Avisos"), avisos(resultado.avisos()),
                secao("Memória de cálculo (núcleo " + resultado.versaoNucleo() + ")"), memoria(resultado.memoria())));
        return partes;
    }

    static List<Node> erros(List<String> erros) {
        Label titulo = new Label("Corrija antes de calcular:");
        titulo.getStyleClass().add("titulo-secao");
        VBox caixa = lista(erros.stream().map(e -> "• " + e).toList(), "erro");
        return List.of(titulo, caixa);
    }

    // ---------- Partes ----------

    private static Label secao(String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("titulo-secao");
        return rotulo;
    }

    private VBox semaforo(Dimensionamento d) {
        VBox caixa = new VBox(8);
        List<Avaliacao> ordenadas = d.semaforo().stream()
                .sorted(Comparator.comparing(Avaliacao::cor).reversed())
                .toList();
        for (Avaliacao a : ordenadas) {
            Label etiqueta = new Label(a.cor().name());
            etiqueta.getStyleClass().addAll("etiqueta-cor", "cor-" + a.cor().name().toLowerCase(Locale.ROOT));
            etiqueta.setMinWidth(100);
            Label indicador = new Label(a.indicador().rotulo());
            indicador.getStyleClass().add("negrito");
            Label mensagem = new Label(a.mensagem());
            mensagem.setWrapText(true);
            VBox textos = new VBox(2, indicador, mensagem);
            if (a.cor() == Cor.VERMELHO) {
                TextField justificativa = new TextField();
                justificativa.setPromptText("Justificativa obrigatória para gerar o PDF");
                justificativa.setId("justificativa-" + a.indicador().name());
                justificativas.put(a.indicador(), justificativa);
                textos.getChildren().add(justificativa);
            }
            HBox.setHgrow(textos, Priority.ALWAYS);
            caixa.getChildren().add(new HBox(10, etiqueta, textos));
        }
        return caixa;
    }

    private static GridPane resultados(Dimensionamento d) {
        GridPane grade = new GridPane();
        grade.getStyleClass().add("grade");
        int linha = 0;
        linha = linha(grade, linha, "Potência", Apresentacao.potencia(d.potencia().virabrequimKw()) + " · "
                + formatar(d.potencia().especificaCvPorLitro(), 1) + " cv/L");
        Dimensionamento.Combustao c = d.combustao();
        linha = linha(grade, linha, "Vazão de ar", Apresentacao.vazaoAr(c.vazaoArKgS()));
        linha = linha(grade, linha, "Combustível", Apresentacao.combustivel(c.combustivelKgH()) + " · BSFC "
                + formatar(c.bsfcLbHpH(), 3) + " lb/hp·h · AFR alvo " + formatar(c.afrAlvo(), 2) + ":1");
        Dimensionamento.LadoDoAr ar = d.ladoDoAr();
        linha = linha(grade, linha, "MAP necessário", Apresentacao.pressaoAbsoluta(ar.mapKpa()));
        if (ar.sobrealimentado()) {
            linha = linha(grade, linha, "Pressão do turbo",
                    Apresentacao.pressaoManometrica(Math.max(0, ar.mapKpa() - ar.patmKpa())) + " · PR "
                            + formatar(ar.razaoPressao(), 2));
            linha = linha(grade, linha, "Temperaturas", "T2 " + Apresentacao.temperatura(ar.t2C()) + " · Tm "
                    + Apresentacao.temperatura(ar.tmC()));
        } else {
            linha = linha(grade, linha, "MAP ÷ P_atm local", formatar(ar.mapSobrePatm(), 3) + " (P_atm "
                    + formatar(ar.patmKpa(), 1) + " kPa) · Tm " + Apresentacao.temperatura(ar.tmC()));
        }
        Dimensionamento.BicoNecessario bn = d.bicoNecessario();
        linha = linha(grade, linha, "Bico necessário", formatar(bn.vazaoMassaGMin(), 0) + " g/min · "
                + formatar(bn.vazaoCombustivelCm3Min(), 0) + " cm³/min de combustível · nominal "
                + Apresentacao.vazaoBico(bn.vazaoNominalCm3Min(), bn.densidadeTesteKgL()) + " a "
                + formatar(Conversoes.kpaParaBar(bn.pressaoTesteKpa()), 2) + " bar (sem tempo morto)");
        Dimensionamento.BicoAvaliado b = d.bicoAvaliado();
        if (b != null) {
            String texto = "TI " + formatar(b.tiEfetivoMs(), 2) + " + " + formatar(b.tempoMortoMs(), 2) + " = "
                    + formatar(b.tiTotalMs(), 2) + " ms num ciclo de " + formatar(d.porInjecao().cicloMs(), 2)
                    + " ms · DC " + percentual(b.dutyCycle(), 1);
            if (Double.isFinite(b.vazaoNominalMinimaCm3Min())) {
                texto += " · mínimo com tempo morto: " + formatar(b.vazaoNominalMinimaCm3Min(), 0) + " cm³/min";
            }
            if (Double.isFinite(b.tiEfetivoMarchaLentaMs())) {
                texto += " · marcha lenta: TI efetivo " + formatar(b.tiEfetivoMarchaLentaMs(), 2) + " ms";
            }
            linha = linha(grade, linha, "Bico informado", texto);
        }
        Dimensionamento.BombaNecessaria bomba = d.bomba();
        linha = linha(grade, linha, "Bomba", formatar(bomba.vazaoLh(), 1) + " L/h a "
                + Apresentacao.pressaoManometrica(bomba.pressaoKpa()) + " · com folga verde: "
                + formatar(bomba.vazaoComFolgaVerdeLh(), 0) + " L/h");
        Dimensionamento.Esforco e = d.esforco();
        linha = linha(grade, linha, "Esforço", "BMEP " + formatar(e.bmepBar(), 1) + " bar · torque "
                + formatar(e.torqueNm(), 0) + " N·m · pistão " + formatar(e.velocidadePistaoMS(), 1) + " m/s");
        linha = linha(grade, linha, "Saídas exigidas", d.saidas().bicos() + " de bico"
                + (d.saidas().bicosExtras() > 0 ? " + " + d.saidas().bicosExtras() + " extras" : "") + " · "
                + d.saidas().ignicao() + " de ignição");
        linha = linha(grade, linha, "Sensor MAP", "faixa mínima " + formatar(d.sensorMap().faixaMinimaKpa(), 0)
                + " kPa absolutos (MAP máximo previsto " + formatar(d.sensorMap().mapMaximoPrevistoKpa(), 1) + " kPa)");
        Dimensionamento.PontoTorque torque = d.pontoTorque();
        if (torque != null) {
            linha = linha(grade, linha, "Torque máximo", formatar(torque.rpm(), 0) + " rpm · VE "
                    + formatar(torque.ve(), 2) + " · Wa " + Apresentacao.vazaoAr(torque.vazaoArKgS()) + " · torque "
                    + formatar(torque.torqueNm(), 0) + " N·m (estimado)");
        }
        Dimensionamento.NoMapa noMapa = d.noMapa();
        if (noMapa != null) {
            linha = linha(grade, linha, "No mapa: potência", Apresentacao.pontoNoMapa(noMapa.potencia()));
            if (noMapa.torque() != null) {
                linha(grade, linha, "No mapa: torque", Apresentacao.pontoNoMapa(noMapa.torque()));
            }
        }
        return grade;
    }

    private static int linha(GridPane grade, int linha, String rotulo, String valor) {
        Label r = new Label(rotulo);
        r.getStyleClass().add("negrito");
        r.setMinWidth(150);
        Label v = new Label(valor);
        v.setWrapText(true);
        grade.addRow(linha, r, v);
        return linha + 1;
    }

    private static VBox avisos(List<Aviso> avisos) {
        if (avisos.isEmpty()) {
            return lista(List.of("Nenhum aviso."), null);
        }
        VBox caixa = new VBox(4);
        for (Aviso aviso : avisos) {
            Label rotulo = new Label("• [" + aviso.nivel().rotulo() + "] " + aviso.mensagem());
            rotulo.setWrapText(true);
            if (aviso.nivel() != NivelAviso.INFORMACAO) {
                rotulo.getStyleClass().add(aviso.nivel() == NivelAviso.VERMELHO ? "erro" : "aviso-amarelo");
            }
            caixa.getChildren().add(rotulo);
        }
        return caixa;
    }

    private static VBox lista(List<String> linhas, String estilo) {
        VBox caixa = new VBox(4);
        for (String texto : linhas) {
            Label rotulo = new Label(texto);
            rotulo.setWrapText(true);
            if (estilo != null) {
                rotulo.getStyleClass().add(estilo);
            }
            caixa.getChildren().add(rotulo);
        }
        return caixa;
    }

    private static TableView<Passo> memoria(List<Passo> passos) {
        TableView<Passo> tabela = new TableView<>();
        tabela.setId("tabelaMemoria");
        tabela.getColumns().add(coluna("Grandeza", 230, Passo::grandeza));
        tabela.getColumns().add(coluna("Resultado", 130, p -> Apresentacao.valor(p.resultado())));
        tabela.getColumns().add(coluna("Fórmula", 340, Passo::formula));
        tabela.getColumns().add(coluna("Entradas", 420, p -> p.entradas().stream()
                .map(e -> e.simbolo() + " = " + Apresentacao.valor(e.valor()))
                .collect(Collectors.joining("; "))));
        tabela.getItems().setAll(passos);
        tabela.setPrefHeight(420);
        return tabela;
    }

    private static TableColumn<Passo, String> coluna(String titulo, double largura,
            Function<Passo, String> texto) {
        TableColumn<Passo, String> coluna = new TableColumn<>(titulo);
        coluna.setPrefWidth(largura);
        coluna.setCellValueFactory(celula -> new ReadOnlyStringWrapper(texto.apply(celula.getValue())));
        coluna.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio ? null : item);
                setTooltip(vazio || item == null ? null : new Tooltip(item));
            }
        });
        return coluna;
    }
}
