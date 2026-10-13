package br.com.calibra.core.calculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.Fixtures;
import br.com.calibra.core.cadastro.TipoCambio;
import br.com.calibra.core.memoria.Aviso;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.memoria.VersaoNucleo;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.pecas.Cambio;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.Avaliacao;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.unidades.Conversoes;

/** Núcleo 1.1.0 na cadeia: meta pela pressão, ponto de torque, mapa do turbo e torque × câmbio, sobre o caso B. */
class CalculoTurboTest {

    private static Dimensionamento calcular(EntradaDimensionamento entrada) {
        return CalculoDimensionamento.calcular(entrada).valor();
    }

    private static EntradaDimensionamento comTorque(EntradaDimensionamento entrada, int rpm) {
        return entrada.comMeta(entrada.meta().comRotacaoTorque(rpm));
    }

    @Test
    void resultadoVemComONucleo110() {
        assertEquals("1.1.0", VersaoNucleo.ATUAL);
        assertEquals("1.1.0", CalculoDimensionamento.calcular(Fixtures.casoB()).versaoNucleo());
    }

    /** Ida e volta: potência → MAP → potência pela pressão, com diferença de até 0,1%. */
    @Test
    void metaPelaPressaoIdaEVolta() {
        Dimensionamento pelaPotencia = calcular(Fixtures.casoB());
        double pressao = pelaPotencia.ladoDoAr().mapKpa() - pelaPotencia.ladoDoAr().patmKpa();
        Dimensionamento pelaPressao = calcular(Fixtures.casoB()
                .comMeta(Meta.pelaPressao(pressao, null, 7000, null, Uso.RUA)));
        assertEquals(400.0, pelaPressao.potencia().cv(), 400.0 * 0.001);
        assertEquals(pelaPotencia.ladoDoAr().mapKpa(), pelaPressao.ladoDoAr().mapKpa(), 1e-9);
        assertEquals(pelaPotencia.ladoDoAr().razaoPressao(), pelaPressao.ladoDoAr().razaoPressao(), 1e-9);
        assertEquals(pelaPotencia.combustao().vazaoArLbMin(), pelaPressao.combustao().vazaoArLbMin(),
                pelaPotencia.combustao().vazaoArLbMin() * 0.001);
        assertEquals(0, pelaPressao.ladoDoAr().iteracoes(), "sem iteração: o MAP é conhecido");
    }

    @Test
    void metaPelaPressaoNoCaminhoBsfc() {
        EntradaDimensionamento bsfc = Fixtures.casoB().comParametros(Parametros.padrao().comBsfc(0.60));
        Dimensionamento pelaPotencia = calcular(bsfc);
        double pressao = pelaPotencia.ladoDoAr().mapKpa() - pelaPotencia.ladoDoAr().patmKpa();
        Dimensionamento pelaPressao = calcular(bsfc.comMeta(Meta.pelaPressao(pressao, null, 7000, null, Uso.RUA)));
        assertEquals(400.0, pelaPressao.potencia().cv(), 400.0 * 0.001);
    }

    @Test
    void metaPelaPressaoComPerdaEstimaARoda() {
        Resultado<Dimensionamento> resultado = CalculoDimensionamento.calcular(Fixtures.casoB()
                .comMeta(Meta.pelaPressao(Conversoes.barParaKpa(1.0), 15.0, 7000, null, Uso.RUA)));
        assertTrue(resultado.memoria().stream().anyMatch(p -> p.grandeza().equals("Potência na roda (estimada)")));
        assertTrue(resultado.valor().potencia().cv() > 0);
    }

