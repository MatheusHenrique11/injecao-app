package br.com.calibra.dados.backup;

import java.nio.file.Path;

/** Um backup na pasta: o arquivo zip, o manifesto dele e o tamanho do zip. */
public record InfoBackup(Path arquivo, Manifesto manifesto, long tamanhoBytes) {
}
