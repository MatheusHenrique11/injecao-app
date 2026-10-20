package br.com.calibra.relatorios;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.oficina.FichaChicote;
import br.com.calibra.core.oficina.PontoChicote;
import br.com.calibra.core.oficina.SituacaoChicote;
import br.com.calibra.core.oficina.SituacaoItem;
import br.com.calibra.relatorios.DadosFichaChicote.ItemDeBancada;

/**
 * A ficha do chicote em PDF (Fase 3B, SPEC 6.6.5): cada parte, o ponto "a verificar", os testes de bancada do
 * checklist da OS, a ficha sem projeto e sem checklist, e a tabela grande que quebra entre as páginas. Dados fictícios.
 */
class RelatorioChicoteTest {

    private static final LocalDateTime QUANDO = LocalDateTime.of(2026, 10, 20, 14, 30);

    @TempDir
    Path pasta;

    /** O chicote da FT450 da Ana, com o projeto de preparação (o motor) e o checklist da confecção marcado. */
    static DadosFichaChicote exemplo() {
        FichaChicote ficha = new FichaChicote("FuelTech FT450", 31L, 7L, "v1", "Manual da FT450, conector A",
                List.of(new PontoChicote("Bico do cilindro 1", "A", "12", "", false),
                        new PontoChicote("Bico do cilindro 2", "A", "13", "", false),
                        new PontoChicote("Bobina 1 e 4", "A", "20", "ignição em centelha perdida", false),
                        new PontoChicote("TPS", "B", "4", "sinal de 0 a 5 V", true),
                        new PontoChicote("MAP", "B", "5", "", true),
                        new PontoChicote("Sensor de rotação", "C", "1", "blindado", false)),
                "Cabo 1 mm², conectores Deutsch, conduíte de 10 mm", "Rota pela caixa de ar, longe do coletor.",
                SituacaoChicote.TESTADO);
        List<ItemDeBancada> bancada = List.of(
                new ItemDeBancada("Teste de continuidade de cada fio", SituacaoItem.FEITO, "todos ok", "",
                        "Carlos (fictício)", QUANDO),
                new ItemDeBancada("Teste de isolamento e de curto", SituacaoItem.FEITO, "", "", "Carlos (fictício)",
                        QUANDO.plusMinutes(10)),
                new ItemDeBancada("Fotos", SituacaoItem.NAO_SE_APLICA, "", "o cliente levou antes", "",
                        QUANDO.plusMinutes(20)),
                new ItemDeBancada("Ficha do chicote preenchida", SituacaoItem.PENDENTE, "", "", "", null));
        return new DadosFichaChicote(ExemplosOs.oficina(), null, "OS-2026-0009", "Ana Costa (fictícia)",
                "VW Gol GTi 1994 · ABC1D23", "v2 · AP 2.0 turbo", ficha, bancada, "0.3.1-teste", QUANDO.plusHours(2));
    }

