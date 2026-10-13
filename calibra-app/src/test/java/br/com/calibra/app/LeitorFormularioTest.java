package br.com.calibra.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.calibra.app.catalogo.Combustiveis;
import br.com.calibra.app.catalogo.ExemplosCatalogo;
import br.com.calibra.app.catalogo.ExemplosCatalogo.Exemplo;
import br.com.calibra.app.catalogo.PecasEscolhidas;
import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.combustivel.GasolinaComEtanol;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Bomba;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.pecas.Intercooler;
import br.com.calibra.core.pecas.Sensor;
import br.com.calibra.core.pecas.TipoIntercooler;
import br.com.calibra.core.pecas.TipoSensor;
import br.com.calibra.core.semaforo.Cor;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import org.junit.jupiter.api.Test;

/** Leitura dos campos da tela, sem JavaFX. */
class LeitorFormularioTest {

    private static LeitorFormulario.Leitura ler(Map<Campo, String> valores) {
        return LeitorFormulario.ler(valores, Parametros.padrao());
    }

    private static Map<Campo, String> casoBCom(Campo campo, String valor) {
        Map<Campo, String> valores = ValoresDeTeste.casoB();
        valores.put(campo, valor);
        return valores;
    }

    @Test
    void casoBDigitadoNaTelaDaOMesmoResultadoDaSpec() {
        LeitorFormulario.Leitura leitura = ler(ValoresDeTeste.casoB());
        assertTrue(leitura.erros().isEmpty(), leitura.erros()::toString);
        EntradaDimensionamento entrada = leitura.entrada();
        assertEquals(294.2, entrada.meta().potenciaKw(), 1e-9);
        assertEquals(300.0, entrada.pressaoBaseKpa(), 1e-9);
        assertEquals(2000.0, entrada.motor().cilindradaCc(), 1.0);
        assertEquals(0.80, entrada.parametros().dcMax(), 1e-12);

        Dimensionamento d = CalculoDimensionamento.calcular(entrada).valor();
        assertEquals(0.86, d.bicoAvaliado().dutyCycle(), 0.86 * 0.01);
        assertEquals(Cor.VERMELHO, d.avaliacao(Indicador.DC_BICO).orElseThrow().cor());
        assertEquals(241, d.ladoDoAr().mapKpa(), 241 * 0.01);
    }

    /** Os limiares não têm campo na calculadora: vêm dos padrões salvos em Configurações. */
    @Test
    void limiaresVemDosPadroesSalvos() {
        LimiaresSemaforo l = LimiaresSemaforo.padrao();
        LimiaresSemaforo dcMaisFolgado = new LimiaresSemaforo(0.90, 0.95, l.folgaBombaVerde(), l.folgaBombaAmarelo(),
                l.bmepReferenciaAspiradoBar(), l.bmepReferenciaSobrealimentadoBar(), l.bmepFatorAmarelo(),
                l.pistaoVerdeAte(), l.pistaoAmareloAte(), l.sensorMapVerde(), l.sensorMapAmarelo(),
                l.aspiradoVerdeAte(), l.aspiradoAmareloAte());
        Parametros p = Parametros.padrao();
        Parametros salvos = new Parametros(p.caminhoAr(), p.kAr(), p.bsfc(), p.dcMax(), p.etaC(), p.eps(),
                p.perdaDescargaKpa(), p.perdaAdmissaoKpa(), p.acrescimoTmAspiradoC(), p.densidadeFluidoTesteKgL(),
                p.tiMinimoMarchaLentaMs(), p.gruposSimultaneo(), dcMaisFolgado);

        EntradaDimensionamento entrada = LeitorFormulario.ler(ValoresDeTeste.casoB(), salvos).entrada();
        assertEquals(dcMaisFolgado, entrada.parametros().limiares());
        Dimensionamento d = CalculoDimensionamento.calcular(entrada).valor();
        assertEquals(Cor.VERDE, d.avaliacao(Indicador.DC_BICO).orElseThrow().cor(), "DC de 86% com verde até 90%");
    }

