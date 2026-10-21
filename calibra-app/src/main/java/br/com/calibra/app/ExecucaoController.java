package br.com.calibra.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.app.casos.CasosOficina.ChecklistDaOs;
import br.com.calibra.app.casos.CasosOficina.MarcacaoDoItem;
import br.com.calibra.core.oficina.ItemDoChecklist;
import br.com.calibra.core.oficina.Marcacao;
import br.com.calibra.core.oficina.ResumoChecklist;
import br.com.calibra.core.oficina.SituacaoItem;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.dados.oficina.ChecklistSalvo;
import br.com.calibra.dados.oficina.ItemChecklistSalvo;
import br.com.calibra.dados.oficina.MecanicoSalvo;
import br.com.calibra.dados.oficina.OrdemServicoSalva;

/**
 * Aba Execução da OS (Fase 3B, SPEC 6.5 e 8.9): um quadro por serviço com checklist, copiado do modelo quando o serviço
 * começou. Cada item tem a situação (pendente, feito ou não se aplica), a observação e, no "não se aplica", a
 * justificativa. "Salvar marcações" grava o que mudou, com quem está marcando e a hora. Embaixo, os laudos de
 * diagnóstico elétrico ({@link LaudosController}), as instalações da OS ({@link InstalacoesController}) e as fichas de
 * chicote ({@link ChicotesController}). Depois da entrega ou do cancelamento, a aba fica só para ver.
 */
public class ExecucaoController {

    /** O que a aba pede à tela da OS: fazer a ação e, se der certo, ler tudo de novo e mostrar a mensagem dela. */
    @FunctionalInterface
    interface Executor {

        boolean executar(Supplier<String> acao);
    }

    /** Um item na tela: o que está gravado e os campos da marcação. */
    private record ItemTela(ItemChecklistSalvo salvo, ToggleGroup situacao, TextField observacao,
            TextField justificativa) {

        SituacaoItem escolhida() {
            Toggle marcado = situacao.getSelectedToggle();
            return marcado == null ? null : (SituacaoItem) marcado.getUserData();
        }

        Marcacao marcacao() {
            return new Marcacao(escolhida(), observacao.getText(), justificativa.getText());
        }

        boolean mudou() {
            return !marcacao().equals(salvo.item().marcacao());
        }

        /** O item como está na tela, para o resumo do quadro. */
        ItemDoChecklist naTela() {
            ItemDoChecklist item = salvo.item();
            return new ItemDoChecklist(item.texto(), item.obrigatorio(), item.aVerificar(), marcacao());
        }
    }

    @FXML private Label situacao;
    @FXML private HBox linhaQuem;
    @FXML private ComboBox<MecanicoSalvo> quem;
    @FXML private VBox blocos;
    @FXML private HBox linhaSalvar;
    @FXML private Button botaoSalvar;
    @FXML private LaudosController laudosController;
    @FXML private InstalacoesController instalacoesController;
    @FXML private ChicotesController chicotesController;

    private CasosOficina oficina;
    private long osId;
    private Executor executor;
    private CaixaMecanico caixaQuem;
    private final List<List<ItemTela>> checklists = new ArrayList<>();

    /** @param mecanicoDaOs quem já vem em "Quem está marcando": o mecânico da OS, se estiver ativo; null = nenhum */
    void iniciar(CasosOficina oficina, long osId, Executor executor, Long mecanicoDaOs) {
        this.oficina = oficina;
        this.osId = osId;
        this.executor = executor;
        this.caixaQuem = new CaixaMecanico(quem, oficina);
        caixaQuem.carregar(mecanicoDaOs);
    }

    /** Lê os checklists de novo e mostra conforme o status da OS; o que foi mudado e não salvo continua na tela. */
    void atualizar(OrdemServicoSalva os) {
        Map<Long, Marcacao> editadas = editadas();
        List<ChecklistDaOs> lidos = oficina.checklists(osId);
        boolean leitura = os.status().finalizada();
        checklists.clear();
        blocos.getChildren().clear();
        for (ChecklistDaOs lido : lidos) {
            blocos.getChildren().add(bloco(lido, editadas, leitura));
        }
        situacao.setText(situacao(os.status(), !lidos.isEmpty()));
        mostrar(linhaQuem, !lidos.isEmpty() && !leitura);
        mostrar(linhaSalvar, !lidos.isEmpty() && !leitura);
        if (!leitura) {
            caixaQuem.carregar(caixaQuem.escolhido());
        }
        laudosController.atualizar(os);
        instalacoesController.atualizar(os);
        chicotesController.atualizar(os);
    }

    /** A área dos laudos de diagnóstico elétrico (bloco 3B.4), embaixo dos checklists. */
    LaudosController laudos() {
        return laudosController;
    }

    /** A área das instalações desta OS (bloco 3B.5), embaixo dos laudos. */
    InstalacoesController instalacoes() {
        return instalacoesController;
    }

    /** A área das fichas de chicote (bloco 3B.6), no fim da aba. */
    ChicotesController chicotes() {
        return chicotesController;
    }

