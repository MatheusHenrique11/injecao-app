package br.com.calibra.core.semaforo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Fronteiras do semáforo da SPEC 7.4. */
class SemaforoTest {

    private final LimiaresSemaforo l = LimiaresSemaforo.padrao();

    @ParameterizedTest
    @CsvSource({"0.70,VERDE", "0.80,VERDE", "0.8001,AMARELO", "0.85,AMARELO", "0.8501,VERMELHO", "0.863,VERMELHO"})
    void dcDoBico(double dc, Cor esperada) {
        assertEquals(esperada, Semaforo.dcBico(dc, l).cor());
    }

    @ParameterizedTest
    @CsvSource({"0.50,VERDE", "0.30,VERDE", "0.2999,AMARELO", "0.15,AMARELO", "0.1499,VERMELHO", "-0.10,VERMELHO"})
    void folgaDaBomba(double folga, Cor esperada) {
        assertEquals(esperada, Semaforo.folgaBomba(folga, l).cor());
    }

    @ParameterizedTest
    @CsvSource({
        // bmep, sobrealimentado, pistões forjados, bielas forjadas, cor
        "22.0,true,false,false,VERDE",
        "25.2,true,false,false,AMARELO",
        "25.3,true,false,false,AMARELO",
        "25.31,true,false,false,VERMELHO",
        "27.0,true,true,false,VERMELHO",
        "27.0,true,false,true,VERMELHO",
        "27.0,true,true,true,AMARELO",
        "13.0,false,false,false,VERDE",
        "14.95,false,false,false,AMARELO",
        "15.0,false,false,false,VERMELHO"})
    void bmepContraAReferencia(double bmep, boolean sobrealimentado, boolean pistoes, boolean bielas, Cor esperada) {
        assertEquals(esperada, Semaforo.bmep(bmep, sobrealimentado, pistoes, bielas, l).cor());
    }

    @Test
    void bmepComInternosForjadosAvisaNaMensagem() {
        Avaliacao a = Semaforo.bmep(27.0, true, true, true, l);
        assertTrue(a.mensagem().contains("forjados"), a.mensagem());
    }

    @ParameterizedTest
    @CsvSource({"14.0,VERDE", "20.0,VERDE", "20.01,AMARELO", "21.7,AMARELO", "25.0,AMARELO", "25.01,VERMELHO"})
    void velocidadeDoPistao(double ms, Cor esperada) {
        assertEquals(esperada, Semaforo.velocidadePistao(ms, l).cor());
    }

    @ParameterizedTest
    @CsvSource({"300,241,VERDE", "289.2,241,VERDE", "289,241,AMARELO", "241,241,AMARELO", "240,241,VERMELHO"})
    void faixaDoSensorMap(double faixa, double mapMaximo, Cor esperada) {
        assertEquals(esperada, Semaforo.faixaSensorMap(faixa, mapMaximo, l).cor());
    }

    @ParameterizedTest
    @CsvSource({"0.85,VERDE", "0.90,VERDE", "0.953,AMARELO", "0.98,AMARELO", "1.024,VERMELHO"})
    void aspiradoMapSobrePatm(double razao, Cor esperada) {
        assertEquals(esperada, Semaforo.aspiradoMapPatm(razao, l).cor());
    }

    @Test
    void saidasDaInjecao() {
        assertEquals(Cor.VERDE, Semaforo.saidasInjecao("X", 4, 8, 4, 4, null, 4, true).cor());
        Avaliacao faltaBico = Semaforo.saidasInjecao("FT450", 12, 6, 6, 4, null, 12, true);
        assertEquals(Cor.VERMELHO, faltaBico.cor());
        assertTrue(faltaBico.mensagem().contains("A injeção FT450 tem 6 saídas de bico; este motor pede 12."),
                faltaBico.mensagem());
        assertTrue(faltaBico.mensagem().contains("saídas de ignição"), faltaBico.mensagem());
    }

    @Test
    void saidasDaInjecaoConferemOLimiteEmSequencial() {
        Avaliacao limite = Semaforo.saidasInjecao("X", 12, 16, 12, 12, 8, 12, true);
        assertEquals(Cor.VERMELHO, limite.cor());
        assertTrue(limite.mensagem().contains("controla até 8 cilindros em sequencial; este motor tem 12"),
                limite.mensagem());
        assertEquals(Cor.VERDE, Semaforo.saidasInjecao("X", 12, 16, 6, 12, 8, 12, false).cor(),
                "sem modo sequencial o limite não se aplica");
    }

    @Test
    void compressorSemMapaEhAmarelo() {
        assertEquals(Cor.AMARELO, Semaforo.compressorSemMapa().cor());
    }

    @Test
    void vermelhoSemJustificativaBloqueia() {
        List<Avaliacao> avaliacoes = List.of(
                Semaforo.dcBico(0.86, l),
                Semaforo.velocidadePistao(21.7, l),
                Semaforo.bmep(30, true, false, false, l));
        assertEquals(List.of(Indicador.DC_BICO, Indicador.BMEP),
                Semaforo.vermelhosSemJustificativa(avaliacoes, List.of()));

        Justificativa j = new Justificativa(Indicador.DC_BICO, "Cliente vai trocar os bicos depois.", LocalDateTime.now());
        assertEquals(List.of(Indicador.BMEP), Semaforo.vermelhosSemJustificativa(avaliacoes, List.of(j)));
    }

    @Test
    void justificativaVaziaNaoVale() {
        assertThrows(IllegalArgumentException.class,
                () -> new Justificativa(Indicador.DC_BICO, "   ", LocalDateTime.now()));
    }

    @Test
    void limiaresForaDeOrdemSaoRecusados() {
        assertThrows(IllegalArgumentException.class,
                () -> new LimiaresSemaforo(0.90, 0.85, 0.30, 0.15, 13, 22, 1.15, 20, 25, 1.2, 1.0, 0.90, 0.98));
    }
}
