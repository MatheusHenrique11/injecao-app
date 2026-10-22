package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.contemTexto;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.cadastro.Cliente;
import br.com.calibra.core.cadastro.Consentimento;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.ComparacaoMotor;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.cadastro.EventoAuditoria;
import br.com.calibra.dados.projeto.FiltroProjetos;

/**
 * Teste de interface do bloco 2.2 (opcional, fora do verify padrão): cria um projeto pelo assistente, edita o
 * veículo, salva um cálculo, grava a v2 do motor e confere o "desatualizado — recalcular". Salva imagens da
 * cena em target/. Dados fictícios.
 */
@Tag("ui")
class ProjetoUiTest {

    @TempDir
    Path pasta;

    private ContextoApp contexto;
    private PrincipalController principal;
    private Scene cena;
    private CasosProjeto casos;

    @Test
    void doAssistenteAoCalculoDesatualizado() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(this::assistente);
            TelaDeTeste.naTelaDoJavaFx(this::editarVeiculo);
            TelaDeTeste.naTelaDoJavaFx(this::calcular);
            TelaDeTeste.esperarCalculo(principal.projetoAberto().calculadora());
            TelaDeTeste.naTelaDoJavaFx(this::salvarCalculo);
            TelaDeTeste.naTelaDoJavaFx(this::versaoNovaDoMotor);
            TelaDeTeste.naTelaDoJavaFx(this::calculoDesatualizado);
            TelaDeTeste.naTelaDoJavaFx(this::listaComBusca);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    private void abrir() throws Exception {
        contexto = Partida.abrir(new PastasDados(pasta), "0.2.0-teste", Clock.systemDefaultZone());
        casos = new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp());
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("principal.fxml"));
        Parent raiz = carregador.load();
        principal = carregador.getController();
        principal.iniciar(contexto, null, new Stage());
        cena = new Scene(raiz, 1380, 860);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        assertTrue(TelaDeTeste.<ToggleButton>achar(cena, "navPainel").isSelected(), "o app abre no Painel");
        TelaDeTeste.<ToggleButton>achar(cena, "navPreparacao").fire();
        assertTrue(TelaDeTeste.<TableView<?>>achar(cena, "tabela").getItems().isEmpty(), "começa na lista vazia");
        assertTrue(contemTexto(cena.getRoot(), "Nenhum projeto ainda"), "texto da lista vazia");
    }

    private void assistente() throws Exception {
        TelaDeTeste.<Button>achar(cena, "botaoNovo").fire();
        // 1. Cliente novo, com consentimento.
        TelaDeTeste.<TextField>achar(cena, "nome").setText("João da Silva (fictício)");
        TelaDeTeste.<TextField>achar(cena, "telefone").setText("(11) 98765-4321");
        TelaDeTeste.<CheckBox>achar(cena, "consentiu").setSelected(true);
        proximo();
        // 2. Veículo novo.
        TelaDeTeste.<TextField>achar(cena, "marca").setText("VW");
        TelaDeTeste.<TextField>achar(cena, "modelo").setText("Gol");
        TelaDeTeste.<TextField>achar(cena, "versao").setText("GTi");
        TelaDeTeste.<TextField>achar(cena, "anoFabricacao").setText("1994");
        TelaDeTeste.<TextField>achar(cena, "anoModelo").setText("1994");
        TelaDeTeste.<TextField>achar(cena, "placa").setText("abc-1d23");
        proximo();
        // 3. Motor (caso golden B).
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
        proximo();
        // 4. Meta e uso.
        TelaDeTeste.<TextField>achar(cena, "potencia").setText("400");
        TelaDeTeste.<TextField>achar(cena, "rpmPotencia").setText("7.000");
        TelaDeTeste.<ComboBox<Uso>>achar(cena, "uso").setValue(Uso.RUA);
        salvar(cena, "captura-assistente-meta.png");
        proximo();
        // 5. Injeção (opcional): o catálogo de injeções começa vazio, então segue sem.
        assertTrue(TelaDeTeste.<Button>achar(cena, "botaoCriar").isVisible(), "Criar projeto na última etapa");
        assertTrue(TelaDeTeste.<VBox>achar(cena, "painelInjecao").isVisible(), "painel da injeção");
        assertFalse(TelaDeTeste.<VBox>achar(cena, "painelMeta").isVisible(), "a meta saiu da tela");
        TelaDeTeste.<Button>achar(cena, "botaoSugerirInjecao").fire();
        assertTrue(contemTexto(achar(cena, "mensagensInjecao"), "Nenhuma injeção cadastrada no catálogo."));
        salvar(cena, "captura-assistente-injecao.png");
        TelaDeTeste.<Button>achar(cena, "botaoCriar").fire();
        if (estaNoAssistente()) {
            fail("o assistente não criou o projeto: " + ((VBox) cena.lookup("#mensagens")).getChildren());
        }

        assertTrue(TelaDeTeste.<Label>achar(cena, "titulo").getText().startsWith("João da Silva (fictício) · VW Gol"));
        assertEquals("Rascunho", TelaDeTeste.<Label>achar(cena, "status").getText());
        salvar(cena, "captura-projeto-resumo.png");
    }

    private void editarVeiculo() throws Exception {
        abas().getSelectionModel().select(1);
        TelaDeTeste.<TextField>achar(cena, "cor").setText("Vermelho");
        TelaDeTeste.<TextField>achar(cena, "usoAcumulado").setText("251.500");
        TelaDeTeste.<Button>achar(cena, "botaoSalvarVeiculo").fire();
        TableView<EventoAuditoria> historico = achar(cena, "historicoVeiculo");
        List<String> campos = historico.getItems().stream().map(EventoAuditoria::campo).toList();
        assertTrue(campos.containsAll(List.of("Uso acumulado", "Cor", "Cadastro")), campos::toString);
        salvar(cena, "captura-projeto-veiculo.png");
    }

    private void calcular() throws Exception {
        abas().getSelectionModel().select(3);
        TelaDeTeste.<TextField>achar(cena, "ve").setText("0,95");
        TelaDeTeste.<TextField>achar(cena, "pressaoBase").setText("3");
        TelaDeTeste.<TextField>achar(cena, "bicoVazao").setText("1.050");
        TelaDeTeste.<TextField>achar(cena, "bicoPressaoTeste").setText("3");
        TelaDeTeste.<TextField>achar(cena, "bicoTempoMorto").setText("1");
        TelaDeTeste.<Button>achar(cena, "botaoCalcular").fire();
    }

    /** Depois do cálculo em segundo plano (decisão 290; o teste espera ele terminar). */
    private void salvarCalculo() throws Exception {
        Button salvarCalculo = achar(cena, "botaoSalvarCalculo");
        assertFalse(salvarCalculo.isDisabled(), "calculado: dá para salvar");
        TelaDeTeste.<TextField>achar(cena, "justificativa-DC_BICO").setText("Bico de 1.300 cm³/min encomendado.");
        salvarCalculo.fire();

        assertEquals(1, casos.calculos(1).size(), "cálculo salvo no projeto");
        assertEquals("Dimensionado", TelaDeTeste.<Label>achar(cena, "status").getText());
        assertEquals(StatusProjeto.DIMENSIONADO, casos.abrir(1).projeto().status());
        salvar(cena, "captura-projeto-calculo-salvo.png");
    }

    private void versaoNovaDoMotor() throws Exception {
        abas().getSelectionModel().select(2);
        TelaDeTeste.<TextField>achar(cena, "taxa").setText("9,0");
        TelaDeTeste.<Button>achar(cena, "botaoSalvarMotor").fire();
        assertEquals(2, casos.versoesMotor(1).size(), "v2 do motor");
        TableView<ComparacaoMotor.Diferenca> comparacao = achar(cena, "comparacao");
        assertTrue(comparacao.getItems().contains(new ComparacaoMotor.Diferenca("Taxa de compressão (:1)", "9,5", "9")),
                comparacao.getItems()::toString);
        cena.getRoot().applyCss();
        cena.getRoot().layout();
        TelaDeTeste.<ScrollPane>achar(cena, "rolagemMotor").setVvalue(1.0);
        salvar(cena, "captura-projeto-motor-v1-v2.png");
    }

    private void calculoDesatualizado() throws Exception {
        abas().getSelectionModel().select(3);
        assertTrue(casos.calculos(1).getFirst().desatualizado());
        assertTrue(contemTexto(cena.getRoot(), "desatualizado — recalcular"), "faixa do desatualizado");
        salvar(cena, "captura-projeto-calculo-desatualizado.png");
    }

    private void listaComBusca() throws Exception {
        casos.criarProjeto(new NovoProjeto(
                new ClienteNovo(Cliente.novo("Maria Antônia (fictícia)", "(21) 99876-5432", "", "",
                        new Consentimento(LocalDate.now(), Consentimento.FINALIDADE_PADRAO), "")),
                new VeiculoNovo(new Veiculo(TipoVeiculo.LANCHA, "Focker", "240", "", 2019, 2020, "", "", "Maré Alta",
                        "", "", 120.0, TipoCambio.NAUTICO_RABETA_OU_REVERSOR, false, null, null, "", "")),
                "Mercury 4.5 V6", casos.abrir(1).motor().configuracao().paraConstrutor()
                        .aspiracao(Aspiracao.ASPIRADO).construir(),
                casos.abrir(1).projeto().meta()));
        TelaDeTeste.<Button>achar(cena, "botaoVoltarProjetos").fire();
        TableView<?> tabela = achar(cena, "tabela");
        assertEquals(2, tabela.getItems().size());
        salvar(cena, "captura-inicio.png");
        TelaDeTeste.<TextField>achar(cena, "busca").setText("joao");
        assertEquals(1, tabela.getItems().size(), "busca sem acento");
        TelaDeTeste.<TextField>achar(cena, "busca").setText("mare alta");
        assertEquals(1, tabela.getItems().size(), "nome da embarcação");
        TelaDeTeste.<TextField>achar(cena, "busca").setText("");
        assertEquals(2, casos.pesquisar(FiltroProjetos.todos()).size());
    }

    private void proximo() {
        TelaDeTeste.<Button>achar(cena, "botaoProximo").fire();
        VBox mensagens = achar(cena, "mensagens");
        assertTrue(mensagens.getChildren().isEmpty(), () -> "erros no assistente: " + mensagens.getChildren());
    }

    private boolean estaNoAssistente() {
        cena.getRoot().applyCss();
        return cena.lookup("#botaoCriar") != null;
    }

    private TabPane abas() {
        return achar(cena, "abas");
    }
}
