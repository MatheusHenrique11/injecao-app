package br.com.calibra.core.oficina;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** A ficha do chicote para injeção programável (SPEC 6.6.5, Fase 3B): as regras, os textos e a versão nova. */
class FichaChicoteTest {

    private static FichaChicote ficha(String versao, String fonte, List<PontoChicote> pontos) {
        return new FichaChicote("FuelTech FT450", 31L, null, versao, fonte, pontos, "Cabo 1 mm², conectores Deutsch",
                "", SituacaoChicote.EM_CONFECCAO);
    }

    @Test
    void fichaCompletaPassa() {
        FichaChicote ficha = new FichaChicote("  FuelTech FT450 ", 31L, 7L, " v1 ", " Manual da FT450, pág. 12 ",
                List.of(new PontoChicote(" Bico do cilindro 1 ", "A", "12", "", false),
                        new PontoChicote("TPS", "B", "4", "sinal 0 a 5 V", true)),
                " Cabo 1 mm² ", "", SituacaoChicote.TESTADO);
        assertEquals(List.of(), ficha.validar());
        assertEquals("FuelTech FT450", ficha.injecao(), "sem espaços nas pontas");
        assertEquals("v1", ficha.versaoChicote());
        assertEquals("Manual da FT450, pág. 12", ficha.fontePinout());
        assertEquals("Bico do cilindro 1", ficha.pontos().getFirst().funcao());
        assertEquals("testado", ficha.rotuloDaSituacao());
    }

    @Test
    void regrasDaFicha() {
        FichaChicote vazia = new FichaChicote(null, null, null, " ", "  ", List.of(
                new PontoChicote("Bico do cilindro 1", "A", "12", "", false),
                new PontoChicote("", "B", "4", "", false)), null, null, null);
        assertEquals(List.of("Escreva a versão do chicote (ex.: v1).",
                "Escreva de onde veio o pinout (ex.: o manual da injeção, o modelo do fabricante).",
                "Ponto 2: escreva a função do fio (ex.: bico do cilindro 1, TPS).",
                "Escolha a situação do chicote (em confecção ou testado)."), vazia.validar());
        assertEquals("", vazia.injecao(), "a injeção é opcional");
        assertEquals("a definir", vazia.rotuloDaSituacao());
        assertEquals(List.of(), ficha("v1", "Manual", List.of()).validar(), "sem pontos ainda, grava");
    }

    /** A versão nova começa da anterior: tudo igual, a próxima versão e a situação em confecção. */
    @Test
    void versaoNova() {
        List<PontoChicote> pontos = List.of(new PontoChicote("MAP", "C", "7", "", true));
        FichaChicote v1 = new FichaChicote("FuelTech FT450", 31L, 7L, "v1", "Manual", pontos, "Cabo", "Obs",
                SituacaoChicote.TESTADO);
        FichaChicote v2 = v1.versaoNova();
        assertEquals(new FichaChicote("FuelTech FT450", 31L, 7L, "v2", "Manual", pontos, "Cabo", "Obs",
                SituacaoChicote.EM_CONFECCAO), v2);
        assertEquals("V10", ficha("V9", "Manual", List.of()).versaoNova().versaoChicote().toUpperCase());
        assertEquals("v4", ficha("v 3", "Manual", List.of()).versaoNova().versaoChicote());
        assertEquals("Rev. A v2", ficha("Rev. A", "Manual", List.of()).versaoNova().versaoChicote(),
                "outro nome ganha a versão 2");
    }

    @Test
    void situacoes() {
        assertEquals(List.of("EM_CONFECCAO", "TESTADO", "ENTREGUE"), Arrays.stream(SituacaoChicote.values())
                .map(SituacaoChicote::name).toList());
        assertEquals(List.of("em confecção", "testado", "entregue"), Arrays.stream(SituacaoChicote.values())
                .map(SituacaoChicote::rotulo).toList());
    }
}
