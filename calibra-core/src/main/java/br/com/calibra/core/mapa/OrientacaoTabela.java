package br.com.calibra.core.mapa;

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
}
