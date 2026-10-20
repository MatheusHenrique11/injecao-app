package br.com.calibra.app.casos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.calibra.app.casos.CasosInstalacao.Rascunho;
import br.com.calibra.app.casos.CasosOficina.NovaOs;
import br.com.calibra.app.casos.CasosProjeto.ClienteNovo;
import br.com.calibra.app.casos.CasosProjeto.VeiculoNovo;
import br.com.calibra.core.EntradaInvalidaException;
import br.com.calibra.core.cadastro.Instalacao;
import br.com.calibra.core.cadastro.TipoInstalacao;
import br.com.calibra.core.oficina.EntradaVeiculo;
import br.com.calibra.core.oficina.Entrega;
import br.com.calibra.core.oficina.Garantia;
import br.com.calibra.core.oficina.ItemVistoria;
import br.com.calibra.core.oficina.NivelCombustivel;
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
import br.com.calibra.dados.cadastro.EventoAuditoria;
import br.com.calibra.dados.cadastro.InstalacaoSalva;
import br.com.calibra.dados.oficina.ResumoOs;
import br.com.calibra.dados.orcamento.OrcamentoSalvo;

/**
 * As instalações do veículo (Fase 3B, bloco 3B.5; SPEC 6.2): a nova pela ficha e pela OS, gravar, mudar, retirar e
 * apagar, as regras de data e da OS de origem, e o teste obrigatório nº 7 da fase, na parte das instalações. Dados
 * fictícios.
 */
class CasosInstalacaoTest {

    @TempDir
    Path raiz;

    private final RelogioAjustavel relogio = new RelogioAjustavel(LocalDateTime.of(2026, 10, 20, 9, 0));
    private CasosOrcamento orcamentos;
    private CasosOficina oficina;
    private CasosInstalacao casos;
    private long os;
    private long veiculo;
    private long cliente;

