package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.calibra.app.casos.SugestaoPecas.Sugestao;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Recurso;
import br.com.calibra.core.pecas.TempoMorto;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;

/** "Sugerir" do dimensionamento a partir do catálogo, com o caso golden B. Peças fictícias. */
class SugestaoPecasTest {

    private static final String FONTE = "Ficha fictícia de teste";
    private static final LocalDate DATA = LocalDate.of(2026, 10, 1);

    private static <T> ItemCatalogo<T> item(long id, TipoCatalogo<T> tipo, String nome, T dados) {
        return new ItemCatalogo<>(id, tipo, 1, dados, new InfoCatalogo(nome, FONTE, DATA, false, null, ""), false,
                null, LocalDateTime.of(2026, 10, 12, 9, 0));
    }

    private static ItemCatalogo<Bico> bico(long id, double vazao) {
        String nome = "Bico teste " + (int) vazao;
        return item(id, TipoCatalogo.BICO, nome, new Bico(nome, vazao, 300.0, 0.70, TempoMorto.fixo(1.0), null, FONTE,
                DATA));
    }

    private static ItemCatalogo<Bomba> bomba(long id, String nome, double vazaoA3Bar, double vazaoA6Bar) {
        return item(id, TipoCatalogo.BOMBA, nome, new Bomba(nome, List.of(new Bomba.Ponto(300.0, 13.5, vazaoA3Bar),
                new Bomba.Ponto(600.0, 13.5, vazaoA6Bar)), FONTE, DATA));
    }

    private static ItemCatalogo<Injecao> injecao(long id, String marca, int saidas, Set<Recurso> recursos) {
        Injecao injecao = new Injecao(marca, "ECU " + saidas, saidas, saidas, null, recursos, FONTE, DATA);
        return item(id, TipoCatalogo.INJECAO, injecao.nome(), injecao);
    }

    /** No caso B, o bico de 1.050 fica em 86% de DC: o menor que fica até 80% é o de 1.300. */
    @Test
    void bicoMenorQueMantemODc() {
        List<ItemCatalogo<Bico>> bicos = List.of(bico(1, 1300), bico(2, 630), bico(3, 1050));
        Sugestao<Bico> sugestao = SugestaoPecas.bico(bicos, CasosProjetoTest.casoB());
        assertEquals(1L, sugestao.item().orElseThrow().id());
        assertTrue(sugestao.mensagem().startsWith("Sugerido: Bico teste 1300 (DC "), sugestao.mensagem());

        Sugestao<Bico> nenhum = SugestaoPecas.bico(List.of(bico(2, 630)), CasosProjetoTest.casoB());
        assertTrue(nenhum.item().isEmpty());
        assertEquals("Nenhum bico do catálogo atende.", nenhum.mensagem());
        assertTrue(nenhum.avisos().getFirst().startsWith("Nenhum bico do catálogo mantém o DC até 80%"),
                nenhum.avisos()::toString);
    }

    @Test
    void bicoComDuasBancadasNaoTemSugestaoAutomatica() {
        EntradaDimensionamento entrada = CasosProjetoTest.casoB();
        ConfiguracaoMotor duasBancadas = entrada.motor().paraConstrutor().bicosPorCilindro(2).construir();
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class,
                () -> SugestaoPecas.bico(List.of(bico(1, 1300)), entrada.comMotor(duasBancadas)));
        assertTrue(erro.getMessage().contains("uma bancada"), erro.getMessage());
    }

    @Test
    void bombaMenorComFolgaVerdeNaTensaoDeTrabalho() {
        List<ItemCatalogo<Bomba>> bombas = List.of(bomba(1, "Bomba grande", 450, 400), bomba(2, "Bomba pequena", 120, 90),
                bomba(3, "Bomba média", 300, 260));
        EntradaDimensionamento entrada = CasosProjetoTest.casoB().comTensao(13.5);
        Sugestao<Bomba> sugestao = SugestaoPecas.bomba(bombas, entrada);
        assertEquals(3L, sugestao.item().orElseThrow().id(), sugestao.mensagem());
        assertTrue(sugestao.mensagem().startsWith("Sugerida: Bomba média ("), sugestao.mensagem());

        Sugestao<Bomba> semTensao = SugestaoPecas.bomba(bombas, CasosProjetoTest.casoB());
        assertTrue(semTensao.item().isEmpty());
        assertEquals("Informe a tensão de trabalho para ler as curvas das bombas.", semTensao.mensagem());
    }

    @Test
    void injecaoQueAtendeOMotorEOsRecursos() {
        ConfiguracaoMotor motor = CasosProjetoTest.casoB().motor();
        List<ItemCatalogo<Injecao>> injecoes = List.of(injecao(1, "Beta", 8, Set.of(Recurso.SENSOR_ETANOL)),
                injecao(2, "Alfa", 4, Set.of()), injecao(3, "Gama", 2, Set.of(Recurso.SENSOR_ETANOL)));

        Sugestao<Injecao> semExigencia = SugestaoPecas.injecao(injecoes, motor, 1, Set.of());
        assertEquals(2L, semExigencia.item().orElseThrow().id());
        assertEquals("Sugerida: Alfa ECU 4. Também atendem: Beta ECU 8.", semExigencia.mensagem());

        Sugestao<Injecao> comEtanol = SugestaoPecas.injecao(injecoes, motor, 1, Set.of(Recurso.SENSOR_ETANOL));
        assertEquals(1L, comEtanol.item().orElseThrow().id());
        assertEquals("Sugerida: Beta ECU 8 (recursos pedidos: sensor de etanol).", comEtanol.mensagem());

        Sugestao<Injecao> nenhuma = SugestaoPecas.injecao(List.of(injecoes.get(2)), motor, 1, Set.of());
        assertTrue(nenhuma.item().isEmpty());
        assertEquals(List.of("Gama ECU 2: tem 2 saídas de bico; o motor pede 4; tem 2 saídas de ignição; o motor pede 4"),
                nenhuma.avisos());
    }

    @Test
    void catalogoVazioAvisa() {
        assertEquals("Nenhum bico cadastrado no catálogo.",
                SugestaoPecas.bico(List.of(), CasosProjetoTest.casoB()).mensagem());
        assertEquals("Nenhuma injeção cadastrada no catálogo.",
                SugestaoPecas.injecao(List.of(), CasosProjetoTest.casoB().motor(), 1, Set.of()).mensagem());
    }
}
