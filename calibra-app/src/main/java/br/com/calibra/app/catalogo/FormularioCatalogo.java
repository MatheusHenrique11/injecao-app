package br.com.calibra.app.catalogo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.catalogo.VersaoCatalogo;

/**
 * O formulário completo de um item de catálogo: o nome, os campos do tipo e, no fim, o que todo item tem (fonte,
 * data, "a verificar", preço de referência e observações). Lê e escreve sem JavaFX.
 *
 * @param <T> o record do núcleo do tipo
 */
public final class FormularioCatalogo<T> {

    public static final String NOME = "info.nome";
    public static final String FONTE = "info.fonte";
    public static final String DATA = "info.data";
    public static final String A_VERIFICAR = "info.aVerificar";
    public static final String PRECO = "info.preco";
    public static final String OBSERVACOES = "info.observacoes";

    /** Valor que não foi informado, no histórico. */
    static final String VAZIO = "—";

    /** Resultado da leitura: os dados e a informação comum (null se houve erro) e os erros. */
    public record Leitura<T>(T dados, InfoCatalogo info, List<String> erros) {

        public Leitura {
            erros = List.copyOf(erros);
        }

        public boolean ok() {
            return erros.isEmpty();
        }
    }

    private final FormaCatalogo<T> forma;

    FormularioCatalogo(FormaCatalogo<T> forma) {
        this.forma = Objects.requireNonNull(forma, "forma");
    }

    public TipoCatalogo<T> tipo() {
        return forma.tipo();
    }

    /** Todos os campos, na ordem da tela. */
    public List<CampoForm> campos() {
        List<CampoForm> campos = new ArrayList<>();
        if (forma.nomeDigitado()) {
            campos.add(CampoForm.texto(NOME, "Nome", true));
        }
        campos.addAll(forma.campos());
        campos.add(CampoForm.texto(FONTE, "Fonte", true)
                .comDica("De onde vieram os dados: manual, site do fabricante, medição na bancada..."));
        campos.add(CampoForm.data(DATA, "Data da informação"));
        campos.add(CampoForm.marcar(A_VERIFICAR, "A verificar (dado ainda não confirmado)"));
        campos.add(CampoForm.texto(PRECO, "Preço de referência (R$)", false).comDica("Opcional; vai para o orçamento."));
        campos.add(CampoForm.area(OBSERVACOES, "Observações"));
        return campos;
    }

    /** Valores de um item novo. */
    public ValoresForm novo() {
        ValoresForm valores = new ValoresForm();
        forma.novo(valores);
        return valores;
    }

    /** Valores de um item gravado, para editar. */
    public ValoresForm escrever(T dados, InfoCatalogo info) {
        ValoresForm valores = new ValoresForm()
                .texto(NOME, info.nome())
                .texto(FONTE, info.fonte())
                .texto(DATA, TextoForm.data(info.data()))
                .texto(A_VERIFICAR, String.valueOf(info.aVerificar()))
                .texto(PRECO, info.precoReferencia() == null ? "" : info.precoReferencia().formatarSemSimbolo())
                .texto(OBSERVACOES, info.observacoes());
        forma.escrever(dados, valores);
        return valores;
    }

    /** Valores de uma cópia: "(cópia)" no nome e os ajustes do tipo (ex.: código do combustível vazio). */
    public ValoresForm duplicar(T dados, InfoCatalogo info) {
        ValoresForm valores = escrever(dados, info);
        if (forma.nomeDigitado()) {
            valores.texto(NOME, info.nome() + " (cópia)");
        }
        forma.duplicar(valores);
        return valores;
    }

