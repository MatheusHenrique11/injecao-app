package br.com.calibra.core.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DocumentoTest {

    @Test
    void cpfComDigitosCertos() {
        // Dígitos conferidos à mão: 295 × 10 mod 11 = 2; 347 × 10 mod 11 = 5.
        assertTrue(Documento.cpfValido("529.982.247-25"));
        assertTrue(Documento.cpfValido("52998224725"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"529.982.247-24", "529.982.247-15", "111.111.111-11", "5299822472", "", "abc"})
    void cpfInvalido(String cpf) {
        assertFalse(Documento.cpfValido(cpf));
    }

    @Test
    void cnpjNumerico() {
        // 102 mod 11 = 3 → 8; 120 mod 11 = 10 → 1.
        assertTrue(Documento.cnpjValido("11.222.333/0001-81"));
        assertFalse(Documento.cnpjValido("11.222.333/0001-82"));
        assertFalse(Documento.cnpjValido("00.000.000/0000-00"));
    }

    @Test
    void cnpjAlfanumericoDaReceita() {
        // Exemplo da Receita Federal. Letra vale ASCII − 48 (A = 17): 459 mod 11 = 8 → 3; 424 mod 11 = 6 → 5.
        assertTrue(Documento.cnpjValido("12.ABC.345/01DE-35"));
        assertTrue(Documento.cnpjValido("12abc34501de35"), "minúsculas e sem pontuação também valem");
        assertFalse(Documento.cnpjValido("12.ABC.345/01DE-36"));
        assertFalse(Documento.cnpjValido("12.ABC.345/01DE-3A"), "os dígitos verificadores são sempre números");
    }

    @Test
    void validoEscolhePeloTamanho() {
        assertTrue(Documento.valido("529.982.247-25"));
        assertTrue(Documento.valido("11.222.333/0001-81"));
        assertFalse(Documento.valido("123"));
    }

    @Test
    void formata() {
        assertEquals("529.982.247-25", Documento.formatar("52998224725"));
        assertEquals("12.ABC.345/01DE-35", Documento.formatar("12abc34501de35"));
        assertEquals("123", Documento.formatar(" 123 "));
    }

    @Test
    void oficinaConfereCnpjEEmail() {
        assertTrue(DadosOficina.vazia().validar().isEmpty());
        DadosOficina errada = new DadosOficina("Oficina", "11.222.333/0001-82", "", "", "sem-arroba", null);
        assertEquals(2, errada.validar().size());
        DadosOficina certa = new DadosOficina(" Oficina ", "11.222.333/0001-81", "", "", "contato@oficina.com.br", " ");
        assertTrue(certa.validar().isEmpty());
        assertEquals("Oficina", certa.nome());
        assertEquals(null, certa.logo());
    }
}
