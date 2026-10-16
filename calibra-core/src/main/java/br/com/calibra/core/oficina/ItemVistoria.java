package br.com.calibra.core.oficina;

/**
 * Um item conferido na entrada do veículo (ex.: "Lataria", "Estepe") e o que se viu ("risco na porta direita",
 * "ok").
 */
public record ItemVistoria(String item, String observacao) {

    public ItemVistoria {
        item = item == null ? "" : item.strip();
        observacao = observacao == null ? "" : observacao.strip();
    }
}
