package br.com.calibra.dados.oficina;

import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.calibra.core.oficina.TipoAnexo;

/**
 * Um anexo gravado: o arquivo fica na pasta de anexos, no caminho {@code arquivo} (relativo a ela).
 *
 * @param osId null = anexo do veículo, sem OS
 * @param validade null = sem validade
 * @param pessoal documento com dados pessoais (sai na anonimização do cliente)
 */
public record AnexoSalvo(long id, long veiculoId, Long osId, TipoAnexo tipo, String descricao, String arquivo,
        String nomeOriginal, long tamanhoBytes, String sha256, LocalDate validade, boolean pessoal,
        LocalDateTime criadoEm) {
}
