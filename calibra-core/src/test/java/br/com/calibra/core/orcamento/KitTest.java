package br.com.calibra.core.orcamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.orcamento.Kit.Linha;
import br.com.calibra.core.orcamento.Kit.TipoLinha;

/** Itens e kits do catálogo (SPEC 6.6.4, Fase 3B): as regras de cada um. Dados fictícios. */
class KitTest {

    private static final String FONTE = "Tabela da oficina";

    private static Linha linha(TipoLinha tipo, long id, String nome, String quantidade) {
        return new Linha(tipo, id, nome, new BigDecimal(quantidade));
    }

    @Test
    void itemPrecisaDaUnidade() {
        assertEquals(List.of(), new ItemMaterial("Fusível lâmina 30 A", "un", FONTE, null).validar());
        assertEquals("m", new ItemMaterial("Cabo 6 mm²", "  m ", FONTE, null).unidade(), "sem espaços nas pontas");
        assertEquals(List.of("Informe a unidade do item (un, m, par, kit...)."),
                new ItemMaterial("Fusível lâmina 30 A", " ", FONTE, null).validar());
    }

    /** O kit com serviço é um pacote (ex.: "chicote + instalação"); só com itens, não. */
    @Test
    void pacoteEOKitComServico() {
        Kit pacote = new Kit("Chicote + instalação", List.of(
                linha(TipoLinha.SERVICO, 21, "Confecção de chicote para injeção programável", "1"),
                linha(TipoLinha.SERVICO, 22, "Instalação de injeção programável", "1"),
                linha(TipoLinha.ITEM, 31, "Relé 12 V 40 A", "2")), FONTE, null);
        assertEquals(List.of(), pacote.validar());
        assertTrue(pacote.pacote());
        Kit soItens = new Kit("Kit de alimentação do som", List.of(linha(TipoLinha.ITEM, 31, "Cabo 6 mm²", "5"),
                linha(TipoLinha.ITEM, 32, "Fusível lâmina 30 A", "1")), FONTE, null);
        assertFalse(soItens.pacote());
        assertEquals(new BigDecimal("5"), soItens.linhas().getFirst().quantidade());
        assertEquals(new BigDecimal("2.5"), linha(TipoLinha.ITEM, 31, "Cabo", "2.50").quantidade(), "2,50 = 2,5");
    }

    @Test
    void regrasDoKit() {
        assertEquals(List.of("O kit precisa de pelo menos uma linha (um item ou um serviço do catálogo)."),
                new Kit("Vazio", List.of(), FONTE, null).validar());
        assertEquals(List.of("Linha 1 (Cabo 6 mm²): a quantidade deve ser maior que zero.",
                "Linha 3 (Cabo 6 mm²): esse item já está no kit; some as quantidades numa linha só."),
                new Kit("Errado", List.of(linha(TipoLinha.ITEM, 31, "Cabo 6 mm²", "0"),
                        linha(TipoLinha.SERVICO, 31, "Instalação", "1"),
                        linha(TipoLinha.ITEM, 31, "Cabo 6 mm²", "2")), FONTE, null).validar(),
                "o mesmo id em catálogos diferentes (item e serviço) é outra coisa");
    }
}
