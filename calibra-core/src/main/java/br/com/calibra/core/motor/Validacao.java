package br.com.calibra.core.motor;

import java.util.List;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.memoria.Aviso;

/** Resultado da validação: erros bloqueiam e explicam o motivo; avisos amarelos nunca bloqueiam. */
public record Validacao(List<String> erros, List<Aviso> avisos) {

    public Validacao {
        erros = List.copyOf(erros);
        avisos = List.copyOf(avisos);
    }

    public boolean valida() {
        return erros.isEmpty();
    }

    public void exigirValida() {
        if (!valida()) {
            throw new EntradaInvalidaException(erros);
        }
    }
}
