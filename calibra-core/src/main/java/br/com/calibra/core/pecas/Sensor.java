package br.com.calibra.core.pecas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sensor do catálogo (SPEC 8.5), com a faixa de medição. Num sensor MAP, a faixa máxima (kPa absolutos) é a
 * "faixa do sensor MAP" do semáforo.
 *
 * @param faixaMinima opcional; na unidade do tipo ({@link TipoSensor#unidade()})
 * @param faixaMaxima na unidade do tipo
 */
public record Sensor(String nome, TipoSensor tipo, Double faixaMinima, double faixaMaxima, String fonte,
        LocalDate data) {

    public Sensor {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(fonte, "fonte");
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (tipo == null) {
            erros.add("Escolha o tipo do sensor.");
        }
        if (!Double.isFinite(faixaMaxima)) {
            erros.add("Informe a faixa máxima do sensor.");
        } else if (tipo == TipoSensor.MAP && !(faixaMaxima > 0)) {
            erros.add("A faixa máxima do sensor MAP deve ser maior que zero (kPa absolutos).");
        }
        if (faixaMinima != null && Double.isFinite(faixaMaxima) && !(faixaMinima < faixaMaxima)) {
            erros.add("A faixa mínima do sensor precisa ser menor que a máxima.");
        }
        return erros;
    }
}
