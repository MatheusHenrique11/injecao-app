package br.com.calibra.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.cadastro.TipoVeiculo;
import br.com.calibra.core.cadastro.Tracao;
import br.com.calibra.core.cadastro.Veiculo;
import br.com.calibra.core.pecas.Cambio;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Formulário do veículo (SPEC 6.2), usado no assistente e na aba Veículo. Os campos de identificação mudam
 * conforme o tipo; os escondidos não são gravados ({@link Veiculo#ajustadoAoTipo()}).
 */
public class VeiculoFormController {

    /** Leitura dos campos: o veículo (null se houve erro de digitação) e os erros. */
    record Leitura(Veiculo veiculo, List<String> erros) {

        Leitura {
            erros = List.copyOf(erros);
        }
    }

    @FXML private ComboBox<TipoVeiculo> tipo;
    @FXML private TextField cor;
    @FXML private TextField marca;
    @FXML private TextField modelo;
    @FXML private TextField versao;
    @FXML private TextField anoFabricacao;
    @FXML private TextField anoModelo;
    @FXML private Node secaoTerrestre;
    @FXML private TextField placa;
    @FXML private TextField chassi;
    @FXML private ComboBox<Tracao> tracao;
    @FXML private Node secaoNautica;
    @FXML private TextField nomeEmbarcacao;
    @FXML private TextField numeroInscricao;
    @FXML private TextField numeroCasco;
    @FXML private Label rotuloUso;
    @FXML private TextField usoAcumulado;
    @FXML private TextField pesoKg;
    @FXML private ComboBox<TipoCambio> cambio;
    @FXML private CheckBox cambioControlado;
    @FXML private Label rotuloCambioCatalogo;
    @FXML private ComboBox<ItemCatalogo<Cambio>> cambioCatalogo;
    @FXML private Label dicaCambioCatalogo;
    @FXML private TextArea observacoes;

    private CaixaCatalogo<Cambio> caixaCambio;
    private CasosCatalogo catalogo;

    @FXML
    private void initialize() {
        CamposTela.configurar(tipo, Arrays.asList(TipoVeiculo.values()), TipoVeiculo::rotulo);
        CamposTela.configurarOpcional(tracao, Arrays.asList(Tracao.values()), Tracao::rotulo);
        caixaCambio = new CaixaCatalogo<>(cambioCatalogo, "(nenhum)");
        tipo.valueProperty().addListener((obs, antes, depois) -> atualizarTipo());
        cambio.valueProperty().addListener((obs, antes, depois) -> atualizarCambioControlado());
        caixaCambio.aoMudar(this::aplicarCambioDoCatalogo);
        limpar();
    }

    /** Liga o formulário ao catálogo: aparece a escolha do câmbio do catálogo. */
    void usarCatalogo(CasosCatalogo casos) {
        this.catalogo = casos;
        for (Node no : List.of(rotuloCambioCatalogo, cambioCatalogo, dicaCambioCatalogo)) {
            mostrar(no, true);
        }
        atualizarCambiosDoCatalogo();
    }

    /** Mostra o veículo gravado, com o câmbio do catálogo dele (mesmo arquivado). */
    void preencher(Veiculo v, Long cambioItemId) {
        preencher(v);
        if (catalogo != null && cambioItemId != null) {
            catalogo.buscar(TipoCatalogo.CAMBIO, cambioItemId).ifPresent(caixaCambio::escolher);
        } else {
            caixaCambio.limpar();
        }
    }

    /** O câmbio do catálogo escolhido; null se nenhum. */
    Long cambioItemId() {
        return caixaCambio.escolhido() == null ? null : caixaCambio.escolhido().id();
    }

    /** Câmbio do catálogo escolhido: o tipo de câmbio do veículo passa a ser o dele. */
    private void aplicarCambioDoCatalogo() {
        ItemCatalogo<Cambio> item = caixaCambio.escolhido();
        if (item != null && item.dados().tipo() != null) {
            cambio.setValue(item.dados().tipo());
        }
        cambio.setDisable(item != null);
    }

    /** Lê de novo os câmbios do catálogo (a tela Catálogos pode tê-los mudado). */
    void atualizarCatalogo() {
        atualizarCambiosDoCatalogo();
    }

    /** Os câmbios do catálogo do tipo do veículo (terrestre ou náutico). */
    private void atualizarCambiosDoCatalogo() {
        if (catalogo == null) {
            return;
        }
        boolean nautico = tipo.getValue() != null && tipo.getValue().nautico();
        caixaCambio.itens(catalogo.ativos(TipoCatalogo.CAMBIO).stream()
                .filter(item -> item.dados().tipo() == null || item.dados().tipo().nautico() == nautico).toList());
    }

    /** Formulário de veículo novo: um carro, com o resto em branco. */
    void limpar() {
        for (TextField texto : List.of(cor, marca, modelo, versao, anoFabricacao, anoModelo, placa, chassi,
                nomeEmbarcacao, numeroInscricao, numeroCasco, usoAcumulado, pesoKg)) {
            texto.clear();
        }
        observacoes.clear();
        tipo.setValue(TipoVeiculo.CARRO);
        tracao.setValue(null);
        caixaCambio.limpar();
        cambio.setValue(null);
        cambioControlado.setSelected(false);
        atualizarTipo();
    }

    void preencher(Veiculo v) {
        tipo.setValue(v.tipo());
        cor.setText(v.cor());
        marca.setText(v.marca());
        modelo.setText(v.modelo());
        versao.setText(v.versao());
        anoFabricacao.setText(v.anoFabricacao() == null ? "" : v.anoFabricacao().toString());
        anoModelo.setText(v.anoModelo() == null ? "" : v.anoModelo().toString());
        placa.setText(v.placa());
        chassi.setText(v.chassi());
        tracao.setValue(v.tracao());
        nomeEmbarcacao.setText(v.nomeEmbarcacao());
        numeroInscricao.setText(v.numeroInscricao());
        numeroCasco.setText(v.numeroCasco());
        usoAcumulado.setText(v.usoAcumulado() == null ? "" : NumeroBr.formatar(v.usoAcumulado(), 0, 1));
        pesoKg.setText(v.pesoKg() == null ? "" : NumeroBr.formatar(v.pesoKg(), 0, 1));
        atualizarTipo();
        cambio.setValue(v.cambio());
        cambioControlado.setSelected(v.cambioControladoPelaInjecao());
        observacoes.setText(v.observacoes());
    }

    /** O veículo como está nos campos; quem confere as regras da SPEC 6.2 é {@link Veiculo#validar}. */
    Leitura ler() {
        List<String> erros = new ArrayList<>();
        Integer fabricacao = inteiro(anoFabricacao, "O ano de fabricação", erros);
        Integer doModelo = inteiro(anoModelo, "O ano do modelo", erros);
        Double uso = numero(usoAcumulado, "O uso acumulado", erros);
        Double peso = numero(pesoKg, "O peso", erros);
        if (!erros.isEmpty()) {
            return new Leitura(null, erros);
        }
        Veiculo veiculo = new Veiculo(tipo.getValue(), marca.getText(), modelo.getText(), versao.getText(),
                fabricacao, doModelo, placa.getText(), chassi.getText(), nomeEmbarcacao.getText(),
                numeroInscricao.getText(), numeroCasco.getText(), uso, cambio.getValue(),
                cambioControlado.isSelected(), tracao.getValue(), peso, cor.getText(), observacoes.getText());
        return new Leitura(veiculo.ajustadoAoTipo(), erros);
    }

    void focar() {
        marca.requestFocus();
    }

    /** Mostra os campos do tipo: placa, chassi e tração nos terrestres; nome, inscrição e casco nos náuticos. */
    private void atualizarTipo() {
        TipoVeiculo atual = tipo.getValue();
        boolean nautico = atual != null && atual.nautico();
        mostrar(secaoTerrestre, !nautico);
        mostrar(secaoNautica, nautico);
        rotuloUso.setText("Uso acumulado (" + (nautico ? "horas de motor" : "km") + ")");
        TipoCambio escolhido = cambio.getValue();
        List<TipoCambio> doTipo = Arrays.stream(TipoCambio.values()).filter(c -> c.nautico() == nautico).toList();
        CamposTela.configurarOpcional(cambio, doTipo, TipoCambio::rotulo);
        cambio.setValue(doTipo.contains(escolhido) ? escolhido : null);
        ItemCatalogo<Cambio> doCatalogo = caixaCambio.escolhido();
        if (doCatalogo != null && doCatalogo.dados().tipo() != null && doCatalogo.dados().tipo().nautico() != nautico) {
            caixaCambio.limpar();
        }
        atualizarCambiosDoCatalogo();
        atualizarCambioControlado();
    }

    /** "Câmbio controlado pela injeção" só para automático, automatizado e dupla embreagem (SPEC 6.2). */
    private void atualizarCambioControlado() {
        boolean pode = cambio.getValue() != null && cambio.getValue().podeSerControladoPelaInjecao();
        cambioControlado.setDisable(!pode);
        if (!pode) {
            cambioControlado.setSelected(false);
        }
    }

    private static void mostrar(Node no, boolean visivel) {
        no.setVisible(visivel);
        no.setManaged(visivel);
    }

    private static Integer inteiro(TextField campo, String nome, List<String> erros) {
        Double valor = numero(campo, nome, erros);
        if (valor == null) {
            return null;
        }
        if (valor != Math.rint(valor)) {
            erros.add(nome + ": use um número inteiro.");
            return null;
        }
        return valor.intValue();
    }

    private static Double numero(TextField campo, String nome, List<String> erros) {
        String texto = campo.getText();
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return NumeroBr.ler(texto);
        } catch (NumberFormatException erro) {
            erros.add(nome + ": " + erro.getMessage());
            return null;
        }
    }
}
