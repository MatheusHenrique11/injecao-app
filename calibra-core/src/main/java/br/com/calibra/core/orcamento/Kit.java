package br.com.calibra.core.orcamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Um kit do catálogo (SPEC 6.6.4, Fase 3B): linhas de itens e de serviços do catálogo, cada uma com a quantidade. O kit
 * com serviço é um pacote (ex.: "chicote + instalação"). No orçamento, ele se expande em linhas soltas e editáveis,
 * com a versão de agora de cada item e serviço; o kit não tem preço próprio.
 */
public record Kit(String nome, List<Linha> linhas, String fonte, LocalDate data) {

    /** De qual catálogo a linha vem. */
    public enum TipoLinha {
        ITEM("item"),
        SERVICO("serviço");

        private final String rotulo;

        TipoLinha(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    /**
     * Uma linha do kit.
     *
     * @param itemId o item ou o serviço no catálogo
     * @param nome o nome dele quando o kit foi gravado, só para mostrar (o orçamento usa o catálogo de agora)
     * @param quantidade maior que zero (ex.: 2 alto-falantes; 1,5 m de cabo)
     */
    public record Linha(TipoLinha tipo, long itemId, String nome, BigDecimal quantidade) {

        public Linha {
            Objects.requireNonNull(tipo, "tipo");
            nome = nome == null ? "" : nome.strip();
            quantidade = quantidade == null ? null : quantidade.stripTrailingZeros();
            if (quantidade != null && quantidade.scale() < 0) {
                quantidade = quantidade.setScale(0);
            }
        }
    }

    public Kit {
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(fonte, "fonte");
        linhas = linhas == null ? List.of() : List.copyOf(linhas);
    }

    /** O kit tem serviço: é um pacote. */
    public boolean pacote() {
        return linhas.stream().anyMatch(linha -> linha.tipo() == TipoLinha.SERVICO);
    }

    public List<String> validar() {
        List<String> erros = new ArrayList<>();
        if (linhas.isEmpty()) {
            erros.add("O kit precisa de pelo menos uma linha (um item ou um serviço do catálogo).");
        }
        Set<String> vistas = new HashSet<>();
        for (int i = 0; i < linhas.size(); i++) {
            Linha linha = linhas.get(i);
            String onde = "Linha " + (i + 1) + (linha.nome().isEmpty() ? "" : " (" + linha.nome() + ")") + ": ";
            if (linha.quantidade() == null || linha.quantidade().signum() <= 0) {
                erros.add(onde + "a quantidade deve ser maior que zero.");
            }
            if (!vistas.add(linha.tipo() + ":" + linha.itemId())) {
                erros.add(onde + "esse " + linha.tipo().rotulo() + " já está no kit; some as quantidades numa linha só.");
            }
        }
        return erros;
    }
}