    @Test
    void metaPelaPressaoNoAspiradoERecusada() {
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class, () -> calcular(Fixtures.casoD(0)
                .comMeta(Meta.pelaPressao(50.0, null, 6500, null, Uso.RUA))));
        assertTrue(erro.erros().contains("A meta pela pressão só vale para motor sobrealimentado; no aspirado, informe "
                + "a potência."), erro.erros()::toString);
        EntradaInvalidaException zero = assertThrows(EntradaInvalidaException.class, () -> calcular(Fixtures.casoB()
                .comMeta(Meta.pelaPressao(0.0, null, 7000, null, Uso.RUA))));
        assertTrue(zero.erros().contains("A pressão do turbo da meta deve ser maior que zero."));
    }

    /** Com a rotação e a VE da potência, o ponto de torque é o próprio ponto de potência. */
    @Test
    void pontoDeTorqueComRpmEVeDaPotenciaEOMesmoPonto() {
        Dimensionamento d = calcular(comTorque(Fixtures.casoB(), 7000));
        Dimensionamento.PontoTorque torque = d.pontoTorque();
        assertEquals(d.combustao().vazaoArLbMin(), torque.vazaoArLbMin(), d.combustao().vazaoArLbMin() * 1e-4);
        assertEquals(d.potencia().virabrequimKw(), torque.potenciaKw(), d.potencia().virabrequimKw() * 1e-4);
        assertEquals(d.esforco().torqueNm(), torque.torqueNm(), d.esforco().torqueNm() * 1e-4);
    }

    /** Mesmo MAP e mesma Tm: o torque estimado só muda com a VE (a vazão cai junto com a rotação). */
    @Test
    void torqueEstimadoAcompanhaAVe() {
        Dimensionamento d = calcular(comTorque(Fixtures.casoB(), 5000).comVeTorque(0.90));
        Dimensionamento.PontoTorque torque = d.pontoTorque();
        assertEquals(5000, torque.rpm());
        assertEquals(0.90, torque.ve(), 0.0);
        assertEquals(d.esforco().torqueNm() * 0.90 / 0.95, torque.torqueNm(), d.esforco().torqueNm() * 1e-4);
        assertEquals(d.combustao().vazaoArLbMin() * (5000.0 / 7000.0) * (0.90 / 0.95), torque.vazaoArLbMin(),
                d.combustao().vazaoArLbMin() * 1e-4);
        assertNull(calcular(Fixtures.casoB()).pontoTorque(), "sem rotação de torque, sem ponto");
    }

    @Test
    void torqueCambioUsaOMaiorTorque() {
        // Caso B: 401 N·m na potência (SPEC 7.6). Com VE de torque 0,99, o ponto de torque passa disso.
        EntradaDimensionamento entrada = Fixtures.casoB();
        assertEquals(Cor.VERDE, torqueCambio(entrada.comCambio(cambio(450.0))).cor(), "401 / 450 = 89%");
        assertEquals(Cor.AMARELO, torqueCambio(entrada.comCambio(cambio(420.0))).cor(), "401 / 420 = 95%");
        assertEquals(Cor.VERMELHO, torqueCambio(entrada.comCambio(cambio(380.0))).cor(), "401 / 380 = 106%");

        EntradaDimensionamento comPontoDeTorque = comTorque(entrada, 5000).comVeTorque(0.99).comCambio(cambio(450.0));
        Dimensionamento d = calcular(comPontoDeTorque);
        Avaliacao avaliacao = d.avaliacao(Indicador.TORQUE_CAMBIO).orElseThrow();
        assertEquals(d.pontoTorque().torqueNm() / 450.0, avaliacao.valor(), 1e-9, "vale o maior dos dois torques");
    }

    @Test
    void semCambioOuSemLimiteSoAviso() {
        Resultado<Dimensionamento> semCambio = CalculoDimensionamento.calcular(Fixtures.casoB());
        assertTrue(semCambio.valor().avaliacao(Indicador.TORQUE_CAMBIO).isEmpty());
        assertTrue(semCambio.avisos().stream().map(Aviso::mensagem)
                .anyMatch(m -> m.startsWith("Torque × câmbio não avaliado: escolha o câmbio do catálogo")));
        Resultado<Dimensionamento> semLimite = CalculoDimensionamento.calcular(Fixtures.casoB().comCambio(cambio(null)));
        assertTrue(semLimite.avisos().stream().map(Aviso::mensagem)
                .anyMatch(m -> m.contains("não tem torque máximo cadastrado")));
    }

    @Test
    void turboComMapaNoSemaforo() {
        Dimensionamento d = calcular(Fixtures.casoB().comTurbo(Fixtures.turbo("Fictício", Fixtures.mapaFicticio())));
        assertEquals(0.3101, d.noMapa().potencia().vazaoCorrigidaKgS(), 0.0005, "0,284 kg/s corrigidos para o mapa");
        assertEquals(0.77, d.noMapa().potencia().eficiencia(), 1e-12);
        assertEquals(Cor.VERDE, d.avaliacao(Indicador.MAPA_COMPRESSOR).orElseThrow().cor());
        assertTrue(d.recomendacoes().stream().anyMatch(r -> r.startsWith("Turbo: compressor para 37,6 lb/min a PR 2,91")));

        // A 4.500 rpm o mesmo turbo trabalha à esquerda do surge: o ponto de torque fica fora do mapa.
        Dimensionamento torqueBaixo = calcular(comTorque(Fixtures.casoB(), 4500)
                .comTurbo(Fixtures.turbo("Fictício", Fixtures.mapaFicticio())));
        assertEquals("à esquerda da linha de surge", torqueBaixo.noMapa().torque().foraDoMapa());
        assertEquals(Cor.VERMELHO, torqueBaixo.avaliacao(Indicador.MAPA_COMPRESSOR).orElseThrow().cor());
    }

    @Test
    void turboSemMapaFicaAmarelo() {
        Dimensionamento d = calcular(Fixtures.casoB().comTurbo(Fixtures.turbo("Sem mapa", null)));
        Avaliacao avaliacao = d.avaliacao(Indicador.MAPA_COMPRESSOR).orElseThrow();
        assertEquals(Cor.AMARELO, avaliacao.cor());
        assertTrue(avaliacao.mensagem().startsWith("Turbo Sem mapa sem mapa cadastrado"), avaliacao.mensagem());
        assertNull(d.noMapa());
        assertTrue(d.compressor() != null, "os pontos do compressor saem mesmo sem mapa (para sugerir turbos)");
    }

    @Test
    void pontosDoCompressor() {
        Dimensionamento d = calcular(comTorque(Fixtures.casoB(), 5000));
        Dimensionamento.PontosCompressor pontos = d.compressor();
        assertEquals(Conversoes.cParaK(25.0), pontos.temperaturaEntradaK(), 1e-9);
        assertEquals(d.ladoDoAr().patmKpa() - Parametros.padrao().perdaAdmissaoKpa(), pontos.pressaoEntradaKpa(), 1e-9);
        assertEquals(1, pontos.turbosEmParalelo());
        assertEquals(d.combustao().vazaoArKgS(), pontos.vazaoPotenciaKgS(), 1e-12);
        assertEquals(d.pontoTorque().vazaoArKgS(), pontos.vazaoTorqueKgS(), 1e-12);
        assertNull(calcular(Fixtures.casoD(0)).compressor(), "aspirado não tem compressor");
    }

    @Test
    void biturboDivideAVazao() {
        EntradaDimensionamento casoB = Fixtures.casoB();
        EntradaDimensionamento biturbo = casoB.comMotor(casoB.motor().paraConstrutor().aspiracao(Aspiracao.BITURBO)
                .construir());
        Dimensionamento d = calcular(biturbo);
        assertEquals(2, d.compressor().turbosEmParalelo());
        assertEquals(d.combustao().vazaoArKgS() / 2, d.compressor().vazaoPotenciaKgS(), 1e-12);
    }

    private static Avaliacao torqueCambio(EntradaDimensionamento entrada) {
        return calcular(entrada).avaliacao(Indicador.TORQUE_CAMBIO).orElseThrow();
    }

    private static Cambio cambio(Double torqueMaximoNm) {
        return new Cambio("Câmbio fictício", TipoCambio.MANUAL, torqueMaximoNm, "Ficha fictícia de teste", null);
    }
}
