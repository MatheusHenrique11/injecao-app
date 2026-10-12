package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CalibraAppTest {

    @Test
    void versaoVemDoPom() {
        // Se o filtro do versao.properties parar de funcionar, Versao.atual() lança erro em vez de "${...}".
        assertEquals("0.2.0-SNAPSHOT", Versao.atual());
    }

    @Test
    void enxergaJavaFx() {
        assertDoesNotThrow(() -> Class.forName("javafx.application.Application"));
    }

    @Test
    void telasEEstiloEstaoNoApp() {
        assertNotNull(CalibraApp.class.getResource("principal.fxml"));
        assertNotNull(CalibraApp.class.getResource("calculadora.fxml"));
        assertNotNull(CalibraApp.class.getResource("configuracoes.fxml"));
        assertNotNull(CalibraApp.folhaDeEstilo());
    }
}
