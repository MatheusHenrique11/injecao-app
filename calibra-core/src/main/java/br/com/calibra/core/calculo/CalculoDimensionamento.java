package br.com.calibra.core.calculo;

import static br.com.calibra.core.memoria.MemoriaCalculo.e;
import static br.com.calibra.core.util.NumeroBr.formatar;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.combustivel.Combustivel;
import br.com.calibra.core.memoria.Entrada;
import br.com.calibra.core.memoria.MemoriaCalculo;
import br.com.calibra.core.memoria.NivelAviso;
import br.com.calibra.core.memoria.Resultado;
import br.com.calibra.core.motor.Aspiracao;
import br.com.calibra.core.motor.ConfiguracaoMotor;
import br.com.calibra.core.motor.Construcao;
import br.com.calibra.core.motor.ModoIgnicao;
import br.com.calibra.core.motor.ModoInjecao;
import br.com.calibra.core.motor.SaidasExigidas;
import br.com.calibra.core.motor.ValidadorMotor;
import br.com.calibra.core.pecas.Bico;
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.PontoMedicao;
import br.com.calibra.core.semaforo.Avaliacao;
import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.core.semaforo.Semaforo;
import br.com.calibra.core.unidades.Conversoes;

/**
 * A cadeia de dimensionamento da SPEC 7: potência → ar → combustível → bicos e bomba → turbo →
 * tempo de injeção → esforço, com semáforo e memória de cálculo em cada elo.
 */
public final class CalculoDimensionamento {

    /** Temperatura inicial da iteração MAP ↔ Tm (SPEC 7.1). */
    public static final double TM_INICIAL_C = 50.0;
    /** Critério de parada da iteração (SPEC 7.1). */
    public static final double TOLERANCIA_TM_C = 0.01;
    /** Máximo de passos da iteração (SPEC 7.1). */
    public static final int MAX_ITERACOES = 100;
    /** Lambda da checagem de marcha lenta (SPEC 7.5: 1,00 abaixo de 70 kPa). */
    public static final double LAMBDA_MARCHA_LENTA = 1.00;

    private CalculoDimensionamento() {
    }

    /**
     * Faz o dimensionamento completo.
     *
     * @throws EntradaInvalidaException com todas as mensagens, se a entrada não for aceita
     * @throws CalculoNaoConvergiuException se a iteração MAP ↔ Tm não convergir
     */
    public static Resultado<Dimensionamento> calcular(EntradaDimensionamento entrada) {
        validar(entrada);
        return new Cadeia(entrada).calcular();
    }

    // ===================== Iteração MAP ↔ Tm =====================

    /** Resultado da iteração MAP ↔ Tm: a temperatura no coletor convergida e quantos passos levou. */
    record IteracaoMapTm(double tmC, int passos) {
    }

    /**
     * Iteração MAP ↔ Tm da SPEC 7.1: começa em Tm = 50 °C e para quando |ΔTm| < 0,01 °C. Com PR até 1
     * o compressor não aquece o ar (T2 = T1).
     *
     * @throws CalculoNaoConvergiuException se não convergir em {@code maxPassos} passos
     */
    static IteracaoMapTm iterarMapTm(double waLbMin, double ve, double rpm, double cilindradaPol3, double patmPsia,
            double perdasPsi, double admissaoPsi, double temperaturaAmbienteC, double etaC, double eps, int maxPassos) {
        double t1K = Conversoes.cParaK(temperaturaAmbienteC);
        double tmC = TM_INICIAL_C;
        for (int passo = 1; passo <= maxPassos; passo++) {
            double mapPsia = Formulas.mapPsia(waLbMin, Conversoes.cParaF(tmC), ve, rpm, cilindradaPol3);
            double pr = Formulas.razaoPressao(mapPsia, perdasPsi, patmPsia, admissaoPsi);
            double t2K = pr > 1.0 ? Formulas.temperaturaSaidaCompressorK(t1K, pr, etaC) : t1K;
            double novaTmC = Formulas.temperaturaColetor(Conversoes.kParaC(t2K), eps, temperaturaAmbienteC);
            boolean convergiu = Math.abs(novaTmC - tmC) < TOLERANCIA_TM_C;
            tmC = novaTmC;
            if (convergiu) {
                return new IteracaoMapTm(tmC, passo);
            }
        }
        throw new CalculoNaoConvergiuException("A iteração MAP ↔ Tm não convergiu em " + maxPassos
                + " passos. Confira VE, rotação e meta.");
    }

    // ===================== Validação da entrada =====================

    static void validar(EntradaDimensionamento entrada) {
        if (entrada.motor() == null) {
            throw new EntradaInvalidaException("Informe a configuração do motor.");
        }
        List<String> erros = new ArrayList<>(ValidadorMotor.validar(entrada.motor()).erros());
        validarMeta(entrada.meta(), entrada.motor(), erros);
        if (!(entrada.lambdaAlvo() > 0)) {
            erros.add("A lambda alvo deve ser maior que zero.");
        }
        if (!(entrada.ve() > 0 && entrada.ve() <= 1.5)) {
            erros.add("A VE deve ficar entre 0 e 1,5 (use fração: 0,95 = 95%).");
        }
        if (!Double.isFinite(entrada.temperaturaAmbienteC())) {
            erros.add("Informe a temperatura ambiente.");
        }
        if (!(entrada.pressaoBaseKpa() > 0)) {
            erros.add("Informe a pressão de base do combustível (diferencial no bico).");
        }
        validarParametros(entrada.parametros(), erros);
        validarPerdaAntesDoCompressor(entrada, erros);
        validarBicos(entrada, erros);
        if (entrada.tensaoV() != null && !(entrada.tensaoV() > 0)) {
            erros.add("A tensão deve ser maior que zero.");
        }
        MarchaLenta marchaLenta = entrada.marchaLenta();
        if (marchaLenta != null && !(marchaLenta.mapKpa() > 0 && marchaLenta.ve() > 0 && marchaLenta.ve() <= 1.5)) {
            erros.add("Marcha lenta: informe MAP maior que zero e VE entre 0 e 1,5.");
        }
        if (entrada.vazaoBombaDisponivelLh() != null && !(entrada.vazaoBombaDisponivelLh() > 0)) {
            erros.add("A vazão da bomba deve ser maior que zero.");
        }
        if (entrada.faixaSensorMapKpa() != null && !(entrada.faixaSensorMapKpa() > 0)) {
            erros.add("A faixa do sensor MAP deve ser maior que zero.");
        }
        Injecao injecao = entrada.injecao();
        if (injecao != null && (injecao.saidasBico() < 0 || injecao.saidasIgnicao() < 0)) {
            erros.add("As saídas da injeção não podem ser negativas.");
        }
        if (!erros.isEmpty()) {
            throw new EntradaInvalidaException(erros);
        }
    }

