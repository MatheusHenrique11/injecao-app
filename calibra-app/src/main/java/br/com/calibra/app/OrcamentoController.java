package br.com.calibra.app;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosOrcamento;
import br.com.calibra.app.casos.CasosOrcamento.SituacaoOrcamento;
import br.com.calibra.app.casos.FormularioOrcamento;
import br.com.calibra.app.casos.FormularioOrcamento.Linha;
import br.com.calibra.app.casos.FormularioOrcamento.TipoDesconto;
import br.com.calibra.app.casos.LogoOficina;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.orcamento.StatusOrcamento;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;
import br.com.calibra.relatorios.DadosOrcamento;
import br.com.calibra.relatorios.RelatorioOrcamento;
import br.com.calibra.relatorios.TextoOrcamento;
import br.com.calibra.relatorios.TextoWhatsApp;

/**
 * O editor do orçamento (bloco 2.5), na aba "Peças e orçamento" do projeto e na aba "Orçamento" da OS (bloco 3.2): a
 * revisão mais nova (itens, serviços, desconto, prazo, validade), o PDF, o texto para WhatsApp, o envio, a resposta do
 * cliente e as revisões. Só o rascunho se edita. De onde vem o orçamento, diz a {@link FonteOrcamento}.
 */
public class OrcamentoController {

    /** O que a aba usa do resto do app. */
    record Recursos(CasosOrcamento casos, Supplier<PadroesOrcamento> padroes, Supplier<DadosOficina> oficina,
            Path anexos, String versaoApp) {
    }

    @FXML private Label numero;
    @FXML private Label situacao;
    @FXML private Label explicacao;
    @FXML private HBox faixaDesatualizado;
    @FXML private Label motivos;
    @FXML private Button botaoUsarCalculoMaisNovo;
    @FXML private Label dicaItens;
    @FXML private GridPane gradeItens;
    @FXML private Button botaoAdicionarPeca;
    @FXML private Button botaoAdicionarServico;
    @FXML private Button botaoPecasDoCalculo;
    @FXML private ComboBox<TipoDesconto> tipoDesconto;
    @FXML private TextField desconto;
    @FXML private TextField validadeDias;
    @FXML private TextField prazoDias;
    @FXML private TextField prazoObservacao;
    @FXML private TextArea condicoes;
    @FXML private TextArea observacoesOrcamento;
    @FXML private Label totalPecas;
    @FXML private Label totalServicos;
    @FXML private Label rotuloDesconto;
    @FXML private Label totalDesconto;
    @FXML private Label total;
    @FXML private Button botaoSalvar;
    @FXML private Button botaoDescartar;
    @FXML private VBox mensagens;
    @FXML private Button botaoPdf;
    @FXML private Button botaoWhatsApp;
    @FXML private Label avisoDocumento;
    @FXML private Button botaoEnviado;
    @FXML private Button botaoAprovado;
    @FXML private Button botaoRecusado;
    @FXML private Button botaoNovaRevisao;
    @FXML private TableView<SituacaoOrcamento> revisoes;
    @FXML private TableColumn<SituacaoOrcamento, String> colunaNumero;
    @FXML private TableColumn<SituacaoOrcamento, String> colunaSituacao;
    @FXML private TableColumn<SituacaoOrcamento, String> colunaTotal;
    @FXML private TableColumn<SituacaoOrcamento, String> colunaCriado;
    @FXML private TableColumn<SituacaoOrcamento, String> colunaEnviado;
    @FXML private TableColumn<SituacaoOrcamento, String> colunaResposta;

    private final List<LinhaTela> linhas = new ArrayList<>();
    private Recursos recursos;
    private HostServices servicos;
    private FonteOrcamento fonte;
    private Runnable aoMudar = () -> { };
    /** A revisão mais nova; null enquanto não há orçamento gravado. */
    private SituacaoOrcamento atual;
    /** Sem orçamento gravado e sem cálculo em dia: não dá para começar um. */
    private String semCalculo;
    /** O que está no editor mudou e não foi gravado. */
    private boolean alterado;
    private boolean preenchendo;

    /** Uma linha de item na tela. */
    private final class LinhaTela {
        private final ComboBox<TipoItem> tipo = new ComboBox<>();
        private final TextField descricao = new TextField();
        private final TextField quantidade = new TextField();
        private final TextField unidade = new TextField();
        private final TextField valor = new TextField();
        private final Label totalLinha = new Label();
        private final Button remover = new Button("Remover");
        private final Long catalogoItemId;
        private final Integer catalogoVersao;

