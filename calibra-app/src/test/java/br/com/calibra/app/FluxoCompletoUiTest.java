package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.apertar;
import static br.com.calibra.app.TelaDeTeste.apertarOk;
import static br.com.calibra.app.TelaDeTeste.contemTexto;
import static br.com.calibra.app.TelaDeTeste.janelaAberta;
import static br.com.calibra.app.TelaDeTeste.painel;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static br.com.calibra.app.TelaDeTeste.textoInteiro;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosProjeto;
import br.com.calibra.app.casos.FormularioOrcamento;
import br.com.calibra.app.casos.FormularioOrcamento.TipoDesconto;
import br.com.calibra.app.catalogo.FormularioCatalogo;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.StatusOrcamento;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.backup.InfoBackup;
import br.com.calibra.dados.backup.MotivoBackup;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.config.Chave;
import br.com.calibra.dados.projeto.FiltroProjetos;
import br.com.calibra.dados.projeto.ResumoProjeto;

/**
 * Ensaio do bloco 2.6 (teste de interface, opcional, fora do verify padrão): a fase 2 pelas telas, numa pasta de
 * dados vazia. Configura a oficina, cadastra um bico no catálogo, cria o projeto pelo assistente, calcula (medindo
 * cada recálculo), monta o orçamento, envia e aprova pela janela de resposta. Depois faz um backup pela aba Backup,
 * muda os dados e restaura pela mesma aba, respondendo às janelas da restauração. Roda também no Java embutido do
 * MSI instalado (docs/fase2/ensaio-instalador.md). Dados fictícios. Salva imagens, o PDF, o texto do WhatsApp e um
 * resumo em target/.
 */
@Tag("ui")
class FluxoCompletoUiTest {

    private static final String BICO = "Bico ensaio 1050 (fictício)";

    @TempDir
    Path pasta;

    private ContextoApp contexto;
    private CasosProjeto casos;
    private PrincipalController principal;
    private Stage palco;
    private Scene cena;
    private long projeto;
    private Path backupFeito;
    private final List<Long> recalculosMs = new ArrayList<>();
    private final List<String> resumo = new ArrayList<>();
    /** Erro de uma ação que abriu janela e não foi esperada (ver semEsperar). */
    private final AtomicReference<Throwable> falhaSemEsperar = new AtomicReference<>();

