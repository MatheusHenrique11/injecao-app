package br.com.calibra.core.combustivel;

import java.util.List;

import br.com.calibra.core.memoria.Entrada;

/**
 * Como o AFR estequiométrico de um combustível é obtido (SPEC 7.2): pela mistura gasolina + etanol,
 * pelo teor de etanol do hidratado ou por um valor fixo de catálogo.
 */
public sealed interface Composicao permits GasolinaComEtanol, EtanolHidratado, AfrFixo {

    /** AFR estequiométrico gasolina pura (SPEC 7.2). */
    double AFR_GASOLINA = 14.7;
    /** AFR estequiométrico etanol (SPEC 7.2). */
    double AFR_ETANOL = 9.0;
    /** Densidade da gasolina pura em kg/L (SPEC 7.2). */
    double DENSIDADE_GASOLINA = 0.74;
    /** Densidade do etanol anidro em kg/L (SPEC 7.2). */
    double DENSIDADE_ETANOL = 0.789;

    double afrEstequiometrico();

    /** A conta usada, em texto, para a memória de cálculo. */
    String formula();

    /** As entradas da conta, para a memória de cálculo. */
    List<Entrada> entradas();
}
