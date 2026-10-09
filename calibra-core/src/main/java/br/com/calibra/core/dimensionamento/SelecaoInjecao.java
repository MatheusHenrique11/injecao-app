package br.com.calibra.core.dimensionamento;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.ModoIgnicao;
import br.com.calibra.core.motor.ModoInjecao;
import br.com.calibra.core.motor.SaidasExigidas;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Recurso;

/**
 * Filtro de injeções do catálogo (SPEC 7.3): saídas de bico e de ignição, cilindros em sequencial e
 * recursos exigidos.
 */
public final class SelecaoInjecao {

    private SelecaoInjecao() {
    }

    /** O que falta na injeção para este motor. Lista vazia = atende. */
    public static List<String> faltas(Injecao injecao, ConfiguracaoMotor motor, int gruposSimultaneo,
            Set<Recurso> exigidos) {
        SaidasExigidas.Saidas saidas = SaidasExigidas.calcular(motor, gruposSimultaneo);
        List<String> faltas = new ArrayList<>();
        if (injecao.saidasBico() < saidas.bicosTotal()) {
            faltas.add("tem " + injecao.saidasBico() + " saídas de bico; o motor pede " + saidas.bicosTotal());
        }
        if (injecao.saidasIgnicao() < saidas.ignicao()) {
            faltas.add("tem " + injecao.saidasIgnicao() + " saídas de ignição; o motor pede " + saidas.ignicao());
        }
        boolean sequencial = motor.modoInjecao() == ModoInjecao.SEQUENCIAL
                || motor.modoIgnicao() == ModoIgnicao.SEQUENCIAL;
        Integer maximo = injecao.maxCilindrosSequencial();
        if (sequencial && maximo != null && motor.numeroCilindros() > maximo) {
            faltas.add("controla até " + maximo + " cilindros em sequencial; o motor tem " + motor.numeroCilindros());
        }
        for (Recurso recurso : exigidos) {
            if (!injecao.recursos().contains(recurso)) {
                faltas.add("não tem " + recurso.rotulo().toLowerCase(Locale.ROOT));
            }
        }
        return faltas;
    }

    /** As injeções do catálogo que atendem ao motor e aos recursos exigidos, em ordem de nome. */
    public static List<Injecao> queAtendem(List<Injecao> catalogo, ConfiguracaoMotor motor, int gruposSimultaneo,
            Set<Recurso> exigidos) {
        return catalogo.stream()
                .filter(i -> faltas(i, motor, gruposSimultaneo, exigidos).isEmpty())
                .sorted(Comparator.comparing(Injecao::nome))
                .toList();
    }
}
