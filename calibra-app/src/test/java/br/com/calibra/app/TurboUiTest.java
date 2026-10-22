package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.app.catalogo.ExemplosCatalogo;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.cadastro.Tracao;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.pecas.Cambio;
import br.com.calibra.core.pecas.Turbo;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Teste de interface do bloco 2.4 (opcional, fora do verify padrão): "Sugerir turbos" no caso B, o mapa desenhado com
 * os pontos de potência e de torque, o torque × câmbio, a meta pela pressão e o câmbio do veículo no projeto. Mapas e
 * peças fictícios. Salva imagens em target/.
 */
@Tag("ui")
class TurboUiTest {

    private static final String FONTE = "Ficha fictícia de teste";

    @TempDir
    Path pasta;

    private ContextoApp contexto;
    private CasosCatalogo catalogo;
    private PrincipalController principal;
    private ItemCatalogo<Cambio> cambio;
    private CalculadoraController calculadora;
    private Scene cenaCalculadora;

    @Test
    void turboMetaPelaPressaoETorqueCambio() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirECadastrar);
            TelaDeTeste.naTelaDoJavaFx(this::calculadoraComTurbo);
            TelaDeTeste.esperarCalculo(calculadora);
            TelaDeTeste.naTelaDoJavaFx(this::conferirTurboEMudarAMeta);
            TelaDeTeste.esperarCalculo(calculadora);
            TelaDeTeste.naTelaDoJavaFx(this::conferirMetaPelaPressao);
            TelaDeTeste.naTelaDoJavaFx(this::cambioDoVeiculoNoProjeto);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    private static InfoCatalogo info(String nome) {
        return new InfoCatalogo(nome, FONTE, null, true, null, "Mapa fictício, só para teste.");
    }

    private void turbo(String nome, boolean grande, double eficiencia) {
        catalogo.criar(TipoCatalogo.TURBO, new Turbo(nome, ExemplosCatalogo.mapaCasoB(grande, eficiencia), 0.63, null,
                null, FONTE, null), info(nome));
    }

    private void abrirECadastrar() throws Exception {
        contexto = Partida.abrir(new PastasDados(pasta), "0.2.0-teste", Clock.systemDefaultZone());
        catalogo = new CasosCatalogo(contexto.banco(), contexto.relogio());
        turbo("Turbo teste B (fictício)", true, 0.72);
        turbo("Turbo teste A (fictício)", true, 0.78);
        turbo("Turbo teste pequeno (fictício)", false, 0.78);
        cambio = catalogo.criar(TipoCatalogo.CAMBIO, new Cambio("Câmbio teste 420 N·m", TipoCambio.MANUAL, 420.0, FONTE,
                null), info("Câmbio teste 420 N·m"));
    }

    private void calculadoraComTurbo() throws Exception {
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("calculadora.fxml"));
        Parent raiz = carregador.load();
        calculadora = carregador.getController();
        Scene cena = new Scene(raiz, 1280, 1000);
        cenaCalculadora = cena;
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        calculadora.usarCatalogo(catalogo);
        calculadora.preencher(ValoresDeTeste.casoB());
        calculadora.preencher(Map.of(Campo.RPM_TORQUE, "5.500", Campo.VE_TORQUE, "0,9"));
        TelaDeTeste.<TabPane>achar(cena, "abas").getSelectionModel().select(1);

        TelaDeTeste.<Button>achar(cena, "botaoSugerirTurbo").fire();
        assertEquals("Turbo teste A (fictício)", calculadora.pecasEscolhidas().turbo().info().nome(),
                calculadora.mensagensPecas()::toString);
        assertTrue(calculadora.mensagensPecas().getFirst().contains("Também atendem: Turbo teste B (fictício) (72%)"),
                calculadora.mensagensPecas()::toString);
        assertTrue(calculadora.mensagensPecas().getFirst().contains("A/R da caixa quente"), "compromisso do A/R");
        assertTrue(calculadora.mensagensPecas().stream().anyMatch(m -> m.contains("Turbo teste pequeno (fictício) não "
                + "atende")), calculadora.mensagensPecas()::toString);
        ComboBox<ItemCatalogo<Cambio>> caixaCambio = achar(cena, "catalogoCambio");
        caixaCambio.setValue(caixaCambio.getItems().getLast());
        salvar(cena, "captura-dimensionamento-turbo-pecas.png");

        TelaDeTeste.<Button>achar(cena, "botaoCalcular").fire();
    }

    /** Os passos depois de cada "Calcular" ficam separados: o cálculo roda em segundo plano (decisão 290). */
    private void conferirTurboEMudarAMeta() throws Exception {
        Scene cena = cenaCalculadora;
        cena.getRoot().applyCss();
        cena.getRoot().layout();
        CalculadoraController.CalculoNaTela naTela = calculadora.calculoNaTela().orElseThrow();
        Dimensionamento d = naTela.resultado().valor();
        assertEquals("Turbo teste A (fictício)", d.noMapa().turbo());
        assertTrue(d.noMapa().potencia().dentro() && d.noMapa().torque().dentro(), d.noMapa()::toString);
        assertEquals(Cor.AMARELO, d.avaliacao(Indicador.TORQUE_CAMBIO).orElseThrow().cor(),
                "uns 400 N·m num câmbio de 420 N·m: acima de 90%");
        assertEquals(2, naTela.pecas().usadas().size(), "turbo e câmbio entre as peças usadas");
        Canvas mapa = achar(cena, "graficoMapa");
        salvar(mapa, "captura-mapa-do-compressor.png");
        salvar(cena, "captura-dimensionamento-turbo-resultado.png");

        // Meta pela pressão: 1,4 bar no lugar dos 400 cv.
        TelaDeTeste.<TabPane>achar(cena, "abas").getSelectionModel().select(1);
        calculadora.preencher(Map.of(Campo.TIPO_META, TipoMeta.PRESSAO.name(), Campo.PRESSAO_META, "1,4",
                Campo.UNIDADE_PRESSAO_META, TipoMeta.Unidade.BAR.name()));
        assertTrue(TelaDeTeste.<TextField>achar(cena, "potencia").isDisabled(), "a potência sai do cálculo");
        salvar(cena, "captura-meta-pela-pressao.png");
        TelaDeTeste.<Button>achar(cena, "botaoCalcular").fire();
    }

    private void conferirMetaPelaPressao() throws Exception {
        Scene cena = cenaCalculadora;
        CalculadoraController.CalculoNaTela pelaPressao = calculadora.calculoNaTela().orElseThrow();
        assertTrue(pelaPressao.entrada().meta().pelaPressao());
        assertEquals(140.0, pelaPressao.entrada().meta().pressaoRelativaKpa(), 1e-9);
        assertTrue(pelaPressao.resultado().valor().potencia().cv() > 0, "potência estimada pela pressão");
        salvar(cena, "captura-meta-pela-pressao-resultado.png");
    }

    private void cambioDoVeiculoNoProjeto() throws Exception {
        CasosProjeto casos = new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp());
        EntradaDimensionamento casoB = LeitorFormulario.ler(ValoresDeTeste.casoB(), Parametros.padrao()).entrada();
        Cliente cliente = Cliente.novo("Maria Souza (fictícia)", "(11) 91234-5678", "", "",
                new Consentimento(LocalDate.now(contexto.relogio()), Consentimento.FINALIDADE_PADRAO), "");
        Veiculo carro = new Veiculo(TipoVeiculo.CARRO, "VW", "Gol", "GTi", 1994, 1994, "ABC1D23", "", "", "", "", null,
                TipoCambio.MANUAL, false, Tracao.DIANTEIRA, null, "", "");
        long projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(cliente), new VeiculoNovo(carro, cambio.id()),
                "AP 2.0 turbo", casoB.motor(), casoB.meta()));

        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("principal.fxml"));
        Parent raiz = carregador.load();
        principal = carregador.getController();
        principal.iniciar(contexto, null, new Stage());
        Scene cena = new Scene(raiz, 1380, 900);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        principal.abrirProjeto(projeto);
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(abas.getTabs().stream().filter(t -> "abaDimensionamento".equals(t.getId()))
                .findFirst().orElseThrow());
        ComboBox<ItemCatalogo<Cambio>> caixaCambio = achar(cena, "catalogoCambio");
        assertEquals(cambio.id(), caixaCambio.getValue().id(), "o câmbio do veículo");
        assertTrue(caixaCambio.isDisabled(), "no projeto, o câmbio se troca na aba Veículo");
    }
}
