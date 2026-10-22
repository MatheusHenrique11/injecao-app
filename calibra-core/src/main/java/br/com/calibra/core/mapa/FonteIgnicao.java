package br.com.calibra.core.mapa;

import java.util.Objects;

/**
 * De onde vem o avanço do mapa base (SPEC 7.5): o modelo da família no catálogo ou uma tabela digitada no projeto. O
 * mapa final de um projeto parecido, da biblioteca, entra na fase 6. Sem fonte, a ignição sai vazia, em vermelho.
 *
 * @param descricao o nome do modelo ou "Tabela digitada no projeto"
 * @param combustivel o código do combustível em que o avanço foi feito; vazio se não se sabe
 * @param temperaturaArReferenciaC a temperatura do ar em que o avanço foi feito; null se não se sabe
 * @param avanco graus em rpm × kPa
 */
public record FonteIgnicao(Origem origem, String descricao, String combustivel, Double temperaturaArReferenciaC,
        Tabela avanco) {

    /** A origem do avanço, na ordem de preferência da SPEC 7.5 (a biblioteca vem na fase 6). */
    public enum Origem {
        MODELO_DA_FAMILIA("Modelo da família"),
        TABELA_DIGITADA("Tabela digitada");

        private final String rotulo;

        Origem(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    public FonteIgnicao {
        Objects.requireNonNull(origem, "origem");
        Objects.requireNonNull(avanco, "avanco");
        descricao = descricao == null ? "" : descricao.strip();
        combustivel = combustivel == null ? "" : combustivel.strip();
    }

    public static FonteIgnicao doModelo(ModeloIgnicao modelo) {
        return new FonteIgnicao(Origem.MODELO_DA_FAMILIA, modelo.nome(), modelo.combustivel(),
                modelo.temperaturaArReferenciaC(), modelo.avanco());
    }

    /**
     * @param combustivel o código do combustível em que a tabela foi feita; vazio se não se sabe
     * @param temperaturaArC a temperatura do ar da tabela; null se não se sabe
     */
    public static FonteIgnicao digitada(Tabela avanco, String combustivel, Double temperaturaArC) {
        return new FonteIgnicao(Origem.TABELA_DIGITADA, "Tabela digitada no projeto", combustivel, temperaturaArC,
                avanco);
    }
}