        LinhaTela(Linha linha) {
            catalogoItemId = linha.catalogoItemId();
            catalogoVersao = linha.catalogoVersao();
            CamposTela.configurar(tipo, Arrays.asList(TipoItem.values()), TipoItem::rotulo);
            tipo.setValue(linha.tipo());
            descricao.setText(linha.descricao());
            quantidade.setText(linha.quantidade());
            unidade.setText(linha.unidade());
            valor.setText(linha.valor());
            quantidade.setPrefColumnCount(5);
            unidade.setPrefColumnCount(4);
            valor.setPrefColumnCount(8);
            totalLinha.setMinWidth(110);
            if (catalogoItemId != null) {
                descricao.setTooltip(new Tooltip("Peça do catálogo (versão " + catalogoVersao + "). O preço vem do "
                        + "preço de referência dela."));
            }
            tipo.valueProperty().addListener((obs, antes, depois) -> aoMudarCampo());
            for (TextField campo : List.of(descricao, quantidade, unidade, valor)) {
                campo.textProperty().addListener((obs, antes, depois) -> aoMudarCampo());
            }
            remover.setOnAction(evento -> {
                linhas.remove(this);
                montarGrade();
                aoMudarCampo();
            });
        }

        Linha linha() {
            return new Linha(tipo.getValue(), descricao.getText(), quantidade.getText(), unidade.getText(),
                    valor.getText(), catalogoItemId, catalogoVersao);
        }

        void habilitar(boolean editavel) {
            for (Control controle : List.of(tipo, descricao, quantidade, unidade, valor)) {
                controle.setDisable(!editavel);
            }
            remover.setVisible(editavel);
        }
    }

    @FXML
    private void initialize() {
        CamposTela.configurar(tipoDesconto, Arrays.asList(TipoDesconto.values()), TipoDesconto::rotulo);
        tipoDesconto.valueProperty().addListener((obs, antes, depois) -> aoMudarCampo());
        for (TextInputControl campo : List.of(desconto, validadeDias, prazoDias, prazoObservacao, condicoes,
                observacoesOrcamento)) {
            campo.textProperty().addListener((obs, antes, depois) -> aoMudarCampo());
        }
        coluna(colunaNumero, s -> s.orcamento().numero().comRevisao());
        coluna(colunaSituacao, s -> s.orcamento().status().rotulo() + (s.desatualizado()
                && s.orcamento().status() != StatusOrcamento.SUBSTITUIDO ? " (desatualizado)" : ""));
        coluna(colunaTotal, s -> s.orcamento().conteudo().total().formatar());
        coluna(colunaCriado, s -> Datas.diaHora(s.orcamento().criadoEm()));
        coluna(colunaEnviado, s -> s.orcamento().enviadoEm() == null ? "—" : Datas.diaHora(s.orcamento().enviadoEm()));
        coluna(colunaResposta, OrcamentoController::resposta);
        gradeItens.getColumnConstraints().setAll(new ColumnConstraints(110), coluna(280, true),
                new ColumnConstraints(80), new ColumnConstraints(70), new ColumnConstraints(150),
                new ColumnConstraints(120), new ColumnConstraints(90));
    }

    private static ColumnConstraints coluna(double minima, boolean cresce) {
        ColumnConstraints coluna = new ColumnConstraints();
        coluna.setMinWidth(minima);
        coluna.setHgrow(cresce ? Priority.ALWAYS : Priority.NEVER);
        return coluna;
    }

    /**
     * @param aoMudar chamado quando o status do projeto ou da OS pode ter mudado (enviado, aprovado, recusado)
     */
    void iniciar(Recursos recursos, HostServices servicos, FonteOrcamento fonte, Runnable aoMudar) {
        this.recursos = recursos;
        this.servicos = servicos;
        this.fonte = fonte;
        this.aoMudar = aoMudar;
        atualizar();
    }

    /**
     * Lê de novo as revisões e a situação (ao voltar para a aba: o cálculo ou o motor podem ter mudado). O que está
     * sendo editado e não foi gravado fica.
     */
    void atualizar() {
        List<SituacaoOrcamento> lista = fonte.revisoes();
        revisoes.getItems().setAll(lista);
        SituacaoOrcamento anterior = atual;
        atual = lista.isEmpty() ? null : lista.getFirst();
        boolean outraRevisao = anterior == null ? atual != null
                : atual == null || anterior.orcamento().id() != atual.orcamento().id();
        if (!alterado || outraRevisao) {
            preencherEditor();
        }
        atualizarEstado();
    }

