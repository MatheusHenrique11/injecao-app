package br.com.calibra.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.FormaPagamento;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.GarantiaDoServico;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.oficina.AnexoSalvo;

/**
 * As janelas da tela da OS: o motivo (da espera por peças, do cancelamento), a entrega, o recebimento e os dados do
 * anexo. Cada uma confere a digitação antes de fechar; as regras ficam com o caso de uso.
 */
final class JanelasOs {

    /** O que se muda num anexo. */
    record DadosAnexo(TipoAnexo tipo, String descricao, LocalDate validade, boolean pessoal) {
    }

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private JanelasOs() {
    }

    /**
     * Pergunta o motivo, por escrito.
     *
     * @param exemplo o texto de exemplo no campo vazio
     * @param obrigatorio sem o motivo, "Gravar" não fecha a janela
     * @return vazio se a pessoa desistiu
     */
    static Optional<String> motivo(Window dono, String titulo, String pergunta, String exemplo, boolean obrigatorio) {
        Dialog<String> dialogo = dialogo(dono, titulo, pergunta);
        TextArea texto = new TextArea();
        texto.setId("motivo");
        texto.setPrefRowCount(3);
        texto.setWrapText(true);
        texto.setPromptText(exemplo);
        Label erro = erro();
        dialogo.getDialogPane().setContent(new VBox(8.0, texto, erro));
        ButtonType gravar = botoes(dialogo);
        dialogo.getDialogPane().lookupButton(gravar).addEventFilter(ActionEvent.ACTION, evento -> {
            if (obrigatorio && texto.getText().isBlank()) {
                erro.setText("Escreva o motivo.");
                evento.consume();
            }
        });
        dialogo.setResultConverter(botao -> botao == gravar ? texto.getText().strip() : null);
        Platform.runLater(texto::requestFocus);
        return dialogo.showAndWait();
    }

    /**
     * A entrega: o dia e a hora, o km (ou as horas de motor) na saída e a garantia em dias. Os serviços com garantia
     * própria (do catálogo) aparecem embaixo da garantia, que vale para os outros.
     *
     * @param rotuloUso "Km" ou "Horas de motor"
     * @param garantiaPadrao a garantia de Configurações › OS, que já vem preenchida
     * @param garantias a garantia de cada serviço da revisão aprovada
     */
    static Optional<Entrega> entrega(Window dono, LocalDateTime agora, String rotuloUso, int garantiaPadrao,
            List<GarantiaDoServico> garantias) {
        Dialog<Entrega> dialogo = dialogo(dono, "Entregar o veículo", "Entrega ao cliente: quando, com quanto no painel "
                + "e com quantos dias de garantia?");
        DatePicker data = new DatePicker(agora.toLocalDate());
        Datas.emPortugues(data);
        TextField hora = new TextField(HORA.format(agora));
        hora.setPrefColumnCount(6);
        TextField uso = new TextField();
        uso.setId("entregaUso");
        uso.setPromptText("opcional");
        TextField garantia = new TextField(Integer.toString(garantiaPadrao));
        garantia.setId("entregaGarantia");
        garantia.setPromptText("0 = sem garantia");
        Label erro = erro();
        GridPane grade = grade();
        grade.addRow(0, new Label("Data"), data);
        grade.addRow(1, new Label("Hora (hh:mm)"), hora);
        grade.addRow(2, new Label(rotuloUso + " na saída"), uso);
        grade.addRow(3, new Label("Garantia (dias)"), garantia);
        String proprias = garantiasProprias(garantias);
        if (!proprias.isEmpty()) {
            Label aviso = new Label("Com garantia própria (do catálogo): " + proprias + ". Os outros serviços ficam "
                    + "com a garantia acima.");
            aviso.setId("entregaGarantiasProprias");
            aviso.getStyleClass().add("dica");
            aviso.setWrapText(true);
            aviso.setPrefWidth(440);
            aviso.setMinHeight(Region.USE_PREF_SIZE);
            grade.add(aviso, 0, 4, 2, 1);
        }
        grade.add(erro, 0, 5, 2, 1);
        dialogo.getDialogPane().setContent(grade);
        ButtonType gravar = botoes(dialogo);
        dialogo.getDialogPane().lookupButton(gravar).addEventFilter(ActionEvent.ACTION, evento -> {
            List<String> erros = new ArrayList<>();
            quando(data, hora, erros);
            numero(uso.getText(), rotuloUso + " na saída", erros);
            dias(garantia.getText(), erros);
            if (!erros.isEmpty()) {
                erro.setText(String.join("\n", erros));
                evento.consume();
            }
        });
        dialogo.setResultConverter(botao -> {
            if (botao != gravar) {
                return null;
            }
            List<String> ignorados = new ArrayList<>();
            return new Entrega(quando(data, hora, ignorados), numero(uso.getText(), "", ignorados),
                    new Garantia(dias(garantia.getText(), ignorados)));
        });
        Platform.runLater(uso::requestFocus);
        return dialogo.showAndWait();
    }

