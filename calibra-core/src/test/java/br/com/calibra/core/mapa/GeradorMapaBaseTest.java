package br.com.calibra.core.mapa;

import static br.com.calibra.core.Fixtures.assertProximo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.Fixtures;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.motor.Comando;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.ModeloCombustivel;
import br.com.calibra.core.pecas.PerfilInjecao;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.Uso;
import br.com.calibra.core.semaforo.Cor;

/**
 * O gerador do mapa base (fase 4, bloco 4.3; decisão 294). As contas de cada célula são as da SPEC 7.1: com o ar na
 * Tm e sem a riqueza extra, a célula da potência máxima repete o dimensionamento dos casos golden.
 */
class GeradorMapaBaseTest {

    private static final double EPS = 1e-9;
    private static final List<Double> ROTACOES_RELATIVAS = List.of(0.15, 0.25, 0.40, 0.60, 0.80, 1.00, 1.20, 1.40,
            1.60, 2.00);
    private static final List<Double> CARGAS_RELATIVAS = List.of(0.20, 0.35, 0.50, 0.70, 0.85, 1.00, 1.50, 2.00, 3.00);

    /** Um modelo como os que vêm com o app: a VE da marcha lenta até 35 kPa, linear até o pico em 100 kPa. */
    private static ModeloVe modelo(double lenta, double pico) {
        Tabela ve = Tabela.preencher(ROTACOES_RELATIVAS, CARGAS_RELATIVAS,
                (r, c) -> lenta + (pico - lenta) * Math.clamp((c - 0.35) / 0.65, 0.0, 1.0));
        return new ModeloVe("4 válvulas de teste", 4, Set.of(Comando.ORIGINAL), ve, "teste", null);
    }

    private static Injecao injecao(PerfilInjecao perfil) {
        return new Injecao("Marca Teste", "ECU", 8, 8, null, Set.of(), perfil, "teste", null);
    }

    private static PerfilInjecao perfilMs(Double temperaturaReferenciaC, Boolean tempoMortoNaTabela) {
        return new PerfilInjecao(null, null, ModeloCombustivel.MS, List.of(), temperaturaReferenciaC,
                tempoMortoNaTabela, "", "");
    }

    /** O caso com a rotação de torque máximo (a meta dos casos golden não tem). */
    private static EntradaDimensionamento comTorque(EntradaDimensionamento caso, int rpmTorque) {
        return caso.comMeta(caso.meta().comRotacaoTorque(rpmTorque));
    }

    private static Dimensionamento dimensionar(EntradaDimensionamento entrada) {
        return CalculoDimensionamento.calcular(entrada).valor();
    }

    private static EntradaMapaBase entrada(EntradaDimensionamento dim, Injecao injecao, FonteIgnicao ignicao,
            RegrasMapaBase regras) {
        return new EntradaMapaBase(dim, injecao, modelo(0.50, 0.97), ignicao, regras, null);
    }

    private static boolean temAlerta(MapaBase mapa, AlertaMapa.Codigo codigo, Cor cor) {
        return mapa.alertas().stream().anyMatch(a -> a.codigo() == codigo && a.cor() == cor);
    }

    // ---------- Casos golden (SPEC 7.6) ----------

    /**
     * Com o ar de referência na Tm e sem a riqueza extra, a célula (rpm da potência, MAP do dimensionamento) dá o TI e
     * o DC do dimensionamento: 13,81 + 1,0 = 14,81 ms e 86% no caso B; 14,19 + 1,0 = 15,19 ms e 70% no caso C.
     */
    @Test
    void celulaDaPotenciaRepeteODimensionamentoDosCasosGolden() {
        conferirGolden(comTorque(Fixtures.casoB(), 5000), 14.81, 0.86);
        conferirGolden(comTorque(Fixtures.casoC(), 4000), 15.19, 0.70);
    }

