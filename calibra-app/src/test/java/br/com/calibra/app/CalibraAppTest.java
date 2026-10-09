package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CalibraAppTest {

    @Test
    void versaoDaFase0() {
        assertEquals("0.0.1", CalibraApp.VERSAO);
    }

    @Test
    void enxergaJavaFx() {
        assertDoesNotThrow(() -> Class.forName("javafx.application.Application"));
    }
}