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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
import br.com.calibra.core.calculo.EntradaDimensionamento;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.FormaPagamento;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.NivelCombustivel;
import br.com.calibra.core.oficina.Pagamento;
import br.com.calibra.core.oficina.SituacaoPagamento;
import br.com.calibra.core.oficina.StatusOs;
import br.com.calibra.core.oficina.TipoAnexo;
import br.com.calibra.core.oficina.Vistoria;
import br.com.calibra.core.orcamento.Desconto;
import br.com.calibra.core.orcamento.Dinheiro;
import br.com.calibra.core.orcamento.ItemOrcamento;
import br.com.calibra.core.orcamento.MeioAprovacao;
import br.com.calibra.core.orcamento.Orcamento;
import br.com.calibra.core.orcamento.PadroesOrcamento;
import br.com.calibra.core.orcamento.TipoItem;
import br.com.calibra.core.projeto.StatusProjeto;
import br.com.calibra.core.semaforo.Indicador;
import br.com.calibra.core.semaforo.Justificativa;
import br.com.calibra.dados.BancoDados;
import br.com.calibra.dados.Migracoes;
import br.com.calibra.dados.OperacaoRecusadaException;
import br.com.calibra.dados.PastasDados;
import br.com.calibra.dados.backup.Backup;
import br.com.calibra.dados.oficina.AnexoSalvo;
import br.com.calibra.dados.oficina.FiltroOs;
import br.com.calibra.dados.oficina.MudancaStatusOs;
import br.com.calibra.dados.oficina.OrdemServicoSalva;
import br.com.calibra.dados.oficina.PagamentoSalvo;
import br.com.calibra.dados.oficina.ResumoOs;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;

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

    @BeforeEach
    void abrirBanco() throws IOException {
        pastas = new PastasDados(raiz.resolve("dados"));
        pastas.criar();
        BancoDados banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.3.0-teste", relogio));
        casos = new CasosProjeto(banco, relogio, "0.3.0-teste");
        orcamentos = new CasosOrcamento(banco, relogio, casos);
        oficina = new CasosOficina(banco, relogio, casos, pastas.anexos());
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
}
