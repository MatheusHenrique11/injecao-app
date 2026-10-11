package br.com.calibra.dados;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio de teste: começa numa data fixa e anda um segundo a cada leitura. */
public final class RelogioDeTeste extends Clock {

    private static final ZoneId FUSO = ZoneOffset.ofHours(-3);

    private Instant agora;

    public RelogioDeTeste(LocalDateTime inicio) {
        this.agora = inicio.atZone(FUSO).toInstant();
    }

    public RelogioDeTeste() {
        this(LocalDateTime.of(2026, 10, 11, 18, 30, 0));
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
        Instant leitura = agora;
        agora = agora.plusSeconds(1);
        return leitura;
    }

    /** Pula para a frente (ex.: um dia depois). */
    public synchronized void avancarSegundos(long segundos) {
        agora = agora.plusSeconds(segundos);
    }
}
