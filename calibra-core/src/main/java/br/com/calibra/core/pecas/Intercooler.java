package br.com.calibra.core.pecas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Intercooler do catálogo (SPEC 8.5). A eficiência, quando o fabricante ou o usuário informar, vira o eps
 * do cálculo (SPEC 7.1); sem ela, vale o padrão de Configurações.
 *
 * @param eficiencia fração de 0 a 1 (0,80 = 80%); opcional
 */
public record Intercooler(String nome, TipoIntercooler tipo, Double eficiencia, String fonte, LocalDate data) {

    public Intercooler {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(fonte, "fonte");
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (tipo == null) {
            erros.add("Escolha o tipo do intercooler (ar-ar ou ar-água).");
        }
        if (eficiencia != null && !(eficiencia > 0 && eficiencia <= 1)) {
            erros.add("A eficiência do intercooler deve ficar entre 0 e 1 (0,80 = 80%).");
        }
        return erros;
    }
}
