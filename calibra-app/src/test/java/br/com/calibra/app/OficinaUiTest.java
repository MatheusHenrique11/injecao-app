package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.apertarOk;
import static br.com.calibra.app.TelaDeTeste.contemTexto;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static br.com.calibra.app.TelaDeTeste.textoInteiro;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.app.casos.CasosOficina.NovaOs;
import br.com.calibra.app.casos.CasosOficina.OsAberta;
import br.com.calibra.app.casos.CasosOrcamento;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.app.casos.FormularioOrcamento.Linha;
import br.com.calibra.app.catalogo.ExemplosCatalogo;
import br.com.calibra.app.catalogo.ExemplosCatalogo.Exemplo;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.cadastro.Tracao;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.FormaPagamento;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.PadroesOs;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemMaterial;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Kit;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.cadastro.ClienteSalvo;
import br.com.calibra.dados.cadastro.VeiculoSalvo;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.config.Chave;
import br.com.calibra.dados.oficina.AnexoSalvo;
import br.com.calibra.dados.oficina.FiltroOs;
import br.com.calibra.dados.oficina.MecanicoSalvo;
import br.com.calibra.dados.oficina.MudancaStatusOs;
import br.com.calibra.dados.oficina.PagamentoSalvo;
import br.com.calibra.dados.oficina.ResumoOs;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;

/**
 * Teste de interface da fase 3 (opcional, fora do verify padrão): o Painel, que é a tela inicial, a lista da Oficina
 * com a busca e o filtro por status, e a Nova OS. Clientes, veículos e fotos fictícios. Salva imagens das telas em
 * target/.
 */
@Tag("ui")
class OficinaUiTest {

    @TempDir
    Path pasta;

    private ContextoApp contexto;
    private CasosOficina oficina;
    private PrincipalController principal;
    private Scene cena;
    private long ana;
    private long bruno;
    private long carla;
    /** A OS aberta pela Nova OS com as fotos, para conferir que a segunda usa o mesmo carro. */
    private long primeiraOs;

