package br.com.calibra.app;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import br.com.calibra.app.ListaOs.Coluna;
import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.dados.oficina.ResumoOs;

/**
 * Painel (SPEC 8.8), a tela inicial: quantas OS em andamento há em cada status (o cartão abre a Oficina com esse
 * filtro), os veículos que estão na oficina, as entregas previstas até hoje (as atrasadas junto) e o último backup.
 */
public class PainelController {

    /** Cartão de status sem nenhuma OS: o número fica cinza. */
    private static final PseudoClass VAZIO = PseudoClass.getPseudoClass("vazio");

    @FXML private Label hoje;
    @FXML private FlowPane cartoes;
    @FXML private Label tituloNaOficina;
    @FXML private TableView<ResumoOs> naOficina;
    @FXML private Label tituloEntregas;
    @FXML private TableView<ResumoOs> entregas;
    @FXML private Label backup;

    private final Map<StatusOs, Button> botoes = new EnumMap<>(StatusOs.class);
    private final Map<StatusOs, Label> numeros = new EnumMap<>(StatusOs.class);
    private CasosOficina casos;
    private Navegacao navegacao;
    private Clock relogio;
    private Supplier<String> ultimoBackup;

    @FXML
    private void initialize() {
        for (StatusOs status : StatusOs.values()) {
            if (status.finalizada()) {
                continue;
            }
            Label numero = new Label("0");
            numero.getStyleClass().add("cartao-numero");
            Label rotulo = new Label(rotulo(status));
            rotulo.getStyleClass().add("cartao-rotulo");
            Button cartao = new Button(null, new VBox(2.0, numero, rotulo));
            cartao.setId("cartao_" + status.name());
            cartao.getStyleClass().add("cartao");
            cartao.setOnAction(evento -> navegacao.abrirOficina(status));
            cartoes.getChildren().add(cartao);
            botoes.put(status, cartao);
            numeros.put(status, numero);
        }
    }

    /** @param ultimoBackup o texto do último backup, o mesmo da barra de status */
    void iniciar(CasosOficina casos, Navegacao navegacao, Clock relogio, Supplier<String> ultimoBackup) {
        this.casos = casos;
        this.navegacao = navegacao;
        this.relogio = relogio;
        this.ultimoBackup = ultimoBackup;
        ListaOs.montar(naOficina, relogio, Coluna.NUMERO, Coluna.CLIENTE, Coluna.VEICULO, Coluna.SERVICO,
                Coluna.STATUS, Coluna.CHEGADA, Coluna.PREVISAO);
        ListaOs.montar(entregas, relogio, Coluna.NUMERO, Coluna.CLIENTE, Coluna.VEICULO, Coluna.SERVICO,
                Coluna.STATUS, Coluna.PREVISAO);
        atualizar();
    }

    /** Lê tudo de novo do banco (ao mostrar o Painel). */
    void atualizar() {
        if (casos == null) {
            return;
        }
        hoje.setText(Datas.porExtenso(LocalDate.now(relogio)));
        CasosOficina.Painel painel = casos.painel();
        botoes.forEach((status, cartao) -> {
            int quantas = painel.porStatus().getOrDefault(status, 0);
            numeros.get(status).setText(Integer.toString(quantas));
            cartao.pseudoClassStateChanged(VAZIO, quantas == 0);
            cartao.setAccessibleText(rotulo(status) + ": " + quantas);
        });
        naOficina.getItems().setAll(painel.naOficina());
        tituloNaOficina.setText("Na oficina agora (" + painel.naOficina().size() + ")");
        entregas.getItems().setAll(painel.entregas());
        tituloEntregas.setText("Entregas previstas até hoje (" + painel.entregas().size() + ")");
        atualizarBackup();
    }

    @FXML
    private void novaOs() {
        navegacao.novaOs();
    }

    /** O texto do último backup (depois de um backup em segundo plano). */
    void atualizarBackup() {
        if (ultimoBackup != null) {
            backup.setText(ultimoBackup.get());
        }
    }

    /** O nome do cartão: o status no plural, como se fala na oficina. */
    static String rotulo(StatusOs status) {
        return switch (status) {
            case ABERTA -> "Abertas";
            case AGUARDANDO_APROVACAO -> "Aguardando aprovação";
            case APROVADA -> "Aprovadas, na fila";
            case EM_EXECUCAO -> "Em execução";
            case AGUARDANDO_PECAS -> "Aguardando peças";
            case PRONTA -> "Prontas para retirar";
            case ENTREGUE -> "Entregues";
            case CANCELADA -> "Canceladas";
        };
    }
}