    private static void conferirGolden(EntradaDimensionamento caso, double tiSpec, double dcSpec) {
        Dimensionamento d = dimensionar(caso);
        EntradaMapaBase entrada = entrada(caso, injecao(perfilMs(d.ladoDoAr().tmC(), true)), null,
                RegrasMapaBase.padrao().semRiquezaExtra());
        int rpm = caso.meta().rotacaoPotenciaMaxima();
        Resultado<CelulaMapa> celula = GeradorMapaBase.memoriaDaCelula(entrada, rpm, d.ladoDoAr().mapKpa());
        assertEquals(caso.ve(), celula.valor().ve(), EPS, "a VE do projeto manda no ponto da potência");
        assertEquals(caso.lambdaAlvo(), celula.valor().lambda(), EPS);
        assertEquals(d.bicoAvaliado().tiTotalMs(), celula.valor().tiTabelaMs(), 1e-6, "o mesmo TI do dimensionamento");
        assertEquals(d.bicoAvaliado().dutyCycle(), celula.valor().dutyCycle(), 1e-9, "o mesmo DC");
        assertProximo(tiSpec, celula.valor().tiTabelaMs(), "TI da SPEC 7.6");
        assertProximo(dcSpec, celula.valor().dutyCycle(), "DC da SPEC 7.6");
        assertTrue(celula.memoria().stream().anyMatch(p -> p.grandeza().equals("Massa de ar por cilindro")),
                "a memória da célula tem as contas");
    }

    @Test
    void vePoeOTorqueEAPotenciaNosPontosDelesEOModeloNoResto() {
        EntradaDimensionamento caso = comTorque(Fixtures.casoB(), 5000).comVeTorque(0.98);
        Dimensionamento d = dimensionar(caso);
        EntradaMapaBase entrada = entrada(caso, null, null, RegrasMapaBase.padrao());
        double map = d.ladoDoAr().mapKpa();
        assertEquals(0.98, GeradorMapaBase.memoriaDaCelula(entrada, 5000, map).valor().ve(), EPS, "torque");
        assertEquals(0.95, GeradorMapaBase.memoriaDaCelula(entrada, 7000, map).valor().ve(), EPS, "potência");
        assertEquals((0.98 + 0.95) / 2, GeradorMapaBase.memoriaDaCelula(entrada, 6000, map).valor().ve(), EPS,
                "linear entre os dois");
        // Na marcha lenta, a forma do modelo (50% em 35 kPa) com o fator do torque (0,98 ÷ 0,97).
        assertEquals(0.50 * 0.98 / 0.97, GeradorMapaBase.memoriaDaCelula(entrada, 900, 35).valor().ve(), EPS);

        // Trocar o modelo muda a forma, não os pontos do projeto (SPEC 7.5: o modelo é tabela editável).
        EntradaMapaBase outroModelo = new EntradaMapaBase(caso, null, modelo(0.40, 0.90), null,
                RegrasMapaBase.padrao(), null);
        assertEquals(0.95, GeradorMapaBase.memoriaDaCelula(outroModelo, 7000, map).valor().ve(), EPS);
        assertEquals(0.40 * 0.98 / 0.90, GeradorMapaBase.memoriaDaCelula(outroModelo, 900, 35).valor().ve(), EPS);
    }

    // ---------- Lambda (SPEC 7.5) ----------

