package br.com.calibra.core.calculo;

import br.com.calibra.core.unidades.Conversoes;

/**
 * As fórmulas da SPEC 7.1, uma função por fórmula, nas unidades em que a spec as escreve. Cada
 * função tem um teste próprio. O texto de cada fórmula, usado na memória de cálculo, fica na
 * constante {@code TEXTO_...} logo acima dela.
 */
public final class Formulas {

    /** Constante da fórmula do MAP no método Garrett (SPEC 7.1). */
    public static final double CONSTANTE_MAP_GARRETT = 639.6;
    /** Constante do ar: 287,05 J/(kg·K) (SPEC 7.1). */
    public static final double R_AR = 287.05;

    private Formulas() {
    }

    // ---------- Ar e combustível ----------

    public static final String TEXTO_POTENCIA_HP = "P_hp = P_cv × 0,98632";

    public static double potenciaHp(double potenciaCv) {
        return potenciaCv * Conversoes.HP_POR_CV;
    }

    public static final String TEXTO_WA_KAR = "Wa = P_hp / k_ar";

    /** Vazão de ar em lb/min pelo fator k_ar (hp por lb/min). */
    public static double vazaoArPorKar(double potenciaHp, double kAr) {
        return potenciaHp / kAr;
    }

    public static final String TEXTO_WA_BSFC = "Wa = P_hp × AFR × BSFC / 60";

    /** Vazão de ar em lb/min pelo BSFC (lb/hp·h) e pela relação ar/combustível alvo (caminho da Garrett). */
    public static double vazaoArPorBsfc(double potenciaHp, double afr, double bsfc) {
        return potenciaHp * afr * bsfc / 60.0;
    }

    public static final String TEXTO_COMBUSTIVEL = "mf = 60 × Wa / (AFR_est × lambda)";

    /** Vazão de combustível em lb/h a partir da vazão de ar em lb/min. */
    public static double combustivelLbH(double vazaoArLbMin, double afrEstequiometrico, double lambda) {
        return 60.0 * vazaoArLbMin / (afrEstequiometrico * lambda);
    }

    public static final String TEXTO_BSFC = "BSFC = mf / P_hp";

    /** Consumo específico em lb/hp·h. Só conferência. */
    public static double bsfc(double combustivelLbH, double potenciaHp) {
        return combustivelLbH / potenciaHp;
    }

    // ---------- Bicos e bomba ----------

    public static final String TEXTO_VAZAO_POR_BICO = "Q_bico = mf / (N_bicos × DC_max)";

    /** Vazão de massa por bico, na mesma unidade da vazão total de combustível. */
    public static double vazaoPorBico(double combustivel, int numeroBicos, double dcMaximo) {
        return combustivel / (numeroBicos * dcMaximo);
    }

    public static final String TEXTO_CORRECAO_PRESSAO = "Q2 = Q1 × raiz(dP2 / dP1)";

    /** Corrige a vazão do bico pela pressão diferencial (combustível menos coletor). */
    public static double corrigirPorPressao(double vazao1, double pressaoDiferencial1, double pressaoDiferencial2) {
        return vazao1 * Math.sqrt(pressaoDiferencial2 / pressaoDiferencial1);
    }

    public static final String TEXTO_FLUIDO_TESTE = "Q_comb = Q_teste × raiz(rho_teste / rho_comb)";

    /** Converte a vazão medida com o fluido de teste para vazão em volume do combustível real. */
    public static double vazaoNoCombustivel(double vazaoTeste, double densidadeTeste, double densidadeCombustivel) {
        return vazaoTeste * Math.sqrt(densidadeTeste / densidadeCombustivel);
    }

    public static final String TEXTO_BOMBA = "Bomba = mf / rho_comb";

    /** Vazão da bomba em L/h a partir do combustível em kg/h e da densidade em kg/L. */
    public static double vazaoBombaLh(double combustivelKgH, double densidadeKgL) {
        return combustivelKgH / densidadeKgL;
    }

