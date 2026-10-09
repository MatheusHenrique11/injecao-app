package br.com.calibra.core.calculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.calibra.core.Fixtures;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;
import org.junit.jupiter.api.Test;

/**
 * Testes de coerência da SPEC 7.6 (sempre verdadeiros). "Converter unidade e voltar devolve o valor
 * original" está em {@code ConversoesTest}.
 */
class CoerenciaTest {

    private static Dimensionamento calcular(EntradaDimensionamento entrada) {
        return CalculoDimensionamento.calcular(entrada).valor();
    }

    @Test
    void maisPotenciaNuncaPedeBicoMenor() {
        double nominalAnterior = 0;
        double comTempoMortoAnterior = 0;
        for (int cv = 150; cv <= 600; cv += 25) {
            Dimensionamento d = calcular(Fixtures.casoB().comMeta(Meta.noVirabrequim(cv, 7000, Uso.RUA)));
            double nominal = d.bicoNecessario().vazaoNominalCm3Min();
            double comTempoMorto = d.bicoAvaliado().vazaoNominalMinimaCm3Min();
            assertTrue(nominal >= nominalAnterior, "vazão nominal caiu em " + cv + " cv");
            assertTrue(comTempoMorto >= comTempoMortoAnterior, "vazão mínima com tempo morto caiu em " + cv + " cv");
            nominalAnterior = nominal;
            comTempoMortoAnterior = comTempoMorto;
        }
    }

    @Test
    void maisAltitudeNuncaPedeRazaoDePressaoMenor() {
        double prAnterior = 0;
        for (int altitude = 0; altitude <= 3000; altitude += 250) {
            EntradaDimensionamento entrada = Fixtures.casoB();
            entrada = entrada.comMotor(entrada.motor().paraConstrutor().altitudeLocalM(altitude).construir());
            double pr = calcular(entrada).ladoDoAr().razaoPressao();
            assertTrue(pr >= prAnterior, "PR caiu a " + altitude + " m");
            prAnterior = pr;
        }
    }

    /**
     * "Trocar o bico não muda a tabela de VE": na fase 1 ainda não há tabela de VE (fase 3), então o
     * teste confere que trocar o bico não muda nada do lado do ar.
     */
    @Test
    void trocarOBicoNaoMudaOLadoDoAr() {
        Dimensionamento original = calcular(Fixtures.casoB());
        Dimensionamento outroBico = calcular(Fixtures.casoB().comBico(Bico.informado("outro", 1300, 380, 0.72, 0.8)));
        assertEquals(original.ladoDoAr(), outroBico.ladoDoAr());
        assertEquals(original.combustao(), outroBico.combustao());
        assertEquals(original.porInjecao(), outroBico.porInjecao());
    }

    @Test
    void mesmaPotenciaTotalAVazaoPorBicoCaiNaProporcaoDe1SobreN() {
        double referencia = Double.NaN;
        for (int n = 3; n <= 12; n++) {
            EntradaDimensionamento entrada = Fixtures.casoB().comBico(null);
            entrada = entrada.comMotor(Fixtures.motor(n).combustivel(entrada.motor().combustivel())
                    .altitudeLocalM(600).construir());
            double vazaoVezesN = calcular(entrada).bicoNecessario().vazaoMassaGMin() * n;
            if (Double.isNaN(referencia)) {
                referencia = vazaoVezesN;
            }
            assertEquals(referencia, vazaoVezesN, referencia * 1e-12, n + " cilindros");
        }
    }
}
