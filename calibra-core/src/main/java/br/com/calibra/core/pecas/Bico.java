package br.com.calibra.core.pecas;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Bico injetor (SPEC 8, catálogo de bicos). Os dados vêm do fabricante ou do usuário, nunca do app.
 *
 * @param vazaoNominalCm3Min vazão medida no teste, com o fluido de teste
 * @param pressaoTesteKpa pressão diferencial do teste
 * @param densidadeFluidoTesteKgL densidade do fluido de teste (padrão da SPEC: 0,70 kg/L)
 * @param impedanciaOhm opcional
 * @param data data da informação, opcional
 */
public record Bico(
        String nome,
        double vazaoNominalCm3Min,
        double pressaoTesteKpa,
        double densidadeFluidoTesteKgL,
        TempoMorto tempoMorto,
        Double impedanciaOhm,
        String fonte,
        LocalDate data) {

    public Bico {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(tempoMorto, "tempoMorto");
        Objects.requireNonNull(fonte, "fonte");
    }

    /** Bico digitado na calculadora, com tempo morto de valor único. */
    public static Bico informado(String nome, double vazaoNominalCm3Min, double pressaoTesteKpa,
            double densidadeFluidoTesteKgL, double tempoMortoMs) {
        return new Bico(nome, vazaoNominalCm3Min, pressaoTesteKpa, densidadeFluidoTesteKgL,
                TempoMorto.fixo(tempoMortoMs), null, "informado na calculadora", null);
    }
}