    /** Alguma marcação mudou na tela e ainda não foi salva. */
    boolean mudou() {
        return checklists.stream().flatMap(List::stream).anyMatch(ItemTela::mudou);
    }

    /** A OS tem checklist (a cópia já foi feita). */
    boolean temChecklist() {
        return !checklists.isEmpty();
    }

    /** Grava todas as marcações da tela; o caso de uso só muda o que está diferente do gravado. */
    @FXML
    void salvar() {
        List<MarcacaoDoItem> marcacoes = checklists.stream().flatMap(List::stream)
                .map(item -> new MarcacaoDoItem(item.salvo().id(), item.marcacao())).toList();
        Long mecanico = caixaQuem.escolhido();
        executor.executar(() -> {
            int mudaram = oficina.marcar(osId, marcacoes, mecanico);
            return mudaram == 0 ? "Nada mudou nas marcações."
                    : mudaram == 1 ? "1 item do checklist marcado." : mudaram + " itens do checklist marcados.";
        });
    }

    // ===================== Montagem =====================

    private static String situacao(StatusOs status, boolean comChecklist) {
        if (status.finalizada()) {
            return comChecklist ? "OS " + status.rotulo().toLowerCase(Locale.ROOT) + ": o checklist fica só para ver."
                    : "Esta OS não teve checklist.";
        }
        if (!comChecklist) {
            return status.antesDaAprovacao() || status == StatusOs.APROVADA
                    ? "Os checklists aparecem quando o serviço começar: cada serviço do orçamento aprovado que tem "
                            + "checklist no catálogo ganha uma cópia do modelo."
                    : "Nenhum serviço do orçamento aprovado tem checklist. O checklist de cada serviço se escolhe em "
                            + "Catálogos › Serviços.";
        }
        return "Marque cada item e clique em \"Salvar marcações\". Os itens obrigatórios pendentes travam o "
                + "\"pronto\": dá para deixar a OS pronta com uma justificativa, que fica no histórico.";
    }

    /** O quadro de um checklist: o serviço, de onde veio a cópia, o resumo e os itens. */
    private Node bloco(ChecklistDaOs lido, Map<Long, Marcacao> editadas, boolean leitura) {
        ChecklistSalvo checklist = lido.checklist();
        Label servico = new Label(checklist.servico());
        servico.getStyleClass().add("negrito");
        Label origem = new Label("Checklist " + checklist.modeloNome() + " (v" + checklist.modeloVersao()
                + "), copiado em " + Datas.diaHora(checklist.criadoEm()));
        origem.getStyleClass().add("dica");
        VBox bloco = new VBox(6, servico, origem);
        bloco.getStyleClass().add("checklist");
        if (lido.bloqueado()) {
            Label semEntrada = new Label("Bloqueado até a entrada do veículo (aba Entrada): este serviço precisa do "
                    + "veículo na oficina.");
            semEntrada.setId("bloqueadoChecklist" + checklist.id());
            semEntrada.getStyleClass().add("aviso-amarelo");
            semEntrada.setWrapText(true);
            bloco.getChildren().add(semEntrada);
        }
        if (!lido.noOrcamento()) {
            Label fora = new Label("Este serviço saiu do orçamento aprovado: o checklist fica guardado, mas não trava "
                    + "o \"pronto\".");
            fora.getStyleClass().add("aviso-amarelo");
            fora.setWrapText(true);
            bloco.getChildren().add(fora);
        }
        Label resumo = new Label();
        resumo.setId("resumoChecklist" + checklist.id());
        bloco.getChildren().add(resumo);
        List<ItemTela> itens = new ArrayList<>();
        for (ItemChecklistSalvo salvo : checklist.itens()) {
            ItemTela item = item(salvo, editadas.getOrDefault(salvo.id(), salvo.item().marcacao()),
                    leitura || lido.bloqueado());
            item.situacao().selectedToggleProperty().addListener((obs, antes, depois) -> resumir(resumo, itens));
            itens.add(item);
            bloco.getChildren().add(linha(item));
        }
        resumir(resumo, itens);
        checklists.add(itens);
        return bloco;
    }

