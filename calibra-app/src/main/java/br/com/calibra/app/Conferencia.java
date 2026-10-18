package br.com.calibra.app;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.Construcao;
import br.com.calibra.core.motor.RodaFonica;
import br.com.calibra.core.motor.SugestoesMotor;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.relatorios.DadosRelatorio;
import br.com.calibra.relatorios.PaginasPdf;
import br.com.calibra.relatorios.RelatorioMemoria;

/**
 * Conferência do pacote ({@code Inicio --conferir}): roda no Java embutido do instalador, sem abrir a tela, e confere
 * o que o jlink pode deixar de fora sem ninguém perceber: a vírgula decimal (dados do idioma pt-BR), o SQLite (a DLL
 * do sqlite-jdbc) com as migrações, o PDF (fontes e imagens), o JavaFX e as páginas para imprimir (bloco 3.3). Tudo
 * numa pasta temporária: não mexe nos dados da oficina. A lista de módulos (regra 2.1, sem rede) quem confere é o
 * script do instalador.
 */
final class Conferencia {

    /** Um item conferido. */
    record Item(String nome, boolean ok, String detalhe) {
    }

    private Conferencia() {
    }

    /** Confere tudo numa pasta temporária e apaga a pasta no fim. */
    static List<Item> executar() throws IOException {
        Path pasta = Files.createTempDirectory("calibra-conferencia");
        try {
            return executar(pasta);
        } finally {
            apagar(pasta);
        }
    }

    /** Confere tudo usando esta pasta para o banco, os caches e o PDF. */
    static List<Item> executar(Path pasta) {
        PastasDados pastas = new PastasDados(pasta.resolve("dados"));
        List<Item> itens = new ArrayList<>();
        itens.add(conferir("Vírgula decimal (pt-BR)", () -> {
            String numero = NumeroBr.formatar(1234.5, 1);
            String reais = new Dinheiro(123456).formatar();
            return item("1.234,5".equals(numero) && "R$ 1.234,56".equals(reais), numero + " · " + reais);
        }));
        itens.add(conferir("Banco SQLite e migrações", () -> {
            ContextoApp contexto = Partida.abrir(pastas, Versao.atual(), Clock.systemDefaultZone());
            int combustiveis = new CasosCatalogo(contexto.banco(), contexto.relogio()).combustiveis().size();
            return item(combustiveis == 6, "banco na versão " + contexto.migracao().versaoDepois() + ", "
                    + combustiveis + " combustíveis da SPEC 7.2");
        }));
        itens.add(conferir("PDF com fontes e imagens", () -> {
            Path pdf = pasta.resolve("memoria.pdf");
            RelatorioMemoria.gerar(dadosDeExemplo(), pdf);
            boolean imagem = ImageIO.read(CalibraApp.class.getResource("icones/calibra-32.png")) != null;
            long tamanho = Files.size(pdf);
            return item(tamanho > 3_000 && imagem, "memória de " + tamanho / 1024 + " KB"
                    + (imagem ? "; imagens PNG lidas" : "; imagem PNG não abriu"));
        }));
        itens.add(conferir("JavaFX", () -> {
            // Só carrega a classe, sem inicializar: inicializar pediria a tela ligada.
            Class<?> botao = Class.forName("javafx.scene.control.Button", false, Conferencia.class.getClassLoader());
            String origem = botao.getModule().isNamed() ? "módulo " + botao.getModule().getName() : "classpath";
            return item(true, origem);
        }));
        itens.add(conferir("Páginas para imprimir", () -> {
            // A impressão (bloco 3.3) desenha as páginas do PDF; as fontes do PDFBox vão para o cache da pasta.
            Partida.prepararCaches(pastas);
            Path pdf = pasta.resolve("para-imprimir.pdf");
            RelatorioMemoria.gerar(dadosDeExemplo(), pdf);
            List<BufferedImage> paginas = PaginasPdf.desenhar(pdf, 72f);
            BufferedImage primeira = paginas.getFirst();
            boolean a4 = Math.abs(primeira.getWidth() - 595) <= 1 && Math.abs(primeira.getHeight() - 842) <= 1;
            return item(a4, paginas.size() + (paginas.size() == 1 ? " página" : " páginas") + " de "
                    + primeira.getWidth() + " × " + primeira.getHeight() + " pontos (A4 a 72 DPI)");
        }));
        return itens;
    }

    /** Escreve o resultado e devolve o código de saída: 0 se tudo deu certo. */
    static int imprimir(List<Item> itens, PrintStream saida) {
        saida.println("Calibra " + Versao.atual() + " · conferência do pacote · Java " + Runtime.version());
        for (Item item : itens) {
            saida.println((item.ok() ? "OK      " : "FALHOU  ") + item.nome() + ": " + item.detalhe());
        }
        boolean tudoCerto = itens.stream().allMatch(Item::ok);
        saida.println(tudoCerto ? "Tudo certo." : "Há itens com falha.");
        return tudoCerto ? 0 : 1;
    }

    private static Item conferir(String nome, Callable<Item> conferencia) {
        try {
            Item resultado = conferencia.call();
            return new Item(nome, resultado.ok(), resultado.detalhe());
        } catch (Exception | LinkageError erro) {
            return new Item(nome, false, erro.getClass().getSimpleName() + ": " + erro.getMessage());
        }
    }

    private static Item item(boolean ok, String detalhe) {
        return new Item("", ok, detalhe);
    }

    /** O caso golden B da SPEC 7.6, sem bico; os vermelhos levam uma justificativa de teste. */
    private static DadosRelatorio dadosDeExemplo() {
        ConfiguracaoMotor motor = SugestoesMotor.aplicar(ConfiguracaoMotor.construtor(), 4)
                .diametroMm(82.83).cursoMm(92.8).taxaCompressao(9.5).valvulasPorCilindro(4).comando(Comando.ORIGINAL)
                .aspiracao(Aspiracao.TURBO).pistoes(Construcao.ORIGINAL).bielas(Construcao.ORIGINAL)
                .rotacaoMaxima(7500).rotacaoMarchaLenta(900).bicosPorCilindro(1).rodaFonica(new RodaFonica(60, 2, true))
                .combustivel(CombustiveisPadrao.EHC).altitudeLocalM(600).construir();
        EntradaDimensionamento entrada = EntradaDimensionamento.de(motor, Meta.noVirabrequim(400, 7000, Uso.RUA), 0.80,
                0.95, 25, 300, Parametros.padrao());
        Resultado<Dimensionamento> resultado = CalculoDimensionamento.calcular(entrada);
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        List<Justificativa> justificativas = resultado.valor().semaforo().stream()
                .filter(avaliacao -> avaliacao.cor() == Cor.VERMELHO)
                .map(avaliacao -> new Justificativa(avaliacao.indicador(), "Conferência do pacote.", agora))
                .toList();
        return new DadosRelatorio(entrada, resultado, justificativas, Versao.atual(), agora);
    }

    /** Apaga a pasta temporária; o que estiver preso (a DLL do SQLite carregada) fica para o Windows limpar. */
    private static void apagar(Path pasta) {
        try (Stream<Path> caminhos = Files.walk(pasta)) {
            caminhos.sorted(Comparator.reverseOrder()).forEach(caminho -> caminho.toFile().delete());
        } catch (IOException erro) {
            // Pasta temporária: sobrar arquivo não atrapalha nada.
        }
    }
}
