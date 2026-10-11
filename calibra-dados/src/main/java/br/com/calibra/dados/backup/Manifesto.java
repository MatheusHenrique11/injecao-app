package br.com.calibra.dados.backup;

import java.time.LocalDateTime;

/**
 * O que está dentro de um backup (arquivo manifesto.json no zip).
 *
 * @param formato formato do backup; sobe se o jeito de montar o zip mudar
 * @param versaoBanco última migração aplicada no banco copiado
 * @param sha256Banco impressão digital do banco copiado, para descobrir arquivo corrompido
 * @param anexos quantos arquivos de anexo foram junto
 */
public record Manifesto(
        int formato,
        String versaoApp,
        String versaoBanco,
        LocalDateTime criadoEm,
        MotivoBackup motivo,
        String sha256Banco,
        long tamanhoBanco,
        int anexos) {

    public static final int FORMATO = 1;
}
