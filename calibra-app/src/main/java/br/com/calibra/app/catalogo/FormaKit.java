package br.com.calibra.app.catalogo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.orcamento.Kit;
import br.com.calibra.core.orcamento.Kit.TipoLinha;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Kit de itens e serviços do catálogo (Fase 3B, SPEC 6.6.4); com serviço, é um pacote (ex.: "chicote + instalação").
 * Cada linha aponta para o catálogo, com a quantidade. O kit não tem preço próprio: no orçamento, ele se expande nas
 * linhas, com o preço de agora de cada uma.
 *
 * <p>Nos valores do formulário, cada linha é uma lista de texto: o tipo (ITEM ou SERVICO), o id no catálogo, o nome e
 * a quantidade ({@link #linha}).
 */
public final class FormaKit implements FormaCatalogo<Kit> {

    public static final String LINHAS = "linhas";

    private static final int TIPO = 0;
    private static final int ID = 1;
    private static final int NOME = 2;
    private static final int QUANTIDADE = 3;

    FormaKit() {
    }

    @Override
    public TipoCatalogo<Kit> tipo() {
        return TipoCatalogo.KIT;
    }

    @Override
    public List<CampoForm> campos() {
        return List.of(CampoForm.linhas(LINHAS, "Linhas do kit")
                .comDica("Cada linha é um item ou um serviço do catálogo, com a quantidade. No orçamento, o kit vira "
                        + "estas linhas, com o preço de agora de cada uma, e dá para mudar ou apagar cada linha lá."));
    }

    @Override
    public void escrever(Kit kit, ValoresForm valores) {
        valores.tabela(LINHAS, kit.linhas().stream()
                .map(linha -> linha(linha.tipo(), linha.itemId(), linha.nome(), quantidade(linha.quantidade())))
                .toList());
    }

    @Override
    public Kit ler(LeituraForm leitura, InfoCatalogo info) {
        List<Kit.Linha> linhas = new ArrayList<>();
        int numero = 0;
        for (List<String> celulas : leitura.linhasDeTexto(LINHAS)) {
            numero++;
            String nome = celula(celulas, NOME);
            String onde = "Linha " + numero + (nome.isEmpty() ? "" : " (" + nome + ")") + ": ";
            TipoLinha tipo;
            long id;
            try {
                tipo = TipoLinha.valueOf(celula(celulas, TIPO));
                id = Long.parseLong(celula(celulas, ID));
            } catch (IllegalArgumentException erro) {
                leitura.erro(onde + "escolha o item ou o serviço no catálogo.");
                continue;
            }
            BigDecimal quantidade = null;
            String texto = celula(celulas, QUANTIDADE);
            if (texto.isEmpty()) {
                leitura.erro(onde + "informe a quantidade.");
            } else {
                try {
                    quantidade = NumeroBr.lerDecimal(texto);
                } catch (NumberFormatException erro) {
                    leitura.erro(onde + "quantidade: " + erro.getMessage());
                }
            }
            linhas.add(new Kit.Linha(tipo, id, nome, quantidade));
        }
        if (leitura.temErros()) {
            return null;
        }
        return new Kit(info.nome(), linhas, info.fonte(), info.data());
    }

    @Override
    public List<String> validar(Kit kit) {
        return kit.validar();
    }

    @Override
    public List<String> colunas() {
        return List.of("Linhas", "Pacote");
    }

    @Override
    public List<String> resumo(Kit kit) {
        long itens = kit.linhas().stream().filter(linha -> linha.tipo() == TipoLinha.ITEM).count();
        long servicos = kit.linhas().size() - itens;
        List<String> partes = new ArrayList<>();
        if (itens > 0) {
            partes.add(itens + (itens == 1 ? " item" : " itens"));
        }
        if (servicos > 0) {
            partes.add(servicos + (servicos == 1 ? " serviço" : " serviços"));
        }
        return List.of(partes.isEmpty() ? "—" : String.join(", ", partes), kit.pacote() ? "sim" : "—");
    }

    @Override
    public void novo(ValoresForm valores) {
        valores.texto(FormularioCatalogo.FONTE, FormaServico.FONTE_PADRAO);
    }

    /** O kit não tem preço próprio: o valor sai das linhas, no orçamento. */
    @Override
    public boolean temPreco() {
        return false;
    }

    // ===================== Linhas nos valores do formulário =====================

    /** Uma linha nos valores do formulário: o tipo, o id no catálogo, o nome e a quantidade (como na tela). */
    public static List<String> linha(TipoLinha tipo, long id, String nome, String quantidade) {
        return List.of(tipo.name(), Long.toString(id), nome == null ? "" : nome, quantidade == null ? "" : quantidade);
    }

    public static TipoLinha tipo(List<String> linha) {
        return TipoLinha.valueOf(celula(linha, TIPO));
    }

    public static long id(List<String> linha) {
        return Long.parseLong(celula(linha, ID));
    }

    public static String nome(List<String> linha) {
        return celula(linha, NOME);
    }

    public static String quantidade(List<String> linha) {
        return celula(linha, QUANTIDADE);
    }

    /** "2" ou "2,5", como a quantidade aparece no campo. */
    static String quantidade(BigDecimal quantidade) {
        return quantidade == null ? "" : NumeroBr.formatar(quantidade.doubleValue(), 0, 3);
    }

    /** "2 × Fusível lâmina 30 A (item)", para o histórico de versões. */
    static String exibir(List<String> linha) {
        String tipo;
        try {
            tipo = " (" + tipo(linha).rotulo() + ")";
        } catch (IllegalArgumentException erro) {
            tipo = "";
        }
        String quantidade = quantidade(linha);
        return (quantidade.isEmpty() ? "" : quantidade + " × ") + nome(linha) + tipo;
    }

    private static String celula(List<String> linha, int indice) {
        String valor = indice < linha.size() ? linha.get(indice) : null;
        return valor == null ? "" : valor.strip();
    }
}