    private ItemTela item(ItemChecklistSalvo salvo, Marcacao marcacao, boolean leitura) {
        ToggleGroup grupo = new ToggleGroup();
        TextField observacao = new TextField(marcacao.observacao());
        observacao.setId("observacao" + salvo.id());
        observacao.setPromptText("Observação (opcional)");
        observacao.setPrefColumnCount(22);
        TextField justificativa = new TextField(marcacao.justificativa());
        justificativa.setId("justificativa" + salvo.id());
        justificativa.setPromptText("Por que não se aplica (obrigatório)");
        justificativa.setPrefColumnCount(22);
        for (SituacaoItem possivel : SituacaoItem.values()) {
            RadioButton opcao = new RadioButton(maiuscula(possivel.rotulo()));
            opcao.setId("item" + salvo.id() + "-" + possivel.name());
            opcao.setUserData(possivel);
            opcao.setToggleGroup(grupo);
            opcao.setSelected(possivel == marcacao.situacao());
            opcao.setMinWidth(Region.USE_PREF_SIZE);
        }
        mostrar(justificativa, marcacao.situacao() == SituacaoItem.NAO_SE_APLICA);
        grupo.selectedToggleProperty().addListener((obs, antes, depois) -> {
            boolean naoSeAplica = depois != null && depois.getUserData() == SituacaoItem.NAO_SE_APLICA;
            mostrar(justificativa, naoSeAplica);
            if (naoSeAplica) {
                justificativa.requestFocus();
            }
        });
        for (Node campo : List.of(observacao, justificativa)) {
            campo.setDisable(leitura);
        }
        grupo.getToggles().forEach(opcao -> ((RadioButton) opcao).setDisable(leitura));
        return new ItemTela(salvo, grupo, observacao, justificativa);
    }

    /** O item: o texto com as marcas, a situação com a observação e a justificativa, e quem marcou. */
    private static Node linha(ItemTela item) {
        ItemDoChecklist dados = item.salvo().item();
        Label texto = new Label(item.salvo().ordem() + ". " + dados.texto());
        texto.setWrapText(true);
        HBox.setHgrow(texto, Priority.ALWAYS);
        texto.setMaxWidth(Double.MAX_VALUE);
        HBox topo = new HBox(8, texto);
        topo.setAlignment(Pos.CENTER_LEFT);
        if (dados.obrigatorio()) {
            topo.getChildren().add(marca("obrigatório"));
        }
        if (dados.aVerificar()) {
            topo.getChildren().add(marca("a verificar"));
        }
        HBox marcacao = new HBox(12);
        marcacao.setAlignment(Pos.CENTER_LEFT);
        item.situacao().getToggles().forEach(opcao -> marcacao.getChildren().add((RadioButton) opcao));
        HBox.setHgrow(item.observacao(), Priority.ALWAYS);
        HBox.setHgrow(item.justificativa(), Priority.ALWAYS);
        marcacao.getChildren().addAll(item.observacao(), item.justificativa());
        VBox linha = new VBox(4, topo, marcacao);
        linha.getStyleClass().add("item-checklist");
        ItemChecklistSalvo salvo = item.salvo();
        if (salvo.marcadoEm() != null) {
            Label quem = new Label("Marcado" + (salvo.marcadoPor() == null ? "" : " por " + salvo.marcadoPor())
                    + " em " + Datas.diaHora(salvo.marcadoEm()));
            quem.getStyleClass().add("dica");
            linha.getChildren().add(quem);
        }
        return linha;
    }

    /** "3 de 9 itens feitos, 6 pendentes · 5 obrigatórios pendentes", com o que está na tela. */
    private static void resumir(Label resumo, List<ItemTela> itens) {
        List<ItemDoChecklist> naTela = itens.stream().map(ItemTela::naTela).toList();
        long obrigatorios = naTela.stream().filter(ItemDoChecklist::pendente).count();
        resumo.setText(ResumoChecklist.de(naTela).texto() + (obrigatorios == 0 ? ""
                : " · " + obrigatorios + (obrigatorios == 1 ? " obrigatório pendente" : " obrigatórios pendentes")));
    }

    private static Label marca(String texto) {
        Label marca = new Label(texto);
        marca.getStyleClass().add("marca");
        marca.setMinWidth(Region.USE_PREF_SIZE);
        return marca;
    }

    private static String maiuscula(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private Map<Long, Marcacao> editadas() {
        Map<Long, Marcacao> editadas = new HashMap<>();
        checklists.stream().flatMap(List::stream).filter(ItemTela::mudou)
                .forEach(item -> editadas.put(item.salvo().id(), item.marcacao()));
        return editadas;
    }

    private static void mostrar(Node no, boolean visivel) {
        no.setVisible(visivel);
        no.setManaged(visivel);
    }

    // ===================== Para os testes =====================

    /** Marca um item pela tela, sem salvar: o checklist e o item contam a partir de 0. */
    void marcar(int checklist, int item, SituacaoItem nova, String observacao, String justificativa) {
        ItemTela tela = checklists.get(checklist).get(item);
        tela.situacao().getToggles().stream().filter(opcao -> opcao.getUserData() == nova).findFirst()
                .ifPresent(opcao -> tela.situacao().selectToggle(opcao));
        tela.observacao().setText(observacao);
        tela.justificativa().setText(justificativa);
    }

    /** Quantos itens tem cada checklist da tela. */
    List<Integer> itensPorChecklist() {
        return checklists.stream().map(List::size).toList();
    }

    /** O campo da observação do item (desligado depois da entrega). */
    TextField observacao(int checklist, int item) {
        return checklists.get(checklist).get(item).observacao();
    }

    ComboBox<MecanicoSalvo> quem() {
        return quem;
    }

    Label situacao() {
        return situacao;
    }

    Button botaoSalvar() {
        return botaoSalvar;
    }
}