    /** O editor com a revisão mais nova; sem orçamento, a sugestão (no de preparação, a do último cálculo). */
    private void preencherEditor() {
        semCalculo = null;
        if (atual != null) {
            preencher(atual.orcamento().conteudo());
            return;
        }
        try {
            preencher(fonte.sugestao(recursos.padroes().get()));
        } catch (EntradaInvalidaException erro) {
            semCalculo = erro.getMessage();
            preencher(recursos.padroes().get().orcamento(List.of()));
        }
    }

    private void preencher(Orcamento orcamento) {
        preenchendo = true;
        try {
            FormularioOrcamento.Textos textos = FormularioOrcamento.escrever(orcamento);
            linhas.clear();
            textos.linhas().forEach(linha -> linhas.add(new LinhaTela(linha)));
            montarGrade();
            tipoDesconto.setValue(textos.tipoDesconto());
            desconto.setText(textos.desconto());
            validadeDias.setText(textos.validadeDias());
            prazoDias.setText(textos.prazoDias());
            prazoObservacao.setText(textos.prazoObservacao());
            condicoes.setText(textos.condicoes());
            observacoesOrcamento.setText(textos.observacoes());
        } finally {
            preenchendo = false;
        }
        alterado = false;
        atualizarTotais();
    }

    private void montarGrade() {
        gradeItens.getChildren().clear();
        String[] titulos = {"Tipo", "Descrição", "Qtd.", "Un.", "Valor unitário (R$)", "Total", ""};
        for (int i = 0; i < titulos.length; i++) {
            Label titulo = new Label(titulos[i]);
            titulo.getStyleClass().add("negrito");
            gradeItens.add(titulo, i, 0);
        }
        for (int i = 0; i < linhas.size(); i++) {
            LinhaTela linha = linhas.get(i);
            gradeItens.addRow(i + 1, linha.tipo, linha.descricao, linha.quantidade, linha.unidade, linha.valor,
                    linha.totalLinha, linha.remover);
            GridPane.setHalignment(linha.totalLinha, HPos.RIGHT);
        }
        if (linhas.isEmpty()) {
            Label vazio = new Label("Nenhum item. Use \"+ Peça\" ou \"+ Serviço\".");
            vazio.getStyleClass().add("dica");
            gradeItens.add(vazio, 0, 1, 7, 1);
        }
        linhas.forEach(linha -> linha.habilitar(editavel()));
    }

    // ===================== Reações =====================

    private void aoMudarCampo() {
        desconto.setDisable(!editavel() || tipoDesconto.getValue() == TipoDesconto.NENHUM);
        if (preenchendo) {
            return;
        }
        alterado = true;
        atualizarTotais();
        atualizarBotoes();
    }

    private FormularioOrcamento.Textos textos() {
        return new FormularioOrcamento.Textos(linhas.stream().map(LinhaTela::linha).toList(), tipoDesconto.getValue(),
                desconto.getText(), prazoDias.getText(), prazoObservacao.getText(), validadeDias.getText(),
                condicoes.getText(), observacoesOrcamento.getText());
    }

    /** Totais enquanto se digita: de cada linha que já fecha; o desconto e o total só com tudo certo. */
    private void atualizarTotais() {
        Dinheiro pecas = Dinheiro.ZERO;
        Dinheiro servicos = Dinheiro.ZERO;
        for (LinhaTela linha : linhas) {
            Optional<Dinheiro> doItem = FormularioOrcamento.totalDaLinha(linha.linha());
            linha.totalLinha.setText(doItem.map(Dinheiro::formatar).orElse("—"));
            if (doItem.isPresent()) {
                if (linha.tipo.getValue() == TipoItem.SERVICO) {
                    servicos = servicos.mais(doItem.get());
                } else {
                    pecas = pecas.mais(doItem.get());
                }
            }
        }
        totalPecas.setText(pecas.formatar());
        totalServicos.setText(servicos.formatar());
        Orcamento lido = FormularioOrcamento.ler(textos()).orcamento();
        rotuloDesconto.setText(lido == null ? "Desconto" : TextoOrcamento.rotuloDesconto(lido.desconto()));
        totalDesconto.setText(lido == null ? "—" : "− " + lido.valorDesconto().formatar());
        total.setText(lido == null ? "— (confira os itens)" : lido.total().formatar());
    }