    @Test
    void pontoDecimalERecusadoComExplicacao() {
        List<String> erros = ler(casoBCom(Campo.DIAMETRO, "82.5")).erros();
        assertEquals(1, erros.size(), erros::toString);
        assertTrue(erros.getFirst().startsWith("O diâmetro"), erros::toString);
        assertTrue(erros.getFirst().contains("vírgula"), erros::toString);
    }

    @Test
    void campoObrigatorioVazio() {
        Map<Campo, String> valores = ValoresDeTeste.casoB();
        valores.remove(Campo.VE);
        LeitorFormulario.Leitura leitura = ler(valores);
        assertNull(leitura.entrada());
        assertTrue(leitura.erros().contains("Informe a VE."), leitura.erros()::toString);
    }

    @Test
    void semVazaoNaoHaBicoEBicoIncompletoEhErro() {
        Map<Campo, String> semBico = ValoresDeTeste.casoB();
        semBico.remove(Campo.BICO_VAZAO);
        assertNull(ler(semBico).entrada().bico());

        Map<Campo, String> incompleto = ValoresDeTeste.casoB();
        incompleto.remove(Campo.BICO_TEMPO_MORTO);
        assertTrue(ler(incompleto).erros().contains("Informe o tempo morto do bico."));
    }

    @Test
    void metaNaRodaPedeAPerda() {
        LeitorFormulario.Leitura leitura = ler(casoBCom(Campo.PONTO_MEDICAO, "RODA"));
        assertTrue(leitura.erros().contains("Informe a perda de transmissão."), leitura.erros()::toString);
        Map<Campo, String> comPerda = casoBCom(Campo.PONTO_MEDICAO, "RODA");
        comPerda.put(Campo.PERDA_TRANSMISSAO, "15");
        assertEquals(15.0, ler(comPerda).entrada().meta().perdaTransmissaoPct(), 1e-12);
    }

    @Test
    void ordemEAngulosAceitamSeparadoresComuns() {
        Map<Campo, String> valores = casoBCom(Campo.ORDEM_IGNICAO, "1, 3, 4, 2");
        valores.put(Campo.ANGULOS_PMS, "0 - 180 - 360 - 540");
        EntradaDimensionamento entrada = ler(valores).entrada();
        assertEquals(List.of(1, 3, 4, 2), entrada.motor().ordemIgnicao());
        assertEquals(List.of(0.0, 180.0, 360.0, 540.0), entrada.motor().angulosPms());

        Map<Campo, String> decimais = casoBCom(Campo.CILINDROS, "7");
        decimais.put(Campo.ANGULOS_PMS, "0; 102,9; 205,7; 308,6; 411,4; 514,3; 617,1");
        assertEquals(102.9, LeitorFormulario.lerMotor(decimais).motor().angulosPms().get(1), 1e-12);
    }

    @Test
    void inteiroComDecimalERecusado() {
        List<String> erros = ler(casoBCom(Campo.ROTACAO_MAXIMA, "7.500,5")).erros();
        assertTrue(erros.contains("A rotação máxima: use um número inteiro."), erros::toString);
    }

    @Test
    void injecaoEOpcionalMasCompleta() {
        Map<Campo, String> valores = casoBCom(Campo.INJECAO_SAIDAS_BICO, "8");
        valores.put(Campo.INJECAO_SAIDAS_IGNICAO, "4");
        valores.put(Campo.INJECAO_NOME, "FT450");
        EntradaDimensionamento entrada = ler(valores).entrada();
        assertEquals(8, entrada.injecao().saidasBico());
        assertEquals("FT450", entrada.injecao().nome());
    }

    // ===================== Peças do catálogo (bloco 2.3) =====================

    private static <T> ItemCatalogo<T> item(Exemplo<T> exemplo, long id) {
        return new ItemCatalogo<>(id, exemplo.tipo(), 1, exemplo.dados(), exemplo.info(), false, null,
                LocalDateTime.of(2026, 10, 12, 9, 0));
    }

