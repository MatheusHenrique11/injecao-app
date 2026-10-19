package br.com.calibra.dados.backup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A pasta {@code anexos-dos-backups} (backup do formato 2, fase 3B): fica ao lado dos zips, na mesma pasta de backup,
 * e guarda cada anexo uma vez só, com a impressão digital (SHA-256) como nome. Os zips levam o banco e a lista dos
 * anexos; uma foto que não mudou não é copiada de novo a cada backup.
 */
final class AnexosDosBackups {

    /** O nome da pasta, dentro da pasta de backup. */
    static final String PASTA = "anexos-dos-backups";

    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    /** O que ficou guardado: o SHA-256 (o nome do arquivo na pasta) e o tamanho. */
    record Guardado(String sha256, long tamanho) {
    }

    private final Path pasta;

    private AnexosDosBackups(Path pasta) {
        this.pasta = pasta;
    }

    /** A pasta dos anexos da pasta de backup. */
    static AnexosDosBackups na(Path pastaDeBackup) {
        return new AnexosDosBackups(pastaDeBackup.resolve(PASTA));
    }

    /** A pasta dos anexos de um backup: a que fica ao lado do zip. */
    static AnexosDosBackups doBackup(Path zip) {
        return na(zip.toAbsolutePath().getParent());
    }

    Path pasta() {
        return pasta;
    }

    /**
     * Guarda o conteúdo do arquivo, se ele ainda não está na pasta. Grava como {@code .parcial} e renomeia no fim:
     * nunca fica um arquivo pela metade com o nome certo.
     *
     * @return vazio se o arquivo sumiu no meio (um anexo removido durante o backup)
     */
    Optional<Guardado> guardar(Path arquivo) throws IOException {
        try {
            String sha256 = Arquivos.sha256(arquivo);
            long tamanho = Files.size(arquivo);
            Path guardado = arquivo(sha256);
            if (Files.isRegularFile(guardado) && Files.size(guardado) == tamanho) {
                return Optional.of(new Guardado(sha256, tamanho));
            }
            return Optional.of(copiar(arquivo));
        } catch (NoSuchFileException sumiu) {
            return Optional.empty();
        }
    }

    /**
     * Copia o arquivo e dá a ele o nome do SHA-256 do que foi copiado, calculado durante a cópia: se o arquivo mudou
     * depois da primeira leitura, o nome é o do conteúdo novo.
     */
    private Guardado copiar(Path arquivo) throws IOException {
        Files.createDirectories(pasta);
        Path parcial = Files.createTempFile(pasta, "copia-", Backup.PARCIAL);
        try {
            MessageDigest resumo = novoResumo();
            try (InputStream entrada = new DigestInputStream(Files.newInputStream(arquivo), resumo)) {
                Files.copy(entrada, parcial, StandardCopyOption.REPLACE_EXISTING);
            }
            String sha256 = HexFormat.of().formatHex(resumo.digest());
            long tamanho = Files.size(parcial);
            Path guardado = arquivo(sha256);
            if (!Files.isRegularFile(guardado) || Files.size(guardado) != tamanho) {
                Arquivos.mover(parcial, guardado);
            }
            return new Guardado(sha256, tamanho);
        } finally {
            Files.deleteIfExists(parcial);
        }
    }

    /**
     * O arquivo da pasta com o conteúdo deste SHA-256. Um nome que não é um SHA-256 (ex.: "../fora", num manifesto
     * mexido) é recusado.
     */
    Path arquivo(String sha256) {
        if (sha256 == null || !SHA256.matcher(sha256).matches()) {
            throw new BackupInvalidoException("O backup tem um anexo com impressão digital inválida: " + sha256);
        }
        return pasta.resolve(sha256);
    }

    /**
     * Apaga da pasta o que nenhum backup usa e as cópias pela metade. Só mexe em arquivos com nome de SHA-256 ou
     * terminados em {@code .parcial}; qualquer outro fica.
     *
     * @param emUso os SHA-256 que os backups da pasta usam
     * @return quantos arquivos saíram
     */
    int limpar(Set<String> emUso) throws IOException {
        if (!Files.isDirectory(pasta)) {
            return 0;
        }
        List<Path> arquivos;
        try (Stream<Path> itens = Files.list(pasta)) {
            arquivos = itens.filter(Files::isRegularFile).toList();
        }
        int apagados = 0;
        for (Path arquivo : arquivos) {
            String nome = arquivo.getFileName().toString();
            boolean sobra = nome.endsWith(Backup.PARCIAL) || (SHA256.matcher(nome).matches() && !emUso.contains(nome));
            if (sobra && Files.deleteIfExists(arquivo)) {
                apagados++;
            }
        }
        return apagados;
    }

    /** O tamanho da pasta, em bytes; zero se ela ainda não existe. */
    long tamanho() throws IOException {
        if (!Files.isDirectory(pasta)) {
            return 0;
        }
        long total = 0;
        try (Stream<Path> itens = Files.list(pasta)) {
            for (Path arquivo : itens.filter(Files::isRegularFile).toList()) {
                total += Files.size(arquivo);
            }
        }
        return total;
    }

    private static MessageDigest novoResumo() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("O Java sempre tem SHA-256", erro);
        }
    }
}
