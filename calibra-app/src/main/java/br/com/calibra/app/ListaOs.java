package br.com.calibra.app;

import java.time.Clock;
import java.time.LocalDate;
import java.util.function.LongConsumer;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import br.com.calibra.dados.oficina.ResumoOs;

/** As listas de OS do Painel e da Oficina: as mesmas colunas, escritas do mesmo jeito nas duas telas. */
final class ListaOs {

    /** As colunas que uma lista de OS pode ter, com o título e a largura (a lista inteira cabe em 1.150 px). */
    enum Coluna {
        NUMERO("OS", 115),
        CLIENTE("Cliente", 180),
        VEICULO("Veículo", 210),
        SERVICO("Serviço pedido", 200),
        STATUS("Status", 160),
        CHEGADA("Chegada", 130),
        PREVISAO("Previsão de entrega", 155);

        private final String titulo;
        private final double largura;

        Coluna(String titulo, double largura) {
            this.titulo = titulo;
            this.largura = largura;
        }
    }

    private ListaOs() {
    }

    /**
     * Põe as colunas na tabela, na ordem pedida. As larguras valem como proporção: as colunas ocupam a largura da
     * tabela, sem barra de rolagem para o lado.
     */
    static void montar(TableView<ResumoOs> tabela, Clock relogio, Coluna... colunas) {
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        for (Coluna coluna : colunas) {
            TableColumn<ResumoOs, String> nova = new TableColumn<>(coluna.titulo);
            nova.setPrefWidth(coluna.largura);
            nova.setCellValueFactory(celula -> new ReadOnlyStringWrapper(
                    texto(coluna, celula.getValue(), LocalDate.now(relogio))));
            tabela.getColumns().add(nova);
        }
    }

    /** O duplo clique ou o Enter numa linha abrem a OS. */
    static void aoAbrir(TableView<ResumoOs> tabela, LongConsumer abrir) {
        tabela.setRowFactory(lista -> {
            TableRow<ResumoOs> linha = new TableRow<>();
            linha.setOnMouseClicked(evento -> {
                if (evento.getButton() == MouseButton.PRIMARY && evento.getClickCount() == 2 && !linha.isEmpty()) {
                    abrir.accept(linha.getItem().osId());
                }
            });
            return linha;
        });
        tabela.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ENTER && tabela.getSelectionModel().getSelectedItem() != null) {
                abrir.accept(tabela.getSelectionModel().getSelectedItem().osId());
            }
        });
    }

    /** O texto da coluna para a OS: o veículo com a placa (ou o nome da embarcação) e a previsão com "atrasada". */
    static String texto(Coluna coluna, ResumoOs os, LocalDate hoje) {
        return switch (coluna) {
            case NUMERO -> os.numero().texto();
            case CLIENTE -> os.cliente();
            case VEICULO -> os.identificacao() == null || os.identificacao().isBlank() ? os.veiculo()
                    : os.veiculo() + " · " + os.identificacao();
            case SERVICO -> os.servicoPedido();
            case STATUS -> os.status().rotulo();
            case CHEGADA -> os.entradaEm() == null ? "Ainda não chegou" : Datas.diaHora(os.entradaEm());
            case PREVISAO -> os.atrasada(hoje) ? Datas.dia(os.previsaoEntrega()) + " · atrasada"
                    : Datas.dia(os.previsaoEntrega());
        };
    }
}
