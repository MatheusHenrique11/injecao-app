package br.com.calibra.core.mapa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import br.com.calibra.core.motor.Comando;

/** Fase 4 (decisão 293): a tabela de rotação × carga, a orientação da tela da injeção e os modelos do catálogo. */
class TabelaTest {

    private static final double EPS = 1e-9;

    /** 2 cargas × 3 rotações: a célula vale rotação/1000 + carga/10. */
    private static Tabela exemplo() {
        return Tabela.preencher(List.of(1000.0, 2000.0, 4000.0), List.of(50.0, 100.0),
                (rotacao, carga) -> rotacao / 1000 + carga / 10);
    }

    @Test
    void interpolaNosPontosEntreElesENuncaExtrapola() {
        Tabela tabela = exemplo();
        assertEquals(6.0, tabela.interpolar(1000, 50).orElseThrow(), EPS, "no ponto");
        assertEquals(14.0, tabela.interpolar(4000, 100).orElseThrow(), EPS, "na última célula");
        assertEquals(1.5 + 7.5, tabela.interpolar(1500, 75).orElseThrow(), EPS, "no meio: plano exato");
        assertEquals(3.0 + 10.0, tabela.interpolar(3000, 100).orElseThrow(), EPS);
        assertTrue(tabela.interpolar(999, 50).isEmpty(), "abaixo do eixo");
        assertTrue(tabela.interpolar(1000, 101).isEmpty(), "acima do eixo");
        assertEquals(6.0, tabela.interpolarNaBorda(500, 20), EPS, "na borda, fora da faixa vale a borda");
        assertEquals(14.0, tabela.interpolarNaBorda(9000, 300), EPS);
    }

    @Test
    void celulaVaziaNaoEntraNaContaNemVira0() {
        Tabela tabela = new Tabela(List.of(1000.0, 2000.0), List.of(50.0, 100.0),
                List.of(Arrays.asList(10.0, null), List.of(20.0, 30.0)));
        assertFalse(tabela.completa());
        assertFalse(tabela.semValores());
        assertEquals(15.0, tabela.interpolar(1000, 75).orElseThrow(), EPS, "na coluna cheia, a vazia nem entra");
        assertTrue(tabela.interpolar(1500, 75).isEmpty(), "a conta precisaria da célula vazia");
        assertTrue(tabela.interpolar(2000, 50).isEmpty());
        assertTrue(Tabela.vazia(List.of(1.0, 2.0), List.of(1.0, 2.0)).semValores());
        assertThrows(IllegalArgumentException.class,
                () -> new Tabela(List.of(1.0, 2.0), List.of(1.0), List.of(List.of(1.0))), "linha curta");
    }

    /** O que vai para a tela da injeção volta igual, nas quatro orientações. */
    @ParameterizedTest
    @EnumSource(OrientacaoTabela.EixoDasColunas.class)
    void naTelaEDaTelaSaoOCaminhoDeIdaEVolta(OrientacaoTabela.EixoDasColunas colunas) {
        Tabela tabela = exemplo();
        for (OrientacaoTabela.OrdemDasLinhas linhas : OrientacaoTabela.OrdemDasLinhas.values()) {
            OrientacaoTabela orientacao = new OrientacaoTabela(colunas, linhas);
            List<List<Double>> tela = orientacao.naTela(tabela);
            assertEquals(tabela, orientacao.daTela(tabela.rotacoes(), tabela.cargas(), tela), orientacao.toString());
            assertEquals(orientacao.eixoDasLinhas(tabela).size(), tela.size());
            assertEquals(orientacao.eixoDasColunas(tabela).size(), tela.getFirst().size());
        }
    }

