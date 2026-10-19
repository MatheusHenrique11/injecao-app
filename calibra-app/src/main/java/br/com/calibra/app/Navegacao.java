package br.com.calibra.app;

import br.com.calibra.core.oficina.StatusOs;

/** O que as telas pedem para a janela principal: trocar de tela. */
interface Navegacao {

    void abrirProjeto(long projetoId);

    void novoProjeto();

    /** O assistente de projeto para a OS: começa no motor, e o projeto nasce ligado a ela. */
    void novoProjetoDaOs(long osId);

    /** Volta para a lista de projetos, atualizada. */
    void voltarParaProjetos();

    /** Volta para a lista da Oficina, atualizada, com a busca e o filtro que estavam nela. */
    void voltarParaOficina();

    /** A lista da Oficina só com as OS do status (o cartão do Painel); null = as em andamento. */
    void abrirOficina(StatusOs status);

    /** A Nova OS, na área da Oficina; a que está sendo preenchida volta com o que foi digitado (decisão 183). */
    void novaOs();

    /** A Nova OS foi gravada ou cancelada: a próxima começa em branco. */
    void fecharNovaOs();

    /** A tela da OS, na área da Oficina. */
    void abrirOs(long osId);

    /**
     * A ficha do veículo (bloco 3.3), na área da Oficina.
     *
     * @param rotuloVoltar de onde ela foi aberta, para o botão de voltar (ex.: "OS-2026-0001", "Projeto #3")
     * @param voltar o que o botão de voltar faz
     */
    void abrirFichaDoVeiculo(long veiculoId, String rotuloVoltar, Runnable voltar);
}
