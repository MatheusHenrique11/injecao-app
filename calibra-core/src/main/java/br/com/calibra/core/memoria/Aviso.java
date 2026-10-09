package br.com.calibra.core.memoria;

import java.util.Objects;

/** Aviso que acompanha um resultado. Não bloqueia o cálculo. */
public record Aviso(NivelAviso nivel, String mensagem) {

    public Aviso {
        Objects.requireNonNull(nivel, "nivel");
        Objects.requireNonNull(mensagem, "mensagem");
    }

    public static Aviso amarelo(String mensagem) {
        return new Aviso(NivelAviso.AMARELO, mensagem);
    }

    public static Aviso informacao(String mensagem) {
        return new Aviso(NivelAviso.INFORMACAO, mensagem);
    }
}
