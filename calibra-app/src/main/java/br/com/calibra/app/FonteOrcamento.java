package br.com.calibra.app;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import br.com.calibra.app.casos.CasosOrcamento;
import br.com.calibra.app.casos.CasosOrcamento.SituacaoOrcamento;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.PadroesOrcamento;

/**
 * De onde vem o orçamento que o editor mostra: o do projeto de preparação (aba "Peças e orçamento") ou o da OS (aba
 * "Orçamento"). O resto (salvar, enviar, responder, revisar) é igual para os dois, pelo id do orçamento.
 */
interface FonteOrcamento {

    /** As revisões, da mais nova para a mais antiga. */
    List<SituacaoOrcamento> revisoes();

    /** Por que o orçamento não muda mais (a OS foi entregue ou cancelada): o editor só mostra. Vazio = dá para mexer. */
    Optional<String> trava();

    /**
     * O orçamento novo já preenchido.
     *
     * @throws EntradaInvalidaException se ele vem de um cálculo que não está em dia
     */
    Orcamento sugestao(PadroesOrcamento padroes);

    /** Grava o primeiro orçamento, em rascunho. */
    void criar(Orcamento conteudo);

    /** Um orçamento novo daqui usa o cálculo do projeto (as peças do cálculo e o PDF com o resumo técnico). */
    boolean comCalculo();

    /** O que muda no projeto ao marcar como enviado e ao aprovar, para a mensagem; vazio na OS. */
    String aoEnviar();

    String aoAprovar();

    /**
     * O orçamento do projeto de preparação, o da OS em andamento dele: sempre com o cálculo, e o projeto acompanha o
     * envio e a aprovação. Sem OS em andamento, o orçamento novo abre uma.
     */
    static FonteOrcamento doProjeto(CasosOrcamento casos, long projetoId) {
        return new FonteOrcamento() {
            @Override
            public List<SituacaoOrcamento> revisoes() {
                return casos.orcamentos(projetoId);
            }

            @Override
            public Optional<String> trava() {
                return Optional.empty();
            }

            @Override
            public Orcamento sugestao(PadroesOrcamento padroes) {
                return casos.sugestao(projetoId, padroes);
            }

            @Override
            public void criar(Orcamento conteudo) {
                casos.criar(projetoId, conteudo);
            }

            @Override
            public boolean comCalculo() {
                return true;
            }

            @Override
            public String aoEnviar() {
                return " O projeto passou para Orçado.";
            }

            @Override
            public String aoAprovar() {
                return " O projeto passou para Aprovado.";
            }
        };
    }

    /**
     * O orçamento da OS: com o cálculo quando a OS tem projeto de preparação; na OS comum, os itens são digitados.
     *
     * @param comProjeto se a OS tem projeto ligado agora (ele pode ser ligado com a tela aberta)
     */
    static FonteOrcamento daOs(CasosOrcamento casos, long osId, BooleanSupplier comProjeto) {
        return new FonteOrcamento() {
            @Override
            public List<SituacaoOrcamento> revisoes() {
                return casos.orcamentosDaOs(osId);
            }

            @Override
            public Optional<String> trava() {
                return casos.trava(osId);
            }

            @Override
            public Orcamento sugestao(PadroesOrcamento padroes) {
                return casos.sugestaoDaOs(osId, padroes);
            }

            @Override
            public void criar(Orcamento conteudo) {
                casos.criarNaOs(osId, conteudo);
            }

            @Override
            public boolean comCalculo() {
                return comProjeto.getAsBoolean();
            }

            @Override
            public String aoEnviar() {
                return "";
            }

            @Override
            public String aoAprovar() {
                return "";
            }
        };
    }
}
