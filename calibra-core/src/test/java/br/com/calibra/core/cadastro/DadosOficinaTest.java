package br.com.calibra.core.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DadosOficinaTest {

    @Test
    void vaziaNaoTemErro() {
        assertTrue(DadosOficina.vazia().validar().isEmpty());
        assertTrue(new DadosOficina(null, null, null, null, null, " ").validar().isEmpty());
        assertNull(new DadosOficina(null, null, null, null, null, " ").logo());
    }

    @Test
    void tiraEspacosDasPontas() {
        DadosOficina oficina = new DadosOficina("  Oficina do Teste ", "", "", "", "", " logo.png ");
        assertEquals("Oficina do Teste", oficina.nome());
        assertEquals("logo.png", oficina.logo());
    }

    @Test
    void cnpjEEmailCertos() {
        DadosOficina oficina = new DadosOficina("Oficina", "11.222.333/0001-81", "", "", "contato@oficina.com.br", null);
        assertTrue(oficina.validar().isEmpty());
    }

    @Test
    void cnpjEEmailErrados() {
        DadosOficina oficina = new DadosOficina("Oficina", "11.222.333/0001-82", "", "", "contato.oficina", null);
        assertEquals(List.of("CNPJ da oficina inválido: confira os números (dígitos verificadores).",
                "E-mail da oficina inválido."), oficina.validar());
    }
}