    private static void validarMeta(Meta meta, ConfiguracaoMotor motor, List<String> erros) {
        if (meta == null) {
            erros.add("Informe a meta: potência, rotação e uso.");
            return;
        }
        if (!(meta.potenciaKw() > 0)) {
            erros.add("A potência meta deve ser maior que zero.");
        }
        if (meta.pontoMedicao() == null) {
            erros.add("Informe se a potência meta é medida na roda ou no virabrequim.");
        } else if (meta.pontoMedicao() == PontoMedicao.RODA) {
            Double perda = meta.perdaTransmissaoPct();
            if (perda == null) {
                erros.add("Meta medida na roda: informe a perda de transmissão (%).");
            } else if (!(perda >= 0 && perda < 100)) {
                erros.add("A perda de transmissão deve ficar entre 0 e 100%.");
            }
        }
        int rpm = meta.rotacaoPotenciaMaxima();
        if (rpm <= 0) {
            erros.add("Informe a rotação de potência máxima.");
        } else if (motor.rotacaoMaxima() > 0 && rpm > motor.rotacaoMaxima()) {
            erros.add("A rotação de potência máxima (" + rpm + " rpm) passa da rotação máxima do motor ("
                    + motor.rotacaoMaxima() + " rpm).");
        }
        Integer torque = meta.rotacaoTorqueMaximo();
        if (torque != null && !(torque > 0 && (motor.rotacaoMaxima() <= 0 || torque <= motor.rotacaoMaxima()))) {
            erros.add("A rotação de torque máximo deve ficar entre zero e a rotação máxima do motor.");
        }
        if (meta.uso() == null) {
            erros.add("Escolha o uso do veículo.");
        }
    }

    private static void validarParametros(Parametros p, List<String> erros) {
        if (p == null) {
            erros.add("Informe os parâmetros do cálculo.");
            return;
        }
        if (p.caminhoAr() == null) {
            erros.add("Escolha o caminho da vazão de ar (k_ar ou BSFC).");
        } else if (p.caminhoAr() == CaminhoAr.K_AR
                && !(p.kAr() >= Parametros.K_AR_MINIMO && p.kAr() <= Parametros.K_AR_MAXIMO)) {
            erros.add("O fator k_ar deve ficar entre " + formatar(Parametros.K_AR_MINIMO, 1) + " e "
                    + formatar(Parametros.K_AR_MAXIMO, 0) + " hp por lb/min.");
        } else if (p.caminhoAr() == CaminhoAr.BSFC && !(p.bsfc() > 0)) {
            erros.add("Caminho por BSFC: informe o BSFC (lb/hp·h).");
        }
        if (!(p.dcMax() > 0 && p.dcMax() <= 1)) {
            erros.add("O DC máximo deve ficar entre 0 e 100%.");
        }
        if (!(p.etaC() > 0 && p.etaC() <= 1)) {
            erros.add("A eficiência do compressor (eta_c) deve ficar entre 0 e 1.");
        }
        if (!(p.eps() >= 0 && p.eps() <= 1)) {
            erros.add("A eficiência do intercooler (eps) deve ficar entre 0 e 1.");
        }
        if (!(p.perdaDescargaKpa() >= 0) || !(p.perdaAdmissaoKpa() >= 0)) {
            erros.add("As perdas de pressão não podem ser negativas.");
        }
        if (!Double.isFinite(p.acrescimoTmAspiradoC())) {
            erros.add("Informe o acréscimo de temperatura do aspirado.");
        }
        if (!(p.densidadeFluidoTesteKgL() > 0)) {
            erros.add("A densidade do fluido de teste deve ser maior que zero.");
        }
        if (!(p.tiMinimoMarchaLentaMs() >= 0)) {
            erros.add("O tempo mínimo de injeção na marcha lenta não pode ser negativo.");
        }
        if (p.gruposSimultaneo() < 1 || p.gruposSimultaneo() > 2) {
            erros.add("O modo simultâneo usa 1 ou 2 grupos de saídas.");
        }
        if (p.limiares() == null) {
            erros.add("Informe os limiares do semáforo.");
        }
    }

    /**
     * A perda antes do compressor precisa ser menor que a pressão atmosférica local; senão a entrada do
     * compressor fica com pressão zero ou negativa e a PR perde o sentido (ex.: 15 psi digitado no lugar de 1,5).
     */
    private static void validarPerdaAntesDoCompressor(EntradaDimensionamento entrada, List<String> erros) {
        ConfiguracaoMotor motor = entrada.motor();
        Parametros p = entrada.parametros();
        boolean sobrealimentado = motor.aspiracao() != null && motor.aspiracao().sobrealimentado();
        if (p == null || !sobrealimentado || !(p.perdaAdmissaoKpa() >= 0)
                || !(motor.altitudeLocalM() >= 0 && motor.altitudeLocalM() <= 3000)) {
            return;
        }
        double patmKpa = Formulas.pressaoAtmosfericaKpa(motor.altitudeLocalM());
        if (p.perdaAdmissaoKpa() >= patmKpa) {
            erros.add("A perda antes do compressor (" + formatar(Conversoes.kpaParaPsi(p.perdaAdmissaoKpa()), 1)
                    + " psi) precisa ser menor que a pressão atmosférica local ("
                    + formatar(Conversoes.kpaParaPsi(patmKpa), 1) + " psi).");
        }
    }

    private static void validarBicos(EntradaDimensionamento entrada, List<String> erros) {
        validarBico(entrada.bico(), "Bico", erros);
        validarBico(entrada.bicoSecundario(), "Bico secundário", erros);
        int porCilindro = entrada.motor().bicosPorCilindro();
        if (entrada.bicoSecundario() != null && porCilindro != 2) {
            erros.add("O bico secundário só vale com 2 bicos por cilindro.");
        }
        if (entrada.bicoSecundario() != null && entrada.bico() == null) {
            erros.add("Informe também o bico da bancada primária.");
        }
        if (porCilindro == 2 && entrada.bico() != null && entrada.bicoSecundario() == null) {
            erros.add("Com 2 bicos por cilindro, informe também o bico da bancada secundária.");
        }
    }