    @Test
    void lambdaSegueAsRegrasDaSpec() {
        MapaBase turbo = GeradorMapaBase.gerar(entrada(comTorque(Fixtures.casoB(), 5000), null, null,
                RegrasMapaBase.padrao())).valor();
        assertEquals(1.00, lambdaEm(turbo, 50), EPS, "abaixo de 70 kPa: 1,00");
        assertEquals(1.00, lambdaEm(turbo, 70), EPS);
        assertEquals(1.00 + (10.0 / 30) * (0.87 - 1.00), lambdaEm(turbo, 80), EPS,
                "de 70 a 100 kPa: até o do aspirado em plena carga (EHC: 0,87)");
        assertEquals(1.00 + (20.0 / 30) * (0.87 - 1.00), lambdaEm(turbo, 90), EPS, "em 90 kPa ainda sem a riqueza");
        assertEquals(0.87 - 0.03, lambdaEm(turbo, 100), EPS, "acima de 90 kPa: 0,03 mais rico");
        assertEquals(0.80 - 0.03, lambdaEm(turbo, 150), EPS, "acima de 100 kPa: o alvo sob pressão, −0,03");

        EntradaDimensionamento nautico = comTorque(Fixtures.casoB(), 5000);
        nautico = nautico.comMeta(new Meta(nautico.meta().potenciaKw(), nautico.meta().pontoMedicao(), null, 7000,
                5000, Uso.NAUTICO));
        Resultado<MapaBase> noBarco = GeradorMapaBase.gerar(entrada(nautico, null, null, RegrasMapaBase.padrao()));
        assertEquals(0.80 - 0.03 - 0.02, lambdaEm(noBarco.valor(), 150), EPS, "náutico: mais 0,02");
        assertEquals(1.00, lambdaEm(noBarco.valor(), 50), EPS);
        assertTrue(noBarco.avisos().stream().anyMatch(a -> a.mensagem().contains("em Náutico, o mapa usa o mesmo")));

        MapaBase aspirado = GeradorMapaBase.gerar(entrada(comTorque(Fixtures.casoD(0), 5000), null, null,
                RegrasMapaBase.padrao())).valor();
        assertEquals(0.87 - 0.03, lambdaEm(aspirado, 100), EPS, "aspirado: o alvo de plena carga do projeto");
        assertEquals(0.87 - 0.03, lambdaEm(aspirado, 105), EPS, "acima de 100 kPa, ainda o do aspirado");
    }

    private static double lambdaEm(MapaBase mapa, double carga) {
        int linha = mapa.lambda().cargas().indexOf(carga);
        assertTrue(linha >= 0, "o eixo tem " + carga + " kPa");
        return mapa.lambda().valor(linha, 0);
    }

    // ---------- Eixos e travas ----------

    @Test
    void eixosPadraoDaSpec() {
        List<Double> ate7300 = GeradorMapaBase.rotacoesPadrao(7300);
        assertEquals(List.of(500.0, 1000.0), ate7300.subList(0, 2));
        assertEquals(List.of(6500.0, 7000.0, 7300.0), ate7300.subList(12, 15), "a máxima entra no fim");
        assertEquals(15, GeradorMapaBase.rotacoesPadrao(7500).size(), "500 a 7.500 de 500 em 500");
        List<Double> turbo = GeradorMapaBase.cargasPadrao(true, 241.2);
        assertEquals(20.0, turbo.getFirst());
        assertEquals(270.0, turbo.getLast(), "1,1 × 241,2 = 265,3: arredonda para cima, de 10 em 10");
        assertEquals(26, turbo.size());
        List<Double> aspirado = GeradorMapaBase.cargasPadrao(false, 94.3);
        assertEquals(List.of(20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0, 105.0), aspirado);

        MapaBase mapa = GeradorMapaBase.gerar(entrada(comTorque(Fixtures.casoB(), 5000), null, null,
                RegrasMapaBase.padrao())).valor();
        assertEquals(GeradorMapaBase.rotacoesPadrao(7500), mapa.combustivel().rotacoes());
        assertTrue(mapa.combustivel().completa() && mapa.ve().completa() && mapa.dutyCycle().completa());
        assertEquals(mapa.combustivel().cargas(), mapa.ignicao().cargas(), "sem perfil, todas nos mesmos eixos");
    }

