package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.imageio.ImageIO;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.app.casos.FormularioOrcamento.TipoDesconto;
import br.com.calibra.app.casos.LogoOficina;
import br.com.calibra.app.catalogo.PecasEscolhidas;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.cadastro.Tracao;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.orcamento.StatusOrcamento;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.config.Chave;

/**
 * Teste de interface do bloco 2.5 (opcional, fora do verify padrão): o orçamento do projeto do cálculo ao aprovado,
 * com PDF, texto de WhatsApp, revisão e os padrões em Configurações. Oficina, cliente e peças fictícios. Salva
 * imagens, o PDF e o texto em target/.
 */
@Tag("ui")
class OrcamentoUiTest {

    private static final String FONTE = "Ficha fictícia de teste";

    @TempDir
    Path pasta;

    private ContextoApp contexto;
    private PrincipalController principal;
    private Scene cena;
    private long projeto;

    @Test
    void doCalculoAoOrcamentoAprovado() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::prepararDados);
            TelaDeTeste.naTelaDoJavaFx(this::abrirProjeto);
            TelaDeTeste.naTelaDoJavaFx(this::montarERascunho);
            TelaDeTeste.naTelaDoJavaFx(this::enviarEAprovar);
            TelaDeTeste.naTelaDoJavaFx(this::configuracoes);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    private void prepararDados() throws Exception {
        contexto = Partida.abrir(new PastasDados(pasta.resolve("dados")), "0.2.0-teste", Clock.systemDefaultZone());
        CasosCatalogo catalogo = new CasosCatalogo(contexto.banco(), contexto.relogio());
        CasosProjeto casos = new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp());

        String logo = LogoOficina.guardar(logoFicticio(), contexto.pastas().anexos());
        contexto.configuracao().gravar(Chave.OFICINA, new DadosOficina("Oficina Teste (fictícia)",
                "11.222.333/0001-81", "Rua das Bielas, 100 · São Paulo", "(11) 99999-0000",
                "contato@oficina-teste.com.br", logo));
        contexto.configuracao().gravar(Chave.ORCAMENTO, new PadroesOrcamento(Desconto.NENHUM, 10,
                "a partir da aprovação e da chegada das peças", 15, "50% na aprovação e 50% na entrega",
                Dinheiro.ler("180,00")));

        Bico bico = new Bico("Bico teste 1050 (fictício)", 1050.0, 300.0, 0.70, TempoMorto.fixo(1.0), null, FONTE, null);
        ItemCatalogo<Bico> item = catalogo.criar(TipoCatalogo.BICO, bico, new InfoCatalogo(bico.nome(), FONTE, null,
                true, Dinheiro.ler("450,00"), ""));
        EntradaDimensionamento casoB = LeitorFormulario.ler(ValoresDeTeste.casoB(), Parametros.padrao()).entrada();
        Cliente cliente = Cliente.novo("João da Silva (fictício)", "(11) 91234-5678", "", "",
                new Consentimento(LocalDate.now(contexto.relogio()), Consentimento.FINALIDADE_PADRAO), "");
        Veiculo carro = new Veiculo(TipoVeiculo.CARRO, "VW", "Gol", "GTi", 1994, 1994, "ABC1D23", "", "", "", "", null,
                TipoCambio.MANUAL, false, Tracao.DIANTEIRA, null, "", "");
        projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(cliente), new VeiculoNovo(carro),
                "AP 2.0 turbo", casoB.motor(), casoB.meta()));
        casos.salvarCalculo(projeto, casoB.comMotor(casos.abrir(projeto).motor().configuracao()).comBico(bico),
                List.of(new Justificativa(Indicador.DC_BICO, "Bico maior encomendado; este é provisório.",
                        LocalDateTime.now().withNano(0))), new PecasEscolhidas(item, null, null, null, null, null));
    }

    private Path logoFicticio() throws Exception {
        BufferedImage imagem = new BufferedImage(300, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        g.setColor(new Color(21, 101, 192));
        g.fillRect(0, 0, 300, 100);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g.drawString("OFICINA TESTE", 60, 58);
        g.dispose();
        Path arquivo = pasta.resolve("logo-ficticio.png");
        ImageIO.write(imagem, "png", arquivo.toFile());
        return arquivo;
    }

    private void abrirProjeto() throws Exception {
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("principal.fxml"));
        Parent raiz = carregador.load();
        principal = carregador.getController();
        principal.iniciar(contexto, null, new Stage());
        cena = new Scene(raiz, 1380, 900);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        principal.abrirProjeto(projeto);
        TabPane abas = achar(cena, "abas");
        Tab aba = abas.getTabs().stream().filter(t -> "abaOrcamento".equals(t.getId())).findFirst().orElseThrow();
        assertEquals("Peças e orçamento", aba.getText());
        abas.getSelectionModel().select(aba);
    }

    private OrcamentoController orcamento() {
        return principal.projetoAberto().orcamento();
    }

    @SuppressWarnings("unchecked")
    private <T extends Node> T daAba(String id) {
        TabPane abas = achar(cena, "abas");
        Node conteudo = abas.getSelectionModel().getSelectedItem().getContent();
        conteudo.applyCss();
        Node no = conteudo.lookup("#" + id);
        assertTrue(no != null, "não achei #" + id + " na aba");
        return (T) no;
    }

    private void montarERascunho() throws Exception {
        OrcamentoController orcamento = orcamento();
        assertTrue(orcamento.atual().isEmpty(), "ainda sem orçamento gravado");
        assertEquals("Orçamento novo", TelaDeTeste.<Label>achar(cena, "numero").getText());
        assertEquals("Bico teste 1050 (fictício)", orcamento.linhas().getFirst().descricao(),
                "a peça do cálculo, com o preço do catálogo");
        assertEquals("4", orcamento.linhas().getFirst().quantidade());
        assertEquals("450,00", orcamento.linhas().getFirst().valor());
        assertTrue(this.<Button>daAba("botaoPdf").isDisabled(), "PDF só depois de gravar");

        this.<Button>daAba("botaoAdicionarServico").fire();
        assertEquals("180,00", orcamento.linhas().get(1).valor(), "o valor da hora de Configurações");
        orcamento.preencherLinha(1, "Mão de obra de instalação e acerto de marcha lenta", "2,5", "h", "180,00");
        this.<ComboBox<TipoDesconto>>daAba("tipoDesconto").setValue(TipoDesconto.PERCENTUAL);
        this.<TextField>daAba("desconto").setText("10");
        assertEquals("R$ 2.025,00", this.<Label>daAba("total").getText(), "(1.800 + 450) − 10%");
        salvar(cena, "captura-orcamento-rascunho.png");

        this.<Button>daAba("botaoSalvar").fire();
        assertEquals(StatusOrcamento.RASCUNHO, orcamento.atual().orElseThrow().orcamento().status(),
                orcamento.mensagens()::toString);
        String numero = "ORC-" + LocalDate.now().getYear() + "-0001";
        assertEquals("Orçamento " + numero, TelaDeTeste.<Label>achar(cena, "numero").getText());
        assertFalse(this.<Button>daAba("botaoPdf").isDisabled());

        Path alvo = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target");
        Files.createDirectories(alvo);
        orcamento.escreverPdf(alvo.resolve("exemplo-orcamento-app.pdf"));
        assertTrue(Files.size(alvo.resolve("exemplo-orcamento-app.pdf")) > 3_000);
        this.<Button>daAba("botaoWhatsApp").fire();
        String texto = Clipboard.getSystemClipboard().getString();
        assertTrue(texto.startsWith("*Orçamento " + numero + "* — Oficina Teste (fictícia)"), texto);
        assertTrue(texto.contains("*Total: R$ 2.025,00*"), texto);
        Files.writeString(alvo.resolve("exemplo-orcamento-whatsapp-app.txt"), texto);
    }

    private void enviarEAprovar() throws Exception {
        OrcamentoController orcamento = orcamento();
        this.<Button>daAba("botaoEnviado").fire();
        assertEquals(StatusOrcamento.ENVIADO, orcamento.atual().orElseThrow().orcamento().status(),
                orcamento.mensagens()::toString);
        assertEquals("Orçado", TelaDeTeste.<Label>achar(cena, "status").getText(), "o projeto passou para Orçado");
        assertTrue(this.<Button>daAba("botaoSalvar").isDisabled(), "enviado não se edita");

        orcamento.aprovar(LocalDateTime.now().minusMinutes(10).withSecond(0).withNano(0), MeioAprovacao.WHATSAPP,
                "Aprovou por mensagem.");
        assertEquals(StatusOrcamento.APROVADO, orcamento.atual().orElseThrow().orcamento().status(),
                orcamento.mensagens()::toString);
        assertEquals("Aprovado", TelaDeTeste.<Label>achar(cena, "status").getText(), "o projeto passou para Aprovado");
        assertFalse(this.<Button>daAba("botaoNovaRevisao").isDisabled());
        salvar(cena, "captura-orcamento-aprovado.png");

        this.<Button>daAba("botaoNovaRevisao").fire();
        assertEquals("ORC-" + LocalDate.now().getYear() + "-0001 rev. 2",
                orcamento.atual().orElseThrow().orcamento().numero().comRevisao());
        assertEquals(StatusOrcamento.RASCUNHO, orcamento.atual().orElseThrow().orcamento().status());
        assertFalse(this.<Button>daAba("botaoSalvar").isDisabled(), "a revisão nova se edita");
        assertEquals(2, this.<TableView<?>>daAba("revisoes").getItems().size());
        // Rola até o fim da aba: os botões de envio e a tabela de revisões aparecem na foto.
        TabPane abas = achar(cena, "abas");
        ScrollPane rolagem = (ScrollPane) abas.getSelectionModel().getSelectedItem().getContent();
        cena.getRoot().applyCss();
        cena.getRoot().layout();
        rolagem.setVvalue(1.0);
        salvar(cena, "captura-orcamento-revisao.png");
    }

    private void configuracoes() throws Exception {
        TelaDeTeste.<ToggleButton>achar(cena, "navConfiguracoes").fire();
        TabPane abas = achar(cena, "abasConfiguracoes");
        abas.getSelectionModel().select(abas.getTabs().stream().filter(t -> "abaOrcamento".equals(t.getId()))
                .findFirst().orElseThrow());
        assertEquals("180,00", TelaDeTeste.<TextField>achar(cena, "orcamentoValorHora").getText());
        TelaDeTeste.<TextField>achar(cena, "orcamentoValidadeDias").setText("10");
        TelaDeTeste.<Button>achar(cena, "botaoSalvarOrcamento").fire();
        assertEquals(10, contexto.configuracao().ler(Chave.ORCAMENTO).validadeDias());
        salvar(cena, "captura-configuracoes-orcamento.png");
    }
}
