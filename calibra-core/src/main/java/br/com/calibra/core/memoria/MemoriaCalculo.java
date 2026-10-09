package br.com.calibra.core.memoria;

import java.util.ArrayList;
import java.util.List;

/**
 * Caderno de anotações de um cálculo: cada conta feita vira um {@link Passo}. No fim,
 * {@link #resultado(Object)} junta valor, passos, avisos e versão do núcleo.
 */
public final class MemoriaCalculo {

    private final List<Passo> passos = new ArrayList<>();
    private final List<Aviso> avisos = new ArrayList<>();

    /**
     * Anota um passo e devolve o próprio resultado, para o código ficar na ordem da conta:
     * {@code double wa = memoria.passo("Vazão de ar", "Wa = P_hp / k_ar", conta, "lb/min", ...);}
     */
    public double passo(String grandeza, String formula, double resultado, String unidade, Entrada... entradas) {
        passos.add(new Passo(grandeza, formula, List.of(entradas), new Valor(resultado, unidade)));
        return resultado;
    }

    public void aviso(Aviso aviso) {
        avisos.add(aviso);
    }

    public void aviso(NivelAviso nivel, String mensagem) {
        avisos.add(new Aviso(nivel, mensagem));
    }

    public List<Passo> passos() {
        return List.copyOf(passos);
    }

    public List<Aviso> avisos() {
        return List.copyOf(avisos);
    }

    public <T> Resultado<T> resultado(T valor) {
        return new Resultado<>(valor, passos, avisos, VersaoNucleo.ATUAL);
    }

    /** Atalho para escrever entradas: {@code e("P_hp", 394.5, "hp")}. */
    public static Entrada e(String simbolo, double numero, String unidade) {
        return Entrada.de(simbolo, numero, unidade);
    }
}