    @Test
    void travasDaPressaoEDoLimitador() {
        EntradaDimensionamento caso = comTorque(Fixtures.casoB(), 5000);
        Dimensionamento d = dimensionar(caso);
        double prevista = d.sensorMap().mapMaximoPrevistoKpa() - d.ladoDoAr().patmKpa();
        MapaBase semMola = GeradorMapaBase.gerar(entrada(caso, null, null, RegrasMapaBase.padrao())).valor();
        assertEquals(prevista, semMola.travas().pressaoPrevistaKpa(), EPS);
        assertEquals(0.5 * prevista, semMola.travas().pressaoAlvoKpa(), EPS, "sem mola: 50% da prevista");
        assertNull(semMola.travas().molaWastegateKpa());
        assertEquals(7000, semMola.travas().limitadorRpm(), "500 rpm abaixo da máxima (7.500)");

        MapaBase molaFraca = GeradorMapaBase.gerar(new EntradaMapaBase(caso, null, modelo(0.5, 0.97), null,
                RegrasMapaBase.padrao(), 50.0)).valor();
        assertEquals(50.0, molaFraca.travas().pressaoAlvoKpa(), EPS, "a mola, se for menor");
        MapaBase molaForte = GeradorMapaBase.gerar(new EntradaMapaBase(caso, null, modelo(0.5, 0.97), null,
                RegrasMapaBase.padrao(), 120.0)).valor();
        assertEquals(0.5 * prevista, molaForte.travas().pressaoAlvoKpa(), EPS, "50% da prevista, se for menor");

        MapaBase aspirado = GeradorMapaBase.gerar(entrada(comTorque(Fixtures.casoD(0), 5000), null, null,
                RegrasMapaBase.padrao())).valor();
        assertTrue(Double.isNaN(aspirado.travas().pressaoAlvoKpa()), "aspirado: sem pressão de turbo");
    }

    // ---------- Ignição (SPEC 7.5: o app não inventa avanço) ----------

    @Test
    void semFonteAIgnicaoFicaVaziaEVermelha() {
        MapaBase mapa = GeradorMapaBase.gerar(entrada(comTorque(Fixtures.casoB(), 5000), null, null,
                RegrasMapaBase.padrao())).valor();
        assertTrue(mapa.ignicao().semValores());
        assertTrue(mapa.ignicaoFonte().semValores());
        assertTrue(temAlerta(mapa, AlertaMapa.Codigo.IGNICAO_SEM_FONTE, Cor.VERMELHO));
        assertTrue(mapa.vermelhos().stream().anyMatch(a -> a.codigo() == AlertaMapa.Codigo.IGNICAO_SEM_FONTE));
    }