    private static void validarBico(Bico bico, String nome, List<String> erros) {
        if (bico == null) {
            return;
        }
        if (!(bico.vazaoNominalCm3Min() > 0)) {
            erros.add(nome + ": a vazão nominal deve ser maior que zero.");
        }
        if (!(bico.pressaoTesteKpa() > 0)) {
            erros.add(nome + ": a pressão de teste deve ser maior que zero.");
        }
        if (!(bico.densidadeFluidoTesteKgL() > 0)) {
            erros.add(nome + ": a densidade do fluido de teste deve ser maior que zero.");
        }
        boolean tempoNegativo = bico.tempoMorto().pontos().stream().anyMatch(p -> !(p.tempoMs() >= 0));
        if (tempoNegativo) {
            erros.add(nome + ": o tempo morto não pode ser negativo.");
        }
    }

    // ===================== A cadeia =====================

    /** Guarda o estado de um cálculo: entrada, memória, semáforo e recomendações. */
    private static final class Cadeia {

        private final EntradaDimensionamento entrada;
        private final ConfiguracaoMotor motor;
        private final Parametros p;
        private final LimiaresSemaforo limiares;
        private final Combustivel combustivel;
        private final boolean sobrealimentado;
        private final int rpm;
        private final MemoriaCalculo memoria = new MemoriaCalculo();
        private final List<Avaliacao> semaforo = new ArrayList<>();
        private final List<String> recomendacoes = new ArrayList<>();

        Cadeia(EntradaDimensionamento entrada) {
            this.entrada = entrada;
            this.motor = entrada.motor();
            this.p = entrada.parametros();
            this.limiares = p.limiares();
            this.combustivel = motor.combustivel();
            this.sobrealimentado = motor.aspiracao().sobrealimentado();
            this.rpm = entrada.meta().rotacaoPotenciaMaxima();
        }

        Resultado<Dimensionamento> calcular() {
            ValidadorMotor.validar(motor).avisos().forEach(memoria::aviso);
            if (combustivel.avisoLambda() != null) {
                memoria.aviso(NivelAviso.AMARELO, combustivel.avisoLambda());
            }

            Dimensionamento.Potencia potencia = potencia();
            Dimensionamento.Combustao combustao = combustao(potencia);
            Dimensionamento.LadoDoAr ladoDoAr = ladoDoAr(combustao);
            Dimensionamento.PorInjecao porInjecao = porInjecao(combustao, ladoDoAr);
            Dimensionamento.BicoNecessario bicoNecessario = bicoNecessario(combustao);
            Dimensionamento.BicoAvaliado bicoAvaliado = avaliarBico(combustao, porInjecao);
            Dimensionamento.BombaNecessaria bomba = bomba(combustao, ladoDoAr);
            Dimensionamento.Esforco esforco = esforco(potencia, ladoDoAr);
            SaidasExigidas.Saidas saidas = saidas();
            Dimensionamento.SensorMap sensorMap = sensorMap(ladoDoAr);
            Dimensionamento.PontoOperacao pontoOperacao = pontoOperacao(combustao, ladoDoAr);
            recomendar(esforco, bicoNecessario, bicoAvaliado, bomba, saidas, sensorMap);

            return memoria.resultado(new Dimensionamento(potencia, combustao, ladoDoAr, porInjecao, bicoNecessario,
                    bicoAvaliado, bomba, esforco, saidas, sensorMap, pontoOperacao, semaforo, recomendacoes));
        }

        // ---------- 1. Potência ----------

        private Dimensionamento.Potencia potencia() {
            Meta meta = entrada.meta();
            double kw = meta.potenciaKw();
            if (meta.pontoMedicao() == PontoMedicao.RODA) {
                double perda = meta.perdaTransmissaoPct() / 100.0;
                kw = memoria.passo("Potência no virabrequim", Formulas.TEXTO_RODA_VIRABREQUIM,
                        Formulas.potenciaNoVirabrequim(meta.potenciaKw(), perda), "kW",
                        e("P_roda", meta.potenciaKw(), "kW"), e("perda", perda, "fração"));
            }
            double cv = Conversoes.kwParaCv(kw);
            double hp = memoria.passo("Potência em hp", Formulas.TEXTO_POTENCIA_HP, Formulas.potenciaHp(cv), "hp",
                    e("P_cv", cv, "cv"));
            double especifica = cv / (motor.cilindradaCc() / 1000.0);
            return new Dimensionamento.Potencia(kw, cv, hp, especifica);
        }

        // ---------- 2. Ar e combustível ----------

        private Dimensionamento.Combustao combustao(Dimensionamento.Potencia potencia) {
            double afrEst = memoria.passo("AFR estequiométrico (" + combustivel.codigo() + ")",
                    combustivel.composicao().formula(), combustivel.afrEstequiometrico(), ":1",
                    combustivel.composicao().entradas().toArray(new Entrada[0]));
            double lambda = entrada.lambdaAlvo();
            double afrAlvo = memoria.passo("AFR alvo", "AFR_alvo = AFR_est × lambda", afrEst * lambda, ":1",
                    e("AFR_est", afrEst, ":1"), e("lambda", lambda, ""));
            double hp = potencia.hp();
            double waLbMin;
            if (p.caminhoAr() == CaminhoAr.K_AR) {
                waLbMin = memoria.passo("Vazão de ar Wa", Formulas.TEXTO_WA_KAR, Formulas.vazaoArPorKar(hp, p.kAr()),
                        "lb/min", e("P_hp", hp, "hp"), e("k_ar", p.kAr(), "hp por lb/min"));
            } else {
                waLbMin = memoria.passo("Vazão de ar Wa", Formulas.TEXTO_WA_BSFC,
                        Formulas.vazaoArPorBsfc(hp, afrAlvo, p.bsfc()), "lb/min",
                        e("P_hp", hp, "hp"), e("AFR", afrAlvo, ":1"), e("BSFC", p.bsfc(), "lb/hp·h"));
            }
            double mfLbH = memoria.passo("Combustível mf", Formulas.TEXTO_COMBUSTIVEL,
                    Formulas.combustivelLbH(waLbMin, afrEst, lambda), "lb/h",
                    e("Wa", waLbMin, "lb/min"), e("AFR_est", afrEst, ":1"), e("lambda", lambda, ""));
            double mfKgH = memoria.passo("Combustível mf", "mf[kg/h] = mf[lb/h] × 0,453592",
                    Conversoes.lbHParaKgH(mfLbH), "kg/h", e("mf", mfLbH, "lb/h"));
            double bsfc = memoria.passo("BSFC (conferência)", Formulas.TEXTO_BSFC, Formulas.bsfc(mfLbH, hp), "lb/hp·h",
                    e("mf", mfLbH, "lb/h"), e("P_hp", hp, "hp"));
            if (combustivel.eGasolina() && sobrealimentado && (bsfc < 0.50 || bsfc > 0.65)) {
                memoria.aviso(NivelAviso.INFORMACAO, "BSFC de " + formatar(bsfc, 3)
                        + " lb/hp·h, fora da faixa de conferência de 0,50 a 0,65 para turbo a gasolina.");
            }
            return new Dimensionamento.Combustao(afrEst, lambda, afrAlvo, Conversoes.lbMinParaKgS(waLbMin), waLbMin,
                    mfKgH, mfLbH, bsfc);
        }

