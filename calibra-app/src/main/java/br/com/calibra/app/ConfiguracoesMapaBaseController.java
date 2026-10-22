package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import br.com.calibra.core.mapa.RegrasMapaBase;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.config.Chave;
import br.com.calibra.dados.config.ConfiguracaoRepositorio;

/**
 * Configurações › Mapa base (fase 4; decisão 294): as regras do lambda, as reduções de ignição e as travas. Salvar
 * vale para os próximos mapas; os mapas salvos com as regras antigas aparecem como desatualizados.
 */
public class ConfiguracoesMapaBaseController {

    private static final Logger LOG = Logger.getLogger(ConfiguracoesMapaBaseController.class.getName());

    @FXML private TextField lambdaCargaParcial;
    @FXML private TextField cargaParcialAte;
    @FXML private TextField plenaCarga;
    @FXML private TextField riquezaAcima;
    @FXML private TextField riqueza;
    @FXML private TextField riquezaUsoContinuo;
    @FXML private TextField reducaoPressao;
    @FXML private TextField reducaoTemperatura;
    @FXML private TextField reducaoCombustivel;
    @FXML private TextField margemLimitador;
    @FXML private TextField fracaoPressao;
    @FXML private VBox mensagensRegras;

    private ConfiguracaoRepositorio configuracao;
    private RegrasMapaBase salvas = RegrasMapaBase.padrao();

    void iniciar(ConfiguracaoRepositorio configuracao) {
        this.configuracao = configuracao;
        salvas = configuracao.ler(Chave.MAPA_BASE);
        descartar();
    }

    @FXML
    void descartar() {
        escrever(salvas);
        mensagensRegras.getChildren().clear();
    }

    @FXML
    void voltarAosPadroes() {
        escrever(RegrasMapaBase.padrao());
        mostrar("Campos com os padrões da SPEC (reduções em branco). Clique em Salvar para gravar.", "ok");
    }

    @FXML
    void salvar() {
        List<String> erros = new ArrayList<>();
        Double lambda = numero(lambdaCargaParcial, "Lambda da carga parcial", false, erros);
        Double parcial = numero(cargaParcialAte, "Carga parcial até", false, erros);
        Double plena = numero(plenaCarga, "Plena carga em", false, erros);
        Double acima = numero(riquezaAcima, "Mais rico acima de", false, erros);
        Double extra = numero(riqueza, "Quanto mais rico", false, erros);
        Double continuo = numero(riquezaUsoContinuo, "A mais em náutico e circuito", false, erros);
        Double porPressao = numero(reducaoPressao, "Redução por pressão", true, erros);
        Double porTemperatura = numero(reducaoTemperatura, "Redução por temperatura", true, erros);
        Double porCombustivel = numero(reducaoCombustivel, "Redução por troca de combustível", true, erros);
        Double margem = numero(margemLimitador, "Limitador abaixo da máxima", false, erros);
        Double fracao = numero(fracaoPressao, "Pressão alvo do turbo", false, erros);
        if (!erros.isEmpty()) {
            mostrarErros(erros);
            return;
        }
        RegrasMapaBase regras = new RegrasMapaBase(lambda, parcial, plena, acima, extra, continuo, porPressao,
                porTemperatura, porCombustivel, margem, fracao / 100.0);
        List<String> regrasErradas = regras.validar();
        if (!regrasErradas.isEmpty()) {
            mostrarErros(regrasErradas);
            return;
        }
        try {
            configuracao.gravar(Chave.MAPA_BASE, regras);
        } catch (ErroDados erro) {
            LOG.log(Level.SEVERE, "Não foi possível salvar as regras do mapa base", erro);
            mostrarErros(List.of(erro.getMessage()));
            return;
        }
        salvas = regras;
        descartar();
        mostrar("Regras salvas. Valem para os próximos mapas; os mapas salvos antes aparecem como desatualizados.",
                "ok");
    }

    /** As regras salvas (para os testes). */
    RegrasMapaBase salvas() {
        return salvas;
    }

    private void escrever(RegrasMapaBase regras) {
        lambdaCargaParcial.setText(NumeroBr.formatar(regras.lambdaCargaParcial(), 2, 3));
        cargaParcialAte.setText(NumeroBr.formatar(regras.cargaParcialAteKpa(), 0, 1));
        plenaCarga.setText(NumeroBr.formatar(regras.plenaCargaKpa(), 0, 1));
        riquezaAcima.setText(NumeroBr.formatar(regras.riquezaExtraAcimaKpa(), 0, 1));
        riqueza.setText(NumeroBr.formatar(regras.riquezaExtra(), 2, 3));
        riquezaUsoContinuo.setText(NumeroBr.formatar(regras.riquezaExtraUsoContinuo(), 2, 3));
        reducaoPressao.setText(opcional(regras.reducaoIgnicaoPorPressao()));
        reducaoTemperatura.setText(opcional(regras.reducaoIgnicaoPorTemperatura()));
        reducaoCombustivel.setText(opcional(regras.reducaoIgnicaoTrocaCombustivel()));
        margemLimitador.setText(NumeroBr.formatar(regras.margemLimitadorRpm(), 0));
        fracaoPressao.setText(NumeroBr.formatar(regras.fracaoPressaoPrevista() * 100, 0, 1));
    }

    private static String opcional(Double valor) {
        return valor == null ? "" : NumeroBr.formatar(valor, 0, 2);
    }

    private static Double numero(TextField campo, String rotulo, boolean podeFicarVazio, List<String> erros) {
        if (campo.getText().isBlank()) {
            if (!podeFicarVazio) {
                erros.add("Preencha \"" + rotulo + "\".");
            }
            return null;
        }
        try {
            return NumeroBr.ler(campo.getText());
        } catch (NumberFormatException erro) {
            erros.add("\"" + rotulo + "\": " + erro.getMessage());
            return null;
        }
    }

    private void mostrarErros(List<String> erros) {
        mensagensRegras.getChildren().clear();
        for (String erro : erros) {
            Label rotulo = new Label("• " + erro);
            rotulo.getStyleClass().add("erro");
            rotulo.setWrapText(true);
            mensagensRegras.getChildren().add(rotulo);
        }
    }

    private void mostrar(String texto, String estilo) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add(estilo);
        rotulo.setWrapText(true);
        mensagensRegras.getChildren().setAll(rotulo);
    }
}
