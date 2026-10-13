package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.contemTexto;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
import br.com.calibra.app.catalogo.PecasEscolhidas;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.cadastro.Tracao;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.motor.ModeloMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Intercooler;
import br.com.calibra.core.pecas.Sensor;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.core.pecas.TipoIntercooler;
import br.com.calibra.core.pecas.TipoSensor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Teste de interface do bloco 2.3, parte 2 (opcional, fora do verify padrão): peças do catálogo na calculadora com
 * "Sugerir", "Preencher pelo catálogo" no motor e o projeto com cálculo desatualizado por peça. Salva imagens em
 * target/. Peças fictícias.
 */
@Tag("ui")
class PecasCatalogoUiTest {

    private static final String FONTE = "Ficha fictícia de teste";

    @TempDir
    Path pasta;

    private ContextoApp contexto;
    private CasosCatalogo catalogo;
    private PrincipalController principal;

    @Test
    void pecasDoCatalogoNaCalculadoraNoMotorENoProjeto() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrirECadastrar);
            TelaDeTeste.naTelaDoJavaFx(this::calculadoraComSugestoes);
            TelaDeTeste.naTelaDoJavaFx(this::motorPeloCatalogo);
            TelaDeTeste.naTelaDoJavaFx(this::projetoComPecaDesatualizada);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    private static InfoCatalogo info(String nome) {
        return new InfoCatalogo(nome, FONTE, null, false, null, "");
    }

    private ItemCatalogo<Bico> bico(double vazao) {
        String nome = "Bico teste " + (int) vazao;
        return catalogo.criar(TipoCatalogo.BICO, new Bico(nome, vazao, 300.0, 0.70, TempoMorto.fixo(1.0), null, FONTE,
                null), info(nome));
    }

    private void abrirECadastrar() throws Exception {
        contexto = Partida.abrir(new PastasDados(pasta), "0.2.0-teste", Clock.systemDefaultZone());
        catalogo = new CasosCatalogo(contexto.banco(), contexto.relogio());
        bico(630);
        bico(1050);
        bico(1300);
        catalogo.criar(TipoCatalogo.BOMBA, new Bomba("Bomba teste média", List.of(new Bomba.Ponto(300.0, 13.5, 300.0),
                new Bomba.Ponto(600.0, 13.5, 260.0)), FONTE, null), info("Bomba teste média"));
        catalogo.criar(TipoCatalogo.BOMBA, new Bomba("Bomba teste pequena", List.of(new Bomba.Ponto(300.0, 13.5, 120.0),
                new Bomba.Ponto(600.0, 13.5, 90.0)), FONTE, null), info("Bomba teste pequena"));
        catalogo.criar(TipoCatalogo.INTERCOOLER, new Intercooler("Intercooler teste", TipoIntercooler.AR_AR, 0.75, FONTE,
                null), info("Intercooler teste"));
        catalogo.criar(TipoCatalogo.SENSOR, new Sensor("MAP teste 4 bar", TipoSensor.MAP, 10.0, 400.0, FONTE, null),
                info("MAP teste 4 bar"));
        Injecao injecao = new Injecao("Marca Teste", "ECU 8", 8, 8, null, Set.of(), FONTE, null);
        catalogo.criar(TipoCatalogo.INJECAO, injecao, info(injecao.nome()));
    }

    private void calculadoraComSugestoes() throws Exception {
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("calculadora.fxml"));
        Parent raiz = carregador.load();
        CalculadoraController calculadora = carregador.getController();
        Scene cena = new Scene(raiz, 1280, 1000);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        calculadora.usarCatalogo(catalogo);
        calculadora.preencher(ValoresDeTeste.casoB());
        TelaDeTeste.<TextField>achar(cena, "tensao").setText("13,5");
        TelaDeTeste.<TabPane>achar(cena, "abas").getSelectionModel().select(1);

        TelaDeTeste.<Button>achar(cena, "botaoSugerirBico").fire();
        assertEquals("Bico teste 1300", calculadora.pecasEscolhidas().bico().info().nome(),
                calculadora.mensagensPecas()::toString);
        assertTrue(calculadora.mensagensPecas().getFirst().startsWith("Sugerido: Bico teste 1300"),
                calculadora.mensagensPecas()::toString);
        assertTrue(TelaDeTeste.<TextField>achar(cena, "bicoVazao").isDisabled(), "campos do bico travados");
        assertEquals("1.300", TelaDeTeste.<TextField>achar(cena, "bicoVazao").getText(), "mostra o bico do catálogo");

        TelaDeTeste.<Button>achar(cena, "botaoSugerirBomba").fire();
        assertEquals("Bomba teste média", calculadora.pecasEscolhidas().bomba().info().nome(),
                calculadora.mensagensPecas()::toString);
        TelaDeTeste.<Button>achar(cena, "botaoSugerirInjecao").fire();
        assertEquals("Marca Teste ECU 8", calculadora.pecasEscolhidas().injecao().info().nome(),
                calculadora.mensagensPecas()::toString);
        ComboBox<ItemCatalogo<Intercooler>> intercooler = achar(cena, "catalogoIntercooler");
        intercooler.setValue(intercooler.getItems().getLast());
        assertEquals("0,75", TelaDeTeste.<TextField>achar(cena, "eps").getText(), "a eficiência vira o eps");
        ComboBox<ItemCatalogo<Sensor>> sensor = achar(cena, "catalogoSensorMap");
        sensor.setValue(sensor.getItems().getLast());
        salvar(cena, "captura-calculadora-pecas-catalogo.png");

        TelaDeTeste.<Button>achar(cena, "botaoCalcular").fire();
        raiz.applyCss();
        raiz.layout();
        assertTrue(calculadora.calculoNaTela().isPresent(), "calculou com as peças do catálogo");
        CalculadoraController.CalculoNaTela naTela = calculadora.calculoNaTela().orElseThrow();
        assertEquals(5, naTela.pecas().usadas().size(), "bico, bomba, intercooler, sensor e injeção");
        assertTrue(naTela.resultado().memoria().stream()
                .anyMatch(passo -> passo.grandeza().equals("Vazão da bomba Bomba teste média na pressão exigida")),
                "a bomba entra pela curva, na memória de cálculo");
        assertEquals(0.75, naTela.entrada().parametros().eps(), 1e-12);
        salvar(cena, "captura-calculadora-pecas-resultado.png");
    }

    private void motorPeloCatalogo() throws Exception {
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("motor.fxml"));
        Parent raiz = carregador.load();
        MotorFormController motor = carregador.getController();
        Scene cena = new Scene(raiz, 1100, 900);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        motor.usarCatalogo(catalogo);
        assertTrue(TelaDeTeste.<Button>achar(cena, "botaoPreencherCatalogo").isVisible());

        ItemCatalogo<ModeloMotor> ap = catalogo.ativos(TipoCatalogo.MOTOR).stream()
                .filter(item -> "motor:vw-ap-2.0".equals(item.chaveSemente())).findFirst().orElseThrow();
        TelaDeTeste.<TextField>achar(cena, "taxa").setText("10");
        motor.preencherComModelo(ap);
        assertEquals("82,5", TelaDeTeste.<TextField>achar(cena, "diametro").getText());
        assertEquals("92,8", TelaDeTeste.<TextField>achar(cena, "curso").getText());
        assertEquals("", TelaDeTeste.<TextField>achar(cena, "taxa").getText(), "sem dado no catálogo: fica em branco");
        assertTrue(contemTexto(raiz, "Preenchido com " + ap.info().nome()), "mensagem do preenchimento");
        assertTrue(contemTexto(raiz, "Sem dado no catálogo, preencha: "), "avisa o que faltou");
        assertTrue(contemTexto(raiz, "Confira a ordem de ignição no manual"), "pede conferir a ordem");
        assertEquals(6, TelaDeTeste.<ComboBox<?>>achar(cena, "combustivel").getItems().size(), "combustíveis do catálogo");
        salvar(cena, "captura-motor-pelo-catalogo.png");
    }

    private void projetoComPecaDesatualizada() throws Exception {
        CasosProjeto casos = new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp());
        EntradaDimensionamento casoB = LeitorFormulario.ler(ValoresDeTeste.casoB(), Parametros.padrao()).entrada();
        Cliente cliente = Cliente.novo("Maria Souza (fictícia)", "(11) 91234-5678", "", "",
                new Consentimento(LocalDate.now(contexto.relogio()), Consentimento.FINALIDADE_PADRAO), "");
        Veiculo carro = new Veiculo(TipoVeiculo.CARRO, "VW", "Gol", "GTi", 1994, 1994, "ABC1D23", "", "", "", "", null,
                TipoCambio.MANUAL, false, Tracao.DIANTEIRA, null, "", "");
        long projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(cliente), new VeiculoNovo(carro),
                "AP 2.0 turbo", casoB.motor(), casoB.meta()));
        ItemCatalogo<Bico> bico1050 = catalogo.listar(TipoCatalogo.BICO, "1050", false).getFirst();
        EntradaDimensionamento comBico = casoB.comMotor(casos.abrir(projeto).motor().configuracao())
                .comBico(bico1050.dados());
        casos.salvarCalculo(projeto, comBico, List.of(new Justificativa(Indicador.DC_BICO, "Bico provisório.",
                LocalDateTime.now().withNano(0))), new PecasEscolhidas(bico1050, null, null, null, null, null));
        Bico menosMorto = new Bico(bico1050.dados().nome(), 1050.0, 300.0, 0.70, TempoMorto.fixo(0.9), null, FONTE, null);
        catalogo.salvar(TipoCatalogo.BICO, bico1050.id(), menosMorto, bico1050.info());

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
        assertTrue(contemTexto(raiz, "o bico mudou no catálogo: Bico teste 1050 v1 → v2"),
                "faixa de desatualizado por peça");
        ComboBox<ItemCatalogo<Bico>> combo = achar(cena, "catalogoBico");
        assertNotNull(combo.getValue(), "o bico do cálculo salvo vem escolhido");
        assertEquals(2, combo.getValue().versao(), "na versão atual do catálogo, para recalcular");
        assertFalse(TelaDeTeste.<Label>achar(cena, "resumoInjecao").getText().isEmpty());
        salvar(cena, "captura-projeto-peca-desatualizada.png");
    }
}