    // ---------- Turbo (método Garrett) ----------

    public static final String TEXTO_MAP = "MAP = Wa × 639,6 × (460 + Tm[°F]) / (VE × (RPM/2) × Vd[pol³])";

    /** Pressão absoluta no coletor em psia. */
    public static double mapPsia(double vazaoArLbMin, double tmF, double ve, double rpm, double cilindradaPol3) {
        return vazaoArLbMin * CONSTANTE_MAP_GARRETT * (460.0 + tmF) / (ve * (rpm / 2.0) * cilindradaPol3);
    }

    public static final String TEXTO_WA_PELO_MAP = "Wa = MAP × VE × (RPM/2) × Vd[pol³] / (639,6 × (460 + Tm[°F]))";

    /**
     * A fórmula do MAP ao contrário (núcleo 1.1.0): vazão de ar em lb/min com o MAP conhecido, em psia. Serve à meta
     * pela pressão e ao ponto de torque máximo.
     */
    public static double vazaoArPeloMap(double mapPsia, double tmF, double ve, double rpm, double cilindradaPol3) {
        return mapPsia * ve * (rpm / 2.0) * cilindradaPol3 / (CONSTANTE_MAP_GARRETT * (460.0 + tmF));
    }

    public static final String TEXTO_MAP_PELA_PRESSAO = "MAP = P_atm + pressão relativa";

    /** MAP absoluto a partir da pressão relativa do turbo (meta pela pressão), na mesma unidade das duas. */
    public static double mapPelaPressao(double patm, double pressaoRelativa) {
        return patm + pressaoRelativa;
    }

    public static final String TEXTO_POTENCIA_POR_KAR = "P_hp = Wa × k_ar";

    /** Potência em hp pela vazão de ar em lb/min (inverso de Wa = P_hp / k_ar). */
    public static double potenciaPorKar(double vazaoArLbMin, double kAr) {
        return vazaoArLbMin * kAr;
    }

    public static final String TEXTO_POTENCIA_POR_BSFC = "P_hp = 60 × Wa / (AFR × BSFC)";

    /** Potência em hp pela vazão de ar em lb/min (inverso de Wa = P_hp × AFR × BSFC / 60). */
    public static double potenciaPorBsfc(double vazaoArLbMin, double afr, double bsfc) {
        return 60.0 * vazaoArLbMin / (afr * bsfc);
    }

    public static final String TEXTO_VAZAO_CORRIGIDA = "Wc = Wa × raiz(T1 / T_ref) / (P1 / P_ref)";

    /**
     * Vazão corrigida para o mapa do compressor: T1 e P1 na entrada do compressor, T_ref e P_ref as do próprio mapa
     * (temperaturas absolutas; pressões absolutas na mesma unidade). A vazão sai na unidade de Wa.
     */
    public static double vazaoCorrigida(double vazaoAr, double t1K, double p1, double tReferenciaK, double pReferencia) {
        return vazaoAr * Math.sqrt(t1K / tReferenciaK) / (p1 / pReferencia);
    }

    public static final String TEXTO_RAZAO_PRESSAO = "PR = (MAP + dP_perdas) / (P_atm − dP_admissao)";

    /** Razão de pressão do compressor; todas as pressões em psi (absolutas, menos as perdas). */
    public static double razaoPressao(double mapPsia, double perdasPsi, double patmPsia, double admissaoPsi) {
        return (mapPsia + perdasPsi) / (patmPsia - admissaoPsi);
    }

    public static final String TEXTO_PRESSAO_ATMOSFERICA = "P_atm = 101,325 × (1 − 2,25577e−5 × h)^5,25588";

    public static double pressaoAtmosfericaKpa(double altitudeM) {
        return 101.325 * Math.pow(1.0 - 2.25577e-5 * altitudeM, 5.25588);
    }

    public static final String TEXTO_T2 = "T2 = T1 × (1 + (PR^0,2857 − 1) / eta_c)";