    @Test
    void doPrimeiroUsoAoOrcamentoAprovadoComBackupERestauracao() throws Throwable {
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(this::configurarOficina);
            TelaDeTeste.naTelaDoJavaFx(this::cadastrarBico);
            TelaDeTeste.naTelaDoJavaFx(this::criarProjeto);
            calcularESalvar();
            TelaDeTeste.naTelaDoJavaFx(this::montarEEnviarOrcamento);
            aprovarPelaJanela();
            TelaDeTeste.naTelaDoJavaFx(this::fazerBackup);
            TelaDeTeste.esperar(() -> contemTexto(cena.getRoot(), "Backup feito:"), "o backup terminar");
            TelaDeTeste.naTelaDoJavaFx(this::mudarOsDados);
            restaurarPelaJanela();
            TelaDeTeste.naTelaDoJavaFx(this::conferirDadosRestaurados);
            assertNull(falhaSemEsperar.get(), () -> "erro numa janela: " + falhaSemEsperar.get());
        } finally {
            gravarResumo();
            Platform.runLater(() -> {
                if (palco != null) {
                    palco.hide();
                }
            });
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /** A janela principal de verdade (mostrada), como o app abre; as janelas de diálogo ficam presas a ela. */
    private void abrir() throws Exception {
        contexto = Partida.abrir(new PastasDados(pasta.resolve("dados")), Versao.atual(), Clock.systemDefaultZone());
        casos = new CasosProjeto(contexto.banco(), contexto.relogio(), contexto.versaoApp());
        FXMLLoader carregador = new FXMLLoader(CalibraApp.class.getResource("principal.fxml"));
        Parent raiz = carregador.load();
        principal = carregador.getController();
        palco = new Stage();
        principal.iniciar(contexto, null, palco);
        cena = new Scene(raiz, 1380, 900);
        cena.getStylesheets().add(CalibraApp.folhaDeEstilo());
        palco.setScene(cena);
        palco.getIcons().setAll(CalibraApp.icones());
        palco.show();
        resumo.add("Java: " + System.getProperty("java.home") + " · " + Runtime.version());
        resumo.add("Calibra " + Versao.atual() + " lido de " + CalibraApp.class.getProtectionDomain().getCodeSource()
                .getLocation());
        assertTrue(TelaDeTeste.<ToggleButton>achar(cena, "navPainel").isSelected(), "abre no Painel");
        assertTrue(contemTexto(cena.getRoot(), "Nenhum veículo na oficina agora."), "começa sem OS");
    }

    /** Primeiro uso: os dados da oficina e o valor da hora do orçamento, em Configurações. */
    private void configurarOficina() throws Exception {
        TelaDeTeste.<ToggleButton>achar(cena, "navConfiguracoes").fire();
        TelaDeTeste.<TextField>achar(cena, "oficinaNome").setText("Oficina Ensaio (fictícia)");
        TelaDeTeste.<TextField>achar(cena, "oficinaCnpj").setText("11.222.333/0001-81");
        TelaDeTeste.<TextField>achar(cena, "oficinaTelefone").setText("(11) 99999-0000");
        TelaDeTeste.<Button>achar(cena, "botaoSalvarOficina").fire();
        assertEquals("Oficina Ensaio (fictícia)", contexto.configuracao().ler(Chave.OFICINA).nome());

        TabPane abas = achar(cena, "abasConfiguracoes");
        abas.getSelectionModel().select(aba(abas, "abaOrcamento"));
        TelaDeTeste.<TextField>achar(cena, "orcamentoValorHora").setText("180,00");
        TelaDeTeste.<TextInputControl>achar(cena, "orcamentoCondicoes").setText("50% na aprovação e 50% na entrega");
        TelaDeTeste.<Button>achar(cena, "botaoSalvarOrcamento").fire();
        assertEquals(Dinheiro.ler("180,00"), contexto.configuracao().ler(Chave.ORCAMENTO).valorHora());
    }

    /** Catálogo: o bico com a tabela de tempo morto e o preço que o orçamento vai usar. */
    private void cadastrarBico() throws Exception {
        TelaDeTeste.<ToggleButton>achar(cena, "navCatalogos").fire();
        PainelCatalogo<Bico> bicos = achar(cena, "painelBICO");
        JanelaItemCatalogo<Bico> janela = bicos.janelaNovo();
        FormularioTela tela = janela.tela();
        texto(tela, FormularioCatalogo.NOME).setText(BICO);
        texto(tela, "vazao").setText("1050");
        texto(tela, "pressaoTeste").setText("3");
        tela.tabela("tempoMorto").preencher(List.of(List.of("12", "3", "1,10"), List.of("14", "3", "0,90"),
                List.of("12", "4", "1,20"), List.of("14", "4", "1,00")));
        texto(tela, FormularioCatalogo.FONTE).setText("Ficha fictícia do ensaio");
        texto(tela, FormularioCatalogo.PRECO).setText("450,00");
        assertTrue(janela.gravar(), janela.erros()::toString);
        bicos.atualizar();
        assertEquals(1, bicos.tabela().getItems().size());
    }

    /** O assistente: cliente com consentimento, veículo, motor (caso golden B) e meta. */
    private void criarProjeto() throws Exception {
        TelaDeTeste.<ToggleButton>achar(cena, "navPreparacao").fire();
        TelaDeTeste.<Button>achar(cena, "botaoNovo").fire();
        TelaDeTeste.<TextField>achar(cena, "nome").setText("Cliente Ensaio (fictício)");
        TelaDeTeste.<TextField>achar(cena, "telefone").setText("(11) 98765-4321");
        TelaDeTeste.<CheckBox>achar(cena, "consentiu").setSelected(true);
        proximo();
        TelaDeTeste.<TextField>achar(cena, "marca").setText("VW");
        TelaDeTeste.<TextField>achar(cena, "modelo").setText("Gol");
        TelaDeTeste.<TextField>achar(cena, "versao").setText("GTi");
        TelaDeTeste.<TextField>achar(cena, "anoFabricacao").setText("1994");
        TelaDeTeste.<TextField>achar(cena, "anoModelo").setText("1994");
        TelaDeTeste.<TextField>achar(cena, "placa").setText("ens-1a23");
        proximo();
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
        TelaDeTeste.<TextField>achar(cena, "potencia").setText("400");
        TelaDeTeste.<TextField>achar(cena, "rpmPotencia").setText("7.000");
        TelaDeTeste.<ComboBox<Uso>>achar(cena, "uso").setValue(Uso.RUA);
        proximo();
        TelaDeTeste.<Button>achar(cena, "botaoCriar").fire();

        List<ResumoProjeto> projetos = casos.pesquisar(FiltroProjetos.todos());
        assertEquals(1, projetos.size(), () -> "o assistente não criou o projeto: " + cena.lookup("#mensagens"));
        projeto = projetos.getFirst().projetoId();
        assertEquals("Rascunho", TelaDeTeste.<Label>achar(cena, "status").getText());
        salvar(cena, "ensaio-1-projeto-criado.png");
    }

    /**
     * Dimensionamento com o bico do catálogo; cada "Calcular" é cronometrado do clique até a tela montada (bloco 2.6:
     * menos de 1 s). O cálculo roda em segundo plano (decisão 290): o relógio para quando o resultado chega à tela,
     * mais o tempo de montar a cena, sem contar a espera do teste.
     */
    private void calcularESalvar() throws Throwable {
        AtomicLong chegou = new AtomicLong();
        TelaDeTeste.naTelaDoJavaFx(() -> {
            TabPane abas = achar(cena, "abas");
            abas.getSelectionModel().select(aba(abas, "abaDimensionamento"));
            ComboBox<ItemCatalogo<Bico>> bico = achar(cena, "catalogoBico");
            bico.setValue(bico.getItems().stream().filter(item -> item != null && BICO.equals(item.info().nome()))
                    .findFirst().orElseThrow());
            assertTrue(TelaDeTeste.<TextField>achar(cena, "bicoVazao").isDisabled(),
                    "o bico do catálogo trava os campos");
            TelaDeTeste.<TextField>achar(cena, "pressaoBase").setText("3");
            TelaDeTeste.<TextField>achar(cena, "tensao").setText("13,5");
            calculadora().aoMudarResultado(() -> chegou.set(System.nanoTime()));
        });
        for (String valor : List.of("0,95", "0,93", "0,97", "0,90", "0,95")) {
            AtomicLong inicio = new AtomicLong();
            TelaDeTeste.naTelaDoJavaFx(() -> {
                TelaDeTeste.<TextField>achar(cena, "ve").setText(valor);
                inicio.set(System.nanoTime());
                TelaDeTeste.<Button>achar(cena, "botaoCalcular").fire();
            });
            TelaDeTeste.esperarCalculo(calculadora());
            TelaDeTeste.naTelaDoJavaFx(() -> {
                assertTrue(chegou.get() > inicio.get(), "o resultado chegou à tela");
                long antesDeMontar = System.nanoTime();
                cena.getRoot().applyCss();
                cena.getRoot().layout();
                long montar = System.nanoTime() - antesDeMontar;
                recalculosMs.add(TimeUnit.NANOSECONDS.toMillis(chegou.get() - inicio.get() + montar));
            });
        }
        resumo.add("Recálculos (ms, do clique em Calcular até a tela montada): " + recalculosMs);
        assertTrue(recalculosMs.stream().allMatch(ms -> ms < 1000), () -> "recalcular passou de 1 s: " + recalculosMs);

        TelaDeTeste.naTelaDoJavaFx(() -> {
            Button salvarCalculo = achar(cena, "botaoSalvarCalculo");
            assertFalse(salvarCalculo.isDisabled(), "calculado: dá para salvar");
            cena.getRoot().lookupAll(".text-field").stream()
                    .filter(no -> no.getId() != null && no.getId().startsWith("justificativa-"))
                    .forEach(no -> ((TextField) no).setText("Bico maior encomendado; este é provisório."));
            salvarCalculo.fire();
            assertEquals(StatusProjeto.DIMENSIONADO, casos.abrir(projeto).projeto().status());
            assertEquals("Dimensionado", TelaDeTeste.<Label>achar(cena, "status").getText());
            salvar(cena, "ensaio-2-calculo-salvo.png");
        });
    }

    /**
     * Peças e orçamento: o bico do cálculo com o preço do catálogo, a mão de obra pelo valor da hora e 10% de
     * desconto. Grava, gera o PDF e o texto do WhatsApp e marca como enviado.
     */
    private void montarEEnviarOrcamento() throws Exception {
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaOrcamento"));
        OrcamentoController orcamento = orcamento();
        FormularioOrcamento.Linha bico = orcamento.linhas().getFirst();
        assertEquals(List.of(BICO, "4", "450,00"), List.of(bico.descricao(), bico.quantidade(), bico.valor()));
        this.<Button>daAba("botaoAdicionarServico").fire();
        assertEquals("180,00", orcamento.linhas().get(1).valor(), "o valor da hora de Configurações");
        orcamento.preencherLinha(1, "Mão de obra de instalação e acerto de marcha lenta", "2,5", "h", "180,00");
        this.<ComboBox<TipoDesconto>>daAba("tipoDesconto").setValue(TipoDesconto.PERCENTUAL);
        this.<TextField>daAba("desconto").setText("10");
        assertEquals("R$ 2.025,00", this.<Label>daAba("total").getText(), "(4 × 450 + 2,5 × 180) − 10%");
        this.<Button>daAba("botaoSalvar").fire();
        String numero = "ORC-" + LocalDate.now().getYear() + "-0001";
        assertEquals("Orçamento " + numero, TelaDeTeste.<Label>achar(cena, "numero").getText(),
                orcamento.mensagens()::toString);

        Path pdf = alvo().resolve("ensaio-orcamento.pdf");
        orcamento.escreverPdf(pdf);
        try (PDDocument documento = Loader.loadPDF(pdf.toFile())) {
            String texto = new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
            assertTrue(texto.contains(numero) && texto.contains("2.025,00"), texto);
        }
        this.<Button>daAba("botaoWhatsApp").fire();
        String whatsapp = Clipboard.getSystemClipboard().getString();
        assertTrue(whatsapp.contains("*Total: R$ 2.025,00*"), whatsapp);
        Files.writeString(alvo().resolve("ensaio-orcamento-whatsapp.txt"), whatsapp);

        this.<Button>daAba("botaoEnviado").fire();
        assertEquals(StatusOrcamento.ENVIADO, orcamento.atual().orElseThrow().orcamento().status(),
                orcamento.mensagens()::toString);
        assertEquals("Orçado", TelaDeTeste.<Label>achar(cena, "status").getText());
        resumo.add("Orçamento " + numero + " · total R$ 2.025,00 · PDF de " + Files.size(pdf) + " bytes");
    }

    /** "Aprovado pelo cliente…" abre a janela de resposta: escolhe WhatsApp, escreve a observação e grava. */
    @SuppressWarnings("unchecked")
    private void aprovarPelaJanela() throws Throwable {
        semEsperar(() -> this.<Button>daAba("botaoAprovado").fire());
        Stage janela = esperarJanela("Aprovado pelo cliente");
        TelaDeTeste.naTelaDoJavaFx(() -> {
            DialogPane painel = painel(janela);
            ((ComboBox<MeioAprovacao>) painel.lookup(".combo-box")).setValue(MeioAprovacao.WHATSAPP);
            painel.lookupAll(".text-field").stream().map(TextField.class::cast)
                    .filter(campo -> campo.getPromptText() != null && campo.getPromptText().startsWith("opcional"))
                    .findFirst().orElseThrow().setText("Aprovou por mensagem.");
            salvar(janela.getScene(), "ensaio-3-janela-aprovado.png");
            apertar(painel, "Gravar");
        });
        TelaDeTeste.esperar(() -> orcamento().atual()
                .filter(situacao -> situacao.orcamento().status() == StatusOrcamento.APROVADO).isPresent(),
                "o orçamento ficar aprovado");
        TelaDeTeste.naTelaDoJavaFx(() -> {
            assertEquals("Aprovado", TelaDeTeste.<Label>achar(cena, "status").getText(), "o projeto passou para Aprovado");
            salvar(cena, "ensaio-4-orcamento-aprovado.png");
        });
        resumo.add("Aprovado pela janela de resposta (WhatsApp); projeto em Aprovado");
    }

    private void fazerBackup() {
        TelaDeTeste.<ToggleButton>achar(cena, "navConfiguracoes").fire();
        TabPane abas = achar(cena, "abasConfiguracoes");
        abas.getSelectionModel().select(aba(abas, "abaBackup"));
        TelaDeTeste.<Button>achar(cena, "botaoBackupAgora").fire();
    }

    /** Depois do backup: uma revisão nova do orçamento e a cor do veículo. A restauração tem de desfazer as duas. */
    private void mudarOsDados() throws Exception {
        backupFeito = Backup.listar(contexto.pastas().backupsPadrao()).stream()
                .filter(info -> info.manifesto().motivo() == MotivoBackup.MANUAL).findFirst().orElseThrow().arquivo();
        resumo.add("Backup feito pela tela: " + backupFeito.getFileName());
        salvar(cena, "ensaio-5-backup-feito.png");

        TelaDeTeste.<ToggleButton>achar(cena, "navPreparacao").fire();
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaOrcamento"));
        this.<Button>daAba("botaoNovaRevisao").fire();
        assertEquals(2, this.<TableView<?>>daAba("revisoes").getItems().size());
        abas.getSelectionModel().select(aba(abas, "abaVeiculo"));
        TelaDeTeste.<TextField>achar(cena, "cor").setText("Vermelho");
        TelaDeTeste.<Button>achar(cena, "botaoSalvarVeiculo").fire();
        assertEquals("Vermelho", casos.abrir(projeto).veiculo().veiculo().cor());
        resumo.add("Depois do backup: revisão 2 do orçamento aberta e cor do veículo = Vermelho");
    }

    /** Configurações › Backup › Restaurar: escolhe o backup feito, confirma e lê o aviso do fim. */
    @SuppressWarnings("unchecked")
    private void restaurarPelaJanela() throws Throwable {
        TelaDeTeste.naTelaDoJavaFx(() -> {
            TelaDeTeste.<ToggleButton>achar(cena, "navConfiguracoes").fire();
            TabPane abas = achar(cena, "abasConfiguracoes");
            abas.getSelectionModel().select(aba(abas, "abaBackup"));
        });
        semEsperar(() -> TelaDeTeste.<Button>achar(cena, "botaoRestaurar").fire());

        Stage escolha = esperarJanela("Restaurar um backup");
        TelaDeTeste.naTelaDoJavaFx(() -> {
            DialogPane painel = painel(escolha);
            ListView<InfoBackup> lista = (ListView<InfoBackup>) painel.lookup(".list-view");
            lista.getSelectionModel().select(lista.getItems().stream()
                    .filter(info -> info.arquivo().equals(backupFeito)).findFirst().orElseThrow());
            salvar(escolha.getScene(), "ensaio-6-escolher-backup.png");
            apertar(painel, "Restaurar o escolhido…");
        });

        Stage confirmacao = esperarJanela("Restaurar backup");
        TelaDeTeste.naTelaDoJavaFx(() -> {
            salvar(confirmacao.getScene(), "ensaio-7-confirmar-restauracao.png");
            textoInteiro(painel(confirmacao));
            apertar(painel(confirmacao), "Restaurar");
        });

        Stage aviso = esperarJanela("Backup restaurado");
        TelaDeTeste.naTelaDoJavaFx(() -> {
            DialogPane painel = painel(aviso);
            resumo.add("Aviso da restauração: " + painel.getContentText().replaceAll("\\s+", " "));
            salvar(aviso.getScene(), "ensaio-8-backup-restaurado.png");
            textoInteiro(painel);
            apertarOk(painel);
        });
        TelaDeTeste.esperar(() -> janelaAberta("Backup restaurado") == null, "o aviso fechar");
    }

    /** Os dados voltaram ao ponto do backup: uma revisão só (aprovada) e o veículo sem cor. */
    private void conferirDadosRestaurados() throws Exception {
        InfoBackup seguranca = Backup.listar(contexto.pastas().backupsPadrao()).stream()
                .filter(info -> info.manifesto().motivo() == MotivoBackup.ANTES_DE_RESTAURAR).findFirst().orElseThrow();
        resumo.add("Backup de segurança (os dados de antes da restauração): " + seguranca.arquivo().getFileName());
        salvar(cena, "ensaio-9-aba-backup-depois.png");
        assertEquals(1, casos.pesquisar(FiltroProjetos.todos()).size());
        assertEquals("", Objects.toString(casos.abrir(projeto).veiculo().veiculo().cor(), ""),
                "a cor gravada depois do backup sumiu");

        TelaDeTeste.<ToggleButton>achar(cena, "navPreparacao").fire();
        assertEquals(1, TelaDeTeste.<TableView<?>>achar(cena, "tabela").getItems().size());
        principal.abrirProjeto(projeto);
        assertEquals("Aprovado", TelaDeTeste.<Label>achar(cena, "status").getText());
        TabPane abas = achar(cena, "abas");
        abas.getSelectionModel().select(aba(abas, "abaOrcamento"));
        assertEquals(1, this.<TableView<?>>daAba("revisoes").getItems().size(), "a revisão feita depois do backup sumiu");
        assertEquals(StatusOrcamento.APROVADO, orcamento().atual().orElseThrow().orcamento().status());
        salvar(cena, "ensaio-10-orcamento-restaurado.png");
        abas.getSelectionModel().select(aba(abas, "abaVeiculo"));
        assertEquals("", Objects.toString(TelaDeTeste.<TextField>achar(cena, "cor").getText(), ""));
        resumo.add("Depois de restaurar: 1 projeto, orçamento aprovado sem a revisão 2, veículo sem cor (como no backup)");
    }

    // ===================== Utilidades =====================

    private CalculadoraController calculadora() {
        return principal.projetoAberto().calculadora();
    }

    private OrcamentoController orcamento() {
        return principal.projetoAberto().orcamento();
    }

    @SuppressWarnings("unchecked")
    private <T extends Node> T daAba(String id) {
        TabPane abas = achar(cena, "abas");
        Node conteudo = abas.getSelectionModel().getSelectedItem().getContent();
        conteudo.applyCss();
        Node no = conteudo.lookup("#" + id);
        assertNotNull(no, "não achei #" + id + " na aba");
        return (T) no;
    }

    private void proximo() {
        TelaDeTeste.<Button>achar(cena, "botaoProximo").fire();
        VBox mensagens = achar(cena, "mensagens");
        assertTrue(mensagens.getChildren().isEmpty(), () -> "erros no assistente: " + mensagens.getChildren());
    }

    private static Tab aba(TabPane abas, String id) {
        return abas.getTabs().stream().filter(a -> id.equals(a.getId())).findFirst().orElseThrow();
    }

    private static TextField texto(FormularioTela tela, String chave) {
        return (TextField) tela.controle(chave);
    }

    /**
     * Roda na thread do JavaFX uma ação que abre janela e espera a resposta (showAndWait), sem esperar por ela:
     * o teste segue, acha a janela e aperta o botão, como a pessoa faria.
     */
    private void semEsperar(Runnable acao) {
        TelaDeTeste.semEsperar(acao, falhaSemEsperar);
    }

    private Stage esperarJanela(String titulo) throws Throwable {
        return TelaDeTeste.esperarJanela(titulo, falhaSemEsperar);
    }

    private static Path alvo() throws IOException {
        Path pasta = Path.of(System.getProperty("basedir", "")).toAbsolutePath().resolve("target");
        Files.createDirectories(pasta);
        return pasta;
    }

    private void gravarResumo() {
        try {
            Files.write(alvo().resolve("ensaio-fluxo-completo.txt"), resumo);
        } catch (IOException erro) {
            System.err.println("Não gravei o resumo do ensaio: " + erro);
        }
        resumo.forEach(System.out::println);
    }
}
