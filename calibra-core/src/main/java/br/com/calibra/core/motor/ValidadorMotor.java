package br.com.calibra.core.motor;

import static br.com.calibra.core.util.NumeroBr.formatar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.com.calibra.core.memoria.Aviso;

/**
 * Regras da configuração do motor (SPEC 6.3). Erros bloqueiam e explicam o motivo; avisos amarelos
 * nunca bloqueiam.
 */
public final class ValidadorMotor {

    public static final int MIN_CILINDROS = 3;
    public static final int MAX_CILINDROS = 12;
    public static final String MENSAGEM_CILINDROS = "O app aceita motores de 3 a 12 cilindros.";

    /** Tolerância para achar o par a 360° (os ângulos podem vir arredondados da tela). */
    private static final double TOLERANCIA_PAR_GRAUS = 0.5;

    private ValidadorMotor() {
    }

    public static Validacao validar(ConfiguracaoMotor m) {
        List<String> erros = new ArrayList<>();
        List<Aviso> avisos = new ArrayList<>();
        int n = m.numeroCilindros();
        if (n < MIN_CILINDROS || n > MAX_CILINDROS) {
            erros.add(MENSAGEM_CILINDROS);
            return new Validacao(erros, avisos);
        }
        validarArranjo(m, n, erros, avisos);
        validarDimensoes(m, erros, avisos);
        validarComandoEInternos(m, erros);
        validarRotacoes(m, erros, avisos);
        validarOrdemIgnicao(m.ordemIgnicao(), n, erros);
        boolean angulosValidos = validarAngulos(m.angulosPms(), n, erros);
        validarBicos(m, erros);
        validarRodaFonica(m.rodaFonica(), erros);
        validarModos(m, n, angulosValidos, erros);
        if (m.combustivel() == null) {
            erros.add("Escolha o combustível.");
        }
        if (!(m.altitudeLocalM() >= 0 && m.altitudeLocalM() <= 3000)) {
            erros.add("A altitude da oficina deve ficar entre 0 e 3.000 m.");
        }
        return new Validacao(erros, avisos);
    }

    /** Verdadeiro se todo cilindro tem outro defasado 360° no virabrequim (SPEC 6.3). */
    public static boolean todosTemParA360(List<Double> angulosPms) {
        for (int i = 0; i < angulosPms.size(); i++) {
            boolean achou = false;
            for (int j = 0; j < angulosPms.size() && !achou; j++) {
                if (i != j) {
                    double diferenca = ((angulosPms.get(j) - angulosPms.get(i)) % 720.0 + 720.0) % 720.0;
                    achou = Math.abs(diferenca - 360.0) <= TOLERANCIA_PAR_GRAUS;
                }
            }
            if (!achou) {
                return false;
            }
        }
        return !angulosPms.isEmpty();
    }

    private static void validarArranjo(ConfiguracaoMotor m, int n, List<String> erros, List<Aviso> avisos) {
        Arranjo arranjo = m.arranjo();
        if (arranjo == null) {
            erros.add("Escolha o arranjo dos cilindros.");
            return;
        }
        if ((arranjo == Arranjo.V || arranjo == Arranjo.BOXER) && n % 2 != 0) {
            avisos.add(Aviso.amarelo("Arranjo " + arranjo.rotulo() + " com " + n + " cilindros é raro: confira."));
        }
        if (arranjo == Arranjo.EM_LINHA && n > 6) {
            avisos.add(Aviso.amarelo("Motor em linha com " + n + " cilindros é raro: confira."));
        }
        if (arranjo.exigeAnguloEntreBancadas()) {
            Double angulo = m.anguloEntreBancadas();
            if (angulo == null) {
                erros.add("Informe o ângulo entre bancadas (obrigatório para V e VR/W).");
            } else if (!(angulo > 0 && angulo < 180)) {
                erros.add("O ângulo entre bancadas deve ficar entre 0 e 180°.");
            }
        }
    }

