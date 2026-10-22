package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

/**
 * A tela de abertura (decisão 283): sem a opção -splash (como nos testes e no app aberto pelo Maven sem ela), não há o
 * que fechar, e fechar não dá erro, nem duas vezes seguidas. A abertura de verdade é conferida no app instalado.
 */
class TelaDeAberturaTest {

    @Test
    void fecharSemTelaDeAberturaNaoFazNada() {
        assertDoesNotThrow(TelaDeAbertura::fechar);
        assertDoesNotThrow(TelaDeAbertura::fechar, "pode ser chamado de novo");
    }
}
