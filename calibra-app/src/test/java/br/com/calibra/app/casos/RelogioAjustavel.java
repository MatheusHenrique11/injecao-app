package br.com.calibra.app.casos;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio de teste parado numa data, que o teste adianta ou atrasa. */
final class RelogioAjustavel extends Clock {

    private static final ZoneId FUSO = ZoneOffset.ofHours(-3);

    private Instant agora;

    RelogioAjustavel(LocalDateTime inicio) {
        this.agora = inicio.atZone(FUSO).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return FUSO;
    }

    @Override
    public Clock withZone(ZoneId fuso) {
        throw new UnsupportedOperationException("relógio de teste tem fuso fixo");
    }

    @Override
    public synchronized Instant instant() {
        return agora;
    }

    synchronized void andar(Duration quanto) {
        agora = agora.plus(quanto);
    }
}