    @Test
    void comFonteAIgnicaoSaiDelaSemEstenderEComAsReducoes() {
        EntradaDimensionamento caso = comTorque(Fixtures.casoB(), 5000);
        Tabela avanco = Tabela.preencher(List.of(1000.0, 7000.0), List.of(30.0, 200.0), (r, c) -> 20.0);
        FonteIgnicao fonte = FonteIgnicao.digitada(avanco, "E32", 25.0);

        MapaBase semReducao = GeradorMapaBase.gerar(entrada(caso, null, fonte, RegrasMapaBase.padrao())).valor();
        int linha150 = semReducao.ignicao().cargas().indexOf(150.0);
        int coluna3000 = semReducao.ignicao().rotacoes().indexOf(3000.0);
        assertEquals(20.0, semReducao.ignicao().valor(linha150, coluna3000), EPS, "sem reduções: igual à fonte");
        assertTrue(temAlerta(semReducao, AlertaMapa.Codigo.REDUCOES_IGNICAO, Cor.AMARELO), "avisa que faltam");
        assertNull(semReducao.ignicao().valor(semReducao.ignicao().cargas().indexOf(250.0), coluna3000),
                "acima dos 200 kPa da fonte: vazia, não estende");
        assertNull(semReducao.ignicao().valor(linha150, semReducao.ignicao().rotacoes().indexOf(7500.0)));
        assertTrue(temAlerta(semReducao, AlertaMapa.Codigo.IGNICAO_INCOMPLETA, Cor.VERMELHO));

        RegrasMapaBase regras = RegrasMapaBase.padrao().comReducoes(1.0, 0.5, 2.0);
        Resultado<MapaBase> reduzido = GeradorMapaBase.gerar(entrada(caso, null, fonte, regras));
        MapaBase mapa = reduzido.valor();
        Dimensionamento d = dimensionar(caso);
        double aspirado = 25 + Parametros.padrao().acrescimoTmAspiradoC();
        double ar150 = aspirado + (d.ladoDoAr().tmC() - aspirado) * 50 / (d.ladoDoAr().mapKpa() - 100);
        double esperado150 = 20 - (1.0 * 5 + 0.5 * (ar150 - 25) / 10 + 2.0);
        assertEquals(esperado150, mapa.ignicao().valor(linha150, coluna3000), 1e-9,
                "5 × 1° (50 kPa acima de 100), a temperatura do ar e 2° pela troca E32 → etanol");
        int linha50 = mapa.ignicao().cargas().indexOf(50.0);
        assertEquals(20 - (0.5 * (aspirado - 25) / 10 + 2.0), mapa.ignicao().valor(linha50, coluna3000), 1e-9);
        assertEquals(20.0, mapa.ignicaoFonte().valor(linha150, coluna3000), EPS, "a fonte fica, para comparar");
        assertFalse(temAlerta(mapa, AlertaMapa.Codigo.REDUCOES_IGNICAO, Cor.AMARELO));

        // A redução nunca soma avanço: a mesma fonte, num projeto do mesmo combustível e com o ar mais frio.
        FonteIgnicao quente = FonteIgnicao.digitada(avanco, "EHC", 60.0);
        MapaBase semSomar = GeradorMapaBase.gerar(entrada(caso, null, quente, regras.comReducoes(0.0, 0.5, 2.0)))
                .valor();
        assertEquals(20.0, semSomar.ignicao().valor(linha50, coluna3000), EPS);
    }

    // ---------- Perfil da injeção ----------

    @Test
    void perfilDaInjecaoDaOsEixosEOJeitoDaTabela() {
        EntradaDimensionamento caso = comTorque(Fixtures.casoB(), 5000);
        OrientacaoTabela orientacao = new OrientacaoTabela(OrientacaoTabela.EixoDasColunas.ROTACAO,
                OrientacaoTabela.OrdemDasLinhas.CRESCE_PARA_CIMA);
        List<Double> rotacoes = List.of(800.0, 2000.0, 4000.0, 6000.0, 8000.0);
        List<Double> cargas = List.of(30.0, 60.0, 100.0, 180.0, 260.0);
        PerfilTabela combustivel = new PerfilTabela("Principal", FuncaoTabela.COMBUSTIVEL, TipoCarga.MAP_KPA, "ms",
                rotacoes, cargas, orientacao);
        PerfilInjecao semTempoMorto = new PerfilInjecao(null, null, ModeloCombustivel.MS, List.of(combustivel), 20.0,
                false, "", "");
        MapaBase mapa = GeradorMapaBase.gerar(entrada(caso, injecao(semTempoMorto), null, RegrasMapaBase.padrao()))
                .valor();
        assertEquals(rotacoes, mapa.combustivel().rotacoes());
        assertEquals(cargas, mapa.ignicao().cargas(), "a ignição sem tabela própria segue a do combustível");
        assertFalse(mapa.tempoMortoNaTabela());
        assertEquals(20.0, mapa.temperaturaReferenciaC(), EPS);
        assertFalse(mapa.alertas().stream().anyMatch(a -> a.codigo() == AlertaMapa.Codigo.PERFIL_INCOMPLETO));

        PerfilInjecao comTempoMorto = new PerfilInjecao(null, null, ModeloCombustivel.MS, List.of(combustivel), 20.0,
                true, "", "");
        MapaBase somado = GeradorMapaBase.gerar(entrada(caso, injecao(comTempoMorto), null, RegrasMapaBase.padrao()))
                .valor();
        assertEquals(1.0, somado.combustivel().valor(2, 2) - mapa.combustivel().valor(2, 2), 1e-9,
                "a diferença é o tempo morto do bico (1,0 ms)");
        assertEquals(somado.dutyCycle(), mapa.dutyCycle(), "o DC sempre tem o tempo morto");

        PerfilInjecao porVe = new PerfilInjecao(null, null, ModeloCombustivel.VE, List.of(combustivel), 20.0, null,
                "", "");
        MapaBase emVe = GeradorMapaBase.gerar(entrada(caso, injecao(porVe), null, RegrasMapaBase.padrao())).valor();
        assertEquals(emVe.ve().valor(3, 3) * 100, emVe.combustivel().valor(3, 3), 1e-9, "por VE: a VE em %");
        assertEquals(ModeloCombustivel.VE, emVe.modeloCombustivel());
    }

