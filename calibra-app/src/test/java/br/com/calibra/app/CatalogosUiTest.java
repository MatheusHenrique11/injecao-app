package br.com.calibra.app;

import static br.com.calibra.app.TelaDeTeste.achar;
import static br.com.calibra.app.TelaDeTeste.contemTexto;
import static br.com.calibra.app.TelaDeTeste.salvar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.catalogo.ExemplosCatalogo;
import br.com.calibra.app.catalogo.FormularioCatalogo;
import br.com.calibra.app.catalogo.FormularioCatalogo.LinhaHistorico;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.motor.ModeloMotor;
import br.com.calibra.core.orcamento.ItemMaterial;
import br.com.calibra.core.orcamento.Kit;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.orcamento.ServicosSugeridos;
import br.com.calibra.core.orcamento.ServicosSugeridos.Sugestao;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Teste de interface do bloco 2.3 (opcional, fora do verify padrão): cadastra um bico com a tabela de tempo morto,
 * edita (v2), compara as versões, abre um combustível e arquiva o bico; e, na Fase 3B, os serviços sugeridos e os
 * campos novos do serviço. Salva imagens da cena em target/. Dados fictícios.
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

    /**
     * Fase 3B (SPEC 6.6.1): o botão "Serviços sugeridos…" abre a lista; a linha marcada sem horas nem preço segura a
     * janela aberta com o erro; com as horas ou o preço, os serviços entram com a categoria, e ao abrir de novo
     * aparecem como "já cadastrado". O formulário do serviço mostra os campos novos.
     */
    @Test
    void servicosSugeridosEOsCamposNovosDoServico() throws Throwable {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                TabPane abas = achar(cena, "abas");
                abas.getSelectionModel().select(aba(abas, "abaSERVICO"));
                Button sugeridos = achar(cena, "servicosSugeridos");
                List<Node> barra = sugeridos.getParent().getChildrenUnmodifiable();
                assertEquals(barra.indexOf(achar(cena, "novoSERVICO")) + 1, barra.indexOf(sugeridos),
                        "ao lado de \"Cadastrar…\"");
                assertTrue(servicos().tabela().getItems().isEmpty());
            });

            TelaDeTeste.semEsperar(() -> TelaDeTeste.<Button>achar(cena, "servicosSugeridos").fire(), falha);
            Stage janela = TelaDeTeste.esperarJanela("Serviços sugeridos", falha);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                DialogPane painel = TelaDeTeste.painel(janela);
                TextField falantes = (TextField) painel.lookup("#sugeridoHoras" + indice("Troca de alto-falantes"));
                falantes.setText("1,5");
                assertTrue(((CheckBox) painel.lookup("#sugeridoMarcar" + indice("Troca de alto-falantes")))
                        .isSelected(), "digitar as horas já marca a linha");
                ((TextField) painel.lookup("#sugeridoHoras" + indice("Reparo ou troca de alternador"))).setText("2");
                ((TextField) painel.lookup("#sugeridoPreco" + indice("Confecção de chicote para injeção programável")))
                        .setText("1.800,00");
                ((CheckBox) painel.lookup("#sugeridoMarcar" + indice("Instalação de som completa"))).setSelected(true);
                TelaDeTeste.apertar(painel, "Adicionar ao catálogo");
                assertTrue(janela.isShowing(), "com erro, a janela fica aberta");
                assertEquals("Instalação de som completa: informe as horas ou o preço.",
                        ((Label) painel.lookup("#erroSugeridos")).getText());
                assertTrue(casos.ativos(TipoCatalogo.SERVICO).isEmpty(), "com erro, nada é gravado");
                salvar(janela.getScene(), "captura-catalogo-servicos-sugeridos-erro.png");

                ((CheckBox) painel.lookup("#sugeridoMarcar" + indice("Instalação de som completa"))).setSelected(false);
                TelaDeTeste.apertar(painel, "Adicionar ao catálogo");
            });
            TelaDeTeste.esperar(() -> !janela.isShowing(), "a janela fechar depois de gravar");
            TelaDeTeste.naTelaDoJavaFx(() -> {
                assertTrue(contemTexto(cena.getRoot(), "3 serviços adicionados ao catálogo."), "a mensagem na aba");
                TableView<ItemCatalogo<ServicoPadrao>> tabela = servicos().tabela();
                assertEquals(List.of("Confecção de chicote para injeção programável", "Reparo ou troca de alternador",
                        "Troca de alto-falantes"), tabela.getItems().stream().map(i -> i.info().nome()).toList());
                assertEquals(List.of("Injeção programável", "Partida e carga", "Som"), coluna(tabela, "Categoria"));
                assertEquals(List.of("—", "2 h", "1,5 h"), coluna(tabela, "Horas"));
                assertEquals(List.of("da OS", "da OS", "da OS"), coluna(tabela, "Garantia"));
                salvar(cena, "captura-catalogo-servicos.png");

                JanelaServicosSugeridos deNovo = new JanelaServicosSugeridos(null, casos);
                assertTrue(deNovo.linha("Troca de alto-falantes").cadastrado());
                assertTrue(deNovo.linha("Troca de alto-falantes").marcar().isDisabled(), "já cadastrado não se marca");
                assertFalse(deNovo.linha("Instalação de som completa").cadastrado());
                fotografar(deNovo.dialogo(), "captura-catalogo-servicos-sugeridos.png");

                ItemCatalogo<ServicoPadrao> chicote = tabela.getItems().getFirst();
                JanelaItemCatalogo<ServicoPadrao> edicao = servicos().janelaEditar(chicote);
                assertFalse(((CheckBox) edicao.tela().controle("exigeEntradaDoVeiculo")).isSelected(),
                        "a confecção de chicote é de bancada");
                texto(edicao.tela(), "descricaoPadrao").setText("Chicote novo para a injeção do cliente");
                texto(edicao.tela(), "garantiaDias").setText("365");
                fotografar(edicao.dialogo(), "captura-catalogo-servico-edicao.png");
                assertTrue(edicao.gravar(), edicao.erros()::toString);
                assertEquals("Gravado: Confecção de chicote para injeção programável (v2).", edicao.mensagem());
                ServicoPadrao gravado = casos.buscar(TipoCatalogo.SERVICO, chicote.id()).orElseThrow().dados();
                assertEquals("Chicote novo para a injeção do cliente", gravado.textoNoOrcamento());
                assertEquals(365, gravado.garantiaDias());
            });
            assertNull(falha.get(), () -> "erro numa janela: " + falha.get());
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    /**
     * Fase 3B (SPEC 6.6.4): cadastrar um item (a unidade já vem "un") e um kit com um item, escolhido pelo botão "+ Item
     * do catálogo…" de dentro do kit, e um serviço. O kit não tem o campo nem a coluna do preço; ao editar, as linhas
     * voltam com os nomes do catálogo.
     */
    @Test
    void itensEKits() throws Throwable {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        AtomicReference<JanelaItemCatalogo<Kit>> janelaKit = new AtomicReference<>();
        AtomicReference<ItemCatalogo<ServicoPadrao>> servico = new AtomicReference<>();
        try {
            TelaDeTeste.naTelaDoJavaFx(this::abrir);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                TabPane abas = achar(cena, "abas");
                abas.getSelectionModel().select(aba(abas, "abaITEM"));
                PainelCatalogo<ItemMaterial> itens = achar(cena, "painelITEM");
                JanelaItemCatalogo<ItemMaterial> janela = itens.janelaNovo();
                assertEquals("un", texto(janela.tela(), "unidade").getText(), "a unidade já vem preenchida");
                texto(janela.tela(), FormularioCatalogo.NOME).setText("Relé 12 V 40 A (fictício)");
                texto(janela.tela(), FormularioCatalogo.PRECO).setText("18,50");
                fotografar(janela.dialogo(), "captura-catalogo-item-cadastro.png");
                assertTrue(janela.gravar(), janela.erros()::toString);
                ExemplosCatalogo.Exemplo<ServicoPadrao> hora = ExemplosCatalogo.servicoPorHora();
                servico.set(casos.criar(TipoCatalogo.SERVICO, hora.dados(), hora.info()));

                abas.getSelectionModel().select(aba(abas, "abaKIT"));
                PainelCatalogo<Kit> kits = achar(cena, "painelKIT");
                assertTrue(kits.tabela().getColumns().stream().noneMatch(c -> c.getText().equals("Preço de referência")),
                        "o kit não tem a coluna do preço");
                JanelaItemCatalogo<Kit> novo = kits.janelaNovo();
                texto(novo.tela(), FormularioCatalogo.NOME).setText("Kit relé auxiliar (fictício)");
                janelaKit.set(novo);
            });

            TelaDeTeste.semEsperar(() -> ((Button) linhasDoKit(janelaKit.get()).lookup("#kitAdicionarItem")).fire(),
                    falha);
            Stage escolha = TelaDeTeste.esperarJanela("Item do catálogo", falha);
            TelaDeTeste.naTelaDoJavaFx(() -> {
                DialogPane painel = TelaDeTeste.painel(escolha);
                TableView<?> tabela = (TableView<?>) painel.lookup("#escolhaCatalogo");
                assertEquals(1, tabela.getItems().size(), "só os itens, sem os serviços");
                tabela.getSelectionModel().selectFirst();
                TelaDeTeste.apertar(painel, "Usar este");
            });
            TelaDeTeste.esperar(() -> !escolha.isShowing(), "a escolha fechar");

            TelaDeTeste.naTelaDoJavaFx(() -> {
                JanelaItemCatalogo<Kit> novo = janelaKit.get();
                EditorLinhasKit linhas = linhasDoKit(novo);
                linhas.adicionar(Kit.TipoLinha.SERVICO, servico.get());
                linhas.quantidade(0).setText("2");
                fotografar(novo.dialogo(), "captura-catalogo-kit-cadastro.png");
                assertTrue(novo.gravar(), novo.erros()::toString);
                assertEquals("Cadastrado: Kit relé auxiliar (fictício) (v1).", novo.mensagem());

                PainelCatalogo<Kit> kits = achar(cena, "painelKIT");
                kits.atualizar();
                ItemCatalogo<Kit> kit = kits.tabela().getItems().getFirst();
                assertEquals(List.of("Relé 12 V 40 A (fictício)", "Limpeza de bicos teste"),
                        kit.dados().linhas().stream().map(Kit.Linha::nome).toList());
                assertEquals(List.of(new BigDecimal("2"), BigDecimal.ONE),
                        kit.dados().linhas().stream().map(Kit.Linha::quantidade).toList());
                assertEquals(List.of("1 item, 1 serviço"), coluna(kits.tabela(), "Linhas"));
                assertEquals(List.of("sim"), coluna(kits.tabela(), "Pacote"), "com serviço, é um pacote");
                salvar(cena, "captura-catalogo-kits.png");

                EditorLinhasKit reaberto = linhasDoKit(kits.janelaEditar(kit));
                assertEquals(List.of("Relé 12 V 40 A (fictício)", "Limpeza de bicos teste"), reaberto.linhas()
                        .stream().map(l -> l.get(2)).toList(), "ao editar, as linhas voltam com os nomes");
            });
            assertNull(falha.get(), () -> "erro numa janela: " + falha.get());
        } finally {
            if (principal != null) {
                principal.encerrar();
            }
        }
    }

    private static EditorLinhasKit linhasDoKit(JanelaItemCatalogo<Kit> janela) {
        return (EditorLinhasKit) janela.tela().controle("linhas");
    }

    private static int indice(String nome) {
        List<Sugestao> todos = ServicosSugeridos.todos();
        return todos.indexOf(todos.stream().filter(s -> s.nome().equals(nome)).findFirst().orElseThrow());
    }

    private static <T> List<String> coluna(TableView<ItemCatalogo<T>> tabela, String titulo) {
        TableColumn<ItemCatalogo<T>, ?> coluna = tabela.getColumns().stream()
                .filter(c -> c.getText().equals(titulo)).findFirst().orElseThrow();
        return tabela.getItems().stream().map(item -> String.valueOf(coluna.getCellData(item))).toList();
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

    private PainelCatalogo<ServicoPadrao> servicos() {
        return achar(cena, "painelSERVICO");
    }

    private static TextField texto(FormularioTela tela, String chave) {
        return (TextField) tela.controle(chave);
    }

    private static Tab aba(TabPane abas, String id) {
        return abas.getTabs().stream().filter(a -> id.equals(a.getId())).findFirst().orElseThrow();
    }
}
