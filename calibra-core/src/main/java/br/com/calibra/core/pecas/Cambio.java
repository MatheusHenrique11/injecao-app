package br.com.calibra.core.pecas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import br.com.calibra.core.cadastro.TipoCambio;

/**
 * Câmbio do catálogo (pedido D): modelo, tipo e o torque máximo que ele aguenta. O limite entra no indicador
 * "Torque × câmbio" do bloco 2.4.
 *
 * @param torqueMaximoNm opcional; sem ele, o indicador só avisa
 */
public record Cambio(String nome, TipoCambio tipo, Double torqueMaximoNm, String fonte, LocalDate data) {

    public Cambio {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(fonte, "fonte");
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (tipo == null) {
            erros.add("Escolha o tipo do câmbio.");
        }
        if (torqueMaximoNm != null && !(torqueMaximoNm > 0)) {
            erros.add("O torque máximo do câmbio deve ser maior que zero (N·m).");
        }
        return erros;
    }
}
