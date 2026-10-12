package br.com.calibra.dados.projeto;

import br.com.calibra.core.projeto.StatusProjeto;

/**
 * Filtro da lista de projetos (SPEC 8.1): texto em cliente, placa ou embarcação e motor; status; arquivados.
 *
 * @param termo palavras procuradas, sem diferença de acento; vazio = todos
 * @param status só este status; null = todos
 * @param incluirArquivados mostrar também os projetos arquivados
 */
public record FiltroProjetos(String termo, StatusProjeto status, boolean incluirArquivados) {

    public static FiltroProjetos todos() {
        return new FiltroProjetos("", null, false);
    }
}
