package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosLaudo.Gravado;
import br.com.calibra.app.casos.CasosLaudo.Rascunho;
import br.com.calibra.app.casos.CasosOficina.NovaOs;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.VeiculoExistente;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.oficina.CodigoFalha;
import br.com.calibra.core.oficina.CondicaoMedicao;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.LaudoEletrico;
import br.com.calibra.core.oficina.Medicao;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.ResultadoLaudo;
import br.com.calibra.core.oficina.SituacaoCodigo;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.oficina.LaudoSalvo;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;
import br.com.calibra.relatorios.DadosLaudo;

/**
 * Os laudos de diagnóstico elétrico da OS (Fase 3B, bloco 3B.4; SPEC 6.6.3): o laudo novo já preenchido, gravar,
 * mudar e apagar, as regras da data e do mecânico, o aviso de LGPD, e os testes obrigatórios nº 6 e nº 7 da fase, na
 * parte do laudo. Dados fictícios.
 */
class CasosLaudoTest {

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 20, 9, 0));
    private CasosOrcamento orcamentos;
    private CasosOficina oficina;
    private CasosLaudo laudos;
    private long carlos;
    private long os;

    @BeforeEach
    void abrirBanco() throws IOException {
        PastasDados pastas = new PastasDados(raiz.resolve("dados"));
        pastas.criar();
        BancoDados banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.3.0-teste", relogio));
        CasosProjeto casos = new CasosProjeto(banco, relogio, "0.3.0-teste");
        orcamentos = new CasosOrcamento(banco, relogio, casos);
        oficina = new CasosOficina(banco, relogio, casos, pastas.anexos());
        laudos = new CasosLaudo(banco, relogio);
        carlos = oficina.novoMecanico("Carlos");
        os = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Ana Costa")),
                new VeiculoNovo(CasosProjetoTest.carro("ABC1D23")), "Bateria descarrega de um dia para o outro",
                carlos, null, "", entrada())).osId();
        relogio.andar(Duration.ofMinutes(30));
    }

    private EntradaVeiculo entrada() {
        return new EntradaVeiculo(LocalDateTime.now(relogio), 120_000.0, NivelCombustivel.MEIO,
                new Vistoria(List.of(new ItemVistoria("Lataria", "ok")), ""));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    /** O laudo da bateria, com uma medição e um código. */
    private LaudoEletrico laudo(String queixa, LocalDateTime quando, ResultadoLaudo resultado) {
        return new LaudoEletrico(quando, queixa, List.of(
                new Medicao("Tensão da bateria", "12,1", "V", CondicaoMedicao.REPOUSO, "12,4 a 12,7 V", true)),
                List.of(new CodigoFalha("P0562", "Tensão do sistema baixa", SituacaoCodigo.HISTORICO)),
                "Teste de fuga.", "Módulo do som direto na bateria.", "Ligação pelo pós-chave.", "", resultado);
    }

    private LaudoEletrico laudo(String queixa) {
        return laudo(queixa, agora(), null);
    }

    /** Leva a OS até a entrega: orçamento aprovado, serviço começado, pronto e entregue. */
    private void entregar(long osId) {
        OrcamentoSalvo orcamento = orcamentos.criarNaOs(osId, new Orcamento(List.of(ItemOrcamento.digitado(
                TipoItem.SERVICO, "Diagnóstico elétrico", BigDecimal.ONE, "h", Dinheiro.ler("180,00"))),
                Desconto.NENHUM, 1, "", 15, "", ""));
        orcamentos.marcarEnviado(orcamento.id());
        orcamentos.aprovar(orcamento.id(), agora().minusMinutes(5), MeioAprovacao.WHATSAPP, "Pode fazer.");
        oficina.mudarStatus(osId, StatusOs.EM_EXECUCAO, "");
        oficina.mudarStatus(osId, StatusOs.PRONTA, "");
        oficina.entregar(osId, new Entrega(agora(), 120_010.0, new Garantia(90)));
    }

    @Test
    void laudoNovoJaVemPreenchido() {
        Rascunho rascunho = laudos.rascunho(os);
        assertEquals("Bateria descarrega de um dia para o outro", rascunho.laudo().queixa(), "o serviço pedido");
        assertEquals(LocalDateTime.of(2026, 10, 20, 9, 30), rascunho.laudo().dataHora(), "agora");
        assertEquals(carlos, rascunho.mecanicoId(), "o mecânico da OS");
        assertNull(rascunho.laudo().resultado(), "a definir");

        oficina.ativarMecanico(carlos, false);
        assertNull(laudos.rascunho(os).mecanicoId(), "o mecânico inativo não vem");
    }

    @Test
    void criarMudarEApagar() {
        Gravado gravado = laudos.criar(os, laudo("Bateria descarrega"), carlos);
        assertEquals(List.of(), gravado.avisos());
        List<LaudoSalvo> daOs = laudos.daOs(os);
        assertEquals(1, daOs.size());
        assertEquals("Carlos", daOs.getFirst().mecanico());
        assertEquals(laudo("Bateria descarrega"), daOs.getFirst().laudo());

        LaudoEletrico resolvido = laudo("Bateria descarrega", agora(), ResultadoLaudo.RESOLVIDO);
        assertEquals(List.of(), laudos.salvar(gravado.laudoId(), resolvido, null));
        LaudoSalvo salvo = laudos.buscar(gravado.laudoId()).orElseThrow();
        assertEquals(ResultadoLaudo.RESOLVIDO, salvo.laudo().resultado());
        assertNull(salvo.mecanicoId(), "sem mecânico");

        laudos.apagar(gravado.laudoId());
        assertEquals(List.of(), laudos.daOs(os), "com a OS em andamento, o laudo feito por engano sai");
    }

    @Test
    void regrasAoGravar() {
        EntradaInvalidaException vazio = assertThrows(EntradaInvalidaException.class, () -> laudos.criar(os,
                new LaudoEletrico(agora(), " ", List.of(new Medicao("Tensão", "", "V", null, "", false)), List.of(),
                        "", "", "", "", null), null));
        assertEquals(List.of("Escreva a queixa do cliente, nas palavras dele.", "Medição 1 (Tensão): escreva o valor.",
                "Medição 1 (Tensão): escolha a condição (repouso, motor ligado, carga acionada ou outra)."),
                vazio.erros());
        assertEquals(List.of("A data e a hora do laudo não podem estar no futuro."), assertThrows(
                EntradaInvalidaException.class, () -> laudos.criar(os, laudo("Queixa", agora().plusHours(2), null),
                        null)).erros());
        assertEquals(List.of("A data do laudo não pode ser antes da abertura da OS (20/10/2026 09:00)."),
                assertThrows(EntradaInvalidaException.class, () -> laudos.criar(os, laudo("Queixa",
                        LocalDateTime.of(2026, 10, 19, 18, 0), null), null)).erros());
        assertEquals(List.of(), laudos.daOs(os), "nada foi gravado");

        long laudo = laudos.criar(os, laudo("Queixa"), carlos).laudoId();
        oficina.ativarMecanico(carlos, false);
        assertEquals(List.of(), laudos.salvar(laudo, laudo("Queixa corrigida"), carlos),
                "quem já estava no laudo continua valendo");
        long ana = oficina.novoMecanico("Ana");
        oficina.ativarMecanico(ana, false);
        assertThrows(OperacaoRecusadaException.class, () -> laudos.salvar(laudo, laudo("Queixa"), ana));
        assertThrows(OperacaoRecusadaException.class, () -> laudos.criar(os, laudo("Queixa"), carlos),
                "o laudo novo só com mecânico ativo");
    }

    /** O aviso de LGPD só aparece quando a queixa parece ter dado pessoal; não impede de gravar. */
    @Test
    void avisoDeLgpdSoQuandoParece() {
        Gravado comNome = laudos.criar(os, laudo("A cliente Ana disse que liga no 11 91234-5678"), carlos);
        assertEquals(List.of("A queixa parece ter dado pessoal do cliente: o nome do cliente (Ana), um telefone ou "
                + "documento (11 91234-5678). O laudo fica mesmo se o cliente for anonimizado; se for dado pessoal, "
                + "tire da queixa e salve de novo."), comNome.avisos());
        assertEquals(1, laudos.daOs(os).size(), "gravou assim mesmo");
        assertEquals(List.of(), laudos.salvar(comNome.laudoId(), laudo("A cliente disse que a bateria descarrega"),
                carlos), "sem o dado pessoal, sem aviso");
    }

    /**
     * Teste nº 6 da Fase 3B (a parte do laudo): depois da entrega, o laudo não muda, não se apaga e a OS não ganha
     * laudo novo; ele continua sendo lido e saindo em PDF.
     */
    @Test
    void depoisDaEntregaOLaudoNaoMuda() {
        LaudoEletrico conteudo = laudo("Bateria descarrega");
        long laudo = laudos.criar(os, conteudo, carlos).laudoId();
        relogio.andar(Duration.ofHours(1));
        entregar(os);

        assertEquals("A OS OS-2026-0001 está \"Entregue\": o laudo não muda mais.", assertThrows(
                OperacaoRecusadaException.class, () -> laudos.salvar(laudo, laudo("Outra"), carlos)).getMessage());
        assertEquals("A OS OS-2026-0001 está \"Entregue\": o laudo não é apagado.", assertThrows(
                OperacaoRecusadaException.class, () -> laudos.apagar(laudo)).getMessage());
        assertEquals("A OS OS-2026-0001 está \"Entregue\": não ganha laudo novo.", assertThrows(
                OperacaoRecusadaException.class, () -> laudos.criar(os, laudo("Nova"), null)).getMessage());
        assertThrows(OperacaoRecusadaException.class, () -> laudos.rascunho(os));
        assertEquals(conteudo, laudos.buscar(laudo).orElseThrow().laudo(), "dá para ler");
        assertEquals("OS-2026-0001", laudos.documento(laudo, DadosOficina.vazia(), null, "0.3.1-teste").numeroOs(),
                "e o PDF continua saindo");
    }

    /**
     * Teste nº 7 da Fase 3B (a parte do laudo): anonimizar o cliente deixa o laudo igual (é dado técnico); o PDF sai
     * com o nome anonimizado.
     */
    @Test
    void anonimizarMantemOLaudo() {
        LaudoEletrico conteudo = laudo("Bateria descarrega de um dia para o outro");
        long laudo = laudos.criar(os, conteudo, carlos).laudoId();
        long cliente = oficina.buscar(os).cliente().id();
        oficina.anonimizarCliente(cliente);

        assertEquals(conteudo, laudos.buscar(laudo).orElseThrow().laudo(), "o laudo fica igual");
        String anonimizado = oficina.buscar(os).cliente().cliente().nome();
        assertFalse(anonimizado.contains("Ana"), anonimizado);
        assertEquals(anonimizado, laudos.documento(laudo, DadosOficina.vazia(), null, "0.3.1-teste").cliente());
        assertEquals(List.of(), laudos.salvar(laudo, laudo("Anonimizado " + anonimizado), carlos),
                "o nome do cliente anonimizado não dispara o aviso");
    }

    @Test
    void laudosDoVeiculoEDocumento() {
        LaudoEletrico conteudo = laudo("Bateria descarrega");
        long primeiro = laudos.criar(os, conteudo, carlos).laudoId();
        long veiculo = oficina.buscar(os).veiculo().id();
        long cliente = oficina.buscar(os).cliente().id();
        relogio.andar(Duration.ofDays(10));
        long segundaOs = oficina.abrir(new NovaOs(new CasosProjeto.ClienteExistente(cliente),
                new VeiculoExistente(veiculo), "Luz do alternador acesa", null, null, "", entrada())).osId();
        relogio.andar(Duration.ofMinutes(10));
        long segundo = laudos.criar(segundaOs, laudo("Luz do alternador acesa"), null).laudoId();

        List<LaudoSalvo> doVeiculo = laudos.doVeiculo(veiculo);
        assertEquals(List.of(segundo, primeiro), doVeiculo.stream().map(LaudoSalvo::id).toList(), "o mais novo antes");
        assertEquals(List.of("OS-2026-0002", "OS-2026-0001"), doVeiculo.stream().map(l -> l.numeroOs().texto())
                .toList());

        DadosLaudo dados = laudos.documento(primeiro, DadosOficina.vazia(), null, "0.3.1-teste");
        assertEquals("Ana Costa", dados.cliente());
        assertEquals("Carlos", dados.mecanico());
        assertEquals(conteudo, dados.laudo());
        assertTrue(dados.veiculo().endsWith("ABC1D23"), dados.veiculo());
        assertEquals("", laudos.documento(segundo, DadosOficina.vazia(), null, "0.3.1-teste").mecanico(),
                "sem mecânico");
    }
}
