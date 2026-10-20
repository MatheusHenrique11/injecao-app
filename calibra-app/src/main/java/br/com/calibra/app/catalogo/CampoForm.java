package br.com.calibra.app.catalogo;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Um campo do formulário de um tipo de catálogo: a chave, o rótulo da tela, o tipo de entrada e, nas escolhas
 * e tabelas, as opções e as colunas. A janela de edição monta a tela a partir desta lista.
 *
 * @param obrigatorio numa escolha opcional, aparece "(não informado)"
 * @param dica texto pequeno embaixo do campo; vazio se não houver
 * @param fixoNaEdicao só pode ser digitado no item novo (ex.: o código do combustível)
 * @param mostrarSe o campo só aparece quando outra escolha tem um destes valores; null = sempre aparece
 * @param catalogo na referência, o tipo de catálogo de onde se escolhe o item; null nos outros campos
 */
public record CampoForm(String chave, String rotulo, Tipo tipo, boolean obrigatorio, List<Opcao> opcoes,
        List<String> colunas, String dica, boolean fixoNaEdicao, Condicao mostrarSe, TipoCatalogo<?> catalogo) {

    /**
     * Como o campo é digitado. OCULTO não aparece no formulário: só leva o valor junto (ex.: o mapa do turbo). LINHAS é
     * uma lista de itens e serviços do catálogo, cada um com a quantidade (as linhas do kit, Fase 3B). ITENS é a lista
     * dos itens de um checklist, cada um com o texto e as marcas "obrigatório" e "a verificar" (Fase 3B). REFERENCIA é
     * a escolha de um item de outro catálogo, gravado pelo id (o checklist do serviço, Fase 3B).
     */
    public enum Tipo {
        TEXTO, NUMERO, INTEIRO, ESCOLHA, MARCAR, DATA, TABELA, AREA, OCULTO, LINHAS, ITENS, REFERENCIA
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
        this(chave, rotulo, tipo, obrigatorio, List.of(), List.of(), "", false, null, null);
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
        return new CampoForm(chave, rotulo, Tipo.ESCOLHA, obrigatorio, opcoes, List.of(), "", false, null, null);
    }

    /** Tabela de números (ex.: tempo morto por tensão e pressão), com uma linha por ponto. */
    public static CampoForm tabela(String chave, String rotulo, List<String> colunas) {
        return new CampoForm(chave, rotulo, Tipo.TABELA, true, List.of(), colunas, "", false, null, null);
    }

    /**
     * Linhas de itens e serviços do catálogo, cada uma com a quantidade (as linhas do kit). Nos valores, cada linha é
     * uma lista de texto: o tipo (ITEM ou SERVICO), o id no catálogo, o nome e a quantidade.
     */
    public static CampoForm linhas(String chave, String rotulo) {
        return new CampoForm(chave, rotulo, Tipo.LINHAS, true);
    }

    /**
     * Os itens de um checklist, na ordem em que se confere. Nos valores, cada item é uma lista de texto: o texto,
     * "obrigatório" e "a verificar" ("true" ou "false").
     */
    public static CampoForm itens(String chave, String rotulo) {
        return new CampoForm(chave, rotulo, Tipo.ITENS, true);
    }

    /**
     * A escolha opcional de um item de outro catálogo (ex.: o checklist do serviço). Nos valores, o id do item, ou
     * vazio para nenhum.
     */
    public static CampoForm referencia(String chave, String rotulo, TipoCatalogo<?> catalogo) {
        return new CampoForm(chave, rotulo, Tipo.REFERENCIA, false, List.of(), List.of(), "", false, null,
                Objects.requireNonNull(catalogo, "catalogo"));
    }

    public CampoForm comDica(String texto) {
        return new CampoForm(chave, rotulo, tipo, obrigatorio, opcoes, colunas, texto, fixoNaEdicao, mostrarSe,
                catalogo);
    }

    public CampoForm travarNaEdicao() {
        return new CampoForm(chave, rotulo, tipo, obrigatorio, opcoes, colunas, dica, true, mostrarSe, catalogo);
    }

    public CampoForm mostrarSe(String chaveEscolha, String... valores) {
        return new CampoForm(chave, rotulo, tipo, obrigatorio, opcoes, colunas, dica, fixoNaEdicao,
                new Condicao(chaveEscolha, List.of(valores)), catalogo);
    }
}
