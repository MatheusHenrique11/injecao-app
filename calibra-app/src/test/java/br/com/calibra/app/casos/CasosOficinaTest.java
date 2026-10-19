package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosOficina.Anonimizacao;
import br.com.calibra.app.casos.CasosOficina.FichaVeiculo;
import br.com.calibra.app.casos.CasosOficina.Gravado;
import br.com.calibra.app.casos.CasosOficina.NovaOs;
import br.com.calibra.app.casos.CasosOficina.NovoAnexo;
import br.com.calibra.app.casos.CasosOficina.OsAberta;
import br.com.calibra.app.casos.CasosOficina.ProjetoDoVeiculo;
import br.com.calibra.app.casos.CasosOficina.SituacaoDoPagamento;
import br.com.calibra.app.casos.CasosProjeto.ClienteExistente;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.NovoProjeto;
import br.com.calibra.app.casos.CasosProjeto.VeiculoExistente;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.DadosOficina;
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.FormaPagamento;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.GarantiaDoServico;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.oficina.SituacaoPagamento;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.core.orcamento.CategoriaServico;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.orcamento.ServicoPadrao;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.catalogo.InfoCatalogo;
import br.com.calibra.dados.catalogo.ItemCatalogo;
import br.com.calibra.dados.catalogo.TipoCatalogo;
import br.com.calibra.dados.oficina.AnexoSalvo;
import br.com.calibra.dados.oficina.FiltroOs;
import br.com.calibra.dados.oficina.MudancaStatusOs;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.dados.oficina.PagamentoSalvo;
import br.com.calibra.dados.oficina.ResumoOs;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;
import br.com.calibra.relatorios.DadosOrcamento;
import br.com.calibra.relatorios.DadosOs;

/**
 * A OS (fase 3, bloco 3.1): da entrada à entrega de uma OS comum, o orçamento antes do veículo, a recusa e as
 * revisões movendo a OS, a OS de preparação aberta pelo projeto e as mensagens das regras. Do bloco 3.2: os anexos,
 * ligar um projeto à OS e o Painel. Dados fictícios.
 */
