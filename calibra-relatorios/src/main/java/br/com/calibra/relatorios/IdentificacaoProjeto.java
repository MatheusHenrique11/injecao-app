package br.com.calibra.relatorios;

/**
 * De qual projeto é a memória de cálculo: aparece no cabeçalho do PDF. Só o necessário para a pessoa
 * reconhecer o serviço (regra 2.8): nome do cliente, veículo e motor.
 *
 * @param veiculo ex.: "VW Gol GTi · ABC1D23"
 * @param motor ex.: "v2 · AP 2.0 turbo"
 */
public record IdentificacaoProjeto(long projetoId, String cliente, String veiculo, String motor) {
}
