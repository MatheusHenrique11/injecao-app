package br.com.calibra.dados.catalogo;

import java.time.LocalDateTime;

/**
 * Um item de catálogo na versão atual.
 *
 * @param chaveSemente identifica os itens que vieram com o app (ex.: "motor:vw-ap-2.0"); null nos do usuário
 */
public record ItemCatalogo<T>(long id, TipoCatalogo<T> tipo, int versao, T dados, InfoCatalogo info,
        boolean arquivado, String chaveSemente, LocalDateTime atualizadoEm) {
}
