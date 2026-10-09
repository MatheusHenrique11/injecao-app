package br.com.calibra.ecu;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ModuloEcuTest {

    @Test
    void naoEnxergaJavaFx() {
        // Regra de arquitetura: só o calibra-app depende de JavaFX.
        assertThrows(ClassNotFoundException.class, () -> Class.forName("javafx.application.Application"));
    }
}