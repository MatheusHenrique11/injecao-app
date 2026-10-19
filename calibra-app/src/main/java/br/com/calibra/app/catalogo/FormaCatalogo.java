package br.com.calibra.app.catalogo;

import java.util.List;

import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/**
 * Como um tipo de catálogo aparece na tela: os campos próprios do formulário, as colunas da lista e a conversão
 * entre o texto da tela e o record do núcleo. Sem JavaFX, para dar para testar.
 *
 * @param <T> o record do núcleo do tipo
 */
public interface FormaCatalogo<T> {

    TipoCatalogo<T> tipo();

    /** Campos próprios do tipo (os comuns, como nome e fonte, ficam em {@link FormularioCatalogo}). */
    List<CampoForm> campos();

    /** Escreve os dados nos valores do formulário (para editar ou duplicar). */
    void escrever(T dados, ValoresForm valores);

    /**
     * Monta o record com os campos do tipo e a informação comum (nome, fonte e data).
     *
     * @return null se houve erro de digitação (as mensagens ficam na leitura)
     */
    T ler(LeituraForm leitura, InfoCatalogo info);

    /** Regras da peça (faixas e combinações), em português. */
    List<String> validar(T dados);

    /** Títulos das colunas próprias do tipo na lista. */
    List<String> colunas();

    /** Valores das colunas próprias, na ordem de {@link #colunas()}. */
    List<String> resumo(T dados);

    /** Valores de um item novo (ex.: fluido de teste 0,70 do bico). */
    default void novo(ValoresForm valores) {
    }

    /** Ajustes da cópia (ex.: o código do combustível, que é único, volta vazio). */
    default void duplicar(ValoresForm valores) {
    }

    /** Como um campo oculto aparece no histórico de versões (ex.: o mapa do turbo, resumido). */
    default String exibirOculto(String chave, String valor) {
        return valor;
    }

    /**
     * false quando o tipo não tem preço de referência próprio (o kit: o valor sai das linhas dele). A tela não mostra
     * o campo nem a coluna do preço.
     */
    default boolean temPreco() {
        return true;
    }

    /** false quando o nome sai dos próprios dados ({@link #nomeDerivado}): a tela não mostra o campo Nome. */
    default boolean nomeDigitado() {
        return true;
    }

    /** O nome do item tirado dos dados (injeção: marca e modelo); só vale com {@link #nomeDigitado()} false. */
    default String nomeDerivado(T dados) {
        throw new UnsupportedOperationException("O nome deste tipo é digitado.");
    }
}
