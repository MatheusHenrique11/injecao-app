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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.app.casos.CasosOficina.NovaOs;
import br.com.calibra.app.casos.CasosOficina.OsAberta;
import br.com.calibra.app.casos.CasosOrcamento;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.cadastro.Tracao;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.FormaPagamento;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.cadastro.ClienteSalvo;
import br.com.calibra.dados.cadastro.VeiculoSalvo;
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
            TelaDeTeste.naTelaDoJavaFx(this::novaOsPelaPlacaSemOVeiculo);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * A tela da OS: o orçamento comum enviado e aprovado, os passos do serviço (começar, aguardando peças, retomar,
     * pronto), os anexos com miniatura, os recebimentos em partes (um cancelado) e a entrega com garantia.
     */
    @Test
    void osDoOrcamentoAEntrega() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirBanco);
            TelaDeTeste.naTelaDoJavaFx(() -> ana = oficina.abrir(nova("Ana Costa (fictícia)", "ABC1D23",
                    "Barulho na suspensão dianteira", LocalDate.now().plusDays(2),
                    LocalDateTime.now().minusHours(1).withNano(0))).osId());
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(this::orcamentoDaOs);
            TelaDeTeste.naTelaDoJavaFx(this::passosEAnexos);
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
        TelaDeTeste.<Button>achar(cena, "botaoAbrir").fire();

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
        long primeira = ids("listaOs").getFirst();
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
        assertEquals(oficina.buscar(primeira).veiculo().id(), segunda.veiculo().id(), "o mesmo carro, sem cadastro novo");
        salvar(cena, "oficina-6-lista-com-as-novas.png");
    }

    /** O orçamento da OS comum: itens digitados, enviado e aprovado; o cabeçalho da OS acompanha. */
    private void orcamentoDaOs() throws Exception {
        principal.abrirOs(ana);
        OsController os = principal.osAberta();
        assertEquals("Aberta", texto("status"));
        assertTrue(TelaDeTeste.<Button>achar(cena, "botaoComecar").isDisabled(), "sem orçamento aprovado, não começa");
        assertTrue(texto("proximoPasso").startsWith("Próximo passo: monte o orçamento"), texto("proximoPasso"));
        salvar(cena, "oficina-7-os-entrada.png");

        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaOrcamento"));
        OrcamentoController orcamento = os.orcamento();
        assertEquals(List.of(), orcamento.linhas(), "na OS comum, os itens são digitados");
        assertFalse(this.<Button>daAba("botaoPecasDoCalculo").isVisible(), "sem projeto, sem as peças do cálculo");
        this.<Button>daAba("botaoAdicionarServico").fire();
        orcamento.preencherLinha(0, "Troca dos amortecedores dianteiros", "2", "h", "180,00");
        this.<Button>daAba("botaoAdicionarPeca").fire();
        orcamento.preencherLinha(1, "Amortecedor dianteiro (par)", "1", "par", "640,00");
        this.<Button>daAba("botaoSalvar").fire();
        assertTrue(this.<Button>daAba("botaoPdf").isDisabled(), "o PDF do orçamento da OS comum chega no bloco 3.3");
        assertTrue(this.<Label>daAba("avisoDocumento").isVisible());
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
        ListView<AnexoSalvo> anexos = achar(cena, "listaAnexos");
        assertEquals(List.of(TipoAnexo.FOTO_SERVICO, TipoAnexo.DOCUMENTO), anexos.getItems().stream()
                .map(AnexoSalvo::tipo).toList(), "foto durante o serviço é foto do serviço; PDF é documento");
        anexos.getSelectionModel().selectFirst();
        assertFalse(TelaDeTeste.<Button>achar(cena, "botaoRemoverAnexo").isDisabled(), "em andamento, remove");
        TelaDeTeste.<Button>achar(cena, "botaoPronta").fire();
        assertEquals("Pronta", texto("status"));
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

        os.entregar(new Entrega(agora, 120_150.0, new Garantia(90)));
        assertEquals("Entregue", texto("status"));
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
