package br.com.calibra.core.semaforo;

import java.time.LocalDateTime;
import java.util.Objects;

/** Justificativa escrita para seguir com um resultado vermelho (regra 2.4), com data e hora. */
public record Justificativa(Indicador indicador, String texto, LocalDateTime dataHora) {

    public Justificativa {
        Objects.requireNonNull(indicador, "indicador");
        Objects.requireNonNull(dataHora, "dataHora");
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("A justificativa de \"" + indicador.rotulo() + "\" não pode ficar vazia.");
        }
        texto = texto.strip();
    }
}