    private static String texto(Path pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf.toFile())) {
            return new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
        }
    }

    private static int paginas(Path pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf.toFile())) {
            return documento.getNumberOfPages();
        }
    }

    @Test
    void cadaParteDaFicha() throws IOException {
        Path destino = pasta.resolve("ficha.pdf");
        RelatorioChicote.gerar(exemplo(), destino);
        String texto = texto(destino);
        for (String esperado : List.of("Ficha do chicote", "OS-2026-0009 · versão v1 · testado",
                "Cliente: Ana Costa (fictícia)", "Veículo: VW Gol GTi 1994 · ABC1D23", "Situação: testado",
                "Injeção: FuelTech FT450", "Motor: v2 · AP 2.0 turbo", "Fonte do pinout: Manual da FT450, conector A",
                "Função Conector Pino da injeção Observação", "Bico do cilindro 1 A 12 —",
                "TPS B 4 (a verificar) sinal de 0 a 5 V", "MAP B 5 (a verificar)",
                "Teste de continuidade de cada fio feito todos ok Carlos (fictício), 20/10/2026 14:30",
                "Fotos não se aplica Por quê: o cliente levou antes 20/10/2026 14:50",
                "Ficha do chicote preenchida pendente — —",
                "Materiais: Cabo 1 mm², conectores Deutsch, conduíte de 10 mm",
                "Observações: Rota pela caixa de ar, longe do coletor.",
                "Responsável pela confecção (nome e assinatura)", "ficha do chicote v1 · gerada em 20/10/2026 16:30")) {
            assertTrue(texto.contains(esperado), () -> "faltou \"" + esperado + "\" em: " + texto);
        }
        assertFalse(texto.contains("?"), "caractere sem fonte virou '?'");
        assertTrue(paginas(destino) == 1, "a ficha comum cabe numa página");
    }

    /** Sem projeto (sem motor), sem injeção, sem pontos e sem checklist de bancada: cada parte diz o que falta. */
    @Test
    void fichaSemProjetoSemPontosESemChecklist() throws IOException {
        FichaChicote ficha = new FichaChicote("", null, null, "v1", "Modelo do fabricante", List.of(), "", "",
                SituacaoChicote.EM_CONFECCAO);
        Path destino = pasta.resolve("ficha-vazia.pdf");
        RelatorioChicote.gerar(new DadosFichaChicote(null, null, "OS-2026-0010", "Bruno Dias (fictício)",
                "VW Gol GTi 1994 · BCD2E34", "", ficha, List.of(), "0.3.1-teste", QUANDO), destino);
        String texto = texto(destino);
        for (String esperado : List.of("Injeção: não informada", "Nenhum ponto anotado.",
                "Esta OS não tem checklist de bancada.", "Situação: em confecção")) {
            assertTrue(texto.contains(esperado), () -> "faltou \"" + esperado + "\" em: " + texto);
        }
        assertFalse(texto.contains("Motor:"), "sem projeto, sem motor");
        assertFalse(texto.contains("Materiais"), "o texto em branco não aparece");
        assertFalse(texto.contains("Observações:"));
    }

    /** Um chicote grande: a tabela dos pontos quebra entre as páginas, sem perder nenhum, e a assinatura vem no fim. */
    @Test
    void chicoteGrandeQuebraEntreAsPaginas() throws IOException {
        DadosFichaChicote d = exemplo();
        List<PontoChicote> pontos = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            pontos.add(new PontoChicote("Ponto " + i, "A", Integer.toString(i), "", i % 10 == 0));
        }
        FichaChicote grande = new FichaChicote(d.ficha().injecao(), d.ficha().injecaoItemId(),
                d.ficha().motorVersaoId(), "v3", d.ficha().fontePinout(), pontos, d.ficha().materiais(),
                d.ficha().observacoes(), SituacaoChicote.EM_CONFECCAO);
        Path destino = pasta.resolve("ficha-grande.pdf");
        RelatorioChicote.gerar(new DadosFichaChicote(d.oficina(), null, d.numeroOs(), d.cliente(), d.veiculo(),
                d.motor(), grande, d.bancada(), d.versaoApp(), d.geradoEm()), destino);
        String texto = texto(destino);
        assertTrue(paginas(destino) >= 2, "mais de uma página");
        for (String esperado : List.of("Ponto 1 A 1", "Ponto 30 A 30 (a verificar)", "Ponto 60 A 60 (a verificar)",
                "Responsável pela confecção (nome e assinatura)")) {
            assertTrue(texto.contains(esperado), () -> "faltou \"" + esperado + "\"");
        }
    }

    /** Deixa o PDF de exemplo em target/ para conferência visual. */
    @Test
    void geraExemploParaConferencia() throws IOException {
        Path alvo = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target");
        Files.createDirectories(alvo);
        RelatorioChicote.gerar(exemplo(), alvo.resolve("exemplo-ficha-do-chicote.pdf"));
        assertTrue(Files.size(alvo.resolve("exemplo-ficha-do-chicote.pdf")) > 1_500);
    }
}
