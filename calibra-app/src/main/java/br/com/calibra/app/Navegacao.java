package br.com.calibra.app;

import br.com.calibra.core.oficina.StatusOs;

/** O que as telas pedem para a janela principal: trocar de tela. */
interface Navegacao {

    void abrirProjeto(long projetoId);

    void novoProjeto();

    /** Volta para a lista de projetos, atualizada. */
    void voltarParaProjetos();

    /** Volta para a lista da Oficina, atualizada, com a busca e o filtro que estavam nela. */
    void voltarParaOficina();

    /** A lista da Oficina só com as OS do status (o cartão do Painel); null = as em andamento. */
    void abrirOficina(StatusOs status);

    /** A Nova OS, na área da Oficina. */
    void novaOs();
}