    @Test
    void pecasDoCatalogoValemNoLugarDoQueFoiDigitado() {
        Exemplo<Bico> bico = ExemplosCatalogo.bicoComTabela();
        Exemplo<Bomba> bomba = ExemplosCatalogo.bomba();
        Exemplo<Injecao> injecao = ExemplosCatalogo.injecao();
        PecasEscolhidas pecas = new PecasEscolhidas(item(bico, 1), null, item(bomba, 2),
                item(ExemplosCatalogo.intercooler(), 3), item(ExemplosCatalogo.sensor(), 4), item(injecao, 5));
        Map<Campo, String> valores = casoBCom(Campo.BOMBA_VAZAO, "300");
        valores.put(Campo.SENSOR_MAP_KPA, "250");
        valores.put(Campo.TENSAO_V, "13,5");

        LeitorFormulario.Leitura leitura = LeitorFormulario.ler(valores, Parametros.padrao(), Combustiveis.padrao(), pecas);
        assertTrue(leitura.erros().isEmpty(), leitura.erros()::toString);
        EntradaDimensionamento entrada = leitura.entrada();
        assertEquals(bico.dados(), entrada.bico(), "o bico do catálogo, com a tabela de tempo morto");
        assertEquals(bomba.dados(), entrada.bomba());
        assertNull(entrada.vazaoBombaDisponivelLh(), "a vazão digitada não vale com a bomba do catálogo");
        assertEquals(0.75, entrada.parametros().eps(), 1e-12, "a eficiência do intercooler vira o eps");
        assertEquals(400.0, entrada.faixaSensorMapKpa(), 1e-12, "a faixa máxima do sensor MAP");
        assertEquals(injecao.dados(), entrada.injecao());
        assertEquals(13.5, entrada.tensaoV(), 1e-12);
        assertTrue(CalculoDimensionamento.calcular(entrada).valor().bicoAvaliado().tempoMortoMs() > 0,
                "a tabela de tempo morto é lida na tensão de trabalho");
    }

    @Test
    void sensorQueNaoEMapNaoDaAFaixa() {
        Exemplo<Sensor> map = ExemplosCatalogo.sensor();
        Sensor pressao = new Sensor("Sensor de pressão teste", TipoSensor.PRESSAO, null, 1000.0, map.dados().fonte(), null);
        PecasEscolhidas pecas = new PecasEscolhidas(null, null, null, null,
                item(new Exemplo<>(map.tipo(), pressao, map.info()), 1), null);
        List<String> erros = LeitorFormulario.ler(ValoresDeTeste.casoB(), Parametros.padrao(), Combustiveis.padrao(),
                pecas).erros();
        assertEquals(List.of("O sensor Sensor de pressão teste não é do tipo MAP: escolha um sensor MAP ou digite a "
                + "faixa."), erros);
    }

    @Test
    void intercoolerSemEficienciaDeixaOEpsDigitado() {
        Exemplo<Intercooler> exemplo = ExemplosCatalogo.intercooler();
        Intercooler semEficiencia = new Intercooler(exemplo.dados().nome(), TipoIntercooler.AR_AGUA, null,
                exemplo.dados().fonte(), null);
        PecasEscolhidas pecas = new PecasEscolhidas(null, null, null,
                item(new Exemplo<>(exemplo.tipo(), semEficiencia, exemplo.info()), 1), null, null);
        EntradaDimensionamento entrada = LeitorFormulario.ler(casoBCom(Campo.EPS, "0,7"), Parametros.padrao(),
                Combustiveis.padrao(), pecas).entrada();
        assertEquals(0.7, entrada.parametros().eps(), 1e-12);
    }

    @Test
    void combustivelVemDaListaDoCatalogo() {
        Combustivel e30 = new Combustivel("E30", "Gasolina teste E30", new GasolinaComEtanol(0.30), 0.755, 0.80, 0.87,
                null, null, "Ficha fictícia de teste", 2);
        Combustiveis catalogo = new Combustiveis() {
            @Override
            public List<Combustivel> ativos() {
                return List.of(e30);
            }

            @Override
            public Optional<Combustivel> porCodigo(String codigo) {
                return codigo.equals("E30") ? Optional.of(e30) : Optional.empty();
            }
        };
        assertEquals(e30, LeitorFormulario.lerMotor(casoBCom(Campo.COMBUSTIVEL, "E30"), catalogo).motor().combustivel());
        assertEquals(List.of("Combustível desconhecido: EHC."),
                LeitorFormulario.lerMotor(ValoresDeTeste.casoB(), catalogo).erros());
    }
}