    private boolean editavel() {
        return atual == null ? semCalculo == null : atual.orcamento().status().editavel();
    }

    /** Usa o cálculo do projeto: o orçamento gravado diz por si; o novo, pela fonte (a OS com projeto ligado). */
    private boolean comCalculo() {
        return atual != null ? atual.orcamento().dePreparacao() : fonte.comCalculo();
    }

    private void atualizarEstado() {
        OrcamentoSalvo o = atual == null ? null : atual.orcamento();
        numero.setText(o == null ? "Orçamento novo" : "Orçamento " + o.numero().comRevisao());
        situacao.setText(o == null ? "não gravado" : o.status().rotulo());
        explicacao.setText(explicacao(o));
        boolean comCalculo = comCalculo();
        dicaItens.setText(comCalculo ? "As peças vêm do último cálculo salvo, com o preço de referência do catálogo "
                + "(peça sem preço vem com o valor em branco). Some os serviços, como a mão de obra em horas. Valores "
                + "em reais com vírgula (ex.: 1.234,56)." : "Monte os itens: as peças e os serviços, como a mão de "
                + "obra em horas. Valores em reais com vírgula (ex.: 1.234,56).");
        botaoPecasDoCalculo.setVisible(comCalculo);
        botaoPecasDoCalculo.setManaged(comCalculo);
        avisoDocumento.setVisible(!comCalculo);
        avisoDocumento.setManaged(!comCalculo);
        boolean desatualizado = atual != null && atual.desatualizado() && o.status() != StatusOrcamento.SUBSTITUIDO;
        faixaDesatualizado.setVisible(desatualizado);
        faixaDesatualizado.setManaged(desatualizado);
        motivos.setText(desatualizado ? "Desatualizado: " + String.join("; ", atual.motivos()) + "." : "");
        boolean editavel = editavel();
        for (Control campo : List.of(tipoDesconto, validadeDias, prazoDias, prazoObservacao,
                condicoes, observacoesOrcamento)) {
            campo.setDisable(!editavel);
        }
        desconto.setDisable(!editavel || tipoDesconto.getValue() == TipoDesconto.NENHUM);
        linhas.forEach(linha -> linha.habilitar(editavel));
        atualizarBotoes();
    }

    private String explicacao(OrcamentoSalvo o) {
        if (o == null) {
            return semCalculo != null ? semCalculo
                    : "Orçamento novo, ainda não gravado: confira os itens, some os serviços e clique em Salvar "
                            + "rascunho.";
        }
        return switch (o.status()) {
            case RASCUNHO -> "Rascunho: edite à vontade. Quando mandar ao cliente, marque como enviado.";
            case ENVIADO -> "Enviado em " + Datas.diaHora(o.enviadoEm()) + "; válido "
                    + TextoOrcamento.validade(o.conteudo(), o.enviadoEm().toLocalDate())
                    + ". Registre a resposta do cliente quando ela vier.";
            case APROVADO -> "Aprovado pelo cliente em " + Datas.diaHora(o.respondidoEm()) + " ("
                    + o.meioResposta().rotulo() + "). Não muda mais: para alterar, abra uma revisão nova.";
            case RECUSADO -> "Recusado em " + Datas.diaHora(o.respondidoEm()) + " (" + o.meioResposta().rotulo()
                    + "). Para tentar de novo, abra uma revisão nova.";
            case SUBSTITUIDO -> "Substituído por uma revisão mais nova.";
        };
    }

    private void atualizarBotoes() {
        boolean editavel = editavel();
        StatusOrcamento status = atual == null ? null : atual.orcamento().status();
        botaoSalvar.setDisable(!editavel);
        botaoDescartar.setDisable(!editavel || !alterado);
        botaoAdicionarPeca.setDisable(!editavel);
        botaoAdicionarServico.setDisable(!editavel);
        botaoPecasDoCalculo.setDisable(!editavel);
        boolean documento = atual != null && comCalculo();
        botaoPdf.setDisable(!documento);
        botaoWhatsApp.setDisable(!documento);
        botaoEnviado.setDisable(status != StatusOrcamento.RASCUNHO);
        botaoAprovado.setDisable(status != StatusOrcamento.ENVIADO);
        botaoRecusado.setDisable(status != StatusOrcamento.ENVIADO);
        botaoNovaRevisao.setDisable(status == null || !status.aceitaRevisao());
        botaoUsarCalculoMaisNovo.setDisable(status != StatusOrcamento.RASCUNHO);
    }