    /** Temperatura na saída do compressor, em kelvin. */
    public static double temperaturaSaidaCompressorK(double t1K, double razaoPressao, double etaC) {
        return t1K * (1.0 + (Math.pow(razaoPressao, 0.2857) - 1.0) / etaC);
    }

    public static final String TEXTO_TM = "Tm = T2 − eps × (T2 − T_amb)";

    /** Temperatura no coletor depois do intercooler. Use a mesma unidade (°C ou K) nas três temperaturas. */
    public static double temperaturaColetor(double t2, double eps, double temperaturaAmbiente) {
        return t2 - eps * (t2 - temperaturaAmbiente);
    }

    // ---------- Tempo de injeção ----------

    public static final String TEXTO_MASSA_AR = "m_ar_cil = VE × MAP[Pa] × V_cil[m³] / (287,05 × Tm[K])";

    public static double massaArCilindroKg(double ve, double mapPa, double volumeCilindroM3, double tmK) {
        return ve * mapPa * volumeCilindroM3 / (R_AR * tmK);
    }

    public static final String TEXTO_MASSA_COMBUSTIVEL = "m_f_inj = m_ar_cil / (AFR_est × lambda × injecoesPorCiclo)";

    /** Massa de combustível por injeção, na mesma unidade da massa de ar. */
    public static double massaCombustivelPorInjecao(double massaArCilindro, double afrEstequiometrico, double lambda,
            int injecoesPorCiclo) {
        return massaArCilindro / (afrEstequiometrico * lambda * injecoesPorCiclo);
    }

    public static final String TEXTO_TEMPO_INJECAO = "TI = m_f_inj / mdot_bico + t_morto";

    /** Tempo de injeção em ms: massa por injeção (kg) ÷ vazão do bico (kg/s), mais o tempo morto (ms). */
    public static double tempoInjecaoMs(double massaPorInjecaoKg, double vazaoBicoKgS, double tempoMortoMs) {
        return massaPorInjecaoKg / vazaoBicoKgS * 1000.0 + tempoMortoMs;
    }

    public static final String TEXTO_CICLO = "ciclo = 120000 / RPM";

    /** Duração de um ciclo de 4 tempos (duas voltas) em ms. */
    public static double cicloMs(double rpm) {
        return 120000.0 / rpm;
    }

    public static final String TEXTO_DC = "DC = TI × injecoesPorCiclo / (120000 / RPM)";

    /** Fração do ciclo em que o bico fica aberto (0,80 = 80%). */
    public static double dutyCycle(double tempoInjecaoMs, int injecoesPorCiclo, double rpm) {
        return tempoInjecaoMs * injecoesPorCiclo / cicloMs(rpm);
    }

    // ---------- Esforço ----------

    public static final String TEXTO_BMEP = "BMEP = 1200 × P[kW] / (Vd[L] × RPM)";

    public static double bmepBar(double potenciaKw, double cilindradaL, double rpm) {
        return 1200.0 * potenciaKw / (cilindradaL * rpm);
    }

    public static final String TEXTO_VELOCIDADE_PISTAO = "c_m = 2 × curso[m] × RPM / 60";

    public static double velocidadeMediaPistao(double cursoM, double rpm) {
        return 2.0 * cursoM * rpm / 60.0;
    }

    public static final String TEXTO_TORQUE = "T = 9549 × P[kW] / RPM";

    public static double torqueNm(double potenciaKw, double rpm) {
        return 9549.0 * potenciaKw / rpm;
    }

    // ---------- Meta (SPEC 6.4) ----------

    public static final String TEXTO_RODA_VIRABREQUIM = "P_vira = P_roda / (1 − perda)";

    /** Converte a potência medida na roda para o virabrequim. A perda é fração (0,15 = 15%). */
    public static double potenciaNoVirabrequim(double potenciaNaRoda, double perdaFracao) {
        return potenciaNaRoda / (1.0 - perdaFracao);
    }
}
