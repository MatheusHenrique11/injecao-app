package br.com.calibra.app;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import br.com.calibra.app.casos.CasosOficina;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.dados.ErroDados;
import br.com.calibra.dados.oficina.MecanicoSalvo;

/**
 * A escolha do mecânico (opcional), com o cadastro rápido "Novo mecânico…" (só o nome), na Nova OS e na aba Entrada
 * da OS. A lista completa (renomear, ativar e desativar) fica em Configurações, no bloco 3.3.
 */
final class CaixaMecanico {

    private final ComboBox<MecanicoSalvo> caixa;
    private final CasosOficina casos;

    CaixaMecanico(ComboBox<MecanicoSalvo> caixa, CasosOficina casos) {
        this.caixa = caixa;
        this.casos = casos;
        carregar(null);
    }

    /**
     * Os mecânicos ativos e, se for o caso, o da OS mesmo que esteja inativo (salvar a OS não o tira dela).
     *
     * @param escolhido o que já vem escolhido; null = nenhum
     */
    void carregar(Long escolhido) {
        List<MecanicoSalvo> lista = casos.mecanicos(true).stream()
                .filter(mecanico -> mecanico.ativo() || Objects.equals(mecanico.id(), escolhido)).toList();
        CamposTela.configurarOpcional(caixa, lista, mecanico -> mecanico.ativo() ? mecanico.nome()
                : mecanico.nome() + " (inativo)", "(nenhum)");
        caixa.setValue(lista.stream().filter(mecanico -> Objects.equals(mecanico.id(), escolhido)).findFirst()
                .orElse(null));
    }

    /** O mecânico escolhido; null = nenhum. */
    Long escolhido() {
        return caixa.getValue() == null ? null : caixa.getValue().id();
    }

    /** Pergunta o nome e cadastra; o novo já fica escolhido. Os erros vão para {@code aoErrar}. */
    void novo(Window dono, Consumer<List<String>> aoErrar) {
        TextInputDialog pergunta = new TextInputDialog();
        if (dono != null) {
            pergunta.initOwner(dono);
        }
        pergunta.setTitle("Novo mecânico");
        pergunta.setHeaderText("Cadastrar mecânico");
        pergunta.setContentText("Nome:");
        pergunta.showAndWait().map(String::strip).filter(nome -> !nome.isEmpty()).ifPresent(nome -> {
            try {
                carregar(casos.novoMecanico(nome));
            } catch (EntradaInvalidaException erro) {
                aoErrar.accept(erro.erros());
            } catch (ErroDados erro) {
                aoErrar.accept(List.of(erro.getMessage()));
            }
        });
    }
}
