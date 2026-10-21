package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.calibra.app.casos.CasosChicote.OpcaoInjecao;
import br.com.calibra.core.oficina.FichaChicote;
import br.com.calibra.core.oficina.PontoChicote;
import br.com.calibra.core.oficina.SituacaoChicote;

/** A janela da ficha do chicote, sem a tela (Fase 3B, bloco 3B.6): a injeção do catálogo ou escrita, e os pontos. */
class FormularioChicoteTest {

    private static final List<List<String>> LINHAS = List.of(
            List.of("Bico do cilindro 1", "A", "12", "", "false"),
            List.of("", "", "", "", "true"),
            List.of(" TPS ", "B", "4", "sinal de 0 a 5 V", "true"));

    @Test
    void injecaoDoCatalogoOuEscrita() {
        FichaChicote doCatalogo = FormularioChicote.ler(new OpcaoInjecao(31, "FuelTech FT450"), "ignorado", 7L, "v1",
                "Manual", LINHAS, "Cabo", "", SituacaoChicote.TESTADO);
        assertEquals("FuelTech FT450", doCatalogo.injecao(), "o nome do catálogo");
        assertEquals(31L, doCatalogo.injecaoItemId());
        assertEquals(7L, doCatalogo.motorVersaoId());

        FichaChicote escrita = FormularioChicote.ler(null, " ECU artesanal ", null, "v1", "Modelo do fabricante",
                List.of(), "", "", SituacaoChicote.EM_CONFECCAO);
        assertEquals("ECU artesanal", escrita.injecao(), "o texto livre");
        assertNull(escrita.injecaoItemId());
    }

    @Test
    void pontosVaoEVoltam() {
        List<PontoChicote> pontos = FormularioChicote.pontos(LINHAS);
        assertEquals(List.of(new PontoChicote("Bico do cilindro 1", "A", "12", "", false),
                new PontoChicote("TPS", "B", "4", "sinal de 0 a 5 V", true)), pontos,
                "a linha sem texto (só a marca) fica de fora");
        assertEquals(pontos, FormularioChicote.pontos(FormularioChicote.linhasDosPontos(pontos)));
        assertEquals(List.of("TPS", "B", "4", "sinal de 0 a 5 V", "true"),
                FormularioChicote.linhasDosPontos(pontos).get(1));
    }
}
