package br.com.calibra.dados.backup;

import java.util.Objects;

/**
 * Um anexo de um backup do formato 2: onde ele fica na pasta de anexos e qual arquivo da pasta
 * {@code anexos-dos-backups} tem o conteúdo dele.
 *
 * @param caminho relativo à pasta de anexos, com "/" (ex.: "veiculos/7/0123456789abcdef.jpg")
 * @param sha256 a impressão digital do conteúdo, em hexadecimal: é o nome do arquivo em {@code anexos-dos-backups}
 * @param tamanho em bytes
 */
public record AnexoNoBackup(String caminho, String sha256, long tamanho) {

    public AnexoNoBackup {
        Objects.requireNonNull(caminho, "caminho");
        Objects.requireNonNull(sha256, "sha256");
    }
}
