package br.com.calibra.core.calculo;

import static br.com.calibra.core.Fixtures.assertProximo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.calibra.core.Fixtures;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.unidades.Conversoes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Casos golden B, C e D da SPEC 7.6, tolerância de 1%. O caso A está em {@link FormulasTest}. */
class CasosGoldenTest {

    private static Dimensionamento calcular(EntradaDimensionamento entrada) {
        return CalculoDimensionamento.calcular(entrada).valor();
    }

    private static Cor cor(Dimensionamento d, Indicador indicador) {
        return d.avaliacao(indicador).orElseThrow().cor();
    }

    @Nested
    class CasoB_DoisLitrosTurboEtanol {

        private final Dimensionamento d = calcular(Fixtures.casoB());

        @Test
        void arECombustivel() {
            assertProximo(37.57, d.combustao().vazaoArLbMin(), "Wa");
            assertProximo(152.8, d.combustao().combustivelKgH(), "combustível");
        }

        @Test
        void vazaoPorBico() {
            assertProximo(796, d.bicoNecessario().vazaoMassaGMin(), "vazão por bico (g/min)");
            assertProximo(984, d.bicoNecessario().vazaoCombustivelCm3Min(), "vazão por bico (cm³/min de etanol)");
            assertProximo(1058, d.bicoNecessario().vazaoNominalCm3Min(), "vazão nominal");
        }

        @Test
        void pressoes() {
            assertProximo(94.3, d.ladoDoAr().patmKpa(), "P_atm");
            assertProximo(241, d.ladoDoAr().mapKpa(), "MAP");
            assertProximo(2.91, d.ladoDoAr().razaoPressao(), "PR");
        }

        @Test
        void temperaturas() {
            assertProximo(173, d.ladoDoAr().t2C(), "T2");
            assertProximo(55, d.ladoDoAr().tmC(), "Tm");
        }

        @Test
        void tempoDeInjecaoEDc() {
            assertProximo(13.81, d.bicoAvaliado().tiEfetivoMs(), "TI efetivo");
            assertProximo(14.81, d.bicoAvaliado().tiTotalMs(), "TI com tempo morto");
            assertProximo(17.14, d.porInjecao().cicloMs(), "ciclo");
            assertProximo(0.86, d.bicoAvaliado().dutyCycle(), "DC");
            assertEquals(Cor.VERMELHO, cor(d, Indicador.DC_BICO));
        }

        @Test
        void alternativasDeBico() {
            Dimensionamento maior = calcular(Fixtures.casoB().comBico(Bico.informado("1.300", 1300, 300, 0.70, 1.0)));
            assertProximo(0.71, maior.bicoAvaliado().dutyCycle(), "DC com 1.300 cm³/min a 3 bar");

            Dimensionamento quatroBar = calcular(Fixtures.casoB().comPressaoBase(400));
            assertProximo(0.76, quatroBar.bicoAvaliado().dutyCycle(), "DC com 1.050 cm³/min a 4 bar");
        }

        @Test
        void esforco() {
            assertProximo(25.2, d.esforco().bmepBar(), "BMEP");
            assertEquals(Cor.AMARELO, cor(d, Indicador.BMEP));
            assertProximo(401, d.esforco().torqueNm(), "torque");
            assertProximo(21.7, d.esforco().velocidadePistaoMS(), "velocidade do pistão");
            assertEquals(Cor.AMARELO, cor(d, Indicador.VELOCIDADE_PISTAO));
        }

        @Test
        void turboSemMapaSaiAmarelo() {
            assertEquals(Cor.AMARELO, cor(d, Indicador.MAPA_COMPRESSOR));
            assertFalse(d.avaliacao(Indicador.ASPIRADO_MAP_PATM).isPresent());
        }
    }

    @Nested
    class CasoC_TresCilindrosTurboE32 {

        private final Dimensionamento d = calcular(Fixtures.casoC());

        @Test
        void cilindradaEAr() {
            assertProximo(999, d.ladoDoAr().cilindradaCc(), "cilindrada");
            assertProximo(14.09, d.combustao().vazaoArLbMin(), "Wa");
        }

        @Test
        void combustivel() {
            assertProximo(37.5, d.combustao().combustivelKgH(), "combustível");
            assertProximo(0.558, d.combustao().bsfcLbHpH(), "BSFC");
        }

        @Test
        void vazaoPorBico() {
            assertProximo(260, d.bicoNecessario().vazaoMassaGMin(), "vazão por bico (g/min)");
            assertProximo(344, d.bicoNecessario().vazaoCombustivelCm3Min(), "vazão por bico (cm³/min de gasolina)");
            assertProximo(358, d.bicoNecessario().vazaoNominalCm3Min(), "vazão nominal");
        }

        @Test
        void pressoesETemperaturas() {
            assertProximo(94.3, d.ladoDoAr().patmKpa(), "P_atm");
            assertProximo(229, d.ladoDoAr().mapKpa(), "MAP");
            assertProximo(2.78, d.ladoDoAr().razaoPressao(), "PR");
            assertProximo(165, d.ladoDoAr().t2C(), "T2");
            assertProximo(53, d.ladoDoAr().tmC(), "Tm");
        }

