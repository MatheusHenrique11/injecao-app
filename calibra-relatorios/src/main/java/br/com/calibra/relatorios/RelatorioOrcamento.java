package br.com.calibra.relatorios;

import static br.com.calibra.core.util.NumeroBr.formatar;
import static br.com.calibra.core.util.NumeroBr.percentual;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.calculo.Dimensionamento;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.StatusOrcamento;
import br.com.calibra.core.semaforo.Avaliacao;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.core.unidades.Conversoes;
import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * PDF do orçamento (bloco 2.5): cabeçalho da oficina com o logo; cliente, veículo, motor e meta; peças, serviços e
 * totais; prazo, validade, condições e observações; o resumo técnico (peças e semáforo, com as justificativas) e a
 * nota do mapa base conservador.
 */
public final class RelatorioOrcamento {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SEPARADOR = " · ";
    private static final List<String> COLUNAS = List.of("Descrição", "Qtd.", "Un.", "Valor unitário", "Total");
    private static final float[] LARGURAS = {0.50f, 0.08f, 0.08f, 0.17f, 0.17f};
    private static final boolean[] A_DIREITA = {false, true, false, true, true};

    private RelatorioOrcamento() {
    }

    public static void gerar(DadosOrcamento dados, Path destino) throws IOException {
        try (PDDocument documento = new PDDocument()) {
            EscritorPdf pdf = new EscritorPdf(documento);
            cabecalho(pdf, dados);
            clienteEMeta(pdf, dados);
            itensETotais(pdf, dados.conteudo());
            prazoECondicoes(pdf, dados);
            resumoTecnico(pdf, dados);
            pdf.espaco(8f);
            pdf.caixa(TextoOrcamento.NOTA_MAPA_BASE + " O Calibra não grava nada na injeção.");
            pdf.terminar("Calibra · Orçamento " + dados.numero().comRevisao() + " · gerado em "
                    + dados.geradoEm().format(DATA_HORA));
            // Salva num temporário e troca depois: um PDF aberto no leitor nunca fica pela metade.
            Path temporario = destino.resolveSibling(destino.getFileName() + ".tmp");
            documento.save(temporario.toFile());
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void cabecalho(EscritorPdf pdf, DadosOrcamento dados) throws IOException {
        DadosOficina o = dados.oficina();
        List<String> linhas = new ArrayList<>();
        linhas.add(o.nome().isEmpty() ? "Oficina (preencha os dados em Configurações)" : o.nome());
        if (!o.cnpj().isEmpty()) {
            linhas.add("CNPJ " + o.cnpj());
        }
        if (!o.endereco().isEmpty()) {
            linhas.add(o.endereco());
        }
        List<String> contato = new ArrayList<>();
        if (!o.telefone().isEmpty()) {
            contato.add(o.telefone());
        }
        if (!o.email().isEmpty()) {
            contato.add(o.email());
        }
        if (!contato.isEmpty()) {
            linhas.add(String.join(SEPARADOR, contato));
        }
        pdf.cabecalhoOficina(linhas, dados.logo());
        pdf.titulo("Orçamento " + dados.numero().comRevisao());
        String situacao = dados.status() == StatusOrcamento.RASCUNHO ? SEPARADOR + "RASCUNHO (ainda não enviado)"
                : dados.status() == StatusOrcamento.ENVIADO ? "" : SEPARADOR + dados.status().rotulo();
        pdf.subtitulo("Emitido em " + dados.emissao().format(TextoOrcamento.DIA) + SEPARADOR + "válido "
                + TextoOrcamento.validade(dados.conteudo(), dados.emissao()) + situacao);
    }

    private static void clienteEMeta(EscritorPdf pdf, DadosOrcamento dados) throws IOException {
        pdf.secao("Cliente e veículo");
        pdf.item("Cliente", dados.cliente());
        pdf.item("Veículo", dados.veiculo());
        pdf.item("Motor", dados.motor());
        pdf.item("Meta", RelatorioMemoria.descreverMeta(dados.entrada().meta()));
    }

    private static void itensETotais(EscritorPdf pdf, Orcamento o) throws IOException {
        tabela(pdf, "Peças", o.pecas());
        tabela(pdf, "Serviços", o.servicos());
        pdf.espaco(4f);
        if (!o.pecas().isEmpty()) {
            pdf.total("Peças", o.totalPecas().formatar(), false);
        }
        if (!o.servicos().isEmpty()) {
            pdf.total("Serviços", o.totalServicos().formatar(), false);
        }
        if (!o.desconto().nenhum()) {
            pdf.total(TextoOrcamento.rotuloDesconto(o.desconto()), "− " + o.valorDesconto().formatar(), false);
        }
        pdf.total("Total", o.total().formatar(), true);
    }

    private static void tabela(EscritorPdf pdf, String titulo, List<ItemOrcamento> itens) throws IOException {
        if (itens.isEmpty()) {
            return;
        }
        pdf.secao(titulo);
        pdf.tabela(COLUNAS, LARGURAS, A_DIREITA, TextoOrcamento.linhas(itens));
    }

    private static void prazoECondicoes(EscritorPdf pdf, DadosOrcamento dados) throws IOException {
        Orcamento o = dados.conteudo();
        pdf.secao("Prazo e condições");
        pdf.item("Prazo", TextoOrcamento.prazo(o));
        pdf.item("Validade", TextoOrcamento.validade(o, dados.emissao()));
        if (!o.condicoes().isEmpty()) {
            pdf.item("Condições", o.condicoes());
        }
        if (!o.observacoes().isEmpty()) {
            pdf.item("Observações", o.observacoes());
        }
    }

    /** O que o cálculo diz das peças, com o semáforo e as justificativas dos vermelhos. */
    private static void resumoTecnico(EscritorPdf pdf, DadosOrcamento dados) throws IOException {
        Dimensionamento d = dados.resultado().valor();
        EntradaDimensionamento e = dados.entrada();
        pdf.secao("Resumo técnico (estimativa do cálculo, núcleo " + dados.resultado().versaoNucleo() + ")");
        pdf.item("Potência", Apresentacao.potencia(d.potencia().virabrequimKw()));
        Dimensionamento.LadoDoAr ar = d.ladoDoAr();
        if (ar.sobrealimentado()) {
            pdf.item("Pressão do turbo", Apresentacao.pressaoManometrica(Math.max(0, ar.mapKpa() - ar.patmKpa()))
                    + SEPARADOR + "PR " + formatar(ar.razaoPressao(), 2));
        }
        Dimensionamento.BicoNecessario bn = d.bicoNecessario();
        String bico = "necessário " + Apresentacao.vazaoBico(bn.vazaoNominalCm3Min(), bn.densidadeTesteKgL()) + " a "
                + formatar(Conversoes.kpaParaBar(bn.pressaoTesteKpa()), 2) + " bar (" + bn.numeroBicos() + " bicos)";
        if (e.bico() != null && d.bicoAvaliado() != null) {
            bico += SEPARADOR + e.bico().nome() + ": DC " + percentual(d.bicoAvaliado().dutyCycle(), 1)
                    + " na potência máxima";
        }
        pdf.item("Bico", bico);
        pdf.item("Bomba", "necessária " + formatar(d.bomba().vazaoLh(), 0) + " L/h a "
                + Apresentacao.pressaoManometrica(d.bomba().pressaoKpa()));
        if (d.noMapa() != null) {
            pdf.item("Turbo", d.noMapa().turbo() + ": " + Apresentacao.pontoNoMapa(d.noMapa().potencia()));
        }
        if (e.injecao() != null) {
            pdf.item("Injeção", e.injecao().nome());
        }
        pdf.espaco(4f);
        List<Avaliacao> ordenadas = d.semaforo().stream().sorted(Comparator.comparing(Avaliacao::cor).reversed())
                .toList();
        for (Avaliacao a : ordenadas) {
            pdf.semaforo(a.cor(), a.indicador().rotulo(), a.mensagem());
            for (Justificativa j : dados.justificativas()) {
                if (j.indicador() == a.indicador()) {
                    pdf.italico("Justificativa (" + j.dataHora().format(DATA_HORA) + "): " + j.texto(), 66f);
                    pdf.espaco(3f);
                }
            }
        }
    }
}
