package br.com.calibra.core.pecas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.mapa.FuncaoTabela;
import br.com.calibra.core.mapa.OrientacaoTabela;
import br.com.calibra.core.mapa.PerfilTabela;
import br.com.calibra.core.mapa.TipoCarga;

/** Fase 4 (decisão 292): o perfil completo da injeção (SPEC 9) e o que ele recusa. */
class PerfilInjecaoTest {

    private static final OrientacaoTabela ORIENTACAO = new OrientacaoTabela(OrientacaoTabela.EixoDasColunas.ROTACAO,
            OrientacaoTabela.OrdemDasLinhas.CRESCE_PARA_CIMA);

    private static PerfilTabela tabela(FuncaoTabela funcao, TipoCarga carga, List<Double> rotacoes,
            List<Double> cargas) {
        return new PerfilTabela(" Mapa ", funcao, carga, " ms ", rotacoes, cargas, ORIENTACAO);
    }

    @Test
    void injecaoSemPerfilTemOPerfilVazio() {
        Injecao antiga = new Injecao("Marca", "ECU", 8, 8, null, Set.of(), "manual", null);
        assertEquals(PerfilInjecao.vazio(), antiga.perfil());
        assertTrue(antiga.perfil().tabela(FuncaoTabela.COMBUSTIVEL).isEmpty());
        assertEquals(List.of(), antiga.perfil().validar());
    }

    @Test
    void perfilCertoAchaATabelaPelaFuncao() {
        PerfilTabela combustivel = tabela(FuncaoTabela.COMBUSTIVEL, TipoCarga.MAP_KPA, List.of(500.0, 1000.0, 7000.0),
                List.of(20.0, 100.0, 250.0));
        PerfilTabela ignicao = tabela(FuncaoTabela.IGNICAO, TipoCarga.TPS_PERCENTUAL, List.of(1000.0, 8000.0),
                List.of(0.0, 100.0));
        PerfilInjecao perfil = new PerfilInjecao(ImpedanciaBico.ALTA_E_BAIXA, 2, ModeloCombustivel.MS,
                List.of(combustivel, ignicao), 20.0, false, "", "");
        assertEquals(List.of(), perfil.validar());
        assertEquals(combustivel, perfil.tabela(FuncaoTabela.COMBUSTIVEL).orElseThrow());
        assertEquals("Mapa", combustivel.nome(), "sem os espaços das pontas");
        assertEquals("ms", combustivel.unidade());
        assertTrue(perfil.tabela(FuncaoTabela.LAMBDA_ALVO).isEmpty());
    }

    @Test
    void recusaEixoForaDeOrdemPoucosPontosEFaixaErrada() {
        PerfilInjecao perfil = new PerfilInjecao(null, 0, null, List.of(
                tabela(FuncaoTabela.COMBUSTIVEL, TipoCarga.MAP_KPA, List.of(1000.0, 500.0), List.of(20.0)),
                tabela(FuncaoTabela.IGNICAO, TipoCarga.TPS_PERCENTUAL, List.of(1000.0, 2000.0), List.of(0.0, 120.0)),
                tabela(FuncaoTabela.IGNICAO, TipoCarga.MAP_KPA, List.of(1000.0, 2000.0), List.of(20.0, 40.0))),
                200.0, null, "", "");
        List<String> erros = perfil.validar();
        assertTrue(erros.contains("Bicos por saída: pelo menos 1 (ou deixe em branco)."), erros::toString);
        assertTrue(erros.contains("Temperatura de referência do ar: de −40 a 150 °C (ou deixe em branco)."),
                erros::toString);
        assertTrue(erros.contains("Tabela de combustível, pontos de rotação: os pontos precisam crescer, do menor "
                + "para o maior (o ponto 2 não é maior que o anterior)."), erros::toString);
        assertTrue(erros.contains("Tabela de combustível, pontos de carga: informe pelo menos 2 pontos."),
                erros::toString);
        assertTrue(erros.contains("Tabela de ignição, pontos de carga: o ponto 2 está fora da faixa aceita."),
                "TPS vai de 0 a 100%: " + erros);
        assertTrue(erros.contains("Só uma tabela de ignição por injeção."), erros::toString);
    }
}
