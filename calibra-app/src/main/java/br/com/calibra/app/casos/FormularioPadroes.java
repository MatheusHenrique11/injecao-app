package br.com.calibra.app.casos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import br.com.calibra.core.calculo.CalculoDimensionamento;
import br.com.calibra.core.calculo.CaminhoAr;
import br.com.calibra.core.calculo.Parametros;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.core.unidades.Conversoes;
import br.com.calibra.core.util.NumeroBr;

/**
 * Padrões de cálculo e limiares do semáforo como texto de tela, nas unidades da tela (psi e %), e de volta
 * para os {@link Parametros} do núcleo (kPa e frações). Sem JavaFX, para dar para testar.
 */
public final class FormularioPadroes {

    /** Resultado da leitura: os parâmetros (null se houve erro) e os erros encontrados. */
    public record Leitura(Parametros parametros, List<String> erros) {

        public Leitura {
            erros = List.copyOf(erros);
        }
    }

    private final Map<CampoPadrao, String> textos;
    private final List<String> erros = new ArrayList<>();

    private FormularioPadroes(Map<CampoPadrao, String> textos) {
        this.textos = textos;
    }

    /** Texto de cada campo para os parâmetros dados. */
    public static Map<CampoPadrao, String> textos(Parametros p) {
        LimiaresSemaforo l = p.limiares();
        Map<CampoPadrao, Double> valores = new EnumMap<>(CampoPadrao.class);
        valores.put(CampoPadrao.K_AR, p.kAr());
        valores.put(CampoPadrao.DC_MAX_PCT, p.dcMax() * 100);
        valores.put(CampoPadrao.ETA_C, p.etaC());
        valores.put(CampoPadrao.EPS, p.eps());
        valores.put(CampoPadrao.PERDA_DESCARGA_PSI, Conversoes.kpaParaPsi(p.perdaDescargaKpa()));
        valores.put(CampoPadrao.PERDA_ADMISSAO_PSI, Conversoes.kpaParaPsi(p.perdaAdmissaoKpa()));
        valores.put(CampoPadrao.ACRESCIMO_TM, p.acrescimoTmAspiradoC());
        valores.put(CampoPadrao.DENSIDADE_TESTE, p.densidadeFluidoTesteKgL());
        valores.put(CampoPadrao.TI_MINIMO, p.tiMinimoMarchaLentaMs());
        valores.put(CampoPadrao.DC_VERDE_PCT, l.dcVerdeAte() * 100);
        valores.put(CampoPadrao.DC_AMARELO_PCT, l.dcAmareloAte() * 100);
        valores.put(CampoPadrao.FOLGA_VERDE_PCT, l.folgaBombaVerde() * 100);
        valores.put(CampoPadrao.FOLGA_AMARELO_PCT, l.folgaBombaAmarelo() * 100);
        valores.put(CampoPadrao.PISTAO_VERDE, l.pistaoVerdeAte());
        valores.put(CampoPadrao.PISTAO_AMARELO, l.pistaoAmareloAte());
        valores.put(CampoPadrao.SENSOR_VERDE, l.sensorMapVerde());
        valores.put(CampoPadrao.SENSOR_AMARELO, l.sensorMapAmarelo());
        valores.put(CampoPadrao.ASPIRADO_VERDE, l.aspiradoVerdeAte());
        valores.put(CampoPadrao.ASPIRADO_AMARELO, l.aspiradoAmareloAte());
        valores.put(CampoPadrao.BMEP_ASPIRADO, l.bmepReferenciaAspiradoBar());
        valores.put(CampoPadrao.BMEP_SOBREALIMENTADO, l.bmepReferenciaSobrealimentadoBar());
        valores.put(CampoPadrao.BMEP_AMARELO_PCT, (l.bmepFatorAmarelo() - 1) * 100);
        Map<CampoPadrao, String> textos = new EnumMap<>(CampoPadrao.class);
        valores.forEach((campo, valor) ->
                textos.put(campo, NumeroBr.formatar(valor, campo.minimoCasas(), campo.maximoCasas())));
        return textos;
    }

    /**
     * Lê os campos e monta os parâmetros. O caminho do ar volta a ser o k_ar (o BSFC é escolhido em cada
     * cálculo) e os grupos do modo simultâneo vêm de {@code base}.
     */
    public static Leitura ler(Map<CampoPadrao, String> textos, Parametros base) {
        FormularioPadroes leitor = new FormularioPadroes(textos);
        Parametros parametros = leitor.parametros(base);
        return new Leitura(leitor.erros.isEmpty() ? parametros : null, leitor.erros);
    }

