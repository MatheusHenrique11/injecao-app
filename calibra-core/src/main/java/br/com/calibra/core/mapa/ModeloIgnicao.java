package br.com.calibra.core.mapa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Modelo de ignição de uma família de motor (SPEC 7.5; decisão 293): o avanço, em graus, em rotação (rpm) × carga
 * (MAP em kPa absolutos), digitado ou colado pelo mecânico a partir de um mapa que ele conhece. O app não traz nenhum
 * e não inventa avanço: fora dos eixos do modelo, a célula do mapa base fica vazia.
 *
 * @param familia a família de motor a que o modelo se aplica (texto livre, ex.: "VW AP 2.0 8v turbo")
 * @param combustivel o código do combustível em que o mapa foi feito (ex.: E32); vazio se não se sabe
 * @param temperaturaArReferenciaC a temperatura do ar em que o mapa foi feito; null se não se sabe
 * @param avanco o avanço (graus antes do PMS) em rpm × kPa; todas as células preenchidas
 */
public record ModeloIgnicao(
        String nome,
        String familia,
        String combustivel,
        Double temperaturaArReferenciaC,
        Tabela avanco,
        String fonte,
        LocalDate data) {

    public ModeloIgnicao {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(avanco, "avanco");
        Objects.requireNonNull(fonte, "fonte");
        familia = familia == null ? "" : familia.strip();
        combustivel = combustivel == null ? "" : combustivel.strip();
    }

    /** O que está errado no modelo, em português; vazio se estiver certo. */
    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        erros.addAll(Eixos.validar("Rotação (rpm)", avanco.rotacoes(), 1, 20000));
        erros.addAll(Eixos.validar("Carga (kPa)", avanco.cargas(), 1, 1000));
        if (temperaturaArReferenciaC != null && !(temperaturaArReferenciaC >= -40 && temperaturaArReferenciaC <= 150)) {
            erros.add("Temperatura do ar de referência: de −40 a 150 °C (ou deixe em branco).");
        }
        if (!avanco.completa()) {
            erros.add("Preencha todas as células do avanço.");
        } else if (avanco.valores().stream().flatMap(List::stream).anyMatch(v -> !(v >= -20 && v <= 70))) {
            erros.add("O avanço de cada célula fica entre −20° e 70°.");
        }
        return erros;
    }
}
