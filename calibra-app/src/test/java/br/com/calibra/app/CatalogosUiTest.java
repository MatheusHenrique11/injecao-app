package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.contemTexto;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.catalogo.FormularioCatalogo;
import br.com.calibra.app.catalogo.FormularioCatalogo.LinhaHistorico;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.motor.ModeloMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.catalogo.ItemCatalogo;

/**
 * Teste de interface do bloco 2.3 (opcional, fora do verify padrão): cadastra um bico com a tabela de tempo morto,
 * edita (v2), compara as versões, abre um combustível e arquiva o bico. Salva imagens da cena em target/. Dados
 * fictícios.
 */
@Tag("ui")
class CatalogosUiTest {

    @TempDir
    Path pasta;

    private ContextoApp contexto;
    private PrincipalController principal;
    private Scene cena;
    private CasosCatalogo casos;

    @Test
    void cadastroEdicaoVersoesEArquivo() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(this::cadastrarBico);
            TelaDeTeste.naTelaDoJavaFx(this::editarEVerVersoes);
            TelaDeTeste.naTelaDoJavaFx(this::combustivelEMotores);
            TelaDeTeste.naTelaDoJavaFx(this::arquivarBico);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    private void abrir() throws Exception {
        contexto = Partida.abrir(new PastasDados(pasta), "0.2.0-teste", Clock.systemDefaultZone());
        casos = new CasosCatalogo(contexto.banco(), contexto.relogio());
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("principal.fxml"));
        Parent raiz = carregador.load();
        principal = carregador.getController();
        principal.iniciar(contexto, null, new Stage());
        cena = new Scene(raiz, 1380, 860);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        TelaDeTeste.<ToggleButton>achar(cena, "navCatalogos").fire();
        assertTrue(bicos().tabela().getItems().isEmpty(), "bicos começam vazios (regra 2.7)");
        assertTrue(contemTexto(cena.getRoot(), "Nada cadastrado em Bicos ainda"), "texto da lista vazia");
    }

    private void cadastrarBico() throws Exception {
        JanelaItemCatalogo<Bico> vazia = bicos().janelaNovo();
        assertFalse(vazia.gravar(), "vazio não grava");
        assertTrue(vazia.erros().contains("Preencha \"Nome\"."), vazia.erros()::toString);
        assertTrue(vazia.erros().contains("Preencha \"Fonte\"."), vazia.erros()::toString);

        JanelaItemCatalogo<Bico> janela = bicos().janelaNovo();
        FormularioTela tela = janela.tela();
        texto(tela, FormularioCatalogo.NOME).setText("Bico teste 440 (fictício)");
        texto(tela, "vazao").setText("440");
        texto(tela, "pressaoTeste").setText("3");
        assertEquals("0,7", texto(tela, "densidadeTeste").getText(), "fluido de teste padrão já preenchido");
        tela.tabela("tempoMorto").preencher(List.of(List.of("12", "3", "1,10"), List.of("14", "3", "0,90"),
                List.of("12", "4", "1,20"), List.of("14", "4", "1,00")));
        texto(tela, FormularioCatalogo.FONTE).setText("Ficha fictícia de teste");
        texto(tela, FormularioCatalogo.PRECO).setText("450,00");
        fotografar(janela.dialogo(), "captura-catalogo-bico-cadastro.png");
        assertTrue(janela.gravar(), janela.erros()::toString);
        assertEquals("Cadastrado: Bico teste 440 (fictício) (v1).", janela.mensagem());

        bicos().atualizar();
        ItemCatalogo<Bico> bico = bicos().tabela().getItems().getFirst();
        assertEquals(4, bico.dados().tempoMorto().pontos().size());
        assertEquals(300.0, bico.dados().pressaoTesteKpa(), 1e-9, "3 bar na tela = 300 kPa no núcleo");
    }

    private void editarEVerVersoes() throws Exception {
        ItemCatalogo<Bico> bico = bicos().tabela().getItems().getFirst();
        JanelaItemCatalogo<Bico> semMudar = bicos().janelaEditar(bico);
        assertTrue(semMudar.gravar());
        assertEquals("Nada mudou em Bico teste 440 (fictício): nenhuma versão nova.", semMudar.mensagem());

        JanelaItemCatalogo<Bico> janela = bicos().janelaEditar(bico);
        texto(janela.tela(), "vazao").setText("450");
        assertTrue(janela.gravar(), janela.erros()::toString);
        assertEquals("Gravado: Bico teste 440 (fictício) (v2).", janela.mensagem());

        bicos().atualizar();
        Dialog<ButtonType> versoes = bicos().janelaVersoes(bicos().tabela().getItems().getFirst());
        Scene cenaVersoes = versoes.getDialogPane().getScene();
        TableView<LinhaHistorico> comparacao = achar(cenaVersoes, "versoes");
        LinhaHistorico vazao = comparacao.getItems().stream()
                .filter(l -> l.campo().equals("Vazão nominal (cm³/min)")).findFirst().orElseThrow();
        assertEquals(List.of("450", "440"), vazao.valores());
        assertEquals(List.of(true, false), vazao.mudou());
        fotografar(versoes, "captura-catalogo-versoes.png");
        salvar(cena, "captura-catalogo-bicos.png");
    }

    private void combustivelEMotores() throws Exception {
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaCOMBUSTIVEL"));
        PainelCatalogo<Combustivel> combustiveis = achar(cena, "painelCOMBUSTIVEL");
        assertEquals(6, combustiveis.tabela().getItems().size(), "os 6 da SPEC 7.2");
        ItemCatalogo<Combustivel> e32 = combustiveis.tabela().getItems().stream()
                .filter(i -> i.dados().codigo().equals("E32")).findFirst().orElseThrow();
        JanelaItemCatalogo<Combustivel> janela = combustiveis.janelaEditar(e32);
        assertTrue(janela.tela().controle("codigo").isDisabled(), "o código não muda depois de criado");
        assertTrue(janela.tela().controle("etanolVolumePct").getParent().isVisible(), "gasolina: % de etanol");
        assertFalse(janela.tela().controle("afr").getParent().isVisible(), "gasolina: sem AFR fixo");
        fotografar(janela.dialogo(), "captura-catalogo-combustivel.png");

        abas.getSelectionModel().select(aba(abas, "abaMOTOR"));
        PainelCatalogo<ModeloMotor> motores = achar(cena, "painelMOTOR");
        assertEquals(25, motores.tabela().getItems().size(), "19 motores e 6 montagens");
        salvar(cena, "captura-catalogo-motores.png");
    }

    private void arquivarBico() throws Exception {
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaBICO"));
        long id = bicos().tabela().getItems().getFirst().id();
        casos.arquivar(id, true);
        bicos().atualizar();
        assertTrue(bicos().tabela().getItems().isEmpty(), "arquivado sai da lista");
        TelaDeTeste.<CheckBox>achar(cena, "arquivadosBICO").setSelected(true);
        assertEquals(1, bicos().tabela().getItems().size());
        assertTrue(bicos().tabela().getItems().getFirst().arquivado());
        salvar(cena, "captura-catalogo-arquivado.png");
    }

    /** Abre a janela (sem esperar), salva a imagem já no tamanho dela e fecha. */
    private static void fotografar(Dialog<?> dialogo, String arquivo) throws Exception {
        dialogo.show();
        try {
            salvar(dialogo.getDialogPane().getScene(), arquivo);
        } finally {
            dialogo.close();
        }
    }

    private PainelCatalogo<Bico> bicos() {
        return achar(cena, "painelBICO");
    }

    private static TextField texto(FormularioTela tela, String chave) {
        return (TextField) tela.controle(chave);
    }

    private static Tab aba(TabPane abas, String id) {
        return abas.getTabs().stream().filter(a -> id.equals(a.getId())).findFirst().orElseThrow();
    }
}