    @Test
    void perfilIncompletoTpsEEixoCurtoViramAlerta() {
        EntradaDimensionamento caso = comTorque(Fixtures.casoB(), 5000);
        MapaBase semPerfil = GeradorMapaBase.gerar(entrada(caso, injecao(PerfilInjecao.vazio()), null,
                RegrasMapaBase.padrao())).valor();
        assertEquals(ModeloCombustivel.MS, semPerfil.modeloCombustivel());
        assertTrue(semPerfil.tempoMortoNaTabela(), "sem saber, soma o tempo morto: mais rico");
        assertEquals(PerfilInjecao.TEMPERATURA_REFERENCIA_PADRAO_C, semPerfil.temperaturaReferenciaC(), EPS);
        assertEquals(3, semPerfil.alertas().stream().filter(a -> a.codigo() == AlertaMapa.Codigo.PERFIL_INCOMPLETO
                && a.cor() == Cor.AMARELO).count(), "modelo, tempo morto e temperatura");

        OrientacaoTabela orientacao = new OrientacaoTabela(OrientacaoTabela.EixoDasColunas.ROTACAO,
                OrientacaoTabela.OrdemDasLinhas.CRESCE_PARA_BAIXO);
        PerfilTabela curta = new PerfilTabela("Principal", FuncaoTabela.COMBUSTIVEL, TipoCarga.MAP_KPA, "ms",
                List.of(1000.0, 6000.0), List.of(20.0, 200.0), orientacao);
        PerfilTabela porTps = new PerfilTabela("Avanço", FuncaoTabela.IGNICAO, TipoCarga.TPS_PERCENTUAL, "°",
                List.of(1000.0, 6000.0), List.of(0.0, 100.0), orientacao);
        MapaBase alertas = GeradorMapaBase.gerar(entrada(caso, injecao(new PerfilInjecao(null, null,
                ModeloCombustivel.MS, List.of(curta, porTps), 25.0, true, "", "")), null, RegrasMapaBase.padrao()))
                .valor();
        assertTrue(temAlerta(alertas, AlertaMapa.Codigo.EIXO_CURTO, Cor.VERMELHO), "200 kPa < MAP máximo");
        assertTrue(temAlerta(alertas, AlertaMapa.Codigo.EIXO_CURTO, Cor.AMARELO), "6.000 < 7.500 rpm");
        assertTrue(temAlerta(alertas, AlertaMapa.Codigo.TABELA_POR_TPS, Cor.VERMELHO));
        assertEquals(curta.cargas(), alertas.ignicao().cargas(), "a ignição por TPS sai nos eixos do combustível");
    }

    // ---------- Coerência (SPEC 7.6) e alertas do bico ----------