    // ===================== Itens =====================

    @FXML
    private void adicionarPeca() {
        adicionar(Linha.digitada(TipoItem.PECA, "", "1", "un", "")).descricao.requestFocus();
    }

    @FXML
    private void adicionarServico() {
        Dinheiro hora = recursos.padroes().get().valorHora();
        adicionar(Linha.digitada(TipoItem.SERVICO, "Mão de obra", "1", "h",
                hora == null ? "" : hora.formatarSemSimbolo())).quantidade.requestFocus();
    }

    private LinhaTela adicionar(Linha linha) {
        LinhaTela nova = new LinhaTela(linha);
        linhas.add(nova);
        montarGrade();
        aoMudarCampo();
        return nova;
    }

    /** As peças voltam a ser as do último cálculo (com os preços do catálogo); os serviços ficam. */
    @FXML
    private void trazerPecasDoCalculo() {
        try {
            List<ItemOrcamento> doCalculo = fonte.sugestao(recursos.padroes().get()).itens();
            linhas.removeIf(linha -> linha.tipo.getValue() == TipoItem.PECA);
            List<LinhaTela> pecas = new ArrayList<>();
            FormularioOrcamento.escrever(new Orcamento(doCalculo, null, 0, "", 1, "", "")).linhas()
                    .forEach(linha -> pecas.add(new LinhaTela(linha)));
            linhas.addAll(0, pecas);
            montarGrade();
            aoMudarCampo();
            mostrarOk("Peças trazidas do último cálculo. Confira os valores e salve.");
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
        }
    }

    // ===================== Rascunho =====================

    @FXML
    private void salvarRascunho() {
        if (gravarRascunho()) {
            mostrarOk("Rascunho salvo: " + atual.orcamento().numero().comRevisao() + ".");
        }
    }

    /** Grava o que está no editor (cria o orçamento, se for o primeiro); com erro, mostra o que corrigir. */
    private boolean gravarRascunho() {
        FormularioOrcamento.Leitura leitura = FormularioOrcamento.ler(textos());
        if (!leitura.erros().isEmpty()) {
            mostrarErros(leitura.erros());
            return false;
        }
        try {
            if (atual == null) {
                fonte.criar(leitura.orcamento());
            } else {
                recursos.casos().salvar(atual.orcamento().id(), leitura.orcamento());
            }
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
            return false;
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
            return false;
        }
        alterado = false;
        atualizar();
        return true;
    }

    /** Antes do PDF, do texto e do envio, o que mudou no rascunho é gravado. */
    private boolean gravarSePreciso() {
        if (atual == null || (editavel() && alterado)) {
            return gravarRascunho();
        }
        return true;
    }

    @FXML
    private void descartar() {
        preencherEditor();
        atualizarEstado();
        mensagens.getChildren().clear();
    }

    @FXML
    private void usarCalculoMaisNovo() {
        executar(() -> recursos.casos().usarCalculoMaisNovo(atual.orcamento().id()),
                "O rascunho agora usa o cálculo mais novo. Confira os itens (\"Trazer as peças do cálculo de novo\" "
                        + "refaz as peças).");
    }

    // ===================== PDF, texto e status =====================

    @FXML
    private void gerarPdf() {
        if (!gravarSePreciso()) {
            return;
        }
        DadosOrcamento dados = documento();
        String nome = dados.numero().texto() + (dados.numero().revisao() == 1 ? "" : "-rev" + dados.numero().revisao())
                + ".pdf";
        ArquivoPdf.gerar(janela(), servicos, "Orçamento", nome, destino -> RelatorioOrcamento.gerar(dados, destino),
                this::mostrarOk, () -> { });
    }

    /** O PDF da revisão mais nova, sem perguntar onde salvar (para os testes e a evidência). */
    void escreverPdf(Path destino) throws IOException {
        if (gravarSePreciso()) {
            RelatorioOrcamento.gerar(documento(), destino);
        }
    }

    @FXML
    private void copiarWhatsApp() {
        if (!gravarSePreciso()) {
            return;
        }
        ClipboardContent conteudo = new ClipboardContent();
        conteudo.putString(TextoWhatsApp.de(documento()));
        Clipboard.getSystemClipboard().setContent(conteudo);
        mostrarOk("Texto copiado. Cole no WhatsApp do cliente (Ctrl+V) e envie você mesmo.");
    }

