package br.com.calibra.relatorios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.imageio.ImageIO;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.NumeroOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.StatusOrcamento;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** PDF e texto de WhatsApp do orçamento (bloco 2.5), com o caso golden B e dados fictícios. */
class RelatorioOrcamentoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 13, 16, 45);

    @TempDir
    Path pasta;

    private static Orcamento conteudo() {
        return new Orcamento(List.of(
                ItemOrcamento.digitado(TipoItem.PECA, "Bico teste 1050 (fictício)", BigDecimal.valueOf(4), "un",
                        Dinheiro.ler("450,00")),
                ItemOrcamento.digitado(TipoItem.PECA, "Bomba teste (fictícia)", BigDecimal.ONE, "un",
                        Dinheiro.ler("890,00")),
                ItemOrcamento.digitado(TipoItem.SERVICO, "Mão de obra de instalação", new BigDecimal("2.5"), "h",
                        Dinheiro.ler("180,00"))),
                Desconto.emPercentual(BigDecimal.TEN), 10, "a partir da aprovação", 15, "50% na aprovação e 50% na "
                        + "entrega", "Peças com garantia do fabricante.");
    }

    private Path logo() throws IOException {
        BufferedImage imagem = new BufferedImage(240, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        g.setColor(new Color(21, 101, 192));
        g.fillRect(0, 0, 240, 80);
        g.setColor(Color.WHITE);
        g.drawString("LOGO FICTÍCIO", 70, 45);
        g.dispose();
        Path arquivo = pasta.resolve("logo.png");
        ImageIO.write(imagem, "png", arquivo.toFile());
        return arquivo;
    }

    private DadosOrcamento dados(String cliente, StatusOrcamento status, Path logo) {
        EntradaDimensionamento entrada = RelatorioMemoriaTest.casoB();
        Resultado<Dimensionamento> resultado = CalculoDimensionamento.calcular(entrada);
        return new DadosOrcamento(new DadosOficina("Oficina Teste (fictícia)", "11.222.333/0001-81",
                "Rua das Bielas, 100 · São Paulo", "(11) 99999-0000", "contato@oficina-teste.com.br", null), logo,
                cliente, "VW Gol GTi 1994 · ABC1D23", "AP 2.0 turbo (v1)", new NumeroOrcamento(2026, 1, 1), status,
                conteudo(), LocalDate.of(2026, 10, 13), entrada, resultado,
                List.of(new Justificativa(Indicador.DC_BICO, "Bico maior encomendado.", AGORA)), "0.2.0-teste", AGORA);
    }

    private static String texto(Path pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf.toFile())) {
            return new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
        }
    }

    @Test
    void pdfComOficinaItensTotaisEResumoTecnico() throws IOException {
        Path destino = pasta.resolve("orcamento.pdf");
        RelatorioOrcamento.gerar(dados("João da Silva", StatusOrcamento.ENVIADO, logo()), destino);
        String texto = texto(destino);
        assertTrue(texto.contains("Oficina Teste (fictícia)"), "cabeçalho da oficina");
        assertTrue(texto.contains("CNPJ 11.222.333/0001-81"));
        assertTrue(texto.contains("Orçamento ORC-2026-0001"));
        assertTrue(texto.contains("válido até 28/10/2026 (15 dias)"), "validade em data");
        assertTrue(texto.contains("João da Silva"));
        assertTrue(texto.contains("400,0 cv no virabrequim a 7.000 rpm"), "meta");
        assertTrue(texto.contains("Bico teste 1050 (fictício) 4 un R$ 450,00 R$ 1.800,00"), "linha da tabela");
        assertTrue(texto.contains("Mão de obra de instalação 2,5 h R$ 180,00 R$ 450,00"));
        assertTrue(texto.contains("Desconto (10%) - R$ 314,00"), texto);
        assertTrue(texto.contains("Total R$ 2.826,00"));
        assertTrue(texto.contains("10 dias úteis a partir da aprovação"));
        assertTrue(texto.contains("50% na aprovação e 50% na entrega"));
        assertTrue(texto.contains("Justificativa"), "justificativa do vermelho no resumo técnico");
        assertTrue(texto.contains("DC 86,3%"), "o DC do bico do caso B");
        assertTrue(texto.contains("acerto final é feito no dinamômetro"), "nota do mapa base");
        assertFalse(texto.contains("RASCUNHO"));
        assertFalse(texto.contains("?"), "caractere sem fonte virou '?'");
        assertTrue(temImagem(destino), "o logo entra no PDF");
    }

    @Test
    void rascunhoClienteAnonimizadoESemLogo() throws IOException {
        Path destino = pasta.resolve("rascunho.pdf");
        RelatorioOrcamento.gerar(dados("Cliente anonimizado #3", StatusOrcamento.RASCUNHO, pasta.resolve("sumiu.png")),
                destino);
        String texto = texto(destino);
        assertTrue(texto.contains("Cliente anonimizado #3"), "cliente anonimizado aparece anonimizado");
        assertTrue(texto.contains("RASCUNHO (ainda não enviado)"));
        assertFalse(temImagem(destino), "logo que não existe fica de fora, sem erro");
    }

    @Test
    void textoParaWhatsApp() {
        String esperado = """
                *Orçamento ORC-2026-0001* — Oficina Teste (fictícia)
                Cliente: João da Silva
                Veículo: VW Gol GTi 1994 · ABC1D23
                Motor: AP 2.0 turbo (v1)
                Meta: 400,0 cv no virabrequim a 7.000 rpm · uso: rua

                *Peças*
                • Bico teste 1050 (fictício) — 4 un × R$ 450,00 = R$ 1.800,00
                • Bomba teste (fictícia) — 1 un × R$ 890,00 = R$ 890,00

                *Serviços*
                • Mão de obra de instalação — 2,5 h × R$ 180,00 = R$ 450,00

                Peças: R$ 2.690,00
                Serviços: R$ 450,00
                Desconto (10%): - R$ 314,00
                *Total: R$ 2.826,00*

                Prazo: 10 dias úteis a partir da aprovação
                Validade: até 28/10/2026 (15 dias)
                Condições: 50% na aprovação e 50% na entrega
                Obs.: Peças com garantia do fabricante.

                _O mapa base sai conservador, para o motor pegar com segurança; o acerto final é feito no \
                dinamômetro (ou na rua ou na água), com wideband._
                Oficina Teste (fictícia) · (11) 99999-0000""";
        assertEquals(esperado, TextoWhatsApp.de(dados("João da Silva", StatusOrcamento.ENVIADO, null)));
    }

    /** Deixa um PDF de exemplo em target/ para conferência visual (evidência do bloco 2.5). */
    @Test
    void geraExemploParaConferencia() throws IOException {
        Path destino = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target")
                .resolve("exemplo-orcamento.pdf");
        Files.createDirectories(destino.getParent());
        RelatorioOrcamento.gerar(dados("João da Silva (fictício)", StatusOrcamento.ENVIADO, logo()), destino);
        Files.writeString(destino.resolveSibling("exemplo-orcamento-whatsapp.txt"),
                TextoWhatsApp.de(dados("João da Silva (fictício)", StatusOrcamento.ENVIADO, null)));
        assertTrue(Files.size(destino) > 3_000);
    }

    private static boolean temImagem(Path pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf.toFile())) {
            PDResources recursos = documento.getPage(0).getResources();
            for (COSName nome : recursos.getXObjectNames()) {
                if (recursos.isImageXObject(nome)) {
                    return true;
                }
            }
            return false;
        }
    }
}
