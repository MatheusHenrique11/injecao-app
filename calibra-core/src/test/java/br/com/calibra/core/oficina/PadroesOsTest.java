package br.com.calibra.core.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Os padrões das OS em Configurações (bloco 3.3): os de fábrica e as regras do que se pode gravar. */
class PadroesOsTest {

    @Test
    void padroesDeFabricaEscolhidosPeloDono() {
        PadroesOs padrao = PadroesOs.padrao();
        assertEquals(90, padrao.garantiaDias());
        assertEquals(List.of("Lataria e pintura", "Vidros, faróis e retrovisores", "Pneus, rodas e estepe",
                "Interior e painel"), padrao.itensVistoria());
        assertTrue(padrao.textoVia().startsWith("Autorizo a oficina a fazer o orçamento do serviço pedido."));
        assertEquals(List.of(), padrao.validar());
    }

    @Test
    void limpaOsEspacosEAceitaSemGarantia() {
        PadroesOs lidos = new PadroesOs(0, List.of(" Motor e cofre ", "Lataria"), "  ");
        assertEquals(List.of("Motor e cofre", "Lataria"), lidos.itensVistoria());
        assertEquals("", lidos.textoVia(), "o texto da via pode ficar vazio: só as assinaturas");
        assertEquals(List.of(), lidos.validar(), "0 dias = sem garantia");
    }

    @Test
    void recusaOQueNaoServe() {
        assertEquals(List.of("A garantia padrão vai de 0 (sem garantia) a 3650 dias."),
                new PadroesOs(-1, List.of("Lataria"), "").validar());
        assertEquals(List.of("A garantia padrão vai de 0 (sem garantia) a 3650 dias."),
                new PadroesOs(3651, List.of("Lataria"), "").validar());
        assertEquals(List.of("A vistoria precisa de pelo menos um item."), new PadroesOs(90, List.of(), "").validar());
        assertEquals(List.of("O item 2 da vistoria está sem nome.",
                "O item \"lataria\" aparece duas vezes na vistoria."),
                new PadroesOs(90, List.of("Lataria", " ", "lataria"), "").validar());
        assertEquals(List.of("O item \"" + "x".repeat(61) + "\" passa de 60 letras."),
                new PadroesOs(90, List.of("x".repeat(61)), "").validar());
        assertEquals(List.of("O texto da via do cliente passa de 600 letras."),
                new PadroesOs(90, List.of("Lataria"), "y".repeat(601)).validar());

        List<String> muitos = new ArrayList<>();
        for (int i = 1; i <= 21; i++) {
            muitos.add("Item " + i);
        }
        assertEquals(List.of("A vistoria tem no máximo 20 itens."), new PadroesOs(90, muitos, "").validar());
    }
}
