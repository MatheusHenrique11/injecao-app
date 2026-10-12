package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.calibra.core.calculo.CaminhoAr;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.core.unidades.Conversoes;

/** Padrões de cálculo e limiares da tela de Configurações, sem JavaFX. */
class FormularioPadroesTest {

    private static Map<CampoPadrao, String> textosDaSpecCom(CampoPadrao campo, String valor) {
        Map<CampoPadrao, String> textos = FormularioPadroes.textos(Parametros.padrao());
        textos.put(campo, valor);
        return textos;
    }

    private static List<String> erros(CampoPadrao campo, String valor) {
        return FormularioPadroes.ler(textosDaSpecCom(campo, valor), Parametros.padrao()).erros();
    }

    @Test
    void padroesDaSpecAparecemComoNaCalculadora() {
        Map<CampoPadrao, String> textos = FormularioPadroes.textos(Parametros.padrao());
        assertEquals("10,5", textos.get(CampoPadrao.K_AR));
        assertEquals("80", textos.get(CampoPadrao.DC_MAX_PCT));
        assertEquals("0,72", textos.get(CampoPadrao.ETA_C));
        assertEquals("0,80", textos.get(CampoPadrao.EPS));
        assertEquals("2,0", textos.get(CampoPadrao.PERDA_DESCARGA_PSI));
        assertEquals("1,0", textos.get(CampoPadrao.PERDA_ADMISSAO_PSI));
        assertEquals("10", textos.get(CampoPadrao.ACRESCIMO_TM));
        assertEquals("0,70", textos.get(CampoPadrao.DENSIDADE_TESTE));
        assertEquals("1,0", textos.get(CampoPadrao.TI_MINIMO));
        assertEquals("80", textos.get(CampoPadrao.DC_VERDE_PCT));
        assertEquals("85", textos.get(CampoPadrao.DC_AMARELO_PCT));
        assertEquals("30", textos.get(CampoPadrao.FOLGA_VERDE_PCT));
        assertEquals("15", textos.get(CampoPadrao.FOLGA_AMARELO_PCT));
        assertEquals("20", textos.get(CampoPadrao.PISTAO_VERDE));
        assertEquals("25", textos.get(CampoPadrao.PISTAO_AMARELO));
        assertEquals("1,2", textos.get(CampoPadrao.SENSOR_VERDE));
        assertEquals("1,0", textos.get(CampoPadrao.SENSOR_AMARELO));
        assertEquals("0,90", textos.get(CampoPadrao.ASPIRADO_VERDE));
        assertEquals("0,98", textos.get(CampoPadrao.ASPIRADO_AMARELO));
        assertEquals("13", textos.get(CampoPadrao.BMEP_ASPIRADO));
        assertEquals("22", textos.get(CampoPadrao.BMEP_SOBREALIMENTADO));
        assertEquals("15", textos.get(CampoPadrao.BMEP_AMARELO_PCT));
        assertEquals(CampoPadrao.values().length, textos.size(), "todo campo tem texto");
    }

    @Test
    void idaEVoltaDosPadroesDaSpec() {
        Parametros spec = Parametros.padrao();
        FormularioPadroes.Leitura leitura = FormularioPadroes.ler(FormularioPadroes.textos(spec), spec);
        assertTrue(leitura.erros().isEmpty(), leitura.erros()::toString);
        Parametros lido = leitura.parametros();
        assertEquals(FormularioPadroes.textos(spec), FormularioPadroes.textos(lido));
        assertEquals(spec.perdaDescargaKpa(), lido.perdaDescargaKpa(), 1e-9);
        assertEquals(spec.limiares().bmepFatorAmarelo(), lido.limiares().bmepFatorAmarelo(), 1e-12);
        assertEquals(CaminhoAr.K_AR, lido.caminhoAr());
        assertTrue(Double.isNaN(lido.bsfc()));
    }

