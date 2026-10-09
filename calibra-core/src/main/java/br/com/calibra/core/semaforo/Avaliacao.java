package br.com.calibra.core.semaforo;

import java.util.Objects;

/**
 * Resultado de um indicador do semáforo.
 *
 * @param valor número avaliado (NaN quando o indicador não é numérico, como as saídas da injeção)
 */
public record Avaliacao(Indicador indicador, Cor cor, double valor, String unidade, String mensagem) {

    public Avaliacao {
        Objects.requireNonNull(indicador, "indicador");
        Objects.requireNonNull(cor, "cor");
        Objects.requireNonNull(unidade, "unidade");
        Objects.requireNonNull(mensagem, "mensagem");
    }
}
