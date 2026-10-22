package br.com.calibra.relatorios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.MarchaLenta;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.memoria.VersaoNucleo;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.Construcao;
import br.com.calibra.core.motor.RodaFonica;
import br.com.calibra.core.motor.SugestoesMotor;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Cambio;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.MapaCompressor;
import br.com.calibra.core.pecas.Turbo;
import br.com.calibra.core.pecas.UnidadeVazao;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RelatorioMemoriaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 9, 10, 30);
    private static final String JUSTIFICATIVA = "Cliente vai trocar os bicos por 1.300 cm³/min antes do dinamômetro.";

    /** Caso golden B da SPEC 7.6 (os campos que não entram na conta têm valores de teste). */
    static EntradaDimensionamento casoB() {
        ConfiguracaoMotor motor = SugestoesMotor.aplicar(ConfiguracaoMotor.construtor(), 4)
                .diametroMm(Math.sqrt(2000.0 * 1000.0 / (Math.PI / 4.0 * 92.8 * 4)))
                .cursoMm(92.8)
                .taxaCompressao(9.5)
                .valvulasPorCilindro(4)
                .comando(Comando.ORIGINAL)
                .aspiracao(Aspiracao.TURBO)
                .pistoes(Construcao.ORIGINAL)
                .bielas(Construcao.ORIGINAL)
                .rotacaoMaxima(7500)
                .rotacaoMarchaLenta(900)
                .bicosPorCilindro(1)
                .rodaFonica(new RodaFonica(60, 2, true))
                .combustivel(CombustiveisPadrao.EHC)
                .altitudeLocalM(600)
                .construir();
        return EntradaDimensionamento.de(motor, Meta.noVirabrequim(400, 7000, Uso.RUA), 0.80, 0.95, 25, 300,
                Parametros.padrao())
                .comBico(Bico.informado("1.050 cm³/min", 1050, 300, 0.70, 1.0))
                .comMarchaLenta(new MarchaLenta(40, 0.45))
                .comSensorMap(300.0)
                .comBombaDisponivel(250.0)
                .comInjecao(Injecao.informada("Injeção de teste", 8, 4));
    }

    private static DadosRelatorio dados(List<Justificativa> justificativas) {
        EntradaDimensionamento entrada = casoB();
        Resultado<Dimensionamento> resultado = CalculoDimensionamento.calcular(entrada);
        return new DadosRelatorio(entrada, resultado, justificativas, "0.1.0-teste", AGORA);
    }

    private static List<Justificativa> justificativaDoDc() {
        return List.of(new Justificativa(Indicador.DC_BICO, JUSTIFICATIVA, AGORA));
    }

    private static String texto(File pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento);
        }
    }

    @Test
    void geraOPdfComEntradasSemaforoJustificativaEMemoria(@TempDir Path pasta) throws IOException {
        Path destino = pasta.resolve("memoria.pdf");
        RelatorioMemoria.gerar(dados(justificativaDoDc()), destino);

        assertTrue(Files.size(destino) > 5_000, "PDF pequeno demais");
        String texto = texto(destino.toFile());
        assertTrue(texto.contains("Memória de cálculo"), "título");
        assertTrue(texto.contains("GarageTech 0.1.0-teste"), "versão do app");
        assertTrue(texto.contains("núcleo " + VersaoNucleo.ATUAL), "versão do núcleo");
        assertTrue(texto.contains("09/10/2026 10:30"), "data e hora");
        assertTrue(texto.contains("VERMELHO"), "DC vermelho no semáforo");
        assertTrue(texto.contains("Cliente vai trocar os bicos"), "justificativa");
        assertTrue(texto.contains("Etanol hidratado"), "combustível");
        assertTrue(texto.contains("MAP = Wa × 639,6"), "fórmula do MAP na memória");
        assertTrue(texto.contains("241,0 kPa"), "MAP do caso B");
        assertTrue(texto.contains("lambda"), "λ trocado por texto");
        assertTrue(texto.contains("página 1 de"), "rodapé");
        assertTrue(texto.contains("TI mínimo na marcha lenta 1,0 ms"), "parâmetro que decide aviso");
        assertTrue(texto.contains("Limiares: padrão da SPEC 7.4"), "limiares do semáforo");
        assertFalse(texto.contains("?"), "caractere sem fonte virou '?'");
        try (PDDocument documento = Loader.loadPDF(destino.toFile())) {
            assertTrue(documento.getNumberOfPages() >= 2);
        }
    }

    /** Bloco 2.2: a memória salva num projeto leva o cliente, o veículo e a versão do motor no cabeçalho. */
    @Test
    void memoriaDoProjetoTemClienteVeiculoEMotor(@TempDir Path pasta) throws IOException {
        DadosRelatorio avulso = dados(justificativaDoDc());
        DadosRelatorio doProjeto = new DadosRelatorio(avulso.entrada(), avulso.resultado(), avulso.justificativas(),
                avulso.versaoApp(), avulso.geradoEm(),
                new IdentificacaoProjeto(12, "João da Silva", "VW Gol GTi · ABC1D23", "v2 · AP 2.0 turbo"));
        Path destino = pasta.resolve("memoria-projeto.pdf");
        RelatorioMemoria.gerar(doProjeto, destino);
        String texto = texto(destino.toFile());
        assertTrue(texto.contains("#12"), "número do projeto");
        assertTrue(texto.contains("João da Silva"), "cliente");
        assertTrue(texto.contains("VW Gol GTi · ABC1D23"), "veículo");
        assertTrue(texto.contains("v2 · AP 2.0 turbo"), "versão do motor");

        Path semProjeto = pasta.resolve("memoria-avulsa.pdf");
        RelatorioMemoria.gerar(avulso, semProjeto);
        assertFalse(texto(semProjeto.toFile()).contains("João da Silva"), "a calculadora avulsa não tem projeto");
    }

    /** Bloco 2.4: meta pela pressão, VE do torque, turbo com mapa (fictício) e câmbio aparecem na memória. */
    @Test
    void memoriaComMetaPelaPressaoTurboECambio(@TempDir Path pasta) throws IOException {
        MapaCompressor mapa = new MapaCompressor(UnidadeVazao.KG_S, 298.0, 100.0,
                List.of(ponto(0.10, 1.3), ponto(0.18, 2.5), ponto(0.24, 3.2)),
                List.of(ponto(0.40, 1.3), ponto(0.46, 2.5), ponto(0.48, 3.2)),
                List.of(new MapaCompressor.Ilha(0.75, List.of(ponto(0.20, 1.8), ponto(0.42, 1.8), ponto(0.38, 3.0),
                        ponto(0.26, 3.0)))),
                List.of(new MapaCompressor.LinhaRotacao(120_000, List.of(ponto(0.15, 2.2), ponto(0.45, 1.9))),
                        new MapaCompressor.LinhaRotacao(160_000, List.of(ponto(0.22, 3.1), ponto(0.47, 2.9)))),
                170_000.0);
        EntradaDimensionamento entrada = casoB().comMeta(Meta.pelaPressao(140.0, null, 7000, 4500, Uso.RUA))
                .comVeTorque(0.9)
                .comTurbo(new Turbo("Turbo teste (mapa fictício)", mapa, 0.63, null, null, "Mapa fictício", null))
                .comCambio(new Cambio("Câmbio teste 6M", TipoCambio.MANUAL, 600.0, "Ficha fictícia", null));
        Resultado<Dimensionamento> resultado = CalculoDimensionamento.calcular(entrada);
        List<Justificativa> justificativas = resultado.valor().semaforo().stream()
                .filter(a -> a.cor() == Cor.VERMELHO)
                .map(a -> new Justificativa(a.indicador(), "Teste.", AGORA))
                .toList();
        Path destino = pasta.resolve("memoria-pressao.pdf");
        RelatorioMemoria.gerar(new DadosRelatorio(entrada, resultado, justificativas, "0.2.0-teste", AGORA), destino);

        // O PDF quebra as linhas longas: junta tudo numa linha só para procurar as frases.
        String texto = texto(destino.toFile()).replaceAll("\\s+", " ");
        assertTrue(texto.contains("pressão do turbo 1,40 bar"), "meta pela pressão");
        assertTrue(texto.contains("a potência sai do cálculo"), "a potência não foi dada");
        assertTrue(texto.contains("VE no torque máximo 0,90"), "VE do torque");
        assertTrue(texto.contains("Turbo teste (mapa fictício)"), "turbo");
        assertTrue(texto.contains("pico de eficiência 75%"), "resumo do mapa");
        assertTrue(texto.contains("torque máximo 600 N·m"), "câmbio");
        assertTrue(texto.contains("Torque máximo (estimado)"), "ponto de torque");
        assertTrue(texto.contains("No mapa do Turbo teste (mapa fictício)"), "pontos no mapa");
        assertTrue(texto.contains("torque × câmbio verde até 90%"), "limiares novos");
        assertFalse(texto.contains("NaN"), "a potência vazia da meta não aparece como NaN");
        assertFalse(texto.contains("?"), "caractere sem fonte virou '?'");
    }

    private static MapaCompressor.PontoMapa ponto(double vazaoKgS, double razaoPressao) {
        return new MapaCompressor.PontoMapa(vazaoKgS, razaoPressao);
    }

    @Test
    void naoGeraComVermelhoSemJustificativa(@TempDir Path pasta) {
        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> RelatorioMemoria.gerar(dados(List.of()), pasta.resolve("memoria.pdf")));
        assertTrue(erro.getMessage().contains(Indicador.DC_BICO.rotulo()));
        assertFalse(Files.exists(pasta.resolve("memoria.pdf")));
    }

    /** Deixa um PDF de exemplo em target/ para conferência visual (evidência da fase 1). */
    @Test
    void geraExemploParaConferencia() throws IOException {
        Path destino = Path.of(System.getProperty("basedir", "")).toAbsolutePath()
                .resolve("target").resolve("exemplo-memoria-caso-B.pdf");
        Files.createDirectories(destino.getParent());
        Files.deleteIfExists(destino);
        RelatorioMemoria.gerar(dados(justificativaDoDc()), destino);
        assertTrue(Files.exists(destino));
    }

    @Test
    void apresentacaoEmVariasUnidades() {
        assertEquals("241,0 kPa · 2,41 bar · 34,95 psia", Apresentacao.pressaoAbsoluta(241.0));
        assertEquals("400,0 cv · 394,5 hp · 294,2 kW", Apresentacao.potencia(294.2));
        assertEquals("86,3%", Apresentacao.valor(0.8633, "fração"));
        assertEquals("37,57 lb/min", Apresentacao.valor(37.574, "lb/min"));
        assertEquals("2,91", Apresentacao.valor(2.91, ""));
        assertEquals("4 passos", Apresentacao.valor(4.0, "passos"));
        assertEquals("7.000 rpm", Apresentacao.valor(7000, "rpm"));
    }
}
