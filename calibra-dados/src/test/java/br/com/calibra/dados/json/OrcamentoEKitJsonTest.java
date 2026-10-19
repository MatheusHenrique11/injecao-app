package br.com.calibra.dados.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemMaterial;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Kit;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.TipoItem;

/**
 * O JSON da Fase 3B (bloco 3B.2): o orçamento gravado pelo 0.3.0 continua sendo lido igual (as linhas como "não
 * fornecido pelo cliente"), e o item, o kit e a linha do cliente vão e voltam sem mudar.
 */
class OrcamentoEKitJsonTest {

    /** Um orçamento como o 0.3.0 grava (gerado pelo código daquela versão antes do bloco 3B.2). */
    private static final String ORCAMENTO_030 = "/br/com/calibra/dados/json/orcamento-0.3.0.json";

    @Test
    void orcamentoGravadoPeloApp030ContinuaIgual() throws IOException {
        String json;
        try (InputStream dados = OrcamentoEKitJsonTest.class.getResourceAsStream(ORCAMENTO_030)) {
            assertNotNull(dados, "falta o arquivo " + ORCAMENTO_030);
            json = new String(dados.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertFalse(json.contains("fornecidoPeloCliente"), "o arquivo é do 0.3.0: sem o campo novo");
        Orcamento lido = Json.ler(json, Orcamento.class);
        assertEquals(3, lido.itens().size());
        assertTrue(lido.itens().stream().noneMatch(ItemOrcamento::fornecidoPeloCliente));
        assertEquals(Dinheiro.ler("520,00"), lido.itens().getFirst().valorUnitario());
        assertEquals(Dinheiro.ler("793,25"), lido.total(), "R$ 835,00 − 5%, como no 0.3.0");
        assertEquals(List.of(), lido.validar());
    }

    @Test
    void linhaDoClienteEKitVaoEVoltam() {
        ItemOrcamento ecu = new ItemOrcamento(TipoItem.PECA, "ECU FuelTech FT450", BigDecimal.ONE, "un",
                Dinheiro.ler("4.900,00"), 11L, 1, true);
        Orcamento orcamento = new Orcamento(List.of(ecu, ItemOrcamento.doItem("Relé 12 V 40 A", "un",
                Dinheiro.ler("18,50"), 9, 3)), Desconto.NENHUM, 1, "", 15, "", "");
        Orcamento lido = Json.ler(Json.escrever(orcamento), Orcamento.class);
        assertEquals(orcamento, lido);
        assertTrue(lido.itens().getFirst().fornecidoPeloCliente());
        assertEquals(Dinheiro.ler("18,50"), lido.total());

        ItemMaterial cabo = new ItemMaterial("Cabo 6 mm²", "m", "Tabela da oficina", LocalDate.of(2026, 10, 19));
        assertEquals(cabo, Json.ler(Json.escrever(cabo), ItemMaterial.class));
        Kit kit = new Kit("Chicote + instalação", List.of(
                new Kit.Linha(Kit.TipoLinha.SERVICO, 21, "Confecção de chicote", BigDecimal.ONE),
                new Kit.Linha(Kit.TipoLinha.ITEM, 31, "Cabo 6 mm²", new BigDecimal("2.5"))), "Tabela da oficina",
                null);
        assertEquals(kit, Json.ler(Json.escrever(kit), Kit.class));
    }
}
