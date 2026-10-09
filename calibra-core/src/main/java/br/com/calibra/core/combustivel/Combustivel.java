package br.com.calibra.core.combustivel;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Registro do catálogo de combustíveis (SPEC 7.2). É versionado: o teor de etanol da gasolina muda
 * por decisão do governo, e cada projeto guarda a versão que usou.
 *
 * @param codigo identificador curto (E32, EHC...)
 * @param densidadeKgL densidade usada para converter massa em volume
 * @param lambdaSobPressao lambda alvo padrão em plena carga, sobrealimentado
 * @param lambdaAspiradoPlenaCarga lambda alvo padrão em plena carga, aspirado
 * @param avisoLambda aviso amarelo que acompanha os lambdas padrão (metanol), ou null
 * @param vigenteDesde início da vigência, ou null se não se aplica
 * @param fonte de onde vieram os valores
 * @param versao versão do registro, a partir de 1
 */
public record Combustivel(
        String codigo,
        String nome,
        Composicao composicao,
        double densidadeKgL,
        double lambdaSobPressao,
        double lambdaAspiradoPlenaCarga,
        String avisoLambda,
        LocalDate vigenteDesde,
        String fonte,
        int versao) {

    public Combustivel {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(composicao, "composicao");
        Objects.requireNonNull(fonte, "fonte");
        if (!(densidadeKgL > 0)) {
            throw new IllegalArgumentException("A densidade do combustível deve ser maior que zero.");
        }
        if (!(lambdaSobPressao > 0) || !(lambdaAspiradoPlenaCarga > 0)) {
            throw new IllegalArgumentException("Os lambdas padrão devem ser maiores que zero.");
        }
        if (versao < 1) {
            throw new IllegalArgumentException("A versão do combustível começa em 1.");
        }
    }

    public double afrEstequiometrico() {
        return composicao.afrEstequiometrico();
    }

    /** Lambda alvo padrão em plena carga: sob pressão no sobrealimentado, aspirado no aspirado. */
    public double lambdaPadrao(boolean sobrealimentado) {
        return sobrealimentado ? lambdaSobPressao : lambdaAspiradoPlenaCarga;
    }

    /** Gasolina com ou sem etanol (para a faixa de conferência do BSFC). */
    public boolean eGasolina() {
        return composicao instanceof GasolinaComEtanol;
    }
}
