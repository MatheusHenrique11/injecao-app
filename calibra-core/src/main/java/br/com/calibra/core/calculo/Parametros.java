package br.com.calibra.core.calculo;

import br.com.calibra.core.semaforo.LimiaresSemaforo;
import br.com.calibra.core.unidades.Conversoes;

/**
 * Parâmetros do cálculo, com os padrões da SPEC 7.1. Todos editáveis.
 *
 * @param caminhoAr k_ar (padrão) ou BSFC
 * @param kAr hp por lb/min de ar; padrão 10,5, aceito de 8,0 a 11 (a spec dizia 9,5 a 11; ampliado em
 *     09/10/2026 porque motores antigos de 2 válvulas ficam perto de 8,5–9,5, decisão 40)
 * @param bsfc lb/hp·h; só no caminho BSFC (NaN no caminho k_ar)
 * @param dcMax DC máximo do bico; padrão 0,80
 * @param etaC eficiência do compressor; padrão 0,72
 * @param eps eficiência do intercooler; padrão 0,80 (ar-ar)
 * @param perdaDescargaKpa perdas entre compressor e coletor; padrão 2 psi
 * @param perdaAdmissaoKpa perda antes do compressor; padrão 1 psi
 * @param acrescimoTmAspiradoC aspirado: Tm = T_amb + este valor; padrão 10 °C
 * @param densidadeFluidoTesteKgL fluido de teste dos bicos quando nenhum bico é informado; padrão 0,70
 * @param tiMinimoMarchaLentaMs TI efetivo mínimo na marcha lenta; padrão 1,0 ms (SPEC 7.3)
 * @param gruposSimultaneo grupos de saídas no modo simultâneo; 1 até existir o perfil da injeção
 * @param limiares limiares do semáforo (SPEC 7.4)
 */
public record Parametros(
        CaminhoAr caminhoAr,
        double kAr,
        double bsfc,
        double dcMax,
        double etaC,
        double eps,
        double perdaDescargaKpa,
        double perdaAdmissaoKpa,
        double acrescimoTmAspiradoC,
        double densidadeFluidoTesteKgL,
        double tiMinimoMarchaLentaMs,
        int gruposSimultaneo,
        LimiaresSemaforo limiares) {

    /** Menor k_ar aceito (decisão 40). */
    public static final double K_AR_MINIMO = 8.0;
    /** Maior k_ar aceito (SPEC 7.1). */
    public static final double K_AR_MAXIMO = 11.0;

    /** Padrões da SPEC 7.1. */
    public static Parametros padrao() {
        return new Parametros(CaminhoAr.K_AR, 10.5, Double.NaN, 0.80, 0.72, 0.80,
                Conversoes.psiParaKpa(2), Conversoes.psiParaKpa(1), 10.0, 0.70, 1.0, 1,
                LimiaresSemaforo.padrao());
    }

    /** Os mesmos parâmetros, com a vazão de ar pelo BSFC informado. */
    public Parametros comBsfc(double bsfcLbHpH) {
        return new Parametros(CaminhoAr.BSFC, kAr, bsfcLbHpH, dcMax, etaC, eps, perdaDescargaKpa, perdaAdmissaoKpa,
                acrescimoTmAspiradoC, densidadeFluidoTesteKgL, tiMinimoMarchaLentaMs, gruposSimultaneo, limiares);
    }

    /** Os mesmos parâmetros, com outro k_ar. */
    public Parametros comKAr(double novoKAr) {
        return new Parametros(CaminhoAr.K_AR, novoKAr, Double.NaN, dcMax, etaC, eps, perdaDescargaKpa,
                perdaAdmissaoKpa, acrescimoTmAspiradoC, densidadeFluidoTesteKgL, tiMinimoMarchaLentaMs,
                gruposSimultaneo, limiares);
    }
}
