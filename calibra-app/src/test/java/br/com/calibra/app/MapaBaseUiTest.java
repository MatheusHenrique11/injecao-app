package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.contemTexto;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosMapaBase.SituacaoMapa;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.cadastro.Tracao;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.mapa.ModeloVe;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.catalogo.ItemCatalogo;

/**
 * Teste de interface da aba Mapa base (fase 4, bloco 4.4): gerar em segundo plano, ver os alertas e as tabelas, a conta
 * de uma célula, salvar só com as justificativas e o "desatualizado" depois de mudar as regras em Configurações. Salva
 * imagens em target/. Dados fictícios.
 */
@Tag("ui")
class MapaBaseUiTest {

    @TempDir
    Path pasta;

    private ContextoApp contexto;
    private PrincipalController principal;
    private Scene cena;
    private long projeto;

    @Test
    void geraMostraESalvaOMapaBase() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirProjetoComCalculo);
            TelaDeTeste.naTelaDoJavaFx(this::gerar);
            TelaDeTeste.esperar(() -> !mapaBase().gerando().get(), "o mapa ser gerado");
            TelaDeTeste.naTelaDoJavaFx(this::conferirESalvar);
            TelaDeTeste.naTelaDoJavaFx(this::mudarAsRegras);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    private MapaBaseController mapaBase() {
        return principal.projetoAberto().mapaBase();
    }

    private void abrirProjetoComCalculo() throws Exception {
        contexto = Partida.abrir(new PastasDados(pasta), "0.4.0-teste", Clock.systemDefaultZone());
        CasosProjeto casos = new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp());
        EntradaDimensionamento caso = LeitorFormulario.ler(ValoresDeTeste.casoB(), Parametros.padrao()).entrada();
        caso = caso.comMeta(caso.meta().comRotacaoTorque(5000));
        Cliente cliente = Cliente.novo("Maria Souza (fictícia)", "(11) 91234-5678", "", "",
                new Consentimento(java.time.LocalDate.now(contexto.relogio()), Consentimento.FINALIDADE_PADRAO), "");
        Veiculo carro = new Veiculo(TipoVeiculo.CARRO, "VW", "Gol", "GTi", 1994, 1994, "ABC1D23", "", "", "", "",
                null, TipoCambio.MANUAL, false, Tracao.DIANTEIRA, null, "", "");
        projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(cliente), new VeiculoNovo(carro), "AP 2.0 turbo",
                caso.motor(), caso.meta()));
        casos.salvarCalculo(projeto, caso, List.of(new Justificativa(Indicador.DC_BICO, "Bico provisório.",
                LocalDateTime.now(contexto.relogio()))));

        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("principal.fxml"));
        Parent raiz = carregador.load();
        principal = carregador.getController();
        principal.iniciar(contexto, null, new Stage());
        cena = new Scene(raiz, 1380, 900);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        principal.abrirProjeto(projeto);
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(abas.getTabs().stream().filter(t -> "abaMapaBase".equals(t.getId()))
                .findFirst().orElseThrow());
        assertTrue(TelaDeTeste.<Label>achar(cena, "calculoUsado").getText().startsWith("Cálculo usado: #"));
        ComboBox<ItemCatalogo<ModeloVe>> modelo = achar(cena, "caixaModeloVe");
        assertEquals("modelo-ve:4v-normal", modelo.getValue().chaveSemente(), "o modelo da família já vem escolhido");
        assertTrue(TelaDeTeste.<Button>achar(cena, "botaoSalvarMapa").isDisabled(), "salvar só depois de gerar");
    }

    private void gerar() {
        TelaDeTeste.<TextField>achar(cena, "molaWastegate").setText("0,5");
        TelaDeTeste.<Button>achar(cena, "botaoGerar").fire();
    }

    private void conferirESalvar() throws Exception {
        assertTrue(contemTexto(cena.getRoot(), "Ignição sem modelo"), "o vermelho da ignição");
        assertTrue(contemTexto(cena.getRoot(), "DC do bico no mapa"));
        GridPane combustivel = achar(cena, "tabelaCombustivel");
        assertTrue(combustivel.getChildren().size() > 300, "15 rotações × 26 cargas, com os eixos");
        Label celula = (Label) combustivel.getChildren().stream()
                .filter(no -> GridPane.getRowIndex(no) != null && GridPane.getRowIndex(no) == 1
                        && GridPane.getColumnIndex(no) != null && GridPane.getColumnIndex(no) == 14)
                .findFirst().orElseThrow();
        celula.getOnMouseClicked().handle(null);
        assertTrue(contemTexto(achar(cena, "contaDaCelula"), "Conta da célula 7.000 rpm · 270 kPa"),
                "a conta da célula clicada (rotação nas colunas, a carga maior em cima)");
        assertTrue(contemTexto(cena.getRoot(), "Pressão do turbo alvo"));
        salvar(cena, "captura-mapa-base-gerado.png");
        salvar(TelaDeTeste.<TabPane>achar(cena, "tabelasDoMapa"), "captura-mapa-base-tabela.png");

        TelaDeTeste.<Button>achar(cena, "botaoSalvarMapa").fire();
        assertTrue(mapaBase().mensagensMapa().getFirst().contains("Para salvar, escreva a justificativa de"),
                mapaBase().mensagensMapa()::toString);
        TelaDeTeste.<TextField>achar(cena, "justificativa-IGNICAO_SEM_FONTE").setText("Avanço pelo assistente da "
                + "injeção, conferido com a lâmpada.");
        TelaDeTeste.<TextField>achar(cena, "justificativa-DC_ALTO").setText("Bico maior encomendado.");
        TelaDeTeste.<Button>achar(cena, "botaoSalvarMapa").fire();
        assertTrue(TelaDeTeste.<Label>achar(cena, "estadoMapa").getText().startsWith("Mapa base v1 salvo"),
                TelaDeTeste.<Label>achar(cena, "estadoMapa")::getText);
        TableView<SituacaoMapa> salvos = achar(cena, "mapasSalvos");
        assertEquals(1, salvos.getItems().size());
        assertFalse(salvos.getItems().getFirst().desatualizado());
        assertEquals(StatusProjeto.DIMENSIONADO.rotulo(), TelaDeTeste.<Label>achar(cena, "status").getText(),
                "antes do orçamento aprovado, o status fica");
        salvar(cena, "captura-mapa-base-salvo.png");
    }

    /** Configurações › Mapa base: com uma redução nova, o mapa salvo fica desatualizado. */
    private void mudarAsRegras() throws Exception {
        TelaDeTeste.<ToggleButton>achar(cena, "navConfiguracoes").fire();
        TabPane abas = achar(cena, "abasConfiguracoes");
        abas.getSelectionModel().select(abas.getTabs().stream().filter(t -> "abaRegrasMapaBase".equals(t.getId()))
                .findFirst().orElseThrow());
        TelaDeTeste.<TextField>achar(cena, "reducaoPressao").setText("1");
        TelaDeTeste.<Button>achar(cena, "botaoSalvarRegras").fire();
        assertEquals(1.0, contexto.configuracao().ler(br.com.calibra.dados.config.Chave.MAPA_BASE)
                .reducaoIgnicaoPorPressao(), 1e-9);
        salvar(cena, "captura-configuracoes-mapa-base.png");

        principal.abrirProjeto(projeto);
        TabPane doProjeto = achar(cena, "abas");
        doProjeto.getSelectionModel().select(doProjeto.getTabs().stream()
                .filter(t -> "abaMapaBase".equals(t.getId())).findFirst().orElseThrow());
        TableView<SituacaoMapa> salvos = achar(cena, "mapasSalvos");
        assertTrue(salvos.getItems().getFirst().motivos().contains("as regras do mapa base mudaram em Configurações"),
                salvos.getItems().getFirst().motivos()::toString);
    }
}
