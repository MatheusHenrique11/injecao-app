package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.contemTexto;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Teste de interface (opcional, fora do verify padrão): abre a calculadora sem mostrar janela,
 * preenche o caso golden B, calcula e salva imagens da própria cena em target/. Precisa de uma área de
 * trabalho do Windows. Rodar com: mvn -q -pl calibra-app verify -Dgroups=ui -Dtestes.excluidos=nenhum
 */
@Tag("ui")
class CalculadoraUiTest {

    @Test
    void preencheOCasoBCalculaEMostraOSemaforo() throws Throwable {
        TelaDeTeste.naTelaDoJavaFx(CalculadoraUiTest::executar);
    }

    private static void executar() throws IOException {
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("calculadora.fxml"));
        Parent raiz = carregador.load();
        CalculadoraController controlador = carregador.getController();
        Scene cena = new Scene(raiz, 1280, 900);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());

        controlador.preencher(ValoresDeTeste.casoB());
        TabPane abas = (TabPane) cena.lookup("#abas");
        abas.getSelectionModel().select(0);
        salvar(cena, "captura-calculadora-motor.png");
        abas.getSelectionModel().select(1);
        salvar(cena, "captura-calculadora-meta.png");

        ((Button) cena.lookup("#botaoCalcular")).fire();
        raiz.applyCss();
        raiz.layout();

        Node painel = cena.lookup("#painelResultado");
        assertNotNull(painel);
        assertTrue(contemTexto(painel, "VERMELHO"), "o DC de 86% deveria aparecer vermelho");
        assertTrue(contemTexto(painel, "DC do bico na potência máxima"));
        assertNotNull(cena.lookup("#justificativa-DC_BICO"), "campo de justificativa do vermelho");
        assertFalse(((Button) cena.lookup("#botaoPdf")).isDisabled(), "PDF liberado depois de calcular");
        salvar(cena, "captura-calculadora-resultado.png");

        controlador.preencher(Map.of(Campo.VE, "0,90"));
        assertTrue(((Button) cena.lookup("#botaoPdf")).isDisabled(), "mudou um campo: PDF bloqueado");
        assertTrue(contemTexto(painel, "desatualizado"), "faixa de resultado desatualizado");
    }
}
