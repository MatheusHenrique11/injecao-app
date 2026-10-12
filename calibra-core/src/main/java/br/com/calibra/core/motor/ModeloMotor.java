package br.com.calibra.core.motor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Motor de fábrica ou montagem do catálogo (pedido C), para preencher o motor do projeto. Só a geometria e os
 * dados que têm fonte: o que não se sabe fica vazio (regra: não inventar dado técnico). A ordem de ignição não
 * entra: o formulário sugere uma e pede "confirmar no manual", como já faz.
 *
 * @param montagem true para uma montagem (ex.: AP "1.9" com pistões do 2.0 e virabrequim do 1.8)
 * @param bielaMm comprimento da biela, centro a centro; opcional
 * @param taxaCompressao opcional
 * @param valvulasPorCilindro opcional
 * @param cilindradaFichaCc cilindrada publicada pelo fabricante ou pela fonte; opcional
 * @param aspiracao de fábrica; opcional
 * @param descricao receita da montagem ou observação técnica
 */
public record ModeloMotor(
        String nome,
        boolean montagem,
        int cilindros,
        double diametroMm,
        double cursoMm,
        Double bielaMm,
        Double taxaCompressao,
        Integer valvulasPorCilindro,
        Double cilindradaFichaCc,
        Aspiracao aspiracao,
        String descricao,
        String fonte,
        LocalDate data) {

    public ModeloMotor {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(fonte, "fonte");
        descricao = descricao == null ? "" : descricao.strip();
    }

    /** π/4 × D² × S × n, em cm³ (SPEC 6.3). */
    public double cilindradaCc() {
        return Math.PI / 4.0 * diametroMm * diametroMm * cursoMm * cilindros / 1000.0;
    }

    /** Relação biela/curso (ex.: 1,55 no AP 2.0); NaN sem a biela. Curso maior com a mesma biela piora a relação. */
    public double relacaoBielaCurso() {
        return bielaMm == null ? Double.NaN : bielaMm / cursoMm;
    }

    /** Diferença entre a cilindrada calculada e a da ficha, em fração; NaN sem a ficha. */
    public double diferencaDaFicha() {
        return cilindradaFichaCc == null ? Double.NaN : cilindradaCc() / cilindradaFichaCc - 1.0;
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (cilindros < ValidadorMotor.MIN_CILINDROS || cilindros > ValidadorMotor.MAX_CILINDROS) {
            erros.add(ValidadorMotor.MENSAGEM_CILINDROS);
        }
        if (!(diametroMm > 0) || !(cursoMm > 0)) {
            erros.add("O diâmetro e o curso devem ser maiores que zero.");
        }
        if (bielaMm != null && !(bielaMm > 0)) {
            erros.add("A biela deve ser maior que zero (centro a centro, mm).");
        }
        if (taxaCompressao != null && !(taxaCompressao >= 6.0 && taxaCompressao <= 16.0)) {
            erros.add("A taxa de compressão deve ficar entre 6,0 e 16,0.");
        }
        if (valvulasPorCilindro != null && (valvulasPorCilindro < 2 || valvulasPorCilindro > 5)) {
            erros.add("As válvulas por cilindro devem ficar entre 2 e 5.");
        }
        if (cilindradaFichaCc != null && !(cilindradaFichaCc > 0)) {
            erros.add("A cilindrada da ficha deve ser maior que zero.");
        }
        return erros;
    }
}
