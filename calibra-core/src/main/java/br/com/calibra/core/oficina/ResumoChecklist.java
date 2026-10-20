package br.com.calibra.core.oficina;

import java.util.List;

/**
 * Quantos itens de um checklist da OS estão feitos, quantos não se aplicam e quantos faltam (SPEC 6.5, Fase 3B): a
 * aba Execução e o comprovante de entrega mostram assim.
 *
 * @param pendentes os que ainda não foram feitos nem marcados "não se aplica" (obrigatórios ou não)
 */
public record ResumoChecklist(int total, int feitos, int naoSeAplica, int pendentes) {

    public static ResumoChecklist de(List<ItemDoChecklist> itens) {
        int feitos = 0;
        int naoSeAplica = 0;
        for (ItemDoChecklist item : itens) {
            switch (item.marcacao().situacao()) {
                case FEITO -> feitos++;
                case NAO_SE_APLICA -> naoSeAplica++;
                case PENDENTE -> {
                }
            }
        }
        return new ResumoChecklist(itens.size(), feitos, naoSeAplica, itens.size() - feitos - naoSeAplica);
    }

    /** "9 de 9 itens feitos", "5 de 6 itens feitos, 1 não se aplica" ou "3 de 9 itens feitos, 6 pendentes". */
    public String texto() {
        StringBuilder texto = new StringBuilder().append(feitos).append(" de ").append(total)
                .append(total == 1 ? " item feito" : " itens feitos");
        if (naoSeAplica > 0) {
            texto.append(", ").append(naoSeAplica).append(naoSeAplica == 1 ? " não se aplica" : " não se aplicam");
        }
        if (pendentes > 0) {
            texto.append(", ").append(pendentes).append(pendentes == 1 ? " pendente" : " pendentes");
        }
        return texto.toString();
    }
}
