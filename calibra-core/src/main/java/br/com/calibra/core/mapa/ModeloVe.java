package br.com.calibra.core.mapa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import br.com.calibra.core.motor.Comando;

/**
 * Modelo de VE de uma família de motor (SPEC 7.5; decisão 293): a VE em cada ponto de rotação relativa (rpm ÷ rpm do
 * torque máximo) e de carga relativa (MAP ÷ 100 kPa). Fica no catálogo, editável, nunca como constante no código. No
 * projeto, a VE do torque e a da potência mandam nos pontos delas; o modelo dá a forma do resto do mapa.
 *
 * @param valvulasPorCilindro a família: o número de válvulas por cilindro; null = qualquer
 * @param comandos a família: os comandos a que o modelo se aplica; vazio = qualquer
 * @param ve a VE (fração: 0,97 = 97%) em rotação relativa × carga relativa; todas as células preenchidas
 */
public record ModeloVe(
        String nome,
        Integer valvulasPorCilindro,
        Set<Comando> comandos,
        Tabela ve,
        String fonte,
        LocalDate data) {

    /** A carga relativa da plena carga do aspirado: MAP de 100 kPa. */
    public static final double KPA_DA_CARGA_RELATIVA = 100.0;

    public ModeloVe {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(ve, "ve");
        Objects.requireNonNull(fonte, "fonte");
        comandos = comandos == null || comandos.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(comandos));
    }

    /** O modelo serve para o motor: as válvulas batem (ou o modelo vale para qualquer uma), e o comando também. */
    public boolean serve(int valvulas, Comando comando) {
        return (valvulasPorCilindro == null || valvulasPorCilindro == valvulas)
                && (comandos.isEmpty() || comandos.contains(comando));
    }

    /**
     * A VE do modelo no ponto, por interpolação. Fora dos eixos do modelo vale a borda: a VE não continua subindo nem
     * descendo (o gerador avisa quando o mapa passa dos eixos).
     */
    public double ve(double rotacaoRelativa, double cargaRelativa) {
        return ve.interpolarNaBorda(rotacaoRelativa, cargaRelativa);
    }

    /** O que está errado no modelo, em português; vazio se estiver certo. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (valvulasPorCilindro != null && (valvulasPorCilindro < 2 || valvulasPorCilindro > 5)) {
            erros.add("Válvulas por cilindro: de 2 a 5 (ou deixe em branco para qualquer motor).");
        }
        erros.addAll(Eixos.validar("Rotação relativa", ve.rotacoes(), 0.01, 5.0));
        erros.addAll(Eixos.validar("Carga relativa", ve.cargas(), 0.01, 10.0));
        if (!ve.completa()) {
            erros.add("Preencha todas as células da VE.");
        } else if (ve.valores().stream().flatMap(List::stream).anyMatch(v -> !(v > 0.0 && v <= 1.5))) {
            erros.add("A VE de cada célula fica entre 1% e 150%.");
        }
        return erros;
    }
}
