package br.com.calibra.relatorios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.oficina.CodigoFalha;
import br.com.calibra.core.oficina.CondicaoMedicao;
import br.com.calibra.core.oficina.LaudoEletrico;
import br.com.calibra.core.oficina.Medicao;
import br.com.calibra.core.oficina.ResultadoLaudo;
import br.com.calibra.core.oficina.SituacaoCodigo;

/**
 * O laudo de diagnóstico elétrico em PDF (Fase 3B, SPEC 6.6.3): cada parte, a referência "a verificar", o resultado
 * "a definir" e o laudo comum numa página, com as assinaturas. Dados fictícios.
 */
class RelatorioLaudoTest {

    private static final LocalDateTime QUANDO = LocalDateTime.of(2026, 10, 20, 10, 30);

    @TempDir
    Path pasta;

    /** A bateria da Ana descarregava: cinco medições, dois códigos e o diagnóstico completo. */
    static DadosLaudo exemplo() {
        LaudoEletrico laudo = new LaudoEletrico(QUANDO, "Bateria descarrega de um dia para o outro; o carro não "
                + "pega de manhã.", List.of(
                        new Medicao("Tensão da bateria", "12,1", "V", CondicaoMedicao.REPOUSO, "12,4 a 12,7 V", true),
                        new Medicao("Tensão de carga", "14,2", "V", CondicaoMedicao.MOTOR_LIGADO, "13,8 a 14,4 V",
                                false),
                        new Medicao("Consumo em repouso", "0,45", "A", CondicaoMedicao.REPOUSO, "", false),
                        new Medicao("Consumo com o som desligado", "0,03", "A", CondicaoMedicao.OUTRA, "", false),
                        new Medicao("Queda de tensão no cabo terra", "0,1", "V", CondicaoMedicao.CARGA_ACIONADA, "",
                                false)),
                List.of(new CodigoFalha("P0562", "Tensão do sistema baixa", SituacaoCodigo.HISTORICO),
                        new CodigoFalha("U0140", "", SituacaoCodigo.APAGADO)),
                "Teste de fuga com o amperímetro, tirando um fusível de cada vez.",
                "Módulo do som ligado direto na bateria, sem passar pela chave.",
                "Ligação do módulo pelo pós-chave, com relé.", "Conferir a bateria em 30 dias.",
                ResultadoLaudo.RESOLVIDO);
        return new DadosLaudo(ExemplosOs.oficina(), null, "OS-2026-0007", "Ana Costa (fictícia)",
                "Fiat Uno Mille 2011 · EFG4H56", "Carlos (fictício)", laudo, "0.3.1-teste", QUANDO.plusHours(2));
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
    void laudoCompletoNumaPagina() throws IOException {
        Path destino = pasta.resolve("laudo.pdf");
        RelatorioLaudo.gerar(exemplo(), destino);
        String texto = texto(destino);
        for (String esperado : List.of("Oficina Teste (fictícia)", "Laudo de diagnóstico elétrico",
                "OS-2026-0007 · 20/10/2026 10:30", "Cliente: Ana Costa (fictícia)",
                "Veículo: Fiat Uno Mille 2011 · EFG4H56", "Mecânico: Carlos (fictício)",
                "Queixa do cliente Bateria descarrega de um dia para o outro; o carro não pega de manhã.",
                "Medição Valor Condição Referência", "Tensão da bateria 12,1 V repouso 12,4 a 12,7 V (a verificar)",
                "Tensão de carga 14,2 V motor ligado 13,8 a 14,4 V", "Consumo em repouso 0,45 A repouso —",
                "Queda de tensão no cabo terra 0,1 V carga acionada —",
                "Código Descrição Situação", "P0562 Tensão do sistema baixa histórico", "U0140 — apagado",
                "Testes realizados: Teste de fuga com o amperímetro, tirando um fusível de cada vez.",
                "Causa encontrada: Módulo do som ligado direto na bateria, sem passar pela chave.",
                "Solução: Ligação do módulo pelo pós-chave, com relé.",
                "Recomendações: Conferir a bateria em 30 dias.", "Resultado: resolvido",
                "Mecânico responsável: Carlos (fictício)", "Cliente: Ana Costa (fictícia)",
                "laudo de diagnóstico elétrico · gerado em 20/10/2026 12:30")) {
            assertTrue(texto.contains(esperado), () -> "faltou \"" + esperado + "\" em: " + texto);
        }
        assertFalse(texto.contains("?"), "caractere sem fonte virou '?'");
        assertFalse(texto.contains("13,8 a 14,4 V (a verificar)"), "a referência conferida não leva a marca");
        assertEquals(1, paginas(destino), "o laudo comum cabe numa página, com as assinaturas");
    }

    /** No começo do diagnóstico: só a queixa, sem medição nem código, o resultado "a definir" e sem mecânico. */
    @Test
    void laudoNoComeco() throws IOException {
        DadosLaudo d = exemplo();
        DadosLaudo inicio = new DadosLaudo(d.oficina(), null, d.numeroOs(), d.cliente(), d.veiculo(), "",
                new LaudoEletrico(QUANDO, "Luz da bateria acesa no painel", List.of(), List.of(), "", "", "", "",
                        null), d.versaoApp(), d.geradoEm());
        Path destino = pasta.resolve("laudo-inicio.pdf");
        RelatorioLaudo.gerar(inicio, destino);
        String texto = texto(destino);
        assertTrue(texto.contains("Resultado: a definir"), texto);
        assertTrue(texto.contains("Mecânico responsável Cliente: Ana Costa (fictícia)"), texto);
        for (String ausente : List.of("Medições", "Códigos de falha", "Testes realizados", "Mecânico:")) {
            assertFalse(texto.contains(ausente), () -> "não devia ter \"" + ausente + "\": " + texto);
        }
    }

    /** Deixa o PDF de exemplo em target/ para conferência visual (evidência do bloco 3B.4). */
    @Test
    void geraExemploParaConferencia() throws IOException {
        Path alvo = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target");
        Files.createDirectories(alvo);
        Path exemplo = alvo.resolve("exemplo-laudo-eletrico.pdf");
        RelatorioLaudo.gerar(exemplo(), exemplo);
        assertTrue(Files.size(exemplo) > 1_500);
    }
}
