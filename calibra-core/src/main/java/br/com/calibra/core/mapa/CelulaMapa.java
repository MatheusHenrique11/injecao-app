package br.com.calibra.core.mapa;

/**
 * As contas de uma célula de combustível do mapa base (SPEC 7.1 e 7.5).
 *
 * @param tiEfetivoMs o tempo de injeção sem o tempo morto
 * @param tiTabelaMs o que vai na tabela em ms: com ou sem o tempo morto, como diz o perfil
 * @param dutyCycle o ciclo do bico, sempre com o tempo morto (é o tempo que o bico fica aberto)
 */
public record CelulaMapa(double rotacao, double cargaKpa, double ve, double lambda, double tiEfetivoMs,
        double tiTabelaMs, double dutyCycle) {
}
