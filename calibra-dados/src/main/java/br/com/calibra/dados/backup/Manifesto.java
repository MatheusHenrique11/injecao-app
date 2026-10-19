package br.com.calibra.dados.backup;

import java.time.LocalDateTime;
import java.util.List;

/**
 * O que está dentro de um backup (arquivo manifesto.json no zip).
 *
 * @param formato formato do backup; sobe se o jeito de montar o zip mudar
 * @param versaoBanco última migração aplicada no banco copiado
 * @param sha256Banco impressão digital do banco copiado, para descobrir arquivo corrompido
 * @param anexos quantos arquivos de anexo foram junto
 * @param arquivos os anexos do formato 2, guardados em {@code anexos-dos-backups}; vazio no formato 1, que leva os
 *        anexos dentro do zip
 */
public record Manifesto(
        int formato,
        String versaoApp,
        String versaoBanco,
        LocalDateTime criadoEm,
        MotivoBackup motivo,
        String sha256Banco,
        long tamanhoBanco,
        int anexos,
        List<AnexoNoBackup> arquivos) {

    /** Formato 2 (fase 3B): o zip leva o banco e a lista dos anexos; cada anexo fica uma vez na pasta de backup. */
    public static final int FORMATO = 2;
    /** Formato 1 (até a 0.3.0): os anexos dentro do zip. Continua restaurando. */
    public static final int FORMATO_ANEXOS_NO_ZIP = 1;

    public Manifesto {
        arquivos = arquivos == null ? List.of() : List.copyOf(arquivos);
    }
}