    @BeforeEach
    void abrirBanco() throws IOException {
        PastasDados pastas = new PastasDados(raiz.resolve("dados"));
        pastas.criar();
        BancoDados banco = new BancoDados(pastas.banco());
        Migracoes.migrar(banco, new Backup(pastas, banco, "0.3.0-teste", relogio));
        CasosProjeto projetos = new CasosProjeto(banco, relogio, "0.3.0-teste");
        orcamentos = new CasosOrcamento(banco, relogio, projetos);
        oficina = new CasosOficina(banco, relogio, projetos, pastas.anexos());
        casos = new CasosInstalacao(banco, relogio);
        os = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Ana Costa")),
                new VeiculoNovo(CasosProjetoTest.carro("ABC1D23")), "Instalar o som", null, null, "",
                entrada())).osId();
        veiculo = oficina.buscar(os).os().veiculoId();
        cliente = oficina.buscar(os).os().clienteId();
        relogio.andar(Duration.ofMinutes(30));
    }

    private EntradaVeiculo entrada() {
        return new EntradaVeiculo(LocalDateTime.now(relogio), 120_000.0, NivelCombustivel.MEIO,
                new Vistoria(List.of(new ItemVistoria("Lataria", "ok")), ""));
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    private static Instalacao som(LocalDate data, String especificacao) {
        return new Instalacao(TipoInstalacao.SOM, "Módulo no porta-malas, com fusível de 80 A", "Taramps", "TS 800x4",
                especificacao, data, "");
    }

    /** Leva a OS até a entrega: orçamento aprovado, serviço começado, pronto e entregue. */
    private void entregar(long osId) {
        OrcamentoSalvo orcamento = orcamentos.criarNaOs(osId, new Orcamento(List.of(ItemOrcamento.digitado(
                TipoItem.SERVICO, "Instalação de som", BigDecimal.ONE, "h", Dinheiro.ler("180,00"))),
                Desconto.NENHUM, 1, "", 15, "", ""));
        orcamentos.marcarEnviado(orcamento.id());
        orcamentos.aprovar(orcamento.id(), LocalDateTime.now(relogio).minusMinutes(5), MeioAprovacao.WHATSAPP,
                "Pode fazer.");
        oficina.mudarStatus(osId, StatusOs.EM_EXECUCAO, "");
        oficina.mudarStatus(osId, StatusOs.PRONTA, "");
        oficina.entregar(osId, new Entrega(LocalDateTime.now(relogio), 120_010.0, new Garantia(90)));
    }

    @Test
    void novaPelaFichaEPelaOs() {
        Rascunho pelaFicha = casos.nova(veiculo);
        assertEquals(veiculo, pelaFicha.veiculoId());
        assertNull(pelaFicha.osId(), "pela ficha, sem OS");
        assertNull(pelaFicha.instalacao().dataInstalacao(), "e sem data");
        assertNull(pelaFicha.instalacao().tipo());

        Rascunho pelaOs = casos.novaDaOs(os);
        assertEquals(veiculo, pelaOs.veiculoId());
        assertEquals(os, pelaOs.osId(), "pela OS, já com ela");
        assertEquals(LocalDate.of(2026, 10, 20), pelaOs.instalacao().dataInstalacao(), "e com a data de hoje");

        List<ResumoOs> ordens = casos.osDoVeiculo(veiculo);
        assertEquals(List.of(os), ordens.stream().map(ResumoOs::osId).toList(), "a escolha da OS de origem");
        assertThrows(OperacaoRecusadaException.class, () -> casos.nova(999));
    }

    @Test
    void criarMudarRetirarEApagar() {
        long id = casos.criar(veiculo, os, som(hoje(), "800 W RMS"));
        InstalacaoSalva gravada = casos.buscar(id).orElseThrow();
        assertEquals(som(hoje(), "800 W RMS"), gravada.instalacao());
        assertEquals(List.of(id), casos.daOs(os).stream().map(InstalacaoSalva::id).toList(), "aparece na OS");
        assertEquals(List.of(id), casos.doVeiculo(veiculo).stream().map(InstalacaoSalva::id).toList(),
                "e na ficha");

        relogio.andar(Duration.ofHours(1));
        casos.salvar(id, os, som(hoje(), "800 W RMS, 4 canais"));
        assertEquals("800 W RMS, 4 canais", casos.buscar(id).orElseThrow().instalacao().potenciaOuEspecificacao());
        EventoAuditoria mudanca = casos.historico(id).getFirst();
        assertEquals("Potência ou especificação", mudanca.campo());
        assertEquals("800 W RMS", mudanca.valorAntigo());
        assertEquals("800 W RMS, 4 canais", mudanca.valorNovo());

        relogio.andar(Duration.ofMinutes(10));
        casos.retirar(id, hoje());
        assertEquals(hoje(), casos.buscar(id).orElseThrow().retiradaEm());
        assertEquals("Retirada do veículo", casos.historico(id).getFirst().campo());
        casos.desfazerRetirada(id);
        assertNull(casos.buscar(id).orElseThrow().retiradaEm());

        long alarme = casos.criar(veiculo, null, new Instalacao(TipoInstalacao.ALARME_RASTREADOR,
                "Alarme informado pelo cliente", "", "", "", null, ""));
        assertNull(casos.buscar(alarme).orElseThrow().osId(), "sem OS");
        assertEquals(List.of(id), casos.daOs(os).stream().map(InstalacaoSalva::id).toList(), "não aparece na OS");

        casos.apagar(alarme);
        assertTrue(casos.buscar(alarme).isEmpty());
        assertEquals(List.of(), casos.historico(alarme), "o histórico vai junto");
    }

    @Test
    void regrasAoGravarEAoRetirar() {
        EntradaInvalidaException vazia = assertThrows(EntradaInvalidaException.class, () -> casos.criar(veiculo, null,
                new Instalacao(null, " ", "", "", "", hoje().plusDays(1), "")));
        assertEquals(List.of("Escolha o tipo da instalação.", "Descreva o que foi instalado e onde.",
                "A data da instalação não pode estar no futuro."), vazia.erros());
        assertEquals(List.of(), casos.doVeiculo(veiculo), "nada foi gravado");

        long id = casos.criar(veiculo, os, som(LocalDate.of(2026, 10, 18), "800 W RMS"));
        assertEquals(List.of("A data da retirada não pode estar no futuro."), assertThrows(
                EntradaInvalidaException.class, () -> casos.retirar(id, hoje().plusDays(1))).erros());
        assertEquals(List.of("A retirada não pode ser antes da instalação (18/10/2026)."), assertThrows(
                EntradaInvalidaException.class, () -> casos.retirar(id, LocalDate.of(2026, 10, 17))).erros());
        casos.retirar(id, LocalDate.of(2026, 10, 19));
        OperacaoRecusadaException deNovo = assertThrows(OperacaoRecusadaException.class,
                () -> casos.retirar(id, hoje()));
        assertTrue(deNovo.getMessage().contains("já está retirada (19/10/2026)"), deNovo.getMessage());
        assertEquals(List.of("A data da instalação não pode ser depois da retirada (19/10/2026)."), assertThrows(
                EntradaInvalidaException.class, () -> casos.salvar(id, os, som(hoje(), "800 W RMS"))).erros(),
                "mudar a data da instalação para depois da retirada");

        long outraOs = oficina.abrir(new NovaOs(new ClienteNovo(CasosProjetoTest.cliente("Bruno Dias")),
                new VeiculoNovo(CasosProjetoTest.carro("BCD2E34")), "Alarme", null, null, "", null)).osId();
        OperacaoRecusadaException deOutro = assertThrows(OperacaoRecusadaException.class,
                () -> casos.criar(veiculo, outraOs, som(null, "")));
        assertTrue(deOutro.getMessage().contains("é de outro veículo"), deOutro.getMessage());
        assertThrows(OperacaoRecusadaException.class, () -> casos.salvar(id, outraOs, som(null, "")));
        assertThrows(OperacaoRecusadaException.class, () -> casos.criar(veiculo, 999L, som(null, "")),
                "OS que não existe");
    }

    /**
     * A instalação é do veículo: pela OS entregue não se registra (a tela da OS só mostra), mas pela ficha a esquecida
     * entra com aquela OS de origem, e as instalações dela continuam mudando.
     */
    @Test
    void osEntregueSoPelaFicha() {
        long id = casos.criar(veiculo, os, som(hoje(), "800 W RMS"));
        entregar(os);
        OperacaoRecusadaException pelaOs = assertThrows(OperacaoRecusadaException.class, () -> casos.novaDaOs(os));
        assertTrue(pelaOs.getMessage().contains("está \"Entregue\": registre a instalação pela ficha do veículo"),
                pelaOs.getMessage());

        long esquecida = casos.criar(veiculo, os, new Instalacao(TipoInstalacao.OUTRA, "Tomada USB no console", "",
                "", "", hoje(), ""));
        assertEquals(List.of(esquecida, id), casos.daOs(os).stream().map(InstalacaoSalva::id).toList(),
                "a esquecida aparece na OS (mesma data: a cadastrada por último antes)");
        casos.salvar(id, os, som(hoje(), "800 W RMS, 4 canais"));
        casos.retirar(id, hoje());
        assertTrue(casos.buscar(id).orElseThrow().retirada());
    }

    /** Teste nº 7 da Fase 3B (a parte das instalações): anonimizar o cliente deixa as instalações e o histórico. */
    @Test
    void anonimizarOClienteMantemAsInstalacoes() {
        long id = casos.criar(veiculo, os, som(hoje(), "800 W RMS"));
        casos.salvar(id, os, som(hoje(), "800 W RMS, 4 canais"));
        long alarme = casos.criar(veiculo, null, new Instalacao(TipoInstalacao.ALARME_RASTREADOR,
                "Alarme informado pelo cliente", "Positron", "", "", null, ""));
        List<InstalacaoSalva> antes = casos.doVeiculo(veiculo);
        List<EventoAuditoria> historicoAntes = casos.historico(id);

        oficina.anonimizarCliente(cliente);
        assertEquals(antes, casos.doVeiculo(veiculo));
        assertEquals(historicoAntes, casos.historico(id));
        assertEquals("Positron", casos.buscar(alarme).orElseThrow().instalacao().marca());
        assertEquals(List.of(id), casos.daOs(os).stream().map(InstalacaoSalva::id).toList());
    }
}
