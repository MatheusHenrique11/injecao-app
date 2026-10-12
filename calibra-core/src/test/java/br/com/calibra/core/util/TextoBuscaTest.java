package br.com.calibra.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Texto de busca sem acento, minúsculo e sem pontuação. */
class TextoBuscaTest {

    @Test
    void tiraAcentoPontuacaoEMaiusculas() {
        assertEquals("joao da silva", TextoBusca.normalizar("  João   da  Silva "));
        assertEquals("acao turbo ap20", TextoBusca.normalizar("Ação Turbo AP-2.0"));
        assertEquals("abc1d23", TextoBusca.normalizar("ABC-1D23"));
        assertEquals("11 987654321", TextoBusca.normalizar("(11) 98765-4321"));
        assertEquals("a b", TextoBusca.normalizar("a - b"));
        assertEquals("", TextoBusca.normalizar(null));
    }

    @Test
    void juntaVariosTextos() {
        assertEquals("joao gol abc1234", TextoBusca.juntar("João", null, "Gol", "ABC-1234"));
    }

    @Test
    void palavrasDaBusca() {
        assertEquals(List.of("joao", "gol"), TextoBusca.palavras(" João  GOL "));
        assertEquals(List.of(), TextoBusca.palavras(" - "));
    }
}