    /** Lê a tela: primeiro os erros de digitação, depois (se não houver) as regras da peça. */
    public Leitura<T> ler(ValoresForm valores) {
        List<String> erros = new ArrayList<>();
        LeituraForm leitura = new LeituraForm(valores, campos(), erros);
        String nome = forma.nomeDigitado() ? leitura.textoObrigatorio(NOME) : "";
        String fonte = leitura.textoObrigatorio(FONTE);
        LocalDate data = leitura.data(DATA);
        boolean aVerificar = leitura.marcado(A_VERIFICAR);
        Dinheiro preco = leitura.dinheiro(PRECO);
        if (preco != null && preco.centavos() < 0) {
            leitura.erro("O preço de referência não pode ser negativo.");
        }
        String observacoes = leitura.texto(OBSERVACOES);
        InfoCatalogo info = new InfoCatalogo(nome, fonte, data, aVerificar, preco, observacoes);
        T dados = forma.ler(leitura, info);
        if (dados == null || !erros.isEmpty()) {
            return new Leitura<>(null, null, erros);
        }
        if (!forma.nomeDigitado()) {
            info = new InfoCatalogo(forma.nomeDerivado(dados), fonte, data, aVerificar, preco, observacoes);
        }
        erros.addAll(info.validar());
        erros.addAll(forma.validar(dados));
        return erros.isEmpty() ? new Leitura<>(dados, info, erros) : new Leitura<>(null, null, erros);
    }

    /** Regras do item já montado (as mesmas da tela), para os casos de uso conferirem antes de gravar. */
    public List<String> validar(T dados, InfoCatalogo info) {
        List<String> erros = new ArrayList<>(info.validar());
        erros.addAll(forma.validar(dados));
        return erros;
    }

    /** Títulos das colunas próprias do tipo na lista. */
    public List<String> colunas() {
        return forma.colunas();
    }

    public List<String> resumo(T dados) {
        return forma.resumo(dados);
    }

    // ===================== Histórico de versões =====================

    /**
     * Uma linha da comparação entre versões: o campo e o valor em cada versão (da mais nova para a mais antiga).
     *
     * @param mudou em cada versão, se o valor é diferente do da versão anterior a ela
     */
    public record LinhaHistorico(String campo, List<String> valores, List<Boolean> mudou) {

        public LinhaHistorico {
            valores = List.copyOf(valores);
            mudou = List.copyOf(mudou);
        }
    }

    /**
     * A comparação campo a campo entre as versões (da mais nova para a mais antiga). Campos vazios em todas as
     * versões ficam de fora; o campo que não se aplica numa versão (ex.: AFR fixo numa gasolina) aparece em branco.
     */
    public List<LinhaHistorico> historico(List<VersaoCatalogo<T>> versoes) {
        List<ValoresForm> valores = versoes.stream().map(v -> escrever(v.dados(), v.info())).toList();
        List<LinhaHistorico> linhas = new ArrayList<>();
        for (CampoForm campo : campos()) {
            List<String> textos = valores.stream()
                    .map(v -> campo.mostrarSe() == null || campo.mostrarSe().atende(v) ? exibir(campo, v) : "")
                    .toList();
            if (textos.stream().allMatch(texto -> texto.isEmpty() || texto.equals(VAZIO))) {
                continue;
            }
            List<Boolean> mudou = new ArrayList<>();
            for (int i = 0; i < textos.size(); i++) {
                mudou.add(i + 1 < textos.size() && !textos.get(i).equals(textos.get(i + 1)));
            }
            linhas.add(new LinhaHistorico(campo.rotulo(), textos, mudou));
        }
        return linhas;
    }

    /**
     * O valor de um campo como aparece no histórico: o rótulo da escolha, "sim"/"não", as linhas da tabela e, no campo
     * oculto, o resumo que o tipo der.
     */
    String exibir(CampoForm campo, ValoresForm valores) {
        String texto = valores.texto(campo.chave()).strip();
        return switch (campo.tipo()) {
            case OCULTO -> texto.isEmpty() ? VAZIO : forma.exibirOculto(campo.chave(), texto);
            case MARCAR -> Boolean.parseBoolean(texto) ? "sim" : "não";
            case ESCOLHA -> campo.opcoes().stream().filter(opcao -> opcao.valor().equals(texto))
                    .map(CampoForm.Opcao::rotulo).findFirst().orElse(texto.isEmpty() ? VAZIO : texto);
            case TABELA -> {
                List<String> linhas = valores.tabela(campo.chave()).stream()
                        .map(linha -> String.join(" · ", linha.stream().map(c -> c.isBlank() ? VAZIO : c).toList()))
                        .toList();
                yield linhas.isEmpty() ? VAZIO : String.join("\n", linhas);
            }
            default -> texto.isEmpty() ? VAZIO : texto;
        };
    }
}