        @Test
        void tempoDeInjecaoEDc() {
            assertProximo(14.19, d.bicoAvaliado().tiEfetivoMs(), "TI efetivo");
            assertProximo(15.19, d.bicoAvaliado().tiTotalMs(), "TI com tempo morto");
            assertProximo(21.82, d.porInjecao().cicloMs(), "ciclo");
            assertProximo(0.70, d.bicoAvaliado().dutyCycle(), "DC");
            assertEquals(Cor.VERDE, cor(d, Indicador.DC_BICO));
        }

        @Test
        void esforco() {
            assertProximo(24.1, d.esforco().bmepBar(), "BMEP");
            assertEquals(Cor.AMARELO, cor(d, Indicador.BMEP));
            assertProximo(192, d.esforco().torqueNm(), "torque");
            assertProximo(14.0, d.esforco().velocidadePistaoMS(), "velocidade do pistão");
            assertEquals(Cor.VERDE, cor(d, Indicador.VELOCIDADE_PISTAO));
        }

        @Test
        void saidasEmSequencial() {
            assertEquals(3, d.saidas().bicos());
            assertEquals(3, d.saidas().ignicao());
        }
    }

    @Nested
    class CasoD_V12Aspirado {

        private final Dimensionamento nivelDoMar = calcular(Fixtures.casoD(0));
        private final Dimensionamento a600m = calcular(Fixtures.casoD(600));

        @Test
        void cilindradaEAr() {
            assertProximo(6002, nivelDoMar.ladoDoAr().cilindradaCc(), "cilindrada");
            assertProximo(42.27, nivelDoMar.combustao().vazaoArLbMin(), "Wa");
        }

        @Test
        void combustivel() {
            assertProximo(103.3, nivelDoMar.combustao().combustivelKgH(), "combustível");
            assertProximo(0.513, nivelDoMar.combustao().bsfcLbHpH(), "BSFC");
        }

        @Test
        void vazaoPorBico() {
            assertProximo(179, nivelDoMar.bicoNecessario().vazaoMassaGMin(), "vazão por bico (g/min)");
            assertProximo(237, nivelDoMar.bicoNecessario().vazaoCombustivelCm3Min(), "vazão por bico (cm³/min)");
            assertProximo(247, nivelDoMar.bicoNecessario().vazaoNominalCm3Min(), "vazão nominal");
        }

        @Test
        void mapNecessarioContraPatm() {
            assertProximo(96.6, nivelDoMar.ladoDoAr().mapKpa(), "MAP");
            assertProximo(0.953, nivelDoMar.ladoDoAr().mapSobrePatm(), "MAP ÷ P_atm ao nível do mar");
            assertEquals(Cor.AMARELO, nivelDoMar.avaliacao(Indicador.ASPIRADO_MAP_PATM).orElseThrow().cor());
            assertProximo(1.024, a600m.ladoDoAr().mapSobrePatm(), "MAP ÷ P_atm a 600 m");
            assertEquals(Cor.VERMELHO, a600m.avaliacao(Indicador.ASPIRADO_MAP_PATM).orElseThrow().cor());
            assertProximo(35, nivelDoMar.ladoDoAr().tmC(), "Tm");
        }

        @Test
        void tempoDeInjecaoEDc() {
            assertProximo(14.01, nivelDoMar.bicoAvaliado().tiEfetivoMs(), "TI efetivo");
            assertProximo(15.01, nivelDoMar.bicoAvaliado().tiTotalMs(), "TI com tempo morto");
            assertProximo(18.46, nivelDoMar.porInjecao().cicloMs(), "ciclo");
            assertProximo(0.81, nivelDoMar.bicoAvaliado().dutyCycle(), "DC");
            assertEquals(Cor.AMARELO, nivelDoMar.avaliacao(Indicador.DC_BICO).orElseThrow().cor());
        }

        @Test
        void esforco() {
            assertProximo(10.2, nivelDoMar.esforco().bmepBar(), "BMEP");
            assertEquals(Cor.VERDE, nivelDoMar.avaliacao(Indicador.BMEP).orElseThrow().cor());
            assertProximo(486, nivelDoMar.esforco().torqueNm(), "torque");
            assertProximo(17.4, nivelDoMar.esforco().velocidadePistaoMS(), "velocidade do pistão");
            assertEquals(Cor.VERDE, nivelDoMar.avaliacao(Indicador.VELOCIDADE_PISTAO).orElseThrow().cor());
        }

        @Test
        void aspiradoNaoTemCompressor() {
            assertTrue(Double.isNaN(nivelDoMar.ladoDoAr().razaoPressao()));
            assertEquals(null, nivelDoMar.pontoOperacao());
            assertFalse(nivelDoMar.avaliacao(Indicador.MAPA_COMPRESSOR).isPresent());
        }

        @Test
        void saidas() {
            assertEquals(12, nivelDoMar.saidas().bicos());
            assertEquals(12, nivelDoMar.saidas().ignicao());
        }

        @Test
        void potenciaConfere() {
            assertProximo(450, Conversoes.kwParaCv(nivelDoMar.potencia().virabrequimKw()), "potência");
        }
    }
}
