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

/**
 * PDF e texto de WhatsApp do orçamento (bloco 2.5), com o caso golden B, e do orçamento da OS sem projeto (bloco
 * 3.3). Dados fictícios.
 */
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

    private static DadosOficina oficina() {
        return new DadosOficina("Oficina Teste (fictícia)", "11.222.333/0001-81", "Rua das Bielas, 100 · São Paulo",
                "(11) 99999-0000", "contato@oficina-teste.com.br", null);
    }

    private DadosOrcamento dados(String cliente, StatusOrcamento status, Path logo) {
        EntradaDimensionamento entrada = RelatorioMemoriaTest.casoB();
        Resultado<Dimensionamento> resultado = CalculoDimensionamento.calcular(entrada);
        return new DadosOrcamento(oficina(), logo, cliente, "VW Gol GTi 1994 · ABC1D23", new NumeroOrcamento(2026, 1, 1),
                status, conteudo(), LocalDate.of(2026, 10, 13), new DadosOrcamento.Preparacao("AP 2.0 turbo (v1)",
                        entrada, resultado, List.of(new Justificativa(Indicador.DC_BICO, "Bico maior encomendado.",
                                AGORA))), "0.2.0-teste", AGORA);
    }

    /** O orçamento da OS sem projeto (bloco 3.3): 4 l de óleo e 2 h de mão de obra, sem a parte de preparação. */
    private static DadosOrcamento dadosDaOs(Path logo) {
        Orcamento revisao = new Orcamento(List.of(
                ItemOrcamento.digitado(TipoItem.PECA, "Óleo 5W30 (litro)", BigDecimal.valueOf(4), "l",
                        Dinheiro.ler("45,00")),
                ItemOrcamento.digitado(TipoItem.SERVICO, "Troca de óleo e filtros", BigDecimal.valueOf(2), "h",
                        Dinheiro.ler("180,00"))),
                Desconto.NENHUM, 1, "", 15, "", "");
        return new DadosOrcamento(oficina(), logo, "Ana Costa (fictícia)", "Fiat Uno Mille 2011 · EFG4H56",
                new NumeroOrcamento(2026, 3, 1), StatusOrcamento.ENVIADO, revisao, LocalDate.of(2026, 10, 13), null,
                "0.3.0-teste", AGORA);
    }

    /**
     * Fase 3B: um orçamento de injeção programável, com as linhas de um kit (2 relés, o chicote e a instalação) e a ECU
     * que o cliente traz.
     */
    private static DadosOrcamento dadosComKitEPecaDoCliente(Path logo) {
        Orcamento revisao = new Orcamento(List.of(
                new ItemOrcamento(TipoItem.PECA, "ECU programável (fictícia)", BigDecimal.ONE, "un",
                        Dinheiro.ler("4.900,00"), 11L, 1, true),
                ItemOrcamento.doItem("Relé 12 V 40 A", "un", Dinheiro.ler("18,50"), 12, 1)
                        .multiplicarQuantidade(BigDecimal.valueOf(2)),
                ItemOrcamento.doServico("Confecção de chicote para injeção programável", null, Dinheiro.ler("1.800,00"),
                        null, 21, 1),
                ItemOrcamento.doServico("Instalação de injeção programável", 8.0, null, Dinheiro.ler("180,00"), 22, 1)),
                Desconto.NENHUM, 5, "", 15, "", "");
        return new DadosOrcamento(oficina(), logo, "Carla Reis (fictícia)", "VW Gol GTi 1994 · CDE3F45",
                new NumeroOrcamento(2026, 4, 1), StatusOrcamento.ENVIADO, revisao, LocalDate.of(2026, 10, 13), null,
                "0.3.0-teste", AGORA);
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

    /** O orçamento da OS sem projeto (bloco 3.3) sai sem o motor, a meta, o resumo técnico e a nota do mapa base. */
    @Test
    void pdfDaOsSemProjeto() throws IOException {
        Path destino = pasta.resolve("orcamento-os.pdf");
        RelatorioOrcamento.gerar(dadosDaOs(logo()), destino);
        String texto = texto(destino);
        assertTrue(texto.contains("Orçamento ORC-2026-0003"));
        assertTrue(texto.contains("Ana Costa (fictícia)"));
        assertTrue(texto.contains("Fiat Uno Mille 2011 · EFG4H56"));
        assertTrue(texto.contains("Óleo 5W30 (litro) 4 l R$ 45,00 R$ 180,00"), "linha da tabela");
        assertTrue(texto.contains("Troca de óleo e filtros 2 h R$ 180,00 R$ 360,00"));
        assertTrue(texto.contains("Total R$ 540,00"));
        assertTrue(texto.contains("1 dia útil"));
        for (String deFora : List.of("Motor", "Meta", "Resumo técnico", "mapa base", "dinamômetro")) {
            assertFalse(texto.contains(deFora), deFora);
        }
        assertFalse(texto.contains("?"), "caractere sem fonte virou '?'");
        assertTrue(temImagem(destino), "o logo entra no PDF");
    }

    @Test
    void textoParaWhatsAppDaOsSemProjeto() {
        String esperado = """
                *Orçamento ORC-2026-0003* — Oficina Teste (fictícia)
                Cliente: Ana Costa (fictícia)
                Veículo: Fiat Uno Mille 2011 · EFG4H56

                *Peças*
                • Óleo 5W30 (litro) — 4 l × R$ 45,00 = R$ 180,00

                *Serviços*
                • Troca de óleo e filtros — 2 h × R$ 180,00 = R$ 360,00

                Peças: R$ 180,00
                Serviços: R$ 360,00
                *Total: R$ 540,00*

                Prazo: 1 dia útil
                Validade: até 28/10/2026 (15 dias)

                Oficina Teste (fictícia) · (11) 99999-0000""";
        assertEquals(esperado, TextoWhatsApp.de(dadosDaOs(null)));
        DadosOrcamento semTelefone = new DadosOrcamento(DadosOficina.vazia(), null, "Ana Costa (fictícia)", "Fiat Uno",
                new NumeroOrcamento(2026, 3, 1), StatusOrcamento.ENVIADO, dadosDaOs(null).conteudo(),
                LocalDate.of(2026, 10, 13), null, "0.3.0-teste", AGORA);
        assertTrue(TextoWhatsApp.de(semTelefone).endsWith("Validade: até 28/10/2026 (15 dias)"),
                "sem a nota e sem o telefone, o texto termina na validade");
    }

    /**
     * Fase 3B (SPEC 6.6.4): a peça do cliente sai marcada e com R$ 0,00, fora do total; as linhas do kit saem como as
     * outras.
     */
    @Test
    void pdfComKitEPecaDoCliente() throws IOException {
        Path destino = pasta.resolve("orcamento-kit.pdf");
        RelatorioOrcamento.gerar(dadosComKitEPecaDoCliente(null), destino);
        String texto = texto(destino);
        for (String esperado : List.of("ECU programável (fictícia) (fornecido pelo cliente) 1 un R$ 0,00 R$ 0,00",
                "Relé 12 V 40 A 2 un R$ 18,50 R$ 37,00",
                "Confecção de chicote para injeção programável 1 serviço R$ 1.800,00 R$ 1.800,00",
                "Instalação de injeção programável 8 h R$ 180,00 R$ 1.440,00", "Total R$ 3.277,00")) {
            assertTrue(texto.contains(esperado), () -> "faltou \"" + esperado + "\" em: " + texto);
        }
        assertFalse(texto.contains("?"), "caractere sem fonte virou '?'");
    }

    @Test
    void textoParaWhatsAppComPecaDoCliente() {
        String texto = TextoWhatsApp.de(dadosComKitEPecaDoCliente(null));
        assertTrue(texto.contains("""
                *Peças*
                • ECU programável (fictícia) — 1 un, fornecido pelo cliente
                • Relé 12 V 40 A — 2 un × R$ 18,50 = R$ 37,00"""), texto);
        assertTrue(texto.contains("Peças: R$ 37,00"), "a peça do cliente não soma");
        assertTrue(texto.contains("*Total: R$ 3.277,00*"), texto);
    }

    /** Deixa PDFs de exemplo em target/ para conferência visual (evidência dos blocos 2.5 e 3.3). */
    @Test
    void geraExemploParaConferencia() throws IOException {
        Path destino = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target")
                .resolve("exemplo-orcamento.pdf");
        Files.createDirectories(destino.getParent());
        RelatorioOrcamento.gerar(dados("João da Silva (fictício)", StatusOrcamento.ENVIADO, logo()), destino);
        Files.writeString(destino.resolveSibling("exemplo-orcamento-whatsapp.txt"),
                TextoWhatsApp.de(dados("João da Silva (fictício)", StatusOrcamento.ENVIADO, null)));
        assertTrue(Files.size(destino) > 3_000);
        Path daOs = destino.resolveSibling("exemplo-orcamento-os.pdf");
        RelatorioOrcamento.gerar(dadosDaOs(logo()), daOs);
        Files.writeString(destino.resolveSibling("exemplo-orcamento-os-whatsapp.txt"), TextoWhatsApp.de(dadosDaOs(null)));
        assertTrue(Files.size(daOs) > 1_500, "uma página só, menor que a do orçamento de preparação");
        Path comKit = destino.resolveSibling("exemplo-orcamento-kit-e-peca-do-cliente.pdf");
        RelatorioOrcamento.gerar(dadosComKitEPecaDoCliente(logo()), comKit);
        Files.writeString(destino.resolveSibling("exemplo-orcamento-kit-e-peca-do-cliente-whatsapp.txt"),
                TextoWhatsApp.de(dadosComKitEPecaDoCliente(null)));
        assertTrue(Files.size(comKit) > 1_500);
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
