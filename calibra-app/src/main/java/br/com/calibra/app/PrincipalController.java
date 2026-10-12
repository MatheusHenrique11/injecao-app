package br.com.calibra.app;

import java.io.IOException;
import java.io.UncheckedIOException;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import br.com.calibra.core.memoria.VersaoNucleo;
import br.com.calibra.dados.config.Chave;

/**
 * Janela principal: navegação entre as telas, barra de status e versão (SPEC 10). Cada tela é carregada na
 * primeira vez que é aberta e continua com o que foi digitado quando se troca de tela.
 */
public class PrincipalController {

    @FXML private ToggleGroup navegacao;
    @FXML private ToggleButton navCalculadora;
    @FXML private ToggleButton navConfiguracoes;
    @FXML private StackPane conteudo;
    @FXML private Label rotuloStatus;
    @FXML private Label rotuloVersao;

    private ContextoApp contexto;
    private HostServices servicos;
    private Stage palco;

    private Parent telaCalculadora;
    private CalculadoraController calculadora;
    private Parent telaConfiguracoes;

    @FXML
    private void initialize() {
        rotuloVersao.setText("Calibra " + Versao.atual() + " · núcleo " + VersaoNucleo.ATUAL + " · offline");
        // Clicar de novo no botão da tela aberta não pode deixar a navegação sem nada escolhido.
        navegacao.selectedToggleProperty().addListener((obs, antes, depois) -> {
            if (depois == null && antes != null) {
                antes.setSelected(true);
            }
        });
    }

    void iniciar(ContextoApp contexto, HostServices servicos, Stage palco) {
        this.contexto = contexto;
        this.servicos = servicos;
        this.palco = palco;
        mostrarCalculadora();
    }

    @FXML
    private void mostrarCalculadora() {
        if (telaCalculadora == null) {
            FXMLLoader carregador = carregador("calculadora.fxml");
            telaCalculadora = carregar(carregador);
            calculadora = carregador.getController();
            calculadora.usarServicos(servicos);
            calculadora.usarPadroes(contexto.configuracao().ler(Chave.CALCULO));
        }
        mostrar(telaCalculadora, navCalculadora, "Calculadora");
    }

    @FXML
    private void mostrarConfiguracoes() {
        if (telaConfiguracoes == null) {
            FXMLLoader carregador = carregador("configuracoes.fxml");
            telaConfiguracoes = carregar(carregador);
            ConfiguracoesController configuracoes = carregador.getController();
            configuracoes.iniciar(contexto.configuracao(), contexto.pastas().anexos(), padroes -> {
                if (calculadora != null) {
                    calculadora.usarPadroes(padroes);
                }
            });
        }
        mostrar(telaConfiguracoes, navConfiguracoes, "Configurações");
    }

    private void mostrar(Parent tela, ToggleButton botao, String titulo) {
        conteudo.getChildren().setAll(tela);
        botao.setSelected(true);
        if (palco != null) {
            palco.setTitle("Calibra " + Versao.atual() + " — " + titulo);
        }
    }

    private static FXMLLoader carregador(String fxml) {
        return new FXMLLoader(PrincipalController.class.getResource(fxml));
    }

    private static Parent carregar(FXMLLoader carregador) {
        try {
            return carregador.load();
        } catch (IOException erro) {
            throw new UncheckedIOException("Tela do app não abriu: " + carregador.getLocation(), erro);
        }
    }
}
