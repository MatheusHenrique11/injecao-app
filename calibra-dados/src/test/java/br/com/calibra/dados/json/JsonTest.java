package br.com.calibra.dados.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.type.TypeReference;

import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.combustivel.CombustiveisPadrao;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.memoria.VersaoNucleo;
import br.com.calibra.core.motor.RodaFonica;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Recurso;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.dados.CasoBDados;
import br.com.calibra.dados.ErroDados;

/** O JSON grava e lê de volta todos os tipos do core sem perder nada (records iguais). */
class JsonTest {

    private static final TypeReference<Resultado<Dimensionamento>> RESULTADO = new TypeReference<>() {
    };

    @Test
    void entradaDoCasoBVaiEVolta() {
        EntradaDimensionamento entrada = CasoBDados.entrada();
        assertEquals(entrada, Json.ler(Json.escrever(entrada), EntradaDimensionamento.class));
    }

    @Test
    void resultadoComMemoriaEAvisosVaiEVolta() {
        Resultado<Dimensionamento> resultado = CalculoDimensionamento.calcular(CasoBDados.entrada());
        Resultado<Dimensionamento> lido = Json.ler(Json.escrever(resultado), RESULTADO);
        assertEquals(resultado, lido);
        assertFalse(lido.memoria().isEmpty());
        assertEquals(VersaoNucleo.ATUAL, lido.versaoNucleo());
    }

    static List<Combustivel> combustiveis() {
        return CombustiveisPadrao.todos();
    }

    @ParameterizedTest
    @MethodSource("combustiveis")
    void cadaCombustivelVaiEVoltaComATipoDeComposicao(Combustivel combustivel) {
        String json = Json.escrever(combustivel);
        assertTrue(json.contains("\"tipo\":\""), json);
        Combustivel lido = Json.ler(json, Combustivel.class);
        assertEquals(combustivel, lido);
        assertEquals(combustivel.afrEstequiometrico(), lido.afrEstequiometrico(), 1e-12);
    }

    @Test
    void pecasComTabelasVaoEVoltam() {
        TempoMorto tabela = new TempoMorto(List.of(new TempoMorto.Ponto(12, 300, 1.10), new TempoMorto.Ponto(14, 300, 0.90)));
        Bico bico = new Bico("Bico de catálogo", 1050, 300, 0.70, tabela, 12.0, "ficha do fabricante",
                LocalDate.of(2026, 10, 1));
        Bomba bomba = new Bomba("Bomba", List.of(new Bomba.Ponto(300, 13.5, 255), new Bomba.Ponto(500, 13.5, 210)),
                "curva do fabricante", null);
        Injecao injecao = new Injecao("Marca", "Modelo", 8, 8, 8,
                Set.of(Recurso.CONTROLE_PRESSAO_TURBO, Recurso.SENSOR_ETANOL), "manual", LocalDate.of(2026, 9, 30));

        assertEquals(bico, Json.ler(Json.escrever(bico), Bico.class));
        assertEquals(bomba, Json.ler(Json.escrever(bomba), Bomba.class));
        assertEquals(injecao, Json.ler(Json.escrever(injecao), Injecao.class));
    }

    @Test
    void naNVoltaComoNaN() {
        // Tempo morto fixo tem tensão e pressão "não se aplica" (NaN); BSFC também, no caminho k_ar.
        TempoMorto fixo = TempoMorto.fixo(1.0);
        assertEquals(fixo, Json.ler(Json.escrever(fixo), TempoMorto.class));
        Parametros padrao = Parametros.padrao();
        String json = Json.escrever(padrao);
        assertTrue(json.contains("\"bsfc\":\"NaN\""), json);
        assertTrue(Double.isNaN(Json.ler(json, Parametros.class).bsfc()));
    }

    /** Regra 2.9: ponto decimal em arquivos e JSON. */
    @Test
    void numerosComPontoDecimal() {
        String json = Json.escrever(LimiaresSemaforo.padrao());
        assertTrue(json.contains("\"dcVerdeAte\":0.8"), json);
        assertFalse(json.contains("0,8"));
    }

    @Test
    void datasNoFormatoIso() {
        Justificativa justificativa = new Justificativa(Indicador.DC_BICO, "Bico maior a caminho",
                LocalDateTime.of(2026, 10, 11, 18, 30, 5));
        String json = Json.escrever(justificativa);
        assertTrue(json.contains("\"dataHora\":\"2026-10-11T18:30:05\""), json);
        assertEquals(justificativa, Json.ler(json, Justificativa.class));
        assertTrue(Json.escrever(CombustiveisPadrao.E32).contains("\"vigenteDesde\":\"2026-08-01\""));
    }

    @Test
    void campoDesconhecidoEIgnorado() {
        String json = "{\"numeroDeVersaoFutura\":3,\"dentesTotais\":60,\"dentesFaltantes\":2,\"sensorFase\":true}";
        assertEquals(60, Json.ler(json, RodaFonica.class).dentesTotais());
    }

    @Test
    void jsonQuebradoDaErroDados() {
        ErroDados erro = assertThrows(ErroDados.class, () -> Json.ler("{\"dentesTotais\":", Bico.class));
        assertTrue(erro.getMessage().startsWith("JSON inválido para Bico"), erro.getMessage());
        assertThrows(ErroDados.class, () -> Json.ler("{\"dataHora\":\"ontem\"}", Justificativa.class));
    }
}