        // ---------- 3. Cilindrada, atmosfera e lado do ar ----------

        private Dimensionamento.LadoDoAr ladoDoAr(Dimensionamento.Combustao combustao) {
            double cilindradaCc = memoria.passo("Cilindrada", "Vd = π/4 × D² × S × n", motor.cilindradaCc(), "cm³",
                    e("D", motor.diametroMm(), "mm"), e("S", motor.cursoMm(), "mm"),
                    e("n", motor.numeroCilindros(), "cilindros"));
            Double informada = motor.cilindradaInformadaCc();
            if (informada != null) {
                memoria.passo("Cilindrada informada (conferência)",
                        "diferença = |informada − calculada| / calculada (aviso acima de 2%)",
                        Math.abs(informada - cilindradaCc) / cilindradaCc, "fração",
                        e("informada", informada, "cm³"), e("calculada", cilindradaCc, "cm³"));
            }
            double cilindradaL = cilindradaCc / 1000.0;
            double vdPol3 = memoria.passo("Cilindrada", "Vd[pol³] = Vd[L] × 61,0237",
                    Conversoes.litrosParaPol3(cilindradaL), "pol³", e("Vd", cilindradaL, "L"));
            double patmKpa = memoria.passo("Pressão atmosférica local", Formulas.TEXTO_PRESSAO_ATMOSFERICA,
                    Formulas.pressaoAtmosfericaKpa(motor.altitudeLocalM()), "kPa", e("h", motor.altitudeLocalM(), "m"));
            return sobrealimentado
                    ? ladoDoArSobrealimentado(combustao.vazaoArLbMin(), vdPol3, cilindradaCc, patmKpa)
                    : ladoDoArAspirado(combustao.vazaoArLbMin(), vdPol3, cilindradaCc, patmKpa);
        }

        private Dimensionamento.LadoDoAr ladoDoArSobrealimentado(double waLbMin, double vdPol3, double cilindradaCc,
                double patmKpa) {
            double patmPsia = Conversoes.kpaParaPsi(patmKpa);
            double perdasPsi = Conversoes.kpaParaPsi(p.perdaDescargaKpa());
            double admissaoPsi = Conversoes.kpaParaPsi(p.perdaAdmissaoKpa());
            double tAmbiente = entrada.temperaturaAmbienteC();
            double t1K = Conversoes.cParaK(tAmbiente);

            IteracaoMapTm iteracao = iterarMapTm(waLbMin, entrada.ve(), rpm, vdPol3, patmPsia, perdasPsi, admissaoPsi,
                    tAmbiente, p.etaC(), p.eps(), MAX_ITERACOES);
            double tmC = iteracao.tmC();
            int iteracoes = iteracao.passos();
            memoria.passo("Iteração MAP ↔ Tm", "começa em Tm = 50 °C; para quando |ΔTm| < 0,01 °C (máximo de 100 passos)",
                    iteracoes, "passos", e("Tm convergida", tmC, "°C"));

            double tmF = Conversoes.cParaF(tmC);
            double mapPsia = memoria.passo("MAP necessário", Formulas.TEXTO_MAP,
                    Formulas.mapPsia(waLbMin, tmF, entrada.ve(), rpm, vdPol3), "psia",
                    e("Wa", waLbMin, "lb/min"), e("Tm", tmF, "°F"), e("VE", entrada.ve(), ""), e("RPM", rpm, "rpm"),
                    e("Vd", vdPol3, "pol³"));
            double mapKpa = memoria.passo("MAP necessário", "MAP[kPa] = MAP[psia] × 6,894757",
                    Conversoes.psiParaKpa(mapPsia), "kPa", e("MAP", mapPsia, "psia"));
            double pr = memoria.passo("Razão de pressão do compressor PR", Formulas.TEXTO_RAZAO_PRESSAO,
                    Formulas.razaoPressao(mapPsia, perdasPsi, patmPsia, admissaoPsi), "",
                    e("MAP", mapPsia, "psia"), e("dP_perdas", perdasPsi, "psi"), e("P_atm", patmPsia, "psia"),
                    e("dP_admissao", admissaoPsi, "psi"));
            double t2K;
            if (pr > 1.0) {
                t2K = memoria.passo("Temperatura na saída do compressor T2", Formulas.TEXTO_T2,
                        Formulas.temperaturaSaidaCompressorK(t1K, pr, p.etaC()), "K",
                        e("T1", t1K, "K"), e("PR", pr, ""), e("eta_c", p.etaC(), ""));
            } else {
                t2K = t1K;
                memoria.aviso(NivelAviso.INFORMACAO, "PR de " + formatar(pr, 2)
                        + " (até 1): a meta não pede pressão positiva; considerado T2 = T1.");
            }
            double t2C = Conversoes.kParaC(t2K);
            double tmFinalC = memoria.passo("Temperatura no coletor Tm", Formulas.TEXTO_TM,
                    Formulas.temperaturaColetor(t2C, p.eps(), tAmbiente), "°C",
                    e("T2", t2C, "°C"), e("eps", p.eps(), ""), e("T_amb", tAmbiente, "°C"));
            return new Dimensionamento.LadoDoAr(true, cilindradaCc, patmKpa, mapKpa, pr, t2C, tmFinalC, iteracoes,
                    mapKpa / patmKpa);
        }

