package br.com.calibra.app.casos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.core.util.NumeroBr;

/**
 * O orçamento como texto da tela (bloco 2.5) e de volta, sem JavaFX: as linhas de peças e serviços, o desconto, o
 * prazo, a validade, as condições e as observações. Também os padrões do orçamento, na tela de Configurações. Erros
 * de digitação viram mensagens em português; as regras ficam no núcleo.
 */
public final class FormularioOrcamento {

    /** Como o desconto é dado na tela. */
    public enum TipoDesconto {
        NENHUM("Sem desconto"),
        VALOR("Em reais (R$)"),
        PERCENTUAL("Em percentual (%)");

        private final String rotulo;

        TipoDesconto(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }

    /**
     * Uma linha da tela.
     *
     * @param catalogoItemId de onde a peça veio no catálogo; null se digitada
     */
    public record Linha(TipoItem tipo, String descricao, String quantidade, String unidade, String valor,
            Long catalogoItemId, Integer catalogoVersao) {

        public Linha {
            descricao = limpo(descricao);
            quantidade = limpo(quantidade);
            unidade = limpo(unidade);
            valor = limpo(valor);
        }

        /** Linha nova digitada, sem origem no catálogo. */
        public static Linha digitada(TipoItem tipo, String descricao, String quantidade, String unidade, String valor) {
            return new Linha(tipo, descricao, quantidade, unidade, valor, null, null);
        }

        static Linha de(ItemOrcamento item) {
            return new Linha(item.tipo(), item.descricao(),
                    item.quantidade() == null ? "" : NumeroBr.formatar(item.quantidade().doubleValue(), 0, 3),
                    item.unidade(), item.valorUnitario() == null ? "" : item.valorUnitario().formatarSemSimbolo(),
                    item.catalogoItemId(), item.catalogoVersao());
        }
    }

    /** Todos os campos do orçamento na tela. */
    public record Textos(List<Linha> linhas, TipoDesconto tipoDesconto, String desconto, String prazoDias,
            String prazoObservacao, String validadeDias, String condicoes, String observacoes) {

        public Textos {
            linhas = List.copyOf(linhas);
            tipoDesconto = tipoDesconto == null ? TipoDesconto.NENHUM : tipoDesconto;
            desconto = limpo(desconto);
            prazoDias = limpo(prazoDias);
            prazoObservacao = limpo(prazoObservacao);
            validadeDias = limpo(validadeDias);
            condicoes = limpo(condicoes);
            observacoes = limpo(observacoes);
        }
    }

    /** O orçamento lido (null se houve erro) e os erros encontrados. */
    public record Leitura(Orcamento orcamento, List<String> erros) {

        public Leitura {
            erros = List.copyOf(erros);
        }
    }

    /** Os padrões do orçamento na tela de Configurações. */
    public record TextosPadroes(TipoDesconto tipoDesconto, String desconto, String prazoDias, String prazoObservacao,
            String validadeDias, String condicoes, String valorHora) {

        public TextosPadroes {
            tipoDesconto = tipoDesconto == null ? TipoDesconto.NENHUM : tipoDesconto;
            desconto = limpo(desconto);
            prazoDias = limpo(prazoDias);
            prazoObservacao = limpo(prazoObservacao);
            validadeDias = limpo(validadeDias);
            condicoes = limpo(condicoes);
            valorHora = limpo(valorHora);
        }
    }

    /** Os padrões lidos (null se houve erro) e os erros encontrados. */
    public record LeituraPadroes(PadroesOrcamento padroes, List<String> erros) {

        public LeituraPadroes {
            erros = List.copyOf(erros);
        }
    }

    private FormularioOrcamento() {
    }

    // ===================== Orçamento =====================

    public static Textos escrever(Orcamento orcamento) {
        return new Textos(orcamento.itens().stream().map(Linha::de).toList(), tipo(orcamento.desconto()),
                textoDesconto(orcamento.desconto()), String.valueOf(orcamento.prazoDiasUteis()),
                orcamento.prazoObservacao(), String.valueOf(orcamento.validadeDias()), orcamento.condicoes(),
                orcamento.observacoes());
    }

    /**
     * Lê a tela. Primeiro os erros de digitação de todas as linhas e campos; com tudo digitado certo, as regras do
     * núcleo (ex.: desconto maior que o total).
     */
    public static Leitura ler(Textos textos) {
        List<String> erros = new ArrayList<>();
        List<ItemOrcamento> itens = new ArrayList<>();
        for (int i = 0; i < textos.linhas().size(); i++) {
            itens.add(item(textos.linhas().get(i), i + 1, erros));
        }
        Desconto desconto = desconto(textos.tipoDesconto(), textos.desconto(), erros);
        Integer prazo = inteiro(textos.prazoDias(), "o prazo em dias úteis (0 = a combinar)", erros);
        Integer validade = inteiro(textos.validadeDias(), "a validade em dias", erros);
        if (!erros.isEmpty()) {
            return new Leitura(null, erros);
        }
        Orcamento orcamento = new Orcamento(itens, desconto, prazo, textos.prazoObservacao(), validade,
                textos.condicoes(), textos.observacoes());
        List<String> regras = orcamento.validar();
        return new Leitura(regras.isEmpty() ? orcamento : null, regras);
    }

