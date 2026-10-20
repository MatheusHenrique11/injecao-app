package br.com.calibra.core.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Os checklists da Fase 3B (SPEC 6.5 e 6.6.2): o modelo, a marcação de cada item e o que trava o "pronto". */
class ChecklistTest {

    private static final String FONTE = "Tabela da oficina";

    @Test
    void regrasDoModelo() {
        ChecklistModelo som = new ChecklistModelo("Som", List.of(
                new ChecklistModelo.Item(" Medir a tensão da bateria antes de começar ", true, true),
                new ChecklistModelo.Item("Fotos de antes e depois", false, false)), FONTE, null);
        assertEquals(List.of(), som.validar());
        assertEquals("Medir a tensão da bateria antes de começar", som.itens().getFirst().texto(), "sem espaços");
        assertEquals(List.of("O checklist precisa de pelo menos um item."),
                new ChecklistModelo("Vazio", List.of(), FONTE, null).validar());
        assertEquals(List.of("Item 2: escreva o que conferir.",
                "Item 3 (fotos de antes e depois): já está no checklist."), new ChecklistModelo("Errado", List.of(
                        new ChecklistModelo.Item("Fotos de antes e depois", true, false),
                        new ChecklistModelo.Item("  ", true, false),
                        new ChecklistModelo.Item("fotos de antes e depois", true, false)), FONTE, null).validar(),
                "o mesmo texto, sem diferença de maiúscula, conta como repetido");
    }

    /** Teste nº 4 da Fase 3B: "não se aplica" sem justificativa é recusado. */
    @Test
    void naoSeAplicaPrecisaDeJustificativa() {
        assertEquals(List.of("Fotos de antes e depois: escreva por que o item não se aplica."),
                new Marcacao(SituacaoItem.NAO_SE_APLICA, "", "  ").validar("Fotos de antes e depois"));
        assertEquals(List.of(), new Marcacao(SituacaoItem.NAO_SE_APLICA, "", "Cliente não quis fotos.")
                .validar("Fotos de antes e depois"));
        assertEquals(List.of(), new Marcacao(SituacaoItem.FEITO, "12,6 V", "").validar("Medir a tensão"));
        assertEquals(List.of("Medir a tensão: escolha a situação (pendente, feito ou não se aplica)."),
                new Marcacao(null, "", "").validar("Medir a tensão"));
        assertEquals("", new Marcacao(SituacaoItem.FEITO, "", "sobrou do não se aplica").justificativa(),
                "a justificativa só vale no não se aplica");
        assertEquals("12,6 V", new Marcacao(SituacaoItem.FEITO, " 12,6 V ", null).observacao());
    }

    /** O que trava o "pronto": item obrigatório pendente (ou "não se aplica" sem justificativa). */
    @Test
    void obrigatorioPendenteTravaOPronto() {
        ItemDoChecklist copia = ItemDoChecklist.copia(new ChecklistModelo.Item("Teste de ruído", true, true));
        assertEquals(SituacaoItem.PENDENTE, copia.marcacao().situacao());
        assertTrue(copia.aVerificar(), "a cópia leva o \"a verificar\" do modelo");
        assertTrue(copia.pendente());
        assertFalse(new ItemDoChecklist("Teste de ruído", true, true, new Marcacao(SituacaoItem.FEITO, "", ""))
                .pendente());
        assertFalse(new ItemDoChecklist("Teste de ruído", true, true, new Marcacao(SituacaoItem.NAO_SE_APLICA, "",
                "Sem rádio no carro.")).pendente());
        assertTrue(new ItemDoChecklist("Teste de ruído", true, true, new Marcacao(SituacaoItem.NAO_SE_APLICA, "", ""))
                .pendente(), "não se aplica sem justificativa ainda trava");
        assertFalse(ItemDoChecklist.copia(new ChecklistModelo.Item("Fotos", false, false)).pendente(),
                "item que não é obrigatório não trava");
    }

    /** O resumo da aba Execução e do comprovante: feitos de quantos, os que não se aplicam e os que faltam. */
    @Test
    void resumoDoChecklist() {
        ItemDoChecklist feito = item(SituacaoItem.FEITO, "");
        ItemDoChecklist naoSeAplica = item(SituacaoItem.NAO_SE_APLICA, "Sem rádio no carro.");
        ItemDoChecklist pendente = item(SituacaoItem.PENDENTE, "");
        assertEquals("9 de 9 itens feitos", ResumoChecklist.de(Collections.nCopies(9, feito)).texto());
        assertEquals("5 de 6 itens feitos, 1 não se aplica",
                ResumoChecklist.de(List.of(feito, feito, feito, naoSeAplica, feito, feito)).texto());
        ResumoChecklist misturado = ResumoChecklist.de(List.of(feito, pendente, naoSeAplica, naoSeAplica, pendente));
        assertEquals(new ResumoChecklist(5, 1, 2, 2), misturado);
        assertEquals("1 de 5 itens feitos, 2 não se aplicam, 2 pendentes", misturado.texto());
        assertEquals("0 de 1 item feito, 1 pendente", ResumoChecklist.de(List.of(pendente)).texto());
    }

    private static ItemDoChecklist item(SituacaoItem situacao, String justificativa) {
        return new ItemDoChecklist("Item", true, false, new Marcacao(situacao, "", justificativa));
    }
}