        private Dimensionamento.LadoDoAr ladoDoArAspirado(double waLbMin, double vdPol3, double cilindradaCc,
                double patmKpa) {
            double tAmbiente = entrada.temperaturaAmbienteC();
            double tmC = memoria.passo("Temperatura no coletor (aspirado)", "Tm = T_amb + acréscimo",
                    tAmbiente + p.acrescimoTmAspiradoC(), "°C",
                    e("T_amb", tAmbiente, "°C"), e("acréscimo", p.acrescimoTmAspiradoC(), "°C"));
            double tmF = Conversoes.cParaF(tmC);
            double mapPsia = memoria.passo("MAP necessário", Formulas.TEXTO_MAP,
                    Formulas.mapPsia(waLbMin, tmF, entrada.ve(), rpm, vdPol3), "psia",
                    e("Wa", waLbMin, "lb/min"), e("Tm", tmF, "°F"), e("VE", entrada.ve(), ""), e("RPM", rpm, "rpm"),
                    e("Vd", vdPol3, "pol³"));
            double mapKpa = memoria.passo("MAP necessário", "MAP[kPa] = MAP[psia] × 6,894757",
                    Conversoes.psiParaKpa(mapPsia), "kPa", e("MAP", mapPsia, "psia"));
            double razao = memoria.passo("MAP necessário ÷ P_atm local", "razão = MAP / P_atm", mapKpa / patmKpa, "",
                    e("MAP", mapKpa, "kPa"), e("P_atm", patmKpa, "kPa"));
            semaforo.add(Semaforo.aspiradoMapPatm(razao, limiares));
            return new Dimensionamento.LadoDoAr(false, cilindradaCc, patmKpa, mapKpa, Double.NaN, Double.NaN, tmC, 0,
                    razao);
        }

        // ---------- 4. Massas por injeção ----------

        private Dimensionamento.PorInjecao porInjecao(Dimensionamento.Combustao combustao,
                Dimensionamento.LadoDoAr ladoDoAr) {
            int injecoes = motor.modoInjecao().injecoesPorCiclo();
            double volumeCilindroM3 = motor.volumeCilindroCc() / 1e6;
            double mapPa = ladoDoAr.mapKpa() * 1000.0;
            double tmK = Conversoes.cParaK(ladoDoAr.tmC());
            double massaArG = memoria.passo("Massa de ar por cilindro", Formulas.TEXTO_MASSA_AR,
                    Formulas.massaArCilindroKg(entrada.ve(), mapPa, volumeCilindroM3, tmK) * 1000.0, "g",
                    e("VE", entrada.ve(), ""), e("MAP", mapPa, "Pa"), e("V_cil", volumeCilindroM3, "m³"),
                    e("Tm", tmK, "K"));
            double massaCombustivelMg = memoria.passo("Combustível por injeção", Formulas.TEXTO_MASSA_COMBUSTIVEL,
                    Formulas.massaCombustivelPorInjecao(massaArG, combustao.afrEstequiometrico(),
                            combustao.lambdaAlvo(), injecoes) * 1000.0, "mg",
                    e("m_ar_cil", massaArG, "g"), e("AFR_est", combustao.afrEstequiometrico(), ":1"),
                    e("lambda", combustao.lambdaAlvo(), ""), e("injecoesPorCiclo", injecoes, ""));
            double cicloMs = memoria.passo("Duração do ciclo", Formulas.TEXTO_CICLO, Formulas.cicloMs(rpm), "ms",
                    e("RPM", rpm, "rpm"));
            return new Dimensionamento.PorInjecao(injecoes, cicloMs, massaArG * 1000.0, massaCombustivelMg);
        }

        // ---------- 5. Bico necessário (fórmula da 7.1, sem tempo morto) ----------

        private Dimensionamento.BicoNecessario bicoNecessario(Dimensionamento.Combustao combustao) {
            int numeroBicos = motor.numeroCilindros() * motor.bicosPorCilindro();
            double densidade = combustivel.densidadeKgL();
            double vazaoMassaGMin = memoria.passo("Vazão por bico (massa)", Formulas.TEXTO_VAZAO_POR_BICO,
                    Conversoes.kgHParaGMin(Formulas.vazaoPorBico(combustao.combustivelKgH(), numeroBicos, p.dcMax())),
                    "g/min", e("mf", combustao.combustivelKgH(), "kg/h"), e("N_bicos", numeroBicos, ""),
                    e("DC_max", p.dcMax(), ""));
            double vazaoCombustivel = memoria.passo("Vazão por bico (combustível)", "Q = Q_bico / rho_comb",
                    vazaoMassaGMin / densidade, "cm³/min",
                    e("Q_bico", vazaoMassaGMin, "g/min"), e("rho_comb", densidade, "kg/L"));

            Bico bico = entrada.bico();
            double densidadeTeste = bico != null ? bico.densidadeFluidoTesteKgL() : p.densidadeFluidoTesteKgL();
            double pressaoTeste = bico != null ? bico.pressaoTesteKpa() : entrada.pressaoBaseKpa();
            double noFluidoTeste = memoria.passo("Vazão por bico no fluido de teste",
                    "Q_teste = Q_comb × raiz(rho_comb / rho_teste)  (inverso de " + Formulas.TEXTO_FLUIDO_TESTE + ")",
                    Formulas.vazaoNoCombustivel(vazaoCombustivel, densidade, densidadeTeste), "cm³/min",
                    e("Q_comb", vazaoCombustivel, "cm³/min"), e("rho_comb", densidade, "kg/L"),
                    e("rho_teste", densidadeTeste, "kg/L"));
            double nominal = memoria.passo("Vazão nominal mínima do bico (sem tempo morto)",
                    Formulas.TEXTO_CORRECAO_PRESSAO,
                    Formulas.corrigirPorPressao(noFluidoTeste, entrada.pressaoBaseKpa(), pressaoTeste), "cm³/min",
                    e("Q1", noFluidoTeste, "cm³/min"), e("dP1 (base)", entrada.pressaoBaseKpa(), "kPa"),
                    e("dP2 (teste)", pressaoTeste, "kPa"));
            return new Dimensionamento.BicoNecessario(numeroBicos, vazaoMassaGMin, vazaoCombustivel, nominal,
                    pressaoTeste, densidadeTeste);
        }

        // ---------- 6. Bico informado: TI, DC, tempo morto e marcha lenta ----------