    /**
     * O total de uma linha, para a tela mostrar enquanto a pessoa digita; vazio se a quantidade ou o valor ainda não
     * fecham.
     */
    public static Optional<Dinheiro> totalDaLinha(Linha linha) {
        try {
            return Optional.of(Dinheiro.ler(linha.valor()).vezes(NumeroBr.lerDecimal(linha.quantidade())));
        } catch (NumberFormatException erro) {
            return Optional.empty();
        }
    }

    private static ItemOrcamento item(Linha linha, int numero, List<String> erros) {
        String onde = "Item " + numero + (linha.descricao().isEmpty() ? "" : " (" + linha.descricao() + ")") + ": ";
        if (linha.descricao().isEmpty()) {
            erros.add(onde + "escreva a descrição.");
        }
        BigDecimal quantidade = null;
        if (linha.quantidade().isEmpty()) {
            erros.add(onde + "informe a quantidade.");
        } else {
            try {
                quantidade = NumeroBr.lerDecimal(linha.quantidade());
            } catch (NumberFormatException erro) {
                erros.add(onde + "quantidade: " + erro.getMessage());
            }
        }
        Dinheiro valor = null;
        if (linha.valor().isEmpty()) {
            erros.add(onde + "informe o valor.");
        } else {
            try {
                valor = Dinheiro.ler(linha.valor());
            } catch (NumberFormatException erro) {
                erros.add(onde + "valor: " + erro.getMessage());
            }
        }
        return new ItemOrcamento(linha.tipo(), linha.descricao(), quantidade, linha.unidade(), valor,
                linha.catalogoItemId(), linha.catalogoVersao());
    }

    // ===================== Padrões =====================

    public static TextosPadroes escrever(PadroesOrcamento padroes) {
        return new TextosPadroes(tipo(padroes.desconto()), textoDesconto(padroes.desconto()),
                String.valueOf(padroes.prazoDiasUteis()), padroes.prazoObservacao(),
                String.valueOf(padroes.validadeDias()), padroes.condicoes(),
                padroes.valorHora() == null ? "" : padroes.valorHora().formatarSemSimbolo());
    }

    public static LeituraPadroes ler(TextosPadroes textos) {
        List<String> erros = new ArrayList<>();
        Desconto desconto = desconto(textos.tipoDesconto(), textos.desconto(), erros);
        Integer prazo = inteiro(textos.prazoDias(), "o prazo padrão em dias úteis", erros);
        Integer validade = inteiro(textos.validadeDias(), "a validade padrão em dias", erros);
        Dinheiro valorHora = null;
        if (!textos.valorHora().isEmpty()) {
            try {
                valorHora = Dinheiro.ler(textos.valorHora());
            } catch (NumberFormatException erro) {
                erros.add("Valor da hora: " + erro.getMessage());
            }
        }
        if (!erros.isEmpty()) {
            return new LeituraPadroes(null, erros);
        }
        PadroesOrcamento padroes = new PadroesOrcamento(desconto, prazo, textos.prazoObservacao(), validade,
                textos.condicoes(), valorHora);
        List<String> regras = padroes.validar();
        return new LeituraPadroes(regras.isEmpty() ? padroes : null, regras);
    }

    // ===================== Comum =====================

    static TipoDesconto tipo(Desconto desconto) {
        if (desconto.valor() != null) {
            return TipoDesconto.VALOR;
        }
        return desconto.percentual() != null ? TipoDesconto.PERCENTUAL : TipoDesconto.NENHUM;
    }

    static String textoDesconto(Desconto desconto) {
        if (desconto.valor() != null) {
            return desconto.valor().formatarSemSimbolo();
        }
        return desconto.percentual() == null ? "" : NumeroBr.formatar(desconto.percentual().doubleValue(), 0, 2);
    }

    private static Desconto desconto(TipoDesconto tipo, String texto, List<String> erros) {
        if (tipo == TipoDesconto.NENHUM) {
            return Desconto.NENHUM;
        }
        if (texto.isEmpty()) {
            erros.add(tipo == TipoDesconto.VALOR ? "Informe o desconto em reais." : "Informe o desconto em %.");
            return Desconto.NENHUM;
        }
        try {
            return tipo == TipoDesconto.VALOR ? Desconto.emValor(Dinheiro.ler(texto))
                    : Desconto.emPercentual(NumeroBr.lerDecimal(texto));
        } catch (NumberFormatException erro) {
            erros.add("Desconto: " + erro.getMessage());
            return Desconto.NENHUM;
        }
    }

    /** Número inteiro obrigatório; vazio ou com casas decimais vira mensagem. */
    private static Integer inteiro(String texto, String nome, List<String> erros) {
        if (texto.isEmpty()) {
            erros.add("Informe " + nome + ".");
            return null;
        }
        try {
            BigDecimal numero = NumeroBr.lerDecimal(texto);
            if (numero.stripTrailingZeros().scale() > 0) {
                erros.add(maiuscula(nome) + ": use um número inteiro.");
                return null;
            }
            return numero.intValueExact();
        } catch (NumberFormatException | ArithmeticException erro) {
            erros.add(maiuscula(nome) + ": " + (erro instanceof NumberFormatException ? erro.getMessage()
                    : "número grande demais."));
            return null;
        }
    }

    private static String maiuscula(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String limpo(String texto) {
        return texto == null ? "" : texto.strip();
    }
}
