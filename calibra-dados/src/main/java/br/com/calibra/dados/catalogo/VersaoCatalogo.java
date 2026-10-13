package br.com.calibra.dados.catalogo;

import java.time.LocalDateTime;

/** Uma versão gravada de um item de catálogo, como ficou na hora (nunca muda). */
public record VersaoCatalogo<T>(long itemId, int versao, T dados, InfoCatalogo info, LocalDateTime criadaEm) {
}