    private static void validarDimensoes(ConfiguracaoMotor m, List<String> erros, List<Aviso> avisos) {
        boolean diametroOk = m.diametroMm() > 0;
        boolean cursoOk = m.cursoMm() > 0;
        if (!diametroOk) {
            erros.add("O diâmetro do cilindro deve ser maior que zero.");
        }
        if (!cursoOk) {
            erros.add("O curso deve ser maior que zero.");
        }
        Double informada = m.cilindradaInformadaCc();
        if (diametroOk && cursoOk && informada != null) {
            if (!(informada > 0)) {
                erros.add("A cilindrada informada deve ser maior que zero.");
            } else {
                double calculada = m.cilindradaCc();
                double diferenca = Math.abs(informada - calculada) / calculada;
                if (diferenca > 0.02) {
                    avisos.add(Aviso.amarelo("A cilindrada informada (" + formatar(informada, 0)
                            + " cm³) difere " + formatar(diferenca * 100, 1) + "% da calculada pelo diâmetro e curso ("
                            + formatar(calculada, 0) + " cm³): confira."));
                }
            }
        }
        double taxa = m.taxaCompressao();
        if (!(taxa >= 6.0 && taxa <= 16.0)) {
            erros.add("A taxa de compressão deve ficar entre 6,0 e 16,0.");
        } else if (taxa > 11.0 && m.aspiracao() != null && m.aspiracao().sobrealimentado()) {
            avisos.add(Aviso.amarelo("Taxa de compressão de " + formatar(taxa, 1)
                    + ":1 com sobrealimentação (acima de 11,0): confira."));
        }
        if (m.valvulasPorCilindro() < 2 || m.valvulasPorCilindro() > 5) {
            erros.add("O motor deve ter de 2 a 5 válvulas por cilindro.");
        }
    }

    private static void validarComandoEInternos(ConfiguracaoMotor m, List<String> erros) {
        if (m.comando() == null) {
            erros.add("Escolha o tipo de comando.");
        }
        if (m.duracaoComandoGraus() != null && !(m.duracaoComandoGraus() > 0)) {
            erros.add("A duração do comando deve ser maior que zero.");
        }
        if (m.cruzamentoGraus() != null && !Double.isFinite(m.cruzamentoGraus())) {
            erros.add("O cruzamento do comando precisa ser um número.");
        }
        if (m.aspiracao() == null) {
            erros.add("Escolha a aspiração.");
        }
        if (m.pistoes() == null) {
            erros.add("Informe se os pistões são originais ou forjados.");
        }
        if (m.bielas() == null) {
            erros.add("Informe se as bielas são originais ou forjadas.");
        }
    }

    private static void validarRotacoes(ConfiguracaoMotor m, List<String> erros, List<Aviso> avisos) {
        boolean maximaOk = m.rotacaoMaxima() >= 1000 && m.rotacaoMaxima() <= 12000;
        boolean lentaOk = m.rotacaoMarchaLenta() >= 500 && m.rotacaoMarchaLenta() <= 1500;
        if (!maximaOk) {
            erros.add("A rotação máxima deve ficar entre 1.000 e 12.000 rpm.");
        } else if (m.rotacaoMaxima() > 9000) {
            avisos.add(Aviso.amarelo("Rotação máxima acima de 9.000 rpm: confira."));
        }
        if (!lentaOk) {
            erros.add("A rotação de marcha lenta deve ficar entre 500 e 1.500 rpm.");
        }
        if (maximaOk && lentaOk && m.rotacaoMarchaLenta() >= m.rotacaoMaxima()) {
            erros.add("A marcha lenta deve ser menor que a rotação máxima.");
        }
    }

    private static void validarOrdemIgnicao(List<Integer> ordem, int n, List<String> erros) {
        if (ordem.size() != n) {
            erros.add("A ordem de ignição deve ter exatamente " + n + " números (tem " + ordem.size() + ").");
            return;
        }
        Set<Integer> vistos = new HashSet<>();
        for (int cilindro : ordem) {
            if (cilindro < 1 || cilindro > n) {
                erros.add("A ordem de ignição cita o cilindro " + cilindro + ", que não existe num motor de "
                        + n + " cilindros.");
            } else if (!vistos.add(cilindro)) {
                erros.add("A ordem de ignição repete o cilindro " + cilindro + ".");
            }
        }
        for (int cilindro = 1; cilindro <= n; cilindro++) {
            if (!vistos.contains(cilindro)) {
                erros.add("A ordem de ignição não tem o cilindro " + cilindro + ".");
            }
        }
        if (ordem.getFirst() != 1) {
            erros.add("A ordem de ignição deve começar no cilindro 1.");
        }
    }

