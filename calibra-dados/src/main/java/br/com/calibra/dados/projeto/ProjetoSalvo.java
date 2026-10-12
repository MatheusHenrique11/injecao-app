package br.com.calibra.dados.projeto;

import java.time.LocalDateTime;

import br.com.calibra.core.projeto.Meta;
import br.com.calibra.core.projeto.StatusProjeto;

/**
 * Um projeto gravado (SPEC 6.4). O cliente é o dono do veículo.
 *
 * @param statusAntesDeArquivar para onde o projeto volta ao ser reativado; null se não está arquivado
 */
public record ProjetoSalvo(
        long id,
        long veiculoId,
        StatusProjeto status,
        StatusProjeto statusAntesDeArquivar,
        Meta meta,
        String observacoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm) {
}