    @Test
    void valoresEditadosViramParametrosNasUnidadesDoNucleo() {
        Map<CampoPadrao, String> textos = FormularioPadroes.textos(Parametros.padrao());
        textos.put(CampoPadrao.K_AR, "9,5");
        textos.put(CampoPadrao.DC_MAX_PCT, "78");
        textos.put(CampoPadrao.PERDA_ADMISSAO_PSI, "0,5");
        textos.put(CampoPadrao.DC_VERDE_PCT, "78");
        textos.put(CampoPadrao.DC_AMARELO_PCT, "83");
        textos.put(CampoPadrao.BMEP_AMARELO_PCT, "10");

        Parametros p = FormularioPadroes.ler(textos, Parametros.padrao()).parametros();
        assertEquals(9.5, p.kAr(), 1e-12);
        assertEquals(0.78, p.dcMax(), 1e-12);
        assertEquals(Conversoes.psiParaKpa(0.5), p.perdaAdmissaoKpa(), 1e-12);
        LimiaresSemaforo l = p.limiares();
        assertEquals(0.78, l.dcVerdeAte(), 1e-12);
        assertEquals(0.83, l.dcAmareloAte(), 1e-12);
        assertEquals(1.10, l.bmepFatorAmarelo(), 1e-12);
        // O que foi digitado volta igual para a tela.
        assertEquals("9,5", FormularioPadroes.textos(p).get(CampoPadrao.K_AR));
        assertEquals("78", FormularioPadroes.textos(p).get(CampoPadrao.DC_VERDE_PCT));
    }

    @Test
    void gruposDoModoSimultaneoVemDaBase() {
        Parametros p = Parametros.padrao();
        Parametros base = new Parametros(p.caminhoAr(), p.kAr(), p.bsfc(), p.dcMax(), p.etaC(), p.eps(),
                p.perdaDescargaKpa(), p.perdaAdmissaoKpa(), p.acrescimoTmAspiradoC(), p.densidadeFluidoTesteKgL(),
                p.tiMinimoMarchaLentaMs(), 2, p.limiares());
        assertEquals(2, FormularioPadroes.ler(FormularioPadroes.textos(base), base).parametros().gruposSimultaneo());
    }

    @Test
    void campoVazioOuComPontoDecimal() {
        assertEquals(List.of("Informe o k_ar."), erros(CampoPadrao.K_AR, " "));
        List<String> ponto = erros(CampoPadrao.ETA_C, "0.72");
        assertEquals(1, ponto.size(), ponto::toString);
        assertTrue(ponto.getFirst().startsWith("A eficiência do compressor"), ponto::toString);
        assertTrue(ponto.getFirst().contains("vírgula"), ponto::toString);
    }

    @Test
    void faixasDoNucleoValemTambemNasConfiguracoes() {
        List<String> kAr = erros(CampoPadrao.K_AR, "12");
        assertEquals(1, kAr.size(), kAr::toString);
        assertTrue(kAr.getFirst().contains("k_ar deve ficar entre 8,0 e 11"), kAr::toString);
        assertTrue(erros(CampoPadrao.ETA_C, "1,2").getFirst().contains("eta_c"));
        assertTrue(erros(CampoPadrao.DC_MAX_PCT, "120").getFirst().contains("DC máximo"));
    }

    @Test
    void limiaresForaDeOrdemSaoRecusadosComONomeDoIndicador() {
        Map<CampoPadrao, String> textos = FormularioPadroes.textos(Parametros.padrao());
        textos.put(CampoPadrao.DC_VERDE_PCT, "90");
        textos.put(CampoPadrao.FOLGA_AMARELO_PCT, "40");
        FormularioPadroes.Leitura leitura = FormularioPadroes.ler(textos, Parametros.padrao());
        assertNull(leitura.parametros());
        assertEquals(2, leitura.erros().size(), leitura.erros()::toString);
        assertTrue(leitura.erros().get(0).startsWith("DC do bico: limites fora de ordem"), leitura.erros()::toString);
        assertTrue(leitura.erros().get(1).startsWith("Folga da bomba: limites fora de ordem"), leitura.erros()::toString);
    }

    @Test
    void limiaresSemSentidoSaoRecusados() {
        assertTrue(erros(CampoPadrao.PISTAO_VERDE, "0").getFirst().contains("maior que zero"));
        assertTrue(erros(CampoPadrao.BMEP_AMARELO_PCT, "-5").getFirst().contains("zero ou mais"));
        List<String> dc = erros(CampoPadrao.DC_AMARELO_PCT, "105");
        assertTrue(dc.getFirst().contains("até 100%"), dc::toString);
    }
}
