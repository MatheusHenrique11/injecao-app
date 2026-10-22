package br.com.calibra.core.mapa;

import java.time.LocalDateTime;
import java.util.Objects;

/** A justificativa escrita para seguir com um alerta vermelho do mapa base (regra 2.4), com data e hora. */
public record JustificativaMapa(AlertaMapa.Codigo alerta, String texto, LocalDateTime dataHora) {

    public JustificativaMapa {
        Objects.requireNonNull(alerta, "alerta");
        Objects.requireNonNull(dataHora, "dataHora");
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("A justificativa de \"" + alerta.rotulo() + "\" não pode ficar vazia.");
        }
        texto = texto.strip();
    }
}
