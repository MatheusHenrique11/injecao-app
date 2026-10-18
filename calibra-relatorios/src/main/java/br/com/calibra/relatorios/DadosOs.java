package br.com.calibra.relatorios;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.oficina.SituacaoPagamento;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.Orcamento;

/**
 * Tudo que entra nos documentos da OS (bloco 3.3): a via do cliente, o comprovante de entrega e os textos de
 * WhatsApp.
 *
 * @param logo o arquivo do logo da oficina; null sem logo
 * @param numero ex.: "OS-2026-0001"
 * @param cliente o nome como está no cadastro (anonimizado, se o cliente foi anonimizado)
 * @param telefoneCliente vazio sem telefone
 * @param veiculo ex.: "VW Gol GTi 1994 · ABC1D23"
 * @param embarcacao o uso é em horas de motor, não em km
 * @param previsao a previsão de entrega; null sem previsão
 * @param mecanico o nome; vazio sem mecânico
 * @param entrada null = o veículo ainda não chegou
 * @param entrega null = não foi entregue
 * @param aprovado a última revisão aprovada do orçamento; null sem orçamento aprovado
 * @param recebimentos todos, na ordem em que foram recebidos; os cancelados ficam e não contam
 * @param textoVia o texto acima das assinaturas na via do cliente (Configurações › OS); vazio = só as assinaturas
 */
public record DadosOs(
        DadosOficina oficina,
        Path logo,
        String numero,
        StatusOs status,
        LocalDateTime abertaEm,
        String cliente,
        String telefoneCliente,
        String veiculo,
        boolean embarcacao,
        String servicoPedido,
        LocalDate previsao,
        String mecanico,
        EntradaVeiculo entrada,
        Entrega entrega,
        OrcamentoAprovado aprovado,
        List<Recebimento> recebimentos,
        String textoVia,
        String versaoApp,
        LocalDateTime geradoEm) {

    /**
     * A revisão aprovada do orçamento.
     *
     * @param numero ex.: "ORC-2026-0001 rev. 2"
     */
    public record OrcamentoAprovado(String numero, Orcamento conteudo) {

        public OrcamentoAprovado {
            Objects.requireNonNull(numero, "numero");
            Objects.requireNonNull(conteudo, "conteudo");
        }
    }

    /** Um recebimento; o cancelado fica na lista e não conta. */
    public record Recebimento(Pagamento pagamento, boolean cancelado) {

        public Recebimento {
            Objects.requireNonNull(pagamento, "pagamento");
        }
    }

    public DadosOs {
        oficina = oficina == null ? DadosOficina.vazia() : oficina;
        Objects.requireNonNull(numero, "numero");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(abertaEm, "abertaEm");
        Objects.requireNonNull(cliente, "cliente");
        telefoneCliente = telefoneCliente == null ? "" : telefoneCliente.strip();
        Objects.requireNonNull(veiculo, "veiculo");
        servicoPedido = servicoPedido == null ? "" : servicoPedido.strip();
        mecanico = mecanico == null ? "" : mecanico.strip();
        recebimentos = recebimentos == null ? List.of() : List.copyOf(recebimentos);
        textoVia = textoVia == null ? "" : textoVia.strip();
        Objects.requireNonNull(versaoApp, "versaoApp");
        Objects.requireNonNull(geradoEm, "geradoEm");
    }

    /** O valor da OS: o total da revisão aprovada; null sem orçamento aprovado. */
    public Dinheiro valor() {
        return aprovado == null ? null : aprovado.conteudo().total();
    }

    /** A soma dos recebimentos que valem. */
    public Dinheiro recebido() {
        return recebimentos.stream().filter(r -> !r.cancelado()).map(r -> r.pagamento().valor())
                .reduce(Dinheiro.ZERO, Dinheiro::mais);
    }

    /** O que falta receber; null sem orçamento aprovado. */
    public Dinheiro falta() {
        return valor() == null ? null : SituacaoPagamento.falta(valor(), recebido());
    }

    /** Pendente, parcial ou pago; null sem orçamento aprovado. */
    public SituacaoPagamento situacao() {
        return valor() == null ? null : SituacaoPagamento.de(valor(), recebido());
    }

    /** "km" ou "h" (horas de motor, na embarcação). */
    public String unidadeUso() {
        return embarcacao ? "h" : "km";
    }
}
