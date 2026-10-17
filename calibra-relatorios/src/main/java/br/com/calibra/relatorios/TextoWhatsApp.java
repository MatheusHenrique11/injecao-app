package br.com.calibra.relatorios;

import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Orcamento;

/**
 * O orçamento em texto para colar no WhatsApp (bloco 2.5), com o *negrito* do WhatsApp. O app só copia para a área
 * de transferência: quem envia é a pessoa (regra 2.1, zero rede). O da OS sem projeto (bloco 3.3) sai sem o motor, a
 * meta e a nota do mapa base.
 */
public final class TextoWhatsApp {

    private TextoWhatsApp() {
    }

    public static String de(DadosOrcamento dados) {
        Orcamento o = dados.conteudo();
        DadosOficina oficina = dados.oficina();
        List<String> linhas = new ArrayList<>();
        linhas.add("*Orçamento " + dados.numero().comRevisao() + "*"
                + (oficina.nome().isEmpty() ? "" : " — " + oficina.nome()));
        linhas.add("Cliente: " + dados.cliente());
        linhas.add("Veículo: " + dados.veiculo());
        if (dados.dePreparacao()) {
            linhas.add("Motor: " + dados.preparacao().motor());
            linhas.add("Meta: " + RelatorioMemoria.descreverMeta(dados.preparacao().entrada().meta()));
        }
        itens(linhas, "*Peças*", o.pecas());
        itens(linhas, "*Serviços*", o.servicos());
        linhas.add("");
        if (!o.pecas().isEmpty()) {
            linhas.add("Peças: " + o.totalPecas().formatar());
        }
        if (!o.servicos().isEmpty()) {
            linhas.add("Serviços: " + o.totalServicos().formatar());
        }
        if (!o.desconto().nenhum()) {
            linhas.add(TextoOrcamento.rotuloDesconto(o.desconto()) + ": - " + o.valorDesconto().formatar());
        }
        linhas.add("*Total: " + o.total().formatar() + "*");
        linhas.add("");
        linhas.add("Prazo: " + TextoOrcamento.prazo(o));
        linhas.add("Validade: " + TextoOrcamento.validade(o, dados.emissao()));
        if (!o.condicoes().isEmpty()) {
            linhas.add("Condições: " + o.condicoes());
        }
        if (!o.observacoes().isEmpty()) {
            linhas.add("Obs.: " + o.observacoes());
        }
        List<String> rodape = new ArrayList<>();
        if (dados.dePreparacao()) {
            rodape.add("_" + TextoOrcamento.NOTA_MAPA_BASE + "_");
        }
        if (!oficina.telefone().isEmpty()) {
            rodape.add(oficina.nome().isEmpty() ? oficina.telefone() : oficina.nome() + " · " + oficina.telefone());
        }
        if (!rodape.isEmpty()) {
            linhas.add("");
            linhas.addAll(rodape);
        }
        return String.join("\n", linhas);
    }

    private static void itens(List<String> linhas, String titulo, List<ItemOrcamento> itens) {
        if (itens.isEmpty()) {
            return;
        }
        linhas.add("");
        linhas.add(titulo);
        for (ItemOrcamento item : itens) {
            linhas.add("• " + item.descricao() + " — " + TextoOrcamento.quantidadeComUnidade(item) + " × "
                    + item.valorUnitario().formatar() + " = " + item.total().formatar());
        }
    }
}
