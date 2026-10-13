package br.com.calibra.app;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import br.com.calibra.app.casos.CasosCatalogo;
import br.com.calibra.app.catalogo.FormasCatalogo;
import br.com.calibra.app.catalogo.FormularioCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/** Catálogos (SPEC 8.5): uma aba por tipo, cada uma com busca, lista e botões. */
public class CatalogosController {

    @FXML private TabPane abas;

    private final List<PainelCatalogo<?>> paineis = new ArrayList<>();

    void iniciar(CasosCatalogo casos) {
        for (FormularioCatalogo<?> formulario : FormasCatalogo.todos()) {
            PainelCatalogo<?> painel = painel(casos, formulario);
            paineis.add(painel);
            Tab aba = new Tab(formulario.tipo().plural(), painel);
            aba.setId("aba" + formulario.tipo().codigo());
            abas.getTabs().add(aba);
        }
    }

    /** Lê as listas de novo (ao voltar para a tela). */
    void atualizar() {
        paineis.forEach(PainelCatalogo::atualizar);
    }

    /** Abre a aba do tipo. */
    void mostrar(TipoCatalogo<?> tipo) {
        for (int i = 0; i < paineis.size(); i++) {
            if (paineis.get(i).tipo() == tipo) {
                abas.getSelectionModel().select(i);
            }
        }
    }

    @SuppressWarnings("unchecked")
    <T> PainelCatalogo<T> painel(TipoCatalogo<T> tipo) {
        // Cada painel foi montado com o formulário do próprio tipo.
        return (PainelCatalogo<T>) paineis.stream().filter(p -> p.tipo() == tipo).findFirst().orElseThrow();
    }

    private static <T> PainelCatalogo<T> painel(CasosCatalogo casos, FormularioCatalogo<T> formulario) {
        return new PainelCatalogo<>(casos, formulario);
    }
}
