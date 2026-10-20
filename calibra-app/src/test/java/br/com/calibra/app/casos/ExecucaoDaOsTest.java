package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosOficina.ChecklistDaOs;
import br.com.calibra.app.casos.CasosOficina.MarcacaoDoItem;
import br.com.calibra.app.casos.CasosOficina.NovaOs;
import br.com.calibra.app.casos.CasosOficina.NovoAnexo;
import br.com.calibra.app.casos.CasosOficina.Pendencia;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.oficina.ChecklistModelo;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.FormaPagamento;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.ItemDoChecklist;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.Marcacao;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.oficina.ResumoChecklist;
import br.com.calibra.core.oficina.SituacaoItem;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.orcamento.ServicosSugeridos;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.oficina.ChecklistSalvo;
import br.com.calibra.dados.oficina.ItemChecklistSalvo;
import br.com.calibra.dados.oficina.MudancaStatusOs;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;
import br.com.calibra.relatorios.DadosOs;

/**
 * A execução da OS com os checklists (Fase 3B, bloco 3B.3; SPEC 6.5): a cópia ao começar o serviço, as marcações, a
 * trava do "pronto" e a revisão aprovada no meio do serviço. Testes obrigatórios nº 2, 3, 4 e 6 da fase. Dados
 * fictícios; os modelos são os 7 que vêm com o app.
 */
class ExecucaoDaOsTest {

