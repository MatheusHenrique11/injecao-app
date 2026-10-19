package br.com.calibra.relatorios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.TipoItem;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A via do cliente e o comprovante de entrega da OS (bloco 3.3), e as páginas desenhadas para imprimir. */
class RelatorioOsTest {

    @TempDir
    Path pasta;

    private Path logo() throws IOException {
        BufferedImage imagem = new BufferedImage(240, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        g.setColor(new Color(21, 101, 192));
        g.fillRect(0, 0, 240, 80);
        g.dispose();
        Path arquivo = pasta.resolve("logo.png");
        ImageIO.write(imagem, "png", arquivo.toFile());
        return arquivo;
    }

    private static String texto(Path pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf.toFile())) {
            return new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
        }
    }

    @Test
    void viaDoClienteComAVistoriaOTextoEAsAssinaturas() throws IOException {
        Path destino = pasta.resolve("via.pdf");
        RelatorioOs.viaDoCliente(ExemplosOs.entregue(logo()), destino);
        String texto = texto(destino);
        for (String esperado : List.of("Oficina Teste (fictícia)", "Ordem de serviço OS-2026-0007",
                "Via do cliente · aberta em 13/10/2026 08:40", "Cliente: Ana Costa (fictícia) · (11) 91234-5678",
                "Veículo: Fiat Uno Mille 2011 · EFG4H56", "Serviço pedido: Barulho na suspensão dianteira em lombada",
                "Previsão de entrega: 15/10/2026", "Mecânico: Carlos (fictício)", "Chegada: 13/10/2026 09:02",
                "Km: 120.500 km", "Tanque: 1/2", "Lataria e pintura Risco na porta direita",
                "Vidros, faróis e retrovisores nada anotado", "Pneus, rodas e estepe Pneu traseiro esquerdo careca",
                "Objetos deixados no veículo: Estepe e macaco", "Autorizo a oficina a fazer o orçamento",
                "Responsável pela oficina", "via do cliente · gerada em 14/10/2026 17:35")) {
            assertTrue(texto.contains(esperado), () -> "faltou \"" + esperado + "\" em: " + texto);
        }
        assertFalse(texto.contains("?"), "caractere sem fonte virou '?'");
    }

    @Test
    void viaSemOVeiculoESemTexto() throws IOException {
        Path destino = pasta.resolve("via-sem-o-veiculo.pdf");
        RelatorioOs.viaDoCliente(ExemplosOs.semOVeiculo(), destino);
        String texto = texto(destino);
        assertTrue(texto.contains("O veículo ainda não chegou. A entrada (km, tanque e vistoria) é anotada quando ele "
                + "chegar."), texto);
        assertTrue(texto.contains("Oficina (preencha os dados em Configurações)"));
        assertTrue(texto.contains("Cliente: Bruno Dias (fictício) Veículo"), "sem telefone, só o nome");
        assertFalse(texto.contains("Autorizo"), "sem o texto, só as assinaturas");
        assertFalse(texto.contains("Mecânico"));
        assertTrue(texto.contains("Responsável pela oficina"));
    }

    @Test
    void comprovanteComAGarantiaOServicoEOsRecebimentos() throws IOException {
        Path destino = pasta.resolve("comprovante.pdf");
        RelatorioOs.comprovanteDeEntrega(ExemplosOs.entregue(logo()), destino);
        String texto = texto(destino);
        for (String esperado : List.of("Comprovante de entrega", "OS-2026-0007 · entregue em 14/10/2026 17:30",
                "Entregue em: 14/10/2026 17:30", "Km na saída: 120.530 km", "Garantia: 90 dias, até 12/01/2027",
                "Conforme o orçamento ORC-2026-0005 rev. 2, aprovado pelo cliente.",
                "Amortecedor dianteiro (par) 1 par R$ 640,00 R$ 640,00",
                "Troca dos amortecedores dianteiros 2 h R$ 180,00 R$ 360,00", "Total R$ 1.000,00",
                "PIX R$ 500,00 Sinal", "CANCELADO, não conta", "Valor da OS R$ 1.000,00", "Recebido R$ 1.000,00",
                "Falta R$ 0,00", "Situação Pago", "Quem recebeu o veículo (nome e assinatura)")) {
            assertTrue(texto.contains(esperado), () -> "faltou \"" + esperado + "\" em: " + texto);
        }
        assertFalse(texto.contains("Garantia própria"), "sem serviço com garantia própria, como era no 0.3.0");
        assertTrue(texto.indexOf("Amortecedor dianteiro") < texto.indexOf("Troca dos amortecedores"),
                "as peças antes dos serviços");
        assertFalse(texto.contains("?"), "caractere sem fonte virou '?'");
        assertFalse(texto.contains("Recebido a mais"), "pago certinho: sem a linha do que passou");
    }

    /** Revisão pelo agente: o recebido a mais aparece, em vez de sumir atrás do "Pago". */
    @Test
    void comprovanteMostraORecebidoAMais() throws IOException {
        Path destino = pasta.resolve("comprovante-a-mais.pdf");
        RelatorioOs.comprovanteDeEntrega(ExemplosOs.pagaAMais(), destino);
        String texto = texto(destino);
        for (String esperado : List.of("Recebido R$ 1.075,00", "Falta R$ 0,00", "Recebido a mais R$ 75,00",
                "Situação Pago")) {
            assertTrue(texto.contains(esperado), () -> "faltou \"" + esperado + "\" em: " + texto);
        }
    }

    /**
     * Fase 3B (SPEC 6.6.1): os serviços com garantia própria aparecem embaixo da garantia da OS, com o dia em que ela
     * vence (0 dias: "sem garantia"); o serviço sem a própria fica com a da OS e não se repete. Uma OS de três serviços
     * continua numa página só, com a assinatura.
     */
    @Test
    void comprovanteMostraOsServicosComGarantiaPropria() throws IOException {
        Path destino = pasta.resolve("comprovante-garantia-propria.pdf");
        RelatorioOs.comprovanteDeEntrega(ExemplosOs.comGarantiaPorServico(), destino);
        String texto = texto(destino);
        assertTrue(texto.contains("Garantia: 90 dias, até 12/01/2027 Garantia própria: Reparo ou troca de alternador, "
                + "365 dias, até 14/10/2027; Diagnóstico de falha elétrica, sem garantia Serviço feito"), texto);
        assertFalse(texto.contains("Troca dos amortecedores dianteiros,"), "o da garantia da OS não se repete");
        assertFalse(texto.contains("?"), "caractere sem fonte virou '?'");
        try (PDDocument documento = Loader.loadPDF(destino.toFile())) {
            assertEquals(1, documento.getNumberOfPages(), "a assinatura fica na mesma página");
        }

        Path soPecas = pasta.resolve("comprovante-so-pecas.pdf");
        RelatorioOs.comprovanteDeEntrega(ExemplosOs.soComPecas(), soPecas);
        assertFalse(texto(soPecas).contains("Garantia própria"), "sem serviço no orçamento, nada a mais");
    }

    /** Fase 3B (SPEC 6.6.4): no comprovante, a peça do cliente também sai marcada e com R$ 0,00. */
    @Test
    void comprovanteComPecaDoCliente() throws IOException {
        DadosOs d = ExemplosOs.entregue(null);
        Orcamento comEcu = new Orcamento(List.of(new ItemOrcamento(TipoItem.PECA, "ECU programável (fictícia)",
                BigDecimal.ONE, "un", Dinheiro.ler("4.900,00"), 11L, 1, true), ItemOrcamento.digitado(TipoItem.SERVICO,
                "Instalação de injeção programável", BigDecimal.valueOf(8), "h", Dinheiro.ler("180,00"))),
                Desconto.NENHUM, 5, "", 15, "", "");
        DadosOs dados = new DadosOs(d.oficina(), null, d.numero(), d.status(), d.abertaEm(), d.cliente(),
                d.telefoneCliente(), d.veiculo(), false, d.servicoPedido(), d.previsao(), d.mecanico(), d.entrada(),
                d.entrega(), new DadosOs.OrcamentoAprovado("ORC-2026-0009 rev. 1", comEcu), List.of(), List.of(), "",
                d.versaoApp(), d.geradoEm());
        Path destino = pasta.resolve("comprovante-peca-do-cliente.pdf");
        RelatorioOs.comprovanteDeEntrega(dados, destino);
        String texto = texto(destino);
        assertTrue(texto.contains("ECU programável (fictícia) (fornecido pelo cliente) 1 un R$ 0,00 R$ 0,00"), texto);
        assertTrue(texto.contains("Valor da OS R$ 1.440,00"), texto);
    }

    @Test
    void comprovanteSoDepoisDaEntrega() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> RelatorioOs.comprovanteDeEntrega(ExemplosOs.pronta(), pasta.resolve("nao.pdf")));
        assertEquals("A OS OS-2026-0007 ainda não foi entregue.", erro.getMessage());
        assertFalse(Files.exists(pasta.resolve("nao.pdf")));
    }

    @Test
    void embarcacaoEmHorasDeMotor() throws IOException {
        Path via = pasta.resolve("via-lancha.pdf");
        RelatorioOs.viaDoCliente(ExemplosOs.embarcacao(), via);
        assertTrue(texto(via).contains("Horas de motor: 1.250 h"), texto(via));
        Path comprovante = pasta.resolve("comprovante-lancha.pdf");
        RelatorioOs.comprovanteDeEntrega(ExemplosOs.embarcacao(), comprovante);
        String texto = texto(comprovante);
        assertTrue(texto.contains("Horas de motor na saída: 1.252 h"), texto);
        assertTrue(texto.contains("Garantia: sem garantia"), texto);
    }

    /** A impressão desenha as páginas a 200 DPI: a folha A4 inteira, com o texto (não uma página em branco). */
    @Test
    void viaDesenhadaEmA4ParaImprimir() throws IOException {
        Path via = pasta.resolve("via-para-imprimir.pdf");
        RelatorioOs.viaDoCliente(ExemplosOs.entregue(null), via);
        List<BufferedImage> paginas = PaginasPdf.desenhar(via, 200f);
        assertEquals(1, paginas.size());
        BufferedImage pagina = paginas.getFirst();
        assertEquals(210 / 25.4 * 200, pagina.getWidth(), 1.0, "210 mm a 200 DPI");
        assertEquals(297 / 25.4 * 200, pagina.getHeight(), 1.0, "297 mm a 200 DPI");
        int escuros = 0;
        for (int y = 0; y < pagina.getHeight(); y += 2) {
            for (int x = 0; x < pagina.getWidth(); x += 2) {
                if ((pagina.getRGB(x, y) & 0xFF) < 128) {
                    escuros++;
                }
            }
        }
        assertTrue(escuros > 5_000, "o texto aparece na página: " + escuros + " pontos escuros");
    }

    /** Deixa os PDFs e os textos de exemplo em target/ para conferência visual (evidência do bloco 3.3). */
    @Test
    void geraExemplosParaConferencia() throws IOException {
        Path alvo = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target");
        Files.createDirectories(alvo);
        RelatorioOs.viaDoCliente(ExemplosOs.entregue(logo()), alvo.resolve("exemplo-via-do-cliente.pdf"));
        RelatorioOs.comprovanteDeEntrega(ExemplosOs.entregue(logo()),
                alvo.resolve("exemplo-comprovante-de-entrega.pdf"));
        RelatorioOs.comprovanteDeEntrega(ExemplosOs.comGarantiaPorServico(),
                alvo.resolve("exemplo-comprovante-garantia-por-servico.pdf"));
        Files.writeString(alvo.resolve("exemplo-os-whatsapp.txt"), TextoWhatsAppOs.osAberta(ExemplosOs.entregue(null))
                + "\n\n----------\n\n" + TextoWhatsAppOs.prontaParaRetirar(ExemplosOs.pronta()));
        assertTrue(Files.size(alvo.resolve("exemplo-via-do-cliente.pdf")) > 1_500);
        assertTrue(Files.size(alvo.resolve("exemplo-comprovante-de-entrega.pdf")) > 1_500);
    }
}