class CasosOficinaTest {

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 13, 9, 0));
    private PastasDados pastas;
    private CasosProjeto casos;
    private CasosOrcamento orcamentos;
    private CasosOficina oficina;
    private CasosCatalogo catalogo;

    @BeforeEach
    void abrirBanco() throws IOException {
        pastas = new PastasDados(raiz.resolve("dados"));
        pastas.criar();
        BancoDados banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.3.0-teste", relogio));
        casos = new CasosProjeto(banco, relogio, "0.3.0-teste");
        orcamentos = new CasosOrcamento(banco, relogio, casos);
        oficina = new CasosOficina(banco, relogio, casos, pastas.anexos());
        catalogo = new CasosCatalogo(banco, relogio);
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }

    private EntradaVeiculo entrada(double km) {
        return new EntradaVeiculo(agora(), km, NivelCombustivel.UM_QUARTO, new Vistoria(List.of(
                new ItemVistoria("Lataria", "risco na porta direita"), new ItemVistoria("Estepe", "ok")), ""));
    }

    private long abrirComEntrada(String cliente, String placa) {
        return oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente(cliente)),
                new VeiculoNovo(CasosProjetoTest.carro(placa)), "Revisão e troca de óleo", null, null, "",
                entrada(251_500.0))).osId();
    }

    /** 4 l de óleo a R$ 45,00 e 2 h de mão de obra. */
    private static Orcamento revisao(String valorHora) {
        return new Orcamento(List.of(
                ItemOrcamento.digitado(TipoItem.PECA, "Óleo 5W30 (litro)", BigDecimal.valueOf(4), "l",
                        Dinheiro.ler("45,00")),
                ItemOrcamento.digitado(TipoItem.SERVICO, "Troca de óleo e filtros", BigDecimal.valueOf(2), "h",
                        Dinheiro.ler(valorHora))),
                Desconto.NENHUM, 1, "", 15, "", "");
    }

    private StatusOs status(long os) {
        return oficina.buscar(os).os().status();
    }

    private void enviarEAprovar(OrcamentoSalvo orcamento) {
        orcamentos.marcarEnviado(orcamento.id());
        orcamentos.aprovar(orcamento.id(), agora().minusMinutes(5), MeioAprovacao.WHATSAPP, "Pode fazer.");
    }

    @Test
    void osComumDaEntradaAEntrega() {
        long carlos = oficina.novoMecanico("Carlos");
        Gravado aberta = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Ana Costa")),
                new VeiculoNovo(CasosProjetoTest.carro("ABC1D23")), "Revisão e troca de óleo", carlos,
                LocalDate.of(2026, 10, 15), "", entrada(251_500.0)));
        assertEquals(List.of(), aberta.avisos());
        long os = aberta.osId();
        OsAberta lida = oficina.buscar(os);
        assertEquals("OS-2026-0001", lida.os().numero().texto());
        assertEquals(StatusOs.ABERTA, lida.os().status());
        assertEquals("Carlos", lida.mecanico().nome());
        assertEquals(251_500.0, lida.veiculo().veiculo().usoAcumulado(), "o km da entrada vai para o veículo");
        assertTrue(casos.historicoVeiculo(lida.veiculo().id()).stream()
                .anyMatch(evento -> evento.campo().equals("Uso acumulado")), "com histórico");
        assertNull(lida.pagamento().valor(), "sem orçamento aprovado, sem valor");

        assertTrue(orcamentos.sugestaoDaOs(os, PadroesOrcamento.padrao()).itens().isEmpty(), "OS comum: só os padrões");
        OrcamentoSalvo orcamento = orcamentos.criarNaOs(os, revisao("180,00"));
        assertFalse(orcamento.dePreparacao());
        assertEquals(List.of(), orcamentos.orcamentosDaOs(os).getFirst().motivos(), "a OS comum não desatualiza");
        DadosOrcamento documento = orcamentos.documento(orcamento.id(), DadosOficina.vazia(), null, "0.3.0-teste");
        assertFalse(documento.dePreparacao(), "o PDF da OS comum sai sem motor, meta e resumo técnico");
        assertEquals("Ana Costa", documento.cliente(), "o cliente da OS");
        assertEquals("VW Gol GTi 1994 · ABC1D23", documento.veiculo(), "o veículo da OS");
        assertEquals(LocalDate.of(2026, 10, 13), documento.emissao(), "rascunho: a data da geração");

        OperacaoRecusadaException cedo = assertThrows(OperacaoRecusadaException.class,
                () -> oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, ""));
        assertEquals("A OS OS-2026-0001 está \"Aberta\" e não pode passar para \"Em execução\".", cedo.getMessage());
        orcamentos.marcarEnviado(orcamento.id());
        assertEquals(StatusOs.AGUARDANDO_APROVACAO, status(os));
        orcamentos.aprovar(orcamento.id(), agora().minusMinutes(5), MeioAprovacao.WHATSAPP, "Pode fazer.");
        assertEquals(StatusOs.APROVADA, status(os));

        oficina.registrarPagamento(os, new Pagamento(agora(), FormaPagamento.PIX, Dinheiro.ler("270,00"), "sinal"));
        SituacaoDoPagamento parcial = oficina.pagamento(os);
        assertEquals(Dinheiro.ler("540,00"), parcial.valor(), "4 × 45 + 2 × 180");
        assertEquals(SituacaoPagamento.PARCIAL, parcial.situacao());
        assertEquals(Dinheiro.ler("270,00"), parcial.falta());

        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");
        oficina.mudarStatus(os, StatusOs.AGUARDANDO_PECAS, "Filtro de óleo encomendado.");
        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");
        oficina.mudarStatus(os, StatusOs.PRONTA, "");
        relogio.andar(Duration.ofHours(3));
        assertEquals(List.of(), oficina.entregar(os, new Entrega(agora(), 251_530.0, new Garantia(90))).avisos());
        OsAberta entregue = oficina.buscar(os);
        assertEquals(StatusOs.ENTREGUE, entregue.os().status());
        assertEquals(LocalDate.of(2027, 1, 11), entregue.os().garantiaAte(), "90 dias a partir de 13/10");
        assertEquals(251_530.0, entregue.veiculo().veiculo().usoAcumulado(), "o km da saída também");

        oficina.registrarPagamento(os, new Pagamento(agora(), FormaPagamento.DINHEIRO, Dinheiro.ler("270,00"),
                "na entrega"));
        long errado = oficina.registrarPagamento(os, new Pagamento(agora(), FormaPagamento.DINHEIRO,
                Dinheiro.ler("270,00"), "lançado duas vezes"));
        oficina.cancelarPagamento(errado, "Lançado duas vezes.");
        SituacaoDoPagamento pago = oficina.pagamento(os);
        assertEquals(SituacaoPagamento.PAGO, pago.situacao(), "recebimento depois da entrega vale");
        assertEquals(Dinheiro.ler("540,00"), pago.recebido(), "o cancelado não conta");
        assertEquals(3, pago.recebimentos().size(), "mas fica na lista");
        assertTrue(pago.recebimentos().stream().anyMatch(PagamentoSalvo::cancelado));
        assertEquals(Dinheiro.ZERO, pago.aMais());
        oficina.registrarPagamento(os, new Pagamento(agora(), FormaPagamento.PIX, Dinheiro.ler("10,00"), "a mais"));
        assertEquals(Dinheiro.ler("10,00"), oficina.pagamento(os).aMais(), "o que passou aparece (revisão pelo agente)");

        assertEquals(List.of(StatusOs.ABERTA, StatusOs.AGUARDANDO_APROVACAO, StatusOs.APROVADA, StatusOs.EM_EXECUCAO,
                StatusOs.AGUARDANDO_PECAS, StatusOs.EM_EXECUCAO, StatusOs.PRONTA, StatusOs.ENTREGUE),
                oficina.historico(os).stream().map(MudancaStatusOs::status).toList());
        assertEquals("Orçamento ORC-2026-0001 enviado.", oficina.historico(os).get(1).observacao());
        assertThrows(OperacaoRecusadaException.class, () -> oficina.editarAbertura(os, "Outra coisa", null, null, ""),
                "entregue não muda");
        OperacaoRecusadaException revisao = assertThrows(OperacaoRecusadaException.class,
                () -> orcamentos.novaRevisao(orcamento.id()));
        assertEquals("A OS OS-2026-0001 está \"Entregue\": o orçamento dela não muda mais.", revisao.getMessage());
    }

    /**
     * Os dados dos documentos da OS (bloco 3.3): o cliente como está no cadastro, o veículo, o mecânico, a entrada, o
     * orçamento aprovado e os recebimentos, com o cancelado na lista e fora da conta.
     */
    @Test
    void documentoDaOs() {
        long carlos = oficina.novoMecanico("Carlos");
        long os = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Ana Costa")),
                new VeiculoNovo(CasosProjetoTest.carro("ABC1D23")), "Revisão e troca de óleo", carlos,
                LocalDate.of(2026, 10, 15), "", entrada(251_500.0))).osId();
        DadosOs aberta = oficina.documento(os, DadosOficina.vazia(), null, "0.3.0-teste", "Texto da via (teste).");
        assertEquals("OS-2026-0001", aberta.numero());
        assertEquals(StatusOs.ABERTA, aberta.status());
        assertEquals("Ana Costa", aberta.cliente());
        assertEquals("(11) 98765-4321", aberta.telefoneCliente());
        assertEquals("VW Gol GTi 1994 · ABC1D23", aberta.veiculo());
        assertFalse(aberta.embarcacao());
        assertEquals("Revisão e troca de óleo", aberta.servicoPedido());
        assertEquals(LocalDate.of(2026, 10, 15), aberta.previsao());
        assertEquals("Carlos", aberta.mecanico());
        assertEquals(251_500.0, aberta.entrada().uso());
        assertEquals("risco na porta direita", aberta.entrada().vistoria().itens().getFirst().observacao());
        assertNull(aberta.entrega());
        assertNull(aberta.aprovado(), "sem orçamento aprovado");
        assertNull(aberta.valor());
        assertEquals("Texto da via (teste).", aberta.textoVia());

        enviarEAprovar(orcamentos.criarNaOs(os, revisao("180,00")));
        oficina.registrarPagamento(os, new Pagamento(agora(), FormaPagamento.PIX, Dinheiro.ler("270,00"), "sinal"));
        long errado = oficina.registrarPagamento(os, new Pagamento(agora(), FormaPagamento.PIX, Dinheiro.ler("270,00"),
                "lançado duas vezes"));
        oficina.cancelarPagamento(errado, "Lançado duas vezes.");
        DadosOs paga = oficina.documento(os, DadosOficina.vazia(), null, "0.3.0-teste", "");
        assertEquals("ORC-2026-0001", paga.aprovado().numero());
        assertEquals(Dinheiro.ler("540,00"), paga.valor(), "4 × 45 + 2 × 180");
        assertEquals(List.of(false, true), paga.recebimentos().stream().map(DadosOs.Recebimento::cancelado).toList());
        assertEquals(Dinheiro.ler("270,00"), paga.recebido(), "o cancelado não conta");
        assertEquals(Dinheiro.ler("270,00"), paga.falta());
        assertEquals(SituacaoPagamento.PARCIAL, paga.situacao());
    }

    /**
     * Fase 3B (SPEC 6.6.1): a garantia de cada serviço da revisão aprovada. O do catálogo com garantia própria usa a
     * dele, a da versão que está no orçamento (mudar o serviço no catálogo depois não muda a OS); o do catálogo sem
     * garantia própria e o digitado ficam com a da OS; as peças não entram.
     */
    @Test
    void garantiaDeCadaServicoDoOrcamentoAprovado() {
        ServicoPadrao alternador = servico("Reparo ou troca de alternador", 365);
        ItemCatalogo<ServicoPadrao> comGarantia = catalogo.criar(TipoCatalogo.SERVICO, alternador, info(alternador));
        ServicoPadrao bateria = servico("Teste de bateria e do sistema de carga", null);
        ItemCatalogo<ServicoPadrao> semGarantia = catalogo.criar(TipoCatalogo.SERVICO, bateria, info(bateria));
        long os = abrirComEntrada("Ana Costa", "ABC1D23");
        assertEquals(List.of(), oficina.garantiasDosServicos(os), "sem orçamento aprovado");

        Dinheiro hora = Dinheiro.ler("180,00");
        OrcamentoSalvo orcamento = orcamentos.criarNaOs(os, new Orcamento(List.of(
                ItemOrcamento.digitado(TipoItem.PECA, "Alternador recondicionado", BigDecimal.ONE, "un",
                        Dinheiro.ler("650,00")),
                ItemOrcamento.doServico(alternador.textoNoOrcamento(), alternador.horas(), null, hora,
                        comGarantia.id(), comGarantia.versao()),
                ItemOrcamento.doServico(bateria.textoNoOrcamento(), bateria.horas(), null, hora, semGarantia.id(),
                        semGarantia.versao()),
                ItemOrcamento.digitado(TipoItem.SERVICO, "Limpeza dos terminais", BigDecimal.ONE, "h", hora)),
                Desconto.NENHUM, 1, "", 15, "", ""));
        assertEquals(List.of(), oficina.garantiasDosServicos(os), "o rascunho ainda não vale");
        enviarEAprovar(orcamento);
        assertTrue(catalogo.salvar(TipoCatalogo.SERVICO, comGarantia.id(), servico(alternador.nome(), 30),
                info(alternador)).versaoNova(), "o serviço muda no catálogo depois da aprovação (v2)");

        List<GarantiaDoServico> esperado = List.of(new GarantiaDoServico("Reparo ou troca de alternador", 365),
                new GarantiaDoServico("Teste de bateria e do sistema de carga", null),
                new GarantiaDoServico("Limpeza dos terminais", null));
        assertEquals(esperado, oficina.garantiasDosServicos(os), "a v1, que está no orçamento");
        assertEquals(esperado, oficina.buscar(os).garantias(), "a tela da OS recebe a mesma lista");
        assertEquals(esperado, oficina.documento(os, DadosOficina.vazia(), null, "0.3.0-teste", "").garantias(),
                "e o comprovante de entrega");
    }

    private static ServicoPadrao servico(String nome, Integer garantiaDias) {
        return new ServicoPadrao(nome, 1.0, "Tabela da oficina", null, CategoriaServico.PARTIDA_CARGA, "", true,
                garantiaDias);
    }

    private static InfoCatalogo info(ServicoPadrao servico) {
        return new InfoCatalogo(servico.nome(), "Tabela da oficina", null, false, null, "");
    }

    /**
     * O Painel: os cartões contam as OS em andamento por status; na oficina ficam só as que têm a entrada; nas entregas,
     * as previstas para hoje e as atrasadas, sem as de outro dia e sem as canceladas.
     */
    @Test
    void painel() {
        assertEquals(new CasosOficina.Painel(Map.of(StatusOs.ABERTA, 0, StatusOs.AGUARDANDO_APROVACAO, 0,
                StatusOs.APROVADA, 0, StatusOs.EM_EXECUCAO, 0, StatusOs.AGUARDANDO_PECAS, 0, StatusOs.PRONTA, 0),
                List.of(), List.of()), oficina.painel(), "sem nenhuma OS, tudo zero");

        LocalDate hoje = agora().toLocalDate();
        long ana = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Ana Costa")),
                new VeiculoNovo(CasosProjetoTest.carro("ABC1D23")), "Revisão", null, hoje, "", entrada(251_500.0)))
                .osId();
        long bruno = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Bruno Dias")),
                new VeiculoNovo(CasosProjetoTest.carro("BCD2E34")), "Embreagem", null, hoje.minusDays(1), "", null))
                .osId();
        relogio.andar(Duration.ofHours(1));
        long carla = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Carla Reis")),
                new VeiculoNovo(CasosProjetoTest.carro("CDE3F45")), "Freios", null, hoje.plusDays(7), "",
                entrada(251_500.0))).osId();
        long dani = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Dani Lima")),
                new VeiculoNovo(CasosProjetoTest.carro("DEF4G56")), "Suspensão", null, hoje, "", entrada(251_500.0)))
                .osId();
        oficina.cancelar(dani, "O cliente desistiu.");
        enviarEAprovar(orcamentos.criarNaOs(carla, revisao("180,00")));

        CasosOficina.Painel painel = oficina.painel();
        assertEquals(2, painel.porStatus().get(StatusOs.ABERTA), "Ana e Bruno abertas");
        assertEquals(1, painel.porStatus().get(StatusOs.APROVADA));
        assertFalse(painel.porStatus().containsKey(StatusOs.CANCELADA), "o Painel não mostra as finalizadas");
        assertEquals(List.of(ana, carla), painel.naOficina().stream().map(ResumoOs::osId).toList(),
                "quem chegou primeiro em cima; Bruno ainda não chegou");
        assertEquals(List.of(bruno, ana), painel.entregas().stream().map(ResumoOs::osId).toList(),
                "a atrasada em cima; a de semana que vem e a cancelada ficam fora");
    }

    /** Orçamento por WhatsApp antes do carro chegar: só começa o serviço depois da entrada. */
    @Test
    void orcamentoAntesDoVeiculo() {
        long os = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Bruno Dias")),
                new VeiculoNovo(CasosProjetoTest.carro("BCD2E34")), "Embreagem patinando", null, null,
                "Orçamento pelo WhatsApp.", null)).osId();
        assertFalse(oficina.buscar(os).os().veiculoChegou());
        enviarEAprovar(orcamentos.criarNaOs(os, revisao("180,00")));

        OperacaoRecusadaException semEntrada = assertThrows(OperacaoRecusadaException.class,
                () -> oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, ""));
        assertEquals("Registre a entrada do veículo (km, tanque e vistoria) antes de começar o serviço.",
                semEntrada.getMessage());
        Gravado entrada = oficina.registrarEntrada(os, entrada(249_000.0));
        assertEquals(List.of("O uso da entrada (249.000 km) é menor que o do cadastro do veículo (250.000 km). Gravei "
                + "assim mesmo: confira se não foi erro de digitação ou troca de painel."), entrada.avisos());
        assertEquals(249_000.0, oficina.buscar(os).veiculo().veiculo().usoAcumulado());
        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");
        assertEquals(StatusOs.EM_EXECUCAO, status(os));
    }

    /**
     * Revisão pelo agente: corrigir a entrada sem mexer no km (ex.: só o tanque) não regrava o km no veículo, que pode
     * ter mudado depois por outra OS. Com o km corrigido, ele vai para o veículo.
     */
    @Test
    void corrigirAEntradaSemMexerNoKmNaoMudaOVeiculo() {
        long os = abrirComEntrada("Rita Alves", "NOP4Q56");
        OsAberta aberta = oficina.buscar(os);
        oficina.abrir(new NovaOs(new ClienteExistente(aberta.cliente().id()), new VeiculoExistente(aberta.veiculo().id()),
                "Alinhamento", null, null, "", entrada(251_800.0)));
        assertEquals(251_800.0, oficina.buscar(os).veiculo().veiculo().usoAcumulado(), "a outra OS subiu o km");

        Gravado corrigida = oficina.registrarEntrada(os, new EntradaVeiculo(agora(), 251_500.0, NivelCombustivel.MEIO,
                Vistoria.VAZIA));
        assertEquals(List.of(), corrigida.avisos(), "sem aviso de km menor");
        assertEquals(NivelCombustivel.MEIO, oficina.buscar(os).os().entrada().combustivel(), "a correção grava");
        assertEquals(251_800.0, oficina.buscar(os).veiculo().veiculo().usoAcumulado(), "o km do veículo fica");

        oficina.registrarEntrada(os, new EntradaVeiculo(agora(), 251_900.0, NivelCombustivel.MEIO, Vistoria.VAZIA));
        assertEquals(251_900.0, oficina.buscar(os).veiculo().veiculo().usoAcumulado(), "km corrigido vai para o veículo");
    }

    /** Antes da aprovação, recusa e revisão voltam a OS para aberta; depois, a revisão não mexe na OS. */
    @Test
    void recusaERevisaoMovemAOsSoAntesDaAprovacao() {
        long os = abrirComEntrada("Carla Mendes", "CDE3F45");
        OrcamentoSalvo r1 = orcamentos.criarNaOs(os, revisao("180,00"));
        orcamentos.marcarEnviado(r1.id());
        orcamentos.recusar(r1.id(), agora(), MeioAprovacao.TELEFONE, "Achou caro.");
        assertEquals(StatusOs.ABERTA, status(os));

        OrcamentoSalvo r2 = orcamentos.novaRevisao(r1.id());
        orcamentos.salvar(r2.id(), revisao("150,00"));
        orcamentos.marcarEnviado(r2.id());
        assertEquals(StatusOs.AGUARDANDO_APROVACAO, status(os));
        OrcamentoSalvo r3 = orcamentos.novaRevisao(r2.id());
        assertEquals(StatusOs.ABERTA, status(os), "revisão antes da aprovação volta a OS para aberta");
        enviarEAprovar(r3);
        assertEquals(StatusOs.APROVADA, status(os));
        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");

        OrcamentoSalvo r4 = orcamentos.novaRevisao(r3.id());
        orcamentos.salvar(r4.id(), revisao("200,00"));
        orcamentos.marcarEnviado(r4.id());
        assertEquals(StatusOs.EM_EXECUCAO, status(os), "o cliente pediu mais coisa: a OS segue o serviço");
        assertEquals(Dinheiro.ler("480,00"), oficina.pagamento(os).valor(), "vale a rev. 3, aprovada (4 × 45 + 2 × 150)");
        orcamentos.aprovar(r4.id(), agora(), MeioAprovacao.PESSOALMENTE, "");
        assertEquals(Dinheiro.ler("580,00"), oficina.pagamento(os).valor(), "agora a rev. 4");
        assertEquals(StatusOs.EM_EXECUCAO, status(os));
    }

    /** O orçamento pela aba do projeto abre a OS do projeto, e a OS e o projeto acompanham o orçamento. */
    @Test
    void osDePreparacaoPeloProjeto() {
        EntradaDimensionamento casoB = CasosProjetoTest.casoB();
        long projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(CasosProjetoTest.cliente("Diego Rocha")),
                new VeiculoNovo(CasosProjetoTest.carro("DEF4G56")), "AP 2.0 turbo", casoB.motor(), casoB.meta()));
        casos.salvarCalculo(projeto, casoB.comMotor(casos.abrir(projeto).motor().configuracao()),
                List.of(new Justificativa(Indicador.DC_BICO, "Bico provisório.", agora())));

        OrcamentoSalvo orcamento = orcamentos.criar(projeto, revisao("180,00"));
        assertTrue(orcamento.dePreparacao());
        long os = oficina.osAtualDoProjeto(projeto);
        assertEquals(os, orcamento.osId());
        OsAberta aberta = oficina.buscar(os);
        assertEquals(projeto, aberta.os().projetoId());
        assertEquals("Preparação", aberta.os().servicoPedido());
        assertFalse(aberta.os().veiculoChegou(), "o carro pode chegar depois");

        orcamentos.marcarEnviado(orcamento.id());
        assertEquals(StatusOs.AGUARDANDO_APROVACAO, status(os));
        assertEquals(StatusProjeto.ORCADO, casos.abrir(projeto).projeto().status());
        orcamentos.aprovar(orcamento.id(), agora(), MeioAprovacao.PESSOALMENTE, "");
        assertEquals(StatusOs.APROVADA, status(os));
        assertEquals(StatusProjeto.APROVADO, casos.abrir(projeto).projeto().status());
        assertEquals(os, oficina.osAtualDoProjeto(projeto), "não abre outra OS");
        assertEquals(List.of(orcamento.id()), orcamentos.orcamentosDaOs(os).stream()
                .map(situacao -> situacao.orcamento().id()).toList());
    }

    @Test
    void regrasEmPortugues() {
        EntradaInvalidaException erros = assertThrows(EntradaInvalidaException.class, () -> oficina.abrir(new NovaOs(
                new ClienteNovo(CasosProjetoTest.cliente("Eva Lima")), new VeiculoExistente(1), " ", null, null, "",
                new EntradaVeiculo(agora().plusDays(1), null, null, null))));
        assertEquals(List.of("Cliente novo precisa de veículo novo.", "Descreva o serviço pedido pelo cliente.",
                "A data e a hora da entrada não podem estar no futuro."), erros.erros());

        long fabio = oficina.novoMecanico("Fábio");
        oficina.ativarMecanico(fabio, false);
        OperacaoRecusadaException inativo = assertThrows(OperacaoRecusadaException.class, () -> oficina.abrir(
                new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Eva Lima")),
                        new VeiculoNovo(CasosProjetoTest.carro("EFG5H67")), "Revisão", fabio, null, "", null)));
        assertEquals("O mecânico Fábio está inativo.", inativo.getMessage());
        assertEquals(List.of(), oficina.mecanicos(false));
        oficina.renomearMecanico(fabio, " Fábio Souza ");
        assertEquals("Fábio Souza", oficina.mecanicos(true).getFirst().nome(), "renomear vale também para o inativo");
        assertThrows(EntradaInvalidaException.class, () -> oficina.renomearMecanico(fabio, " "));
        assertThrows(OperacaoRecusadaException.class, () -> oficina.renomearMecanico(999, "Ninguém"));

        long os = abrirComEntrada("Eva Lima", "EFG5H67");
        OperacaoRecusadaException doOrcamento = assertThrows(OperacaoRecusadaException.class,
                () -> oficina.mudarStatus(os, StatusOs.APROVADA, ""));
        assertEquals("Esse passo vem do orçamento: marque como enviado, ou registre a aprovação ou a recusa.",
                doOrcamento.getMessage());
        EntradaInvalidaException antes = assertThrows(EntradaInvalidaException.class,
                () -> oficina.entregar(os, new Entrega(agora().minusDays(1), null, new Garantia(90))));
        assertEquals("A entrega não pode ser antes da entrada do veículo.", antes.getMessage());
        EntradaInvalidaException semMotivo = assertThrows(EntradaInvalidaException.class,
                () -> oficina.cancelar(os, " "));
        assertEquals("Escreva o motivo do cancelamento.", semMotivo.getMessage());

        oficina.cancelar(os, "Cliente desistiu.");
        assertEquals(StatusOs.CANCELADA, status(os));
        OperacaoRecusadaException cancelada = assertThrows(OperacaoRecusadaException.class,
                () -> oficina.registrarPagamento(os, new Pagamento(agora(), FormaPagamento.PIX, Dinheiro.ler("10,00"),
                        "")));
        assertEquals("A OS OS-2026-0001 está cancelada e não recebe pagamento.", cancelada.getMessage(),
                "as tentativas que falharam não gastaram número");
        assertThrows(OperacaoRecusadaException.class, () -> orcamentos.criarNaOs(os, revisao("180,00")),
                "OS cancelada não ganha orçamento");
    }

    /** Fotos e documentos (bloco 3.2): a cópia na pasta de anexos, uma cópia só para o mesmo arquivo, e as recusas. */
    @Test
    void anexosDaOs() throws Exception {
        long os = abrirComEntrada("Gabi Nunes", "FGH6I78");
        Path foto = raiz.resolve("IMG_0001.jpg");
        Files.write(foto, new byte[] {(byte) 0xFF, (byte) 0xD8, 1, 2, 3});
        Path nota = Files.writeString(raiz.resolve("nota.pdf"), "%PDF-1.7 nota fictícia");
        List<AnexoSalvo> gravados = oficina.anexar(os, List.of(
                new NovoAnexo(foto, TipoAnexo.FOTO_ENTRADA, " Porta direita ", null, false),
                new NovoAnexo(nota, TipoAnexo.NOTA_FISCAL, "NFS-e 123", null, false),
                new NovoAnexo(foto, TipoAnexo.FOTO_SERVICO, "A mesma foto", null, false)));
        assertEquals(3, gravados.size());
        assertEquals("Porta direita", gravados.getFirst().descricao());
        assertEquals("IMG_0001.jpg", gravados.getFirst().nomeOriginal());
        assertEquals(64, gravados.getFirst().sha256().length());
        assertEquals(gravados.get(0).arquivo(), gravados.get(2).arquivo(), "o mesmo arquivo é uma cópia só");
        Path copia = oficina.arquivoDoAnexo(gravados.getFirst()).orElseThrow();
        assertTrue(copia.startsWith(pastas.anexos()), "a cópia fica na pasta de anexos (o backup leva)");
        assertArrayEquals(Files.readAllBytes(foto), Files.readAllBytes(copia));

        OperacaoRecusadaException heic = assertThrows(OperacaoRecusadaException.class, () -> oficina.anexar(os,
                List.of(new NovoAnexo(Files.writeString(raiz.resolve("foto.heic"), "x"), TipoAnexo.FOTO_ENTRADA, "",
                        null, false))));
        assertTrue(heic.getMessage().startsWith("O arquivo foto.heic não é foto (JPG ou PNG) nem PDF."),
                heic.getMessage());
        Path grande = raiz.resolve("grande.pdf");
        try (RandomAccessFile arquivo = new RandomAccessFile(grande.toFile(), "rw")) {
            arquivo.setLength(AnexosDaOficina.TAMANHO_MAXIMO_BYTES + 1);
        }
        assertThrows(OperacaoRecusadaException.class, () -> oficina.anexar(os, List.of(
                new NovoAnexo(nota, TipoAnexo.LAUDO, "", null, false),
                new NovoAnexo(grande, TipoAnexo.LAUDO, "", null, false))), "20 MB no máximo");
        assertEquals(3, oficina.anexosDaOs(os).size(), "com um recusado, nenhum da leva é gravado");

        oficina.removerAnexo(gravados.get(2).id());
        assertTrue(Files.exists(copia), "a outra foto ainda usa a cópia");
        oficina.removerAnexo(gravados.get(0).id());
        assertFalse(Files.exists(copia), "ninguém usa mais: o arquivo vai embora");
        assertEquals(List.of(gravados.get(1).id()), oficina.anexosDaOs(os).stream().map(AnexoSalvo::id).toList());

        oficina.cancelar(os, "Cliente desistiu.");
        OperacaoRecusadaException cancelada = assertThrows(OperacaoRecusadaException.class,
                () -> oficina.removerAnexo(gravados.get(1).id()));
        assertEquals("A OS OS-2026-0001 está \"Cancelada\": os anexos dela não se removem.", cancelada.getMessage());
        assertEquals(1, oficina.anexar(os, List.of(new NovoAnexo(nota, TipoAnexo.OUTRO, "Depois", null, false)))
                .size(), "anexar ainda vale");
    }

    /**
     * A ficha do veículo (bloco 3.3): as OS dele, os projetos (também o arquivado) e os anexos de todas as OS, com os
     * documentos do próprio veículo, sem OS.
     */
    @Test
    void fichaEDocumentosDoVeiculo() throws Exception {
        EntradaDimensionamento casoB = CasosProjetoTest.casoB();
        long projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(CasosProjetoTest.cliente("Gabi Nunes")),
                new VeiculoNovo(CasosProjetoTest.carro("FGH6I78")), "AP 2.0 turbo", casoB.motor(), casoB.meta()));
        var aberto = casos.abrir(projeto);
        long veiculo = aberto.veiculo().id();
        long os = oficina.abrir(new NovaOs(new ClienteExistente(aberto.cliente().id()), new VeiculoExistente(veiculo),
                "Revisão", null, null, "", entrada(251_500.0))).osId();
        casos.arquivar(projeto);

        Path foto = raiz.resolve("IMG_0002.jpg");
        Files.write(foto, new byte[] {(byte) 0xFF, (byte) 0xD8, 4, 5, 6});
        oficina.anexar(os, List.of(new NovoAnexo(foto, TipoAnexo.FOTO_ENTRADA, "", null, false)));
        Path crlv = Files.writeString(raiz.resolve("crlv.pdf"), "%PDF-1.7 CRLV fictício");
        AnexoSalvo documento = oficina.anexarAoVeiculo(veiculo, List.of(new NovoAnexo(crlv, TipoAnexo.DOCUMENTO,
                "CRLV 2026", LocalDate.of(2027, 3, 31), true))).getFirst();
        assertNull(documento.osId(), "do veículo, sem OS");
        assertEquals(LocalDate.of(2027, 3, 31), documento.validade());
        assertEquals(List.of(TipoAnexo.FOTO_ENTRADA, TipoAnexo.DOCUMENTO), oficina.anexosDoVeiculo(veiculo).stream()
                .map(AnexoSalvo::tipo).toList(), "os das OS e os do veículo");
        assertEquals(1, oficina.anexosDaOs(os).size(), "o documento do veículo não entra na OS");

        FichaVeiculo ficha = oficina.ficha(veiculo);
        assertEquals("Gabi Nunes", ficha.cliente().cliente().nome());
        assertEquals("FGH6I78", ficha.veiculo().veiculo().placa());
        assertEquals(List.of(os), ficha.ordens().stream().map(ResumoOs::osId).toList());
        assertEquals(List.of(new ProjetoDoVeiculo(projeto, "AP 2.0 turbo v1", StatusProjeto.ARQUIVADO)),
                ficha.projetos(), "na ficha, também o arquivado");
        assertEquals(List.of(), oficina.projetosDoVeiculo(veiculo), "para ligar à OS, não");

        assertThrows(OperacaoRecusadaException.class, () -> oficina.anexarAoVeiculo(9_999, List.of(
                new NovoAnexo(crlv, TipoAnexo.DOCUMENTO, "", null, true))));
        assertThrows(OperacaoRecusadaException.class, () -> oficina.ficha(9_999));
    }

    /**
     * LGPD (bloco 3.3): anonimizar o cliente apaga, na mesma gravação, os anexos marcados como documento pessoal (das
     * OS dele e do veículo dele); o arquivo sai da pasta só quando nenhum outro anexo o usa. Fotos e notas ficam, e os
     * documentos de outro cliente também.
     */
    @Test
    void anonimizarApagaSoOsDocumentosPessoais() throws Exception {
        long os = abrirComEntrada("Lia Mota", "JKL0M12");
        OsAberta aberta = oficina.buscar(os);
        long veiculo = aberta.veiculo().id();
        Path foto = raiz.resolve("IMG_0003.jpg");
        Files.write(foto, new byte[] {(byte) 0xFF, (byte) 0xD8, 7, 8, 9});
        Path cnh = Files.writeString(raiz.resolve("cnh.pdf"), "%PDF-1.7 CNH fictícia");
        Path nota = Files.writeString(raiz.resolve("nota-lia.pdf"), "%PDF-1.7 nota fictícia");
        List<AnexoSalvo> daOs = oficina.anexar(os, List.of(
                new NovoAnexo(foto, TipoAnexo.FOTO_ENTRADA, "", null, false),
                new NovoAnexo(cnh, TipoAnexo.DOCUMENTO, "CNH", null, true),
                new NovoAnexo(nota, TipoAnexo.NOTA_FISCAL, "", null, false)));
        Path crlv = Files.writeString(raiz.resolve("crlv-lia.pdf"), "%PDF-1.7 CRLV fictício");
        List<AnexoSalvo> doVeiculo = oficina.anexarAoVeiculo(veiculo, List.of(
                new NovoAnexo(crlv, TipoAnexo.DOCUMENTO, "CRLV", null, true),
                new NovoAnexo(cnh, TipoAnexo.OUTRO, "A mesma CNH, sem a marca de pessoal", null, false)));
        long outra = abrirComEntrada("Nina Rocha", "KLM1N23");
        Path doc = Files.writeString(raiz.resolve("doc-nina.pdf"), "%PDF-1.7 documento fictício");
        AnexoSalvo daNina = oficina.anexar(outra, List.of(new NovoAnexo(doc, TipoAnexo.DOCUMENTO, "", null, true)))
                .getFirst();

        assertEquals(new Anonimizacao(2, 0), oficina.anonimizarCliente(aberta.cliente().id()),
                "a CNH da OS e o CRLV do veículo");
        assertTrue(oficina.buscar(os).cliente().cliente().nome().startsWith("Cliente anonimizado #"));
        assertEquals("", oficina.buscar(os).veiculo().veiculo().placa());
        assertEquals(List.of(daOs.get(0).id(), daOs.get(2).id(), doVeiculo.get(1).id()),
                oficina.anexosDoVeiculo(veiculo).stream().map(AnexoSalvo::id).toList(), "a foto e a nota ficam");
        assertFalse(Files.exists(pastas.anexos().resolve(doVeiculo.get(0).arquivo())), "o arquivo do CRLV sai");
        assertTrue(Files.exists(pastas.anexos().resolve(daOs.get(1).arquivo())),
                "o da CNH fica: o outro anexo (sem a marca de pessoal) ainda usa o mesmo arquivo");
        assertEquals(List.of(daNina.id()), oficina.anexosDaOs(outra).stream().map(AnexoSalvo::id).toList(),
                "o documento da outra cliente fica");
        assertEquals(new Anonimizacao(0, 0), oficina.anonimizarCliente(aberta.cliente().id()),
                "de novo: nada mais a apagar");
    }

    /**
     * Revisão final (bloco 3.4): o documento pessoal sai também da OS entregue, cujo anexo não se remove pela tela; a
     * OS continua como estava.
     */
    @Test
    void anonimizarApagaDocumentoPessoalDeOsEntregue() throws Exception {
        long os = abrirComEntrada("Olga Lima", "LMN2O34");
        enviarEAprovar(orcamentos.criarNaOs(os, revisao("180,00")));
        oficina.mudarStatus(os, StatusOs.EM_EXECUCAO, "");
        oficina.mudarStatus(os, StatusOs.PRONTA, "");
        Path cnh = Files.writeString(raiz.resolve("cnh-olga.pdf"), "%PDF-1.7 CNH fictícia");
        AnexoSalvo documento = oficina.anexar(os, List.of(new NovoAnexo(cnh, TipoAnexo.DOCUMENTO, "CNH", null, true)))
                .getFirst();
        relogio.andar(Duration.ofHours(2));
        oficina.entregar(os, new Entrega(agora(), 251_530.0, new Garantia(90)));
        assertThrows(OperacaoRecusadaException.class, () -> oficina.removerAnexo(documento.id()),
                "pela tela, o anexo da OS entregue não sai");

        assertEquals(new Anonimizacao(1, 0), oficina.anonimizarCliente(oficina.buscar(os).cliente().id()));
        assertEquals(List.of(), oficina.anexosDaOs(os), "a anonimização tira o documento pessoal mesmo da OS entregue");
        assertFalse(Files.exists(pastas.anexos().resolve(documento.arquivo())));
        assertEquals(StatusOs.ENTREGUE, status(os), "a OS fica como estava");
    }

    /**
     * Revisão pelo agente: cliente anonimizado não ganha OS nova pelo orçamento do projeto, nem documento pessoal novo
     * (na OS ou no veículo). Anexo sem a marca de pessoal ainda entra.
     */
    @Test
    void clienteAnonimizadoNaoRecebeOsNovaNemDocumentoPessoal() throws Exception {
        EntradaDimensionamento casoB = CasosProjetoTest.casoB();
        long projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(CasosProjetoTest.cliente("Sara Prado")),
                new VeiculoNovo(CasosProjetoTest.carro("OPQ5R67")), "AP 2.0 turbo", casoB.motor(), casoB.meta()));
        long os = oficina.osAtualDoProjeto(projeto);
        oficina.cancelar(os, "Cliente desistiu.");
        var aberto = casos.abrir(projeto);
        long veiculo = aberto.veiculo().id();
        oficina.anonimizarCliente(aberto.cliente().id());

        OperacaoRecusadaException semOs = assertThrows(OperacaoRecusadaException.class,
                () -> oficina.osAtualDoProjeto(projeto));
        assertEquals("O cliente deste projeto foi anonimizado (LGPD) e não recebe OS nova. O projeto e os cálculos "
                + "ficam para consulta.", semOs.getMessage());
        assertEquals(List.of(os), oficina.ficha(veiculo).ordens().stream().map(ResumoOs::osId).toList(),
                "nenhuma OS nova");

        Path cnh = Files.writeString(raiz.resolve("cnh-sara.pdf"), "%PDF-1.7 CNH fictícia");
        NovoAnexo documento = new NovoAnexo(cnh, TipoAnexo.DOCUMENTO, "CNH", null, true);
        OperacaoRecusadaException pessoal = assertThrows(OperacaoRecusadaException.class,
                () -> oficina.anexarAoVeiculo(veiculo, List.of(documento)));
        assertTrue(pessoal.getMessage().startsWith("O cliente foi anonimizado (LGPD)"), pessoal.getMessage());
        assertThrows(OperacaoRecusadaException.class, () -> oficina.anexar(os, List.of(documento)), "nem na OS");
        assertFalse(Files.exists(pastas.anexos().resolve("veiculos").resolve(String.valueOf(veiculo))),
                "recusado antes de copiar");
        Path laudo = Files.writeString(raiz.resolve("laudo.pdf"), "%PDF-1.7 laudo fictício");
        assertEquals(1, oficina.anexar(os, List.of(new NovoAnexo(laudo, TipoAnexo.LAUDO, "", null, false))).size(),
                "sem a marca de pessoal, entra");
    }

    /**
     * Revisão pelo agente: o arquivo do documento pessoal que não sai da pasta na hora (aberto em outro programa; aqui,
     * somente leitura) é contado para o aviso, e a limpeza ao abrir o app apaga depois, junto com a cópia pela metade
     * de uma queda de energia. O arquivo em uso, o recente e o com nome que não é do Calibra ficam.
     */
    @Test
    void arquivoPresoSaiNaLimpezaAoAbrir() throws Exception {
        long os = abrirComEntrada("Paula Reis", "MNO3P45");
        Path cnh = Files.writeString(raiz.resolve("cnh-paula.pdf"), "%PDF-1.7 CNH fictícia");
        Path foto = Files.write(raiz.resolve("IMG_0004.jpg"), new byte[] {(byte) 0xFF, (byte) 0xD8, 1, 2, 3});
        List<AnexoSalvo> gravados = oficina.anexar(os, List.of(
                new NovoAnexo(cnh, TipoAnexo.DOCUMENTO, "CNH", null, true),
                new NovoAnexo(foto, TipoAnexo.FOTO_ENTRADA, "", null, false)));
        Path copiaCnh = pastas.anexos().resolve(gravados.get(0).arquivo());
        Path copiaFoto = pastas.anexos().resolve(gravados.get(1).arquivo());
        Files.setAttribute(copiaCnh, "dos:readonly", true);

        assertEquals(new Anonimizacao(1, 1), oficina.anonimizarCliente(oficina.buscar(os).cliente().id()));
        assertTrue(Files.exists(copiaCnh), "presa: ficou na pasta");
        assertEquals(List.of(gravados.get(1).id()), oficina.anexosDaOs(os).stream().map(AnexoSalvo::id).toList(),
                "o anexo saiu do banco do mesmo jeito");

        Files.setAttribute(copiaCnh, "dos:readonly", false);
        Path pasta = copiaCnh.getParent();
        Path parcial = Files.writeString(pasta.resolve("0123456789abcdef.jpg.parcial"), "foto pela me");
        Path recente = Files.writeString(pasta.resolve("fedcba9876543210.png"), "copiada agora há pouco");
        Path outro = Files.writeString(pasta.resolve("anotacao.txt"), "não é cópia do Calibra");
        for (Path arquivo : List.of(copiaCnh, copiaFoto, parcial, outro)) {
            mudarIdade(arquivo, Duration.ofHours(2));
        }
        mudarIdade(recente, Duration.ofMinutes(30));

        assertEquals(2, oficina.apagarArquivosSemUso(), "a CNH presa e a cópia pela metade");
        assertFalse(Files.exists(copiaCnh));
        assertFalse(Files.exists(parcial));
        assertTrue(Files.exists(copiaFoto), "a foto ainda é anexo da OS");
        assertTrue(Files.exists(recente), "de menos de uma hora: pode ser um anexo sendo gravado agora");
        assertTrue(Files.exists(outro), "nome que o Calibra não dá: não é com ele");
        assertEquals(0, oficina.apagarArquivosSemUso(), "de novo: nada");
    }

    /** Põe a criação e a última mudança do arquivo tanto tempo antes do relógio do teste. */
    private void mudarIdade(Path arquivo, Duration idade) throws IOException {
        FileTime quando = FileTime.from(relogio.instant().minus(idade));
        Files.getFileAttributeView(arquivo, BasicFileAttributeView.class).setTimes(quando, quando, quando);
    }

    /**
     * A Nova OS grava as fotos da entrada junto com a OS. Com uma foto recusada, nada é gravado (nem o cliente, nem a
     * OS) e não sobra cópia na pasta: dá para tirar a foto e tentar de novo sem OS repetida.
     */
    @Test
    void abrirComAsFotosDaEntrada() throws Exception {
        Path porta = Files.write(raiz.resolve("porta.jpg"), new byte[] {(byte) 0xFF, (byte) 0xD8, 4, 5, 6});
        Path painel = Files.write(raiz.resolve("painel.png"), new byte[] {(byte) 0x89, 'P', 'N', 'G', 7});
        Path heic = Files.writeString(raiz.resolve("IMG_0002.heic"), "foto do iPhone");
        NovaOs nova = new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Íris Souza")),
                new VeiculoNovo(CasosProjetoTest.carro("GHI7J89")), "Revisão", null, null, "", entrada(251_500.0));

        assertThrows(OperacaoRecusadaException.class, () -> oficina.abrir(nova, List.of(
                new NovoAnexo(porta, TipoAnexo.FOTO_ENTRADA, "", null, false),
                new NovoAnexo(heic, TipoAnexo.FOTO_ENTRADA, "", null, false))));
        assertEquals(List.of(), oficina.listar(FiltroOs.todas()), "nenhuma OS gravada");
        assertEquals(List.of(), casos.pesquisarClientes("Íris"), "nem o cliente novo");
        try (Stream<Path> arquivos = Files.walk(pastas.anexos())) {
            assertEquals(List.of(), arquivos.filter(Files::isRegularFile).toList(), "a cópia da porta foi apagada");
        }

        long os = oficina.abrir(nova, List.of(new NovoAnexo(porta, TipoAnexo.FOTO_ENTRADA, "Porta", null, false),
                new NovoAnexo(painel, TipoAnexo.FOTO_ENTRADA, "Painel", null, false))).osId();
        assertEquals("OS-2026-0001", oficina.buscar(os).os().numero().texto(), "o número recusado não foi gasto");
        List<AnexoSalvo> fotos = oficina.anexosDaOs(os);
        assertEquals(List.of("porta.jpg", "painel.png"), fotos.stream().map(AnexoSalvo::nomeOriginal).toList());
        assertTrue(fotos.stream().allMatch(foto -> foto.tipo() == TipoAnexo.FOTO_ENTRADA));
        assertArrayEquals(Files.readAllBytes(painel), Files.readAllBytes(oficina.arquivoDoAnexo(fotos.get(1))
                .orElseThrow()));
    }

    /** A OS ganha o projeto de preparação do mesmo veículo; o de outro veículo, não. */
    @Test
    void ligarProjetoDoVeiculo() {
        EntradaDimensionamento casoB = CasosProjetoTest.casoB();
        long projeto = casos.criarProjeto(new NovoProjeto(new ClienteNovo(CasosProjetoTest.cliente("Hugo Reis")),
                new VeiculoNovo(CasosProjetoTest.carro("GHI7J89")), "AP 2.0 turbo", casoB.motor(), casoB.meta()));
        var aberto = casos.abrir(projeto);
        long os = oficina.abrir(new NovaOs(new ClienteExistente(aberto.cliente().id()),
                new VeiculoExistente(aberto.veiculo().id()), "Turbo novo", null, null, "", null)).osId();
        assertEquals(List.of(new ProjetoDoVeiculo(projeto, "AP 2.0 turbo v1", StatusProjeto.RASCUNHO)),
                oficina.projetosDoVeiculo(aberto.veiculo().id()));

        long outra = abrirComEntrada("Iara Luz", "HIJ8K90");
        OperacaoRecusadaException deOutro = assertThrows(OperacaoRecusadaException.class,
                () -> oficina.ligarProjeto(outra, projeto));
        assertEquals("O projeto #" + projeto + " é de outro veículo.", deOutro.getMessage());
        oficina.ligarProjeto(os, projeto);
        assertEquals(projeto, oficina.buscar(os).os().projetoId());
        assertEquals(os, oficina.osAtualDoProjeto(projeto), "a OS ligada é a atual do projeto");
        assertEquals(List.of(os), oficina.ordensDoProjeto(projeto).stream().map(OrdemServicoSalva::id).toList());
    }

    /**
     * O projeto criado pela aba Preparação da OS nasce ligado a ela, com o cliente e o veículo dela. Com a OS já ligada,
     * nada é gravado: não sobra projeto solto.
     */
    @Test
    void projetoCriadoPelaOs() {
        long os = abrirComEntrada("Júlia Prado", "IJK9L01");
        long veiculo = oficina.buscar(os).veiculo().id();
        EntradaDimensionamento casoB = CasosProjetoTest.casoB();
        long projeto = oficina.criarProjetoNaOs(os, "AP 2.0 turbo", casoB.motor(), casoB.meta(), null);
        assertEquals(projeto, oficina.buscar(os).os().projetoId());
        assertEquals(veiculo, casos.abrir(projeto).veiculo().id(), "o veículo da OS, sem cadastro novo");
        assertEquals(oficina.buscar(os).cliente().id(), casos.abrir(projeto).cliente().id());

        OperacaoRecusadaException jaTem = assertThrows(OperacaoRecusadaException.class,
                () -> oficina.criarProjetoNaOs(os, "Outro", casoB.motor(), casoB.meta(), null));
        assertEquals("A OS OS-2026-0001 já está ligada ao projeto #" + projeto + ".", jaTem.getMessage());
        assertEquals(List.of(projeto), oficina.projetosDoVeiculo(veiculo).stream().map(ProjetoDoVeiculo::projetoId)
                .toList(), "a recusa não deixou projeto solto");
    }
}
