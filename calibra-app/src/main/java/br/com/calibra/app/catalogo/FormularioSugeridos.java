package br.com.calibra.app.catalogo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.orcamento.ServicosSugeridos.Sugestao;
import br.com.calibra.core.util.NumeroBr;
import br.com.calibra.core.util.TextoBusca;
import br.com.calibra.dados.catalogo.InfoCatalogo;

/**
 * A leitura da janela "Serviços sugeridos" (SPEC 6.6.1), sem JavaFX: cada linha marcada vira um serviço do catálogo
 * com as horas ou o preço que o dono digitou. A lista vem sem os dois, e o app não inventa tempo de serviço
 * (decisão 216): a linha marcada sem horas e sem preço é recusada.
 */
public final class FormularioSugeridos {

    /** Uma linha da janela, como está na tela (horas e preço como texto). */
    public record Linha(Sugestao sugestao, boolean marcada, String horas, String preco) {

        public Linha {
            Objects.requireNonNull(sugestao, "sugestao");
            horas = horas == null ? "" : horas.strip();
            preco = preco == null ? "" : preco.strip();
        }
    }

    /** Um serviço pronto para gravar no catálogo. */
    public record Novo(ServicoPadrao dados, InfoCatalogo info) {

        public Novo {
            Objects.requireNonNull(dados, "dados");
            Objects.requireNonNull(info, "info");
        }
    }

    /** Os serviços das linhas marcadas (vazio se houve erro) e todos os erros de uma vez. */
    public record Leitura(List<Novo> servicos, List<String> erros) {

        public Leitura {
            servicos = List.copyOf(servicos);
            erros = List.copyOf(erros);
        }

        public boolean ok() {
            return erros.isEmpty();
        }
    }

    private FormularioSugeridos() {
    }

    /**
     * Lê as linhas marcadas; as desmarcadas ficam de fora. Cada serviço entra com a fonte "Tabela da oficina", sem
     * descrição, com a garantia da OS e com a categoria e o "exige o veículo" da lista.
     */
    public static Leitura ler(List<Linha> linhas) {
        List<Novo> servicos = new ArrayList<>();
        List<String> erros = new ArrayList<>();
        for (Linha linha : linhas) {
            if (linha.marcada()) {
                ler(linha, servicos, erros);
            }
        }
        if (servicos.isEmpty() && erros.isEmpty()) {
            erros.add("Marque ao menos um serviço e preencha as horas ou o preço dele.");
        }
        return erros.isEmpty() ? new Leitura(servicos, erros) : new Leitura(List.of(), erros);
    }

    /** O nome para comparar com o catálogo: sem diferença de acento, maiúscula ou pontuação. */
    public static String chave(String nome) {
        return TextoBusca.normalizar(nome);
    }

    private static void ler(Linha linha, List<Novo> servicos, List<String> erros) {
        Sugestao sugestao = linha.sugestao();
        String nome = sugestao.nome();
        if (linha.horas().isEmpty() && linha.preco().isEmpty()) {
            erros.add(nome + ": informe as horas ou o preço.");
            return;
        }
        int antes = erros.size();
        Double horas = null;
        if (!linha.horas().isEmpty()) {
            try {
                horas = NumeroBr.ler(linha.horas());
                if (!(horas > 0)) {
                    erros.add(nome + ": as horas devem ser maiores que zero.");
                }
            } catch (NumberFormatException erro) {
                erros.add(nome + ", horas: " + erro.getMessage());
            }
        }
        Dinheiro preco = null;
        if (!linha.preco().isEmpty()) {
            try {
                preco = Dinheiro.ler(linha.preco());
                if (preco.negativo()) {
                    erros.add(nome + ": o preço não pode ser negativo.");
                }
            } catch (NumberFormatException erro) {
                erros.add(nome + ", preço: " + erro.getMessage());
            }
        }
        if (erros.size() > antes) {
            return;
        }
        String fonte = FormaServico.FONTE_PADRAO;
        servicos.add(new Novo(new ServicoPadrao(nome, horas, fonte, null, sugestao.categoria(), "",
                sugestao.exigeEntradaDoVeiculo(), null, null), new InfoCatalogo(nome, fonte, null, false, preco, "")));
    }
}