    /**
     * Um recebimento: o dia e a hora, a forma, o valor (já vem o que falta) e uma observação.
     *
     * @param falta o que falta receber; null sem orçamento aprovado
     */
    static Optional<Pagamento> recebimento(Window dono, LocalDateTime agora, Dinheiro falta) {
        Dialog<Pagamento> dialogo = dialogo(dono, "Registrar recebimento", "Recebimento do cliente: quando, como e "
                + "quanto?");
        DatePicker data = new DatePicker(agora.toLocalDate());
        Datas.emPortugues(data);
        TextField hora = new TextField(HORA.format(agora));
        hora.setPrefColumnCount(6);
        ComboBox<FormaPagamento> forma = new ComboBox<>();
        forma.setId("recebimentoForma");
        CamposTela.configurar(forma, Arrays.asList(FormaPagamento.values()), FormaPagamento::rotulo);
        TextField valor = new TextField(falta == null || falta.compareTo(Dinheiro.ZERO) <= 0 ? ""
                : falta.formatarSemSimbolo());
        valor.setId("recebimentoValor");
        valor.setPromptText("ex.: 1.234,56");
        TextField observacao = new TextField();
        observacao.setId("recebimentoObservacao");
        observacao.setPrefColumnCount(26);
        observacao.setPromptText("opcional (ex.: sinal de 50% na aprovação)");
        Label erro = erro();
        GridPane grade = grade();
        grade.addRow(0, new Label("Data"), data);
        grade.addRow(1, new Label("Hora (hh:mm)"), hora);
        grade.addRow(2, new Label("Forma de pagamento"), forma);
        grade.addRow(3, new Label("Valor (R$)"), valor);
        grade.addRow(4, new Label("Observação"), observacao);
        grade.add(erro, 0, 5, 2, 1);
        dialogo.getDialogPane().setContent(grade);
        ButtonType gravar = botoes(dialogo);
        dialogo.getDialogPane().lookupButton(gravar).addEventFilter(ActionEvent.ACTION, evento -> {
            List<String> erros = new ArrayList<>();
            quando(data, hora, erros);
            if (forma.getValue() == null) {
                erros.add("Escolha a forma de pagamento.");
            }
            dinheiro(valor.getText(), erros);
            if (!erros.isEmpty()) {
                erro.setText(String.join("\n", erros));
                evento.consume();
            }
        });
        dialogo.setResultConverter(botao -> {
            if (botao != gravar) {
                return null;
            }
            List<String> ignorados = new ArrayList<>();
            return new Pagamento(quando(data, hora, ignorados), forma.getValue(), dinheiro(valor.getText(), ignorados),
                    observacao.getText());
        });
        Platform.runLater(forma::requestFocus);
        return dialogo.showAndWait();
    }

