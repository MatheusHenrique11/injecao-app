package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.contemTexto;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.config.Chave;

/**
 * Teste de interface (opcional, fora do verify padrão): abre a janela principal com uma pasta de dados
 * temporária, salva os dados da oficina e os padrões em Configurações e confere que a calculadora passou a
 * usar o k_ar novo. Salva imagens da cena em target/.
 */
@Tag("ui")
class ConfiguracoesUiTest {

    @TempDir
    Path pasta;

    @Test
    void salvaOficinaEPadroesEACalculadoraUsaOsNovos() throws Throwable {
        TelaDeTeste.naTelaDoJavaFx(this::executar);
    }

    private void executar() throws Exception {
        ContextoApp contexto = Partida.abrir(new PastasDados(pasta), "0.2.0-teste", Clock.systemDefaultZone());
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("principal.fxml"));
        Parent raiz = carregador.load();
        PrincipalController principal = carregador.getController();
        principal.iniciar(contexto, null, new Stage());
        Scene cena = new Scene(raiz, 1380, 860);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        assertEquals("10,5", TelaDeTeste.<TextField>achar(cena, "kAr").getText(), "começa com o padrão da SPEC");

        // Oficina: CNPJ errado é recusado; certo, é salvo.
        TelaDeTeste.<ToggleButton>achar(cena, "navConfiguracoes").fire();
        TelaDeTeste.<TextField>achar(cena, "oficinaNome").setText("Oficina de Teste");
        TelaDeTeste.<TextField>achar(cena, "oficinaCnpj").setText("11.222.333/0001-82");
        TelaDeTeste.<TextField>achar(cena, "oficinaTelefone").setText("(11) 98765-4321");
        TelaDeTeste.<Button>achar(cena, "botaoSalvarOficina").fire();
        assertTrue(contemTexto(cena.getRoot(), "CNPJ da oficina inválido"));
        assertEquals("", contexto.configuracao().ler(Chave.OFICINA).nome(), "nada foi salvo");
        TelaDeTeste.<TextField>achar(cena, "oficinaCnpj").setText("11.222.333/0001-81");
        TelaDeTeste.<Button>achar(cena, "botaoSalvarOficina").fire();
        assertEquals("Oficina de Teste", contexto.configuracao().ler(Chave.OFICINA).nome());
        salvar(cena, "captura-configuracoes-oficina.png");

        // Padrões: k_ar e limiar do DC salvos; a calculadora recebe o k_ar novo.
        TelaDeTeste.<TabPane>achar(cena, "abasConfiguracoes").getSelectionModel().select(1);
        TelaDeTeste.<TextField>achar(cena, "padraoKAr").setText("9,5");
        TelaDeTeste.<TextField>achar(cena, "limiarDcVerde").setText("78");
        TelaDeTeste.<Button>achar(cena, "botaoSalvarPadroes").fire();
        Parametros salvos = contexto.configuracao().ler(Chave.CALCULO);
        assertEquals(9.5, salvos.kAr(), 1e-12);
        assertEquals(0.78, salvos.limiares().dcVerdeAte(), 1e-12);
        assertTrue(contemTexto(cena.getRoot(), "2 valor(es) diferente(s) da SPEC"));
        salvar(cena, "captura-configuracoes-calculo.png");

        TelaDeTeste.<ToggleButton>achar(cena, "navCalculadora").fire();
        assertEquals("9,5", TelaDeTeste.<TextField>achar(cena, "kAr").getText());
        salvar(cena, "captura-principal-calculadora.png");
    }
}