    /** SPEC 7.6: trocar o bico não muda a tabela de VE (muda o tempo de injeção). */
    @Test
    void trocarOBicoNaoMudaAVe() {
        EntradaDimensionamento caso = comTorque(Fixtures.casoB(), 5000);
        MapaBase com1050 = GeradorMapaBase.gerar(entrada(caso, null, null, RegrasMapaBase.padrao())).valor();
        MapaBase com1300 = GeradorMapaBase.gerar(entrada(caso.comBico(Bico.informado("1.300", 1300, 300, 0.70, 1.0)),
                null, null, RegrasMapaBase.padrao())).valor();
        assertEquals(com1050.ve(), com1300.ve());
        assertEquals(com1050.lambda(), com1300.lambda());
        assertNotEquals(com1050.combustivel(), com1300.combustivel());
        assertTrue(com1300.combustivel().valor(10, 10) < com1050.combustivel().valor(10, 10), "bico maior, TI menor");
    }

    @Test
    void dcAltoEDuasBancadasViramVermelho() {
        EntradaDimensionamento caso = comTorque(Fixtures.casoB(), 5000);
        MapaBase mapa = GeradorMapaBase.gerar(entrada(caso, null, null, RegrasMapaBase.padrao())).valor();
        assertTrue(temAlerta(mapa, AlertaMapa.Codigo.DC_ALTO, Cor.VERMELHO), "86% já na potência; mais na máxima");

        MapaBase folgado = GeradorMapaBase.gerar(entrada(caso.comBico(Bico.informado("2.000", 2000, 300, 0.70, 1.0)),
                null, null, RegrasMapaBase.padrao())).valor();
        assertFalse(folgado.alertas().stream().anyMatch(a -> a.codigo() == AlertaMapa.Codigo.DC_ALTO));

        ConfiguracaoMotor duas = caso.motor().paraConstrutor().bicosPorCilindro(2).construir();
        EntradaDimensionamento duasBancadas = caso.comMotor(duas).comBicoSecundario(Bico.informado("440", 440, 300,
                0.70, 1.0));
        MapaBase comDuas = GeradorMapaBase.gerar(entrada(duasBancadas, null, null, RegrasMapaBase.padrao())).valor();
        assertTrue(temAlerta(comDuas, AlertaMapa.Codigo.DUAS_BANCADAS, Cor.VERMELHO));
    }

    @Test
    void semBicoOuSemTorqueNaoGera() {
        EntradaDimensionamento semBico = comTorque(Fixtures.casoB(), 5000).comBico(null);
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class,
                () -> GeradorMapaBase.gerar(entrada(semBico, null, null, RegrasMapaBase.padrao())));
        assertTrue(erro.erros().getFirst().startsWith("O mapa base precisa do bico"), erro.erros()::toString);
        erro = assertThrows(EntradaInvalidaException.class,
                () -> GeradorMapaBase.gerar(entrada(Fixtures.casoB(), null, null, RegrasMapaBase.padrao())));
        assertTrue(erro.erros().getFirst().startsWith("O mapa base precisa da rotação de torque máximo"),
                erro.erros()::toString);
    }

    /** De 3 a 12 cilindros: o mapa sai completo, e a VE do projeto manda no ponto da potência. */
    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5, 6, 8, 10, 12})
    void deTresADozeCilindros(int cilindros) {
        ConfiguracaoMotor motor = Fixtures.motor(cilindros).construir();
        EntradaDimensionamento caso = EntradaDimensionamento.de(motor, Meta.noVirabrequim(35.0 * cilindros, 6000,
                Uso.RUA).comRotacaoTorque(4500), 0.80, 0.95, 25, 300, Parametros.padrao())
                .comBico(Bico.informado("440", 440, 300, 0.70, 1.0));
        EntradaMapaBase entrada = entrada(caso, null, null, RegrasMapaBase.padrao());
        MapaBase mapa = GeradorMapaBase.gerar(entrada).valor();
        assertTrue(mapa.combustivel().completa(), cilindros + " cilindros");
        assertTrue(mapa.combustivel().valores().stream().flatMap(List::stream).allMatch(v -> v > 0 && v < 100));
        double map = dimensionar(caso).ladoDoAr().mapKpa();
        assertEquals(0.95, GeradorMapaBase.memoriaDaCelula(entrada, 6000, map).valor().ve(), EPS);
    }
}
