package br.com.calibra.core.mapa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Como a tabela aparece na tela do software da injeção, para a folha de transcrição sair igual (SPEC 9, nível C).
 * As colunas sempre crescem da esquerda para a direita.
 *
 * @param colunas o eixo que fica nas colunas
 * @param linhas se o eixo das linhas cresce para baixo ou para cima
 */
public record OrientacaoTabela(EixoDasColunas colunas, OrdemDasLinhas linhas) {

    /** O eixo que fica nas colunas. */
    public enum EixoDasColunas {
        ROTACAO("Rotação nas colunas, carga nas linhas"),
        CARGA("Carga nas colunas, rotação nas linhas");

        private final String rotulo;

        EixoDasColunas(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    /** O sentido do eixo das linhas. */
    public enum OrdemDasLinhas {
        CRESCE_PARA_BAIXO("Linhas: o menor em cima, o maior embaixo"),
        CRESCE_PARA_CIMA("Linhas: o maior em cima, o menor embaixo");

        private final String rotulo;

        OrdemDasLinhas(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    public OrientacaoTabela {
        Objects.requireNonNull(colunas, "colunas");
        Objects.requireNonNull(linhas, "linhas");
    }

    /** Os pontos do eixo que fica nas colunas, da esquerda para a direita. */
    public List<Double> eixoDasColunas(Tabela tabela) {
        return colunas == EixoDasColunas.ROTACAO ? tabela.rotacoes() : tabela.cargas();
    }

    /** Os pontos do eixo que fica nas linhas, de cima para baixo. */
    public List<Double> eixoDasLinhas(Tabela tabela) {
        List<Double> eixo = colunas == EixoDasColunas.ROTACAO ? tabela.cargas() : tabela.rotacoes();
        return linhas == OrdemDasLinhas.CRESCE_PARA_BAIXO ? eixo : eixo.reversed();
    }

    /** As células como aparecem na tela da injeção: uma lista por linha, de cima para baixo. */
    public List<List<Double>> naTela(Tabela tabela) {
        int nCargas = tabela.cargas().size();
        int nRotacoes = tabela.rotacoes().size();
        boolean rotacaoNasColunas = colunas == EixoDasColunas.ROTACAO;
        int nLinhas = rotacaoNasColunas ? nCargas : nRotacoes;
        int nColunas = rotacaoNasColunas ? nRotacoes : nCargas;
        List<List<Double>> tela = new ArrayList<>();
        for (int l = 0; l < nLinhas; l++) {
            int noEixo = linhas == OrdemDasLinhas.CRESCE_PARA_BAIXO ? l : nLinhas - 1 - l;
            List<Double> linha = new ArrayList<>();
            for (int c = 0; c < nColunas; c++) {
                linha.add(rotacaoNasColunas ? tabela.valor(noEixo, c) : tabela.valor(c, noEixo));
            }
            tela.add(Collections.unmodifiableList(linha));
        }
        return Collections.unmodifiableList(tela);
    }

    /**
     * O caminho de volta de {@link #naTela}: as células copiadas da tela da injeção viram a tabela, com os eixos dela.
     *
     * @throws IllegalArgumentException em português, se o número de linhas ou de colunas não bate com os eixos
     */
    public Tabela daTela(List<Double> rotacoes, List<Double> cargas, List<List<Double>> naTela) {
        boolean rotacaoNasColunas = colunas == EixoDasColunas.ROTACAO;
        List<Double> eixoLinhas = rotacaoNasColunas ? cargas : rotacoes;
        List<Double> eixoColunas = rotacaoNasColunas ? rotacoes : cargas;
        String nomeLinhas = rotacaoNasColunas ? "carga" : "rotação";
        String nomeColunas = rotacaoNasColunas ? "rotação" : "carga";
        if (naTela.size() != eixoLinhas.size()) {
            throw new IllegalArgumentException("a tabela tem " + naTela.size() + " linha(s), e o eixo de " + nomeLinhas
                    + " tem " + eixoLinhas.size() + " ponto(s).");
        }
        for (int l = 0; l < naTela.size(); l++) {
            if (naTela.get(l).size() != eixoColunas.size()) {
                throw new IllegalArgumentException("a linha " + (l + 1) + " tem " + naTela.get(l).size()
                        + " número(s), e o eixo de " + nomeColunas + " tem " + eixoColunas.size() + " ponto(s).");
            }
        }
        return Tabela.preencher(rotacoes, cargas, (rotacao, carga) -> {
            int r = rotacoes.indexOf(rotacao);
            int c = cargas.indexOf(carga);
            int l = rotacaoNasColunas ? c : r;
            int linhaNaTela = linhas == OrdemDasLinhas.CRESCE_PARA_BAIXO ? l : naTela.size() - 1 - l;
            return naTela.get(linhaNaTela).get(rotacaoNasColunas ? r : c);
        });
    }
}