    private static final String SOM = "Instalação de som completa";
    private static final String ALTERNADOR = "Reparo ou troca de alternador";
    private static final Dinheiro HORA = Dinheiro.ler("180,00");

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 19, 9, 0));
    private CasosOrcamento orcamentos;
    private CasosOficina oficina;
    private CasosCatalogo catalogo;

    @BeforeEach
    void abrirBanco() throws IOException {
        PastasDados pastas = new PastasDados(raiz.resolve("dados"));
        pastas.criar();
        BancoDados banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.3.0-teste", relogio));
        CasosProjeto casos = new CasosProjeto(banco, relogio, "0.3.0-teste");
        orcamentos = new CasosOrcamento(banco, relogio, casos);
        oficina = new CasosOficina(banco, relogio, casos, pastas.anexos());
        catalogo = new CasosCatalogo(banco, relogio);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    /** Um serviço do catálogo, com o modelo que veio com o app (pela chave) ou sem checklist (null). */
    private ItemCatalogo<ServicoPadrao> servico(String nome, String chaveDoModelo) {
        Long modelo = chaveDoModelo == null ? null : catalogo.checklistsDasSementes().get(chaveDoModelo);
        ServicoPadrao servico = new ServicoPadrao(nome, 2.0, "Tabela da oficina", null, CategoriaServico.OUTRO, "",
                true, null, modelo);
        return catalogo.criar(TipoCatalogo.SERVICO, servico, new InfoCatalogo(nome, "Tabela da oficina", null, false,
                null, ""));
    }

    private static ItemOrcamento linha(ItemCatalogo<ServicoPadrao> servico) {
        return ItemOrcamento.doServico(servico.dados().textoNoOrcamento(), servico.dados().horas(), null, HORA,
                servico.id(), servico.versao());
    }

    private static Orcamento orcamento(List<ItemOrcamento> itens) {
        return new Orcamento(itens, Desconto.NENHUM, 1, "", 15, "", "");
    }

    private long abrirComEntrada(String cliente, String placa) {
        EntradaVeiculo entrada = new EntradaVeiculo(agora(), 120_000.0, NivelCombustivel.MEIO,
                new Vistoria(List.of(new ItemVistoria("Lataria", "ok")), ""));
        return oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente(cliente)),
                new VeiculoNovo(CasosProjetoTest.carro(placa)), "Instalação de som", null, null, "", entrada)).osId();
    }

    private OrcamentoSalvo aprovar(long os, List<ItemOrcamento> itens) {
        OrcamentoSalvo orcamento = orcamentos.criarNaOs(os, orcamento(itens));
        enviarEAprovar(orcamento);
        return orcamento;
    }

    private void enviarEAprovar(OrcamentoSalvo orcamento) {
        orcamentos.marcarEnviado(orcamento.id());
        orcamentos.aprovar(orcamento.id(), agora().minusMinutes(5), MeioAprovacao.WHATSAPP, "Pode fazer.");
    }

    /** Uma OS com o som (e o que mais vier) aprovado e o serviço começado. */
    private long emExecucao(String placa, ItemCatalogo<?>... servicos) {
        long os = abrirComEntrada("Ana Costa", placa);
        List<ItemOrcamento> itens = new ArrayList<>();
        for (ItemCatalogo<?> servico : servicos) {
            itens.add(linha(catalogo.buscar(TipoCatalogo.SERVICO, servico.id()).orElseThrow()));
        }
        aprovar(os, itens);
        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");
        return os;
    }

    private List<ItemChecklistSalvo> itens(long os, int checklist) {
        return oficina.checklists(os).get(checklist).checklist().itens();
    }

    /** Marca todos os itens do checklist como feitos, menos os das posições dadas. */
    private void marcarFeitos(long os, int checklist, Long mecanico, int... menos) {
        List<MarcacaoDoItem> marcacoes = new ArrayList<>();
        List<ItemChecklistSalvo> itens = itens(os, checklist);
        for (int i = 0; i < itens.size(); i++) {
            int indice = i;
            if (Arrays.stream(menos).noneMatch(m -> m == indice)) {
                marcacoes.add(new MarcacaoDoItem(itens.get(i).id(), new Marcacao(SituacaoItem.FEITO, "", "")));
            }
        }
        oficina.marcar(os, marcacoes, mecanico);
    }

    /**
     * Teste nº 2 da Fase 3B: entrar em execução cria uma cópia do checklist por serviço do catálogo; mudar o modelo
     * depois não altera a cópia, e voltar a "em execução" não copia de novo.
     */
    @Test
    void entrarEmExecucaoCopiaOChecklistDeCadaServico() {
        ItemCatalogo<ServicoPadrao> som = servico(SOM, ServicosSugeridos.CHECKLIST_SOM);
        ItemCatalogo<ServicoPadrao> alternador = servico(ALTERNADOR, null);
        ItemCatalogo<ServicoPadrao> semChecklist = servico("Lavagem do motor", null);
        long os = abrirComEntrada("Ana Costa", "ABC1D23");
        aprovar(os, List.of(linha(som), linha(alternador), linha(semChecklist),
                ItemOrcamento.digitado(TipoItem.SERVICO, "Ajuste do rádio", BigDecimal.ONE, "h", HORA)));
        ServicoPadrao comChecklist = new ServicoPadrao(ALTERNADOR, 2.0, "Tabela da oficina", null,
                CategoriaServico.OUTRO, "", true, null,
                catalogo.checklistsDasSementes().get(ServicosSugeridos.CHECKLIST_PARTIDA_E_ALTERNADOR));
        catalogo.salvar(TipoCatalogo.SERVICO, alternador.id(), comChecklist, alternador.info());
        assertEquals(List.of(), oficina.checklists(os), "antes de começar o serviço, nenhuma cópia");

        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");

        List<ChecklistDaOs> copias = oficina.checklists(os);
        assertEquals(List.of(SOM, ALTERNADOR), copias.stream().map(c -> c.checklist().servico()).toList(),
                "na ordem do orçamento; o alternador com o checklist que tem agora no catálogo; a linha digitada e o "
                        + "serviço sem checklist ficam sem");
        ChecklistSalvo copiaDoSom = copias.getFirst().checklist();
        assertEquals("Som", copiaDoSom.modeloNome());
        assertEquals(1, copiaDoSom.modeloVersao());
        assertEquals(9, copiaDoSom.itens().size());
        assertEquals(6, copias.get(1).checklist().itens().size());
        assertTrue(copias.stream().allMatch(ChecklistDaOs::noOrcamento));
        assertTrue(copiaDoSom.itens().stream().allMatch(i -> i.item().pendente()), "tudo começa pendente");

        long modelo = copiaDoSom.modeloItemId();
        ItemCatalogo<ChecklistModelo> doCatalogo = catalogo.buscar(TipoCatalogo.CHECKLIST, modelo).orElseThrow();
        ChecklistModelo menor = new ChecklistModelo("Som", doCatalogo.dados().itens().subList(0, 3),
                doCatalogo.dados().fonte(), doCatalogo.dados().data());
        assertTrue(catalogo.salvar(TipoCatalogo.CHECKLIST, modelo, menor, doCatalogo.info()).versaoNova());
        assertEquals(copiaDoSom, oficina.checklists(os).getFirst().checklist(),
                "o modelo mudou (v2, 3 itens), e a cópia da OS continua a mesma");

        oficina.mudarStatus(os, StatusOs.AGUARDANDO_PECAS, "Caixa de som");
        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");
        assertEquals(2, oficina.checklists(os).size(), "voltar a \"em execução\" não copia de novo");
    }

    /**
     * Teste nº 3 da Fase 3B: com item obrigatório pendente, o "pronto" é recusado com a lista do que falta; com a
     * justificativa, a OS fica pronta, e o histórico guarda, com data e hora, o que faltou e a justificativa.
     */
    @Test
    void prontoTravadoComObrigatorioPendente() {
        long os = emExecucao("ABC1D23", servico(SOM, ServicosSugeridos.CHECKLIST_SOM));
        EntradaInvalidaException travado = assertThrows(EntradaInvalidaException.class,
                () -> oficina.mudarStatus(os, StatusOs.PRONTA, "  "));
        assertEquals(10, travado.erros().size(), "a explicação e os 9 itens");
        assertEquals(SOM + ": Medir a tensão da bateria antes de começar", travado.erros().get(1));
        assertEquals(StatusOs.EM_EXECUCAO, oficina.buscar(os).os().status());

        marcarFeitos(os, 0, null, 7);
        assertEquals(List.of(new Pendencia(SOM, "Teste de volume e balanço com o cliente")), oficina.pendencias(os));
        relogio.andar(Duration.ofHours(3));
        oficina.mudarStatus(os, StatusOs.PRONTA, " O cliente testa o volume na retirada. ");

        assertEquals(StatusOs.PRONTA, oficina.buscar(os).os().status());
        MudancaStatusOs pronta = oficina.historico(os).getLast();
        assertEquals(StatusOs.PRONTA, pronta.status());
        assertEquals(LocalDateTime.of(2026, 10, 19, 12, 0), pronta.dataHora(), "com data e hora");
        assertEquals("Pronta com itens obrigatórios do checklist pendentes: " + SOM + ": Teste de volume e balanço "
                + "com o cliente. Justificativa: O cliente testa o volume na retirada.", pronta.observacao());
    }

    /** Sem pendência (tudo feito, ou "não se aplica" com justificativa), o "pronto" segue como antes. */
    @Test
    void prontoSemPendenciaNaoPedeJustificativa() {
        long os = emExecucao("ABC1D23", servico(SOM, ServicosSugeridos.CHECKLIST_SOM));
        marcarFeitos(os, 0, null, 8);
        long fotos = itens(os, 0).get(8).id();
        oficina.marcar(os, List.of(new MarcacaoDoItem(fotos, new Marcacao(SituacaoItem.NAO_SE_APLICA, "",
                "O cliente não quis fotos."))), null);
        assertEquals(List.of(), oficina.pendencias(os));
        oficina.mudarStatus(os, StatusOs.PRONTA, "");
        assertEquals("", oficina.historico(os).getLast().observacao());

        long semChecklist = emExecucao("BCD2E34", servico("Lavagem do motor", null));
        oficina.mudarStatus(semChecklist, StatusOs.PRONTA, "");
        assertEquals(StatusOs.PRONTA, oficina.buscar(semChecklist).os().status(), "OS sem checklist: como antes");
    }

    /** Teste nº 4 da Fase 3B: "não se aplica" sem justificativa é recusado, e nada da lista é gravado. */
    @Test
    void naoSeAplicaSemJustificativaERecusado() {
        long os = emExecucao("ABC1D23", servico(SOM, ServicosSugeridos.CHECKLIST_SOM));
        List<ItemChecklistSalvo> itens = itens(os, 0);
        EntradaInvalidaException erro = assertThrows(EntradaInvalidaException.class, () -> oficina.marcar(os,
                List.of(new MarcacaoDoItem(itens.getFirst().id(), new Marcacao(SituacaoItem.FEITO, "12,6 V", "")),
                        new MarcacaoDoItem(itens.get(8).id(), new Marcacao(SituacaoItem.NAO_SE_APLICA, "", " "))),
                null));
        assertEquals(List.of("Fotos de antes e depois (" + SOM + "): escreva por que o item não se aplica."),
                erro.erros());
        assertTrue(itens(os, 0).stream().allMatch(i -> i.item().pendente()), "nem o item certo foi gravado");
    }

    /**
     * A marcação grava quem marcou e quando, só nos itens que mudaram; o mecânico inativo e o item de outra OS são
     * recusados.
     */
    @Test
    void marcacaoComQuemEQuandoSoNoQueMudou() {
        long carlos = oficina.novoMecanico("Carlos");
        long ana = oficina.novoMecanico("Ana");
        long os = emExecucao("ABC1D23", servico(SOM, ServicosSugeridos.CHECKLIST_SOM));
        List<ItemChecklistSalvo> itens = itens(os, 0);
        MarcacaoDoItem tensao = new MarcacaoDoItem(itens.getFirst().id(), new Marcacao(SituacaoItem.FEITO,
                "12,6 V", ""));
        assertEquals(1, oficina.marcar(os, List.of(tensao), carlos));
        LocalDateTime primeira = agora();
        relogio.andar(Duration.ofMinutes(30));

        MarcacaoDoItem carga = new MarcacaoDoItem(itens.get(1).id(), new Marcacao(SituacaoItem.FEITO, "14,2 V", ""));
        assertEquals(1, oficina.marcar(os, List.of(tensao, carga), ana), "a tensão já estava assim");
        List<ItemChecklistSalvo> depois = itens(os, 0);
        assertEquals("Carlos", depois.getFirst().marcadoPor(), "o que não mudou fica com quem marcou");
        assertEquals(primeira, depois.getFirst().marcadoEm());
        assertEquals("Ana", depois.get(1).marcadoPor());
        assertEquals(agora(), depois.get(1).marcadoEm());
        assertEquals(new ItemDoChecklist("Conferir a carga do alternador", true, true,
                new Marcacao(SituacaoItem.FEITO, "14,2 V", "")), depois.get(1).item());

        oficina.ativarMecanico(ana, false);
        assertThrows(OperacaoRecusadaException.class, () -> oficina.marcar(os, List.of(carga), ana));
        long outra = emExecucao("BCD2E34", servico("Troca de alto-falantes", ServicosSugeridos.CHECKLIST_SOM));
        EntradaInvalidaException deOutra = assertThrows(EntradaInvalidaException.class,
                () -> oficina.marcar(outra, List.of(tensao), carlos));
        assertEquals(List.of("O item #" + tensao.itemId() + " não é do checklist desta OS."), deOutra.erros());
    }

    /**
     * Teste nº 6 da Fase 3B (a parte do checklist): depois da entrega, o checklist não muda, e o pagamento e os anexos
     * continuam.
     */
    @Test
    void depoisDaEntregaOChecklistNaoMudaMasPagamentoEAnexosSim() throws IOException {
        long os = emExecucao("ABC1D23", servico(SOM, ServicosSugeridos.CHECKLIST_SOM));
        marcarFeitos(os, 0, null);
        oficina.mudarStatus(os, StatusOs.PRONTA, "");
        relogio.andar(Duration.ofHours(1));
        oficina.entregar(os, new Entrega(agora(), 120_010.0, new Garantia(90)));

        long item = itens(os, 0).getFirst().id();
        OperacaoRecusadaException erro = assertThrows(OperacaoRecusadaException.class, () -> oficina.marcar(os,
                List.of(new MarcacaoDoItem(item, new Marcacao(SituacaoItem.PENDENTE, "", ""))), null));
        assertEquals("A OS OS-2026-0001 está \"Entregue\": o checklist não muda mais.", erro.getMessage());
        assertFalse(itens(os, 0).getFirst().item().pendente(), "continua feito");
        assertEquals(List.of(new DadosOs.ChecklistDoServico(SOM, new ResumoChecklist(9, 9, 0, 0))),
                oficina.documento(os, DadosOficina.vazia(), null, "0.3.0-teste", "").checklists(),
                "o resumo vai para o comprovante");

        oficina.registrarPagamento(os, new Pagamento(agora(), FormaPagamento.PIX, Dinheiro.ler("360,00"), "resto"));
        assertEquals(1, oficina.pagamento(os).recebimentos().size(), "o pagamento continua");
        Path nota = Files.writeString(raiz.resolve("nota.pdf"), "%PDF-1.7 nota fictícia");
        assertEquals(1, oficina.anexar(os, List.of(new NovoAnexo(nota, TipoAnexo.NOTA_FISCAL, "NFS-e 1", null,
                false))).size(), "e os anexos também");
    }

    /**
     * O cliente pede mais coisa no meio do serviço: a revisão aprovada dá o checklist aos serviços novos. O serviço que
     * sai do orçamento numa revisão nova deixa a cópia guardada, mas ela não trava o "pronto".
     */
    @Test
    void revisaoAprovadaDuranteAExecucao() {
        ItemCatalogo<ServicoPadrao> som = servico(SOM, ServicosSugeridos.CHECKLIST_SOM);
        ItemCatalogo<ServicoPadrao> alternador = servico(ALTERNADOR, ServicosSugeridos.CHECKLIST_PARTIDA_E_ALTERNADOR);
        long os = abrirComEntrada("Ana Costa", "ABC1D23");
        OrcamentoSalvo primeira = aprovar(os, List.of(linha(som)));
        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");
        assertEquals(1, oficina.checklists(os).size());

        OrcamentoSalvo segunda = orcamentos.novaRevisao(primeira.id());
        orcamentos.salvar(segunda.id(), orcamento(List.of(linha(som), linha(alternador))));
        assertEquals(1, oficina.checklists(os).size(), "o rascunho ainda não vale");
        enviarEAprovar(segunda);
        assertEquals(StatusOs.EM_EXECUCAO, oficina.buscar(os).os().status());
        assertEquals(List.of(SOM, ALTERNADOR), oficina.checklists(os).stream().map(c -> c.checklist().servico())
                .toList(), "o alternador ganhou o checklist na aprovação");

        OrcamentoSalvo terceira = orcamentos.novaRevisao(segunda.id());
        orcamentos.salvar(terceira.id(), orcamento(List.of(linha(alternador))));
        enviarEAprovar(terceira);
        List<ChecklistDaOs> copias = oficina.checklists(os);
        assertEquals(2, copias.size(), "a cópia do som fica guardada");
        assertFalse(copias.getFirst().noOrcamento(), "o som saiu do orçamento");
        assertTrue(copias.get(1).noOrcamento());
        assertEquals(6, oficina.pendencias(os).size(), "só os itens do alternador travam o \"pronto\"");
        assertTrue(oficina.pendencias(os).stream().allMatch(p -> p.servico().equals(ALTERNADOR)));
        assertNull(oficina.buscar(os).os().prontaEm());
        assertEquals(List.of(new DadosOs.ChecklistDoServico(ALTERNADOR, new ResumoChecklist(6, 0, 0, 6))),
                oficina.documento(os, DadosOficina.vazia(), null, "0.3.0-teste", "").checklists(),
                "no comprovante, só o do serviço que ficou no orçamento");
    }
}