        private Dimensionamento.BicoAvaliado avaliarBico(Dimensionamento.Combustao combustao,
                Dimensionamento.PorInjecao porInjecao) {
            Bico primario = entrada.bico();
            if (primario == null) {
                memoria.aviso(NivelAviso.INFORMACAO, "Nenhum bico informado: o DC e a marcha lenta não foram "
                        + "avaliados. Use a vazão nominal mínima como referência.");
                return null;
            }
            Bico secundario = entrada.bicoSecundario();
            boolean duasBancadas = secundario != null;
            int injecoes = porInjecao.injecoesPorCiclo();
            double massaPorInjecaoKg = porInjecao.massaCombustivelPorInjecaoMg() / 1e6;

            double realPrimario = vazaoReal(primario, duasBancadas ? "primário" : null);
            double realSecundario = duasBancadas ? vazaoReal(secundario, "secundário") : Double.NaN;
            double densidade = combustivel.densidadeKgL();
            double vazaoKgS = (realPrimario + (duasBancadas ? realSecundario : 0.0)) * densidade / 60000.0;

            double tiEfetivo = memoria.passo("Tempo de injeção efetivo",
                    duasBancadas ? "TI_ef = m_f_inj / (mdot_primário + mdot_secundário)" : "TI_ef = m_f_inj / mdot_bico",
                    Formulas.tempoInjecaoMs(massaPorInjecaoKg, vazaoKgS, 0.0), "ms",
                    e("m_f_inj", porInjecao.massaCombustivelPorInjecaoMg(), "mg"), e("mdot", vazaoKgS * 1000.0, "g/s"));

            double mortoPrimario = tempoMorto(primario, duasBancadas ? "Bico primário" : "Bico");
            double tiPrimario = memoria.passo("TI com tempo morto" + (duasBancadas ? " (primário)" : ""),
                    Formulas.TEXTO_TEMPO_INJECAO, tiEfetivo + mortoPrimario, "ms",
                    e("TI_ef", tiEfetivo, "ms"), e("t_morto", mortoPrimario, "ms"));
            double dc = memoria.passo("DC do bico" + (duasBancadas ? " (primário)" : ""), Formulas.TEXTO_DC,
                    Formulas.dutyCycle(tiPrimario, injecoes, rpm), "fração",
                    e("TI", tiPrimario, "ms"), e("injecoesPorCiclo", injecoes, ""), e("RPM", rpm, "rpm"));

            double mortoSecundario = Double.NaN;
            double tiSecundario = Double.NaN;
            if (duasBancadas) {
                mortoSecundario = tempoMorto(secundario, "Bico secundário");
                tiSecundario = memoria.passo("TI com tempo morto (secundário)", Formulas.TEXTO_TEMPO_INJECAO,
                        tiEfetivo + mortoSecundario, "ms", e("TI_ef", tiEfetivo, "ms"), e("t_morto", mortoSecundario, "ms"));
                double dcSecundario = memoria.passo("DC do bico (secundário)", Formulas.TEXTO_DC,
                        Formulas.dutyCycle(tiSecundario, injecoes, rpm), "fração",
                        e("TI", tiSecundario, "ms"), e("injecoesPorCiclo", injecoes, ""), e("RPM", rpm, "rpm"));
                dc = Math.max(dc, dcSecundario);
            }
            semaforo.add(Semaforo.dcBico(dc, limiares));

            double nominalMinima = duasBancadas
                    ? Double.NaN
                    : vazaoNominalMinimaComTempoMorto(primario, mortoPrimario, massaPorInjecaoKg, porInjecao);
            double tiMarchaLenta = marchaLenta(combustao, porInjecao, realPrimario * densidade / 60000.0);

            return new Dimensionamento.BicoAvaliado(realPrimario, realSecundario, mortoPrimario, mortoSecundario,
                    tiEfetivo, tiPrimario, tiSecundario, dc, nominalMinima, tiMarchaLenta);
        }

        /** Vazão real do bico no combustível e na pressão de base (correção de pressão + fluido de teste). */
        private double vazaoReal(Bico bico, String bancada) {
            double densidade = combustivel.densidadeKgL();
            double naPressao = Formulas.corrigirPorPressao(bico.vazaoNominalCm3Min(), bico.pressaoTesteKpa(),
                    entrada.pressaoBaseKpa());
            return memoria.passo("Vazão real do bico" + (bancada == null ? "" : " (" + bancada + ")"),
                    "Q_real = Q_nom × raiz(dP_base / dP_teste) × raiz(rho_teste / rho_comb)",
                    Formulas.vazaoNoCombustivel(naPressao, bico.densidadeFluidoTesteKgL(), densidade), "cm³/min",
                    e("Q_nom", bico.vazaoNominalCm3Min(), "cm³/min"), e("dP_base", entrada.pressaoBaseKpa(), "kPa"),
                    e("dP_teste", bico.pressaoTesteKpa(), "kPa"), e("rho_teste", bico.densidadeFluidoTesteKgL(), "kg/L"),
                    e("rho_comb", densidade, "kg/L"));
        }

        private double tempoMorto(Bico bico, String nome) {
            OptionalDouble valor = bico.tempoMorto().valorMs(entrada.tensaoV(), entrada.pressaoBaseKpa());
            if (valor.isEmpty()) {
                throw new EntradaInvalidaException(nome + ": a tabela de tempo morto não cobre a condição pedida "
                        + "(informe uma tensão dentro da tabela).");
            }
            return valor.getAsDouble();
        }

        /**
         * Menor vazão nominal que mantém o DC no máximo já com o tempo morto (inverso das fórmulas de TI e DC):
         * janela = DC_max × ciclo / injeções − t_morto; vazão = m_f_inj / janela.
         */
        private double vazaoNominalMinimaComTempoMorto(Bico bico, double tempoMortoMs, double massaPorInjecaoKg,
                Dimensionamento.PorInjecao porInjecao) {
            double janelaMs = p.dcMax() * porInjecao.cicloMs() / porInjecao.injecoesPorCiclo() - tempoMortoMs;
            if (janelaMs <= 0) {
                memoria.aviso(NivelAviso.VERMELHO, "O tempo morto do bico (" + formatar(tempoMortoMs, 2)
                        + " ms) ocupa toda a janela de injeção na rotação de potência máxima.");
                return Double.NaN;
            }
            double densidade = combustivel.densidadeKgL();
            double vazaoCombustivel = massaPorInjecaoKg / (janelaMs / 1000.0) * 60000.0 / densidade;
            double noFluidoTeste = Formulas.vazaoNoCombustivel(vazaoCombustivel, densidade, bico.densidadeFluidoTesteKgL());
            return memoria.passo("Vazão nominal mínima com tempo morto",
                    "janela = DC_max × ciclo / injecoesPorCiclo − t_morto; Q = m_f_inj / janela, levada ao fluido e à "
                            + "pressão de teste",
                    Formulas.corrigirPorPressao(noFluidoTeste, entrada.pressaoBaseKpa(), bico.pressaoTesteKpa()), "cm³/min",
                    e("DC_max", p.dcMax(), ""), e("ciclo", porInjecao.cicloMs(), "ms"),
                    e("injecoesPorCiclo", porInjecao.injecoesPorCiclo(), ""), e("t_morto", tempoMortoMs, "ms"),
                    e("janela", janelaMs, "ms"), e("m_f_inj", massaPorInjecaoKg * 1e6, "mg"));
        }

