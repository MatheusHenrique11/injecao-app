package br.com.calibra.core.motor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Motor e montagem do catálogo (pedido C): cilindrada, relação biela/curso e diferença da ficha. */
class ModeloMotorTest {

    /** AP 2.0 e montagem "2.2" da pesquisa (docs/pesquisa/mercado-preparacao-brasil.md, seção 2.1). */
    private static final ModeloMotor AP_20 = new ModeloMotor("VW AP 2.0", false, 4, 82.5, 92.8, 144.0, null, null,
            1984.0, null, "", "pesquisa", null);
    private static final ModeloMotor AP_22 = new ModeloMotor("AP ~2.2", true, 4, 85.0, 99.0, 144.0, null, null,
            2247.0, null, "Virabrequim 99 + pistão 85", "pesquisa", null);

    @Test
    void cilindradaCalculadaPelaGeometria() {
        assertEquals(1984.3, AP_20.cilindradaCc(), 0.1);
        assertEquals(2247.1, AP_22.cilindradaCc(), 0.1);
        assertTrue(Math.abs(AP_20.diferencaDaFicha()) < 0.001);
    }

    /** A pesquisa: relação biela/curso 1,55 no 2.0 original e 1,45 com curso de 99 mm. */
    @Test
    void relacaoBielaCursoPioraComCursoMaior() {
        assertEquals(1.55, AP_20.relacaoBielaCurso(), 0.005);
        assertEquals(1.45, AP_22.relacaoBielaCurso(), 0.005);
        ModeloMotor semBiela = new ModeloMotor("X", false, 4, 86, 86, null, null, null, null, null, "", "f", null);
        assertTrue(Double.isNaN(semBiela.relacaoBielaCurso()));
        assertTrue(Double.isNaN(semBiela.diferencaDaFicha()));
    }

    @Test
    void validaComAsFaixasDaSpec() {
        assertTrue(AP_20.validar().isEmpty());
        ModeloMotor errado = new ModeloMotor("X", false, 13, 0, 86, -1.0, 17.0, 6, 0.0, null, "", "f", null);
        assertEquals(List.of("O app aceita motores de 3 a 12 cilindros.",
                "O diâmetro e o curso devem ser maiores que zero.",
                "A biela deve ser maior que zero (centro a centro, mm).",
                "A taxa de compressão deve ficar entre 6,0 e 16,0.",
                "As válvulas por cilindro devem ficar entre 2 e 5.",
                "A cilindrada da ficha deve ser maior que zero."), errado.validar());
    }
}
