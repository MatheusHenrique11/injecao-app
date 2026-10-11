package br.com.calibra.dados.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.combustivel.EtanolHidratado;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.dados.CasoBDados;

/**
 * "JSON de ouro": um cálculo do caso B gravado pela versão 0.2.0 fica guardado neste arquivo e precisa
 * continuar sendo lido por todas as versões seguintes. Se alguém renomear ou tirar um campo do core,
 * este teste falha, e é preciso criar uma migração dos dados gravados (decisão 47).
 *
 * <p>O arquivo só é regravado de propósito: {@code mvn -pl calibra-dados test -Dtest=JsonDeOuroTest
 * -Dcalibra.gerarJsonDeOuro=true}. Nunca regravar para "fazer o teste passar".
 */
class JsonDeOuroTest {

    private static final String RECURSO = "/br/com/calibra/dados/json/caso-b-0.2.0.json";

    /** Como o projeto grava um cálculo: a entrada e o resultado com a memória. */
    record CalculoGravado(EntradaDimensionamento entrada, Resultado<Dimensionamento> resultado) {
    }

    @Test
    @EnabledIfSystemProperty(named = "calibra.gerarJsonDeOuro", matches = "true")
    void gerarArquivo() throws IOException {
        EntradaDimensionamento entrada = CasoBDados.entrada();
        CalculoGravado gravado = new CalculoGravado(entrada, CalculoDimensionamento.calcular(entrada));
        Path destino = Path.of(System.getProperty("basedir", ""), "src", "test", "resources")
                .resolve(RECURSO.substring(1));
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, Json.escreverFormatado(gravado) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    @Test
    void calculoGravadoNa020ContinuaLegivel() throws IOException {
        CalculoGravado gravado;
        try (InputStream dados = JsonDeOuroTest.class.getResourceAsStream(RECURSO)) {
            assertNotNull(dados, "falta o arquivo " + RECURSO);
            gravado = Json.ler(new String(dados.readAllBytes(), StandardCharsets.UTF_8), CalculoGravado.class);
        }
        EntradaDimensionamento entrada = gravado.entrada();
        assertEquals(4, entrada.motor().numeroCilindros());
        assertEquals(92.8, entrada.motor().cursoMm(), 1e-9);
        assertEquals("EHC", entrada.motor().combustivel().codigo());
        assertEquals(0.93, ((EtanolHidratado) entrada.motor().combustivel().composicao()).fracaoMassicaEtanol(), 1e-9);
        assertEquals(400.0, Conversoes.kwParaCv(entrada.meta().potenciaKw()), 1e-6);
        assertEquals(1050.0, entrada.bico().vazaoNominalCm3Min(), 1e-9);

        Resultado<Dimensionamento> resultado = gravado.resultado();
        assertEquals("1.0.0", resultado.versaoNucleo());
        Dimensionamento d = resultado.valor();
        assertEquals(37.57, d.combustao().vazaoArLbMin(), 37.57 * 0.01, "Wa do golden B");
        assertEquals(241.0, d.ladoDoAr().mapKpa(), 241.0 * 0.01, "MAP do golden B");
        assertEquals(Cor.VERMELHO, d.avaliacao(Indicador.DC_BICO).orElseThrow().cor(), "DC de 86%");

        // Recalcular a entrada lida dá o mesmo resultado (1%): a entrada voltou inteira.
        Dimensionamento novo = CalculoDimensionamento.calcular(entrada).valor();
        assertEquals(d.ladoDoAr().mapKpa(), novo.ladoDoAr().mapKpa(), d.ladoDoAr().mapKpa() * 0.01);
        assertEquals(d.bicoAvaliado().dutyCycle(), novo.bicoAvaliado().dutyCycle(), 0.01);
    }
}
