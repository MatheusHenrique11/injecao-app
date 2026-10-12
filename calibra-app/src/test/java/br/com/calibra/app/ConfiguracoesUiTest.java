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
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.config.Chave;

/**
 * Teste de interface (opcional, fora do verify padrão): abre a janela principal com uma pasta de dados
 * temporária, salva os dados da oficina e os padrões em Configurações, confere que a calculadora passou a
 * usar o k_ar novo e faz um backup pela tela. Salva imagens da cena em target/.
 */
@Tag("ui")
class ConfiguracoesUiTest {

    @TempDir
    Path pasta;

    private ContextoApp contexto;
    private PrincipalController principal;
    private Scene cena;

    @Test
    void salvaConfiguracoesEFazBackupPelaTela() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(this::salvarOficina);
            TelaDeTeste.naTelaDoJavaFx(this::salvarPadroesEConferirACalculadora);
            TelaDeTeste.naTelaDoJavaFx(this::fazerBackupAgora);
            TelaDeTeste.esperar(() -> TelaDeTeste.<Label>achar(cena, "backupUltimo").getText().contains("feito por você"),
                    "a aba Backup mostrar o backup feito");
            TelaDeTeste.naTelaDoJavaFx(this::conferirBackup);
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    private void abrir() throws Exception {
        contexto = Partida.abrir(new PastasDados(pasta), "0.2.0-teste", Clock.systemDefaultZone());
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("principal.fxml"));
        Parent raiz = carregador.load();
        principal = carregador.getController();
        principal.iniciar(contexto, null, new Stage());
        cena = new Scene(raiz, 1380, 860);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        TelaDeTeste.<ToggleButton>achar(cena, "navCalculadora").fire();
        assertEquals("10,5", TelaDeTeste.<TextField>achar(cena, "kAr").getText(), "começa com o padrão da SPEC");
        assertTrue(contemTexto(cena.getRoot(), "Último backup: nenhum ainda"));
    }

    /** CNPJ errado é recusado; certo, é salvo. */
    private void salvarOficina() throws Exception {
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
    }

    /** k_ar e limiar do DC salvos; a calculadora recebe o k_ar novo. */
    private void salvarPadroesEConferirACalculadora() throws Exception {
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

    private void fazerBackupAgora() {
        TelaDeTeste.<ToggleButton>achar(cena, "navConfiguracoes").fire();
        TelaDeTeste.<TabPane>achar(cena, "abasConfiguracoes").getSelectionModel().select(2);
        assertTrue(TelaDeTeste.<Label>achar(cena, "backupPasta").getText().endsWith("(pasta padrão)"));
        TelaDeTeste.<Button>achar(cena, "botaoBackupAgora").fire();
    }

    private void conferirBackup() throws Exception {
        assertEquals(1, Backup.listar(contexto.pastas().backupsPadrao()).size());
        assertTrue(contemTexto(cena.getRoot(), "Backup feito:"));
        assertTrue(TelaDeTeste.<Label>achar(cena, "rotuloStatus").getText().startsWith("Último backup: "));
        salvar(cena, "captura-configuracoes-backup.png");
    }
}
