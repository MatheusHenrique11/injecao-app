package br.com.calibra.core;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ModuloCoreTest {

    @Test
    void naoEnxergaJavaFx() {
        // Regra de arquitetura: só o calibra-app depende de JavaFX.
        assertThrows(ClassNotFoundException.class, () -> Class.forName("javafx.application.Application"));
    }
}