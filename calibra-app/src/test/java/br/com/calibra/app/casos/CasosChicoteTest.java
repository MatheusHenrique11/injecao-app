package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import br.com.calibra.app.casos.CasosChicote.Opcoes;
import br.com.calibra.app.casos.CasosOficina.MarcacaoDoItem;
import br.com.calibra.app.casos.CasosOficina.NovaOs;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.app.catalogo.ExemplosCatalogo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.FichaChicote;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.Marcacao;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.PontoChicote;
import br.com.calibra.core.oficina.SituacaoChicote;
import br.com.calibra.core.oficina.SituacaoItem;
import br.com.calibra.core.oficina.StatusOs;
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
import br.com.calibra.core.pecas.Injecao;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.oficina.FichaChicoteSalva;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;
import br.com.calibra.relatorios.DadosFichaChicote;

/**
 * As fichas de chicote da OS (Fase 3B, bloco 3B.6; SPEC 6.6.5): a ficha nova preenchida pelo projeto de preparação,
 * gravar, mudar e apagar, as regras, a versão nova que deixa a anterior só para ver (teste nº 13), a situação
 * "entregue" na entrega da OS, os testes de bancada do checklist no PDF e o cliente anonimizado. Dados fictícios.
 */
class CasosChicoteTest {

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 20, 9, 0));
    private CasosProjeto projetos;
    private CasosOrcamento orcamentos;
    private CasosOficina oficina;
    private CasosCatalogo catalogo;
    private CasosChicote chicotes;

    @BeforeEach
    void abrirBanco() throws IOException {
        PastasDados pastas = new PastasDados(raiz.resolve("dados"));
        pastas.criar();
        BancoDados banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.3.0-teste", relogio));
        projetos = new CasosProjeto(banco, relogio, "0.3.0-teste");
        orcamentos = new CasosOrcamento(banco, relogio, projetos);
        oficina = new CasosOficina(banco, relogio, projetos, pastas.anexos());
        catalogo = new CasosCatalogo(banco, relogio);
        chicotes = new CasosChicote(banco, relogio);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    /** A OS de Bruno, sem projeto de preparação e sem a entrada do veículo. */
    private long osSemProjeto(String placa) {
        long os = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Bruno Dias")),
                new VeiculoNovo(CasosProjetoTest.carro(placa)), "Chicote para injeção programável", null, null, "",
                null)).osId();
        relogio.andar(Duration.ofMinutes(10));
        return os;
    }

    /** O projeto de preparação de Diego (o caso B), com a injeção do catálogo escolhida; devolve o id da OS dele. */
    private long osDoProjeto(ItemCatalogo<Injecao> injecao) {
        EntradaDimensionamento casoB = CasosProjetoTest.casoB();
        long projeto = projetos.criarProjeto(new NovoProjeto(new ClienteNovo(CasosProjetoTest.cliente("Diego Rocha")),
                new VeiculoNovo(CasosProjetoTest.carro("DEF4G56")), "AP 2.0 turbo", casoB.motor(), casoB.meta()));
        projetos.trocarInjecao(projeto, injecao.id());
        long os = oficina.osAtualDoProjeto(projeto);
        relogio.andar(Duration.ofMinutes(10));
        return os;
    }

    private ItemCatalogo<Injecao> injecaoDoCatalogo() {
        ExemplosCatalogo.Exemplo<Injecao> exemplo = ExemplosCatalogo.injecao();
        return catalogo.criar(exemplo.tipo(), exemplo.dados(), exemplo.info());
    }

    private static FichaChicote ficha(String versao, SituacaoChicote situacao) {
        return new FichaChicote("FuelTech FT450", null, null, versao, "Manual da FT450, conector A", List.of(
                new PontoChicote("Bico do cilindro 1", "A", "12", "", false),
                new PontoChicote("TPS", "B", "4", "sinal de 0 a 5 V", true)), "Cabo 1 mm²", "", situacao);
    }

    /** Leva a OS até a entrega: entrada, orçamento aprovado, serviço começado, pronto e entregue. */
    private void entregar(long osId) {
        oficina.registrarEntrada(osId, new EntradaVeiculo(agora(), 120_000.0, NivelCombustivel.MEIO,
                new Vistoria(List.of(new ItemVistoria("Lataria", "ok")), "")));
        OrcamentoSalvo orcamento = orcamentos.criarNaOs(osId, new Orcamento(List.of(ItemOrcamento.digitado(
                TipoItem.SERVICO, "Confecção de chicote", BigDecimal.ONE, "h", Dinheiro.ler("180,00"))),
                Desconto.NENHUM, 1, "", 15, "", ""));
        orcamentos.marcarEnviado(orcamento.id());
        orcamentos.aprovar(orcamento.id(), agora(), MeioAprovacao.WHATSAPP, "Pode fazer.");
        oficina.mudarStatus(osId, StatusOs.EM_EXECUCAO, "");
        oficina.mudarStatus(osId, StatusOs.PRONTA, "");
        relogio.andar(Duration.ofMinutes(30));
        oficina.entregar(osId, new Entrega(agora(), 120_010.0, new Garantia(90)));
    }

    /** SPEC 6.6.5, regra 4: com o projeto de preparação ligado à OS, a ficha nova já vem com a injeção e o motor. */
    @Test
    void fichaNovaVemDoProjeto() {
        ItemCatalogo<Injecao> ecu = injecaoDoCatalogo();
        long os = osDoProjeto(ecu);
        FichaChicote nova = chicotes.nova(os);
        assertEquals("Marca Teste ECU 8", nova.injecao());
        assertEquals(ecu.id(), nova.injecaoItemId());
        Opcoes opcoes = chicotes.opcoes(os);
        assertEquals(List.of("v1 · AP 2.0 turbo"), opcoes.motores().stream().map(CasosChicote.OpcaoMotor::rotulo)
                .toList(), "as versões do motor do projeto");
        assertEquals(opcoes.motores().getFirst().id(), nova.motorVersaoId(), "a versão de agora do motor");
        assertEquals("v1", nova.versaoChicote());
        assertEquals(SituacaoChicote.EM_CONFECCAO, nova.situacao());
        assertTrue(opcoes.injecoes().stream().anyMatch(i -> i.id() == ecu.id()), "as injeções do catálogo");

        long semProjeto = osSemProjeto("ABC1D23");
        FichaChicote vazia = chicotes.nova(semProjeto);
        assertEquals("", vazia.injecao(), "sem projeto, a injeção vem do catálogo ou é texto livre");
        assertNull(vazia.motorVersaoId());
        assertEquals(List.of(), chicotes.opcoes(semProjeto).motores());
    }

    @Test
    void criarMudarEApagar() {
        long os = osSemProjeto("ABC1D23");
        long id = chicotes.criar(os, ficha("v1", SituacaoChicote.EM_CONFECCAO));
        assertEquals(ficha("v1", SituacaoChicote.EM_CONFECCAO), chicotes.buscar(id).orElseThrow().ficha());
        chicotes.salvar(id, ficha("v1", SituacaoChicote.TESTADO));
        assertEquals(SituacaoChicote.TESTADO, chicotes.buscar(id).orElseThrow().ficha().situacao());
        chicotes.apagar(id);
        assertEquals(List.of(), chicotes.daOs(os), "com a OS em andamento, a ficha feita por engano sai");
    }

    @Test
    void regrasAoGravar() {
        long os = osSemProjeto("ABC1D23");
        EntradaInvalidaException vazia = assertThrows(EntradaInvalidaException.class, () -> chicotes.criar(os,
                new FichaChicote("", null, null, "v1", " ", List.of(), "", "", SituacaoChicote.ENTREGUE)));
        assertEquals(List.of("Escreva de onde veio o pinout (ex.: o manual da injeção, o modelo do fabricante).",
                "A situação \"entregue\" vem sozinha quando a OS é entregue: escolha em confecção ou testado."),
                vazia.erros());
        OperacaoRecusadaException semInjecao = assertThrows(OperacaoRecusadaException.class, () -> chicotes.criar(
                os, new FichaChicote("ECU sumida", 999L, null, "v1", "Manual", List.of(), "", "",
                        SituacaoChicote.EM_CONFECCAO)));
        assertEquals("A injeção escolhida não está no catálogo de injeções.", semInjecao.getMessage());
        OperacaoRecusadaException semProjeto = assertThrows(OperacaoRecusadaException.class, () -> chicotes.criar(
                os, new FichaChicote("", null, 1L, "v1", "Manual", List.of(), "", "", SituacaoChicote.EM_CONFECCAO)));
        assertEquals("A versão do motor precisa ser do projeto de preparação da OS.", semProjeto.getMessage());
        assertEquals(List.of(), chicotes.daOs(os), "nada foi gravado");
    }

    /** Teste nº 13 da Fase 3B: a versão nova não apaga a anterior, que fica só para ver. */
    @Test
    void versaoNovaDeixaAAnteriorSoParaVer() {
        long os = osSemProjeto("ABC1D23");
        long v1 = chicotes.criar(os, ficha("v1", SituacaoChicote.TESTADO));
        FichaChicote rascunho = chicotes.versaoNova(v1);
        assertEquals("v2", rascunho.versaoChicote());
        assertEquals(SituacaoChicote.EM_CONFECCAO, rascunho.situacao(), "a versão nova começa em confecção");
        assertEquals(ficha("v1", SituacaoChicote.TESTADO).pontos(), rascunho.pontos(), "com os pontos da anterior");
        long v2 = chicotes.criarVersaoNova(v1, rascunho);

        assertEquals(List.of(v1, v2), chicotes.daOs(os).stream().map(FichaChicoteSalva::id).toList());
        OperacaoRecusadaException mudar = assertThrows(OperacaoRecusadaException.class,
                () -> chicotes.salvar(v1, ficha("v1", SituacaoChicote.EM_CONFECCAO)));
        assertEquals("A versão v1 do chicote já tem uma versão nova: ela fica só para ver.", mudar.getMessage());
        assertThrows(OperacaoRecusadaException.class, () -> chicotes.versaoNova(v1), "a nova sai da última");
        assertThrows(OperacaoRecusadaException.class, () -> chicotes.apagar(v1), "a anterior nunca sai");
        assertEquals(ficha("v1", SituacaoChicote.TESTADO), chicotes.buscar(v1).orElseThrow().ficha());

        chicotes.apagar(v2);
        chicotes.salvar(v1, ficha("v1", SituacaoChicote.EM_CONFECCAO));
    }

    /**
     * Na entrega da OS, a última versão de cada chicote fica "entregue"; depois, a ficha só se lê (o mesmo do teste
     * nº 6, para a ficha), e o PDF continua saindo.
     */
    @Test
    void naEntregaFicaEntregueEDepoisSoSeLe() {
        long os = osSemProjeto("ABC1D23");
        long v1 = chicotes.criar(os, ficha("v1", SituacaoChicote.TESTADO));
        long v2 = chicotes.criarVersaoNova(v1, chicotes.versaoNova(v1));
        entregar(os);
        assertEquals(SituacaoChicote.ENTREGUE, chicotes.buscar(v2).orElseThrow().ficha().situacao());
        assertEquals(SituacaoChicote.TESTADO, chicotes.buscar(v1).orElseThrow().ficha().situacao(),
                "a versão anterior fica como era");

        OperacaoRecusadaException mudar = assertThrows(OperacaoRecusadaException.class,
                () -> chicotes.salvar(v2, ficha("v2", SituacaoChicote.TESTADO)));
        assertEquals("A OS OS-2026-0001 está \"Entregue\": a ficha do chicote não muda mais.", mudar.getMessage());
        assertThrows(OperacaoRecusadaException.class, () -> chicotes.nova(os));
        assertThrows(OperacaoRecusadaException.class, () -> chicotes.criar(os, ficha("v1",
                SituacaoChicote.EM_CONFECCAO)));
        assertThrows(OperacaoRecusadaException.class, () -> chicotes.apagar(v2));
        assertEquals("entregue", chicotes.documento(v2, DadosOficina.vazia(), null, "0.3.0-teste").ficha()
                .rotuloDaSituacao(), "o PDF continua saindo");
    }

    /** Os testes de bancada do PDF vêm do checklist da confecção (o serviço de bancada), como está marcado. */
    @Test
    void testesDeBancadaDoChecklistNoPdf() {
        ServicoPadrao confeccao = new ServicoPadrao("Confecção de chicote teste", 8.0, "Tabela da oficina", null,
                CategoriaServico.INJECAO_PROGRAMAVEL, "", false, null,
                catalogo.checklistsDasSementes().get(ServicosSugeridos.CHECKLIST_CHICOTE));
        ItemCatalogo<ServicoPadrao> servico = catalogo.criar(TipoCatalogo.SERVICO, confeccao, new InfoCatalogo(
                confeccao.nome(), "Tabela da oficina", null, false, null, "Dados fictícios."));
        long os = osSemProjeto("ABC1D23");
        OrcamentoSalvo orcamento = orcamentos.criarNaOs(os, new Orcamento(List.of(ItemOrcamento.doServico(
                confeccao.textoNoOrcamento(), confeccao.horas(), null, Dinheiro.ler("125,00"), servico.id(),
                servico.versao())), Desconto.NENHUM, 1, "", 15, "", ""));
        orcamentos.marcarEnviado(orcamento.id());
        orcamentos.aprovar(orcamento.id(), agora(), MeioAprovacao.WHATSAPP, "Pode fazer.");
        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");
        long carlos = oficina.novoMecanico("Carlos");
        long primeiro = oficina.checklists(os).getFirst().checklist().itens().getFirst().id();
        oficina.marcar(os, List.of(new MarcacaoDoItem(primeiro, new Marcacao(SituacaoItem.FEITO, "pinout conferido",
                ""))), carlos);

        long id = chicotes.criar(os, ficha("v1", SituacaoChicote.TESTADO));
        DadosFichaChicote documento = chicotes.documento(id, DadosOficina.vazia(), null, "0.3.0-teste");
        assertEquals(12, documento.bancada().size(), "os itens do checklist da confecção");
        DadosFichaChicote.ItemDeBancada feito = documento.bancada().getFirst();
        assertEquals(SituacaoItem.FEITO, feito.situacao());
        assertEquals("pinout conferido", feito.observacao());
        assertEquals("Carlos", feito.marcadoPor());
        assertEquals(SituacaoItem.PENDENTE, documento.bancada().get(1).situacao());
        assertEquals("", documento.motor(), "sem projeto, sem motor");
        assertEquals("OS-2026-0001", documento.numeroOs());
        assertEquals(documento.bancada(), chicotes.bancada(os), "a janela mostra o mesmo");
    }

    /** O motor da ficha no PDF é a versão do projeto; a ficha fica igual quando o cliente é anonimizado. */
    @Test
    void motorNoPdfEClienteAnonimizado() {
        long os = osDoProjeto(injecaoDoCatalogo());
        FichaChicote nova = chicotes.nova(os);
        long id = chicotes.criar(os, new FichaChicote(nova.injecao(), nova.injecaoItemId(), nova.motorVersaoId(),
                "v1", "Manual da ECU 8", List.of(), "", "", SituacaoChicote.EM_CONFECCAO));
        assertEquals("v1 · AP 2.0 turbo", chicotes.documento(id, DadosOficina.vazia(), null, "0.3.0-teste")
                .motor());
        FichaChicote antes = chicotes.buscar(id).orElseThrow().ficha();

        oficina.anonimizarCliente(oficina.buscar(os).os().clienteId());
        assertEquals(antes, chicotes.buscar(id).orElseThrow().ficha(), "a ficha é dado técnico e fica");
        assertTrue(chicotes.documento(id, DadosOficina.vazia(), null, "0.3.0-teste").cliente()
                .startsWith("Cliente anonimizado #"), "o PDF sai com o nome anonimizado");
    }

    /** No veículo, as fichas de todas as OS dele, da mais nova para a mais antiga. */
    @Test
    void fichasDoVeiculo() {
        long primeira = osSemProjeto("ABC1D23");
        long ficha1 = chicotes.criar(primeira, ficha("v1", SituacaoChicote.EM_CONFECCAO));
        long veiculo = oficina.buscar(primeira).os().veiculoId();
        long segunda = oficina.abrir(new NovaOs(new CasosProjeto.ClienteExistente(oficina.buscar(primeira).os()
                .clienteId()), new CasosProjeto.VeiculoExistente(veiculo), "Revisão do chicote", null, null, "",
                null)).osId();
        long ficha2 = chicotes.criar(segunda, ficha("v1", SituacaoChicote.EM_CONFECCAO));
        assertEquals(List.of(ficha2, ficha1), chicotes.doVeiculo(veiculo).stream().map(FichaChicoteSalva::id)
                .toList());
        assertEquals("OS-2026-0002", chicotes.doVeiculo(veiculo).getFirst().numeroOs().texto());
    }
}
