package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CalibraAppTest {

    @Test
    void versaoDaFase1() {
        assertEquals("0.1.0", CalibraApp.VERSAO);
    }

    @Test
    void enxergaJavaFx() {
        assertDoesNotThrow(() -> Class.forName("javafx.application.Application"));
    }

    @Test
    void telaEEstiloEstaoNoApp() {
        assertNotNull(CalibraApp.class.getResource("calculadora.fxml"));
        assertNotNull(CalibraApp.folhaDeEstilo());
    }
}