    private DadosOrcamento documento() {
        DadosOficina oficina = recursos.oficina().get();
        Path logo = LogoOficina.arquivo(oficina, recursos.anexos()).orElse(null);
        return recursos.casos().documento(atual.orcamento().id(), oficina, logo, recursos.versaoApp());
    }

    @FXML
    private void marcarEnviado() {
        if (!gravarSePreciso()) {
            return;
        }
        executar(() -> recursos.casos().marcarEnviado(atual.orcamento().id()),
                "Marcado como enviado." + fonte.aoEnviar());
    }

    @FXML
    private void aprovado() {
        JanelaResposta.mostrar(janela(), "Aprovado pelo cliente", LocalDateTime.now())
                .ifPresent(r -> aprovar(r.quando(), r.meio(), r.observacao()));
    }

    void aprovar(LocalDateTime quando, MeioAprovacao meio, String observacao) {
        executar(() -> recursos.casos().aprovar(atual.orcamento().id(), quando, meio, observacao),
                "Aprovação registrada." + fonte.aoAprovar());
    }

    @FXML
    private void recusado() {
        JanelaResposta.mostrar(janela(), "Recusado pelo cliente", LocalDateTime.now())
                .ifPresent(r -> recusar(r.quando(), r.meio(), r.observacao()));
    }

    void recusar(LocalDateTime quando, MeioAprovacao meio, String observacao) {
        executar(() -> recursos.casos().recusar(atual.orcamento().id(), quando, meio, observacao),
                "Recusa registrada.");
    }

    @FXML
    private void novaRevisao() {
        executar(() -> recursos.casos().novaRevisao(atual.orcamento().id()),
                "Revisão nova aberta em rascunho. A anterior ficou como substituída.");
    }

    /** Faz a ação, relê tudo e avisa; com erro, mostra a mensagem. */
    private void executar(Runnable acao, String sucesso) {
        try {
            acao.run();
        } catch (EntradaInvalidaException erro) {
            mostrarErros(erro.erros());
            return;
        } catch (ErroDados erro) {
            mostrarErros(List.of(erro.getMessage()));
            return;
        }
        alterado = false;
        atualizar();
        aoMudar.run();
        mostrarOk(sucesso);
    }

    // ===================== Para os testes =====================

    /** A situação da revisão mais nova; vazio sem orçamento gravado. */
    Optional<SituacaoOrcamento> atual() {
        return Optional.ofNullable(atual);
    }

    /** As linhas do editor, como texto. */
    List<Linha> linhas() {
        return linhas.stream().map(LinhaTela::linha).toList();
    }

    /** Escreve numa linha do editor (para os testes): descrição, quantidade, unidade e valor. */
    void preencherLinha(int indice, String descricaoTexto, String quantidadeTexto, String unidadeTexto,
            String valorTexto) {
        LinhaTela linha = linhas.get(indice);
        linha.descricao.setText(descricaoTexto);
        linha.quantidade.setText(quantidadeTexto);
        linha.unidade.setText(unidadeTexto);
        linha.valor.setText(valorTexto);
    }

    List<String> mensagens() {
        return mensagens.getChildren().stream().map(no -> ((Label) no).getText()).toList();
    }

    // ===================== Utilidades =====================

    private static String resposta(SituacaoOrcamento s) {
        OrcamentoSalvo o = s.orcamento();
        if (o.respondidoEm() == null) {
            return "—";
        }
        return Datas.diaHora(o.respondidoEm()) + " · " + o.meioResposta().rotulo()
                + (o.observacaoResposta().isEmpty() ? "" : " · " + o.observacaoResposta());
    }

    private static void coluna(TableColumn<SituacaoOrcamento, String> coluna, Function<SituacaoOrcamento, String> texto) {
        coluna.setCellValueFactory(celula -> new ReadOnlyStringWrapper(texto.apply(celula.getValue())));
    }

    private Window janela() {
        return numero.getScene() == null ? null : numero.getScene().getWindow();
    }

    private void mostrarErros(List<String> erros) {
        mensagens.getChildren().clear();
        for (String erro : erros) {
            Label rotulo = new Label("• " + erro);
            rotulo.getStyleClass().add("erro");
            rotulo.setWrapText(true);
            mensagens.getChildren().add(rotulo);
        }
    }

    private void mostrarOk(String texto) {
        Label rotulo = new Label(texto);
        rotulo.getStyleClass().add("ok");
        rotulo.setWrapText(true);
        mensagens.getChildren().setAll(rotulo);
    }
}
