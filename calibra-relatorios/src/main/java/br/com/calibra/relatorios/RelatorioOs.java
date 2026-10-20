package br.com.calibra.relatorios;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.GarantiaDoServico;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.util.NumeroBr;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Os documentos da OS em PDF A4 (bloco 3.3): a via do cliente, que ele assina na chegada (o serviço pedido, a entrada
 * com a vistoria e o texto padrão de Configurações › OS), e o comprovante de entrega (a entrega, a garantia, o
 * serviço aprovado e os recebimentos).
 */
public final class RelatorioOs {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SEPARADOR = " · ";
    private static final String NAO_ANOTADO = "não anotado";

    /** O que um documento escreve entre o cabeçalho e o rodapé. */
    @FunctionalInterface
    private interface Conteudo {
        void escrever(EscritorPdf pdf) throws IOException;
    }

    private RelatorioOs() {
    }

    /** A via do cliente: a OS, a entrada do veículo com a vistoria item a item, o texto padrão e as assinaturas. */
    public static void viaDoCliente(DadosOs dados, Path destino) throws IOException {
        gerar(destino, dados, "Ordem de serviço " + dados.numero(),
                "Via do cliente" + SEPARADOR + "aberta em " + dados.abertaEm().format(DATA_HORA), pdf -> {
                    clienteEVeiculo(pdf, dados);
                    servico(pdf, dados);
                    entrada(pdf, dados);
                    if (!dados.textoVia().isEmpty()) {
                        pdf.espaco(10f);
                        pdf.paragrafo(dados.textoVia());
                    }
                    pdf.assinaturas(List.of("Cliente: " + dados.cliente(), "Responsável pela oficina"));
                }, "via do cliente" + SEPARADOR + "gerada em ");
    }

    /**
     * O comprovante de entrega: a entrega e a garantia (com a dos serviços que têm a própria, Fase 3B), os itens e o
     * total da revisão aprovada, o resumo do checklist de cada serviço (Fase 3B), os recebimentos e a assinatura de
     * quem recebeu o veículo.
     *
     * @throws IllegalArgumentException se a OS ainda não foi entregue
     */
    public static void comprovanteDeEntrega(DadosOs dados, Path destino) throws IOException {
        Entrega entrega = dados.entrega();
        if (entrega == null) {
            throw new IllegalArgumentException("A OS " + dados.numero() + " ainda não foi entregue.");
        }
        gerar(destino, dados, "Comprovante de entrega", dados.numero() + SEPARADOR + "entregue em "
                + entrega.quando().format(DATA_HORA), pdf -> {
                    clienteEVeiculo(pdf, dados);
                    entrega(pdf, dados, entrega);
                    servicoFeito(pdf, dados);
                    pagamento(pdf, dados);
                    pdf.assinaturas(List.of("Quem recebeu o veículo (nome e assinatura)"));
                }, "comprovante de entrega" + SEPARADOR + "gerado em ");
    }

    private static void gerar(Path destino, DadosOs dados, String titulo, String subtitulo, Conteudo conteudo,
            String rodape) throws IOException {
        try (PDDocument documento = new PDDocument()) {
            EscritorPdf pdf = new EscritorPdf(documento);
            pdf.cabecalhoOficina(TextoOficina.cabecalho(dados.oficina()), dados.logo());
            pdf.titulo(titulo);
            pdf.subtitulo(subtitulo);
            conteudo.escrever(pdf);
            pdf.terminar("Calibra" + SEPARADOR + dados.numero() + SEPARADOR + rodape
                    + dados.geradoEm().format(DATA_HORA));
            pdf.salvar(destino);
        }
    }

    private static void clienteEVeiculo(EscritorPdf pdf, DadosOs dados) throws IOException {
        pdf.secao("Cliente e veículo");
        pdf.item("Cliente", dados.cliente() + (dados.telefoneCliente().isEmpty() ? ""
                : SEPARADOR + dados.telefoneCliente()));
        pdf.item("Veículo", dados.veiculo());
    }

    private static void servico(EscritorPdf pdf, DadosOs dados) throws IOException {
        pdf.secao("Serviço");
        pdf.item("Serviço pedido", dados.servicoPedido().isEmpty() ? NAO_ANOTADO : dados.servicoPedido());
        if (dados.previsao() != null) {
            pdf.item("Previsão de entrega", dados.previsao().format(TextoOrcamento.DIA));
        }
        if (!dados.mecanico().isEmpty()) {
            pdf.item("Mecânico", dados.mecanico());
        }
    }

    /** A chegada, o uso, o tanque, a vistoria item a item e os objetos deixados no veículo. */
    private static void entrada(EscritorPdf pdf, DadosOs dados) throws IOException {
        pdf.secao("Entrada do veículo");
        EntradaVeiculo entrada = dados.entrada();
        if (entrada == null) {
            pdf.paragrafo("O veículo ainda não chegou. A entrada (" + (dados.embarcacao() ? "horas de motor" : "km")
                    + ", tanque e vistoria) é anotada quando ele chegar.");
            return;
        }
        pdf.item("Chegada", entrada.quando().format(DATA_HORA));
        pdf.item(dados.embarcacao() ? "Horas de motor" : "Km", uso(entrada.uso(), dados));
        pdf.item("Tanque", entrada.combustivel() == null ? NAO_ANOTADO : entrada.combustivel().rotulo());
        List<ItemVistoria> itens = entrada.vistoria().itens();
        if (!itens.isEmpty()) {
            pdf.espaco(4f);
            List<List<String>> linhas = new ArrayList<>();
            for (ItemVistoria item : itens) {
                linhas.add(List.of(item.item(), item.observacao().isEmpty() ? "nada anotado" : item.observacao()));
            }
            pdf.tabela(List.of("Vistoria", "Observação"), new float[] {0.38f, 0.62f}, new boolean[] {false, false},
                    linhas);
        }
        String objetos = entrada.vistoria().objetos();
        pdf.item("Objetos deixados no veículo", objetos.isEmpty() ? "nenhum anotado" : objetos);
    }

