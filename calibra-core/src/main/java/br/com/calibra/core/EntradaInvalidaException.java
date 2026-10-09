package br.com.calibra.core;

import java.util.List;

/** Entrada que o núcleo não aceita. Traz todas as mensagens em português, para mostrar de uma vez. */
public final class EntradaInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> erros;

    public EntradaInvalidaException(List<String> erros) {
        super(String.join(" ", erros));
        this.erros = List.copyOf(erros);
    }

    public EntradaInvalidaException(String erro) {
        this(List.of(erro));
    }

    public List<String> erros() {
        return erros;
    }
}
