package br.com.calibra.core.pecas;

import br.com.calibra.core.unidades.Conversoes;

/** Unidade do eixo de vazão de um mapa de compressor, como o fabricante desenha. O núcleo guarda tudo em kg/s. */
public enum UnidadeVazao {
    LB_MIN("lb/min"),
    KG_S("kg/s");

    private final String simbolo;

    UnidadeVazao(String simbolo) {
        this.simbolo = simbolo;
    }

    public String simbolo() {
        return simbolo;
    }

    /** Valor lido no eixo do mapa → kg/s. */
    public double paraKgS(double valor) {
        return this == LB_MIN ? Conversoes.lbMinParaKgS(valor) : valor;
    }

    /** kg/s → valor no eixo do mapa. */
    public double deKgS(double kgS) {
        return this == LB_MIN ? Conversoes.kgSParaLbMin(kgS) : kgS;
    }
}