        /** Checagem da marcha lenta (SPEC 7.3). Devolve NaN se a marcha lenta não foi informada. */
        private double marchaLenta(Dimensionamento.Combustao combustao, Dimensionamento.PorInjecao porInjecao,
                double vazaoPrimarioKgS) {
            MarchaLenta marchaLenta = entrada.marchaLenta();
            if (marchaLenta == null) {
                memoria.aviso(NivelAviso.INFORMACAO, "Marcha lenta não verificada: informe o MAP e a VE de marcha "
                        + "lenta para conferir o tempo mínimo de injeção.");
                return Double.NaN;
            }
            double temperaturaC = entrada.temperaturaAmbienteC() + p.acrescimoTmAspiradoC();
            double massaArKg = Formulas.massaArCilindroKg(marchaLenta.ve(), marchaLenta.mapKpa() * 1000.0,
                    motor.volumeCilindroCc() / 1e6, Conversoes.cParaK(temperaturaC));
            double massaCombustivelKg = Formulas.massaCombustivelPorInjecao(massaArKg, combustao.afrEstequiometrico(),
                    LAMBDA_MARCHA_LENTA, porInjecao.injecoesPorCiclo());
            double tiMs = memoria.passo("TI efetivo na marcha lenta",
                    "TI_ef = m_f_inj (marcha lenta, lambda 1,00) / mdot do bico primário",
                    Formulas.tempoInjecaoMs(massaCombustivelKg, vazaoPrimarioKgS, 0.0), "ms",
                    e("MAP", marchaLenta.mapKpa(), "kPa"), e("VE", marchaLenta.ve(), ""), e("T", temperaturaC, "°C"),
                    e("lambda", LAMBDA_MARCHA_LENTA, ""), e("m_f_inj", massaCombustivelKg * 1e6, "mg"),
                    e("TI mínimo", p.tiMinimoMarchaLentaMs(), "ms"));
            if (tiMs < p.tiMinimoMarchaLentaMs()) {
                memoria.aviso(NivelAviso.AMARELO, "TI efetivo na marcha lenta de " + formatar(tiMs, 2)
                        + " ms, abaixo de " + formatar(p.tiMinimoMarchaLentaMs(), 1)
                        + " ms: bico grande para a marcha lenta. Avaliar duas bancadas por cilindro (primária menor).");
            }
            return tiMs;
        }

        // ---------- 7. Bomba ----------

        private Dimensionamento.BombaNecessaria bomba(Dimensionamento.Combustao combustao,
                Dimensionamento.LadoDoAr ladoDoAr) {
            double densidade = combustivel.densidadeKgL();
            double vazao = memoria.passo("Vazão de combustível na bomba", Formulas.TEXTO_BOMBA,
                    Formulas.vazaoBombaLh(combustao.combustivelKgH(), densidade), "L/h",
                    e("mf", combustao.combustivelKgH(), "kg/h"), e("rho_comb", densidade, "kg/L"));
            double pressaoTurbo = Math.max(0.0, ladoDoAr.mapKpa() - ladoDoAr.patmKpa());
            double pressao = memoria.passo("Pressão exigida da bomba", "p = pressão de base + pressão do turbo (MAP − P_atm)",
                    entrada.pressaoBaseKpa() + pressaoTurbo, "kPa",
                    e("pressão de base", entrada.pressaoBaseKpa(), "kPa"), e("pressão do turbo", pressaoTurbo, "kPa"));
            double comFolga = memoria.passo("Vazão de bomba com folga verde", "Bomba × (1 + folga verde)",
                    vazao * (1.0 + limiares.folgaBombaVerde()), "L/h",
                    e("Bomba", vazao, "L/h"), e("folga verde", limiares.folgaBombaVerde(), "fração"));
            double folga = Double.NaN;
            Double disponivel = entrada.vazaoBombaDisponivelLh();
            if (disponivel != null) {
                folga = memoria.passo("Folga da bomba", "folga = vazão disponível / vazão necessária − 1",
                        disponivel / vazao - 1.0, "fração", e("disponível", disponivel, "L/h"), e("necessária", vazao, "L/h"));
                semaforo.add(Semaforo.folgaBomba(folga, limiares));
            }
            return new Dimensionamento.BombaNecessaria(vazao, pressao, comFolga, folga);
        }

        // ---------- 8. Esforço ----------

        private Dimensionamento.Esforco esforco(Dimensionamento.Potencia potencia, Dimensionamento.LadoDoAr ladoDoAr) {
            double cilindradaL = ladoDoAr.cilindradaCc() / 1000.0;
            double kw = potencia.virabrequimKw();
            double bmep = memoria.passo("BMEP", Formulas.TEXTO_BMEP, Formulas.bmepBar(kw, cilindradaL, rpm), "bar",
                    e("P", kw, "kW"), e("Vd", cilindradaL, "L"), e("RPM", rpm, "rpm"));
            semaforo.add(Semaforo.bmep(bmep, sobrealimentado, motor.pistoes() == Construcao.FORJADO,
                    motor.bielas() == Construcao.FORJADO, limiares));
            double torque = memoria.passo("Torque na potência máxima", Formulas.TEXTO_TORQUE,
                    Formulas.torqueNm(kw, rpm), "N·m", e("P", kw, "kW"), e("RPM", rpm, "rpm"));
            double cursoM = motor.cursoMm() / 1000.0;
            double velocidade = memoria.passo("Velocidade média do pistão", Formulas.TEXTO_VELOCIDADE_PISTAO,
                    Formulas.velocidadeMediaPistao(cursoM, rpm), "m/s", e("curso", cursoM, "m"), e("RPM", rpm, "rpm"));
            semaforo.add(Semaforo.velocidadePistao(velocidade, limiares));
            memoria.passo("Potência específica", "P_cv / Vd[L]", potencia.especificaCvPorLitro(), "cv/L",
                    e("P_cv", potencia.cv(), "cv"), e("Vd", cilindradaL, "L"));
            return new Dimensionamento.Esforco(bmep, torque, velocidade);
        }

