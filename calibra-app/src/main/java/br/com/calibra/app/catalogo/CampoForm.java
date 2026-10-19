package br.com.calibra.app.catalogo;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Um campo do formulário de um tipo de catálogo: a chave, o rótulo da tela, o tipo de entrada e, nas escolhas
 * e tabelas, as opções e as colunas. A janela de edição monta a tela a partir desta lista.
 *
 * @param obrigatorio numa escolha opcional, aparece "(não informado)"
 * @param dica texto pequeno embaixo do campo; vazio se não houver
 * @param fixoNaEdicao só pode ser digitado no item novo (ex.: o código do combustível)
 * @param mostrarSe o campo só aparece quando outra escolha tem um destes valores; null = sempre aparece
 */
public record CampoForm(String chave, String rotulo, Tipo tipo, boolean obrigatorio, List<Opcao> opcoes,
        List<String> colunas, String dica, boolean fixoNaEdicao, Condicao mostrarSe) {

    /**
     * Como o campo é digitado. OCULTO não aparece no formulário: só leva o valor junto (ex.: o mapa do turbo). LINHAS é
     * uma lista de itens e serviços do catálogo, cada um com a quantidade (as linhas do kit, Fase 3B).
     */
    public enum Tipo {
        TEXTO, NUMERO, INTEIRO, ESCOLHA, MARCAR, DATA, TABELA, AREA, OCULTO, LINHAS
    }

    /** Uma opção de escolha: o valor gravado (nome da constante) e o rótulo da tela. */
    public record Opcao(String valor, String rotulo) {
    }

    /** O campo aparece quando a escolha {@code chave} vale um destes valores. */
    public record Condicao(String chave, List<String> valores) {

        public Condicao {
            valores = List.copyOf(valores);
        }

        public boolean atende(ValoresForm valoresForm) {
            return valores.contains(valoresForm.texto(chave));
        }
    }

    public CampoForm {
        opcoes = List.copyOf(opcoes);
        colunas = List.copyOf(colunas);
        dica = dica == null ? "" : dica;
    }

    private CampoForm(String chave, String rotulo, Tipo tipo, boolean obrigatorio) {
        this(chave, rotulo, tipo, obrigatorio, List.of(), List.of(), "", false, null);
    }

    public static CampoForm texto(String chave, String rotulo, boolean obrigatorio) {
        return new CampoForm(chave, rotulo, Tipo.TEXTO, obrigatorio);
    }

    public static CampoForm area(String chave, String rotulo) {
        return new CampoForm(chave, rotulo, Tipo.AREA, false);
    }

    public static CampoForm numero(String chave, String rotulo, boolean obrigatorio) {
        return new CampoForm(chave, rotulo, Tipo.NUMERO, obrigatorio);
    }

    public static CampoForm inteiro(String chave, String rotulo, boolean obrigatorio) {
        return new CampoForm(chave, rotulo, Tipo.INTEIRO, obrigatorio);
    }

    public static CampoForm marcar(String chave, String rotulo) {
        return new CampoForm(chave, rotulo, Tipo.MARCAR, false);
    }

    public static CampoForm data(String chave, String rotulo) {
        return new CampoForm(chave, rotulo, Tipo.DATA, false);
    }

    /** Valor que vai junto sem aparecer no formulário; o rótulo aparece no histórico de versões. */
    public static CampoForm oculto(String chave, String rotulo) {
        return new CampoForm(chave, rotulo, Tipo.OCULTO, false);
    }

    /** Escolha entre as constantes de um enum, mostradas pelo rótulo. */
    public static <E extends Enum<E>> CampoForm escolha(String chave, String rotulo, boolean obrigatorio, E[] valores,
            Function<E, String> rotuloDe) {
        List<Opcao> opcoes = Arrays.stream(valores).map(v -> new Opcao(v.name(), rotuloDe.apply(v))).toList();
        return new CampoForm(chave, rotulo, Tipo.ESCOLHA, obrigatorio, opcoes, List.of(), "", false, null);
    }

    /** Tabela de números (ex.: tempo morto por tensão e pressão), com uma linha por ponto. */
    public static CampoForm tabela(String chave, String rotulo, List<String> colunas) {
        return new CampoForm(chave, rotulo, Tipo.TABELA, true, List.of(), colunas, "", false, null);
    }

    /**
     * Linhas de itens e serviços do catálogo, cada uma com a quantidade (as linhas do kit). Nos valores, cada linha é
     * uma lista de texto: o tipo (ITEM ou SERVICO), o id no catálogo, o nome e a quantidade.
     */
    public static CampoForm linhas(String chave, String rotulo) {
        return new CampoForm(chave, rotulo, Tipo.LINHAS, true);
    }

    public CampoForm comDica(String texto) {
        return new CampoForm(chave, rotulo, tipo, obrigatorio, opcoes, colunas, texto, fixoNaEdicao, mostrarSe);
    }

    public CampoForm travarNaEdicao() {
        return new CampoForm(chave, rotulo, tipo, obrigatorio, opcoes, colunas, dica, true, mostrarSe);
    }

    public CampoForm mostrarSe(String chaveEscolha, String... valores) {
        return new CampoForm(chave, rotulo, tipo, obrigatorio, opcoes, colunas, dica, fixoNaEdicao,
                new Condicao(chaveEscolha, List.of(valores)));
    }
}