    private static void entrega(EscritorPdf pdf, DadosOs dados, Entrega entrega) throws IOException {
        pdf.secao("Entrega");
        pdf.item("Entregue em", entrega.quando().format(DATA_HORA));
        pdf.item(dados.embarcacao() ? "Horas de motor na saída" : "Km na saída", uso(entrega.usoSaida(), dados));
        int dias = entrega.garantia().dias();
        pdf.item("Garantia", dias == 0 ? "sem garantia" : dias(dias) + ", até "
                + entrega.garantiaVence().format(TextoOrcamento.DIA));
        String proprias = garantiasProprias(dados, entrega);
        if (!proprias.isEmpty()) {
            pdf.item("Garantia própria", proprias);
        }
    }

    /**
     * Os serviços com garantia própria (SPEC 6.6.1), com o dia em que ela vence: "Reparo ou troca de alternador, 365
     * dias, até 14/10/2027; Diagnóstico de falha elétrica, sem garantia". Os outros serviços ficam com a garantia da OS,
     * da linha de cima. Vazio se nenhum tiver a própria: o comprovante fica como era, sem ocupar espaço (com uma tabela
     * de todos os serviços, a assinatura ia para a segunda página numa OS de três serviços).
     */
    private static String garantiasProprias(DadosOs dados, Entrega entrega) {
        LocalDate dia = entrega.quando().toLocalDate();
        return dados.garantias().stream().filter(GarantiaDoServico::propria).map(servico -> {
            Garantia garantia = servico.garantia(entrega.garantia());
            return servico.servico() + ", " + (garantia.dias() == 0 ? "sem garantia"
                    : dias(garantia.dias()) + ", até " + garantia.vence(dia).format(TextoOrcamento.DIA));
        }).collect(Collectors.joining("; "));
    }

    /** "1 dia" ou "90 dias". */
    private static String dias(int dias) {
        return dias + (dias == 1 ? " dia" : " dias");
    }

    /** Os itens e os totais da revisão aprovada do orçamento, numa tabela só (as peças antes dos serviços). */
    private static void servicoFeito(EscritorPdf pdf, DadosOs dados) throws IOException {
        pdf.secao("Serviço feito");
        if (dados.aprovado() == null) {
            pdf.paragrafo("Esta OS não tem orçamento aprovado.");
            return;
        }
        Orcamento o = dados.aprovado().conteudo();
        pdf.paragrafo("Conforme o orçamento " + dados.aprovado().numero() + ", aprovado pelo cliente.");
        pdf.espaco(4f);
        List<ItemOrcamento> itens = new ArrayList<>(o.pecas());
        itens.addAll(o.servicos());
        RelatorioOrcamento.tabelaDeItens(pdf, itens);
        RelatorioOrcamento.totais(pdf, o);
        String checklists = checklists(dados);
        if (!checklists.isEmpty()) {
            pdf.espaco(4f);
            pdf.item("Checklist", checklists);
        }
    }

    /**
     * O resumo do checklist de cada serviço (Fase 3B, SPEC 6.5): "Instalação de som completa: 9 de 9 itens feitos;
     * Troca de alto-falantes: 5 de 6 itens feitos, 1 não se aplica". O que ficou pendente aparece como pendente. Uma
     * linha só, como a da garantia própria, para o comprovante continuar numa página na OS comum. Vazio sem checklist.
     */
    private static String checklists(DadosOs dados) {
        return dados.checklists().stream().map(checklist -> checklist.servico() + ": " + checklist.resumo().texto())
                .collect(Collectors.joining("; "));
    }

    /**
     * Os recebimentos (o cancelado aparece marcado e não conta), o valor, o recebido, o que falta (ou o que foi
     * recebido a mais) e a situação.
     */
    private static void pagamento(EscritorPdf pdf, DadosOs dados) throws IOException {
        pdf.secao("Pagamento");
        if (dados.recebimentos().isEmpty()) {
            pdf.paragrafo("Nenhum recebimento registrado.");
        } else {
            List<List<String>> linhas = new ArrayList<>();
            for (DadosOs.Recebimento r : dados.recebimentos()) {
                Pagamento p = r.pagamento();
                String observacao = r.cancelado() ? "CANCELADO, não conta" + (p.observacao().isEmpty() ? ""
                        : SEPARADOR + p.observacao()) : p.observacao();
                linhas.add(List.of(p.recebidoEm().format(DATA_HORA), p.forma().rotulo(), p.valor().formatar(),
                        observacao));
            }
            pdf.tabela(List.of("Recebido em", "Forma", "Valor", "Observação"), new float[] {0.2f, 0.2f, 0.17f, 0.43f},
                    new boolean[] {false, false, true, false}, linhas);
        }
        if (dados.valor() != null) {
            pdf.espaco(4f);
            pdf.total("Valor da OS", dados.valor().formatar(), false);
            pdf.total("Recebido", dados.recebido().formatar(), false);
            pdf.total("Falta", dados.falta().formatar(), false);
            if (dados.aMais().centavos() > 0) {
                pdf.total("Recebido a mais", dados.aMais().formatar(), false);
            }
            pdf.total("Situação", dados.situacao().rotulo(), true);
        }
    }

    /** "120.500 km" ou "1.250 h"; "não anotado" sem o valor. */
    private static String uso(Double uso, DadosOs dados) {
        return uso == null ? NAO_ANOTADO : NumeroBr.formatar(uso, 0) + " " + dados.unidadeUso();
    }
}
