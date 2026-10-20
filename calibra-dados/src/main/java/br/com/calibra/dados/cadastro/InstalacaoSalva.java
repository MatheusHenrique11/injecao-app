package br.com.calibra.dados.cadastro;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.calibra.core.cadastro.Instalacao;
import br.com.calibra.core.oficina.NumeroOs;

/**
 * Uma instalação gravada (SPEC 6.2, V11): o veículo, a OS de origem com o número, o conteúdo e a retirada.
 *
 * @param osId null = sem OS (o item informado pelo cliente ou feito antes do app)
 * @param numeroOs null sem OS
 * @param retiradaEm a data em que o item saiu do veículo; null = continua nele
 */
public record InstalacaoSalva(long id, long veiculoId, Long osId, NumeroOs numeroOs, Instalacao instalacao,
        LocalDate retiradaEm, LocalDateTime criadoEm, LocalDateTime atualizadoEm) {

    public boolean retirada() {
        return retiradaEm != null;
    }
}