        // ---------- 9. Saídas, sensor MAP e ponto de operação ----------

        private SaidasExigidas.Saidas saidas() {
            SaidasExigidas.Saidas saidas = SaidasExigidas.calcular(motor, p.gruposSimultaneo());
            String regraBicos = switch (motor.modoInjecao()) {
                case SEQUENCIAL -> "sequencial: n × bicos por cilindro";
                case SEMISSEQUENCIAL -> "semissequencial: (n / 2) × bicos por cilindro";
                case SIMULTANEO -> "simultâneo: grupos × bicos por cilindro";
            };
            String regraIgnicao = switch (motor.modoIgnicao()) {
                case SEQUENCIAL -> "sequencial: n";
                case CENTELHA_PERDIDA -> "centelha perdida: n / 2";
                case DISTRIBUIDOR -> "distribuidor: 1";
            };
            memoria.passo("Saídas de bico exigidas", regraBicos, saidas.bicos(), "saídas",
                    e("n", motor.numeroCilindros(), "cilindros"), e("bicos por cilindro", motor.bicosPorCilindro(), ""));
            memoria.passo("Saídas de ignição exigidas", regraIgnicao, saidas.ignicao(), "saídas",
                    e("n", motor.numeroCilindros(), "cilindros"));
            Injecao injecao = entrada.injecao();
            if (injecao != null) {
                boolean sequencial = motor.modoInjecao() == ModoInjecao.SEQUENCIAL
                        || motor.modoIgnicao() == ModoIgnicao.SEQUENCIAL;
                semaforo.add(Semaforo.saidasInjecao(injecao.nome(), saidas.bicosTotal(), injecao.saidasBico(),
                        saidas.ignicao(), injecao.saidasIgnicao(), injecao.maxCilindrosSequencial(),
                        motor.numeroCilindros(), sequencial));
            }
            return saidas;
        }

        private Dimensionamento.SensorMap sensorMap(Dimensionamento.LadoDoAr ladoDoAr) {
            double mapMaximo = memoria.passo("MAP máximo previsto", "maior entre o MAP necessário e a P_atm local",
                    Math.max(ladoDoAr.mapKpa(), ladoDoAr.patmKpa()), "kPa",
                    e("MAP necessário", ladoDoAr.mapKpa(), "kPa"), e("P_atm", ladoDoAr.patmKpa(), "kPa"));
            double faixaMinima = memoria.passo("Faixa mínima do sensor MAP",
                    "faixa = " + formatar(limiares.sensorMapVerde(), 1) + " × MAP máximo previsto",
                    limiares.sensorMapVerde() * mapMaximo, "kPa", e("MAP máximo", mapMaximo, "kPa"));
            if (entrada.faixaSensorMapKpa() != null) {
                semaforo.add(Semaforo.faixaSensorMap(entrada.faixaSensorMapKpa(), mapMaximo, limiares));
            }
            return new Dimensionamento.SensorMap(mapMaximo, faixaMinima);
        }

        private Dimensionamento.PontoOperacao pontoOperacao(Dimensionamento.Combustao combustao,
                Dimensionamento.LadoDoAr ladoDoAr) {
            if (!sobrealimentado) {
                return null;
            }
            semaforo.add(Semaforo.compressorSemMapa());
            if (motor.aspiracao() == Aspiracao.BITURBO) {
                memoria.aviso(NivelAviso.INFORMACAO, "Biturbo: se os turbos trabalham em paralelo, cada um recebe "
                        + "metade da vazão (" + formatar(combustao.vazaoArLbMin() / 2.0, 2)
                        + " lb/min) na mesma PR. Confirmar a montagem.");
            }
            return new Dimensionamento.PontoOperacao(combustao.vazaoArKgS(), combustao.vazaoArLbMin(),
                    ladoDoAr.razaoPressao());
        }

        // ---------- 10. Recomendações ----------

        private void recomendar(Dimensionamento.Esforco esforco, Dimensionamento.BicoNecessario bicoNecessario,
                Dimensionamento.BicoAvaliado bicoAvaliado, Dimensionamento.BombaNecessaria bomba,
                SaidasExigidas.Saidas saidas, Dimensionamento.SensorMap sensorMap) {
            String intercooler = Recomendacoes.intercooler(motor.aspiracao(), entrada.meta().uso());
            if (intercooler != null) {
                recomendacoes.add(intercooler);
            }
            String radiador = Recomendacoes.radiadorDeOleo(entrada.meta().uso(), esforco.bmepBar(), sobrealimentado,
                    limiares);
            if (radiador != null) {
                recomendacoes.add(radiador);
            }
            String bico = "Bico: vazão nominal de pelo menos " + formatar(bicoNecessario.vazaoNominalCm3Min(), 0)
                    + " cm³/min a " + formatar(Conversoes.kpaParaBar(bicoNecessario.pressaoTesteKpa()), 1)
                    + " bar (fluido de teste " + formatar(bicoNecessario.densidadeTesteKgL(), 2) + " kg/L)";
            if (bicoAvaliado != null && Double.isFinite(bicoAvaliado.vazaoNominalMinimaCm3Min())) {
                bico += "; com o tempo morto do bico informado, pelo menos "
                        + formatar(bicoAvaliado.vazaoNominalMinimaCm3Min(), 0) + " cm³/min";
            }
            recomendacoes.add(bico + ".");
            recomendacoes.add("Bomba: pelo menos " + formatar(bomba.vazaoComFolgaVerdeLh(), 0) + " L/h a "
                    + formatar(Conversoes.kpaParaBar(bomba.pressaoKpa()), 2) + " bar (necessário + "
                    + formatar(limiares.folgaBombaVerde() * 100, 0) + "% de folga).");
            recomendacoes.add("Sensor MAP: faixa de pelo menos " + formatar(sensorMap.faixaMinimaKpa(), 0)
                    + " kPa absolutos.");
            String injecao = "Injeção: pelo menos " + saidas.bicos() + " saídas de bico";
            if (saidas.bicosExtras() > 0) {
                injecao += " (mais " + saidas.bicosExtras() + " para os bicos extras)";
            }
            recomendacoes.add(injecao + " e " + saidas.ignicao() + " de ignição.");
        }
    }
}
