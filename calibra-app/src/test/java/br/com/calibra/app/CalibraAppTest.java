package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CalibraAppTest {

    @Test
    void versaoVemDoPom() {
        // Se o filtro do versao.properties parar de funcionar, Versao.atual() lança erro em vez de "${...}".
        assertEquals("0.3.1", Versao.atual());
    }

    @Test
    void enxergaJavaFx() {
        assertDoesNotThrow(() -> Class.forName("javafx.application.Application"));
    }

    @Test
    void telasEEstiloEstaoNoApp() {
        for (String tela : new String[] {"principal.fxml", "painel.fxml", "oficina.fxml", "nova-os.fxml", "os.fxml",
                "entrada.fxml", "escolha-cliente.fxml", "escolha-veiculo.fxml", "inicio.fxml", "assistente.fxml",
                "projeto.fxml", "cliente.fxml", "veiculo.fxml", "motor.fxml", "calculadora.fxml", "configuracoes.fxml",
                "configuracoes-os.fxml", "catalogos.fxml", "orcamento.fxml", "anexos.fxml", "ficha-veiculo.fxml"}) {
            assertNotNull(CalibraApp.class.getResource(tela), tela);
        }
        assertNotNull(CalibraApp.folhaDeEstilo());
    }

    /** O ícone da janela, em todos os tamanhos (scripts\gerar-icone.ps1). */
    @Test
    void iconeEstaNoApp() {
        for (int lado : CalibraApp.TAMANHOS_DO_ICONE) {
            assertNotNull(CalibraApp.class.getResource("icones/calibra-" + lado + ".png"), lado + " px");
        }
    }
}