    private static boolean validarAngulos(List<Double> angulos, int n, List<String> erros) {
        if (angulos.size() != n) {
            erros.add("Informe " + n + " ângulos de PMS, um por cilindro na ordem de ignição (tem "
                    + angulos.size() + ").");
            return false;
        }
        for (double angulo : angulos) {
            if (!(angulo >= 0 && angulo < 720)) {
                erros.add("Os ângulos dos PMS devem ficar entre 0 e 719°.");
                return false;
            }
        }
        for (int i = 1; i < n; i++) {
            if (!(angulos.get(i) > angulos.get(i - 1))) {
                erros.add("Os ângulos dos PMS devem crescer na ordem de ignição.");
                return false;
            }
        }
        return true;
    }

    private static void validarBicos(ConfiguracaoMotor m, List<String> erros) {
        if (m.bicosPorCilindro() < 1 || m.bicosPorCilindro() > 2) {
            erros.add("Use 1 ou 2 bicos por cilindro (bancadas primária e secundária).");
        }
        if (m.bicosExtrasCorpoBorboleta() < 0) {
            erros.add("Os bicos extras no corpo de borboleta não podem ser negativos.");
        }
    }

    private static void validarRodaFonica(RodaFonica roda, List<String> erros) {
        if (roda == null) {
            erros.add("Informe a roda fônica (dentes totais e faltantes) e o sensor de fase.");
            return;
        }
        if (roda.dentesTotais() < 1) {
            erros.add("A roda fônica precisa ter pelo menos 1 dente.");
        } else if (roda.dentesFaltantes() < 0 || roda.dentesFaltantes() >= roda.dentesTotais()) {
            erros.add("Os dentes faltantes da roda fônica devem ficar entre 0 e o total menos 1.");
        }
    }

    private static void validarModos(ConfiguracaoMotor m, int n, boolean angulosValidos, List<String> erros) {
        ModoInjecao injecao = m.modoInjecao();
        ModoIgnicao ignicao = m.modoIgnicao();
        if (injecao == null) {
            erros.add("Escolha o modo de injeção.");
        }
        if (ignicao == null) {
            erros.add("Escolha o modo de ignição.");
        }
        boolean impar = n % 2 != 0;
        if (ignicao == ModoIgnicao.CENTELHA_PERDIDA) {
            if (impar) {
                erros.add("Centelha perdida não funciona com " + n + " cilindros: com número ímpar nenhum cilindro "
                        + "tem par a 360°. Use ignição sequencial ou por distribuidor.");
            } else if (angulosValidos && !todosTemParA360(m.angulosPms())) {
                erros.add("Centelha perdida exige que todo cilindro tenha um par defasado 360° no virabrequim: "
                        + "confira os ângulos dos PMS.");
            }
        }
        if (injecao == ModoInjecao.SEMISSEQUENCIAL) {
            if (impar) {
                erros.add("Injeção semissequencial não funciona com " + n + " cilindros: com número ímpar nenhum "
                        + "cilindro tem par a 360°. Use injeção sequencial ou simultânea.");
            } else if (angulosValidos && !todosTemParA360(m.angulosPms())) {
                erros.add("Injeção semissequencial exige que todo cilindro tenha um par defasado 360° no "
                        + "virabrequim: confira os ângulos dos PMS.");
            }
        }
        boolean sensorFase = m.rodaFonica() != null && m.rodaFonica().sensorFase();
        if (injecao == ModoInjecao.SEQUENCIAL && !sensorFase) {
            erros.add("Injeção sequencial exige sensor de fase.");
        }
        if (ignicao == ModoIgnicao.SEQUENCIAL && !sensorFase) {
            erros.add("Ignição sequencial exige sensor de fase.");
        }
    }
}
