package br.com.calibra.core.unidades;

/**
 * Conversões de unidade. As constantes são as da SPEC, seção 7.1.
 *
 * <p>O núcleo trabalha em unidades métricas (kW, kPa, °C, kg/h, cm³...). As unidades inglesas
 * (hp, psi, lb, °F, pol³) só aparecem dentro das fórmulas da Garrett e na interface.
 */
public final class Conversoes {

    /** 1 cv = 0,98632 hp (SPEC 7.1). */
    public static final double HP_POR_CV = 0.98632;
    /** 1 cv = 0,73550 kW (SPEC 7.1). */
    public static final double KW_POR_CV = 0.73550;
    /** 1 lb = 0,453592 kg (SPEC 7.1). */
    public static final double KG_POR_LB = 0.453592;
    /** 1 psi = 6,894757 kPa (SPEC 7.1). */
    public static final double KPA_POR_PSI = 6.894757;
    /** 1 L = 61,0237 pol³ (SPEC 7.1). */
    public static final double POL3_POR_LITRO = 61.0237;
    /** 0 °C = 273,15 K. */
    public static final double KELVIN_DE_ZERO_C = 273.15;

    private Conversoes() {
    }

    // ---------- Potência ----------

    public static double cvParaKw(double cv) {
        return cv * KW_POR_CV;
    }

    public static double kwParaCv(double kw) {
        return kw / KW_POR_CV;
    }

    public static double cvParaHp(double cv) {
        return cv * HP_POR_CV;
    }

    public static double hpParaCv(double hp) {
        return hp / HP_POR_CV;
    }

    public static double kwParaHp(double kw) {
        return cvParaHp(kwParaCv(kw));
    }

    public static double hpParaKw(double hp) {
        return cvParaKw(hpParaCv(hp));
    }

    // ---------- Pressão ----------

    public static double psiParaKpa(double psi) {
        return psi * KPA_POR_PSI;
    }

    public static double kpaParaPsi(double kpa) {
        return kpa / KPA_POR_PSI;
    }

    public static double barParaKpa(double bar) {
        return bar * 100.0;
    }

    public static double kpaParaBar(double kpa) {
        return kpa / 100.0;
    }

    // ---------- Temperatura ----------

    public static double cParaK(double celsius) {
        return celsius + KELVIN_DE_ZERO_C;
    }

    public static double kParaC(double kelvin) {
        return kelvin - KELVIN_DE_ZERO_C;
    }

    public static double cParaF(double celsius) {
        return celsius * 9.0 / 5.0 + 32.0;
    }

    public static double fParaC(double fahrenheit) {
        return (fahrenheit - 32.0) * 5.0 / 9.0;
    }

    // ---------- Massa e vazão de massa ----------

    public static double lbParaKg(double lb) {
        return lb * KG_POR_LB;
    }

    public static double kgParaLb(double kg) {
        return kg / KG_POR_LB;
    }

    public static double lbMinParaKgS(double lbPorMin) {
        return lbPorMin * KG_POR_LB / 60.0;
    }

    public static double kgSParaLbMin(double kgPorS) {
        return kgPorS / KG_POR_LB * 60.0;
    }

    public static double lbHParaKgH(double lbPorH) {
        return lbPorH * KG_POR_LB;
    }

    public static double kgHParaLbH(double kgPorH) {
        return kgPorH / KG_POR_LB;
    }

    public static double kgHParaGMin(double kgPorH) {
        return kgPorH * 1000.0 / 60.0;
    }

    public static double gMinParaKgH(double gPorMin) {
        return gPorMin * 60.0 / 1000.0;
    }

    // ---------- Volume e vazão de volume ----------

    public static double litrosParaPol3(double litros) {
        return litros * POL3_POR_LITRO;
    }

    public static double pol3ParaLitros(double pol3) {
        return pol3 / POL3_POR_LITRO;
    }

    public static double cm3ParaLitros(double cm3) {
        return cm3 / 1000.0;
    }

    public static double litrosParaCm3(double litros) {
        return litros * 1000.0;
    }

    /** cm³/min para L/h. */
    public static double cm3MinParaLh(double cm3PorMin) {
        return cm3PorMin * 60.0 / 1000.0;
    }

    /** L/h para cm³/min. */
    public static double lhParaCm3Min(double litrosPorH) {
        return litrosPorH * 1000.0 / 60.0;
    }

    /**
     * Vazão de bico em cm³/min para lb/h, usando a densidade do fluido em que a vazão foi medida.
     * Ex.: 1.000 cm³/min de fluido de teste a 0,70 kg/L = 92,6 lb/h.
     */
    public static double cm3MinParaLbH(double cm3PorMin, double densidadeKgL) {
        double kgPorH = cm3PorMin * densidadeKgL * 60.0 / 1000.0;
        return kgHParaLbH(kgPorH);
    }
}