    private Parametros parametros(Parametros base) {
        double kAr = numero(CampoPadrao.K_AR);
        double dcMax = numero(CampoPadrao.DC_MAX_PCT) / 100;
        double etaC = numero(CampoPadrao.ETA_C);
        double eps = numero(CampoPadrao.EPS);
        double perdaDescarga = Conversoes.psiParaKpa(numero(CampoPadrao.PERDA_DESCARGA_PSI));
        double perdaAdmissao = Conversoes.psiParaKpa(numero(CampoPadrao.PERDA_ADMISSAO_PSI));
        double acrescimo = numero(CampoPadrao.ACRESCIMO_TM);
        double densidade = numero(CampoPadrao.DENSIDADE_TESTE);
        double tiMinimo = numero(CampoPadrao.TI_MINIMO);
        LimiaresSemaforo limiares = limiares();
        if (!erros.isEmpty()) {
            return null;
        }
        Parametros parametros = new Parametros(CaminhoAr.K_AR, kAr, Double.NaN, dcMax, etaC, eps, perdaDescarga,
                perdaAdmissao, acrescimo, densidade, tiMinimo, base.gruposSimultaneo(), limiares);
        erros.addAll(CalculoDimensionamento.validarParametros(parametros));
        return parametros;
    }

    private LimiaresSemaforo limiares() {
        double dcVerde = positivo(CampoPadrao.DC_VERDE_PCT);
        double dcAmarelo = positivo(CampoPadrao.DC_AMARELO_PCT);
        double folgaVerde = positivo(CampoPadrao.FOLGA_VERDE_PCT);
        double folgaAmarelo = positivo(CampoPadrao.FOLGA_AMARELO_PCT);
        double pistaoVerde = positivo(CampoPadrao.PISTAO_VERDE);
        double pistaoAmarelo = positivo(CampoPadrao.PISTAO_AMARELO);
        double sensorVerde = positivo(CampoPadrao.SENSOR_VERDE);
        double sensorAmarelo = positivo(CampoPadrao.SENSOR_AMARELO);
        double aspiradoVerde = positivo(CampoPadrao.ASPIRADO_VERDE);
        double aspiradoAmarelo = positivo(CampoPadrao.ASPIRADO_AMARELO);
        double bmepAspirado = positivo(CampoPadrao.BMEP_ASPIRADO);
        double bmepSobrealimentado = positivo(CampoPadrao.BMEP_SOBREALIMENTADO);
        double bmepAmarelo = numero(CampoPadrao.BMEP_AMARELO_PCT);
        if (bmepAmarelo < 0) {
            erros.add("O amarelo da BMEP é um percentual acima da referência: use zero ou mais.");
        }
        if (dcAmarelo > 100) {
            erros.add("O DC do bico vai até 100%: o limite do amarelo não pode passar disso.");
        }
        exigirAte(dcVerde, dcAmarelo, "DC do bico", "o do verde não pode passar do do amarelo");
        exigirAte(folgaAmarelo, folgaVerde, "Folga da bomba", "o do amarelo não pode passar do do verde");
        exigirAte(pistaoVerde, pistaoAmarelo, "Velocidade do pistão", "o do verde não pode passar do do amarelo");
        exigirAte(sensorAmarelo, sensorVerde, "Faixa do sensor MAP", "o do amarelo não pode passar do do verde");
        exigirAte(aspiradoVerde, aspiradoAmarelo, "Aspirado (MAP ÷ P_atm)",
                "o do verde não pode passar do do amarelo");
        if (!erros.isEmpty()) {
            return null;
        }
        return new LimiaresSemaforo(dcVerde / 100, dcAmarelo / 100, folgaVerde / 100, folgaAmarelo / 100,
                bmepAspirado, bmepSobrealimentado, 1 + bmepAmarelo / 100, pistaoVerde, pistaoAmarelo, sensorVerde,
                sensorAmarelo, aspiradoVerde, aspiradoAmarelo);
    }

    /** Confere a ordem dos dois limites de um indicador; campos com erro de digitação já foram anotados. */
    private void exigirAte(double menor, double maior, String indicador, String regra) {
        if (Double.isFinite(menor) && Double.isFinite(maior) && menor > maior) {
            erros.add(indicador + ": limites fora de ordem (" + regra + ").");
        }
    }

    private double positivo(CampoPadrao campo) {
        double valor = numero(campo);
        if (valor <= 0) {
            erros.add(maiuscula(campo.nome()) + " precisa ser maior que zero.");
            return Double.NaN;
        }
        return valor;
    }

    /** Número obrigatório. Vazio ou inválido: anota o erro e devolve NaN. */
    private double numero(CampoPadrao campo) {
        String texto = textos.get(campo);
        if (texto == null || texto.isBlank()) {
            erros.add("Informe " + campo.nome() + ".");
            return Double.NaN;
        }
        try {
            return NumeroBr.ler(texto);
        } catch (NumberFormatException erro) {
            erros.add(maiuscula(campo.nome()) + ": " + erro.getMessage());
            return Double.NaN;
        }
    }

    private static String maiuscula(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
