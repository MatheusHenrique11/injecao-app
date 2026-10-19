package br.com.calibra.app;

import java.util.List;

/**
 * Um campo do formulário do catálogo que guarda linhas, como texto: a tabela de números ({@link EditorTabela}) e as
 * linhas do kit ({@link EditorLinhasKit}).
 */
interface EditorDeLinhas {

    /** As linhas como estão na tela. */
    List<List<String>> linhas();

    /** Troca as linhas pelas dos valores. */
    void preencher(List<List<String>> valores);
}
