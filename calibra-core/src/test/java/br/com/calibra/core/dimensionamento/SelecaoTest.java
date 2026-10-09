package br.com.calibra.core.dimensionamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.Fixtures;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.MarchaLenta;
import br.com.calibra.core.memoria.NivelAviso;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Recurso;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import org.junit.jupiter.api.Test;

/** Escolha de bico, bomba e injeção em catálogos (SPEC 7.3). Catálogos fictícios, só de teste. */
class SelecaoTest {

    private static Bico bico(double vazao) {
        return Bico.informado(String.valueOf((int) vazao), vazao, 300, 0.70, 1.0);
    }

    // ---------- Bico ----------

    @Test
    void escolheOMenorBicoQueFicaAte80PorCento() {
        List<Bico> catalogo = List.of(bico(1300), bico(950), bico(1150), bico(1050));
        Resultado<Optional<SelecaoBico.Escolha>> r = SelecaoBico.escolher(catalogo, Fixtures.casoB().comBico(null));
        SelecaoBico.Escolha escolha = r.valor().orElseThrow();
        assertEquals("1150", escolha.bico().nome());
        assertTrue(escolha.dutyCycle() <= 0.80, "DC " + escolha.dutyCycle());
        assertFalse(escolha.sugerirDuasBancadas());
        assertEquals(4, r.memoria().size(), "um passo por bico avaliado");
    }

    @Test
    void sugereDuasBancadasQuandoAMarchaLentaFicaCurta() {
        EntradaDimensionamento entrada = Fixtures.casoB().comBico(null).comMarchaLenta(new MarchaLenta(40, 0.45));
        Resultado<Optional<SelecaoBico.Escolha>> r = SelecaoBico.escolher(List.of(bico(1150), bico(1300)), entrada);
        SelecaoBico.Escolha escolha = r.valor().orElseThrow();
        assertTrue(escolha.tiEfetivoMarchaLentaMs() < 1.0);
        assertTrue(escolha.sugerirDuasBancadas());
        assertTrue(r.avisos().stream().anyMatch(a -> a.nivel() == NivelAviso.AMARELO && a.mensagem().contains("duas bancadas")));
    }

    @Test
    void nenhumBicoAtende() {
        Resultado<Optional<SelecaoBico.Escolha>> r = SelecaoBico.escolher(List.of(bico(800), bico(950)),
                Fixtures.casoB().comBico(null));
        assertTrue(r.valor().isEmpty());
        assertTrue(r.avisos().stream().anyMatch(a -> a.mensagem().contains("Nenhum bico do catálogo")));
    }

    @Test
    void escolhaAutomaticaSoComUmaBancada() {
        EntradaDimensionamento entrada = Fixtures.casoB().comBico(null);
        ConfiguracaoMotor duas = entrada.motor().paraConstrutor().bicosPorCilindro(2).construir();
        assertThrows(EntradaInvalidaException.class,
                () -> SelecaoBico.escolher(List.of(bico(1150)), entrada.comMotor(duas)));
    }

    // ---------- Bomba ----------

    private static Bomba bomba(String nome, double tensao, double vazaoA300, double vazaoA600) {
        return new Bomba(nome, List.of(new Bomba.Ponto(300, tensao, vazaoA300), new Bomba.Ponto(600, tensao, vazaoA600)),
                "teste", null);
    }

    @Test
    void escolheAMenorBombaComFolgaVerde() {
        Dimensionamento d = CalculoDimensionamento.calcular(Fixtures.casoB()).valor();
        double necessaria = d.bomba().vazaoLh();
        double pressao = d.bomba().pressaoKpa();
        List<Bomba> catalogo = List.of(
                bomba("pequena", 13.5, 200, 140),
                bomba("grande", 13.5, 400, 360),
                bomba("média", 13.5, 300, 255),
                bomba("quase", 13.5, 250, 220),
                bomba("só 12 V", 12.0, 400, 350));
        Resultado<Optional<SelecaoBomba.Escolha>> r =
                SelecaoBomba.escolher(catalogo, necessaria, pressao, 13.5, LimiaresSemaforo.padrao());
        SelecaoBomba.Escolha escolha = r.valor().orElseThrow();
        assertEquals("média", escolha.bomba().nome());
        assertTrue(escolha.folga() >= 0.30);
        assertTrue(r.avisos().stream().anyMatch(a -> a.mensagem().contains("só 12 V")), "bomba fora da curva é ignorada");
    }

    @Test
    void nenhumaBombaComFolgaVerde() {
        Resultado<Optional<SelecaoBomba.Escolha>> r = SelecaoBomba.escolher(
                List.of(bomba("pequena", 13.5, 200, 140), bomba("quase", 13.5, 250, 220)),
                188.9, 446.7, 13.5, LimiaresSemaforo.padrao());
        assertTrue(r.valor().isEmpty());
        assertTrue(r.avisos().stream().anyMatch(a -> a.mensagem().contains("maior folga é quase")));
    }

    // ---------- Injeção ----------

    private static final Injecao QUATRO = new Injecao("Marca", "Quatro", 4, 4, 4, Set.of(), "teste", null);
    private static final Injecao OITO = new Injecao("Marca", "Oito", 8, 4, 8, Set.of(Recurso.SENSOR_ETANOL), "teste", null);
    private static final Injecao DOZE = new Injecao("Marca", "Doze", 12, 12, 12,
            Set.of(Recurso.SENSOR_ETANOL, Recurso.CONTROLE_PRESSAO_TURBO), "teste", null);

    @Test
    void filtraPorSaidasCilindrosERecursos() {
        ConfiguracaoMotor quatroCilindros = Fixtures.motor(4).construir();
        assertEquals(List.of(DOZE, OITO, QUATRO),
                SelecaoInjecao.queAtendem(List.of(QUATRO, OITO, DOZE), quatroCilindros, 1, Set.of()));
        assertEquals(List.of(DOZE, OITO),
                SelecaoInjecao.queAtendem(List.of(QUATRO, OITO, DOZE), quatroCilindros, 1, Set.of(Recurso.SENSOR_ETANOL)));

        ConfiguracaoMotor v12 = Fixtures.motor(12).construir();
        assertEquals(List.of(DOZE), SelecaoInjecao.queAtendem(List.of(QUATRO, OITO, DOZE), v12, 1, Set.of()));
    }

    @Test
    void explicaOQueFalta() {
        ConfiguracaoMotor v12 = Fixtures.motor(12).construir();
        List<String> faltas = SelecaoInjecao.faltas(OITO, v12, 1, Set.of(Recurso.CONTROLE_PRESSAO_TURBO));
        assertTrue(faltas.contains("tem 8 saídas de bico; o motor pede 12"), faltas::toString);
        assertTrue(faltas.contains("tem 4 saídas de ignição; o motor pede 12"), faltas::toString);
        assertTrue(faltas.contains("controla até 8 cilindros em sequencial; o motor tem 12"), faltas::toString);
        assertTrue(faltas.contains("não tem controle de pressão do turbo"), faltas::toString);
    }

    @Test
    void bicosExtrasTambemPrecisamDeSaida() {
        ConfiguracaoMotor comExtras = Fixtures.motor(4).bicosExtrasCorpoBorboleta(2).construir();
        assertFalse(SelecaoInjecao.faltas(QUATRO, comExtras, 1, Set.of()).isEmpty());
        assertTrue(SelecaoInjecao.faltas(OITO, comExtras, 1, Set.of()).isEmpty());
    }
}
