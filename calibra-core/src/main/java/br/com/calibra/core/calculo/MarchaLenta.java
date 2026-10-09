package br.com.calibra.core.calculo;

/**
 * Condição de marcha lenta para conferir se o bico não é grande demais (SPEC 7.3). Sem padrão:
 * quem informa é o calibrador.
 *
 * @param mapKpa MAP absoluto na marcha lenta
 * @param ve eficiência volumétrica na marcha lenta (fração)
 */
public record MarchaLenta(double mapKpa, double ve) {
}
