package br.com.calibra.relatorios;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.oficina.CodigoFalha;
import br.com.calibra.core.oficina.LaudoEletrico;
import br.com.calibra.core.oficina.Medicao;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * O laudo de diagnóstico elétrico em PDF A4 (Fase 3B, SPEC 6.6.3), a folha que o cliente leva: a OS, o cliente, o
 * veículo e o mecânico, a queixa, as medições (com a referência que o dono escreveu, marcada "a verificar" quando for
 * o caso), os códigos de falha, o diagnóstico, o resultado e as assinaturas. O app não julga as medições: não há faixa
 * pronta (decisão 219).
 */
public final class RelatorioLaudo {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SEPARADOR = " · ";
    private static final String SEM = "—";

    private RelatorioLaudo() {
    }

    public static void gerar(DadosLaudo dados, Path destino) throws IOException {
        LaudoEletrico laudo = dados.laudo();
        try (PDDocument documento = new PDDocument()) {
            EscritorPdf pdf = new EscritorPdf(documento);
            pdf.cabecalhoOficina(TextoOficina.cabecalho(dados.oficina()), dados.logo());
            pdf.titulo("Laudo de diagnóstico elétrico");
            pdf.subtitulo(dados.numeroOs() + (laudo.dataHora() == null ? ""
                    : SEPARADOR + laudo.dataHora().format(DATA_HORA)));
            pdf.secao("Cliente e veículo");
            pdf.item("Cliente", dados.cliente());
            pdf.item("Veículo", dados.veiculo());
            if (!dados.mecanico().isEmpty()) {
                pdf.item("Mecânico", dados.mecanico());
            }
            pdf.secao("Queixa do cliente");
            pdf.paragrafo(laudo.queixa());
            medicoes(pdf, laudo.medicoes());
            codigos(pdf, laudo.codigos());
            diagnostico(pdf, laudo);
            pdf.assinaturas(List.of("Mecânico responsável" + (dados.mecanico().isEmpty() ? ""
                    : ": " + dados.mecanico()), "Cliente: " + dados.cliente()));
            pdf.terminar("GarageTech" + SEPARADOR + dados.numeroOs() + SEPARADOR + "laudo de diagnóstico elétrico"
                    + SEPARADOR + "gerado em " + dados.geradoEm().format(DATA_HORA));
            pdf.salvar(destino);
        }
    }

    /** O que se mediu, o valor com a unidade, a condição e a referência ("—" sem referência). */
    private static void medicoes(EscritorPdf pdf, List<Medicao> medicoes) throws IOException {
        if (medicoes.isEmpty()) {
            return;
        }
        pdf.secao("Medições");
        List<List<String>> linhas = new ArrayList<>();
        for (Medicao medicao : medicoes) {
            String referencia = medicao.referencia().isEmpty() ? SEM
                    : medicao.referencia() + (medicao.referenciaAVerificar() ? " (a verificar)" : "");
            linhas.add(List.of(medicao.nome(), medicao.valorComUnidade(),
                    medicao.condicao() == null ? SEM : medicao.condicao().rotulo(), referencia));
        }
        pdf.tabela(List.of("Medição", "Valor", "Condição", "Referência"), new float[] {0.34f, 0.16f, 0.18f, 0.32f},
                new boolean[] {false, false, false, false}, linhas);
    }

    private static void codigos(EscritorPdf pdf, List<CodigoFalha> codigos) throws IOException {
        if (codigos.isEmpty()) {
            return;
        }
        pdf.secao("Códigos de falha");
        List<List<String>> linhas = new ArrayList<>();
        for (CodigoFalha codigo : codigos) {
            linhas.add(List.of(codigo.codigo(), codigo.descricao().isEmpty() ? SEM : codigo.descricao(),
                    codigo.situacao() == null ? SEM : codigo.situacao().rotulo()));
        }
        pdf.tabela(List.of("Código", "Descrição", "Situação"), new float[] {0.18f, 0.62f, 0.2f},
                new boolean[] {false, false, false}, linhas);
    }

    /** Os testes, a causa, a solução e as recomendações que foram escritos, e o resultado ("a definir" sem ele). */
    private static void diagnostico(EscritorPdf pdf, LaudoEletrico laudo) throws IOException {
        pdf.secao("Diagnóstico");
        for (String[] parte : new String[][] {{"Testes realizados", laudo.testesRealizados()},
                {"Causa encontrada", laudo.causa()}, {"Solução", laudo.solucao()},
                {"Recomendações", laudo.recomendacoes()}}) {
            if (!parte[1].isEmpty()) {
                pdf.item(parte[0], parte[1]);
            }
        }
        pdf.item("Resultado", laudo.rotuloDoResultado());
    }
}