    /** O tipo, a descrição, a validade e a marca de documento pessoal do anexo. */
    static Optional<DadosAnexo> anexo(Window dono, AnexoSalvo anexo) {
        Dialog<DadosAnexo> dialogo = dialogo(dono, "Anexo", anexo.nomeOriginal());
        ComboBox<TipoAnexo> tipo = new ComboBox<>();
        CamposTela.configurar(tipo, Arrays.asList(TipoAnexo.values()), TipoAnexo::rotulo);
        tipo.setValue(anexo.tipo());
        TextField descricao = new TextField(anexo.descricao());
        descricao.setPrefColumnCount(28);
        descricao.setPromptText("ex.: porta direita, CRLV 2026");
        DatePicker validade = new DatePicker(anexo.validade());
        Datas.emPortugues(validade);
        CheckBox pessoal = new CheckBox("Documento pessoal (sai junto se o cliente pedir para apagar os dados dele)");
        pessoal.setSelected(anexo.pessoal());
        Label erro = erro();
        GridPane grade = grade();
        grade.addRow(0, new Label("Tipo"), tipo);
        grade.addRow(1, new Label("Descrição"), descricao);
        grade.addRow(2, new Label("Validade (opcional)"), validade);
        grade.add(pessoal, 0, 3, 2, 1);
        grade.add(erro, 0, 4, 2, 1);
        dialogo.getDialogPane().setContent(grade);
        ButtonType gravar = botoes(dialogo);
        dialogo.getDialogPane().lookupButton(gravar).addEventFilter(ActionEvent.ACTION, evento -> {
            if (!validade.getEditor().getText().isBlank() && validade.getConverter()
                    .fromString(validade.getEditor().getText()) == null) {
                erro.setText("Validade: use dia/mês/ano, como 31/01/2027.");
                evento.consume();
            }
        });
        dialogo.setResultConverter(botao -> botao == gravar
                ? new DadosAnexo(tipo.getValue(), descricao.getText(), validade.getConverter()
                        .fromString(validade.getEditor().getText()), pessoal.isSelected())
                : null);
        return dialogo.showAndWait();
    }

    // ===================== Utilidades =====================

    private static <T> Dialog<T> dialogo(Window dono, String titulo, String cabecalho) {
        Dialog<T> dialogo = new Dialog<>();
        if (dono != null) {
            dialogo.initOwner(dono);
        }
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(cabecalho);
        dialogo.getDialogPane().getStylesheets().add(CalibraApp.folhaDeEstilo());
        return dialogo;
    }

    /** "Gravar" e "Cancelar"; devolve o "Gravar". */
    private static ButtonType botoes(Dialog<?> dialogo) {
        ButtonType gravar = new ButtonType("Gravar", ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().setAll(gravar, new ButtonType("Cancelar",
                ButtonBar.ButtonData.CANCEL_CLOSE));
        return gravar;
    }

    /**
     * Os serviços com garantia própria: "Reparo ou troca de alternador, 365 dias; Diagnóstico de falha elétrica, sem
     * garantia". Vazio se nenhum tiver.
     */
    static String garantiasProprias(List<GarantiaDoServico> garantias) {
        return garantias.stream().filter(GarantiaDoServico::propria)
                .map(servico -> servico.servico() + ", " + diasDeGarantia(servico.diasProprios()))
                .collect(Collectors.joining("; "));
    }

    /** "sem garantia", "1 dia" ou "365 dias". */
    static String diasDeGarantia(int dias) {
        return dias == 0 ? "sem garantia" : dias + (dias == 1 ? " dia" : " dias");
    }

    private static Label erro() {
        Label erro = new Label();
        erro.getStyleClass().add("erro");
        erro.setWrapText(true);
        return erro;
    }

    private static GridPane grade() {
        GridPane grade = new GridPane();
        grade.getStyleClass().add("grade");
        return grade;
    }

    private static LocalDateTime quando(DatePicker data, TextField hora, List<String> erros) {
        LocalTime lida = null;
        try {
            lida = LocalTime.parse(hora.getText() == null ? "" : hora.getText().strip(),
                    DateTimeFormatter.ofPattern("H:mm"));
        } catch (DateTimeParseException erro) {
            erros.add("Hora: use horas e minutos, como 14:30.");
        }
        if (data.getValue() == null) {
            erros.add("Informe a data.");
        }
        return data.getValue() == null || lida == null ? null : data.getValue().atTime(lida);
    }

    /** Número opcional (vazio = null). */
    private static Double numero(String texto, String nome, List<String> erros) {
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

    private static int dias(String texto, List<String> erros) {
        try {
            return Integer.parseInt(texto == null ? "" : texto.strip());
        } catch (NumberFormatException erro) {
            erros.add("Garantia: informe os dias, em número inteiro (0 = sem garantia).");
            return 0;
        }
    }

    private static Dinheiro dinheiro(String texto, List<String> erros) {
        try {
            return Dinheiro.ler(texto);
        } catch (NumberFormatException erro) {
            erros.add("Valor: " + erro.getMessage());
            return null;
        }
    }
}