    @Test
    void painelEListaDaOficina() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(this::prepararDados);
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(this::painel);
            TelaDeTeste.naTelaDoJavaFx(this::listaDaOficina);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * A Nova OS pelas telas: cliente e veículo novos, com a entrada, a vistoria e as fotos (a HEIC fica fora); depois
     * uma OS do mesmo cliente, achado pela placa, com o orçamento antes do veículo.
     */
    @Test
    void novaOsPelasTelas() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(() -> oficina.novoMecanico("Carlos (fictício)"));
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(this::novaOsComEntradaEFotos);
            TelaDeTeste.esperar(() -> principal.osAberta() != null, "a OS abrir, depois de copiar as fotos");
            TelaDeTeste.naTelaDoJavaFx(this::osAbertaComAsFotos);
            TelaDeTeste.naTelaDoJavaFx(this::novaOsPelaPlacaSemOVeiculo);
            TelaDeTeste.esperar(() -> principal.osAberta() != null, "a segunda OS abrir");
            TelaDeTeste.naTelaDoJavaFx(this::osSemOVeiculo);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * A tela da OS: o orçamento comum enviado e aprovado, os passos do serviço (começar, aguardando peças, retomar,
     * pronto), os anexos com miniatura, os recebimentos em partes (um cancelado), a entrega com garantia e, do bloco
     * 3.3, os documentos da OS (os textos, a via e o comprovante).
     */
    @Test
    void osDoOrcamentoAEntrega() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(() -> ana = oficina.abrir(nova("Ana Costa (fictícia)", "ABC1D23",
                    "Barulho na suspensão dianteira", LocalDate.now().plusDays(2),
                    LocalDateTime.now().minusHours(1).withNano(0))).osId());
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(this::abrirAOsDaAna);
            documentosNaAbertura();
            TelaDeTeste.naTelaDoJavaFx(this::orcamentoDaOs);
            orcamentoPeloWhatsApp();
            TelaDeTeste.naTelaDoJavaFx(this::aprovarOrcamento);
            TelaDeTeste.naTelaDoJavaFx(this::passosEAnexos);
            TelaDeTeste.esperar(() -> TelaDeTeste.<ListView<?>>achar(cena, "listaAnexos").getItems().size() == 2,
                    "a foto e a nota serem copiadas");
            TelaDeTeste.naTelaDoJavaFx(this::anexosEPronta);
            textoProntaParaRetirar();
            TelaDeTeste.esperar(this::miniaturasCarregadas, "as miniaturas dos anexos carregarem");
            TelaDeTeste.naTelaDoJavaFx(this::recebimentosEEntrega);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * As janelas da tela da OS, como a pessoa usa: a entrega (com um erro de digitação na garantia), o recebimento (o
     * valor já vem com o que falta) e o motivo do cancelamento do recebimento (obrigatório).
     */
    @Test
    void janelasDaEntregaEDoRecebimento() throws Throwable {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(this::prontaParaEntregar);
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(() -> principal.abrirOs(ana));

            TelaDeTeste.semEsperar(() -> TelaDeTeste.<Button>achar(cena, "botaoEntregar").fire(), falha);
            Stage entrega = TelaDeTeste.esperarJanela("Entregar o veículo", falha);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                DialogPane painel = TelaDeTeste.painel(entrega);
                assertEquals("90", ((TextField) painel.lookup("#entregaGarantia")).getText(),
                        "a garantia padrão de Configurações › OS já vem preenchida");
                ((TextField) painel.lookup("#entregaGarantia")).setText("noventa");
                apertarOk(painel);
                assertTrue(entrega.isShowing(), "com erro de digitação, a janela fica aberta");
                assertTrue(contemTexto(painel, "Garantia: informe os dias, em número inteiro (0 = sem garantia)."));
                ((TextField) painel.lookup("#entregaGarantia")).setText("90");
                ((TextField) painel.lookup("#entregaUso")).setText("120.300");
                salvar(entrega.getScene(), "oficina-12-janela-entrega.png");
                textoInteiro(painel);
                apertarOk(painel);
            });
            TelaDeTeste.esperar(() -> TelaDeTeste.janelaAberta("Entregar o veículo") == null
                    && "Entregue".equals(texto("status")), "a entrega gravar");

            TelaDeTeste.naTelaDoJavaFx(() -> {
                TabPane abas = achar(cena, "abas");
                abas.getSelectionModel().select(aba(abas, "abaEntrega"));
            });
            TelaDeTeste.semEsperar(() -> TelaDeTeste.<Button>achar(cena, "botaoRegistrarRecebimento").fire(), falha);
            Stage recebimento = TelaDeTeste.esperarJanela("Registrar recebimento", falha);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                DialogPane painel = TelaDeTeste.painel(recebimento);
                assertEquals("360,00", ((TextField) painel.lookup("#recebimentoValor")).getText(), "já vem o que falta");
                @SuppressWarnings("unchecked")
                ComboBox<FormaPagamento> forma = (ComboBox<FormaPagamento>) painel.lookup("#recebimentoForma");
                forma.setValue(FormaPagamento.PIX);
                salvar(recebimento.getScene(), "oficina-13-janela-recebimento.png");
                textoInteiro(painel);
                apertarOk(painel);
            });
            TelaDeTeste.esperar(() -> texto("falta").endsWith("(Pago)"), "o recebimento gravar");

            TelaDeTeste.naTelaDoJavaFx(() -> TelaDeTeste.<TableView<PagamentoSalvo>>achar(cena, "recebimentos")
                    .getSelectionModel().selectFirst());
            TelaDeTeste.semEsperar(() -> TelaDeTeste.<Button>achar(cena, "botaoCancelarRecebimento").fire(), falha);
            Stage motivo = TelaDeTeste.esperarJanela("Cancelar recebimento", falha);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                DialogPane painel = TelaDeTeste.painel(motivo);
                apertarOk(painel);
                assertTrue(motivo.isShowing(), "sem o motivo, não cancela");
                assertTrue(contemTexto(painel, "Escreva o motivo."));
                ((TextArea) painel.lookup("#motivo")).setText("Lançado na OS errada.");
                salvar(motivo.getScene(), "oficina-14-janela-motivo.png");
                textoInteiro(painel);
                apertarOk(painel);
            });
            TelaDeTeste.esperar(() -> texto("falta").endsWith("(Pendente)"), "o cancelamento gravar");
            assertNull(falha.get(), () -> "erro numa janela: " + falha.get());
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * O projeto de preparação criado pela aba Preparação da OS: o assistente começa no motor, com o cliente e o veículo
     * da OS; o projeto nasce ligado a ela, o cabeçalho dele mostra a OS, e o orçamento da OS passa a ser o de
     * preparação (com as peças do cálculo).
     */
    @Test
    void projetoDePreparacaoPelaOs() throws Throwable {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(() -> ana = oficina.abrir(nova("Ana Costa (fictícia)", "ABC1D23",
                    "Preparação: turbo no AP", null, LocalDateTime.now().minusHours(1).withNano(0))).osId());
            TelaDeTeste.naTelaDoJavaFx(this::abrir);

            // Cancelar o assistente aberto pela OS volta para a OS, e a Preparação volta para a lista.
            TelaDeTeste.naTelaDoJavaFx(this::assistentePelaOs);
            TelaDeTeste.semEsperar(() -> TelaDeTeste.<Button>achar(cena, "botaoCancelar").fire(), falha);
            Stage confirmacao = TelaDeTeste.esperarJanela("Cancelar o novo projeto", falha);
            TelaDeTeste.naTelaDoJavaFx(() -> ((Button) TelaDeTeste.painel(confirmacao).lookupButton(ButtonType.YES))
                    .fire());
            TelaDeTeste.esperar(() -> TelaDeTeste.<ToggleButton>achar(cena, "navOficina").isSelected(),
                    "voltar para a OS");
            TelaDeTeste.naTelaDoJavaFx(() -> {
                assertEquals("OS-2026-0001", principal.osAberta().numero());
                TelaDeTeste.<ToggleButton>achar(cena, "navPreparacao").fire();
                assertTrue(TelaDeTeste.<TableView<?>>achar(cena, "tabela").getItems().isEmpty(),
                        "a Preparação voltou para a lista de projetos, sem o assistente cancelado");
            });

            TelaDeTeste.naTelaDoJavaFx(this::projetoPeloAssistente);
            TelaDeTeste.naTelaDoJavaFx(this::osComOProjeto);
            assertNull(falha.get(), () -> "erro numa janela: " + falha.get());
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /** "Criar projeto de preparação…", na aba Preparação da OS da Ana. */
    private void assistentePelaOs() throws Exception {
        principal.abrirOs(ana);
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaPreparacao"));
        TelaDeTeste.<Button>achar(cena, "botaoCriarProjeto").fire();
    }

    private void projetoPeloAssistente() throws Exception {
        assistentePelaOs();
        assertTrue(TelaDeTeste.<ToggleButton>achar(cena, "navPreparacao").isSelected(), "o assistente é da preparação");
        assertEquals("Novo projeto de preparação da OS-2026-0001 · Ana Costa (fictícia) · VW Gol GTi · ABC1D23",
                texto("tituloAssistente"));
        assertTrue(TelaDeTeste.<VBox>achar(cena, "painelMotor").isVisible(), "começa no motor");
        assertTrue(TelaDeTeste.<Button>achar(cena, "botaoVoltar").isDisabled(), "o cliente e o veículo são os da OS");
        // Motor e meta do caso golden B.
        TelaDeTeste.<TextField>achar(cena, "descricaoMotor").setText("AP 2.0 turbo");
        TelaDeTeste.<TextField>achar(cena, "diametro").setText("82,83");
        TelaDeTeste.<TextField>achar(cena, "curso").setText("92,8");
        TelaDeTeste.<TextField>achar(cena, "taxa").setText("9,5");
        TelaDeTeste.<ComboBox<Integer>>achar(cena, "valvulas").setValue(4);
        TelaDeTeste.<ComboBox<Aspiracao>>achar(cena, "aspiracao").setValue(Aspiracao.TURBO);
        TelaDeTeste.<TextField>achar(cena, "rotacaoMaxima").setText("7.500");
        TelaDeTeste.<TextField>achar(cena, "marchaLentaRpm").setText("900");
        TelaDeTeste.<TextField>achar(cena, "dentesTotais").setText("60");
        TelaDeTeste.<CheckBox>achar(cena, "sensorFase").setSelected(true);
        TelaDeTeste.<ComboBox<Combustivel>>achar(cena, "combustivel").setValue(CombustiveisPadrao.EHC);
        TelaDeTeste.<TextField>achar(cena, "altitude").setText("600");
        salvar(cena, "oficina-15-projeto-pela-os.png");
        proximo();
        TelaDeTeste.<TextField>achar(cena, "potencia").setText("400");
        TelaDeTeste.<TextField>achar(cena, "rpmPotencia").setText("7.000");
        TelaDeTeste.<ComboBox<Uso>>achar(cena, "uso").setValue(Uso.RUA);
        proximo();
        TelaDeTeste.<Button>achar(cena, "botaoCriar").fire();

        assertNotNull(principal.projetoAberto(), "criou e abriu o projeto");
        TelaDeTeste.<Button>achar(cena, "botaoFichaVeiculo").fire();
        assertNotNull(principal.fichaAberta(), "o cabeçalho do projeto abre a ficha do veículo");
        assertTrue(TelaDeTeste.<ToggleButton>achar(cena, "navOficina").isSelected(), "a ficha fica na área da Oficina");
        Button voltar = achar(cena, "botaoVoltar");
        assertTrue(voltar.getText().startsWith("← Projeto #"), voltar.getText());
        voltar.fire();
        assertTrue(TelaDeTeste.<ToggleButton>achar(cena, "navPreparacao").isSelected(), "volta para o projeto");
        Button daOs = achar(cena, "botaoAbrirOs");
        assertTrue(daOs.isVisible());
        assertEquals("Abrir a OS-2026-0001 · Aberta", daOs.getText(), "o cabeçalho do projeto mostra a OS");
        salvar(cena, "oficina-16-projeto-com-a-os.png");
        daOs.fire();
    }

    private void osComOProjeto() throws Exception {
        assertTrue(TelaDeTeste.<ToggleButton>achar(cena, "navOficina").isSelected(), "o botão leva à OS");
        assertNotNull(oficina.buscar(ana).os().projetoId(), "o projeto nasceu ligado à OS");
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaPreparacao"));
        assertTrue(texto("resumoPreparacao").startsWith("Projeto de preparação #"), texto("resumoPreparacao"));
        abas.getSelectionModel().select(aba(abas, "abaOrcamento"));
        assertTrue(this.<Button>daAba("botaoPecasDoCalculo").isVisible(), "com projeto, o orçamento usa o cálculo");
        salvar(cena, "oficina-17-os-com-o-projeto.png");
    }

    /**
     * Configurações › OS (bloco 3.3): a garantia, os itens da vistoria e o texto da via são gravados juntos; o mecânico
     * novo e o desativado valem na hora. A Nova OS mostra a vistoria nova e não oferece o mecânico inativo.
     */
    @Test
    void padroesDaOsEMecanicos() throws Throwable {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                TelaDeTeste.<ToggleButton>achar(cena, "navConfiguracoes").fire();
                TabPane abas = achar(cena, "abasConfiguracoes");
                abas.getSelectionModel().select(aba(abas, "abaOs"));
                assertEquals("90", TelaDeTeste.<TextField>achar(cena, "garantiaDias").getText(), "o padrão de fábrica");
                ListView<String> itens = achar(cena, "itensVistoria");
                assertEquals(Vistoria.ITENS_PADRAO, itens.getItems());

                TelaDeTeste.<TextField>achar(cena, "garantiaDias").setText("trinta");
                TelaDeTeste.<Button>achar(cena, "botaoSalvarOs").fire();
                assertTrue(contemTexto(cena.getRoot(), "Garantia padrão: informe os dias, em número inteiro"));

                TelaDeTeste.<TextField>achar(cena, "garantiaDias").setText("30");
                itens.getItems().add("Motor e cofre");
                itens.getSelectionModel().select(4);
                TelaDeTeste.<Button>achar(cena, "botaoSubirItem").fire();
                TelaDeTeste.<TextArea>achar(cena, "textoVia").setText("Texto de teste da via (fictício).");
                TelaDeTeste.<Button>achar(cena, "botaoSalvarOs").fire();
                assertEquals(new PadroesOs(30, List.of("Lataria e pintura", "Vidros, faróis e retrovisores",
                        "Pneus, rodas e estepe", "Motor e cofre", "Interior e painel"),
                        "Texto de teste da via (fictício)."), contexto.configuracao().ler(Chave.OS));
            });

            TelaDeTeste.semEsperar(() -> TelaDeTeste.<Button>achar(cena, "botaoNovoMecanico").fire(), falha);
            Stage nome = TelaDeTeste.esperarJanela("Novo mecânico", falha);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                DialogPane painel = TelaDeTeste.painel(nome);
                ((TextField) painel.lookup(".text-field")).setText("Carlos (fictício)");
                apertarOk(painel);
            });
            TelaDeTeste.esperar(() -> TelaDeTeste.<TableView<MecanicoSalvo>>achar(cena, "mecanicos").getItems()
                    .size() == 1, "o mecânico aparecer na lista");
            TelaDeTeste.naTelaDoJavaFx(() -> {
                TableView<MecanicoSalvo> mecanicos = achar(cena, "mecanicos");
                mecanicos.getSelectionModel().selectFirst();
                Button ativar = achar(cena, "botaoAtivarMecanico");
                assertEquals("Desativar", ativar.getText());
                ativar.fire();
                assertFalse(mecanicos.getItems().getFirst().ativo());
                assertEquals("Reativar", ativar.getText());
                salvar(cena, "oficina-18-configuracoes-os.png");

                TelaDeTeste.<ToggleButton>achar(cena, "navPainel").fire();
                TelaDeTeste.<Button>achar(cena, "botaoNovaOs").fire();
                assertTrue(contemTexto(achar(cena, "gradeVistoria"), "Motor e cofre"), "a Nova OS usa a vistoria nova");
                assertNotNull(TelaDeTeste.<TextField>achar(cena, "vistoria4"), "cinco itens");
                assertEquals(1, TelaDeTeste.<ComboBox<?>>achar(cena, "mecanico").getItems().size(),
                        "só o \"(nenhum)\": o mecânico inativo não entra em OS nova");
            });
            assertNull(falha.get(), () -> "erro numa janela: " + falha.get());
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * O catálogo de serviços (bloco 3.3): a aba Serviços em Catálogos e o "+ Serviço do catálogo…" no orçamento da OS.
     * Sem preço, entram as horas × o valor da hora; com preço, 1 serviço pelo preço. A linha guarda o item e a versão.
     */
    @Test
    void servicoDoCatalogoNoOrcamento() throws Throwable {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        AtomicReference<ItemCatalogo<ServicoPadrao>> porHora = new AtomicReference<>();
        AtomicReference<ItemCatalogo<ServicoPadrao>> comPreco = new AtomicReference<>();
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                CasosCatalogo catalogo = new CasosCatalogo(contexto.banco(), contexto.relogio());
                Exemplo<ServicoPadrao> hora = ExemplosCatalogo.servicoPorHora();
                porHora.set(catalogo.criar(TipoCatalogo.SERVICO, hora.dados(), hora.info()));
                Exemplo<ServicoPadrao> preco = ExemplosCatalogo.servicoComPreco();
                comPreco.set(catalogo.criar(TipoCatalogo.SERVICO, preco.dados(), preco.info()));
                contexto.configuracao().gravar(Chave.ORCAMENTO, new PadroesOrcamento(Desconto.NENHUM, 10, "", 15, "",
                        Dinheiro.ler("180,00")));
                ana = oficina.abrir(nova("Ana Costa (fictícia)", "ABC1D23", "Limpeza de bicos e diagnóstico", null,
                        LocalDateTime.now().minusHours(1).withNano(0))).osId();
            });
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                TelaDeTeste.<ToggleButton>achar(cena, "navCatalogos").fire();
                TabPane abas = achar(cena, "abas");
                abas.getSelectionModel().select(aba(abas, "abaSERVICO"));
                PainelCatalogo<?> servicos = achar(cena, "painelSERVICO");
                assertEquals(2, servicos.tabela().getItems().size());
                salvar(cena, "oficina-19-catalogo-servicos.png");

                principal.abrirOs(ana);
                TabPane abasOs = achar(cena, "abas");
                abasOs.getSelectionModel().select(aba(abasOs, "abaOrcamento"));
            });

            TelaDeTeste.semEsperar(() -> this.<Button>daAba("botaoServicoDoCatalogo").fire(), falha);
            Stage escolha = TelaDeTeste.esperarJanela("Serviço do catálogo", falha);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                DialogPane painel = TelaDeTeste.painel(escolha);
                TableView<?> tabela = (TableView<?>) painel.lookup("#escolhaCatalogo");
                List<String> nomes = tabela.getItems().stream().map(item -> ((ItemCatalogo<?>) item).info().nome())
                        .toList();
                TableColumn<?, ?> precos = tabela.getColumns().stream()
                        .filter(coluna -> coluna.getText().equals("Preço de referência")).findFirst().orElseThrow();
                assertEquals(Dinheiro.ler("250,00").formatar(), precos.getCellData(nomes.indexOf("Diagnóstico teste")),
                        "a escolha mostra o preço de referência");
                tabela.getSelectionModel().select(nomes.indexOf("Limpeza de bicos teste"));
                TelaDeTeste.apertar(painel, "Usar este");
            });
            TelaDeTeste.naTelaDoJavaFx(() -> {
                OrcamentoController orcamento = principal.osAberta().orcamento();
                assertEquals(List.of(new Linha(TipoItem.SERVICO, "Limpeza de bicos teste", "1,5", "h", "180,00",
                        porHora.get().id(), 1, false)), orcamento.linhas(),
                        "sem preço: as horas × o valor da hora; sem descrição, o nome na linha");
                orcamento.adicionarServicoEscolhido(comPreco.get());
                assertEquals(new Linha(TipoItem.SERVICO, "Diagnóstico com scanner e relatório (teste)", "1", "serviço",
                        "250,00", comPreco.get().id(), 1, false), orcamento.linhas().get(1),
                        "com preço: 1 serviço pelo preço; a descrição do serviço na linha");
                assertEquals(Dinheiro.ler("520,00").formatar(), this.<Label>daAba("totalServicos").getText(),
                        "1,5 h × R$ 180 + R$ 250");
                this.<Button>daAba("botaoSalvar").fire();
                List<ItemOrcamento> gravados = orcamento.atual().orElseThrow().orcamento().conteudo().itens();
                assertEquals(List.of(porHora.get().id(), comPreco.get().id()),
                        gravados.stream().map(ItemOrcamento::catalogoItemId).toList(), "a linha guarda o item");
                assertEquals(List.of(1, 1), gravados.stream().map(ItemOrcamento::catalogoVersao).toList());
                salvar(cena, "oficina-20-servico-do-catalogo.png");
            });
            assertNull(falha.get(), () -> "erro numa janela: " + falha.get());
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * Fase 3B (SPEC 6.6.4): "+ Kit do catálogo…" põe uma linha para cada item e serviço do kit; a ECU que o cliente
     * traz, marcada "do cliente", fica com valor zero e travada; sem a linha da instalação, o orçamento fica com o
     * chicote e a ECU do cliente, e grava assim.
     */
    @Test
    void kitEPecaDoClienteNoOrcamento() throws Throwable {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                CasosCatalogo catalogo = new CasosCatalogo(contexto.banco(), contexto.relogio());
                ItemCatalogo<ItemMaterial> ecu = catalogo.criar(TipoCatalogo.ITEM, new ItemMaterial(
                        "ECU programável teste", "un", "Tabela da oficina", null), new InfoCatalogo(
                        "ECU programável teste", "Tabela da oficina", null, false, Dinheiro.ler("4.900,00"), ""));
                ServicoPadrao chicote = new ServicoPadrao("Confecção de chicote teste", null, "Tabela da oficina",
                        null, CategoriaServico.INJECAO_PROGRAMAVEL, "", false, null);
                ItemCatalogo<ServicoPadrao> chicoteNoCatalogo = catalogo.criar(TipoCatalogo.SERVICO, chicote,
                        new InfoCatalogo(chicote.nome(), "Tabela da oficina", null, false, Dinheiro.ler("1.800,00"),
                                ""));
                ServicoPadrao instalacao = new ServicoPadrao("Instalação de injeção teste", 8.0, "Tabela da oficina",
                        null, CategoriaServico.INJECAO_PROGRAMAVEL, "", true, null);
                ItemCatalogo<ServicoPadrao> instalacaoNoCatalogo = catalogo.criar(TipoCatalogo.SERVICO, instalacao,
                        new InfoCatalogo(instalacao.nome(), "Tabela da oficina", null, false, null, ""));
                catalogo.criar(TipoCatalogo.KIT, new Kit("Injeção completa teste", List.of(
                        new Kit.Linha(Kit.TipoLinha.ITEM, ecu.id(), "", BigDecimal.ONE),
                        new Kit.Linha(Kit.TipoLinha.SERVICO, chicoteNoCatalogo.id(), "", BigDecimal.ONE),
                        new Kit.Linha(Kit.TipoLinha.SERVICO, instalacaoNoCatalogo.id(), "", BigDecimal.ONE)),
                        "Tabela da oficina", null), new InfoCatalogo("Injeção completa teste", "Tabela da oficina",
                        null, false, null, ""));
                contexto.configuracao().gravar(Chave.ORCAMENTO, new PadroesOrcamento(Desconto.NENHUM, 10, "", 15, "",
                        Dinheiro.ler("180,00")));
                ana = oficina.abrir(nova("Ana Costa (fictícia)", "ABC1D23", "Injeção programável", null,
                        LocalDateTime.now().minusHours(1).withNano(0))).osId();
            });
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                principal.abrirOs(ana);
                TabPane abasOs = achar(cena, "abas");
                abasOs.getSelectionModel().select(aba(abasOs, "abaOrcamento"));
            });

            TelaDeTeste.semEsperar(() -> this.<Button>daAba("botaoKitDoCatalogo").fire(), falha);
            Stage escolha = TelaDeTeste.esperarJanela("Kit do catálogo", falha);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                DialogPane painel = TelaDeTeste.painel(escolha);
                ((TableView<?>) painel.lookup("#escolhaCatalogo")).getSelectionModel().selectFirst();
                TelaDeTeste.apertar(painel, "Usar este");
            });
            TelaDeTeste.esperar(() -> !escolha.isShowing(), "a escolha do kit fechar");

            TelaDeTeste.naTelaDoJavaFx(() -> {
                OrcamentoController orcamento = principal.osAberta().orcamento();
                assertEquals(List.of("ECU programável teste", "Confecção de chicote teste",
                        "Instalação de injeção teste"), orcamento.linhas().stream().map(Linha::descricao).toList(),
                        "uma linha para cada item e serviço do kit");
                assertEquals(List.of("Kit Injeção completa teste: 3 linhas. Confira os valores e salve."),
                        orcamento.mensagens());
                assertEquals(Dinheiro.ler("4.900,00").formatar(), this.<Label>daAba("totalPecas").getText());
                assertEquals(Dinheiro.ler("3.240,00").formatar(), this.<Label>daAba("totalServicos").getText(),
                        "R$ 1.800 + 8 h × R$ 180");

                orcamento.marcarDoCliente(0, true);
                assertTrue(orcamento.linhas().getFirst().fornecido());
                assertEquals("0,00", orcamento.linhas().getFirst().valor());
                assertTrue(orcamento.valorTravado(0), "o valor da peça do cliente trava");
                assertEquals(Dinheiro.ZERO.formatar(), this.<Label>daAba("totalPecas").getText());
                orcamento.removerLinha(2);
                assertEquals(Dinheiro.ler("1.800,00").formatar(), this.<Label>daAba("totalServicos").getText(),
                        "sem a instalação, só o chicote");
                salvar(cena, "oficina-25-kit-e-peca-do-cliente.png");

                this.<Button>daAba("botaoSalvar").fire();
                Orcamento gravado = orcamento.atual().orElseThrow().orcamento().conteudo();
                assertEquals(2, gravado.itens().size());
                assertTrue(gravado.itens().getFirst().fornecidoPeloCliente(), "a ECU fica marcada como do cliente");
                assertEquals(Dinheiro.ler("1.800,00"), gravado.total());
            });
            assertNull(falha.get(), () -> "erro numa janela: " + falha.get());
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * A ficha do veículo (bloco 3.3), aberta pela OS: o cliente, o veículo editado pela janela, as OS dele e o documento
     * do veículo. Anonimizar apaga o documento pessoal, e o botão de voltar leva de novo à OS.
     */
    @Test
    void fichaDoVeiculo() throws Throwable {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(this::prontaParaEntregar);
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                principal.abrirOs(ana);
                String numero = principal.osAberta().numero();
                TelaDeTeste.<Button>achar(cena, "botaoFichaVeiculo").fire();
                FichaVeiculoController ficha = principal.fichaAberta();
                assertNotNull(ficha, "a OS abre a ficha do veículo");
                assertEquals("← " + numero, TelaDeTeste.<Button>achar(cena, "botaoVoltar").getText());
                assertEquals("Ficha do veículo · VW Gol GTi · ABC1D23", texto("titulo"));
                assertEquals("Ana Costa (fictícia)", texto("clienteNome"));
                assertEquals(List.of(ana), ids("ordens"));
                Path crlv = Files.writeString(pasta.resolve("crlv.pdf"), "%PDF-1.7 CRLV fictício");
                ficha.anexos().adicionarAnexos(List.of(crlv));
                assertTrue(ficha.anexos().copiando(), "a cópia roda em segundo plano");
            });
            TelaDeTeste.esperar(() -> !principal.fichaAberta().anexos().copiando(), "o documento ser copiado");
            TelaDeTeste.naTelaDoJavaFx(() -> {
                FichaVeiculoController ficha = principal.fichaAberta();
                ListView<AnexoSalvo> anexos = ficha.anexos().lista();
                assertEquals(1, anexos.getItems().size());
                assertNull(anexos.getItems().getFirst().osId(), "documento do veículo, sem OS");
                assertTrue(anexos.getItems().getFirst().pessoal(), "documento vem marcado como pessoal");
                assertTrue(ficha.mensagens().getFirst().startsWith("Documento anexado ao veículo."),
                        ficha.mensagens()::toString);
                salvar(cena, "oficina-21-ficha-do-veiculo.png");
            });

            TelaDeTeste.semEsperar(() -> TelaDeTeste.<Button>achar(cena, "botaoEditarVeiculo").fire(), falha);
            Stage edicao = TelaDeTeste.esperarJanela("Editar veículo", falha);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                DialogPane painel = TelaDeTeste.painel(edicao);
                painel.applyCss();
                painel.layout();
                TextField cor = (TextField) painel.lookup("#cor");
                assertNotNull(cor, "o formulário do veículo dentro da janela");
                cor.setText("Vermelho");
                apertarOk(painel);
            });
            TelaDeTeste.esperar(() -> !edicao.isShowing(), "a janela de edição fechar");
            TelaDeTeste.naTelaDoJavaFx(() -> {
                assertTrue(texto("veiculoDetalhes").contains("cor Vermelho"), texto("veiculoDetalhes"));
                FichaVeiculoController ficha = principal.fichaAberta();
                ficha.anonimizarCliente();
                assertTrue(texto("clienteNome").startsWith("Cliente anonimizado #"), texto("clienteNome"));
                assertEquals(List.of(), ficha.anexos().lista().getItems(), "o documento pessoal saiu");
                assertEquals(List.of("Cliente anonimizado. 1 documento pessoal foi apagado."), ficha.mensagens());
                assertTrue(TelaDeTeste.<Button>achar(cena, "botaoEditarCliente").isDisabled());
                salvar(cena, "oficina-22-ficha-anonimizada.png");
                TelaDeTeste.<Button>achar(cena, "botaoVoltar").fire();
                assertNotNull(principal.osAberta(), "o botão de voltar leva de novo à OS");
                assertTrue(texto("titulo").contains("Cliente anonimizado #"), "a OS mostra o cliente anonimizado");
            });
            assertNull(falha.get(), () -> "erro numa janela: " + falha.get());
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * Fase 3B (bloco 3B.0): a cópia de uma foto grande roda na fila de segundo plano. Com a fila ocupada (como durante
     * um backup comprido), a tela segue respondendo, o botão espera, e a foto entra na lista quando a fila chega nela.
     */
    @Test
    void fotoGrandeNaoTravaATela() throws Throwable {
        CountDownLatch liberar = new CountDownLatch(1);
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(this::prontaParaEntregar);
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            Path grande = fotoGrande("motor-por-dentro.png");
            TelaDeTeste.naTelaDoJavaFx(() -> {
                principal.abrirOs(ana);
                TabPane abas = achar(cena, "abas");
                abas.getSelectionModel().select(aba(abas, "abaAnexos"));
                principal.tarefas().executar("Fila ocupada (teste)", () -> liberar.await(30, TimeUnit.SECONDS),
                        feito -> { }, erro -> { });
                principal.osAberta().adicionarAnexos(List.of(grande));
                assertTrue(TelaDeTeste.<Button>achar(cena, "botaoAdicionarAnexos").isDisabled(), "o botão espera");
            });
            AtomicBoolean vazia = new AtomicBoolean();
            TelaDeTeste.naTelaDoJavaFx(() -> vazia.set(TelaDeTeste.<ListView<?>>achar(cena, "listaAnexos").getItems()
                    .isEmpty()));
            assertTrue(vazia.get(), "com a cópia esperando na fila, a tela respondeu, e a foto ainda não está lá");

            liberar.countDown();
            TelaDeTeste.esperar(() -> TelaDeTeste.<ListView<?>>achar(cena, "listaAnexos").getItems().size() == 1,
                    "a foto ser copiada");
            TelaDeTeste.naTelaDoJavaFx(() -> {
                assertFalse(TelaDeTeste.<Button>achar(cena, "botaoAdicionarAnexos").isDisabled(), "o botão volta");
                AnexoSalvo foto = oficina.anexosDaOs(ana).getFirst();
                assertEquals(Files.size(grande), foto.tamanhoBytes());
                assertEquals(TipoAnexo.FOTO_SERVICO, foto.tipo());
            });
            // A miniatura também carrega em segundo plano, com a foto aberta: espera, para a pasta do teste sair.
            TelaDeTeste.esperar(this::miniaturasCarregadas, "a miniatura da foto carregar");
        } finally {
            liberar.countDown();
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * Revisão final (bloco 3.4): o uso gravado com duas casas (1.250,25 h) aparece com uma na aba Entrada (1.250,3). A
     * trava dos documentos compara o que a tela mostra, e a via não fica presa por uma mudança que ninguém fez. Na
     * revisão pelo agente: corrigir só o tanque grava o uso como estava (1.250,25), sem arredondar.
     */
    @Test
    void usoComDuasCasasNaoTravaOsDocumentos() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                LocalDateTime chegada = LocalDateTime.now().minusHours(1).withNano(0);
                NovaOs base = nova("Ana Costa (fictícia)", "ABC1D23", "Revisão", null, chegada);
                ana = oficina.abrir(new NovaOs(base.cliente(), base.veiculo(), base.servicoPedido(), null, null, "",
                        new EntradaVeiculo(chegada, 1_250.25, NivelCombustivel.MEIO, Vistoria.VAZIA))).osId();
            });
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                principal.abrirOs(ana);
                assertEquals("1.250,3", TelaDeTeste.<TextField>achar(cena, "usoEntrada").getText());
                assertEquals(List.of(), principal.osAberta().pendentes(), "nada mudou: a via sai");

                TelaDeTeste.<ComboBox<NivelCombustivel>>achar(cena, "combustivel").setValue(NivelCombustivel.CHEIO);
                principal.osAberta().salvarEntrada();
                EntradaVeiculo gravada = oficina.buscar(ana).os().entrada();
                assertEquals(NivelCombustivel.CHEIO, gravada.combustivel());
                assertEquals(1_250.25, gravada.uso(), "o uso fica como estava, sem virar 1.250,3");
            });
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * Fase 3B (SPEC 6.6.1): o serviço do catálogo com garantia própria. A janela de entrega diz qual serviço tem a
     * própria, e a aba Entrega e pagamento mostra até quando ela vale. Vale a versão que está no orçamento: o serviço
     * mudou no catálogo depois da aprovação, e a OS continua com os 365 dias.
     */
    @Test
    void garantiaPropriaDoServicoNaEntrega() throws Throwable {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(this::prontaComGarantiaPropria);
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(() -> principal.abrirOs(ana));

            TelaDeTeste.semEsperar(() -> TelaDeTeste.<Button>achar(cena, "botaoEntregar").fire(), falha);
            Stage entrega = TelaDeTeste.esperarJanela("Entregar o veículo", falha);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                DialogPane painel = TelaDeTeste.painel(entrega);
                assertEquals("Com garantia própria (do catálogo): Reparo de alternador teste, 365 dias. Os outros "
                        + "serviços ficam com a garantia acima.",
                        ((Label) painel.lookup("#entregaGarantiasProprias")).getText());
                ((TextField) painel.lookup("#entregaUso")).setText("120.300");
                salvar(entrega.getScene(), "oficina-23-janela-entrega-garantia-propria.png");
                textoInteiro(painel);
                apertarOk(painel);
            });
            TelaDeTeste.esperar(() -> TelaDeTeste.janelaAberta("Entregar o veículo") == null
                    && "Entregue".equals(texto("status")), "a entrega gravar");

            TelaDeTeste.naTelaDoJavaFx(() -> {
                TabPane abas = achar(cena, "abas");
                abas.getSelectionModel().select(aba(abas, "abaEntrega"));
                LocalDate hoje = LocalDate.now();
                assertTrue(texto("situacaoEntrega").endsWith("com garantia de 90 dias, até "
                        + Datas.dia(hoje.plusDays(90)) + ". Garantia própria: Reparo de alternador teste, 365 dias, até "
                        + Datas.dia(hoje.plusDays(365)) + "."), texto("situacaoEntrega"));
                salvar(cena, "oficina-24-aba-entrega-garantia-propria.png");

                // O comprovante pelo mesmo caminho do menu Documentos (em target/, para conferir).
                Path alvo = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target");
                Files.createDirectories(alvo);
                Path comprovante = alvo.resolve("exemplo-comprovante-garantia-propria-app.pdf");
                principal.osAberta().escreverComprovante(comprovante);
                try (PDDocument documento = Loader.loadPDF(comprovante.toFile())) {
                    String pdf = new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
                    assertTrue(pdf.contains("Garantia própria: Reparo de alternador teste, 365 dias, até "
                            + Datas.dia(hoje.plusDays(365))), pdf);
                    assertFalse(pdf.contains("Limpeza dos terminais da bateria,"), "a da OS não se repete");
                    assertEquals(1, documento.getNumberOfPages());
                }
            });
            assertNull(falha.get(), () -> "erro numa janela: " + falha.get());
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * Ana, pronta para entregar, com dois serviços aprovados: o "Reparo de alternador teste" do catálogo, com garantia
     * própria de 365 dias, e uma mão de obra digitada (a garantia da OS). Depois da aprovação, o serviço ganha a v2 no
     * catálogo, com 30 dias.
     */
    private void prontaComGarantiaPropria() {
        CasosCatalogo catalogo = new CasosCatalogo(contexto.banco(), contexto.relogio());
        ServicoPadrao alternador = new ServicoPadrao("Reparo de alternador teste", 2.0, "Tabela da oficina", null,
                CategoriaServico.PARTIDA_CARGA, "", true, 365);
        InfoCatalogo info = new InfoCatalogo(alternador.nome(), "Tabela da oficina", null, false, null,
                "Dados fictícios.");
        ItemCatalogo<ServicoPadrao> servico = catalogo.criar(TipoCatalogo.SERVICO, alternador, info);
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        ana = oficina.abrir(nova("Ana Costa (fictícia)", "ABC1D23", "Luz da bateria acesa no painel", null,
                agora.minusHours(3))).osId();
        CasosOrcamento orcamentos = new CasosOrcamento(contexto.banco(), contexto.relogio(),
                new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp()));
        Dinheiro hora = Dinheiro.ler("180,00");
        OrcamentoSalvo orcamento = orcamentos.criarNaOs(ana, new Orcamento(List.of(
                ItemOrcamento.doServico(alternador.textoNoOrcamento(), alternador.horas(), null, hora, servico.id(),
                        servico.versao()),
                ItemOrcamento.digitado(TipoItem.SERVICO, "Limpeza dos terminais da bateria", BigDecimal.ONE, "h", hora)),
                Desconto.NENHUM, 1, "", 15, "", ""));
        orcamentos.marcarEnviado(orcamento.id());
        orcamentos.aprovar(orcamento.id(), agora.minusHours(2), MeioAprovacao.WHATSAPP, "Pode fazer.");
        catalogo.salvar(TipoCatalogo.SERVICO, servico.id(), new ServicoPadrao(alternador.nome(), 2.0,
                "Tabela da oficina", null, CategoriaServico.PARTIDA_CARGA, "", true, 30), info);
        oficina.mudarStatus(ana, StatusOs.EM_EXECUCAO, "");
        oficina.mudarStatus(ana, StatusOs.PRONTA, "");
    }

    /** A OS da Ana pronta para entregar: orçamento de 2 h a R$ 180 aprovado, serviço começado e pronto. */
    private void prontaParaEntregar() {
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        ana = oficina.abrir(nova("Ana Costa (fictícia)", "ABC1D23", "Revisão da suspensão", null,
                agora.minusHours(3))).osId();
        CasosOrcamento orcamentos = new CasosOrcamento(contexto.banco(), contexto.relogio(),
                new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp()));
        OrcamentoSalvo orcamento = orcamentos.criarNaOs(ana, new Orcamento(List.of(
                ItemOrcamento.digitado(TipoItem.SERVICO, "Revisão da suspensão dianteira", BigDecimal.valueOf(2), "h",
                        Dinheiro.ler("180,00"))), Desconto.NENHUM, 1, "", 15, "", ""));
        orcamentos.marcarEnviado(orcamento.id());
        orcamentos.aprovar(orcamento.id(), agora.minusHours(2), MeioAprovacao.WHATSAPP, "Pode fazer.");
        oficina.mudarStatus(ana, StatusOs.EM_EXECUCAO, "");
        oficina.mudarStatus(ana, StatusOs.PRONTA, "");
    }

    private void abrirBanco() throws Exception {
        contexto = Partida.abrir(new PastasDados(pasta.resolve("dados")), "0.3.0-teste", Clock.systemDefaultZone());
        oficina = new CasosOficina(contexto.banco(), contexto.relogio(),
                new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp()), contexto.pastas().anexos());
    }

    /**
     * Três OS: Ana chegou e a entrega é hoje; Bruno ainda não trouxe o carro e a entrega era ontem; Carla chegou, tem
     * o orçamento aprovado e a entrega é daqui a 3 dias.
     */
    private void prepararDados() throws Exception {
        CasosProjeto projetos = new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp());
        CasosOrcamento orcamentos = new CasosOrcamento(contexto.banco(), contexto.relogio(), projetos);
        LocalDate hoje = LocalDate.now(contexto.relogio());
        LocalDateTime agora = LocalDateTime.now(contexto.relogio()).withNano(0);

        ana = oficina.abrir(nova("Ana Costa (fictícia)", "ABC1D23", "Revisão e troca de óleo", hoje,
                agora.minusHours(2))).osId();
        bruno = oficina.abrir(nova("Bruno Dias (fictício)", "BCD2E34", "Embreagem patinando", hoje.minusDays(1),
                null)).osId();
        carla = oficina.abrir(nova("Carla Reis (fictícia)", "CDE3F45", "Barulho na suspensão", hoje.plusDays(3),
                agora.minusHours(1))).osId();
        OrcamentoSalvo orcamento = orcamentos.criarNaOs(carla, new Orcamento(List.of(
                ItemOrcamento.digitado(TipoItem.SERVICO, "Revisão da suspensão dianteira", BigDecimal.valueOf(3), "h",
                        Dinheiro.ler("180,00"))), Desconto.NENHUM, 1, "", 15, "", ""));
        orcamentos.marcarEnviado(orcamento.id());
        orcamentos.aprovar(orcamento.id(), agora.minusMinutes(30), MeioAprovacao.WHATSAPP, "Pode fazer.");
    }

    /** @param chegada null = o veículo ainda não chegou */
    private static NovaOs nova(String nome, String placa, String servico, LocalDate previsao, LocalDateTime chegada) {
        Cliente cliente = Cliente.novo(nome, "(11) 91234-5678", "", "",
                new Consentimento(LocalDate.now(), Consentimento.FINALIDADE_PADRAO), "");
        Veiculo carro = new Veiculo(TipoVeiculo.CARRO, "VW", "Gol", "GTi", 1994, 1994, placa, "", "", "", "", null,
                TipoCambio.MANUAL, false, Tracao.DIANTEIRA, null, "", "");
        EntradaVeiculo entrada = chegada == null ? null
                : new EntradaVeiculo(chegada, 120_000.0, NivelCombustivel.MEIO, Vistoria.VAZIA);
        return new NovaOs(new ClienteNovo(cliente), new VeiculoNovo(carro), servico, null, previsao, "", entrada);
    }

    private void abrir() throws Exception {
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("principal.fxml"));
        Parent raiz = carregador.load();
        principal = carregador.getController();
        principal.iniciar(contexto, null, new Stage());
        cena = new Scene(raiz, 1380, 860);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        assertTrue(TelaDeTeste.<ToggleButton>achar(cena, "navPainel").isSelected(), "o app abre no Painel");
    }

    private void painel() throws Exception {
        assertEquals("2", numero(StatusOs.ABERTA), "Ana e Bruno");
        assertEquals("1", numero(StatusOs.APROVADA), "Carla");
        assertEquals("0", numero(StatusOs.EM_EXECUCAO));
        assertEquals(List.of(ana, carla), ids("naOficina"), "quem chegou primeiro em cima; Bruno ainda não chegou");
        assertEquals(List.of(bruno, ana), ids("entregas"), "a atrasada em cima; a da Carla é daqui a 3 dias");
        assertTrue(contemTexto(cena.getRoot(), "Na oficina agora (2)"));
        assertTrue(contemTexto(cena.getRoot(), "Entregas previstas até hoje (2)"));
        assertTrue(contemTexto(cena.getRoot(), " · atrasada"), "a entrega do Bruno era ontem");
        assertTrue(contemTexto(cena.getRoot(), "Último backup: "), "o backup no Painel");
        salvar(cena, "oficina-1-painel.png");
    }

    private void listaDaOficina() throws Exception {
        TelaDeTeste.<Button>achar(cena, "cartao_ABERTA").fire();
        assertTrue(TelaDeTeste.<ToggleButton>achar(cena, "navOficina").isSelected(), "o cartão abre a Oficina");
        ComboBox<OficinaController.FiltroStatus> filtro = achar(cena, "filtroStatus");
        assertEquals(StatusOs.ABERTA, filtro.getValue().status());
        assertEquals(List.of(bruno, ana), ids("listaOs"), "só as abertas, a que mudou por último em cima");
        salvar(cena, "oficina-2-lista-abertas.png");

        filtro.setValue(OficinaController.FiltroStatus.EM_ANDAMENTO);
        assertEquals(List.of(carla, bruno, ana), ids("listaOs"));
        TextField busca = achar(cena, "busca");
        busca.setText("bcd-2e34");
        assertEquals(List.of(bruno), ids("listaOs"), "pela placa, com o traço");
        busca.setText(oficina.buscar(carla).os().numero().texto());
        assertEquals(List.of(carla), ids("listaOs"), "pelo número da OS");
        busca.setText("ninguém com esse nome");
        assertEquals(List.of(), ids("listaOs"));
        assertTrue(contemTexto(cena.getRoot(), "Nenhuma OS com essa busca."), "o aviso da lista vazia");

        busca.setText("carla");
        TelaDeTeste.<ToggleButton>achar(cena, "navPainel").fire();
        TelaDeTeste.<ToggleButton>achar(cena, "navOficina").fire();
        assertEquals("carla", TelaDeTeste.<TextField>achar(cena, "busca").getText(), "a busca continua lá");
        assertEquals(List.of(carla), ids("listaOs"));
        salvar(cena, "oficina-3-busca.png");
    }

    /** Cliente e veículo novos; a entrada com km, tanque, vistoria, objetos e duas fotos. */
    private void novaOsComEntradaEFotos() throws Exception {
        TelaDeTeste.<Button>achar(cena, "botaoNovaOs").fire();
        assertTrue(TelaDeTeste.<ToggleButton>achar(cena, "navOficina").isSelected(), "a Nova OS fica na Oficina");
        assertTrue(TelaDeTeste.<RadioButton>achar(cena, "clienteNovo").isSelected(), "sem clientes, já vem cliente novo");
        TelaDeTeste.<TextField>achar(cena, "nome").setText("Diego Alves (fictício)");
        TelaDeTeste.<TextField>achar(cena, "telefone").setText("(11) 97654-3210");
        TelaDeTeste.<CheckBox>achar(cena, "consentiu").setSelected(true);
        // Decisão 183 (revisão pelo agente): ir para a lista da Oficina e voltar não perde o que foi digitado.
        principal.abrirOficina(null);
        assertNotNull(principal.novaOsAberta(), "a Nova OS continua guardada");
        TelaDeTeste.<Button>achar(cena, "botaoNovaOs").fire();
        assertEquals("Diego Alves (fictício)", TelaDeTeste.<TextField>achar(cena, "nome").getText());
        proximo();
        TelaDeTeste.<TextField>achar(cena, "marca").setText("Fiat");
        TelaDeTeste.<TextField>achar(cena, "modelo").setText("Uno");
        TelaDeTeste.<TextField>achar(cena, "versao").setText("Mille");
        TelaDeTeste.<TextField>achar(cena, "anoFabricacao").setText("2010");
        TelaDeTeste.<TextField>achar(cena, "anoModelo").setText("2011");
        TelaDeTeste.<TextField>achar(cena, "placa").setText("EFG4H56");
        proximo();

        assertTrue(TelaDeTeste.<CheckBox>achar(cena, "veiculoChegou").isSelected(), "o veículo já está aqui");
        TextField hora = achar(cena, "entradaHora");
        String agora = hora.getText();
        hora.setText("25:00");
        TelaDeTeste.<Button>achar(cena, "botaoAbrir").fire();
        assertTrue(contemTexto(cena.getRoot(), "Descreva o serviço pedido pelo cliente."));
        assertTrue(contemTexto(cena.getRoot(), "Hora da chegada: use horas e minutos, como 09:30."));
        hora.setText(agora);

        TelaDeTeste.<TextArea>achar(cena, "servicoPedido").setText("Barulho na suspensão dianteira em lombada");
        ComboBox<MecanicoSalvo> mecanico = achar(cena, "mecanico");
        mecanico.setValue(mecanico.getItems().stream().filter(Objects::nonNull).findFirst().orElseThrow());
        TelaDeTeste.<DatePicker>achar(cena, "previsao").setValue(LocalDate.now().plusDays(2));
        TelaDeTeste.<TextField>achar(cena, "usoEntrada").setText("120.500");
        TelaDeTeste.<ComboBox<NivelCombustivel>>achar(cena, "combustivel").setValue(NivelCombustivel.MEIO);
        TelaDeTeste.<TextField>achar(cena, "vistoria0").setText("Risco na porta direita");
        TelaDeTeste.<TextField>achar(cena, "objetos").setText("Estepe e macaco");
        Path heic = Files.writeString(pasta.resolve("IMG_0003.heic"), "foto do iPhone (fictícia)");
        principal.novaOsAberta().adicionarFotos(List.of(foto("porta.png"), foto("painel.jpg"), heic));
        assertEquals(2, TelaDeTeste.<ListView<?>>achar(cena, "listaFotos").getItems().size());
        assertTrue(contemTexto(cena.getRoot(), "IMG_0003.heic não é foto em JPG ou PNG."), "a HEIC fica fora");
        salvar(cena, "oficina-4-nova-os.png");
        Button abrir = achar(cena, "botaoAbrir");
        abrir.fire();
        assertTrue(abrir.isDisabled(), "a OS é gravada e as fotos copiadas em segundo plano: o botão espera");
    }

    /** Depois de gravar: a Nova OS fechou e a OS abriu, com a entrada, a vistoria e as duas fotos. */
    private void osAbertaComAsFotos() throws Exception {
        assertNull(principal.novaOsAberta(), "a Nova OS fechou");
        assertNotNull(principal.osAberta(), "e a tela da OS abriu");
        List<ResumoOs> criadas = oficina.listar(FiltroOs.todas());
        assertEquals(1, criadas.size());
        assertEquals(criadas.getFirst().numero().texto(), principal.osAberta().numero());
        OsAberta aberta = oficina.buscar(criadas.getFirst().osId());
        assertEquals("Diego Alves (fictício)", aberta.cliente().cliente().nome());
        assertEquals("Carlos (fictício)", aberta.mecanico().nome());
        assertEquals(LocalDate.now().plusDays(2), aberta.os().previsaoEntrega());
        EntradaVeiculo entrada = aberta.os().entrada();
        assertEquals(120_500.0, entrada.uso());
        assertEquals(NivelCombustivel.MEIO, entrada.combustivel());
        assertEquals(Vistoria.ITENS_PADRAO, entrada.vistoria().itens().stream().map(ItemVistoria::item).toList());
        assertEquals("Risco na porta direita", entrada.vistoria().itens().getFirst().observacao());
        assertEquals("Estepe e macaco", entrada.vistoria().objetos());
        assertEquals(List.of("porta.png", "painel.jpg"), oficina.anexosDaOs(aberta.os().id()).stream()
                .map(AnexoSalvo::nomeOriginal).toList());
        assertEquals(120_500.0, aberta.veiculo().veiculo().usoAcumulado(), "o km da entrada vai para o veículo");
        TelaDeTeste.<Button>achar(cena, "botaoVoltar").fire();
        assertEquals(List.of(aberta.os().id()), ids("listaOs"), "← Oficina volta para a lista");
    }

    /** O cliente achado pela placa já traz o veículo dele escolhido; o orçamento vem antes do veículo. */
    private void novaOsPelaPlacaSemOVeiculo() throws Exception {
        primeiraOs = ids("listaOs").getFirst();
        TelaDeTeste.<Button>achar(cena, "botaoNovaOs").fire();
        TelaDeTeste.<TextField>achar(cena, "buscaCliente").setText("efg-4h56");
        ListView<ClienteSalvo> clientes = achar(cena, "listaClientes");
        assertEquals(1, clientes.getItems().size(), "achou o cliente pela placa do carro dele");
        clientes.getSelectionModel().selectFirst();
        proximo();
        ListView<VeiculoSalvo> veiculos = achar(cena, "listaVeiculos");
        assertEquals("EFG4H56", veiculos.getSelectionModel().getSelectedItem().veiculo().placa());
        proximo();
        assertEquals("Fiat Uno Mille · EFG4H56 — Diego Alves (fictício)",
                TelaDeTeste.<Label>achar(cena, "resumoEscolha").getText());
        assertEquals("no cadastro: 120.500 km", TelaDeTeste.<Label>achar(cena, "dicaUso").getText());
        TelaDeTeste.<TextArea>achar(cena, "servicoPedido").setText("Orçamento de embreagem pelo WhatsApp");
        TelaDeTeste.<CheckBox>achar(cena, "veiculoChegou").setSelected(false);
        salvar(cena, "oficina-5-nova-os-sem-o-veiculo.png");
        TelaDeTeste.<Button>achar(cena, "botaoAbrir").fire();
    }

    /** A OS sem o veículo aberta: a entrada fica para depois, e o próximo passo não espreme os botões. */
    private void osSemOVeiculo() throws Exception {
        assertEquals("Registrar a entrada", TelaDeTeste.<Button>achar(cena, "botaoSalvarEntrada").getText(),
                "o veículo ainda não chegou");
        for (String botao : List.of("botaoComecar", "botaoCancelarOs")) {
            assertFalse(TelaDeTeste.<Button>achar(cena, botao).isTextTruncated(), botao + ": o texto longo do próximo "
                    + "passo não pode espremer os botões");
        }
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaPreparacao"));
        assertTrue(texto("resumoPreparacao").startsWith("Esta OS não tem projeto de preparação"));
        salvar(cena, "oficina-6a-os-sem-o-veiculo-preparacao.png");
        abas.getSelectionModel().select(aba(abas, "abaEntrada"));
        salvar(cena, "oficina-6b-os-sem-o-veiculo-entrada.png");
        TelaDeTeste.<Button>achar(cena, "botaoVoltar").fire();

        List<Long> lista = ids("listaOs");
        assertEquals(2, lista.size());
        OsAberta segunda = oficina.buscar(lista.getFirst());
        assertFalse(segunda.os().veiculoChegou(), "a entrada fica para quando o carro chegar");
        assertEquals(oficina.buscar(primeiraOs).veiculo().id(), segunda.veiculo().id(),
                "o mesmo carro, sem cadastro novo");
        salvar(cena, "oficina-6-lista-com-as-novas.png");
    }

    /** A OS da Ana aberta: o cabeçalho diz o que falta (o orçamento). */
    private void abrirAOsDaAna() throws Exception {
        principal.abrirOs(ana);
        assertEquals("Aberta", texto("status"));
        assertTrue(TelaDeTeste.<Button>achar(cena, "botaoComecar").isDisabled(), "sem orçamento aprovado, não começa");
        assertTrue(texto("proximoPasso").startsWith("Próximo passo: monte o orçamento"), texto("proximoPasso"));
        salvar(cena, "oficina-7-os-entrada.png");
    }

    /** O orçamento da OS comum: itens digitados e o PDF (bloco 3.3). */
    private void orcamentoDaOs() throws Exception {
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaOrcamento"));
        OrcamentoController orcamento = principal.osAberta().orcamento();
        assertEquals(List.of(), orcamento.linhas(), "na OS comum, os itens são digitados");
        assertFalse(this.<Button>daAba("botaoPecasDoCalculo").isVisible(), "sem projeto, sem as peças do cálculo");
        this.<Button>daAba("botaoAdicionarServico").fire();
        orcamento.preencherLinha(0, "Troca dos amortecedores dianteiros", "2", "h", "180,00");
        this.<Button>daAba("botaoAdicionarPeca").fire();
        orcamento.preencherLinha(1, "Amortecedor dianteiro (par)", "1", "par", "640,00");
        this.<Button>daAba("botaoSalvar").fire();
        assertFalse(this.<Button>daAba("botaoPdf").isDisabled(), "o PDF do orçamento da OS comum (bloco 3.3)");
        Path alvo = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target");
        Files.createDirectories(alvo);
        orcamento.escreverPdf(alvo.resolve("exemplo-orcamento-os-app.pdf"));
        assertTrue(Files.size(alvo.resolve("exemplo-orcamento-os-app.pdf")) > 1_500);
    }

    /** O texto do orçamento da OS comum para o WhatsApp (bloco 3.3), sem o motor e a meta. Da thread do teste. */
    private void orcamentoPeloWhatsApp() throws Throwable {
        String whatsapp = TelaDeTeste.copiadoPor(() -> this.<Button>daAba("botaoWhatsApp").fire());
        assertTrue(whatsapp.contains("*Total: R$ 1.000,00*"), whatsapp);
        assertFalse(whatsapp.contains("Motor:"), "sem projeto, o texto sai sem o motor e a meta");
    }

    /** Enviado e aprovado: o cabeçalho da OS acompanha o orçamento. */
    private void aprovarOrcamento() throws Exception {
        OrcamentoController orcamento = principal.osAberta().orcamento();
        this.<Button>daAba("botaoEnviado").fire();
        assertEquals("Aguardando aprovação", texto("status"), "o cabeçalho acompanha o orçamento");
        orcamento.aprovar(LocalDateTime.now().minusMinutes(5).withNano(0), MeioAprovacao.WHATSAPP, "Pode fazer.");
        assertEquals("Aprovada", texto("status"));
        assertEquals(Dinheiro.ler("1.000,00"), oficina.buscar(ana).pagamento().valor(), "2 h a R$ 180 + R$ 640");
        assertFalse(TelaDeTeste.<Button>achar(cena, "botaoComecar").isDisabled(), "aprovado e com a entrada: começa");
        salvar(cena, "oficina-8-os-orcamento.png");
    }

    /** Começar, aguardar peças (com o motivo), retomar, anexar uma foto e a nota e ficar pronta. */
    private void passosEAnexos() throws Exception {
        OsController os = principal.osAberta();
        TelaDeTeste.<Button>achar(cena, "botaoComecar").fire();
        assertEquals("Em execução", texto("status"));
        os.aguardarPecas("Amortecedor dianteiro, chega na quinta");
        assertEquals("Aguardando peças", texto("status"));
        assertEquals("Aguardando peças: Amortecedor dianteiro, chega na quinta. Quando chegarem, retome o serviço.",
                texto("proximoPasso"));
        TelaDeTeste.<Button>achar(cena, "botaoRetomar").fire();
        assertEquals("Em execução", texto("status"));

        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaAnexos"));
        Path nota = Files.writeString(pasta.resolve("nota-fiscal.pdf"), "%PDF-1.7 nota fictícia");
        os.adicionarAnexos(List.of(foto("amortecedor-velho.jpg"), nota));
        assertEquals(List.of(), TelaDeTeste.<ListView<?>>achar(cena, "listaAnexos").getItems(),
                "a cópia roda em segundo plano, fora da thread da tela");
        assertTrue(TelaDeTeste.<Button>achar(cena, "botaoAdicionarAnexos").isDisabled(), "o botão espera a cópia");
    }

    /** Depois da cópia: a foto do serviço e a nota na lista; a OS fica pronta. */
    private void anexosEPronta() throws Exception {
        ListView<AnexoSalvo> anexos = achar(cena, "listaAnexos");
        assertFalse(TelaDeTeste.<Button>achar(cena, "botaoAdicionarAnexos").isDisabled(), "copiou: o botão volta");
        assertEquals(List.of(TipoAnexo.FOTO_SERVICO, TipoAnexo.DOCUMENTO), anexos.getItems().stream()
                .map(AnexoSalvo::tipo).toList(), "foto durante o serviço é foto do serviço; PDF é documento");
        anexos.getSelectionModel().selectFirst();
        assertFalse(TelaDeTeste.<Button>achar(cena, "botaoRemoverAnexo").isDisabled(), "em andamento, remove");
        TelaDeTeste.<Button>achar(cena, "botaoPronta").fire();
        assertEquals("Pronta", texto("status"));
    }

    /** O texto "pronto para retirar" (bloco 3.3), com o valor e o que falta. Da thread do teste. */
    private void textoProntaParaRetirar() throws Throwable {
        TelaDeTeste.naTelaDoJavaFx(() -> {
            assertTrue(item("itemComprovantePdf").isDisable(), "o comprovante só depois da entrega");
            assertFalse(item("itemTextoPronta").isDisable());
        });
        String pronta = TelaDeTeste.copiadoPor(() -> item("itemTextoPronta").fire());
        assertTrue(pronta.startsWith("*" + principal.osAberta().numero() + ": pronto para retirar*"), pronta);
        assertTrue(pronta.contains("Valor: R$ 1.000,00\n*Falta: R$ 1.000,00*"), pronta);
    }

    /**
     * Documentos ▾ com a OS aberta (bloco 3.3): o texto "OS aberta" e a via, que não sai com a abertura ou a entrada
     * mudadas na tela e não salvas. Da thread do teste, por causa da cópia (ver {@link TelaDeTeste#copiadoPor}).
     */
    private void documentosNaAbertura() throws Throwable {
        TelaDeTeste.naTelaDoJavaFx(() -> {
            assertFalse(item("itemViaPdf").isDisable());
            assertTrue(item("itemComprovantePdf").isDisable(), "o comprovante só depois da entrega");
            assertTrue(item("itemTextoPronta").isDisable(), "o pronto para retirar só com a OS pronta");
        });
        String aberta = TelaDeTeste.copiadoPor(() -> item("itemTextoAberta").fire());
        assertTrue(aberta.startsWith("*" + principal.osAberta().numero() + "*"), aberta);
        assertTrue(aberta.contains("\nChegou em ") && aberta.contains("\nServiço pedido: Barulho na suspensão"), aberta);

        TelaDeTeste.naTelaDoJavaFx(() -> {
            OsController os = principal.osAberta();
            assertEquals(List.of(), os.pendentes(), "a tela igual ao gravado");
            TextField vistoria = achar(cena, "vistoria0");
            vistoria.setText("Risco novo, ainda não salvo");
            assertEquals(List.of("a entrada (Salvar a correção da entrada)"), os.pendentes());
            item("itemViaPdf").fire();
            assertEquals(List.of("• Antes do documento, salve o que mudou na aba Entrada: a entrada (Salvar a correção "
                    + "da entrada). O documento usa o que está gravado."), os.mensagens(),
                    "a via não sai sem a vistoria nova");
            vistoria.setText("");
            TelaDeTeste.<TextArea>achar(cena, "servicoPedido").appendText(" e no quebra-molas");
            assertEquals(List.of("a abertura (Salvar abertura)"), os.pendentes());
            TelaDeTeste.<Button>achar(cena, "botaoSalvarAbertura").fire();
            assertEquals(List.of(), os.pendentes(), "salvo, a via pode sair");
        });
    }

    /** Depois da entrega: o comprovante e a via em PDF (em target/, para conferir) e a imagem da página impressa. */
    private void documentosNaEntrega(OsController os) throws IOException {
        assertFalse(item("itemComprovantePdf").isDisable());
        assertFalse(item("itemComprovanteImprimir").isDisable());
        assertTrue(item("itemTextoAberta").isDisable(), "entregue: os textos já passaram");
        assertTrue(item("itemTextoPronta").isDisable());
        Path alvo = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target");
        Files.createDirectories(alvo);
        os.escreverViaDoCliente(alvo.resolve("exemplo-via-do-cliente-app.pdf"));
        os.escreverComprovante(alvo.resolve("exemplo-comprovante-de-entrega-app.pdf"));
        for (String nome : List.of("exemplo-via-do-cliente-app.pdf", "exemplo-comprovante-de-entrega-app.pdf")) {
            assertTrue(Files.size(alvo.resolve(nome)) > 1_500, nome);
            assertEquals("%PDF", Files.readString(alvo.resolve(nome), StandardCharsets.ISO_8859_1).substring(0, 4));
        }

        BufferedImage pagina = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        pagina.setRGB(0, 0, 0xC62828);
        pagina.setRGB(1, 0, 0x1565C0);
        Image imagem = Impressao.imagem(pagina);
        assertEquals(2, imagem.getWidth());
        assertEquals(0xFFC62828, imagem.getPixelReader().getArgb(0, 0), "a página impressa sai com as cores certas");
        assertEquals(0xFF1565C0, imagem.getPixelReader().getArgb(1, 0));
    }

    private MenuItem item(String id) {
        return principal.osAberta().menuDocumentos().getItems().stream().filter(item -> id.equals(item.getId()))
                .findFirst().orElseThrow();
    }

    /** Recebimentos em duas partes (um lançado errado e cancelado), a entrega com garantia e o histórico. */
    private void recebimentosEEntrega() throws Exception {
        salvar(cena, "oficina-9-os-anexos.png");
        OsController os = principal.osAberta();
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaEntrega"));
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        os.registrarRecebimento(new Pagamento(agora.minusMinutes(3), FormaPagamento.PIX, Dinheiro.ler("500,00"),
                "Sinal"));
        assertTrue(texto("falta").endsWith("(Parcial)"), texto("falta"));
        os.registrarRecebimento(new Pagamento(agora.minusMinutes(2), FormaPagamento.CARTAO_DEBITO,
                Dinheiro.ler("50,00"), ""));
        TableView<PagamentoSalvo> recebimentos = achar(cena, "recebimentos");
        os.cancelarRecebimento(recebimentos.getItems().getLast().id(), "Lançado errado.");
        os.registrarRecebimento(new Pagamento(agora.minusMinutes(1), FormaPagamento.DINHEIRO, Dinheiro.ler("500,00"),
                "Resto na entrega"));
        assertTrue(texto("falta").endsWith("(Pago)"), texto("falta"));
        assertEquals(3, recebimentos.getItems().size(), "o cancelado fica na lista");

        // Revisão pelo agente: a vistoria mudada e não salva se perderia na entrega (depois, a OS não muda mais).
        TextField vistoria = achar(cena, "vistoria0");
        String gravada = vistoria.getText();
        vistoria.setText("Arranhão no para-choque, ainda não salvo");
        os.entregar(new Entrega(agora, 120_150.0, new Garantia(90)));
        assertEquals("Pronta", texto("status"), "não entregou");
        assertEquals(List.of("• Antes de entregar, salve o que mudou na aba Entrada: a entrada (Salvar a correção da "
                + "entrada). Depois da entrega, a OS não muda mais."), os.mensagens());
        vistoria.setText(gravada);

        os.entregar(new Entrega(agora, 120_150.0, new Garantia(90)));
        assertEquals("Entregue", texto("status"));
        vistoria.setText("Sobra na tela");
        assertEquals(List.of(), os.pendentes(), "entregue: não há o que salvar, e a via não fica presa");
        documentosNaEntrega(os);
        assertFalse(TelaDeTeste.<Button>achar(cena, "botaoCancelarOs").isVisible(), "entregue não se cancela");
        assertTrue(texto("situacaoEntrega").endsWith("com garantia de 90 dias, até "
                + Datas.dia(agora.toLocalDate().plusDays(90)) + "."), texto("situacaoEntrega"));
        salvar(cena, "oficina-10-os-entrega.png");

        abas.getSelectionModel().select(aba(abas, "abaAnexos"));
        TelaDeTeste.<ListView<AnexoSalvo>>achar(cena, "listaAnexos").getSelectionModel().selectFirst();
        assertTrue(TelaDeTeste.<Button>achar(cena, "botaoRemoverAnexo").isDisabled(), "entregue: o anexo fica");

        abas.getSelectionModel().select(aba(abas, "abaHistorico"));
        assertEquals(List.of(StatusOs.ENTREGUE, StatusOs.PRONTA, StatusOs.EM_EXECUCAO, StatusOs.AGUARDANDO_PECAS,
                StatusOs.EM_EXECUCAO, StatusOs.APROVADA, StatusOs.AGUARDANDO_APROVACAO, StatusOs.ABERTA),
                TelaDeTeste.<TableView<MudancaStatusOs>>achar(cena, "historico").getItems().stream()
                        .map(MudancaStatusOs::status).toList());
        salvar(cena, "oficina-11-os-historico.png");

        TelaDeTeste.<ToggleButton>achar(cena, "navPainel").fire();
        assertEquals(List.of(), ids("naOficina"), "entregue sai do Painel");
    }

    private boolean miniaturasCarregadas() {
        Set<Node> imagens = TelaDeTeste.<ListView<?>>achar(cena, "listaAnexos").lookupAll(".image-view");
        return !imagens.isEmpty() && imagens.stream().map(no -> ((ImageView) no).getImage())
                .allMatch(imagem -> imagem != null && imagem.getProgress() >= 1.0);
    }

    private String texto(String id) {
        return TelaDeTeste.<Label>achar(cena, id).getText();
    }

    /** Um nó da aba escolhida (o conteúdo da aba Orçamento entra pelo código). */
    @SuppressWarnings("unchecked")
    private <T extends Node> T daAba(String id) {
        TabPane abas = achar(cena, "abas");
        Node conteudo = abas.getSelectionModel().getSelectedItem().getContent();
        conteudo.applyCss();
        Node no = conteudo.lookup("#" + id);
        assertNotNull(no, "não achei #" + id + " na aba");
        return (T) no;
    }

    private static Tab aba(TabPane abas, String id) {
        return abas.getTabs().stream().filter(a -> id.equals(a.getId())).findFirst().orElseThrow();
    }

    private void proximo() {
        TelaDeTeste.<Button>achar(cena, "botaoProximo").fire();
    }

    /** Uma foto PNG de uns 5 MB (pontos ao acaso não se comprimem), como a de um celular. */
    private Path fotoGrande(String nome) throws IOException {
        BufferedImage imagem = new BufferedImage(1300, 1300, BufferedImage.TYPE_INT_RGB);
        Random sorteio = new Random(7);
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                imagem.setRGB(x, y, sorteio.nextInt(0x1000000));
            }
        }
        Path arquivo = pasta.resolve(nome);
        ImageIO.write(imagem, "png", arquivo.toFile());
        return arquivo;
    }

    /** Uma foto fictícia (um retângulo azul) em PNG ou JPG, pela extensão do nome. */
    private Path foto(String nome) throws IOException {
        BufferedImage imagem = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D desenho = imagem.createGraphics();
        desenho.setColor(new Color(21, 101, 192));
        desenho.fillRect(0, 0, 320, 240);
        desenho.dispose();
        Path arquivo = pasta.resolve(nome);
        ImageIO.write(imagem, nome.endsWith(".png") ? "png" : "jpg", arquivo.toFile());
        return arquivo;
    }

    private String numero(StatusOs status) {
        Button cartao = achar(cena, "cartao_" + status.name());
        return ((Label) ((VBox) cartao.getGraphic()).getChildren().getFirst()).getText();
    }

    private List<Long> ids(String tabela) {
        return TelaDeTeste.<TableView<ResumoOs>>achar(cena, tabela).getItems().stream().map(ResumoOs::osId).toList();
    }
}