    @Test
    void rotacaoNasColunasECargaMaiorEmCima() {
        OrientacaoTabela orientacao = new OrientacaoTabela(OrientacaoTabela.EixoDasColunas.ROTACAO,
                OrientacaoTabela.OrdemDasLinhas.CRESCE_PARA_CIMA);
        Tabela tabela = exemplo();
        assertEquals(List.of(100.0, 50.0), orientacao.eixoDasLinhas(tabela));
        assertEquals(List.of(List.of(11.0, 12.0, 14.0), List.of(6.0, 7.0, 9.0)), orientacao.naTela(tabela));

        OrientacaoTabela cargaNasColunas = new OrientacaoTabela(OrientacaoTabela.EixoDasColunas.CARGA,
                OrientacaoTabela.OrdemDasLinhas.CRESCE_PARA_BAIXO);
        assertEquals(List.of(50.0, 100.0), cargaNasColunas.eixoDasColunas(tabela));
        assertEquals(List.of(6.0, 11.0), cargaNasColunas.naTela(tabela).getFirst());

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> orientacao.daTela(
                tabela.rotacoes(), tabela.cargas(), List.of(List.of(1.0, 2.0, 3.0), List.of(1.0, 2.0))));
        assertEquals("a linha 2 tem 2 número(s), e o eixo de rotação tem 3 ponto(s).", erro.getMessage());
        erro = assertThrows(IllegalArgumentException.class,
                () -> orientacao.daTela(tabela.rotacoes(), tabela.cargas(), List.of(List.of(1.0, 2.0, 3.0))));
        assertEquals("a tabela tem 1 linha(s), e o eixo de carga tem 2 ponto(s).", erro.getMessage());
    }

    @Test
    void modeloDeVeServeParaAFamiliaEValeABordaForaDosEixos() {
        Tabela ve = Tabela.preencher(List.of(0.2, 1.0), List.of(0.35, 1.0), (r, c) -> c >= 1.0 ? 0.97 : 0.50);
        ModeloVe modelo = new ModeloVe("4 válvulas", 4, Set.of(Comando.ORIGINAL, Comando.LEVE), ve, "SPEC 7.5", null);
        assertTrue(modelo.serve(4, Comando.LEVE));
        assertFalse(modelo.serve(2, Comando.LEVE));
        assertFalse(modelo.serve(4, Comando.AGRESSIVO));
        assertTrue(new ModeloVe("qualquer", null, Set.of(), ve, "SPEC 7.5", null).serve(2, Comando.AGRESSIVO));
        assertEquals(0.97, modelo.ve(1.6, 2.5), EPS, "acima dos eixos: a borda");
        assertEquals(0.50, modelo.ve(0.1, 0.2), EPS);
        assertEquals((0.50 + 0.97) / 2, modelo.ve(0.5, 0.675), EPS);
        assertEquals(List.of(), modelo.validar());

        Tabela errada = new Tabela(List.of(0.2, 0.1), List.of(0.35, 1.0),
                List.of(List.of(0.5, 0.5), Arrays.asList(2.0, null)));
        List<String> erros = new ModeloVe("errado", 6, null, errada, "x", null).validar();
        assertEquals(List.of("Válvulas por cilindro: de 2 a 5 (ou deixe em branco para qualquer motor).",
                "Rotação relativa: os pontos precisam crescer, do menor para o maior (o ponto 2 não é maior que o "
                        + "anterior).",
                "Preencha todas as células da VE."), erros);
    }

    @Test
    void modeloDeIgnicaoConfereEixosEAvanco() {
        Tabela avanco = Tabela.preencher(List.of(1000.0, 6000.0), List.of(30.0, 200.0), (r, c) -> c > 100 ? 12.0 : 30.0);
        ModeloIgnicao modelo = new ModeloIgnicao("AP turbo", " VW AP 2.0 8v turbo ", "E32", 30.0, avanco, "mapa do "
                + "dono", LocalDate.of(2026, 10, 22));
        assertEquals("VW AP 2.0 8v turbo", modelo.familia());
        assertEquals(List.of(), modelo.validar());

        Tabela demais = Tabela.preencher(List.of(1000.0, 6000.0), List.of(30.0, 200.0), (r, c) -> 80.0);
        assertEquals(List.of("O avanço de cada célula fica entre −20° e 70°."),
                new ModeloIgnicao("x", "", "", null, demais, "x", null).validar());
    }
}
