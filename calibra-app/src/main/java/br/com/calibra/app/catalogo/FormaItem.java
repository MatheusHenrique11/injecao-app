package br.com.calibra.app.catalogo;

import java.util.List;

import br.com.calibra.core.orcamento.ItemMaterial;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Item de material para o orçamento (Fase 3B, SPEC 6.6.4): cabo, fusível, relé, a ECU... O nome e a unidade; o preço,
 * quando há, é o preço de referência comum a todo item. Sem saldo nem estoque.
 */
final class FormaItem implements FormaCatalogo<ItemMaterial> {

    static final String UNIDADE = "unidade";

    @Override
    public TipoCatalogo<ItemMaterial> tipo() {
        return TipoCatalogo.ITEM;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(CampoForm.texto(UNIDADE, "Unidade", true)
                .comDica("Como o item se conta no orçamento: un, m, par, kit..."));
    }

    @Override
    public void escrever(ItemMaterial item, ValoresForm valores) {
        valores.texto(UNIDADE, item.unidade());
    }

    @Override
    public ItemMaterial ler(LeituraForm leitura, InfoCatalogo info) {
        String unidade = leitura.textoObrigatorio(UNIDADE);
        if (leitura.temErros()) {
            return null;
        }
        return new ItemMaterial(info.nome(), unidade, info.fonte(), info.data());
    }

    @Override
    public List<String> validar(ItemMaterial item) {
        return item.validar();
    }

    @Override
    public List<String> colunas() {
        return List.of("Unidade");
    }

    @Override
    public List<String> resumo(ItemMaterial item) {
        return List.of(item.unidade());
    }

    @Override
    public void novo(ValoresForm valores) {
        valores.texto(FormularioCatalogo.FONTE, FormaServico.FONTE_PADRAO).texto(UNIDADE, "un");
    }
}
