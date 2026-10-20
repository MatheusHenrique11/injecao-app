package br.com.calibra.relatorios;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import br.com.calibra.core.oficina.FichaChicote;
import br.com.calibra.core.oficina.PontoChicote;
import br.com.calibra.core.oficina.SituacaoItem;
import br.com.calibra.relatorios.DadosFichaChicote.ItemDeBancada;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * A ficha do chicote em PDF A4 (Fase 3B, SPEC 6.6.5), feita na hora a partir do que está gravado: o cabeçalho da
 * oficina, a OS, o cliente e o veículo, a situação, a injeção, o motor e a fonte do pinout, a tabela dos pontos (com
 * "a verificar"), os testes de bancada (o checklist da OS), os materiais, as observações e a assinatura do
 * responsável. A ficha comum cabe numa página; a tabela grande quebra entre as páginas.
 */
public final class RelatorioChicote {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SEPARADOR = " · ";
    private static final String SEM = "—";

    private RelatorioChicote() {
    }

    public static void gerar(DadosFichaChicote dados, Path destino) throws IOException {
        FichaChicote ficha = dados.ficha();
        try (PDDocument documento = new PDDocument()) {
            EscritorPdf pdf = new EscritorPdf(documento);
            pdf.cabecalhoOficina(TextoOficina.cabecalho(dados.oficina()), dados.logo());
            pdf.titulo("Ficha do chicote");
            pdf.subtitulo(dados.numeroOs() + SEPARADOR + "versão " + ficha.versaoChicote() + SEPARADOR
                    + ficha.rotuloDaSituacao());
            pdf.secao("Cliente e veículo");
            pdf.item("Cliente", dados.cliente());
            pdf.item("Veículo", dados.veiculo());
            pdf.secao("Chicote");
            pdf.item("Situação", ficha.rotuloDaSituacao());
            pdf.item("Injeção", ficha.injecao().isEmpty() ? "não informada" : ficha.injecao());
            if (!dados.motor().isEmpty()) {
                pdf.item("Motor", dados.motor());
            }
            pdf.item("Fonte do pinout", ficha.fontePinout());
            pontos(pdf, ficha.pontos());
            bancada(pdf, dados.bancada());
            if (!ficha.materiais().isEmpty() || !ficha.observacoes().isEmpty()) {
                pdf.secao("Materiais e observações");
                if (!ficha.materiais().isEmpty()) {
                    pdf.item("Materiais", ficha.materiais());
                }
                if (!ficha.observacoes().isEmpty()) {
                    pdf.item("Observações", ficha.observacoes());
                }
            }
            pdf.assinaturas(List.of("Responsável pela confecção (nome e assinatura)"));
            pdf.terminar("Calibra" + SEPARADOR + dados.numeroOs() + SEPARADOR + "ficha do chicote "
                    + ficha.versaoChicote() + SEPARADOR + "gerada em " + dados.geradoEm().format(DATA_HORA));
            pdf.salvar(destino);
        }
    }

    /** A função, o conector, o pino da injeção e a observação (com "a verificar" quando o ponto não foi conferido). */
    private static void pontos(EscritorPdf pdf, List<PontoChicote> pontos) throws IOException {
        pdf.secao("Pontos do chicote");
        if (pontos.isEmpty()) {
            pdf.paragrafo("Nenhum ponto anotado.");
            return;
        }
        List<List<String>> linhas = new ArrayList<>();
        for (PontoChicote ponto : pontos) {
            String observacao = ponto.aVerificar()
                    ? "(a verificar)" + (ponto.observacao().isEmpty() ? "" : " " + ponto.observacao())
                    : ponto.observacao();
            linhas.add(List.of(ponto.funcao(), traco(ponto.conector()), traco(ponto.pinoDaInjecao()),
                    traco(observacao)));
        }
        pdf.tabela(List.of("Função", "Conector", "Pino da injeção", "Observação"), new float[] {0.32f, 0.14f, 0.16f,
                0.38f}, new boolean[] {false, false, false, false}, linhas);
    }

    /** Os itens do checklist de bancada, como estão marcados na OS: a situação, a observação, quem e quando. */
    private static void bancada(EscritorPdf pdf, List<ItemDeBancada> itens) throws IOException {
        pdf.secao("Testes de bancada (checklist da OS)");
        if (itens.isEmpty()) {
            pdf.paragrafo("Esta OS não tem checklist de bancada.");
            return;
        }
        List<List<String>> linhas = new ArrayList<>();
        for (ItemDeBancada item : itens) {
            String observacao = item.situacao() == SituacaoItem.NAO_SE_APLICA && !item.justificativa().isEmpty()
                    ? "Por quê: " + item.justificativa() + (item.observacao().isEmpty() ? "" : ". " + item.observacao())
                    : item.observacao();
            String quem = item.marcadoEm() == null ? SEM : (item.marcadoPor().isEmpty() ? ""
                    : item.marcadoPor() + ", ") + item.marcadoEm().format(DATA_HORA);
            linhas.add(List.of(item.texto(), item.situacao().rotulo(), traco(observacao), quem));
        }
        pdf.tabela(List.of("Item", "Situação", "Observação", "Quem e quando"), new float[] {0.42f, 0.13f, 0.25f,
                0.20f}, new boolean[] {false, false, false, false}, linhas);
    }

    private static String traco(String texto) {
        return texto == null || texto.isEmpty() ? SEM : texto;
    }
}
