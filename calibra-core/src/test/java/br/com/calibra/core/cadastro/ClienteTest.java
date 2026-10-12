package br.com.calibra.core.cadastro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Cliente (SPEC 6.1): dados mínimos, consentimento e anonimização. */
class ClienteTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 12);
    private static final Consentimento CONSENTIMENTO = new Consentimento(HOJE, Consentimento.FINALIDADE_PADRAO);

    private static Cliente cliente(String nome, String telefone, String email, String documento) {
        return Cliente.novo(nome, telefone, email, documento, CONSENTIMENTO, "");
    }

    @Test
    void nomeETelefoneBastam() {
        Cliente joao = cliente(" João da Silva ", "(11) 98765-4321", "", "");
        assertTrue(joao.validar(HOJE).isEmpty(), joao.validar(HOJE)::toString);
        assertEquals("João da Silva", joao.nome());
    }

    @Test
    void nomeETelefoneSaoObrigatorios() {
        assertEquals(List.of("Informe o nome do cliente.", "Informe o telefone do cliente."),
                cliente("", " ", "", "").validar(HOJE));
        assertEquals(List.of("Telefone do cliente incompleto: confira os números."),
                cliente("João", "9876-54", "", "").validar(HOJE));
    }

    @Test
    void documentoValidoFicaFormatadoEInvalidoERecusado() {
        assertEquals("529.982.247-25", cliente("João", "11987654321", "", "52998224725").documento());
        assertEquals("11.222.333/0001-81", cliente("Oficina", "11987654321", "", "11222333000181").documento());
        assertEquals(List.of("CPF ou CNPJ do cliente inválido: confira os números (dígitos verificadores)."),
                cliente("João", "11987654321", "", "529.982.247-24").validar(HOJE));
    }

    @Test
    void emailOpcionalMasComFormatoDeEmail() {
        assertTrue(cliente("João", "11987654321", "joao@exemplo.com.br", "").validar(HOJE).isEmpty());
        assertEquals(List.of("E-mail do cliente inválido."),
                cliente("João", "11987654321", "joao.exemplo.com", "").validar(HOJE));
    }

    @Test
    void consentimentoComDataEFinalidade() {
        Cliente semConsentimento = Cliente.novo("João", "11987654321", "", "", null, "");
        assertEquals(List.of("Registre o consentimento do cliente: data e finalidade do uso dos dados (LGPD)."),
                semConsentimento.validar(HOJE));
        Cliente incompleto = Cliente.novo("João", "11987654321", "", "", new Consentimento(null, " "), "");
        assertEquals(List.of("Informe a data do consentimento do cliente.",
                "Informe a finalidade do uso dos dados do cliente."), incompleto.validar(HOJE));
        Cliente noFuturo = Cliente.novo("João", "11987654321", "", "",
                new Consentimento(HOJE.plusDays(1), "Orçamento"), "");
        assertEquals(List.of("A data do consentimento não pode ser depois de hoje."), noFuturo.validar(HOJE));
    }

    @Test
    void anonimizarApagaOsDadosPessoaisEMantemOConsentimento() {
        Cliente joao = Cliente.novo("João da Silva", "11987654321", "joao@exemplo.com.br", "52998224725",
                CONSENTIMENTO, "Prefere contato à tarde");
        Cliente anonimo = joao.anonimizar(42);
        assertEquals("Cliente anonimizado #42", anonimo.nome());
        assertEquals("", anonimo.telefone());
        assertEquals("", anonimo.email());
        assertEquals("", anonimo.documento());
        assertEquals("", anonimo.observacoes());
        assertEquals(CONSENTIMENTO, anonimo.consentimento());
        assertTrue(anonimo.anonimizado());
        assertTrue(anonimo.validar(HOJE).isEmpty(), "anonimizado não precisa de telefone");
    }

    @Test
    void contato() {
        assertEquals(11, Contato.digitos("(11) 98765-4321"));
        assertEquals(0, Contato.digitos(null));
        assertTrue(Contato.emailValido(" a@b.com "));
        assertTrue(!Contato.emailValido("a@b") && !Contato.emailValido(null));
    }
}
