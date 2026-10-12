package br.com.calibra.app;

/** O que as telas pedem para a janela principal: trocar de tela. */
interface Navegacao {

    void abrirProjeto(long projetoId);

    void novoProjeto();

    /** Volta para a lista de projetos, atualizada. */
    void voltarParaProjetos();
}
