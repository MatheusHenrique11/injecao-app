package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.contemTexto;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.app.casos.CasosOficina.NovaOs;
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
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.oficina.ResumoOs;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;

/**
 * Teste de interface da fase 3 (opcional, fora do verify padrão): o Painel, que é a tela inicial, e a lista da Oficina
 * com a busca e o filtro por status. Clientes e veículos fictícios. Salva imagens das telas em target/.
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
     * Três OS: Ana chegou e a entrega é hoje; Bruno ainda não trouxe o carro e a entrega era ontem; Carla chegou, tem
     * o orçamento aprovado e a entrega é daqui a 3 dias.
     */
    private void prepararDados() throws Exception {
        contexto = Partida.abrir(new PastasDados(pasta.resolve("dados")), "0.3.0-teste", Clock.systemDefaultZone());
        CasosProjeto projetos = new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp());
        oficina = new CasosOficina(contexto.banco(), contexto.relogio(), projetos, contexto.pastas().anexos());
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

    private String numero(StatusOs status) {
        Button cartao = achar(cena, "cartao_" + status.name());
        return ((Label) ((VBox) cartao.getGraphic()).getChildren().getFirst()).getText();
    }

    private List<Long> ids(String tabela) {
        return TelaDeTeste.<TableView<ResumoOs>>achar(cena, tabela).getItems().stream().map(ResumoOs::osId).toList();
    }
}
